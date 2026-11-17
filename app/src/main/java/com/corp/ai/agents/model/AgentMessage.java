package com.corp.ai.agents.model;

import java.time.Instant;
import java.util.List;

public class AgentMessage {

    private String id;
    private String sender;       // USER, MAESTRO, SYSTEM
    private String content;
    private String rcaSummary;   // Resumo de Causa-Raiz (se aplicável)
    private List<String> evidenceList;
    private String suggestedAction;
    private HitlActionRequest hitlRequest;
    private Instant timestamp;

    public AgentMessage() {
        this.timestamp = Instant.now();
    }

    public AgentMessage(String id, String sender, String content) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRcaSummary() { return rcaSummary; }
    public void setRcaSummary(String rcaSummary) { this.rcaSummary = rcaSummary; }
    public List<String> getEvidenceList() { return evidenceList; }
    public void setEvidenceList(List<String> evidenceList) { this.evidenceList = evidenceList; }
    public String getSuggestedAction() { return suggestedAction; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public HitlActionRequest getHitlRequest() { return hitlRequest; }
    public void setHitlRequest(HitlActionRequest hitlRequest) { this.hitlRequest = hitlRequest; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
