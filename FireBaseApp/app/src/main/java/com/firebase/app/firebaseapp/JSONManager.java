package com.firebase.app.firebaseapp;

import android.content.Context;

import com.firebase.app.firebaseapp.util.JSONResourceReader;
import com.firebase.app.firebaseapp.util.PaperTags;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.paperdb.Book;
import io.paperdb.Paper;

/**
 * Created by Droid on 19/04/2016.
 * note: need to use gson,load json and loop for questions
 * once completed with json manager continue with DataManager
 */
public class JSONManager {
    private static JSONManager jsonManager;
    private final Context context;
    LinkedHashMap<String,?> json= new LinkedHashMap<>();
    int totalNumberOfQuestions = 0;
    String currentVersionNumber = "1.0";
    ArrayList<?> allQuestions =new ArrayList<>();
    LinkedHashMap currentQuestion = new LinkedHashMap();

    private JSONManager(Context context) {
        this.context = context;
    }

    public static JSONManager getInstance(Context context){
        if(jsonManager == null){
            jsonManager = new JSONManager(context);
        }
        return jsonManager;
    }

    public void load(){

        boolean dataFromExternalStorage = false;
        Book book = Paper.book(PaperTags.BOOK);
        if(book != null){
            try {
                String theJSONText = book.read(PaperTags.JSON_DATA);

                LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,String>>> mJsonMap = new LinkedHashMap<>();
                Map<String,Object> map = new HashMap<String,Object>();

                Gson gson = new GsonBuilder().create();
                mJsonMap = (LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, String>>>) gson.fromJson(theJSONText, map.getClass());

                json = mJsonMap.get("categories");
                currentVersionNumber =mJsonMap.get("version").toString();

                ArrayList<Object> temp = new ArrayList<>();
                totalNumberOfQuestions = 0;

                for (Map.Entry<String, ?> categories : json.entrySet()) {
                    LinkedHashMap<String, ?> mQuestions = (LinkedHashMap<String, ?>) ((LinkedHashMap<String,?>)categories.getValue()).get("questions");
                    if(mQuestions != null){
                        totalNumberOfQuestions = totalNumberOfQuestions +mQuestions.size();
                        for (Map.Entry<String, ?> questions : mQuestions.entrySet()) {
                            temp.add(questions.getValue());
                        }
                    }

                }
                allQuestions = temp;
                dataFromExternalStorage  =true;
            }catch (Exception e){
                dataFromExternalStorage = false;
            }
        }

        if(!dataFromExternalStorage) {
            JSONResourceReader reader= new JSONResourceReader(context.getResources(), R.raw.newdata);
            LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,String>>> mJsonMap = new LinkedHashMap<>();
            mJsonMap = reader.constructUsingGson(mJsonMap.getClass());

            json = mJsonMap.get("categories");
            currentVersionNumber =mJsonMap.get("version").toString();

            ArrayList<Object> temp = new ArrayList<>();
            totalNumberOfQuestions = 0;

            for (Map.Entry<String, ?> categories : json.entrySet()) {
                LinkedHashMap<String, ?> mQuestions = (LinkedHashMap<String, ?>) ((LinkedHashMap<String,?>)categories.getValue()).get("questions");
                if(mQuestions != null){
                    totalNumberOfQuestions = totalNumberOfQuestions +mQuestions.size();
                    for (Map.Entry<String, ?> questions : mQuestions.entrySet()) {
                        temp.add(questions.getValue());
                    }
                }

            }
            allQuestions = temp;
        }

    }


}
