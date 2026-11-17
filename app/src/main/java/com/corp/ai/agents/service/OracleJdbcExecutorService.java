package com.corp.ai.agents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OracleJdbcExecutorService {

    private static final Logger log = LoggerFactory.getLogger(OracleJdbcExecutorService.class);

    public record OraclePingResult(boolean success, long latencyMs, String message) {}

    public OraclePingResult testConnection(String url, String user, String password, int timeoutSeconds) {
        log.info("[Oracle JDBC] Testando conexão com {}", url);
        long start = System.currentTimeMillis();

        DriverManager.setLoginTimeout(timeoutSeconds);

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM DUAL")) {

            stmt.setQueryTimeout(timeoutSeconds);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long latency = System.currentTimeMillis() - start;
                    return new OraclePingResult(true, latency, "Conexão e query canônica 'SELECT 1 FROM DUAL' executadas com sucesso.");
                }
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[Oracle JDBC Error] Falha ao conectar em {} -> {}", url, e.getMessage());
            return new OraclePingResult(false, latency, "Falha de conexão JDBC (VPN ou banco inacessível): " + e.getMessage());
        }

        return new OraclePingResult(false, -1, "Retorno vazio da query DUAL.");
    }

    public Map<String, Object> queryInvalidObjects(String url, String user, String password, int timeoutSeconds) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> invalidList = new ArrayList<>();

        String sql = "SELECT object_name, object_type, status FROM user_objects WHERE status = 'INVALID' ORDER BY object_name";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setQueryTimeout(timeoutSeconds);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> item = new HashMap<>();
                    item.put("name", rs.getString("object_name"));
                    item.put("type", rs.getString("object_type"));
                    invalidList.add(item);
                }
            }
            result.put("success", true);
            result.put("invalidCount", invalidList.size());
            result.put("invalidObjects", invalidList);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Erro ao consultar USER_OBJECTS: " + e.getMessage());
        }

        return result;
    }
}
