package com.example.chatapp.Model;

public class GroupChat {
    private String sender;
    private String message;
    private long time;

    public GroupChat(String sender, String message, long time) {
        this.sender = sender;
        this.message = message;
        this.time = time;
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
}
