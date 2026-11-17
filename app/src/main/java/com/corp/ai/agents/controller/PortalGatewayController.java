package com.corp.ai.agents.controller;

import com.corp.ai.agents.service.AgentHistoryService;
import com.corp.ai.agents.service.RAGService;
import com.corp.ai.agents.skills.CyberSecuritySkill;
import com.corp.ai.agents.skills.OpenLdapSkill;
import com.corp.ai.agents.skills.WebCenterPortalSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
public class PortalGatewayController {

    private final CyberSecuritySkill cyberSecuritySkill;
    private final OpenLdapSkill openLdapSkill;
    private final WebCenterPortalSkill webCenterPortalSkill;
    private final RAGService ragService;
    private final AgentHistoryService agentHistoryService;

    @Autowired
    public PortalGatewayController(CyberSecuritySkill cyberSecuritySkill,
                                   OpenLdapSkill openLdapSkill,
                                   WebCenterPortalSkill webCenterPortalSkill,
                                   RAGService ragService,
                                   AgentHistoryService agentHistoryService) {
        this.cyberSecuritySkill = cyberSecuritySkill;
        this.openLdapSkill = openLdapSkill;
        this.webCenterPortalSkill = webCenterPortalSkill;
        this.ragService = ragService;
        this.agentHistoryService = agentHistoryService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<Map<String, Object>> generateCaptcha() {
        return ResponseEntity.ok(cyberSecuritySkill.generateCaptchaChallenge());
    }

    @GetMapping("/profiles")
    public ResponseEntity<Map<String, Object>> getPortalProfilesAndLayout() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("standardLayout", webCenterPortalSkill.inspectStandardLayoutAssets());
        resp.put("provisionedPortals", webCenterPortalSkill.listProvisionedPortals());
        resp.put("cyberShieldStatus", cyberSecuritySkill.getSecurityShieldStatus());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginSingleEntry(@RequestBody Map<String, Object> payload) {
        String username = String.valueOf(payload.getOrDefault("username", "")).trim();
        String rawRole = payload.get("clientRole") != null ? String.valueOf(payload.get("clientRole")).trim() : "";
        String clientRole = (!rawRole.isBlank() && !"AUTO_LDAP".equalsIgnoreCase(rawRole))
                ? rawRole
                : openLdapSkill.resolveUserGroupFromLdap(username);
        String captchaId = String.valueOf(payload.getOrDefault("captchaId", "")).trim();
        String clientIp = String.valueOf(payload.getOrDefault("clientIp", "127.0.0.1")).trim();

        Integer captchaAnswer = null;
        if (payload.get("captchaAnswer") != null) {
            try {
                captchaAnswer = Integer.parseInt(String.valueOf(payload.get("captchaAnswer")).trim());
            } catch (NumberFormatException ignored) {}
        }

        Map<String, Object> resp = new LinkedHashMap<>();

        // 1. Verifica se o IP já está bloqueado no Escudo Online do Agente de Cibersegurança
        if (cyberSecuritySkill.isIpBlocked(clientIp)) {
            resp.put("authenticated", false);
            resp.put("error", "Acesso bloqueado pelo Escudo Online do Agente de Cibersegurança (IP na Blacklist).");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
        }

        // 2. Inspeção Online contra SQLi / XSS no login
        String lowerUser = username.toLowerCase();
        if (lowerUser.contains("' or ") || lowerUser.contains("1=1") || lowerUser.contains("<script>")) {
            Map<String, Object> defense = cyberSecuritySkill.monitorAndDefendExternalThreats(username);
            ragService.recordAgentLearning(
                    "CYBERSEC",
                    "Agente Cibersegurança",
                    "Tentativa de injeção no login único (/api/portal/login): " + username,
                    String.valueOf(defense.get("message"))
            );
            if (agentHistoryService != null) {
                agentHistoryService.addEntry(
                        "Agente Cibersegurança",
                        "DEFENSE",
                        "Portal Gateway /login (" + clientIp + ")",
                        "Ataque neutralizado em tempo real: " + username,
                        String.valueOf(defense.get("message")),
                        "BLOCKED"
                );
            }
            resp.put("authenticated", false);
            resp.put("blockedByCyberAgent", true);
            resp.put("defenseReport", defense);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
        }

        // 3. Validação obrigatória do Captcha
        if (!cyberSecuritySkill.verifyCaptcha(captchaId, captchaAnswer)) {
            resp.put("authenticated", false);
            resp.put("error", "Desafio CAPTCHA inválido ou expirado. Solicite um novo desafio.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        // 4. Autenticação Bind no OpenLDAP e Resolução da Visualização Distinta do Cliente
        Map<String, Object> ldapAuth = openLdapSkill.testBind(username);
        Map<String, Object> clientView = webCenterPortalSkill.resolveClientViewForSingleEntry(username, clientRole);

        resp.put("authenticated", true);
        resp.put("ldapStatus", ldapAuth.get("status"));
        resp.put("resolvedPortalView", clientView);
        resp.put("message", "Autenticação única concluída com CAPTCHA e OpenLDAP. Redirecionando para visualização do perfil " + clientRole);
        return ResponseEntity.ok(resp);
    }
}
