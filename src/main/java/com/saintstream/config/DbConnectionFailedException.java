package com.saintstream.config;

/** Server başlayanda bazaya qoşulmaq mümkün olmayanda atılır. */
public class DbConnectionFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String url;
    private final String user;

    public DbConnectionFailedException(String url, String user, Throwable cause) {
        super("Verilənlər bazasına qoşulmaq mümkün olmadı", cause);
        this.url = url;
        this.user = user;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }
}
