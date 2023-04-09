package com.example.socialguru.model;

public class Notification2Model {
    int profile_image;
    String notification,time;

    public Notification2Model(int profile_image, String notification, String time) {
        this.profile_image = profile_image;
        this.notification = notification;
        this.time = time;
    }

    public int getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(int profile_image) {
        this.profile_image = profile_image;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "Notification2Model{" +
                "profile_image=" + profile_image +
                ", notification='" + notification + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
