-- =====================================================================
-- SaintStream — BE-SS-003 : Verilənlər bazası sxemi (MySQL 8.0.16+)
-- Prinsip: bir filmin YALNIZ BİR qeydi var (contents).
--          Ana səhifə bölmələri ona collection_items vasitəsilə istinad edir.
-- DİQQƏT: CHECK məhdudiyyəti MySQL 8.0.16-dan əvvəl ignor edilir, 8.0.16+ lazımdır.
-- DİQQƏT: MySQL-də sütunun yanında yazılan REFERENCES ignor edilir,
--         buna görə bütün FOREIGN KEY-lər ayrıca yazılıb.
-- =====================================================================

-- Köhnə cədvəlləri (asılılıq sırası ilə) silirik ki, skript təkrar işləsin.
-- "users" cədvəlinə toxunmuruq, o, BE-AUTH-001-ə aiddir.
DROP TABLE IF EXISTS watchlist_items;
DROP TABLE IF EXISTS collection_items;
DROP TABLE IF EXISTS collections;
DROP TABLE IF EXISTS content_genres;
DROP TABLE IF EXISTS contents;
DROP TABLE IF EXISTS platforms;
DROP TABLE IF EXISTS genres;

-- ---------------------------------------------------------------------
-- users: BE-AUTH-001 hələ edilməyibsə, watchlist_items-in FK-sı işləsin
-- deyə minimal versiya. Auth task-ında yaradılmış cədvəl varsa, bu sətir
-- heç nə etmir (IF NOT EXISTS).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 1. genres -------------------------------------------------------------
CREATE TABLE genres (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_genres_name (name),
    UNIQUE KEY uq_genres_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 2. platforms ----------------------------------------------------------
CREATE TABLE platforms (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(50)  NOT NULL,
    logo_url     VARCHAR(500),
    order_number INT          NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_platforms_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 3. contents: bütün film və seriallar (tək mənbə) ---------------------
CREATE TABLE contents (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    title            VARCHAR(200)  NOT NULL,
    description      TEXT,
    poster_url       VARCHAR(500),
    backdrop_url     VARCHAR(500),
    trailer_url      VARCHAR(500),
    type             ENUM ('movie', 'series') NOT NULL,
    origin_country   CHAR(2),                       -- "KR", "US", "ID" ...
    release_year     INT           NOT NULL,
    release_date     DATE          NOT NULL,
    duration_minutes INT,                           -- filmlər üçün (seriallarda NULL)
    season_count     INT,                           -- seriallar üçün (filmlərdə NULL)
    rating           DECIMAL(2, 1) NOT NULL,
    age_rating       VARCHAR(10),                   -- PG-13, R, TV-MA ...
    platform_id      BIGINT,                        -- NULL ola bilər
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_contents_platform FOREIGN KEY (platform_id) REFERENCES platforms (id),
    CONSTRAINT chk_contents_rating   CHECK (rating BETWEEN 0.0 AND 5.0),
    CONSTRAINT chk_contents_duration CHECK (duration_minutes IS NULL OR duration_minutes > 0),
    CONSTRAINT chk_contents_seasons  CHECK (season_count IS NULL OR season_count > 0),
    INDEX idx_contents_type_release (type, release_date DESC),
    INDEX idx_contents_origin (origin_country),
    INDEX idx_contents_platform (platform_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 4. content_genres: film <-> janr (çoxdan-çoxa) ----------------------
CREATE TABLE content_genres (
    content_id BIGINT NOT NULL,
    genre_id   BIGINT NOT NULL,
    PRIMARY KEY (content_id, genre_id),
    CONSTRAINT fk_cg_content FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE,
    CONSTRAINT fk_cg_genre   FOREIGN KEY (genre_id)   REFERENCES genres (id)   ON DELETE CASCADE,
    INDEX idx_content_genres_genre (genre_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 5. collections: ana səhifədə əl ilə seçilən siyahılar ---------------
CREATE TABLE collections (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    slug  VARCHAR(50)  NOT NULL,           -- hero, featured, popular-week, awards, fast, live
    title VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_collections_slug (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 6. collection_items: hansı siyahıda hansı film hansı sırada ---------
CREATE TABLE collection_items (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    collection_id BIGINT       NOT NULL,
    content_id    BIGINT       NOT NULL,
    order_number  INT          NOT NULL,   -- popular-week-də ekrandakı 1–4 rəqəmi
    label         VARCHAR(100),            -- "#1 in Australia", "Best Pictures" ...
    PRIMARY KEY (id),
    CONSTRAINT uq_collection_content UNIQUE (collection_id, content_id),
    CONSTRAINT fk_ci_collection FOREIGN KEY (collection_id) REFERENCES collections (id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_content    FOREIGN KEY (content_id)    REFERENCES contents (id)    ON DELETE CASCADE,
    INDEX idx_collection_items_order (collection_id, order_number)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 7. watchlist_items: "Add Watchlist" düyməsi -------------------------
CREATE TABLE watchlist_items (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    content_id BIGINT   NOT NULL,
    added_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_watchlist_user_content UNIQUE (user_id, content_id),
    CONSTRAINT fk_wl_user    FOREIGN KEY (user_id)    REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_wl_content FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
