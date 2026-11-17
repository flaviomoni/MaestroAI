package com.corp.ai.agents.controller;

import com.corp.ai.agents.model.AgentMessage;
import com.corp.ai.agents.model.ChatRequest;
import com.corp.ai.agents.orchestrator.MaestroOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final MaestroOrchestrator maestroOrchestrator;

    public ChatApiController(MaestroOrchestrator maestroOrchestrator) {
        this.maestroOrchestrator = maestroOrchestrator;
    }

    @PostMapping
    public ResponseEntity<AgentMessage> handleChat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AgentMessage response = maestroOrchestrator.processUserRequest(request.getMessage());
        return ResponseEntity.ok(response);
    }
}
