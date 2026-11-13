# SaintStream — Ana səhifə API-si (Java / Spring Boot)

Ana səhifənin bölmələrini qidalandıran **açıq (token tələb etməyən) GET endpointləri**.
Java 17 + Spring Boot 3 + MySQL 8 (`JdbcTemplate`). Ayrıca Service/Repository qatı yoxdur: sade API.
Baza (BE-SS-003) layihənin içindədir: `sql/`.

## İşə salma

```bash
docker compose up -d                  # MySQL 8 (db/user/parol: saintstream)
cp .env.example .env                  # Windows: copy .env.example .env   (lazım olsa dəyərləri dəyiş)

# 1) Bazanı bir dəfə doldur (cədvəlləri silib yenidən yaradır, yalnız dev!):
mvn spring-boot:run -Dspring-boot.run.profiles=init
#    (bitəndə Ctrl+C ilə dayandır)

# 2) Normal işə salma:
mvn spring-boot:run                   # http://localhost:5000
```

IntelliJ-də: `SaintStreamApplication`-ı run et. Bazanı doldurmaq üçün Run Configuration → *Active profiles* = `init`.
`.env` layihənin kök qovluğunda olmalıdır (run-un working directory-si).

DB-yə qoşulmaq mümkün olmasa server **başlamır**; konsolda `APPLICATION FAILED TO START` bloku URL, istifadəçi və səbəblə çıxır.

## .env

| Dəyişən | Məna |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | MySQL bağlantısı |
| `PORT` | Server portu (default 5000) |
| `CORS_ORIGINS` | İcazə verilən origin-lər (vergüllə). `http://localhost:3000` və Vercel domeni |

`.env` `.gitignore`-dadır; GitHub-a yalnız `.env.example` gedir.

## Response formatı

Bütün cavablar (xətalar da): `{ "success": true|false, "message": "...", "data": ... }`. Xətada `data: null`.

## Endpointlər

| Endpoint | Təsvir |
|---|---|
| `GET /api/home/hero` | `hero` kolleksiyası, `order_number ASC`, maks. 5. id, title, type, release_year, duration_minutes, season_count, genres[], description, backdrop_url, trailer_url |
| `GET /api/home/platforms` | `is_active = true`, `order_number ASC`. id, name, logo_url |
| `GET /api/home/featured` | `featured` kolleksiyası, `order_number ASC`. id, title, tagline (= `collection_items.label`), rating, duration_minutes, release_year, genres[], description, poster_url, backdrop_url |
| `GET /api/home/sections?section=…&limit=…` | Eyni formalı kart sıraları |

### `/api/home/sections`

`section` (məcburi): `just-release`, `popular-week`, `movies`, `series`, `korean-series`, `awards`, `fast`, `live`.
`limit` (ixtiyari): tam ədəd 1–20, default 10.

| section | Məntiq |
|---|---|
| just-release | `release_date DESC` |
| popular-week | kolleksiya, `order_number ASC` (+ `rank`) |
| movies / series | `type = movie` / `type = series`, `release_date DESC` |
| korean-series | `type = series AND origin_country = 'KR'` |
| awards | kolleksiya (+ `label`, `description`, `backdrop_url`, `duration_minutes`, `platform {name, logo_url}`) |
| fast, live | kolleksiya, `order_number ASC` |

Cavab: `data.section`, `data.items[]` → id, title, poster_url, type, rating, age_rating, release_year, genres[], season_count (+ bölməyə xas sahələr).
Xətalar (**400**): `section` yoxdur / enum-da yoxdur (`Invalid section. Allowed values: …`); `limit` rəqəm deyil, `< 1` və ya `> 20`.
Boş nəticə: **200** + boş `items`. Hər `ORDER BY` ikinci açarla tamamlanıb, sıra həmişə deterministikdir.

## Kod necə qurulub

```
src/main/java/com/saintstream/
  SaintStreamApplication.java      giriş nöqtəsi (main)
  routes/HomeRoutes.java           4 endpoint (@RestController) + yoxlamalar + janrların qruplanması
  routes/Section.java              8 bölmənin SQL parçaları (enum)
  config/CorsConfig.java           CORS (yalnız CORS_ORIGINS)
  config/DbConnectionCheck.java    başlanğıcda DB yoxlaması, uğursuzdursa server qalxmır
  config/DbConnectionFailed*.java  + FailureAnalyzer: aydın xəta mesajı
  common/ApiResponse.java          {success, message, data}
  common/ApiException.java         400 kimi xətalar üçün
  common/GlobalExceptionHandler    bütün xətaları standart JSON-a çevirir (400/404/405/500)
```

## Test

`postman/SaintStream-Home-API.postman_collection.json` faylını Postman-ə import et (`baseUrl` = `http://localhost:5000`), **Run collection** et.
17 sorğu, hər birində test skripti: hero, platforms, featured, 8 section, section yoxdur (400), `section=abc` (400),
`limit=0` (400), `limit=3` (3 element) + 2 əlavə. Nəticənin screenshot-unu Discord-a at.

## Discord üçün: janrlar N+1 olmadan 

Janrları hər kart üçün ayrıca sorğu ilə gətirsəydim, 8 kartlıq sıra üçün 1 + 8 = 9 sorğu olardı (N+1 problemi).
Əvəzinə **iki sorğu** işlədirəm: 1) kartların özü; 2) bütün bu kartların id-ləri üçün **bir** sorğu:
`SELECT cg.content_id, g.name FROM content_genres cg JOIN genres g ON g.id = cg.genre_id WHERE cg.content_id IN (?, ?, ...)`.
Nəticəni Java-da `Map<Long, List<String>>` (content_id → janr adları) ilə qruplayıb hər karta `genres` kimi əlavə edirəm.
Kart sayı nə qədər olsa da (maks. 20) sorğu sayı həmişə 2-dir; frontend də janrlar üçün ayrı sorğu göndərmir.

## SQL injection-dan qorunma

Bütün dəyərlər (`section`-un slug-ı, `limit`, `'KR'`, `'movie'`, id-lər) `?` placeholder ilə `JdbcTemplate`-ə ötürülür, SQL mətninə birləşdirilmir.
`section` əvvəlcə `Section` enum-undakı sabit siyahı ilə yoxlanır, `limit` isə `^\d{1,9}$` + 1–20 aralığı ilə.
SQL-in dəyişən hissələri (JOIN, ORDER BY) yalnız koddakı sabit `Section` enum-undan gəlir, istifadəçidən yox.
`IN (...)` üçün yalnız `?` işarələrinin sayı id sayına görə yaranır.
