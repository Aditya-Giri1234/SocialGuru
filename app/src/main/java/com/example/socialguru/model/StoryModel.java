package com.example.socialguru.model;

public class StoryModel {
    int story,storyType,profile_image;
    String name;

    public StoryModel(int story, int storyType, int profile_image, String name) {
        this.story = story;
        this.storyType = storyType;
        this.profile_image = profile_image;
        this.name = name;
    }

    public int getStory() {
        return story;
    }

    public void setStory(int story) {
        this.story = story;
    }

    public int getStoryType() {
        return storyType;
    }

    public void setStoryType(int storyType) {
        this.storyType = storyType;
    }

    public int getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(int profile_image) {
        this.profile_image = profile_image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "StoryModel{" +
                "story=" + story +
                ", storyType=" + storyType +
                ", profile_image=" + profile_image +
                ", name='" + name + '\'' +
                '}';
    }
}
