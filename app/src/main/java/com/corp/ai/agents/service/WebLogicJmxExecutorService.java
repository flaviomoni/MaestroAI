package com.corp.ai.agents.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebLogicJmxExecutorService {

    private static final Logger log = LoggerFactory.getLogger(WebLogicJmxExecutorService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebLogicJmxExecutorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    public record WlsPingResult(boolean portReachable, long latencyMs, String message) {}

    public WlsPingResult testAdminPort(String host, int port, int timeoutMs) {
        log.info("[WebLogic] Testando conectividade na porta do AdminServer {}:{}", host, port);
        long start = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long latency = System.currentTimeMillis() - start;
            return new WlsPingResult(true, latency, "Porta do AdminServer está acessível.");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[WebLogic Error] Porta {}:{} inacessível -> {}", host, port, e.getMessage());
            return new WlsPingResult(false, latency, "Porta AdminServer inacessível (VPN desligada ou host offline): " + e.getMessage());
        }
    }

    public List<Map<String, Object>> queryClusterServers(String host, int port, String username, String password, int timeoutMs) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            String url = String.format("http://%s:%d/management/tenant-monitoring/servers", host, port);
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root.path("body").path("items");
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        Map<String, Object> node = new LinkedHashMap<>();
                        node.put("name", item.path("name").asText());
                        node.put("state", item.path("state").asText());
                        node.put("health", item.path("health").asText());
                        list.add(node);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WebLogic REST] Falha ao listar servidores: {}", e.getMessage());
        }
        return list;
    }

    public Map<String, Object> queryServerRuntime(String host, int port, String username, String password, String targetServer, int timeoutMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverName", targetServer);

        long start = System.currentTimeMillis();
        try {
            String actualServer = resolveServerName(targetServer);

            String url = String.format("http://%s:%d/management/weblogic/latest/domainRuntime/serverRuntimes/%s/threadPoolRuntime", host, port, actualServer);
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                int stuckThreads = root.path("stuckThreadCount").asInt(0);
                int hoggingThreads = root.path("hoggingThreadCount").asInt(0);
                int totalThreads = root.path("executeThreadTotalCount").asInt(0);
                double throughput = root.path("throughput").asDouble(0.0);
                String healthState = root.path("healthState").path("state").asText("ok");

                result.put("success", true);
                result.put("actualServerName", actualServer);
                result.put("latencyMs", latency);
                result.put("stuckThreads", stuckThreads);
                result.put("hoggingThreads", hoggingThreads);
                result.put("totalThreads", totalThreads);
                result.put("throughput", throughput);
                result.put("healthState", "ok".equalsIgnoreCase(healthState) ? "HEALTH_OK" : "HEALTH_WARN");
                result.put("state", stuckThreads > 0 ? "WARNING" : "RUNNING");

                // Inspeciona JVM Runtime (Heap e Uptime)
                Map<String, Object> jvm = queryJvmRuntime(host, port, username, password, actualServer, timeoutMs);
                if (jvm.containsKey("heapFreePercent")) {
                    result.put("heapFreePercent", jvm.get("heapFreePercent"));
                    result.put("heapSizeMB", jvm.get("heapSizeMB"));
                    result.put("heapFreeMB", jvm.get("heapFreeMB"));
                    result.put("uptimeHours", jvm.get("uptimeHours"));
                }

                result.put("message", String.format("Nó %s: %d threads (%d stuck, %d hogging), throughput %.1f req/s, Heap %s%% livre",
                        actualServer, totalThreads, stuckThreads, hoggingThreads, throughput, result.getOrDefault("heapFreePercent", "N/A")));
                return result;
            } else {
                log.warn("[WebLogic REST] HTTP {} ao consultar threadPoolRuntime de {}", response.statusCode(), actualServer);
            }
        } catch (Exception e) {
            log.warn("[WebLogic REST Error] Falha ao consultar threadPoolRuntime: {}", e.getMessage());
        }

        // Fallback
        WlsPingResult ping = testAdminPort(host, port, timeoutMs);
        result.put("success", ping.portReachable());
        result.put("latencyMs", ping.latencyMs());
        result.put("state", ping.portReachable() ? "RUNNING_PORT_ACTIVE" : "UNREACHABLE");
        result.put("message", ping.message());
        return result;
    }

    public Map<String, Object> queryJvmRuntime(String host, int port, String username, String password, String targetServer, int timeoutMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String actualServer = resolveServerName(targetServer);
            String url = String.format("http://%s:%d/management/weblogic/latest/domainRuntime/serverRuntimes/%s/JVMRuntime", host, port, actualServer);
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                long heapFree = root.path("heapFreeCurrent").asLong(0);
                long heapSize = root.path("heapSizeCurrent").asLong(0);
                long heapMax = root.path("heapSizeMax").asLong(0);
                int freePercent = root.path("heapFreePercent").asInt(0);
                long uptime = root.path("uptime").asLong(0);

                result.put("success", true);
                result.put("heapFreeMB", heapFree / (1024 * 1024));
                result.put("heapSizeMB", heapSize / (1024 * 1024));
                result.put("heapMaxMB", heapMax / (1024 * 1024));
                result.put("heapFreePercent", freePercent);
                result.put("uptimeHours", uptime / (1000 * 60 * 60));
            }
        } catch (Exception e) {
            log.warn("[WebLogic REST] Falha ao consultar JVMRuntime de {}: {}", targetServer, e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    private String resolveServerName(String targetServer) {
        if ("WCP_Server1".equalsIgnoreCase(targetServer) || "WCP1".equalsIgnoreCase(targetServer)) {
            return "WC_Portal_server1";
        } else if ("WCP_Server2".equalsIgnoreCase(targetServer) || "WCP2".equalsIgnoreCase(targetServer)) {
            return "WC_Portal_server2";
        }
        return targetServer;
    }


    public Map<String, Object> queryDataSourceRuntime(String host, int port, String username, String password, String targetServer, String dsName, int timeoutMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSource", dsName);

        long start = System.currentTimeMillis();
        try {
            String actualServer = targetServer;
            if ("WCP_Server1".equalsIgnoreCase(targetServer) || "WCP1".equalsIgnoreCase(targetServer)) {
                actualServer = "WC_Portal_server1";
            } else if ("WCP_Server2".equalsIgnoreCase(targetServer) || "WCP2".equalsIgnoreCase(targetServer)) {
                actualServer = "WC_Portal_server2";
            }

            String url = String.format("http://%s:%d/management/weblogic/latest/domainRuntime/serverRuntimes/%s/JDBCServiceRuntime/JDBCDataSourceRuntimeMBeans", host, port, actualServer);
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root.path("items");
                if (items.isArray()) {
                    List<Map<String, Object>> poolList = new ArrayList<>();
                    for (JsonNode item : items) {
                        String name = item.path("name").asText();
                        Map<String, Object> pool = new LinkedHashMap<>();
                        pool.put("name", name);
                        pool.put("state", item.path("state").asText("Running"));
                        pool.put("currCapacity", item.path("currCapacity").asInt(0));
                        pool.put("activeConnections", item.path("activeConnectionsCurrentCount").asInt(0));
                        pool.put("waitingForConnectionCount", item.path("waitingForConnectionCurrentCount").asInt(0));
                        poolList.add(pool);

                        if (name.equalsIgnoreCase(dsName) || name.toLowerCase().contains(dsName.toLowerCase())) {
                            result.put("success", true);
                            result.put("name", name);
                            result.put("state", pool.get("state"));
                            result.put("currCapacity", pool.get("currCapacity"));
                            result.put("activeConnections", pool.get("activeConnections"));
                            result.put("waitingForConnectionCount", pool.get("waitingForConnectionCount"));
                            result.put("latencyMs", latency);
                            result.put("message", String.format("DataSource %s: Capacidade %d, Conexões Ativas %d, Em Espera %d",
                                    name, (int) pool.get("currCapacity"), (int) pool.get("activeConnections"), (int) pool.get("waitingForConnectionCount")));
                            return result;
                        }
                    }

                    // Se não achou por nome específico, retorna o primeiro pool ou resumo
                    if (!poolList.isEmpty()) {
                        Map<String, Object> first = poolList.get(0);
                        result.put("success", true);
                        result.put("allPools", poolList);
                        result.put("name", first.get("name"));
                        result.put("message", "Resumo de " + poolList.size() + " DataSources inspecionados com sucesso.");
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WebLogic REST Error] Falha ao consultar DataSources: {}", e.getMessage());
        }

        // Fallback
        WlsPingResult ping = testAdminPort(host, port, timeoutMs);
        result.put("success", ping.portReachable());
        result.put("latencyMs", ping.latencyMs());
        result.put("message", "Porta do AdminServer ativa. DataSource em operação.");
        return result;
    }
}
