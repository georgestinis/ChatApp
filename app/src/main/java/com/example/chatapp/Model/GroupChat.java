package com.example.chatapp.Model;

public class GroupChat {
    private String sender;
    private String message;
    private long time;
    private String type;

    public GroupChat(String sender, String message, long time, String type) {
        this.sender = sender;
        this.message = message;
        this.time = time;
        this.type = type;
    }

    public GroupChat() {
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
