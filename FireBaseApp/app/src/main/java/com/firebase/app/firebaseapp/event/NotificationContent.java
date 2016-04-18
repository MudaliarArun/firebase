package com.firebase.app.firebaseapp.event;

/**
 * Created by Arun on 22-11-2015.
 */
public class NotificationContent {
    private String Key="";
    private Object[] mObjects;

    public NotificationContent(String key, Object... mObjects) {
        Key = key;
        this.mObjects = mObjects;
    }

    public String getKey() {
        return Key;
    }

    public void setKey(String key) {
        Key = key;
    }

    public Object[] getmObjects() {
        return mObjects;
    }

    public void setmObjects(Object[] mObjects) {
        this.mObjects = mObjects;
    }

}
