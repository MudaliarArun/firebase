package com.firebase.app.firebaseapp;

import android.content.Context;
import android.util.Base64;

import com.firebase.app.firebaseapp.event.NotificationContent;
import com.firebase.app.firebaseapp.event.NotificationKey;
import com.firebase.app.firebaseapp.util.PaperTags;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.flurry.android.FlurryAgent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import io.paperdb.Paper;

/**
 * Created by Droid on 18/04/2016.
 * https://github.com/greenrobot/EventBus
 * Use event bus to fire error and data from the data manager class
 */
public class DataManager {
    private static DataManager dataManager;
    private final Context context;
    Firebase firebaseReference = new Firebase("https://dentaliq.firebaseio.com");
    Firebase currentUserReference = new Firebase("");
    JSONManager jsonManager;
    //var jsonManager = JsonManager.sharedInstance;
    String newMessage = "";
    private LoginDelegate didStartLoginProcess;
    LinkedHashMap allFavourites = new LinkedHashMap();
    ArrayList<?> allAnswers  = new ArrayList<>();
    ArrayList<?> allDontKnow  = new ArrayList<>();
    ArrayList<?> allSomeWhat  = new ArrayList<>();
    ArrayList<?> allKnow  = new ArrayList<>();
    int highScore = 0;
    boolean unlocked = false;
    ArrayList<?> initialQuestions = new ArrayList<>();
    ArrayList<String> initialCategories = new ArrayList<>(Arrays.asList("Anatomy","Dental Basics","Endodontics","Periodontics","Oral Medicine","Orthodontics","Restorative Dentistry","Paediatrics","Radiology"));
    LinkedHashMap<String,?> registrationDictionary = new LinkedHashMap<>();

    boolean showNormalQuestionTutorial = true;
    boolean showSJTTutorial = true;
    boolean showEthicalScenarioTutorial = true;
    boolean showUserRatingTutorial = true;
    LinkedHashMap<String,?> dataToLink = new LinkedHashMap();


    String subscriptionString  = "No Subscription";

    private DataManager(Context context) {
        this.context= context;
    }
    //SimpleDate dateFormatter = NSDateFormatter();

    public static DataManager getInstance(Context context){
        if(dataManager == null) {
            dataManager = new DataManager(context);
            dataManager.jsonManager =  JSONManager.getInstance(context);
        }

        return dataManager;
    }

    public LoginDelegate getDidStartLoginProcess() {
        return didStartLoginProcess;
    }

    public void setDidStartLoginProcess(LoginDelegate didStartLoginProcess) {
        this.didStartLoginProcess = didStartLoginProcess;
    }
    // MARK: Login

    /**
     * check if user is logged in, you must call the network check functionality on your own in android
     * @return
     */
    public boolean isLoggedIn(){

        if(firebaseReference.getAuth() != null){
            currentUserReference = new Firebase(firebaseReference+"/users/"+firebaseReference.getAuth().getUid());
            // user authenticated
            return true;
        }else{
            // No user is signed in
            return false;
        }
    }

    public boolean isLoggedInAsGuest(){
        try {
            if(firebaseReference.getAuth().getProvider().equalsIgnoreCase("anonymous")){
                return true;
            }
        }catch (NullPointerException e){
            System.out.println("isLoggedInAsGuest "+e.toString());
            return false;
        }
        return false;
    }
    public void  logout() {
        firebaseReference.unauth();
    }

    //MARK: Account Details

    public void sendPasswordReset(String email) {

        // convert the email string to lower case
        String emailToLowerCase = email.toLowerCase();
        // remove any whitespaces before and after the email address
        String emailClean = emailToLowerCase.replaceAll(" ", "").trim();
        firebaseReference.resetPassword(emailClean, new Firebase.ResultHandler() {
            @Override
            public void onSuccess() {
                EventBus.getDefault().post(new NotificationContent(NotificationKey.passResetSuccess, null, null));
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                EventBus.getDefault().post(new NotificationContent(NotificationKey.passResetFailure, null, null));
            }
        });
    }
    public void updateRegistrationDetails() {

        currentUserReference.child("based").setValue(registrationDictionary.get("based"));
        currentUserReference.child("occupation").setValue(registrationDictionary.get("occupation"));
        currentUserReference.child("university").setValue(registrationDictionary.get("university"));
        currentUserReference.child("gradYear").setValue(registrationDictionary.get("gradYear"));
        currentUserReference.child("interests").setValue(registrationDictionary.get("interests"));

        FlurryAgent.setUserId(currentUserReference.getAuth().getUid());
        LinkedHashMap<String,String> newUserSignUp = new LinkedHashMap<>();
        newUserSignUp.put("provider",currentUserReference.getAuth().getProvider());
        newUserSignUp.put("based",registrationDictionary.get("based").toString());
        newUserSignUp.put("occupation",registrationDictionary.get("occupation").toString());
        newUserSignUp.put("university",registrationDictionary.get("university").toString());
        newUserSignUp.put("gradYear",registrationDictionary.get("gradYear").toString());
        newUserSignUp.put("interests",registrationDictionary.get("interests").toString());

        FlurryAgent.logEvent("UserSignUp", newUserSignUp);

    }

    public String getLoginDetails(){

        if (this.firebaseReference.getAuth().getProviderData().containsKey("displayName") &&
                this.firebaseReference.getAuth().getProviderData().get("displayName") != null) {
            return "Logged in as "+this.firebaseReference.getAuth().getProviderData().get("displayName");
        }

        if (this.firebaseReference.getAuth().getProviderData().containsKey("email") &&
                this.firebaseReference.getAuth().getProviderData().get("email") != null) {
            return "Logged in as "+this.firebaseReference.getAuth().getProviderData().containsKey("email");
        }

        return "Logged in via "+firebaseReference.getAuth().getProvider();
    }

    public void emailSignUp(final String email, final String pass) {
        firebaseReference.createUser(email, pass, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> stringObjectMap) {
                emailLogin(email, pass);
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                if (didStartLoginProcess != null)
                    didStartLoginProcess.onError(firebaseError);
                System.out.println(firebaseError.toString());
            }
        });

    }

    public void emailLogin(String email,String pass) {
        if(didStartLoginProcess != null){
            didStartLoginProcess.didStartLoginProcess();
        }
        firebaseReference.authWithPassword(email, pass, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                checkIfUserAlreadyExists();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                if (didStartLoginProcess != null)
                    didStartLoginProcess.onError(firebaseError);

                System.out.println(firebaseError.toString());
            }
        });
    }

    public void checkIfUserAlreadyExists()  {
        currentUserReference = new Firebase(firebaseReference+"/users/"+firebaseReference.getAuth().getUid());

        firebaseReference.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null || !dataSnapshot.hasChild(firebaseReference.getAuth().getUid())) {
                    if (dataToLink != null) {

                        LinkedHashMap linkedUser = new LinkedHashMap();
                        linkedUser.put("uid", firebaseReference.getAuth().getUid());
                        linkedUser.put("provider", firebaseReference.getAuth().getProvider());

                        if (firebaseReference.getAuth().getProviderData().containsKey("displayName") &&
                                firebaseReference.getAuth().getProviderData().get("displayName") != null) {
                            linkedUser.put("displayName", firebaseReference.getAuth().getProviderData().get("displayName").toString());
                        }

                        if (firebaseReference.getAuth().getProviderData().containsKey("email") &&
                                firebaseReference.getAuth().getProviderData().get("email") != null) {
                            linkedUser.put("email", firebaseReference.getAuth().getProviderData().get("email").toString());
                        }

                        for (Map.Entry<String, ?> entry : dataToLink.entrySet()) {
                            String key = entry.getKey();
                            linkedUser.put(key, entry.getValue());
                        }

                        firebaseReference.child("users").child(firebaseReference.getAuth().getUid()).setValue(linkedUser);

                        if (dataToLink.containsKey("answered") && dataToLink.get("answered") != null) {
                            firebaseReference.child("users/" + firebaseReference.getAuth().getUid() + "/answered").setValue(dataToLink.get("answered"));
                        }
                        if (dataToLink.containsKey("favourited") && dataToLink.get("favourited") != null) {
                            firebaseReference.child("users/" + firebaseReference.getAuth().getUid() + "/favourited").setValue(dataToLink.get("favourited"));
                        }
                        if (dataToLink.containsKey("interests") && dataToLink.get("interests") != null) {
                            firebaseReference.child("users/" + firebaseReference.getAuth().getUid() + "/interests").setValue(dataToLink.get("interests"));
                        }
                        if (dataToLink.containsKey("ratedKnow") && dataToLink.get("ratedKnow") != null) {
                            firebaseReference.child("users/" + firebaseReference.getAuth().getUid() + "/ratedKnow").setValue(dataToLink.get("ratedKnow"));
                        }
                        if (dataToLink.containsKey("ratedDontKnow") && dataToLink.get("ratedDontKnow") != null) {
                            firebaseReference.child("users/" + firebaseReference.getAuth().getUid() + "/ratedDontKnow").setValue(dataToLink.get("ratedDontKnow"));
                        }
                        if (dataToLink.containsKey("ratedSomewhat") && dataToLink.get("ratedSomewhat") != null) {
                            firebaseReference.child("users/" + firebaseReference.getAuth().getUid() + "/ratedSomewhat").setValue(dataToLink.get("ratedSomewhat"));
                        }
                    } else {
                        LinkedHashMap newUser = new LinkedHashMap();
                        newUser.put("uid", firebaseReference.getAuth().getUid());
                        newUser.put("provider", firebaseReference.getAuth().getProvider());

                        if (firebaseReference.getAuth().getProviderData().containsKey("displayName") &&
                                firebaseReference.getAuth().getProviderData().get("displayName") != null) {
                            newUser.put("displayName", firebaseReference.getAuth().getProviderData().get("displayName"));
                        }

                        if (firebaseReference.getAuth().getProviderData().containsKey("email") &&
                                firebaseReference.getAuth().getProviderData().get("email") != null) {
                            newUser.put("email", firebaseReference.getAuth().getProviderData().get("email"));
                        }

                        //print("New User")

                        firebaseReference.child("users").child(firebaseReference.getAuth().getUid()).setValue(newUser);
                    }
                }
                checkIfRegisteredDetails();

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("onCancelled " + firebaseError.toString());
            }
        });

    }

    public void checkIfRegisteredDetails()  {

        currentUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.hasChild("based")) {
                    //NSNotificationCenter.defaultCenter().postNotificationName(NotificationKey.extraDetailsCheck, object:true , userInfo: nil);
                    // here i have passed two objects 1st is true, 2nd is null as a userInfo
                    EventBus.getDefault().post(new NotificationContent(NotificationKey.extraDetailsCheck, true, null));
                    System.out.println(dataSnapshot.toString());
                } else {
                    //NSNotificationCenter.defaultCenter().postNotificationName(NotificationKey.extraDetailsCheck, object:false , userInfo: nil);
                    // here i have passed two objects 1st is true, 2nd is null as a userInfo
                    EventBus.getDefault().post(new NotificationContent(NotificationKey.extraDetailsCheck, false, null));
                    System.out.println(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("onCancelled " + firebaseError);
            }
        });
    }
    public void  loadData() {
        //checkLockStatus();
        //loadFavourites();
        //loadAnswered();
        //loadRated();
        //loadHighScore();
        //loadInitialArray();
    }

    public byte[] decodeImageWithBase64String(String base64String){
        if(base64String == null)
            return null;
        return Base64.decode(base64String, Base64.DEFAULT);
    }

    public void showRating() {


        if (allAnswers.size() >= 100) {
            currentUserReference.child("appRatingActionTaken").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    System.out.println(dataSnapshot.toString());
                    if(dataSnapshot.exists() && dataSnapshot.getValue().toString().equalsIgnoreCase("true")){
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.showRatingAction,false,null));
                    }else{
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.showRatingAction,true,null));
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    //EventBus.getDefault().post(new NotificationContent(NotificationKey.showRatingAction,firebaseError,null));
                }
            });

        } else {
            //print("More questions need to be answered to show Rate App Banner")
        }



    }

    public void updateRatingAction() {

        currentUserReference.child("appRatingActionTaken").setValue("true");

    }

    public void checkQuestionVersion(final boolean showAlert, final boolean autoDownload) {
        firebaseReference.child("questionBank/version").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.getValue().toString().equalsIgnoreCase(jsonManager.currentVersionNumber)) {
                    if (autoDownload) {
                        downloadUpdate();
                    } else {
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.newUpdateAvailable, showAlert, null));
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                //EventBus.getDefault().post(new NotificationContent(NotificationKey.newUpdateAvailable,firebaseError,showAlert,null));
            }
        });
    }

    public void downloadUpdate() {

        firebaseReference.child("questionBank/newMessage").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null){
                    newMessage = dataSnapshot.getValue().toString();
                }
                firebaseReference.child("questionBank").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        try {
                            JSONObject json  = new JSONObject((String) dataSnapshot.getValue());
                            String theJSONText = json.toString();
                            Paper.book(PaperTags.BOOK).write(PaperTags.JSON_DATA,theJSONText);//save data to storage

                        } catch (JSONException e) {
                            e.printStackTrace();
                            EventBus.getDefault().post(new NotificationContent(NotificationKey.questionBankError, null, null));
                        }
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.updatedQuestionBank,null, null));
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.questionBankError,firebaseError, null));
                    }
                });


            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                EventBus.getDefault().post(new NotificationContent(NotificationKey.questionBankError,firebaseError, null));
            }
        });


    }


    /*
    public void downloadingNewVersion() {

    }

    public void loadInitialArray() {

        ArrayList<?> temp = new ArrayList<>();

        for q in jsonManager.allQuestions {

            if (!isQuestionLocked(q as! NSDictionary)) {
                temp.addObject(q);
            }

        }

        initialQuestions = temp;
    }*/
}
