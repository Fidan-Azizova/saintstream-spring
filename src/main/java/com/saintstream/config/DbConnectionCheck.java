package com.saintstream.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tətbiq başlayanda bazaya bir bağlantı açıb yoxlayır.
 * Uğursuz olarsa istisna atılır → Spring konteyneri qalxmır → web server başlamır.
 * (@PostConstruct web server başlamazdan əvvəl işləyir.)
 */
@Component
public class DbConnectionCheck {

    private final DataSource dataSource;
    private final String url;
    private final String user;

    public DbConnectionCheck(DataSource dataSource,
                             @Value("${spring.datasource.url}") String url,
                             @Value("${spring.datasource.username}") String user) {
        this.dataSource = dataSource;
        this.url = url;
        this.user = user;
    }

    @PostConstruct
    void verify() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(3)) {
                throw new SQLException("Bağlantı etibarsızdır");
            }
        } catch (SQLException e) {
            throw new DbConnectionFailedException(url, user, e);
        }
    }
}
