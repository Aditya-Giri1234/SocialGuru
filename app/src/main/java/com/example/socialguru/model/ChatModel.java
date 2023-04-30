package com.example.socialguru.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ChatModel implements Parcelable {
    String profile;
    String userName;
    long lastSeen;
    String lastMessage;
    String messageId;
    String message;
    String uId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public ChatModel() {
    }



    public String getLastMessage() {
        return lastMessage;
    }


    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    protected ChatModel(Parcel in) {
        messageId=in.readString();
        messageId=in.readString();
        userName = in.readString();
        profile = in.readString();
        lastSeen = in.readLong();
        lastMessage=in.readString();
        uId=in.readString();
    }
    public static final Creator<ChatModel> CREATOR = new Creator<ChatModel>() {
        @Override
        public ChatModel createFromParcel(Parcel in) {
            return new ChatModel(in);
        }

        @Override
        public ChatModel[] newArray(int size) {
            return new ChatModel[size];
        }
    };


    @Override
    public void writeToParcel(@NonNull Parcel dest, int i) {
        dest.writeString(message);
        dest.writeString(messageId);
        dest.writeString(userName);
        dest.writeString(profile);
        dest.writeLong(lastSeen);
        dest.writeString(lastMessage);
        dest.writeString(uId);
    }
    @Override
    public int describeContents() {
        return 0;
    }

}
