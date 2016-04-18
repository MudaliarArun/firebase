package com.firebase.app.firebaseapp;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Droid on 19/04/2016.
 * note: need to use gson,load json and loop for questions
 * once completed with json manager continue with DataManager
 */
public class JSONManager {
    private static JSONManager jsonManager;
    LinkedHashMap<String,?> json= new LinkedHashMap<>();
    int totalNumberOfQuestions = 0;
    String currentVersionNumber = "1.0";
    ArrayList<?> allQuestions =new ArrayList<>();
    LinkedHashMap currentQuestion = new LinkedHashMap();

    public static JSONManager getInstance(){
        if(jsonManager == null){
            jsonManager = new JSONManager();
        }
        return jsonManager;
    }


}
