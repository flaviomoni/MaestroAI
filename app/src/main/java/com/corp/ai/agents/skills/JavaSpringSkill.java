package com.corp.ai.agents.skills;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.service.SshExecutorService;
import com.corp.ai.agents.service.WebLogicJmxExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JavaSpringSkill {

    private static final Logger log = LoggerFactory.getLogger(JavaSpringSkill.class);
    private final AppProperties properties;
    private final SshExecutorService sshExecutorService;
    private final WebLogicJmxExecutorService jmxExecutorService;
    private final HttpClient httpClient;

    public JavaSpringSkill(AppProperties properties,
                           SshExecutorService sshExecutorService,
                           WebLogicJmxExecutorService jmxExecutorService) {
        this.properties = properties;
        this.sshExecutorService = sshExecutorService;
        this.jmxExecutorService = jmxExecutorService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Map<String, Object> queryActuatorHealth() {
        return queryActuatorHealth(properties.getInfra().getSpringActuatorUrl());
    }

    public Map<String, Object> queryActuatorHealth(String serviceUrl) {
        log.info("[JavaSpringSkill] Coletando /actuator/health de: {}", serviceUrl);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceUrl", serviceUrl);

        if (properties.isSimulationMode()) {
            result.put("status", "UP");
            result.put("dbComponent", "UP");
            result.put("diskSpace", "UP");
            result.put("latencyMs", 42L);
            result.put("message", "Spring Boot Actuator UP (" + serviceUrl + ") - 42ms");
            return result;
        }

        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            String status = response.statusCode() == 200 ? "UP" : "DEGRADED";
            result.put("httpStatus", response.statusCode());
            result.put("latencyMs", latency);
            result.put("body", response.body());
            result.put("status", status);
            result.put("message", String.format("Endpoint %s respondeu HTTP %d (%s) em %dms",
                    serviceUrl, response.statusCode(), status, latency));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            result.put("httpStatus", 0);
            result.put("latencyMs", latency);
            result.put("status", "UNREACHABLE");
            result.put("error", "Endpoint Actuator inacessível: " + e.getMessage());
            result.put("message", String.format("Endpoint %s inacessível (%dms): aplicações rodando embarcadas no WebLogic (APP_server1/APP_server2)",
                    serviceUrl, latency));
        }

        return result;
    }

    public Map<String, Object> checkJvmGcMemory(String processName) {
        log.info("[JavaSpringSkill] Inspecionando memória JVM/GC do processo/servidor: {}", processName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("process", processName);

        if (properties.isSimulationMode()) {
            result.put("edenPercent", 64.2);
            result.put("oldGenPercent", 58.7);
            result.put("metaspacePercent", 71.0);
            result.put("fullGcCount", 3);
            result.put("fullGcTimeSeconds", 1.8);
            result.put("status", "HEALTHY");
            result.put("message", String.format("JVM %s saudável: OldGen 58.7%%, Metaspace 71.0%%, Full GC: 3", processName));
            return result;
        }

        // 1. Coleta telemetria real de Heap/JVM via WebLogic REST Management API (JVMRuntime)
        AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();
        Map<String, Object> jvmRuntime = jmxExecutorService.queryJvmRuntime(
                wls.getAdminHost(), wls.getAdminPort(), wls.getUsername(), wls.getPassword(), processName, wls.getTimeoutMs()
        );

        if (Boolean.TRUE.equals(jvmRuntime.get("success"))) {
            long heapFreeMB = ((Number) jvmRuntime.getOrDefault("heapFreeMB", 0L)).longValue();
            long heapSizeMB = ((Number) jvmRuntime.getOrDefault("heapSizeMB", 0L)).longValue();
            long heapMaxMB = ((Number) jvmRuntime.getOrDefault("heapMaxMB", 0L)).longValue();
            int freePercent = ((Number) jvmRuntime.getOrDefault("heapFreePercent", 0)).intValue();
            long uptimeHours = ((Number) jvmRuntime.getOrDefault("uptimeHours", 0L)).longValue();

            result.put("success", true);
            result.put("heapFreeMB", heapFreeMB);
            result.put("heapSizeMB", heapSizeMB);
            result.put("heapMaxMB", heapMaxMB);
            result.put("heapFreePercent", freePercent);
            result.put("heapUsedPercent", 100 - freePercent);
            result.put("uptimeHours", uptimeHours);
            result.put("status", freePercent < 15 ? "WARN_HIGH_MEMORY" : "HEALTHY");
            result.put("message", String.format(
                    "JVM %s: Heap %dMB/%dMB (Máx %dMB) | %d%% livre (%d%% em uso) | Uptime: %dh",
                    processName, heapSizeMB - heapFreeMB, heapSizeMB, heapMaxMB, freePercent, 100 - freePercent, uptimeHours
            ));
        }

        // 2. Complementa com jstat -gcutil via SSH caso credenciais de SO estejam configuradas
        AppProperties.SshProperties ssh = properties.getInfra().getApache();
        if ((ssh.getPassword() != null && !ssh.getPassword().isBlank())
                || (ssh.getPrivateKeyPath() != null && !ssh.getPrivateKeyPath().isBlank())) {
            String cmd = "PID=$(pgrep -u appuser -f \"" + processName + "\" | head -n 1); if [ -n \"$PID\" ]; then jstat -gcutil $PID 1000 2; else echo 'PID_NOT_FOUND'; fi";
            SshExecutorService.SshResult sshResult = sshExecutorService.executeNonSudoCommand(
                    ssh.getHost(), ssh.getPort(), ssh.getUser(), ssh.getPassword(), ssh.getPrivateKeyPath(), cmd, ssh.getTimeoutMs()
            );
            result.put("sshJstatSuccess", sshResult.success());
            result.put("gcOutput", sshResult.output());
        }

        if (!result.containsKey("message")) {
            result.put("status", "UNREACHABLE");
            result.put("message", "Não foi possível coletar telemetria JVM para " + processName);
        }

        return result;
    }
}
