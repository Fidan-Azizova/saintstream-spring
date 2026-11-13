package com.saintstream.routes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saintstream.common.ApiException;
import com.saintstream.common.ApiResponse;

/**
 * Ana səhifənin 4 açıq endpointi. Sade API: Controller → JdbcTemplate (ayrıca Service/Repository qatı yoxdur).
 */
@RestController
@RequestMapping("/api/home")
public class HomeRoutes {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final Pattern DIGITS = Pattern.compile("^\\d{1,9}$");

    // Bütün bölmələrdə olan ortaq sütunlar
    private static final String BASE_COLUMNS =
            "c.id, c.title, c.poster_url, c.type, c.rating, c.age_rating, c.release_year, c.season_count";

    private final JdbcTemplate jdbc;

    public HomeRoutes(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- GET /api/home/hero
    @GetMapping("/hero")
    public ApiResponse<List<Map<String, Object>>> hero() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT c.id, c.title, c.type, c.release_year, c.duration_minutes, c.season_count,"
                        + " c.description, c.backdrop_url, c.trailer_url"
                        + " FROM collection_items ci"
                        + " JOIN collections col ON col.id = ci.collection_id"
                        + " JOIN contents c ON c.id = ci.content_id"
                        + " WHERE col.slug = ?"
                        + " ORDER BY ci.order_number ASC, c.id ASC"
                        + " LIMIT 5",
                "hero");
        return ApiResponse.ok(attachGenres(rows));
    }

    // ---------------------------------------------------------------- GET /api/home/platforms
    @GetMapping("/platforms")
    public ApiResponse<List<Map<String, Object>>> platforms() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name, logo_url FROM platforms"
                        + " WHERE is_active = ?"
                        + " ORDER BY order_number ASC, id ASC",
                true);
        return ApiResponse.ok(rows);
    }

    // ---------------------------------------------------------------- GET /api/home/featured
    @GetMapping("/featured")
    public ApiResponse<List<Map<String, Object>>> featured() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT c.id, c.title, ci.label AS tagline, c.rating, c.duration_minutes, c.release_year,"
                        + " c.description, c.poster_url, c.backdrop_url"
                        + " FROM collection_items ci"
                        + " JOIN collections col ON col.id = ci.collection_id"
                        + " JOIN contents c ON c.id = ci.content_id"
                        + " WHERE col.slug = ?"
                        + " ORDER BY ci.order_number ASC, c.id ASC",
                "featured");
        return ApiResponse.ok(attachGenres(rows));
    }

    // ---------------------------------------------------------------- GET /api/home/sections
    @GetMapping("/sections")
    public ApiResponse<Map<String, Object>> sections(HttpServletRequest request) {
        Section section = parseSection(request.getParameterValues("section"));
        int limit = parseLimit(request.getParameterValues("limit"));

        String sql = "SELECT " + BASE_COLUMNS + section.extraColumns()
                + " FROM " + section.from()
                + " WHERE " + section.where()
                + " ORDER BY " + section.orderBy()
                + " LIMIT ?";

        List<Object> args = new ArrayList<>(section.params());
        args.add(limit);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, args.toArray());
        if (section.withPlatform()) {
            rows = rows.stream().map(HomeRoutes::groupPlatform).toList();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("section", section.slug());
        data.put("items", attachGenres(rows));
        return ApiResponse.ok(data);
    }

    // ---------------------------------------------------------------- yoxlamalar

    /** section məcburidir və yalnız bir dəfə verilməlidir, enum-da olmalıdır; əks halda 400. */
    private static Section parseSection(String[] values) {
        Section section = (values != null && values.length == 1) ? Section.fromSlug(values[0]) : null;
        if (section == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid section. Allowed values: " + Section.allowedValues());
        }
        return section;
    }

    /** Verilməyibsə 10. Yalnız rəqəmlər, 1–20 aralığı; əks halda 400 ("abc", "1.5", "-1", "", "0", "21" ...). */
    private static int parseLimit(String[] values) {
        if (values == null) {
            return DEFAULT_LIMIT;
        }
        if (values.length == 1 && DIGITS.matcher(values[0]).matches()) {
            int limit = Integer.parseInt(values[0]);
            if (limit >= 1 && limit <= MAX_LIMIT) {
                return limit;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Invalid limit. It must be an integer between 1 and " + MAX_LIMIT);
    }

    // ---------------------------------------------------------------- köməkçilər

    /** platform_name + platform_logo_url → platform {name, logo_url} (platforma yoxdursa null). */
    private static Map<String, Object> groupPlatform(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>(row);
        Object name = result.remove("platform_name");
        Object logo = result.remove("platform_logo_url");
        if (name == null) {
            result.put("platform", null);
        } else {
            Map<String, Object> platform = new LinkedHashMap<>();
            platform.put("name", name);
            platform.put("logo_url", logo);
            result.put("platform", platform);
        }
        return result;
    }

    /**
     * Janrlar N+1 olmadan: bütün kartların id-ləri üçün BİR sorğu.
     * Sonra Map(content_id → janr adları) ilə hər karta genres[] əlavə olunur.
     * "?" sayı id sayına görə yaranır (istifadəçi dəyəri SQL-ə yazılmır).
     */
    private List<Map<String, Object>> attachGenres(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return rows;
        }

        List<Object> ids = rows.stream().map(r -> r.get("id")).toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));

        List<Map<String, Object>> genreRows = jdbc.queryForList(
                "SELECT cg.content_id, g.name FROM content_genres cg"
                        + " JOIN genres g ON g.id = cg.genre_id"
                        + " WHERE cg.content_id IN (" + placeholders + ")"
                        + " ORDER BY cg.content_id ASC, g.name ASC",
                ids.toArray());

        Map<Long, List<String>> byContent = new HashMap<>();
        for (Map<String, Object> g : genreRows) {
            long contentId = ((Number) g.get("content_id")).longValue();
            byContent.computeIfAbsent(contentId, k -> new ArrayList<>()).add((String) g.get("name"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> card = new LinkedHashMap<>(row);
            long id = ((Number) row.get("id")).longValue();
            card.put("genres", byContent.getOrDefault(id, List.of()));
            result.add(card);
        }
        return result;
    }
}
