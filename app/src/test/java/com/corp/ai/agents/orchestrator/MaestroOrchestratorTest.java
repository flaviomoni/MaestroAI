package com.corp.ai.agents.orchestrator;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.model.AgentMessage;
import com.corp.ai.agents.service.*;
import com.corp.ai.agents.skills.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class MaestroOrchestratorTest {

    private MaestroOrchestrator orchestrator;
    private HitlService hitlService;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setSimulationMode(true);

        ObjectMapper objectMapper = new ObjectMapper();
        EventStreamService eventStreamService = new EventStreamService();
        GeminiService geminiService = new GeminiService(properties, objectMapper);
        RAGService ragService = new RAGService(properties);
        hitlService = new HitlService();

        SshExecutorService sshExecutorService = new SshExecutorService();
        WebLogicJmxExecutorService jmxExecutorService = new WebLogicJmxExecutorService(objectMapper);
        LdapExecutorService ldapExecutorService = new LdapExecutorService();
        OracleJdbcExecutorService oracleJdbcExecutorService = new OracleJdbcExecutorService();
        InfraStatusService infraStatusService = new InfraStatusService(properties);

        ApacheSkill apacheSkill = new ApacheSkill(properties, sshExecutorService);
        WebLogicWcpSkill webLogicWcpSkill = new WebLogicWcpSkill(properties, jmxExecutorService, sshExecutorService);
        OpenLdapSkill openLdapSkill = new OpenLdapSkill(properties, ldapExecutorService);
        JavaSpringSkill javaSpringSkill = new JavaSpringSkill(properties, sshExecutorService, jmxExecutorService);
        OracleDbaSkill oracleDbaSkill = new OracleDbaSkill(properties, oracleJdbcExecutorService);
        CyberSecuritySkill cyberSecuritySkill = new CyberSecuritySkill(properties);
        WebCenterPortalSkill webCenterPortalSkill = new WebCenterPortalSkill(properties);
        MiddlewareArchitectSkill middlewareArchitectSkill = new MiddlewareArchitectSkill(properties);
        MonitoringSreSkill monitoringSreSkill = new MonitoringSreSkill(infraStatusService);
        LinuxLogSkill linuxLogSkill = new LinuxLogSkill(properties, sshExecutorService);

        orchestrator = new MaestroOrchestrator(
                properties,
                eventStreamService,
                geminiService,
                ragService,
                hitlService,
                apacheSkill,
                webLogicWcpSkill,
                openLdapSkill,
                javaSpringSkill,
                oracleDbaSkill,
                cyberSecuritySkill,
                webCenterPortalSkill,
                middlewareArchitectSkill,
                monitoringSreSkill,
                LinuxLogSkill.class.cast(linuxLogSkill)
        );
    }

    @ParameterizedTest(name = "Cenário ReAct v2 [{index}]: prompt=\"{0}\" deve conter evidência \"{1}\"")
    @CsvSource({
            "'Usuários reportando erro 504 Gateway Timeout no Portal', 'Apache DMZ'",
            "'Verifique se há Stuck Threads no WebLogic 12c', 'WebLogic Topologia'",
            "'Teste o certificado SSL e reverse proxy no Apache DMZ', 'Certificado SSL'",
            "'Verifique memória JVM, Heap e Actuator das aplicações Spring', 'Spring Boot Actuator'",
            "'Realize um teste de bind no OpenLDAP', 'OpenLDAP Bind'",
            "'Inspecione locks e conexões no banco Oracle WCPDB', 'Oracle Portal (WCPDB)'",
            "'Execute um diagnóstico geral da infraestrutura', 'Java/Spring JVM (APP_server1)'",
            "'Executar Passo 2 de diagnóstico de middleware e perímetro', 'DataSource mds-SpacesDS'",
            "'Monitore possíveis ataques SQLi e ative defesa de cibersegurança online', 'Escudo Cibersegurança Online'",
            "'Provisionar novo portal para cliente usando o layout padrão do WebCenter', 'Layout Padrão (Universal Portal Template)'",
            "'Avaliar arquitetura de middleware, monitoração SLA e logs Linux', 'Especialista Middleware'"
    })
    void shouldRouteAndCollectCrossLayerEvidenceForAllScenarios(String prompt, String expectedEvidenceSubstring) {
        AgentMessage response = orchestrator.processUserRequest(prompt);

        assertNotNull(response);
        assertEquals("MAESTRO", response.getSender());
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isBlank());
        assertNotNull(response.getEvidenceList());
        assertFalse(response.getEvidenceList().isEmpty());

        boolean foundEvidence = response.getEvidenceList().stream()
                .anyMatch(item -> item.contains(expectedEvidenceSubstring));
        assertTrue(foundEvidence,
                () -> "Esperava encontrar '" + expectedEvidenceSubstring + "' nas evidências: " + response.getEvidenceList());
    }

    @Test
    void shouldProposeHitlActionWhenStuckThreadsDetectedDuringIncident504Triage() {
        AgentMessage response = orchestrator.processUserRequest("Erro 504 Gateway Timeout no Portal");

        assertNotNull(response.getHitlRequest(), "Deve propor ação HITL quando APP_server1 apresenta Stuck Threads");
        assertEquals("APP_server1", response.getHitlRequest().getTarget());
        assertEquals("PENDING", response.getHitlRequest().getStatus());
        assertTrue(hitlService.getAction(response.getHitlRequest().getActionId()).isPresent());
    }
}
