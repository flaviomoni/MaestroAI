package com.corp.ai.agents.service;

import com.corp.ai.agents.model.HitlActionRequest;
import com.corp.ai.agents.model.HitlDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class HitlServiceTest {

    private HitlService hitlService;

    @BeforeEach
    void setUp() {
        hitlService = new HitlService();
    }

    @ParameterizedTest(name = "Decisão HITL [{index}]: approved={0} -> status esperado={1}")
    @CsvSource({
            "true, APPROVED",
            "false, REJECTED"
    })
    void shouldProcessHitlDecisionsCorrectly(boolean approved, String expectedStatus) {
        HitlActionRequest req = new HitlActionRequest(
                "ACT-100",
                "Agente WebLogic",
                "collect_thread_dump",
                "APP_server1",
                "LOW",
                "Coletar thread dump de diagnóstico",
                "jstack -l 1234"
        );
        hitlService.registerAction(req);

        HitlDecision decision = new HitlDecision("ACT-100", approved, "Revisado pelo SRE");
        HitlActionRequest resolved = hitlService.processDecision(decision);

        assertEquals(expectedStatus, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
    }

    @Test
    void shouldThrowExceptionWhenActionIdNotFound() {
        HitlDecision decision = new HitlDecision("ACT-NON-EXISTENT", true, "Teste");
        assertThrows(IllegalArgumentException.class, () -> hitlService.processDecision(decision));
    }
}

