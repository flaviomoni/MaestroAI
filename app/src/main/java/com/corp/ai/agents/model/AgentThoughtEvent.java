package com.corp.ai.agents.model;

import java.time.Instant;

public class AgentThoughtEvent {

    private String id;
    private String agentName;     // Maestro, Apache, WebLogic, LDAP, Spring, Oracle
    private String eventType;     // THOUGHT, TOOL_CALL, TOOL_RESULT, RCA, HITL_PROPOSED
    private String content;
    private String details;
    private Instant timestamp;

    public AgentThoughtEvent() {
        this.timestamp = Instant.now();
    }

    public AgentThoughtEvent(String id, String agentName, String eventType, String content, String details) {
        this.id = id;
        this.agentName = agentName;
        this.eventType = eventType;
        this.content = content;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
