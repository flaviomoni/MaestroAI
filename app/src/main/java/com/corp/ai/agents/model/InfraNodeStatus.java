package com.corp.ai.agents.model;

import java.time.Instant;

public class InfraNodeStatus {

    private String nodeId;         // apache, wcp1, wcp2, wls_admin, ldap, spring_apps, oracle_wcp, oracle_apps
    private String name;           // Apache HTTPD DMZ, WebCenter Portal Nó 1, etc.
    private String layer;          // PERIMETER, MIDDLEWARE, IDENTITY, APPLICATION, DATABASE
    private String status;         // HEALTHY, WARNING, CRITICAL, UNKNOWN
    private String message;
    private long responseTimeMs;
    private Instant lastChecked;

    public InfraNodeStatus() {
        this.lastChecked = Instant.now();
    }

    public InfraNodeStatus(String nodeId, String name, String layer, String status, String message, long responseTimeMs) {
        this.nodeId = nodeId;
        this.name = name;
        this.layer = layer;
        this.status = status;
        this.message = message;
        this.responseTimeMs = responseTimeMs;
        this.lastChecked = Instant.now();
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLayer() { return layer; }
    public void setLayer(String layer) { this.layer = layer; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    public Instant getLastChecked() { return lastChecked; }
    public void setLastChecked(Instant lastChecked) { this.lastChecked = lastChecked; }
}
