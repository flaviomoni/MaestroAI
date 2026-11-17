package com.corp.ai.agents.controller;

import com.corp.ai.agents.service.AgentHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryApiController {

    private final AgentHistoryService agentHistoryService;

    public HistoryApiController(AgentHistoryService agentHistoryService) {
        this.agentHistoryService = agentHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<AgentHistoryService.AgentHistoryItem>> getActionHistory() {
        return ResponseEntity.ok(agentHistoryService.getAllHistory());
    }
}
