package com.saintstream.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Uzun stack trace əvəzinə konsolda aydın xəta mesajı çap edir (META-INF/spring.factories-də qeydiyyatdadır). */
public class DbConnectionFailureAnalyzer extends AbstractFailureAnalyzer<DbConnectionFailedException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, DbConnectionFailedException cause) {
        String description = "Verilənlər bazasına qoşulmaq mümkün olmadı. Server başlamır.\n"
                + "  URL:         " + cause.getUrl() + "\n"
                + "  İstifadəçi:  " + cause.getUser() + "\n"
                + "  Səbəb:       " + rootMessage(cause);
        String action = "MySQL-in işlədiyini yoxla və .env dəyərlərini "
                + "(DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD) düzəlt.";
        return new FailureAnalysis(description, action, cause);
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
