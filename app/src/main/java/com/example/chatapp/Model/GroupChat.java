package com.example.chatapp.Model;

public class GroupChat {
    private String sender;
    private String message;
    private long time;
    private String type;
    private int length;

    public GroupChat(String sender, String message, long time, String type, int length) {
        this.sender = sender;
        this.message = message;
        this.time = time;
        this.type = type;
        this.length = length;
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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

}
