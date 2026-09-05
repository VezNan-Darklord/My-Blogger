-- ============================================
-- V1: 创建生产环境完整 Schema（表 + 约束）
-- 与 JPA 实体映射保持一致；唯一约束/外键/复合主键在此显式声明
-- 类型以 Hibernate 6 + MySQL 8 生成结果为准
-- 参考文档：docs/database-migration-guide.md
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
    article_id  BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, category_id),
    CONSTRAINT fk_ac_article  FOREIGN KEY (article_id)  REFERENCES articles (id),
    CONSTRAINT fk_ac_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 6. 管理员表
CREATE TABLE admins (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    username      VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    avatar        VARCHAR(255),
    role          VARCHAR(255) NOT NULL,
    is_active     TINYINT(1)   NOT NULL,
    last_login_at DATETIME(6),
    bio           VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_admins_username UNIQUE (username),
    CONSTRAINT uk_admins_email    UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
