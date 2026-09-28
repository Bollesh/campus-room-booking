package com.campusbooking.types;

public class ApprovalRequest {
    private String approverEmail; // Ignored: the approver is the authenticated caller
    private String status; // "APPROVED" or "REJECTED"
    private String comments;

    // Getters and Setters
    public String getApproverEmail() { return approverEmail; }
    public void setApproverEmail(String approverEmail) { this.approverEmail = approverEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
