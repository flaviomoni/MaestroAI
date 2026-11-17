package com.corp.ai.agents.service;

import com.corp.ai.agents.model.HitlActionRequest;
import com.corp.ai.agents.model.HitlDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HitlService {

    private static final Logger log = LoggerFactory.getLogger(HitlService.class);
    private final Map<String, HitlActionRequest> pendingActions = new ConcurrentHashMap<>();

    public void registerAction(HitlActionRequest request) {
        pendingActions.put(request.getActionId(), request);
        log.warn("[HITL Alert] Nova ação pendente de aprovação humana: ID={}, Ação={}, Alvo={}, Risco={}",
                request.getActionId(), request.getActionName(), request.getTarget(), request.getRiskLevel());
    }

    public Optional<HitlActionRequest> getAction(String actionId) {
        return Optional.ofNullable(pendingActions.get(actionId));
    }

    public Map<String, HitlActionRequest> getAllPending() {
        return pendingActions;
    }

    public HitlActionRequest processDecision(HitlDecision decision) {
        HitlActionRequest action = pendingActions.get(decision.getActionId());
        if (action == null) {
            throw new IllegalArgumentException("Ação não encontrada ou já expirada: " + decision.getActionId());
        }

        action.setResolvedAt(Instant.now());
        if (decision.isApproved()) {
            action.setStatus("APPROVED");
            log.info("[HITL Approved] Ação {} foi APROVADA pelo operador humano. Feedback: {}",
                    action.getActionId(), decision.getFeedback());
        } else {
            action.setStatus("REJECTED");
            log.warn("[HITL Rejected] Ação {} foi REJEITADA pelo operador humano. Motivo: {}",
                    action.getActionId(), decision.getFeedback());
        }

        return action;
    }
}
