package com.example.chatapp.Model;

public class Chatlist {
    public String id;
    public long time;

    public Chatlist(String id, long time) {
        this.id = id;
        this.time = time;
    }

    public Chatlist() {
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
