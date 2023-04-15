package com.example.socialguru.model;

public class CommentModel {
    String commentedBody;
    long commentedAt;
    String commentedBy;

    public CommentModel() {
    }

    public String getCommentedBody() {
        return commentedBody;
    }

    public void setCommentedBody(String commentedBody) {
        this.commentedBody = commentedBody;
    }

    public long getCommentedAt() {
        return commentedAt;
    }

    public void setCommentedAt(long commentedAt) {
        this.commentedAt = commentedAt;
    }

    public String getCommentedBy() {
        return commentedBy;
    }

    public void setCommentedBy(String commentedBy) {
        this.commentedBy = commentedBy;
    }
}
