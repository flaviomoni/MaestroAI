package com.corp.ai.agents.service;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.model.InfraNodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InfraStatusService {

    private static final Logger log = LoggerFactory.getLogger(InfraStatusService.class);
    private final AppProperties properties;
    private final HttpClient httpClient;

    public InfraStatusService(AppProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1200))
                .build();
    }

    /**
     * Detecta dinamicamente se a VPN corporativa está conectada inspecionando adaptadores virtuais de rede (Fortinet, Cisco AnyConnect, etc.)
     */
    public Map<String, Object> checkVpnStatus() {
        Map<String, Object> result = new HashMap<>();

        if (properties.isSimulationMode()) {
            result.put("connected", true);
            result.put("adapter", "Modo Simulação");
            result.put("message", "VPN Corporativa: Ativa (Simulada)");
            return result;
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }

                String name = (iface.getName() + " " + iface.getDisplayName()).toLowerCase();
                if (name.contains("forti") || name.contains("anyconnect") || name.contains("cisco")
                        || name.contains("vpn") || name.contains("wireguard") || name.contains("globalprotect")) {

                    Enumeration<InetAddress> addrs = iface.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (!addr.isLoopbackAddress() && !addr.getHostAddress().contains(":")) {
                            result.put("connected", true);
                            result.put("adapter", iface.getDisplayName());
                            result.put("ip", addr.getHostAddress());
                            result.put("message", "VPN Corporativa: Conectada (" + iface.getDisplayName() + ")");
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[VPN Check] Falha ao listar adaptadores de rede: {}", e.getMessage());
        }

        result.put("connected", false);
        result.put("adapter", null);
        result.put("message", "VPN Corporativa: Desconectada");
        return result;
    }

    public List<InfraNodeStatus> getClusterSnapshot() {
        AppProperties.SshProperties apacheSsh = properties.getInfra().getApache();
        AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();
        AppProperties.LdapProperties ldap = properties.getInfra().getLdap();

        // Se estiver em modo de simulação explícito, exibe a topologia configurada em AppProperties
        if (properties.isSimulationMode()) {
            List<InfraNodeStatus> list = new ArrayList<>();
            list.add(new InfraNodeStatus("apache", "Apache HTTPD (DMZ)", "PERIMETER", "HEALTHY",
                    "Apache " + apacheSsh.getHost() + " ativo (HTTPS). Certificado válido por 18 dias", 45));
            list.add(new InfraNodeStatus("wcp1", "WebCenter Portal (Nó 1)", "MIDDLEWARE", "HEALTHY",
                    "WC_Portal_server1 (" + wls.getWcp1Host() + ":" + wls.getWcp1Port() + ") operando com 0 Stuck Threads", 55));
            list.add(new InfraNodeStatus("wcp2", "WebCenter Portal (Nó 2)", "MIDDLEWARE", "HEALTHY",
                    "WC_Portal_server2 (" + wls.getWcp2Host() + ":" + wls.getWcp2Port() + ") operando com 0 Stuck Threads", 60));
            list.add(new InfraNodeStatus("wls_admin", "WebLogic 12c Admin", "MIDDLEWARE", "HEALTHY",
                    "AdminServer (" + wls.getAdminHost() + ":" + wls.getAdminPort() + ") ativo e gerenciando domínio", 38));
            list.add(new InfraNodeStatus("ldap", "OpenLDAP Directory", "IDENTITY", "HEALTHY",
                    "OpenLDAP " + ldap.getHost() + ":" + ldap.getPort() + " - Binds autenticando normalmente", 22));
            list.add(new InfraNodeStatus("spring_apps", "Aplicações WebLogic (APP_server1)", "MIDDLEWARE", "WARNING",
                    "APP_server1 com 2 Stuck Threads e 3 Hogging Threads", 120));
            list.add(new InfraNodeStatus("oracle_wcp", "Oracle DB 1 (Portal MDS)", "DATABASE", "HEALTHY",
                    "Schema " + properties.getInfra().getOracleWcp().getUser() + " ativo (0 objetos inválidos)", 18));
            list.add(new InfraNodeStatus("oracle_apps", "Oracle DB 2 (Aplicações)", "DATABASE", "HEALTHY",
                    "Schema " + properties.getInfra().getOracleApps().getUser() + " ativo (operacional)", 35));
            return list;
        }

        // =========================================================================
        // MODO REAL: SONDAGEM DINÂMICA DE REDE E PORTAS COM IPS CONFIGURADOS
        // =========================================================================
        List<InfraNodeStatus> list = new ArrayList<>();

        // 1. Apache DMZ
        ProbeResult apacheProbe = probeTcpPort(apacheSsh.getHost(), apacheSsh.getPort(), 1200);
        list.add(new InfraNodeStatus(
                "apache", "Apache HTTPD (DMZ)", "PERIMETER",
                apacheProbe.status(),
                apacheProbe.reachable() ? "Acessível em " + apacheSsh.getHost() + ":" + apacheSsh.getPort() : "Inalcançável em " + apacheSsh.getHost() + ":" + apacheSsh.getPort() + " (VPN offline?)",
                apacheProbe.latencyMs()
        ));

        // 2. WebLogic 12c Admin
        ProbeResult wlsProbe = probeTcpPort(wls.getAdminHost(), wls.getAdminPort(), 1200);
        list.add(new InfraNodeStatus(
                "wls_admin", "WebLogic 12c Admin", "MIDDLEWARE",
                wlsProbe.status(),
                wlsProbe.reachable() ? "AdminServer ativo em " + wls.getAdminHost() + ":" + wls.getAdminPort() : "Inalcançável em " + wls.getAdminHost() + ":" + wls.getAdminPort() + " (VPN offline?)",
                wlsProbe.latencyMs()
        ));

        // 3. WebCenter Portal Nó 1
        ProbeResult wcp1Probe = probeTcpPort(wls.getWcp1Host(), wls.getWcp1Port(), 1200);
        list.add(new InfraNodeStatus(
                "wcp1", "WebCenter Portal (Nó 1)", "MIDDLEWARE",
                wcp1Probe.status(),
                wcp1Probe.reachable() ? "Nó 1 ativo em " + wls.getWcp1Host() + ":" + wls.getWcp1Port() : "Inalcançável em " + wls.getWcp1Host() + ":" + wls.getWcp1Port() + " (VPN offline?)",
                wcp1Probe.latencyMs()
        ));

        // 4. WebCenter Portal Nó 2
        ProbeResult wcp2Probe = probeTcpPort(wls.getWcp2Host(), wls.getWcp2Port(), 1200);
        list.add(new InfraNodeStatus(
                "wcp2", "WebCenter Portal (Nó 2)", "MIDDLEWARE",
                wcp2Probe.status(),
                wcp2Probe.reachable() ? "Nó 2 ativo em " + wls.getWcp2Host() + ":" + wls.getWcp2Port() : "Inalcançável em " + wls.getWcp2Host() + ":" + wls.getWcp2Port() + " (VPN offline?)",
                wcp2Probe.latencyMs()
        ));

        // 5. OpenLDAP (dinâmico via AppProperties)
        ProbeResult ldapProbe = probeTcpPort(ldap.getHost(), ldap.getPort(), 1200);
        list.add(new InfraNodeStatus(
                "ldap", "OpenLDAP Directory", "IDENTITY",
                ldapProbe.status(),
                ldapProbe.reachable() ? "LDAP ativo em " + ldap.getHost() + ":" + ldap.getPort() : "Inalcançável em " + ldap.getHost() + ":" + ldap.getPort() + " (VPN offline?)",
                ldapProbe.latencyMs()
        ));

        // 6. Java 8 Spring Apps
        ProbeResult springProbe = probeHttpUrl(properties.getInfra().getSpringActuatorUrl(), 1200);
        list.add(new InfraNodeStatus(
                "spring_apps", "Java 8 Spring Apps", "APPLICATION",
                springProbe.status(),
                springProbe.reachable() ? "Endpoint Actuator UP" : "Inalcançável em " + properties.getInfra().getSpringActuatorUrl() + " (Serviço ou VPN offline)",
                springProbe.latencyMs()
        ));

        // 7. Oracle DB 1 (WebCenter Portal)
        HostPort wcpDbHostPort = extractHostPortFromJdbcUrl(properties.getInfra().getOracleWcp().getUrl());
        ProbeResult oraWcpProbe = probeTcpPort(wcpDbHostPort.host(), wcpDbHostPort.port(), 1200);
        list.add(new InfraNodeStatus(
                "oracle_wcp", "Oracle DB 1 (Portal MDS)", "DATABASE",
                oraWcpProbe.status(),
                oraWcpProbe.reachable() ? "Listener Oracle ativo na porta " + wcpDbHostPort.port() : "Inalcançável em " + wcpDbHostPort.host() + ":" + wcpDbHostPort.port() + " (VPN offline?)",
                oraWcpProbe.latencyMs()
        ));

        // 8. Oracle DB 2 (Aplicações)
        HostPort appsDbHostPort = extractHostPortFromJdbcUrl(properties.getInfra().getOracleApps().getUrl());
        ProbeResult oraAppsProbe = probeTcpPort(appsDbHostPort.host(), appsDbHostPort.port(), 1200);
        list.add(new InfraNodeStatus(
                "oracle_apps", "Oracle DB 2 (Aplicações)", "DATABASE",
                oraAppsProbe.status(),
                oraAppsProbe.reachable() ? "Listener Oracle ativo na porta " + appsDbHostPort.port() : "Inalcançável em " + appsDbHostPort.host() + ":" + appsDbHostPort.port() + " (VPN offline?)",
                oraAppsProbe.latencyMs()
        ));

        return list;
    }

    private record ProbeResult(boolean reachable, String status, long latencyMs) {}
    private record HostPort(String host, int port) {}

    private ProbeResult probeTcpPort(String host, int port, int timeoutMs) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(true, "HEALTHY", latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, "CRITICAL", latency);
        }
    }

    private ProbeResult probeHttpUrl(String url, int timeoutMs) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latency = System.currentTimeMillis() - start;
            if (response.statusCode() < 400) {
                return new ProbeResult(true, "HEALTHY", latency);
            } else {
                return new ProbeResult(false, "WARNING", latency);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return new ProbeResult(false, "CRITICAL", latency);
        }
    }

    private HostPort extractHostPortFromJdbcUrl(String jdbcUrl) {
        try {
            if (jdbcUrl != null && jdbcUrl.contains("@")) {
                String afterAt = jdbcUrl.substring(jdbcUrl.indexOf('@') + 1);
                while (afterAt.startsWith("/")) {
                    afterAt = afterAt.substring(1);
                }
                String hostPart = afterAt.split("[/:;]")[0];
                int port = 1521;
                if (afterAt.contains(":")) {
                    String portPart = afterAt.split(":")[1].split("[/:;]")[0];
                    port = Integer.parseInt(portPart);
                }
                return new HostPort(hostPart, port);
            }
        } catch (Exception ignored) {}
        return new HostPort("127.0.0.1", 1521);
    }
}
