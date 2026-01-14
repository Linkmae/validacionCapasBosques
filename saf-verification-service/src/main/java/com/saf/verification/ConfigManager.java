package com.saf.verification;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestiona configuraciones parametrizables desde BD.
 */
public class ConfigManager {

    @Resource(lookup = "java:jboss/datasources/SAFLogsDS")
    private DataSource logsDS;

    private Map<String, String> configMap = new HashMap<>();

    @PostConstruct
    public void loadConfigurations() {
        try (Connection conn = logsDS.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT config_key, config_value FROM saf_config");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                configMap.put(rs.getString("config_key"), rs.getString("config_value"));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error cargando configuraciones", e);
        }
    }

    public String getConfig(String key) {
        return configMap.get(key);
    }
}