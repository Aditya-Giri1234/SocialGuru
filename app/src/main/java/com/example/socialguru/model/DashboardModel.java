package com.example.socialguru.model;

public class DashboardModel {
int profile_image,save,postImg;
String username,about,like,comment,share;

    public DashboardModel(int profile_image, int save, int postImg, String username, String about, String like, String comment, String share) {
        this.profile_image = profile_image;
        this.save = save;
        this.postImg = postImg;
        this.username = username;
        this.about = about;
        this.like = like;
        this.comment = comment;
        this.share = share;
    }

    public int getProfile_image() {
        return profile_image;
    }

    public void setProfile_image(int profile_image) {
        this.profile_image = profile_image;
    }

    public int getSave() {
        return save;
    }

    public void setSave(int save) {
        this.save = save;
    }

    public int getPostImg() {
        return postImg;
    }

    public void setPostImg(int postImg) {
        this.postImg = postImg;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getLike() {
        return like;
    }

    public void setLike(String like) {
        this.like = like;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getShare() {
        return share;
    }

    public void setShare(String share) {
        this.share = share;
    }

    @Override
    public String toString() {
        return "DashboardModel{" +
                "profile_image=" + profile_image +
                ", save=" + save +
                ", postImg=" + postImg +
                ", username='" + username + '\'' +
                ", about='" + about + '\'' +
                ", like='" + like + '\'' +
                ", comment='" + comment + '\'' +
                ", share='" + share + '\'' +
                '}';
    }
}
