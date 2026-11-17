package com.corp.ai.agents.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AgentHistoryService {

    private static final Logger log = LoggerFactory.getLogger(AgentHistoryService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public record AgentHistoryItem(
            String id,
            String timestamp,
            String agentName,
            String actionType,    // REMEDIATION, DEFENSE, DIAGNOSTIC, PROVISIONING, HITL
            String target,
            String summary,
            String resolution,
            String status         // RESOLVED, BLOCKED, APPROVED, REJECTED, COMPLETED
    ) {}

    private final List<AgentHistoryItem> history = new CopyOnWriteArrayList<>();

    public AgentHistoryService() {
        initBaselineHistory();
    }

    private void initBaselineHistory() {
        addEntry(
                "Agente Cibersegurança",
                "DEFENSE",
                "Apache DMZ (200.159.224.96)",
                "Ataque SQL Injection barrado na tela de login único (/portal/login)",
                "IP 185.220.101.42 adicionado na ActiveThreatBlocklist por 30min (OWASP CRS 942100).",
                "BLOCKED"
        );
        addEntry(
                "Agente WebLogic",
                "REMEDIATION",
                "APP_server1",
                "Detecção de 2 Stuck Threads com tempo >600s no pool de threads",
                "Coletado dump jstack e aprovada ação HITL para isolamento gracioso da instância.",
                "APPROVED"
        );
        addEntry(
                "Agente Oracle DBA",
                "DIAGNOSTIC",
                "WCPDB (Banco WebCenter)",
                "Auditoria de sessões bloqueadoras e objetos inválidos no schema MDS",
                "Latência de query canônica 'SELECT 1 FROM DUAL' em 18ms. Zero locks ativos.",
                "COMPLETED"
        );
        addEntry(
                "Agente OpenLDAP",
                "DIAGNOSTIC",
                "portal-openldap (389)",
                "Validação de integridade de Bind e verificação de réplica syncrepl",
                "Autenticação de Bind realizada em 24ms. Atributo contextCSN sincronizado.",
                "COMPLETED"
        );
        addEntry(
                "Agente WebCenter Portal",
                "PROVISIONING",
                "WC_Portal_server1",
                "Provisionamento do Portal Corporativo Enterprise via REST API",
                "Template universal 'portal-universal-template' aplicado com cor #004e8b e grupo CLIENT_ENTERPRISE.",
                "COMPLETED"
        );
    }

    public synchronized void addEntry(String agentName, String actionType, String target,
                                      String summary, String resolution, String status) {
        String id = "ACT-" + UUID.randomUUID().toString().substring(0, 8);
        String now = LocalDateTime.now().format(TIME_FMT);
        AgentHistoryItem item = new AgentHistoryItem(id, now, agentName, actionType, target, summary, resolution, status);
        history.add(0, item);
        log.info("[AgentHistory] Nova ação registrada: [{}] {} em {} ({})", actionType, agentName, target, status);
    }

    public List<AgentHistoryItem> getAllHistory() {
        return Collections.unmodifiableList(history);
    }
}
