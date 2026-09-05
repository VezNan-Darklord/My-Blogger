-- ============================================
-- V2: 生产环境索引
-- 唯一索引由 V1 中的 UNIQUE 约束提供；外键索引由 InnoDB 自动创建
-- 修复点：idx_article_published_at → idx_article_updated_at（原命名与列不符）
--         新增 idx_article_favorite_count
-- 参考文档：docs/database-migration-guide.md
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
