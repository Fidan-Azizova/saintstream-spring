package com.saintstream.routes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * /api/home/sections üçün 8 bölmə. Hər bölmənin SQL-də fərqlənən hissələri burada yığılıb:
 * hansı cədvəllərdən oxunur (from), şərt (where + params), sıralama və əlavə sütunlar.
 * Bu hissələr yalnız koddan gəlir; istifadəçi dəyəri SQL mətninə yazılmır (params "?" ilə ötürülür).
 */
public enum Section {

    JUST_RELEASE("just-release", Q.CONTENTS, "1 = 1", List.of(), Q.BY_RELEASE, "", false),
    POPULAR_WEEK("popular-week", Q.COLLECTION, "col.slug = ?", List.of("popular-week"), Q.BY_ORDER,
            ", ci.order_number AS `rank`", false),
    MOVIES("movies", Q.CONTENTS, "c.type = ?", List.of("movie"), Q.BY_RELEASE, "", false),
    SERIES("series", Q.CONTENTS, "c.type = ?", List.of("series"), Q.BY_RELEASE, "", false),
    KOREAN_SERIES("korean-series", Q.CONTENTS, "c.type = ? AND c.origin_country = ?",
            List.of("series", "KR"), Q.BY_RELEASE, "", false),
    AWARDS("awards", Q.COLLECTION + " LEFT JOIN platforms p ON p.id = c.platform_id", "col.slug = ?",
            List.of("awards"), Q.BY_ORDER,
            ", ci.label, c.description, c.backdrop_url, c.duration_minutes,"
                    + " p.name AS platform_name, p.logo_url AS platform_logo_url", true),
    FAST("fast", Q.COLLECTION, "col.slug = ?", List.of("fast"), Q.BY_ORDER, "", false),
    LIVE("live", Q.COLLECTION, "col.slug = ?", List.of("live"), Q.BY_ORDER, "", false);

    private static final Map<String, Section> BY_SLUG = Arrays.stream(values())
            .collect(Collectors.toMap(Section::slug, Function.identity()));

    private final String slug;
    private final String from;
    private final String where;
    private final List<Object> params;
    private final String orderBy;
    private final String extraColumns;
    private final boolean withPlatform;

    Section(String slug, String from, String where, List<Object> params,
            String orderBy, String extraColumns, boolean withPlatform) {
        this.slug = slug;
        this.from = from;
        this.where = where;
        this.params = params;
        this.orderBy = orderBy;
        this.extraColumns = extraColumns;
        this.withPlatform = withPlatform;
    }

    /** "movies" → MOVIES; yoxdursa null. */
    public static Section fromSlug(String slug) {
        return BY_SLUG.get(slug);
    }

    /** İcazə verilən dəyərlər (xəta mesajı üçün): "just-release, popular-week, ..." */
    public static String allowedValues() {
        return Arrays.stream(values()).map(Section::slug).collect(Collectors.joining(", "));
    }

    public String slug() { return slug; }
    public String from() { return from; }
    public String where() { return where; }
    public List<Object> params() { return params; }
    public String orderBy() { return orderBy; }
    public String extraColumns() { return extraColumns; }
    public boolean withPlatform() { return withPlatform; }

    /** SQL parçaları (enum sabitlərindən əvvəl yüklənsin deyə ayrı class-dadır). */
    private static final class Q {
        static final String CONTENTS = "contents c";
        static final String COLLECTION = "collection_items ci"
                + " JOIN collections col ON col.id = ci.collection_id"
                + " JOIN contents c ON c.id = ci.content_id";
        static final String BY_RELEASE = "c.release_date DESC, c.id ASC";
        static final String BY_ORDER = "ci.order_number ASC, c.id ASC";
    }
}
