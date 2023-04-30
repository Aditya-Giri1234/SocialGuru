package com.example.socialguru.model;

public class FollowModel {

    private String followedBy;
    private long followsAt;
    private String profile;



    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public FollowModel() {
    }

    public String getFollowedBy() {
        return followedBy;
    }

    public void setFollowedBy(String followedBy) {
        this.followedBy = followedBy;
    }

    public long getFollowsAt() {
        return followsAt;
    }

    public void setFollowsAt(long followsAt) {
        this.followsAt = followsAt;
    }
}
