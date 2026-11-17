package com.corp.ai.agents.skills;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.service.SshExecutorService;
import com.corp.ai.agents.service.WebLogicJmxExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebLogicWcpSkill {

    private static final Logger log = LoggerFactory.getLogger(WebLogicWcpSkill.class);
    private final AppProperties properties;
    private final WebLogicJmxExecutorService jmxExecutorService;
    private final SshExecutorService sshExecutorService;

    public WebLogicWcpSkill(AppProperties properties,
                            WebLogicJmxExecutorService jmxExecutorService,
                            SshExecutorService sshExecutorService) {
        this.properties = properties;
        this.jmxExecutorService = jmxExecutorService;
        this.sshExecutorService = sshExecutorService;
    }

    public Map<String, Object> queryServerStatus(String serverName) {
        log.info("[WebLogicWcpSkill] Inspecionando MBean/Porta do servidor: {}", serverName);
        Map<String, Object> result = new HashMap<>();
        result.put("serverName", serverName);

        if (properties.isSimulationMode()) {
            if ("APP_server1".equalsIgnoreCase(serverName) || "APP_server2".equalsIgnoreCase(serverName)) {
                result.put("state", "RUNNING");
                result.put("healthState", "HEALTH_WARN");
                result.put("activeThreads", 28);
                result.put("stuckThreads", 2);
                result.put("hoggingThreads", 3);
                result.put("message", "Servidor operacional com 2 stuck threads e 3 hogging threads");
                result.put("heapUsagePercent", 54.2);
            } else {
                result.put("state", "RUNNING");
                result.put("healthState", "HEALTH_OK");
                result.put("activeThreads", 14);
                result.put("stuckThreads", 0);
                result.put("hoggingThreads", 0);
                result.put("message", "Nó do WebCenter Portal 100% íntegro (0 stuck threads)");
                result.put("heapUsagePercent", 32.5);
            }
            return result;
        }

        AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();
        return jmxExecutorService.queryServerRuntime(
                wls.getAdminHost(), wls.getAdminPort(), wls.getUsername(), wls.getPassword(), serverName, wls.getTimeoutMs()
        );
    }

    public Map<String, Object> checkDataSourcePools(String dsName) {
        log.info("[WebLogicWcpSkill] Consultando DataSource JDBC: {}", dsName);
        Map<String, Object> result = new HashMap<>();
        result.put("dataSource", dsName);

        if (properties.isSimulationMode()) {
            result.put("maxCapacity", 50);
            result.put("activeConnections", 2);
            result.put("waitingForConnectionCount", 0);
            result.put("highestNumWaiters", 0);
            result.put("state", "Running");
            result.put("message", "Pool " + dsName + " operando normalmente com 2 conexões ativas e 0 em espera.");
            return result;
        }

        // Em modo real, consulta o runtime do pool via REST Management API
        AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();
        return jmxExecutorService.queryDataSourceRuntime(
                wls.getAdminHost(), wls.getAdminPort(), wls.getUsername(), wls.getPassword(), "WC_Portal_server1", dsName, wls.getTimeoutMs()
        );
    }

    public List<Map<String, Object>> getClusterTopologyOverview() {
        if (properties.isSimulationMode()) {
            return List.of(
                    Map.of("name", "AdminServer", "state", "RUNNING", "health", "HEALTH_OK"),
                    Map.of("name", "WC_Portal_server1", "state", "RUNNING", "health", "HEALTH_OK"),
                    Map.of("name", "WC_Portal_server2", "state", "RUNNING", "health", "HEALTH_OK"),
                    Map.of("name", "APP_server1", "state", "RUNNING", "health", "HEALTH_WARN"),
                    Map.of("name", "APP_server2", "state", "RUNNING", "health", "HEALTH_WARN")
            );
        }
        AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();
        return jmxExecutorService.queryClusterServers(
                wls.getAdminHost(), wls.getAdminPort(), wls.getUsername(), wls.getPassword(), wls.getTimeoutMs()
        );
    }

    public Map<String, Object> captureThreadDumpSummary(String serverName) {
        log.info("[WebLogicWcpSkill] Analisando threads e estado de execução para: {}", serverName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverName", serverName);

        if (properties.isSimulationMode()) {
            result.put("totalThreadsAnalyzed", 28);
            result.put("blockedCount", 2);
            result.put("primaryBlockLocation", "oracle.jdbc.driver.T4CPreparedStatement.execute()");
            result.put("detectedLockResource", "Nó de aplicação APP_server1");
            return result;
        }

        // 1. Inspeciona métricas reais do ThreadPool via REST Management API
        AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();
        Map<String, Object> runtime = jmxExecutorService.queryServerRuntime(
                wls.getAdminHost(), wls.getAdminPort(), wls.getUsername(), wls.getPassword(), serverName, wls.getTimeoutMs()
        );

        int stuck = runtime.get("stuckThreads") != null ? (int) runtime.get("stuckThreads") : 0;
        int hogging = runtime.get("hoggingThreads") != null ? (int) runtime.get("hoggingThreads") : 0;
        int total = runtime.get("totalThreads") != null ? (int) runtime.get("totalThreads") : 0;
        String healthState = String.valueOf(runtime.getOrDefault("healthState", "HEALTH_OK"));

        result.put("success", true);
        result.put("stuckThreads", stuck);
        result.put("hoggingThreads", hogging);
        result.put("totalThreads", total);
        result.put("healthState", healthState);
        result.put("heapFreePercent", runtime.get("heapFreePercent"));

        if (stuck > 0 || hogging > 0) {
            result.put("diagnosticAlert", String.format(
                    "Alerta de contenção: %s possui %d Stuck Threads e %d Hogging Threads no WebLogic 12c.",
                    serverName, stuck, hogging
            ));
        } else {
            result.put("diagnosticAlert", String.format(
                    "ThreadPool saudável: %s operando com %d threads ativas e 0 Stuck Threads.",
                    serverName, total
            ));
        }

        // 2. Tenta capturar dump jstack via SSH se credenciais de SO estiverem presentes
        AppProperties.SshProperties ssh = properties.getInfra().getApache();
        if ((ssh.getPassword() != null && !ssh.getPassword().isBlank()) || (ssh.getPrivateKeyPath() != null && !ssh.getPrivateKeyPath().isBlank())) {
            String cmd = "PID=$(pgrep -u oracle -f \"" + serverName + "\" | head -n 1); if [ -n \"$PID\" ]; then jstack -l $PID | grep -E \"(BLOCKED|STUCK|State:)\" | head -n 30; else echo \"PROCESS_NOT_FOUND\"; fi";
            SshExecutorService.SshResult sshResult = sshExecutorService.executeNonSudoCommand(
                    ssh.getHost(), ssh.getPort(), ssh.getUser(), ssh.getPassword(), ssh.getPrivateKeyPath(), cmd, ssh.getTimeoutMs()
            );
            result.put("sshJstackSuccess", sshResult.success());
            result.put("threadDumpSnippet", sshResult.output());
        } else {
            result.put("sshJstackNotice", "Acesso SSH aos servidores Linux para jstack requer credenciais de SO no .env.");
        }

        return result;
    }

    /**
     * Provisiona plano/configuração de novo Managed Server e JDBC DataSource via WebLogic REST Management API.
     */
    public Map<String, Object> provisionManagedServerAndDataSource(String newServerName, int listenPort, String dsName, String targetDb) {
        log.info("[WebLogicWcpSkill] Preparando provisionamento de Managed Server '{}' (porta {}) e DataSource '{}' ({})",
                newServerName, listenPort, dsName, targetDb);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("newManagedServer", newServerName);
        result.put("listenPort", listenPort);
        result.put("dataSourceName", dsName);
        result.put("targetDatabase", targetDb);
        result.put("restEditEndpoint", "/management/weblogic/latest/edit/servers");
        result.put("restDataSourceEndpoint", "/management/weblogic/latest/edit/JDBCSystemResources");
        result.put("status", "READY_FOR_ACTIVATION");
        result.put("message", String.format(
                "WebLogic Admin: Configuração preparada para Managed Server '%s' (porta %d) e DataSource '%s' apontando para %s via REST Management API.",
                newServerName, listenPort, dsName, targetDb
        ));
        return result;
    }
}
