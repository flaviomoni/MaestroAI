package com.corp.ai.agents.model;

import java.time.Instant;

public class HitlActionRequest {

    private String actionId;
    private String agentName;
    private String actionName;        // ex: wls_restart_managed_node, apache_drain_node
    private String target;            // ex: WCP_Server1, Apache_VirtualHost
    private String riskLevel;         // LOW, MEDIUM, HIGH
    private String justification;
    private String proposedCommand;
    private String status;            // PENDING, APPROVED, REJECTED, EXECUTED
    private Instant createdAt;
    private Instant resolvedAt;

    public HitlActionRequest() {
        this.createdAt = Instant.now();
        this.status = "PENDING";
    }

    public HitlActionRequest(String actionId, String agentName, String actionName, String target, String riskLevel, String justification, String proposedCommand) {
        this.actionId = actionId;
        this.agentName = agentName;
        this.actionName = actionName;
        this.target = target;
        this.riskLevel = riskLevel;
        this.justification = justification;
        this.proposedCommand = proposedCommand;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getActionName() { return actionName; }
    public void setActionName(String actionName) { this.actionName = actionName; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public String getProposedCommand() { return proposedCommand; }
    public void setProposedCommand(String proposedCommand) { this.proposedCommand = proposedCommand; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
