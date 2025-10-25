package com.mockerview.dto;

public class ReviewRequest {
    private Long sessionId;
    private Long answerId;
    private String comment;
    private Double rating;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}