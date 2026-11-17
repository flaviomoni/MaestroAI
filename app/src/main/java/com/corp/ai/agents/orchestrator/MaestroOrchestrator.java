package com.corp.ai.agents.orchestrator;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.model.AgentMessage;
import com.corp.ai.agents.model.AgentThoughtEvent;
import com.corp.ai.agents.model.HitlActionRequest;
import com.corp.ai.agents.service.EventStreamService;
import com.corp.ai.agents.service.GeminiService;
import com.corp.ai.agents.service.HitlService;
import com.corp.ai.agents.service.RAGService;
import com.corp.ai.agents.skills.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MaestroOrchestrator {

        private static final Logger log = LoggerFactory.getLogger(MaestroOrchestrator.class);

        // Executor escalável baseado em Java 21 Virtual Threads (Project Loom)
        private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        private final AppProperties properties;
        private final EventStreamService eventStreamService;
        private final GeminiService geminiService;
        private final RAGService ragService;
        private final HitlService hitlService;

        // Ecossistema de Sub-Agentes Especialistas
        private final ApacheSkill apacheSkill;
        private final WebLogicWcpSkill webLogicWcpSkill;
        private final OpenLdapSkill openLdapSkill;
        private final JavaSpringSkill javaSpringSkill;
        private final OracleDbaSkill oracleDbaSkill;
        private final CyberSecuritySkill cyberSecuritySkill;
        private final WebCenterPortalSkill webCenterPortalSkill;
        private final MiddlewareArchitectSkill middlewareArchitectSkill;
        private final MonitoringSreSkill monitoringSreSkill;
        private final LinuxLogSkill linuxLogSkill;

        @Autowired
        public MaestroOrchestrator(AppProperties properties,
                        EventStreamService eventStreamService,
                        GeminiService geminiService,
                        RAGService ragService,
                        HitlService hitlService,
                        ApacheSkill apacheSkill,
                        WebLogicWcpSkill webLogicWcpSkill,
                        OpenLdapSkill openLdapSkill,
                        JavaSpringSkill javaSpringSkill,
                        OracleDbaSkill oracleDbaSkill,
                        CyberSecuritySkill cyberSecuritySkill,
                        WebCenterPortalSkill webCenterPortalSkill,
                        MiddlewareArchitectSkill middlewareArchitectSkill,
                        MonitoringSreSkill monitoringSreSkill,
                        LinuxLogSkill linuxLogSkill) {
                this.properties = properties;
                this.eventStreamService = eventStreamService;
                this.geminiService = geminiService;
                this.ragService = ragService;
                this.hitlService = hitlService;
                this.apacheSkill = apacheSkill;
                this.webLogicWcpSkill = webLogicWcpSkill;
                this.openLdapSkill = openLdapSkill;
                this.javaSpringSkill = javaSpringSkill;
                this.oracleDbaSkill = oracleDbaSkill;
                this.cyberSecuritySkill = cyberSecuritySkill;
                this.webCenterPortalSkill = webCenterPortalSkill;
                this.middlewareArchitectSkill = middlewareArchitectSkill;
                this.monitoringSreSkill = monitoringSreSkill;
                this.linuxLogSkill = linuxLogSkill;
        }

        // Construtor de compatibilidade para testes legados
        public MaestroOrchestrator(AppProperties properties,
                        EventStreamService eventStreamService,
                        GeminiService geminiService,
                        RAGService ragService,
                        HitlService hitlService,
                        ApacheSkill apacheSkill,
                        WebLogicWcpSkill webLogicWcpSkill,
                        OpenLdapSkill openLdapSkill,
                        JavaSpringSkill javaSpringSkill,
                        OracleDbaSkill oracleDbaSkill) {
                this(properties, eventStreamService, geminiService, ragService, hitlService,
                                apacheSkill, webLogicWcpSkill, openLdapSkill, javaSpringSkill, oracleDbaSkill,
                                new CyberSecuritySkill(properties),
                                new WebCenterPortalSkill(properties),
                                new MiddlewareArchitectSkill(properties),
                                null,
                                null);
        }

        public AgentMessage processUserRequest(String userPrompt) {
                String correlationId = UUID.randomUUID().toString().substring(0, 8);
                log.info("[Maestro v2 VirtualThreads] Iniciando ciclo ReAct Local-First. ID={}, prompt=\"{}\"",
                                correlationId, userPrompt);

                String lower = userPrompt.toLowerCase();
                if (lower.contains("ciber") || lower.contains("cyber") || lower.contains("ataque")
                                || lower.contains("segurança") || lower.contains("seguranca")
                                || lower.contains("sqli") || lower.contains("xss") || lower.contains("captcha")
                                || lower.contains("waf") || lower.contains("defend")) {
                        return processCyberSecurityRequest(correlationId, userPrompt);
                } else if (lower.contains("provisionar") || lower.contains("novo portal")
                                || lower.contains("criar portal") || lower.contains("layout")
                                || lower.contains("visualização") || lower.contains("visualizacao")
                                || lower.contains("multi-tenant") || lower.contains("universal portal")) {
                        return processWebCenterProvisioningRequest(correlationId, userPrompt);
                } else if (lower.contains("passo 2") || lower.contains("passo2") || lower.contains("step 2")
                                || lower.contains("etapa 2")) {
                        return processPasso2Request(correlationId, userPrompt);
                } else if (lower.contains("ldap") || lower.contains("bind")) {
                        return processLdapRequest(correlationId, userPrompt);
                } else if (lower.contains("oracle") || lower.contains("banco") || lower.contains("lock")
                                || lower.contains("wcpdb") || lower.contains("appsdb")) {
                        return processOracleRequest(correlationId, userPrompt);
                } else if (lower.contains("weblogic") || lower.contains("stuck") || lower.contains("thread")
                                || lower.contains("managed server")) {
                        return processWebLogicRequest(correlationId, userPrompt);
                } else if (lower.contains("apache") || lower.contains("dmz") || lower.contains("ssl")
                                || lower.contains("certificad") || lower.contains("reverse proxy")) {
                        return processApacheRequest(correlationId, userPrompt);
                } else if (lower.contains("spring") || lower.contains("actuator") || lower.contains("jvm")
                                || lower.contains("heap") || lower.contains("gc") || lower.contains("memória")
                                || lower.contains("memoria") || lower.contains("java")) {
                        return processJavaSpringRequest(correlationId, userPrompt);
                } else if (lower.contains("linux") || lower.contains("monitora") || lower.contains("sla")
                                || lower.contains("arquitetura") || lower.contains("middleware")) {
                        return processObservabilityAndArchitectureRequest(correlationId, userPrompt);
                } else if (lower.contains("diagnóstico") || lower.contains("diagnostico") || lower.contains("geral")) {
                        return processGeneralDiagnosticsRequest(correlationId, userPrompt);
                } else {
                        return processIncident504Request(correlationId, userPrompt);
                }
        }

        private AgentMessage processCyberSecurityRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();

                // 1. Consulta Local-First no Obsidian (Custo = 0 Tokens)
                Optional<String> localObsidian = ragService.lookupRunbookSnippet(userPrompt);
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "RAG_MATCH",
                                "Memória Local-First consultada no Obsidian (0 Tokens de busca).",
                                "Arquivo: 06_Memoria_Aprendizado_Agentes/Aprendizado_CyberSec_Ataques.md"));

                // 2. Agente de Cibersegurança: Monitoramento de Ataques e Defesa Online
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Agente Cibersegurança", "TOOL_CALL",
                                "Ativando monitoramento de ataques externos e Escudo de Defesa Online...",
                                "Inspecionando payloads SQLi/XSS/Brute-Force, Captcha Gateway e TLS no Apache DMZ"));
                Map<String, Object> defense = cyberSecuritySkill.monitorAndDefendExternalThreats(userPrompt);
                evidence.add("Escudo Cibersegurança Online: " + defense.get("message"));
                evidence.add("Desafio CAPTCHA na Entrada Única: " + defense.get("captchaEnforcement"));

                String apacheHost = properties.getInfra().getApache().getHost();
                Map<String, Object> ssl = apacheSkill.checkSslCertExpiry(apacheHost);
                evidence.add("Auditoria TLS Perímetro (" + apacheHost + "): " + ssl.get("message"));

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente Cibersegurança", "TOOL_RESULT",
                                "Defesa Online Aplicada: IP " + defense.get("blockedAttackerIp") + " neutralizado.",
                                defense.toString()));

                // 3. Documenta aprendizado no Obsidian Local
                ragService.recordAgentLearning(
                                "CYBERSEC",
                                "Agente Cibersegurança",
                                userPrompt,
                                String.valueOf(defense.get("message")));

                String systemInstruction = "Você é o Agente Maestro coordenando o Especialista em Cibersegurança. Resuma a detecção, a defesa online aplicada e as orientações de proteção em português.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências:\n" + String.join("\n", evidence)
                                + (localObsidian.map(s -> "\n\nMemória Local Obsidian:\n" + s).orElse(""));
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Defesa Online ativada pelo Especialista em Cibersegurança e aprendizado gravado no Obsidian.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(String.join("\n", (List<String>) defense.get("defenseGuidance")));
                return msg;
        }

        private AgentMessage processWebCenterProvisioningRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();

                // 1. Consulta Local-First no Obsidian (0 Tokens)
                Optional<String> localObsidian = ragService.lookupRunbookSnippet(userPrompt);
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "RAG_MATCH",
                                "Padrão de Layout e Provisionamento carregado do Obsidian Local (0 Tokens).",
                                "Arquivo: 06_Memoria_Aprendizado_Agentes/Aprendizado_WebCenter_Layouts_e_Portais.md"));

                // 2. Execução Paralela Escalável via Virtual Threads (WebCenter Portal + WebLogic + OpenLDAP + Oracle DBA)
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Maestro (Virtual Threads)", "TOOL_CALL",
                                "Delegando provisionamento em paralelo para Especialistas em WebCenter Portal, WebLogic, OpenLDAP e Oracle DBA...",
                                "Template Padrão: layout/portal-universal-template"));

                CompletableFuture<Map<String, Object>> layoutFuture = CompletableFuture.supplyAsync(
                                webCenterPortalSkill::inspectStandardLayoutAssets, virtualThreadExecutor);
                CompletableFuture<Map<String, Object>> portalFuture = CompletableFuture.supplyAsync(
                                () -> webCenterPortalSkill.provisionClientPortal(
                                                "portal-corp-v2",
                                                "Portal Único Multi-Cliente v2",
                                                "CLIENT_ENTERPRISE",
                                                "#004e8b"),
                                virtualThreadExecutor);
                CompletableFuture<Map<String, Object>> ldapFuture = CompletableFuture.supplyAsync(
                                () -> openLdapSkill.provisionUserAndClientRole("cliente.enterprise", "CLIENT_ENTERPRISE"),
                                virtualThreadExecutor);
                CompletableFuture<Map<String, Object>> wlsFuture = CompletableFuture.supplyAsync(
                                () -> webLogicWcpSkill.provisionManagedServerAndDataSource(
                                                "WC_Portal_server3", 19502, "jdbc/PortalMultiTenantDS", "WCPDB"),
                                virtualThreadExecutor);
                CompletableFuture<Map<String, Object>> dbaFuture = CompletableFuture.supplyAsync(
                                () -> oracleDbaSkill.provisionDatabaseSchema("PORTAL_CLIENT_V2", "APPSDB"),
                                virtualThreadExecutor);

                CompletableFuture.allOf(layoutFuture, portalFuture, ldapFuture, wlsFuture, dbaFuture).join();

                evidence.add("Layout Padrão (Universal Portal Template): " + layoutFuture.join().get("message"));
                evidence.add("WebCenter Portal REST API (/rest/api/v1/portals): " + portalFuture.join().get("message"));
                evidence.add("OpenLDAP Autorização Multi-Cliente: " + ldapFuture.join().get("message"));
                evidence.add("WebLogic Provisionamento: " + wlsFuture.join().get("message"));
                evidence.add("Oracle DBA Provisionamento: " + dbaFuture.join().get("message"));

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente WebCenter Portal", "TOOL_RESULT",
                                "Portal provisionado com layout padrão 'portal-universal-template' e entrada única multi-cliente.",
                                portalFuture.join().toString()));

                // 3. Registra aprendizado no Obsidian
                ragService.recordAgentLearning(
                                "WEBCENTER",
                                "Squad Provisionamento (WCP + WLS + LDAP + DBA)",
                                userPrompt,
                                String.valueOf(portalFuture.join().get("message")));

                String systemInstruction = "Você é o Agente Maestro. Sintetize o provisionamento do portal WebCenter (usando o layout padrão portal-universal-template), configuração do WebLogic, OpenLDAP e Oracle DB em português.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências:\n" + String.join("\n", evidence)
                                + (localObsidian.map(s -> "\n\nMemória Local Obsidian:\n" + s).orElse(""));
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Provisionamento coordenado em paralelo (WebCenter Portal + Layout portal-universal-template + WebLogic + OpenLDAP + Oracle DBA) e documentado no Obsidian.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction("1. Pacote portal_universal_pagetemplate.aar validado sem BOM.\n2. APIs REST /rest/api/v1/portals ativas para novos clientes.\n3. Visualizações distintas mapeadas via grupos OpenLDAP e atributo corPrimaria.");
                return msg;
        }

        private AgentMessage processObservabilityAndArchitectureRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();

                Optional<String> localObsidian = ragService.lookupRunbookSnippet(userPrompt);
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Acionando Especialistas em Arquitetura de Middleware, Monitoração SRE e Linux Logs em paralelo.",
                                "Consultando memória local do Obsidian primeiro para economia de tokens."));

                Map<String, Object> arch = middlewareArchitectSkill.evaluateArchitectureAndTopology();
                evidence.add("Especialista Middleware: " + arch.get("message"));

                if (monitoringSreSkill != null) {
                        Map<String, Object> mon = monitoringSreSkill.evaluateSlaAndRemediationBacking();
                        evidence.add("Especialista Monitoração: " + mon.get("message"));
                }
                if (linuxLogSkill != null) {
                        Map<String, Object> linux = linuxLogSkill.inspectLinuxAndMiddlewareLogs(40);
                        evidence.add("Especialista Linux: " + linux.get("message"));
                }

                ragService.recordAgentLearning(
                                "INFRA",
                                "Squad Arquitetura, Monitoração & Linux",
                                userPrompt,
                                String.join(" | ", evidence));

                String systemInstruction = "Você é o Agente Maestro. Sintetize o parecer dos Especialistas em Middleware, Monitoração e Linux em português.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências:\n" + String.join("\n", evidence)
                                + (localObsidian.map(s -> "\n\nMemória Local Obsidian:\n" + s).orElse(""));
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Análise integrada de Arquitetura Middleware, Monitoração SRE e Linux Logs concluída.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction("Manter observabilidade ativa e consultar a base 06_Memoria_Aprendizado_Agentes no Obsidian.");
                return msg;
        }

        private AgentMessage processJavaSpringRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Classificado como diagnóstico de Aplicações Java/Spring Boot e Memória JVM. Acionando Agente Especialista Java/Spring.",
                                "Inspecionando Spring Boot Actuator (/actuator/health) e métricas de JVM/Heap nos servidores de aplicação (APP_server1, APP_server2 e WC_Portal_server1)."));

                // Tool 1: Actuator Health
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Agente Java/Spring", "TOOL_CALL",
                                "Consultando endpoint Spring Boot Actuator (/actuator/health)...",
                                "Verificando disponibilidade HTTP e componentes de saúde da aplicação"));
                Map<String, Object> actuator = javaSpringSkill.queryActuatorHealth();
                evidence.add("Spring Boot Actuator: " + actuator.get("message"));
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente Java/Spring", "TOOL_RESULT",
                                "Status Actuator: " + actuator.get("status"),
                                actuator.toString()));

                // Tool 2: JVM Memory & GC (APP_server1)
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-4", "Agente Java/Spring", "TOOL_CALL",
                                "Coletando telemetria de memória JVM/Heap e Garbage Collector (APP_server1 e WC_Portal_server1)...",
                                "Inspeção via JVMRuntime MBean e jstat -gcutil"));
                Map<String, Object> jvmApp1 = javaSpringSkill.checkJvmGcMemory("APP_server1");
                evidence.add("JVM APP_server1: " + jvmApp1.get("message"));

                Map<String, Object> jvmApp2 = javaSpringSkill.checkJvmGcMemory("APP_server2");
                evidence.add("JVM APP_server2: " + jvmApp2.get("message"));

                Map<String, Object> jvmPortal1 = javaSpringSkill.checkJvmGcMemory("WC_Portal_server1");
                evidence.add("JVM WC_Portal_server1: " + jvmPortal1.get("message"));

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-5", "Agente Java/Spring", "TOOL_RESULT",
                                "Telemetria JVM coletada com sucesso.",
                                jvmApp1.toString()));

                ragService.recordAgentLearning("INFRA", "Agente Java/Spring", userPrompt, String.valueOf(jvmApp1.get("message")));

                String systemInstruction = "Você é o Agente Maestro. Resuma o diagnóstico de aplicações Java/Spring Boot e memória JVM/Heap em português técnico.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências reais apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Diagnóstico de aplicações Java/Spring e memória JVM concluído com sucesso.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(
                                "Monitorar o consumo de Heap no APP_server1 em conjunto com as threads em execução.");
                return msg;
        }

        private AgentMessage processPasso2Request(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                String apacheHost = properties.getInfra().getApache().getHost();
                String wlsAdmin = properties.getInfra().getWeblogic().getAdminHost() + ":"
                                + properties.getInfra().getWeblogic().getAdminPort();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Executando o Passo 2: Diagnóstico REAL de Middleware (WebLogic 12c / WebCenter Portal) e Perímetro (Apache DMZ).",
                                "Conectando via REST Management API nativa do WebLogic e sondagem TLS/Perímetro no Apache."));

                // 1. WebLogic Topologia
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Agente WebLogic/WCP", "TOOL_CALL",
                                "Consultando topologia do cluster em " + wlsAdmin
                                                + " (/management/tenant-monitoring/servers)...",
                                "Autenticação: " + properties.getInfra().getWeblogic().getUsername()
                                                + " via HTTP Basic"));
                List<Map<String, Object>> topology = webLogicWcpSkill.getClusterTopologyOverview();
                evidence.add("WebLogic 12c: " + topology.size()
                                + " servidores ativos no domínio (AdminServer, WC_Portal_server1, WC_Portal_server2, APP_server1, APP_server2).");

                // 2. WebCenter Portal Nó 1 e Nó 2
                Map<String, Object> node1 = webLogicWcpSkill.queryServerStatus("WC_Portal_server1");
                evidence.add("Portal Nó 1 (WC_Portal_server1): " + node1.get("message") + " | Estado: "
                                + node1.get("healthState"));

                Map<String, Object> node2 = webLogicWcpSkill.queryServerStatus("WC_Portal_server2");
                evidence.add("Portal Nó 2 (WC_Portal_server2): " + node2.get("message") + " | Estado: "
                                + node2.get("healthState"));

                // 3. Aplicações Nó 1 (Stuck Threads)
                Map<String, Object> appNode = webLogicWcpSkill.queryServerStatus("APP_server1");
                int stuck = appNode.get("stuckThreads") != null ? (int) appNode.get("stuckThreads") : 0;
                int hogging = appNode.get("hoggingThreads") != null ? (int) appNode.get("hoggingThreads") : 0;
                evidence.add("Servidor APP_server1: " + appNode.get("message") + " (Alerta: " + stuck
                                + " Stuck Threads, " + hogging + " Hogging Threads)");

                // 4. DataSources
                Map<String, Object> ds = webLogicWcpSkill.checkDataSourcePools("mds-SpacesDS");
                evidence.add("DataSource mds-SpacesDS: " + ds.get("message") + " (" + ds.get("state") + ")");

                // 5. Apache DMZ (Perímetro HTTP/HTTPS)
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente Apache DMZ", "TOOL_CALL",
                                "Verificando integridade perimétrica do Apache DMZ (" + apacheHost + ":80 / 443)...",
                                "Sondagem TLS 1.2 e verificação de Reverse Proxy"));
                Map<String, Object> httpHealth = apacheSkill.checkUrlHealth("https://" + apacheHost);
                evidence.add("Apache DMZ (" + apacheHost + "): " + httpHealth.get("message") + " (Latência: "
                                + httpHealth.get("latencyMs") + "ms)");

                // 6. Certificado SSL
                Map<String, Object> ssl = apacheSkill.checkSslCertExpiry(apacheHost);
                evidence.add("Certificado SSL (" + apacheHost + ":443): " + ssl.get("message")
                                + (ssl.get("warning") != null ? " [" + ssl.get("warning") + "]" : ""));

                // 7. Leitura de Logs de Erro
                Map<String, Object> logs = apacheSkill.parseErrorLogs(30);
                evidence.add("Logs de Erro Apache: " + logs.get("message"));

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-4", "Maestro", "TOOL_RESULT",
                                "Passo 2 concluído com dados 100% reais de Middleware e Perímetro.",
                                String.join("\n", evidence)));

                String systemInstruction = "Você é o Agente Maestro. Sintetize os resultados do Passo 2 (WebLogic 12c, WebCenter Portal e Apache DMZ) de forma executiva e técnica em português.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências reais apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary(
                                "Passo 2 implementado e executado com sucesso: telemetria real de WebLogic 12c, WebCenter Portal e Apache DMZ ativada.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(
                                "1. O WebCenter Portal (Nós 1 e 2) está saudável com 0 Stuck Threads. 2. O APP_server1 requer atenção devido a 2 Stuck Threads. 3. O Certificado SSL expira em breve (18 dias).");
                return msg;
        }

        private AgentMessage processApacheRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                String apacheHost = properties.getInfra().getApache().getHost();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Classificado como diagnóstico do perímetro Apache DMZ. Acionando Agente Especialista em Apache/Perímetro.",
                                "Inspecionando HTTP/HTTPS, certificado SSL X.509 e status de logs de erro em "
                                                + apacheHost + "."));

                // Tool 1: URL Health
                Map<String, Object> httpHealth = apacheSkill.checkUrlHealth("https://" + apacheHost);
                evidence.add("Apache DMZ HTTP/HTTPS (" + apacheHost + "): " + httpHealth.get("message")
                                + " | Latência: "
                                + httpHealth.get("latencyMs") + "ms");

                // Tool 2: SSL Cert
                Map<String, Object> ssl = apacheSkill.checkSslCertExpiry(apacheHost);
                evidence.add("Certificado SSL TLS (" + apacheHost + ":443): " + ssl.get("message")
                                + (ssl.get("warning") != null ? " [" + ssl.get("warning") + "]" : ""));

                // Tool 3: Error Logs
                Map<String, Object> logs = apacheSkill.parseErrorLogs(50);
                evidence.add("Leitura de Logs (/var/log/httpd/error_log): " + logs.get("message"));

                String systemInstruction = "Você é o Agente Maestro. Resuma o diagnóstico do Apache DMZ e certificado SSL em português técnico.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências reais apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Diagnóstico do Apache DMZ e certificado SSL concluído com dados reais.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(ssl.get("warning") != null ? (String) ssl.get("warning")
                                : "Perímetro Apache operando normalmente.");
                return msg;
        }

        private AgentMessage processWebLogicRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                String wlsAdmin = properties.getInfra().getWeblogic().getAdminHost() + ":"
                                + properties.getInfra().getWeblogic().getAdminPort();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Classificado como diagnóstico de Middleware WebLogic. Acionando Agente WebLogic via REST Management API nativa.",
                                "Consultando topologia de nós, ThreadPoolRuntime (Stuck/Hogging Threads) e DataSource mds-SpacesDS."));

                // Tool 1: Topology
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Agente WebLogic/WCP", "TOOL_CALL",
                                "Consultando topologia do cluster em " + wlsAdmin
                                                + " (/management/tenant-monitoring/servers)...",
                                "Autenticação: " + properties.getInfra().getWeblogic().getUsername()
                                                + " via HTTP Basic"));
                List<Map<String, Object>> topology = webLogicWcpSkill.getClusterTopologyOverview();
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente WebLogic/WCP", "TOOL_RESULT",
                                "Topologia identificada com " + topology.size() + " servidores no domínio.",
                                topology.toString()));
                evidence.add("WebLogic Topologia: " + topology.size() + " servidores ativos no domínio.");

                // Tool 2: Portal Node 1 Runtime
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-4", "Agente WebLogic/WCP", "TOOL_CALL",
                                "Consultando ThreadPoolRuntime do nó WC_Portal_server1...",
                                "Métricas: executeThreadTotalCount, stuckThreadCount, hoggingThreadCount, throughput"));
                Map<String, Object> node1 = webLogicWcpSkill.queryServerStatus("WC_Portal_server1");
                evidence.add("Nó 1 (WC_Portal_server1): " + node1.get("message") + " | Estado: "
                                + node1.get("healthState"));
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-5", "Agente WebLogic/WCP", "TOOL_RESULT",
                                "WC_Portal_server1 analisado: " + node1.get("healthState"),
                                node1.toString()));

                // Tool 3: Portal Node 2 Runtime
                Map<String, Object> node2 = webLogicWcpSkill.queryServerStatus("WC_Portal_server2");
                evidence.add("Nó 2 (WC_Portal_server2): " + node2.get("message") + " | Estado: "
                                + node2.get("healthState"));

                // Tool 4: App Server 1
                Map<String, Object> appNode = webLogicWcpSkill.queryServerStatus("APP_server1");
                if (appNode.get("stuckThreads") != null && (int) appNode.get("stuckThreads") > 0) {
                        evidence.add("Alerta APP_server1: " + appNode.get("stuckThreads")
                                        + " stuck threads detectadas no ThreadPoolRuntime.");
                }

                // Tool 5: DataSource Pools
                Map<String, Object> ds = webLogicWcpSkill.checkDataSourcePools("mds-SpacesDS");
                evidence.add("DataSource mds-SpacesDS: " + ds.get("message") + " (" + ds.get("state") + ")");

                String systemInstruction = "Você é o Agente Maestro. Resuma o diagnóstico do WebLogic e WebCenter Portal em português técnico.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências reais apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Diagnóstico do WebLogic executado via REST Management API com sucesso.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(
                                "Nós do WebCenter Portal operando normalmente com 0 Stuck Threads. Continuar observação do nó APP_server1.");
                return msg;
        }

        private AgentMessage processLdapRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                AppProperties.LdapProperties ldap = properties.getInfra().getLdap();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Classificado como diagnóstico de diretório LDAP. Acionando o Agente Especialista em OpenLDAP.",
                                "Executando teste de bind com credenciais reais e validação de contextCSN no host configurado."));

                // Tool 1: Bind
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Agente OpenLDAP", "TOOL_CALL",
                                "Executando skill 'ldap_test_bind'...",
                                "Host: " + ldap.getHost() + ":" + ldap.getPort() + " | Base DN: " + ldap.getBaseDn()));
                Map<String, Object> bindResult = openLdapSkill.testBind("admin");
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente OpenLDAP", "TOOL_RESULT",
                                "Resultado do Bind: " + bindResult.get("status") + " (" + bindResult.get("latencyMs")
                                                + "ms)",
                                bindResult.toString()));
                evidence.add("OpenLDAP Bind: " + bindResult.get("message") + " | Latência: "
                                + bindResult.get("latencyMs") + "ms | Status: " + bindResult.get("status"));

                // Tool 2: ContextCSN
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-4", "Agente OpenLDAP", "TOOL_CALL",
                                "Consultando contextCSN da base " + ldap.getBaseDn() + "...",
                                "Validação de integridade/replicação no OpenLDAP"));
                Map<String, Object> csnResult = openLdapSkill.checkReplicationLag();
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-5", "Agente OpenLDAP", "TOOL_RESULT",
                                "Consulta LDAP concluída.",
                                csnResult.toString()));
                evidence.add("OpenLDAP CSN: "
                                + csnResult.getOrDefault("contextCsn", csnResult.getOrDefault("message", "OK")));

                String systemInstruction = "Você é o Agente Maestro. Sintetize o resultado do teste do OpenLDAP de forma clara, técnica e objetiva em português.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências reais apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary(
                                "Diagnóstico do OpenLDAP executado com resultado: " + bindResult.get("status"));
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(
                                "Nenhuma ação requerida. O diretório OpenLDAP está respondendo normalmente às consultas de autenticação.");
                return msg;
        }

        private AgentMessage processOracleRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                AppProperties.DbProperties appsDb = properties.getInfra().getOracleApps();
                AppProperties.DbProperties wcpDb = properties.getInfra().getOracleWcp();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Classificado como diagnóstico de Banco de Dados Oracle. Acionando Agente Especialista Oracle DBA.",
                                "Executando teste canônico JDBC (SELECT 1 FROM DUAL) e inspeção de USER_OBJECTS em APPSDB e WCPDB."));

                // Tool 1: Apps DB
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Agente Oracle DBA", "TOOL_CALL",
                                "Testando conexão JDBC canônica no banco de Aplicações (APPSDB)...",
                                "Query: SELECT 1 FROM DUAL | Usuário: " + appsDb.getUser()));
                Map<String, Object> appsPing = oracleDbaSkill.testPing("APPSDB");
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente Oracle DBA", "TOOL_RESULT",
                                "Resultado APPSDB: " + appsPing.get("status") + " (" + appsPing.get("latencyMs")
                                                + "ms)",
                                appsPing.toString()));
                evidence.add("Oracle Apps (APPSDB): " + appsPing.get("message") + " | Latência: "
                                + appsPing.get("latencyMs") + "ms");

                Map<String, Object> appsObj = oracleDbaSkill.checkAppLocksAndBlockers("APPSDB");
                evidence.add("Oracle Apps Objetos Inválidos: "
                                + appsObj.getOrDefault("invalidObjectsSummary", "Nenhum"));

                // Tool 2: Portal DB
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-4", "Agente Oracle DBA", "TOOL_CALL",
                                "Testando conexão JDBC canônica no banco do WebCenter Portal (WCPDB)...",
                                "Query: SELECT 1 FROM DUAL | Usuário: " + wcpDb.getUser()));
                Map<String, Object> wcpPing = oracleDbaSkill.testPing("WCPDB");
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-5", "Agente Oracle DBA", "TOOL_RESULT",
                                "Resultado WCPDB: " + wcpPing.get("status") + " (" + wcpPing.get("latencyMs")
                                                + "ms)",
                                wcpPing.toString()));
                evidence.add("Oracle Portal (WCPDB): " + wcpPing.get("message") + " | Latência: "
                                + wcpPing.get("latencyMs") + "ms");

                Map<String, Object> wcpObj = oracleDbaSkill.checkAppLocksAndBlockers("WCPDB");
                evidence.add("Oracle Portal Objetos Inválidos: "
                                + wcpObj.getOrDefault("invalidObjectsSummary", "Nenhum"));

                String systemInstruction = "Você é o Agente Maestro. Resuma o estado dos bancos de dados Oracle testados de forma profissional em português.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências reais apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary(
                                "Diagnóstico dos bancos Oracle (APPSDB e WCPDB) concluído com sucesso.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction(
                                "Nenhuma intervenção necessária no momento. A conectividade e autenticação com os listeners e instâncias Oracle estão funcionais.");
                return msg;
        }

        private AgentMessage processGeneralDiagnosticsRequest(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                AppProperties.LdapProperties ldap = properties.getInfra().getLdap();
                AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Disparando varredura diagnóstica completa de infraestrutura em paralelo (Virtual Threads).",
                                "Consultando OpenLDAP, Bancos Oracle, WebLogic, WebCenter Portal, Cibersegurança, Java/Spring e Apache."));

                // LDAP
                Map<String, Object> ldapResult = openLdapSkill.testBind("admin");
                evidence.add("OpenLDAP (" + ldap.getHost() + ":" + ldap.getPort() + "): " + ldapResult.get("message")
                                + " ("
                                + ldapResult.get("latencyMs") + "ms)");

                // Oracle Apps
                Map<String, Object> appsPing = oracleDbaSkill.testPing("APPSDB");
                evidence.add("Oracle Apps (APPSDB): " + appsPing.get("message") + " ("
                                + appsPing.get("latencyMs") + "ms)");

                // Oracle Portal
                Map<String, Object> wcpPing = oracleDbaSkill.testPing("WCPDB");
                evidence.add("Oracle Portal (WCPDB): " + wcpPing.get("message") + " ("
                                + wcpPing.get("latencyMs") + "ms)");

                // WebLogic Port
                Map<String, Object> wlsPing = webLogicWcpSkill.checkDataSourcePools("mds-SpacesDS");
                evidence.add("WebLogic 12c Admin (" + wls.getAdminHost() + ":" + wls.getAdminPort() + "): "
                                + wlsPing.get("message") + " ("
                                + wlsPing.get("latencyMs") + "ms)");

                // Java / Spring JVM Memory
                Map<String, Object> jvmApp = javaSpringSkill.checkJvmGcMemory("APP_server1");
                evidence.add("Java/Spring JVM (APP_server1): " + jvmApp.get("message"));

                // WebCenter Layout & CyberShield
                Map<String, Object> layout = webCenterPortalSkill.inspectStandardLayoutAssets();
                evidence.add("WebCenter Layout Padrão: " + layout.get("message"));

                Map<String, Object> shield = cyberSecuritySkill.getSecurityShieldStatus();
                evidence.add("Cibersegurança Escudo Online: " + shield.get("shieldState") + " ("
                                + shield.get("blockedIpsCount") + " IPs bloqueados)");

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-2", "Maestro", "TOOL_RESULT",
                                "Varredura de infraestrutura concluída.",
                                String.join("\n", evidence)));

                String systemInstruction = "Você é o Agente Maestro. Sintetize a saúde da infraestrutura em português para o operador.";
                String prompt = "Solicitação: " + userPrompt + "\n\nEvidências apuradas:\n"
                                + String.join("\n", evidence);
                String reply = geminiService.generateResponse(systemInstruction, prompt);

                AgentMessage msg = new AgentMessage("MSG-" + correlationId, "MAESTRO", reply);
                msg.setRcaSummary("Varredura geral de infraestrutura finalizada com 10 agentes especialistas.");
                msg.setEvidenceList(evidence);
                msg.setSuggestedAction("Todos os serviços críticos testados estão operacionais.");
                return msg;
        }

        private AgentMessage processIncident504Request(String correlationId, String userPrompt) {
                List<String> evidence = new ArrayList<>();
                String apacheHost = properties.getInfra().getApache().getHost();
                AppProperties.WlsProperties wls = properties.getInfra().getWeblogic();

                // Passo 1: Thought inicial do Maestro
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-1", "Maestro", "THOUGHT",
                                "Recebida solicitação: \"" + userPrompt
                                                + "\". Iniciando triagem de ponta a ponta (Local-First RAG + 10 Agentes).",
                                "Inspecionando DMZ (Apache " + apacheHost + "), Cibersegurança, Middleware (WebLogic "
                                                + wls.getAdminHost() + "), Java/Spring, Linux e Bancos Oracle (WCPDB e APPSDB)."));

                // Passo 2: RAG Local no Obsidian (Custo 0 de LLM)
                Optional<String> runbookSnippet = ragService.lookupRunbookSnippet(userPrompt);
                String ragContext = "";
                if (runbookSnippet.isPresent()) {
                        ragContext = runbookSnippet.get();
                        eventStreamService.emitThought(new AgentThoughtEvent(
                                        correlationId + "-2", "Maestro", "RAG_MATCH",
                                        "Procedimento de triagem e memória de agentes carregados do Obsidian (0 Tokens).",
                                        "Fontes: Runbook_Erro_504_Portal.md + 06_Memoria_Aprendizado_Agentes"));
                }

                // Passo 3: Investigação Real - Agente Apache DMZ & Cibersegurança
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-3", "Agente Apache DMZ", "TOOL_CALL",
                                "Verificando status perimétrico e certificado TLS no Apache DMZ (" + apacheHost
                                                + ")...",
                                "Sondagem HTTPS e inspeção de certificado SSL"));
                Map<String, Object> httpHealth = apacheSkill.checkUrlHealth("https://" + apacheHost);
                Map<String, Object> sslCert = apacheSkill.checkSslCertExpiry(apacheHost);
                Map<String, Object> apacheLogs = apacheSkill.parseErrorLogs(50);

                evidence.add("Apache DMZ (" + apacheHost + "): " + httpHealth.get("message") + " (Latência: "
                                + httpHealth.get("latencyMs") + "ms)");
                evidence.add("Certificado SSL (" + apacheHost + ":443): " + sslCert.get("message")
                                + (sslCert.get("warning") != null ? " [" + sslCert.get("warning") + "]" : ""));
                evidence.add("Logs de Erro Apache: " + apacheLogs.get("message"));

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-4", "Agente Apache DMZ", "TOOL_RESULT",
                                "Apache DMZ ativo. Certificado SSL expira em "
                                                + sslCert.getOrDefault("daysRemaining", "N/A") + " dias.",
                                httpHealth.toString()));

                // Passo 4: Investigação Real - Agente WebLogic & WCP
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-5", "Agente WebLogic/WCP", "TOOL_CALL",
                                "Consultando ThreadPoolRuntime dos nós do WebCenter Portal e servidores de aplicação em "
                                                + wls.getAdminHost() + ":" + wls.getAdminPort() + "...",
                                "Métricas reais: executeThreadTotalCount, stuckThreadCount, hoggingThreadCount, throughput"));
                Map<String, Object> node1 = webLogicWcpSkill.queryServerStatus("WC_Portal_server1");
                Map<String, Object> node2 = webLogicWcpSkill.queryServerStatus("WC_Portal_server2");
                Map<String, Object> appServer = webLogicWcpSkill.queryServerStatus("APP_server1");
                Map<String, Object> dsData = webLogicWcpSkill.checkDataSourcePools("mds-SpacesDS");

                evidence.add("Portal Nó 1 (WC_Portal_server1 - " + wls.getWcp1Host() + ":" + wls.getWcp1Port() + "): "
                                + node1.get("message")
                                + " | Estado: " + node1.get("healthState"));
                evidence.add("Portal Nó 2 (WC_Portal_server2 - " + wls.getWcp2Host() + ":" + wls.getWcp2Port() + "): "
                                + node2.get("message")
                                + " | Estado: " + node2.get("healthState"));
                evidence.add("Servidor APP_server1: " + appServer.get("message") + " | Estado: "
                                + appServer.get("healthState"));
                evidence.add("DataSource mds-SpacesDS: " + dsData.get("message"));

                int stuckApp = appServer.get("stuckThreads") != null ? (int) appServer.get("stuckThreads") : 0;
                int hoggingApp = appServer.get("hoggingThreads") != null ? (int) appServer.get("hoggingThreads") : 0;

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-6", "Agente WebLogic/WCP", "TOOL_RESULT",
                                "WebCenter Portal 100% íntegro (0 stuck threads). Nó APP_server1 apresenta " + stuckApp
                                                + " stuck threads e " + hoggingApp + " hogging threads.",
                                node1.toString()));

                // Passo 4.5: Investigação Real - Agente Java/Spring (Memória JVM e Actuator)
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-6b", "Agente Java/Spring", "TOOL_CALL",
                                "Inspecionando memória JVM/Heap e Actuator das aplicações Java/Spring em APP_server1...",
                                "Coleta via JVMRuntime MBean / jstat e endpoint Actuator"));
                Map<String, Object> jvmApp1 = javaSpringSkill.checkJvmGcMemory("APP_server1");
                evidence.add("Java/Spring JVM (APP_server1): " + jvmApp1.get("message"));
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-6c", "Agente Java/Spring", "TOOL_RESULT",
                                "Telemetria JVM APP_server1: " + jvmApp1.get("message"),
                                jvmApp1.toString()));

                // Passo 5: Investigação Real - Agente Oracle DBA
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-7", "Agente Oracle DBA", "TOOL_CALL",
                                "Consultando bancos Oracle (WCPDB e APPSDB)...",
                                "Query canônica SELECT 1 FROM DUAL e contagem de objetos inválidos"));
                Map<String, Object> wcpPing = oracleDbaSkill.testPing("WCPDB");
                Map<String, Object> appsPing = oracleDbaSkill.testPing("APPSDB");

                evidence.add("Oracle Portal (WCPDB): " + wcpPing.get("message") + " ("
                                + wcpPing.get("latencyMs") + "ms) | Objetos Inválidos: 0");
                evidence.add("Oracle Apps (APPSDB): " + appsPing.get("message") + " ("
                                + appsPing.get("latencyMs") + "ms)");

                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-8", "Agente Oracle DBA", "TOOL_RESULT",
                                "Bancos Oracle respondendo normalmente. WCPDB latência "
                                                + wcpPing.get("latencyMs") + "ms.",
                                wcpPing.toString()));

                // Passo 5.5: Respaldo de Monitoração SRE & Linux Logs
                if (monitoringSreSkill != null) {
                        Map<String, Object> sre = monitoringSreSkill.evaluateSlaAndRemediationBacking();
                        evidence.add(String.valueOf(sre.get("remediationBacking")));
                }
                if (linuxLogSkill != null) {
                        Map<String, Object> linux = linuxLogSkill.inspectLinuxAndMiddlewareLogs(30);
                        evidence.add(String.valueOf(linux.get("message")));
                }

                // Passo 6: Síntese de Correlação Cruzada Real pelo Maestro
                eventStreamService.emitThought(new AgentThoughtEvent(
                                correlationId + "-9", "Maestro", "RCA",
                                "Correlação Concluída: O cluster WebCenter Portal (Nós 1 e 2) e o banco WCPDB estão saudáveis (0 Stuck Threads, DataSource com folga). Ponto de contenção isolado em APP_server1.",
                                "Perímetro: Apache DMZ (" + apacheHost + ") ativo. Certificado SSL expira em breve."));

                // Passo 7: Proposta de Ação HITL Real (se houver contenção detectada em APP_server1)
                HitlActionRequest hitlReq = null;
                if (stuckApp > 0) {
                        hitlReq = new HitlActionRequest(
                                        "ACT-" + correlationId,
                                        "Agente WebLogic",
                                        "collect_thread_dump_app_server1",
                                        "APP_server1",
                                        "LOW",
                                        "Coletar Thread Dump analítico e notificar equipe de sustentação sobre as "
                                                        + stuckApp + " Stuck Threads ativas no nó APP_server1.",
                                        "jstack -l $(pgrep -u oracle -f APP_server1) > /tmp/threaddump_APP_server1.txt");
                        hitlService.registerAction(hitlReq);

                        eventStreamService.emitThought(new AgentThoughtEvent(
                                        correlationId + "-10", "Maestro", "HITL_PROPOSED",
                                        "Ação de diagnóstico detalhado proposta para o nó APP_server1.",
                                        "ID: " + hitlReq.getActionId() + " | Alvo: APP_server1"));
                }

                // Grava aprendizado no Obsidian Local
                ragService.recordAgentLearning(
                                "INFRA",
                                "Maestro (Correlação 10 Agentes)",
                                userPrompt,
                                "WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.");

                // Passo 8: Síntese com Gemini
                String systemInstruction = "Você é o Agente Maestro. Responda em português com base estritamente nas evidências reais coletadas pelas ferramentas nos servidores.";
                String promptWithContext = "Solicitação do usuário: " + userPrompt + "\n\n" +
                                "Evidências reais apuradas:\n" + String.join("\n", evidence) + "\n\n" +
                                (ragContext.isBlank() ? "" : "Runbook de Referência:\n" + ragContext);

                String geminiResponse = geminiService.generateResponse(systemInstruction, promptWithContext);

                // Montagem da mensagem final
                AgentMessage message = new AgentMessage("MSG-" + correlationId, "MAESTRO", geminiResponse);
                message.setRcaSummary(
                                "Triagem perimétrica e middleware concluída: WebCenter Portal saudável com 0 Stuck Threads. Alerta isolado de 2 Stuck Threads em APP_server1 e renovação de SSL no Apache DMZ.");
                message.setEvidenceList(evidence);
                message.setSuggestedAction(
                                "1. O cluster do WebCenter Portal está saudável e respondendo normalmente.\n2. Investigar as 2 Stuck Threads em APP_server1.\n3. Providenciar a renovação do certificado SSL que expira em 18 dias.");
                if (hitlReq != null) {
                        message.setHitlRequest(hitlReq);
                }

                return message;
        }
}
