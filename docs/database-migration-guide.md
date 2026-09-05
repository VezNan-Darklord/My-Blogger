# 数据库迁移与 Flyway 操作指南（修复版）

> 适用项目：My-Personal-Blogger（Spring Boot 3.2.5 / Java 23 / MySQL 8.0+ / Flyway）
>
> 目标：
> 1. 定位并修复 `src/main/resources/db/migration/V1__create_production_indexes.sql` 的已知问题（README 中登记的 P0）；
> 2. 提供修复后的完整迁移脚本；
> 3. 给出基于 **WSL + Docker** 的端到端迁移操作流程。

---

## 1. 现状诊断：V1 脚本问题清单

对现有 `V1__create_production_indexes.sql` 与实体映射（`User` / `Article` / `Comment` / `Category` / `Admin` + `BaseEntity`，列名遵循 Spring Boot 默认 snake_case 物理命名策略）逐条核对后，问题如下：

### 问题 1（致命，P0）：只建索引、不建表，生产首次部署必然失败

- 现状：V1 只执行 `CREATE INDEX ... ON xxx(...)`，**假定表已存在**。
- 但生产配置 `ddl-auto: validate` **不会建表**（validate 只校验），且全新生产库是空库 → Flyway 执行 V1 时直接报 **`Table 'xxx.users' doesn't exist`**。
- 根因：脚本设计时依赖"表由其他途径先建好"，但 `baseline-on-migrate` 并不能凭空造表（baseline 只是给**已存在的库**打版本标记）。

### 问题 2：索引命名与实际列不符

- 第 26 行：`CREATE INDEX idx_article_published_at ON articles(updated_at DESC);`
- 索引名叫 `published_at`，但建的列是 `updated_at`（实体中**不存在** `published_at` 列）。不报错但严重误导维护者，且掩盖了"文章发布时间"这一真实查询需求未被覆盖的事实。

### 问题 3：缺少与 JPA 建表结果的一致性保障

- 关联表 `article_category` 的主键（复合主键 `article_id + category_id`）、唯一约束、外键此前**全部依赖 Hibernate ddl 生成**；若生产走 Flyway，这些约束必须在迁移脚本中显式声明，否则依赖不成立。

### 问题 4：重复执行 → `Duplicate key name`

- 若开发/演示库**手工执行过 V1**，再启用 Flyway（同一库）会因索引已存在而报 `Duplicate key name 'idx_xxx'`。Flyway 靠 `flyway_schema_history` 表保证"只执行一次"，手动执行会破坏这个前提。

### 问题 5：checksum 变更与已执行库的 `validate` 冲突

- Flyway 会对**已成功执行的脚本做校验和校验**。如果某个库已经跑过旧 V1，再修改旧 V1 文件 → 下次启动报 `FlywayValidateException`（checksum mismatch）。
- 结论：**已执行过的库不能直接改旧脚本**，必须新增 V2；只有**从未执行过**的库才能直接重写 V1。

### 问题 6（延伸建议）：`content` 等无长度 String 被映射为 `varchar(255)`

- `Article.content` 用于存 Markdown 全文，但未加 `@Lob`/`@Column(length=...)`，Hibernate 会生成 `varchar(255)`——255 字符存不下长文。**这不是迁移脚本的错**，但建议后续以"实体变更 + 新迁移"方式把 `content` 改为 `TEXT`（见 §7）。

---

## 2. 修复方案

### 方案 A（推荐）：全新库 / 演示 / 学习环境

适用于**从未启用过 Flyway、或可接受清库重建**的库（你的 WSL + Docker 演练就是这种场景）。

做法：把现有 V1 **重写为完整的建表脚本**（含唯一约束、外键、复合主键），索引独立成 V2。

```
src/main/resources/db/migration/
├── V1__create_production_schema.sql      # 建全部表 + 约束（修复问题 1、3）
└── V2__create_production_indexes.sql     # 建索引（修复问题 2、4）
```

> 提示：V1 建表脚本中的类型以 Hibernate 实际生成为准。最稳妥的做法是先在 dev（`ddl-auto: update` + `show-sql`）跑一次，用 `SHOW CREATE TABLE` 拿到权威 DDL，再人工补充索引与注释（操作流程见 §5.3）。

### 方案 B：库已执行过旧 V1

保留旧 V1 不改（避免 checksum 冲突），**仅新增 V2 增量修正**：

- 命名错误的 `idx_article_published_at` 不动（功能等价、无碍使用），**新增**语义正确的 `idx_article_updated_at`；
- 补齐缺失的 `idx_article_favorite_count`。

---

## 3. 修复后的完整脚本（方案 A）

### 3.1 `V1__create_production_schema.sql`

```sql
-- ============================================
-- V1: 创建生产环境完整 Schema（表 + 约束）
-- 与 JPA 实体映射保持一致；唯一约束/外键/复合主键在此显式声明
-- 类型以 Hibernate 6 + MySQL 8 生成结果为准
-- ============================================

-- 1. 用户表
CREATE TABLE users (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    username            VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(60)  NOT NULL,
    password_updated_at DATETIME(6),
    last_login_at       DATETIME(6),
    email               VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    avatar              VARCHAR(255),
    bio                 VARCHAR(255),
    status              VARCHAR(255),
    role                VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 2. 文章表
CREATE TABLE articles (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    content        VARCHAR(255),
    summary        VARCHAR(255),
    cover_image    VARCHAR(255),
    status         VARCHAR(255),
    like_count     INT,
    favorite_count INT,
    view_count     INT,
    author_id      BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 3. 评论表
CREATE TABLE comments (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    content           VARCHAR(255),
    article_id        BIGINT      NOT NULL,
    commenter_id      BIGINT      NOT NULL,
    parent_comment_id BIGINT,
    like_count        INT,
    is_approved       TINYINT(1),
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_article   FOREIGN KEY (article_id)        REFERENCES articles (id),
    CONSTRAINT fk_comments_commenter FOREIGN KEY (commenter_id)      REFERENCES users (id),
    CONSTRAINT fk_comments_parent    FOREIGN KEY (parent_comment_id) REFERENCES comments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 4. 分类表（自关联层级）
CREATE TABLE categories (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    name               VARCHAR(255) NOT NULL,
    parent_category_id BIGINT,
    description        VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name   UNIQUE (name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 5. 文章-分类关联表（复合主键）
CREATE TABLE article_category (
    article_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, category_id),
    CONSTRAINT fk_ac_article  FOREIGN KEY (article_id)  REFERENCES articles (id),
    CONSTRAINT fk_ac_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 6. 管理员表
CREATE TABLE admins (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    username     VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar       VARCHAR(255),
    role         VARCHAR(255) NOT NULL,
    is_active    TINYINT(1)   NOT NULL,
    last_login_at DATETIME(6),
    bio          VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_admins_username UNIQUE (username),
    CONSTRAINT uk_admins_email    UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
```

> 说明：
> - `status` / `role` / `is_approved` 等枚举/布尔列按 Hibernate 实际生成的**可空**定义书写（实体未标 `nullable=false`）；Hibernate `validate` 只校验列存在性与类型，不校验 nullable/索引，因此不会报错。
> - 外键列（`author_id`、`article_id`、`commenter_id`、`parent_comment_id`、`parent_category_id`）InnoDB 会自动创建索引，V2 中不再重复建单列索引，避免冗余。
> - 若 `validate` 报告 `is_approved`/`is_active` 类型不匹配，请把 `TINYINT(1)` 改为 `BIT(1)`（以 Hibernate 打印的 DDL 为准，见 §5.3）。

### 3.2 `V2__create_production_indexes.sql`

```sql
-- ============================================
-- V2: 生产环境索引
-- 唯一索引由 V1 中的 UNIQUE 约束提供；外键索引由 InnoDB 自动创建
-- 修复点：idx_article_published_at → idx_article_updated_at（原命名与列不符）
--         新增 idx_article_favorite_count
-- ============================================

-- 1. users
CREATE INDEX idx_user_status            ON users(status);
CREATE INDEX idx_user_role              ON users(role);
CREATE INDEX idx_user_created_at        ON users(created_at DESC);
CREATE INDEX idx_user_last_login        ON users(last_login_at DESC);
CREATE INDEX idx_user_status_created    ON users(status, created_at DESC);
CREATE INDEX idx_user_status_lastlogin  ON users(status, last_login_at DESC);

-- 2. articles（author_id 由外键索引覆盖）
CREATE INDEX idx_article_status           ON articles(status);
CREATE INDEX idx_article_created_at       ON articles(created_at DESC);
CREATE INDEX idx_article_updated_at       ON articles(updated_at DESC);
CREATE INDEX idx_article_view_count       ON articles(view_count DESC);
CREATE INDEX idx_article_like_count       ON articles(like_count DESC);
CREATE INDEX idx_article_favorite_count   ON articles(favorite_count DESC);
CREATE INDEX idx_article_author_status    ON articles(author_id, status);
CREATE INDEX idx_article_status_updated   ON articles(status, updated_at DESC);
CREATE INDEX idx_article_status_views     ON articles(status, view_count DESC);
CREATE INDEX idx_article_status_likes     ON articles(status, like_count DESC);

-- 3. comments（article_id / commenter_id / parent_comment_id 由外键索引覆盖）
CREATE INDEX idx_comment_created_at        ON comments(created_at DESC);
CREATE INDEX idx_comment_is_approved       ON comments(is_approved);
CREATE INDEX idx_comment_article_created   ON comments(article_id, created_at DESC);
CREATE INDEX idx_comment_commenter_created ON comments(commenter_id, created_at DESC);
CREATE INDEX idx_comment_article_approved  ON comments(article_id, is_approved);

-- 4. categories（parent_category_id 由外键索引覆盖）
-- 5. article_category（复合主键已覆盖 article_id 前缀；为按分类反查文章补充 category_id 索引）
CREATE INDEX idx_article_category_category_id ON article_category(category_id);
```

### 3.3 方案 B 增量脚本（库已跑过旧 V1 时）

```sql
-- V2__fix_production_indexes.sql
CREATE INDEX idx_article_updated_at     ON articles(updated_at DESC);
CREATE INDEX idx_article_favorite_count ON articles(favorite_count DESC);
-- 说明：旧 idx_article_published_at 命名不准确但功能等价，暂不删除；
--       如需清理，请在维护窗口手动 DROP INDEX（MySQL 不支持 DROP INDEX IF EXISTS）。
```

---

## 4. 相关 Flyway 配置解读（`application-prod.yml`）

| 配置项 | 值 | 含义 |
|---|---|---|
| `spring.flyway.enabled` | `true` | 生产启用 Flyway，应用启动时自动执行未跑过的迁移 |
| `spring.flyway.locations` | `classpath:db/migration` | 迁移脚本目录 |
| `spring.flyway.baseline-on-migrate` | `true` | 库中**已存在**表但无历史表时，自动 baseline 跳过（注意：对空库无用） |
| `spring.flyway.baseline-version` | `0` | 视为基线版本，V0 之前的已存在表不校验 |
| `spring.flyway.out-of-order` | `false` | 禁止乱序执行（生产必须 false） |
| `spring.flyway.validate-on-migrate` | `true` | 迁移前校验已执行脚本 checksum |
| `spring.jpa.hibernate.ddl-auto` | `validate` | 只校验 schema 与实体一致，**不建表** → 建表必须由迁移脚本负责 |

> 关键：dev 与 test 环境 `spring.flyway.enabled=false`（见 `application-dev.yml` / `application-test.properties`），因此 V1/V2 只在 prod 生效，不影响日常开发。

---

## 5. WSL + Docker 详细操作流程

### 5.0 环境核对

```bash
# 进入 WSL（假设发行版已装 Docker 引擎 / Docker Desktop 已启用 WSL2 后端）
wsl
docker version        # 确认 docker 可用
# 项目在 WSL 中的路径
cd /mnt/c/Users/admin/Desktop/Spring_Boot/My_Personal_Blogger/My_Personal_Blogger
```

### 5.1 启动 MySQL 与 Redis 容器

```bash
# MySQL（utf8mb4，与 README 一致）
docker run -d --name blog-mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=csulzc_blogdb_prod \
  -p 3306:3306 \
  -v blog_mysql_data:/var/lib/mysql \
  mysql:8.4 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_general_ci

# Redis（prod profile 默认缓存开关=true，需要 Redis 才能启动）
docker run -d --name blog-redis -p 6379:6379 redis:7

docker ps    # 等待两个容器 healthy
```

> 若 3306 被占用：改为 `-p 3307:3306`，并把数据源 URL 的端口同步改成 3307。

### 5.2 建库 / 核对

```bash
docker exec -it blog-mysql mysql -uroot -proot123 -e "SHOW DATABASES;"
# 应能看到 csulzc_blogdb_prod
```

### 5.3（可选，强烈建议）获取 Hibernate 权威 DDL

为避免类型细节（`DATETIME(6)`、`TINYINT(1)` vs `BIT(1)`）与 validate 校验不一致，先在 dev 生成一次权威 DDL：

```bash
# 方式一：用已有 dev 库（Hibernate ddl-auto:update 建好的表）直接查
docker exec -it blog-mysql mysql -uroot -proot123 csulzc_blogdb_dev -e "SHOW CREATE TABLE users\G"

# 方式二：临时把 prod 的 ddl-auto 设为 create 启动一次打印 DDL（跑完即改回 validate）
```

把查到的列名/类型与 §3.1 的脚本核对，如有出入以 Hibernate 为准修正。

### 5.4 放置迁移脚本

按方案 A，替换/新增：

```bash
src/main/resources/db/migration/
├── V1__create_production_schema.sql     # 删除旧 V1__create_production_indexes.sql
└── V2__create_production_indexes.sql
```

### 5.5 准备运行环境（WSL 内）

```bash
# 生产会写这两个目录，先建好并放开权限（否则启动报错）
sudo mkdir -p /var/www/uploads /var/log/blog
sudo chmod -R 777 /var/www/uploads /var/log/blog

# 确认 JDK 23 + Maven
java -version
mvn -version
```

### 5.6 执行迁移

**方式一（推荐）：启动应用，Flyway 随 Spring 启动自动迁移**

```bash
# WSL 内执行（以 prod profile 启动，Flyway 会在启动阶段执行 V1、V2）
SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/csulzc_blogdb_prod?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8' \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=root123 \
REDIS_HOST=127.0.0.1 \
JWT_SECRET='your-strong-secret-key-at-least-32-chars-long-here' \
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**方式二（备选）：Windows 侧执行**（WSL 中 Docker 端口映射到 localhost，Windows 的 `mvnw.cmd` 也能连上）：

```powershell
# PowerShell（在项目目录）
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/csulzc_blogdb_prod?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="root123"
$env:REDIS_HOST="localhost"
$env:JWT_SECRET="your-strong-secret-key-at-least-32-chars-long-here"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

> 说明：pom 未配置 `flyway-maven-plugin`，因此不提供 `mvn flyway:migrate` 的零改动用法；若想用 Maven 插件手动迁移，可临时在 `pom.xml` 加入 `org.flywaydb:flyway-maven-plugin` 后执行 `mvn flyway:migrate -Dflyway.url=... -Dflyway.user=... -Dflyway.password=...`。日常推荐方式一。

### 5.7 验证迁移结果

```bash
# 1) 检查迁移历史表（应看到 V1、V2 两条 success=1 记录）
docker exec -it blog-mysql mysql -uroot -proot123 csulzc_blogdb_prod -e \
  "SELECT installed_rank, version, description, success FROM flyway_schema_history;"

# 2) 检查索引是否创建
docker exec -it blog-mysql mysql -uroot -proot123 csulzc_blogdb_prod -e \
  "SHOW INDEX FROM articles;"
docker exec -it blog-mysql mysql -uroot -proot123 csulzc_blogdb_prod -e \
  "SHOW INDEX FROM comments;"

# 3) 应用日志应出现
#    Successfully applied 2 migrations to schema `csulzc_blogdb_prod` (execution time ...)
```

---

## 6. 常见错误与处理

| 现象 | 原因 | 处理 |
|---|---|---|
| `Table 'xxx.users' doesn't exist` | V1 只建索引不建表（旧版脚本） | 改用方案 A（建表 + 建索引） |
| `Duplicate key name 'idx_xxx'` | 索引已存在（手动执行过 / 历史遗留） | 检查 `flyway_schema_history`；删除冲突索引或清库重来 |
| `FlywayValidateException: checksum mismatch` | 修改了**已执行**的脚本 | 不要改已执行脚本；执行 `mvn flyway:repair` 或删除历史表中对应记录后按方案 B 新增 V2 |
| `validate failed` | 表/列与实体不一致 | 用 `SHOW CREATE TABLE` 与实体核对，以 Hibernate DDL 为准修正脚本 |
| 启动报 Redis 连接失败 | prod 缓存开启但 Redis 未就绪 | 先 `docker run redis`；或临时 `APP_CACHE_ENABLED=false` |
| `Access denied for user` / 认证失败 | MySQL 8 默认 `caching_sha2_password` | URL 带 `allowPublicKeyRetrieval=true`；确认账号密码 |
| 3306 端口被占 | 本机已有 MySQL | 容器换 `-p 3307:3306` 并同步改 URL |

---

## 7. 生产迁移最佳实践（进阶）

1. **迁移脚本不可变**：一旦在某环境执行成功，永不修改；变更一律通过新版本 V2、V3……。
2. **只增不改**：DDL 以 `ADD COLUMN` / `CREATE INDEX` 等增量语句为主；破坏性变更（删列/删表）放独立脚本并评估回滚。
3. **外键/索引由迁移统一管理**：避免与 Hibernate `ddl-auto` 混用，生产 `ddl-auto` 保持 `validate`。
4. **迁移前备份**：`mysqldump` 备份目标库，脚本失败可安全回滚。
5. **大表加索引**：评估在线 DDL（`ALGORITHM=INPLACE`）与维护窗口。
6. **后续演进建议**：`Article.content` 建议通过"实体 `@Lob` + 新迁移 `ALTER TABLE articles MODIFY content TEXT`"修复 255 长度限制；`Article` 若需按发布时间查询，可补充 `published_at` 列 + 索引（命名与 V2 对齐）。

---

## 8. 本指南关联的 README 条目

修复完成后，请同步更新 `README.md`：
- 「已知问题及解决规划」中 Flyway P0 标记为已完成；
- 「生产部署要点」第 1 条改为：*先由 Flyway 执行 V1（建表）→ V2（建索引）*。
