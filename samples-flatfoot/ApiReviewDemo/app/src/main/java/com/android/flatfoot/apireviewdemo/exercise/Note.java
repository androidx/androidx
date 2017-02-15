package com.android.flatfoot.apireviewdemo.exercise;

public class Note {
    private int id;
    private String label;
    private String body;

    public Note(int id, String label, String body) {
        this.id = id;
        this.label = label;
        this.body = body;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
