DROP TABLE IF EXISTS theories;
DROP TABLE IF EXISTS post_skill_relation;
DROP TABLE IF EXISTS skills;
DROP TABLE IF EXISTS hot_news;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS posts;

CREATE TABLE IF NOT EXISTS hot_news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(512) NOT NULL,
    image_url VARCHAR(512),
    source VARCHAR(128),
    publish_date DATETIME
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS skills (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    pinned TINYINT(1) DEFAULT 0,
    parent_id BIGINT,
    x_axis DOUBLE,
    y_axis DOUBLE,
    version INT DEFAULT 0
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS post_skill_relation (
    post_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, skill_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(512),
    content LONGTEXT,
    category_id BIGINT,
    tags VARCHAR(255),
    view_count INT DEFAULT 0,
    pinned TINYINT(1) DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS theories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_theory_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
