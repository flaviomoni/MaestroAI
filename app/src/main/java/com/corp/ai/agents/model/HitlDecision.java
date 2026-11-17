package com.corp.ai.agents.model;

public class HitlDecision {
    private String actionId;
    private boolean approved;
    private String feedback;

    public HitlDecision() {}

    public HitlDecision(String actionId, boolean approved, String feedback) {
        this.actionId = actionId;
        this.approved = approved;
        this.feedback = feedback;
    }

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
