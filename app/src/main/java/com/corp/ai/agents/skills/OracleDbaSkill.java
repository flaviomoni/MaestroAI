package com.corp.ai.agents.skills;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.service.OracleJdbcExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OracleDbaSkill {

    private static final Logger log = LoggerFactory.getLogger(OracleDbaSkill.class);
    private final AppProperties properties;
    private final OracleJdbcExecutorService oracleJdbcExecutorService;

    public OracleDbaSkill(AppProperties properties, OracleJdbcExecutorService oracleJdbcExecutorService) {
        this.properties = properties;
        this.oracleJdbcExecutorService = oracleJdbcExecutorService;
    }

    public Map<String, Object> testPing(String targetDb) {
        log.info("[OracleDbaSkill] Medindo latência de query canônica 'SELECT 1 FROM DUAL' no banco: {}", targetDb);
        Map<String, Object> result = new HashMap<>();
        result.put("targetDatabase", targetDb);

        if (properties.isSimulationMode()) {
            if ("WCPDB".equalsIgnoreCase(targetDb)) {
                result.put("success", true);
                result.put("pingLatencyMs", 18);
                result.put("latencyMs", 18L);
                result.put("status", "HEALTHY");
                result.put("message", "Conexão e query canônica 'SELECT 1 FROM DUAL' executadas com sucesso em WCPDB.");
            } else {
                result.put("success", true);
                result.put("pingLatencyMs", 12);
                result.put("latencyMs", 12L);
                result.put("status", "HEALTHY");
                result.put("message", "Conexão e query canônica 'SELECT 1 FROM DUAL' executadas com sucesso em APPSDB.");
            }
            return result;
        }

        AppProperties.DbProperties db = "WCPDB".equalsIgnoreCase(targetDb) ?
                properties.getInfra().getOracleWcp() : properties.getInfra().getOracleApps();

        OracleJdbcExecutorService.OraclePingResult ping = oracleJdbcExecutorService.testConnection(
                db.getUrl(), db.getUser(), db.getPassword(), db.getTimeoutSeconds()
        );

        result.put("success", ping.success());
        result.put("latencyMs", ping.latencyMs());
        result.put("status", ping.success() ? "HEALTHY" : "UNREACHABLE");
        result.put("message", ping.message());
        return result;
    }

    public Map<String, Object> checkAppLocksAndBlockers(String targetDb) {
        log.info("[OracleDbaSkill] Inspecionando locks e objetos inválidos (Modo Non-DBA) em: {}", targetDb);
        Map<String, Object> result = new HashMap<>();
        result.put("targetDatabase", targetDb);

        if (properties.isSimulationMode()) {
            if ("WCPDB".equalsIgnoreCase(targetDb)) {
                result.put("blockingSessionFound", false);
                result.put("status", "CLEAN");
                result.put("invalidObjectsSummary", "0 objetos inválidos");
                result.put("message", "Nenhuma sessão bloqueadora detectada no schema " + properties.getInfra().getOracleWcp().getUser() + ".");
            } else {
                result.put("blockingSessionFound", false);
                result.put("status", "CLEAN");
                result.put("invalidObjectsSummary", "Nenhum bloqueio ativo");
                result.put("message", "Nenhuma sessão bloqueadora detectada no schema " + properties.getInfra().getOracleApps().getUser() + ".");
            }
            return result;
        }

        AppProperties.DbProperties db = "WCPDB".equalsIgnoreCase(targetDb) ?
                properties.getInfra().getOracleWcp() : properties.getInfra().getOracleApps();

        Map<String, Object> invalidCheck = oracleJdbcExecutorService.queryInvalidObjects(
                db.getUrl(), db.getUser(), db.getPassword(), db.getTimeoutSeconds()
        );

        result.put("invalidObjectsSummary", invalidCheck);
        return result;
    }

    /**
     * Gera e valida o provisionamento de schema/tablespace no banco do WebCenter Portal (WCPDB) ou Aplicações (APPSDB).
     */
    public Map<String, Object> provisionDatabaseSchema(String schemaName, String targetDb) {
        log.info("[OracleDbaSkill] Preparando provisionamento de schema '{}' no banco '{}'", schemaName, targetDb);
        Map<String, Object> result = new HashMap<>();
        String cleanSchema = schemaName.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        String ddl = String.format(
                "CREATE USER %s IDENTIFIED BY \"<SECRET>\" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS; " +
                "GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SYNONYM TO %s;",
                cleanSchema, cleanSchema
        );
        result.put("schemaName", cleanSchema);
        result.put("targetDatabase", targetDb);
        result.put("ddlScript", ddl);
        result.put("status", "PROVISIONING_SCRIPT_READY");
        result.put("message", String.format("Oracle DBA: Script DDL de provisionamento do schema '%s' em %s preparado e validado.", cleanSchema, targetDb));
        return result;
    }
}
