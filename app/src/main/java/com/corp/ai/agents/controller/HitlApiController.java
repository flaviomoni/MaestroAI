package com.corp.ai.agents.controller;

import com.corp.ai.agents.model.HitlActionRequest;
import com.corp.ai.agents.model.HitlDecision;
import com.corp.ai.agents.service.AgentHistoryService;
import com.corp.ai.agents.service.HitlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/hitl")
public class HitlApiController {

    private final HitlService hitlService;
    private final AgentHistoryService agentHistoryService;

    @Autowired
    public HitlApiController(HitlService hitlService, AgentHistoryService agentHistoryService) {
        this.hitlService = hitlService;
        this.agentHistoryService = agentHistoryService;
    }

    @GetMapping("/pending")
    public ResponseEntity<Collection<HitlActionRequest>> getPendingActions() {
        return ResponseEntity.ok(hitlService.getAllPending().values());
    }

    @PostMapping("/decision")
    public ResponseEntity<HitlActionRequest> submitDecision(@RequestBody HitlDecision decision) {
        HitlActionRequest updatedAction = hitlService.processDecision(decision);
        if (agentHistoryService != null) {
            String status = decision.isApproved() ? "APPROVED" : "REJECTED";
            agentHistoryService.addEntry(
                    "Operador SRE (HITL)",
                    "HITL",
                    updatedAction.getTarget(),
                    "Decisão HITL: " + updatedAction.getActionName() + " (" + status + ")",
                    decision.getFeedback(),
                    status
            );
        }
        return ResponseEntity.ok(updatedAction);
    }
}
