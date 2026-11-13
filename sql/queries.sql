-- =====================================================================
-- SaintStream — ana səhifə bölmələri üçün 9 SQL sorğusu (MySQL 8)
-- Hər sorğunun nəticəsini Workbench/DBeaver-də işlədib screenshot götür.
-- =====================================================================

-- 1) HERO: collections.slug = 'hero' ----------------------------------
SELECT ci.order_number, c.id, c.title, c.description, c.backdrop_url, c.trailer_url, c.rating, c.age_rating,
       (SELECT GROUP_CONCAT(g.name ORDER BY g.name SEPARATOR ', ')
          FROM content_genres cg JOIN genres g ON g.id = cg.genre_id
         WHERE cg.content_id = c.id) AS genres
FROM collection_items ci
JOIN collections col ON col.id = ci.collection_id
JOIN contents    c   ON c.id   = ci.content_id
WHERE col.slug = 'hero'
ORDER BY ci.order_number;

-- 2) PLATFORM SIRASI: yalnız aktiv platformalar (7 ədəd) --------------
SELECT id, name, logo_url, order_number
FROM platforms
WHERE is_active = TRUE
ORDER BY order_number;

-- 3) JUST RELEASE: release_date ən yenidən köhnəyə --------------------
SELECT c.id, c.title, c.type, c.poster_url, c.rating, c.release_date
FROM contents c
ORDER BY c.release_date DESC, c.id
LIMIT 8;

-- 4) POPULAR OF THE WEEK: order_number = ekrandakı 1–4 rəqəmi ---------
-- Qeyd: "rank" MySQL 8-də rezerv sözdür, ona görə popular_rank yazılıb.
SELECT ci.order_number AS popular_rank, c.id, c.title, c.poster_url, c.rating
FROM collection_items ci
JOIN collections col ON col.id = ci.collection_id
JOIN contents    c   ON c.id   = ci.content_id
WHERE col.slug = 'popular-week'
ORDER BY ci.order_number;

-- 5) FEATURED: label = "#1 in Australia" ------------------------------
SELECT ci.order_number, ci.label, c.id, c.title, c.description, c.backdrop_url, c.rating
FROM collection_items ci
JOIN collections col ON col.id = ci.collection_id
JOIN contents    c   ON c.id   = ci.content_id
WHERE col.slug = 'featured'
ORDER BY ci.order_number;

-- 6) MOVIES: type = movie ---------------------------------------------
SELECT c.id, c.title, c.poster_url, c.rating, c.duration_minutes
FROM contents c
WHERE c.type = 'movie'
ORDER BY c.release_date DESC, c.id
LIMIT 8;

-- 7) SERIES: type = series --------------------------------------------
SELECT c.id, c.title, c.poster_url, c.rating, c.season_count
FROM contents c
WHERE c.type = 'series'
ORDER BY c.release_date DESC, c.id
LIMIT 8;

-- 8) KOREAN SERIES: series + origin_country = 'KR' (5 serial) --------
SELECT c.id, c.title, c.poster_url, c.rating, c.season_count
FROM contents c
WHERE c.type = 'series' AND c.origin_country = 'KR'
ORDER BY c.release_date DESC, c.id;

-- 9) MOVIES ON AWARDS / FAST / LIVE: bir sorğu, 3 bölmə ---------------
SELECT col.slug AS section_name, ci.order_number, ci.label, c.id, c.title, c.poster_url, c.rating,
       p.name AS platform
FROM collection_items ci
JOIN collections col ON col.id = ci.collection_id
JOIN contents    c   ON c.id   = ci.content_id
LEFT JOIN platforms p ON p.id  = c.platform_id
WHERE col.slug IN ('awards', 'fast', 'live')
ORDER BY col.slug, ci.order_number;

-- ---------------------------------------------------------------------
-- BONUS: "The Flash" contents-də 1 qeyddir, amma neçə kolleksiyada var?
-- ---------------------------------------------------------------------
SELECT c.title,
       (SELECT COUNT(*) FROM contents WHERE title = 'The Flash') AS rows_in_contents,
       COUNT(ci.id) AS collections_count,
       GROUP_CONCAT(col.slug ORDER BY col.slug SEPARATOR ', ') AS collections
FROM contents c
JOIN collection_items ci ON ci.content_id = c.id
JOIN collections col     ON col.id = ci.collection_id
WHERE c.title = 'The Flash'
GROUP BY c.id, c.title;
