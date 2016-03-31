package com.firebase.app.firebaseapp;

/**
 * Created by Droid on 31/03/2016.
 */
public class batter {

    private String id;
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public batter(String id, String type) {
        this.id = id;
        this.type = type;
    }
}
