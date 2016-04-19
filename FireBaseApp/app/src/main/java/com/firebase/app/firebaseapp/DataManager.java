package com.firebase.app.firebaseapp;

import android.content.Context;
import android.text.format.DateFormat;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private String DATE_FORMAT_COMMON = "dd/MM/yyyy";
    SimpleDateFormat dateFormatter = new SimpleDateFormat(DATE_FORMAT_COMMON);
    //var jsonManager = JsonManager.sharedInstance;
    String newMessage = "";
    private LoginDelegate didStartLoginProcess;
    ArrayList<?> allFavourites = new ArrayList();
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


    /*func facebookLogin() {


        let facebookLogin = FBSDKLoginManager()


        facebookLogin.logInWithReadPermissions(["public_profile","email"], fromViewController: UIApplication.sharedApplication().keyWindow!.rootViewController, handler: {
            (facebookResult, facebookError) -> Void in

            if facebookError != nil {

                //print("Facebook login failed. Error \(facebookError)")

            } else if facebookResult.isCancelled {

                //print("Facebook login was cancelled.")


            } else {

                if self.loginDelegate != nil {
                    self.loginDelegate!.didStartLoginProcess()
                }

                let accessToken = FBSDKAccessToken.currentAccessToken().tokenString
                self.firebaseReference.authWithOAuthProvider("facebook", token: accessToken,
                        withCompletionBlock: { error, authData in
                    if error != nil {
                        //print("Login failed. \(error)")
                    } else {

                        self.checkIfUserAlreadyExists()


                    }
                })
            }
        })

    }

    func twitterLogin() {


        let twitterAuthHelper = TwitterAuthHelper(firebaseRef: firebaseReference, apiKey:"nN0UBlVyTYqxK3YAOqXVS6Yph")
        twitterAuthHelper.selectTwitterAccountWithCallback { error, accounts in
            if error != nil {
                // Error retrieving Twitter accounts

                //print(error)

                NSNotificationCenter.defaultCenter().postNotificationName(NotificationKey.invalidTwitterLogin, object: error, userInfo: nil);

            } else if accounts.count > 1 {
                // Select an account. Here we pick the first one for simplicity
                let account = accounts[0] as? ACAccount

                if self.loginDelegate != nil {
                    self.loginDelegate!.didStartLoginProcess()
                }

                twitterAuthHelper.authenticateAccount(account, withCallback: { error, authData in
                    if error != nil {
                        // Error authenticating account

                        //print(error)
                        NSNotificationCenter.defaultCenter().postNotificationName(NotificationKey.invalidTwitterLogin, object: error, userInfo: nil);
                    } else {
                        // User logged in!


                        self.checkIfUserAlreadyExists()

                    }
                })
            }
        }

    }*/
    public void loginAsGuest() {

        if(didStartLoginProcess != null) {
            didStartLoginProcess.didStartLoginProcess();
        }
        firebaseReference.authAnonymously(new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                checkIfUserAlreadyExists();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                EventBus.getDefault().post(new NotificationContent(NotificationKey.invalidLogin,firebaseError,null));
            }
        });
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

    public void loadDataForLinking() {
        firebaseReference.child("users/"+firebaseReference.getAuth().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dataToLink = (LinkedHashMap<String, ?>) dataSnapshot.getValue();
                EventBus.getDefault().post(new NotificationContent(NotificationKey.popToRoot,null));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

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
        checkLockStatus();
        loadFavourites();
        loadAnswered();
        loadRated();
        loadHighScore();
        loadInitialArray();
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
                if (dataSnapshot != null) {
                    newMessage = dataSnapshot.getValue().toString();
                }
                firebaseReference.child("questionBank").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        try {
                            JSONObject json = new JSONObject((String) dataSnapshot.getValue());
                            String theJSONText = json.toString();
                            Paper.book(PaperTags.BOOK).write(PaperTags.JSON_DATA, theJSONText);//save data to storage

                        } catch (JSONException e) {
                            e.printStackTrace();
                            EventBus.getDefault().post(new NotificationContent(NotificationKey.questionBankError, null, null));
                        }
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.updatedQuestionBank, null, null));
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        EventBus.getDefault().post(new NotificationContent(NotificationKey.questionBankError, firebaseError, null));
                    }
                });


            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                EventBus.getDefault().post(new NotificationContent(NotificationKey.questionBankError, firebaseError, null));
            }
        });


    }


    public void downloadingNewVersion() {

    }

    public void loadInitialArray() {

        ArrayList<LinkedHashMap<String,?>> temp = new ArrayList<>();
        for (Object q:jsonManager.allQuestions) {
            if(q instanceof LinkedHashMap){
                temp.add((LinkedHashMap<String, ?>) q);
            }else{
                System.out.println("loadInitialArray: CHECK ERROR HERE");
            }
        }
        initialQuestions = temp;
    }

    public boolean isQuestionLocked(LinkedHashMap<String,?> question){

        if (unlocked) {
            return false;
        }
        if (initialCategories.contains(String.valueOf(question.get("fromCategory"))) &&
                Integer.valueOf(question.get("position").toString()) <= 5){
            return false;
        }

        return true;

    }

    public void upgradePurchased() {
        currentUserReference.child("subscription").setValue(dateFormatter.format(new Date()));
        unlocked = true;
    }

    public void resetData() {

        currentUserReference.child("answered").removeValue();
        currentUserReference.child("favourited").removeValue();
        currentUserReference.child("ratedDontKnow").removeValue();
        currentUserReference.child("ratedSomewhat").removeValue();
        currentUserReference.child("ratedKnow").removeValue();
        currentUserReference.child("highscore").removeValue();
        allFavourites = new ArrayList<>();
        allDontKnow = new ArrayList<>();
        allAnswers = new ArrayList<>();
        allKnow = new ArrayList<>();
        allSomeWhat = new ArrayList<>();
        highScore = 0;

        EventBus.getDefault().post(new NotificationContent(NotificationKey.dataReset, null,null));
    }

    public void rateCurrentQuestionAs(int rating) {

        LinkedHashMap question = jsonManager.currentQuestion;

        //remove string from any previous rating (if applicable)

        String previousRatingRef = "";

        if (allSomeWhat.contains(question) ) {

            previousRatingRef = "ratedSomewhat";

        } else if (allKnow.contains(question)) {

            previousRatingRef = "ratedKnow";

        } else if (allDontKnow.contains(question)) {

            previousRatingRef = "ratedDontKnow";
        }

        String ratingRef = "";

        if (rating == 1) {//dont know

            ratingRef = "ratedDontKnow";
        }
        else if (rating == 2) {//somewhat
            ratingRef = "ratedSomewhat";

        }
        else if (rating == 3) {//know
            ratingRef = "ratedKnow";
        }

        if (previousRatingRef.length() > 0) {
            removeRating(String.valueOf(question.get("id")), previousRatingRef, ratingRef);
        } else {
            addRating(ratingRef,String.valueOf(question.get("id")));
        }

    }

    public void removeRating(final String questionId, final String previousRef, final String newRef) {
        currentUserReference.child(previousRef).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //check it may return hashmap i am not sure yet bestieee:p
                ArrayList array = (ArrayList) dataSnapshot.getValue();
                array.remove(questionId);
                currentUserReference.child(previousRef).setValue(array, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if(firebaseError != null){
                            //print("Error: \(error!) \(error!.userInfo)")
                        }else{
                            addRating(newRef, questionId);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void addRating(final String ratingRef, final String questionId) {
        currentUserReference.child(ratingRef).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> array = new ArrayList<String>();

                if (dataSnapshot.exists()) {
                    array = (ArrayList<String>) dataSnapshot.getValue(); // i am sure this will fire class cast
                    array.add(questionId);

                } else {

                    array = new ArrayList<String>(Arrays.asList(questionId));
                }
                currentUserReference.child(ratingRef).setValue(new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if(firebaseError != null){

                        }else{
                            loadRated();
                        }
                    }
                });
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public int getCurrentQuestionRating() {
        LinkedHashMap question = jsonManager.currentQuestion;

        //remove string from any previous rating (if applicable)
        if (allSomeWhat.contains(question) ) {

            return 2;

        } else if (allKnow.contains(question)) {

            return 3;

        } else if (allDontKnow.contains(question)) {

            return 1;
        }
        return 0;
    }

    public void dentalProtectionCheck( String memberID){//,var initials:String) {
        final String mem = memberID.toUpperCase().replaceAll(" ","").trim();
        //initials = initials.uppercaseString.stringByTrimmingCharactersInSet(NSCharacterSet.whitespaceCharacterSet())
        firebaseReference.child("dentalProtection/"+mem+"/redeemedBy").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && String.valueOf(dataSnapshot.getValue()).length() == 0){
                    firebaseReference.child("dentalProtection/" + mem + "/redeemedBy").setValue(firebaseReference.getAuth().getUid());
                    EventBus.getDefault().post(new NotificationContent(NotificationKey.validDentalProtection, null, null));
                    upgradePurchased();

                } else {
                    EventBus.getDefault().post(new NotificationContent(NotificationKey.invalidDentalProtection,null,null));
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                EventBus.getDefault().post(new NotificationContent(NotificationKey.invalidDentalProtection,null,null));
            }
        });

    }

    //~~~~~

    public void checkLockStatus() {
        /*
        unlocked = true
        return;
            //comment out above for production
          */


        //dateFormatter.dateFormat = "dd/MM/yyyy";
        currentUserReference.child("subscription").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {

                    String dateString  = String.valueOf(dataSnapshot.getValue());
                    Date date = null;
                    try {
                        date = dateFormatter.parse(dateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    if (daysFrom(date) > 365) {
                        subscriptionString = "Your subscription has expired";
                        unlocked = false;
                        //print("Questions Locked - expired")
                    } else {


                        subscriptionString = "You subscribed on "+dateString;
                        unlocked = true;
                        //print("All questions unlocked")
                    }



                } else {
                    unlocked = false;
                    subscriptionString = "No subscription";
                    //print("Questions Locked")

                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
        
         

    }


    public long daysFrom(Date date){
        long diff = Calendar.getInstance().getTimeInMillis() - date.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    // MARK: Retrieving Data

    public void loadHighScore() {
        currentUserReference.child("highscore").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    highScore = Integer.valueOf(String.valueOf(dataSnapshot.getValue()));
                }else{
                    highScore = 0;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }


    public void loadFavourites() {
        currentUserReference.child("favourited").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    allFavourites = getQuestionsWithIDs((ArrayList) dataSnapshot.getValue());

                    //print("Successfully loaded \(self.allFavourites.count) favourites")


                } else {
                    allFavourites = new ArrayList<Object>();

                    //print("No Favourites")

                }
                EventBus.getDefault().post(new NotificationContent(NotificationKey.recievedFavourites,allFavourites,null));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    public void loadRated() {
        currentUserReference.child("ratedDontKnow").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    allDontKnow = getQuestionsWithIDs((ArrayList) dataSnapshot.getValue());

                    //print("\(self.allDontKnow.count) questions rated as 'Don't Know'")

                } else {
                    allDontKnow = new ArrayList<Object>();

                    //print("Nothing rated as 'Don't Know'")

                }
                EventBus.getDefault().post(new NotificationContent(NotificationKey.refresh,null,null));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        currentUserReference.child("ratedSomewhat").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    allSomeWhat = getQuestionsWithIDs((ArrayList) dataSnapshot.getValue());

                    //print("\(self.allDontKnow.count) questions rated as 'Don't Know'")

                } else {
                    allSomeWhat = new ArrayList<Object>();

                    //print("Nothing rated as 'Don't Know'")

                }
                EventBus.getDefault().post(new NotificationContent(NotificationKey.refresh,null,null));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        currentUserReference.child("ratedKnow").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    allKnow = getQuestionsWithIDs((ArrayList) dataSnapshot.getValue());

                    //print("\(self.allDontKnow.count) questions rated as 'Don't Know'")

                } else {
                    allKnow = new ArrayList<Object>();

                    //print("Nothing rated as 'Don't Know'")

                }
                EventBus.getDefault().post(new NotificationContent(NotificationKey.refresh,null,null));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }

    public void loadAnswered() {

        currentUserReference.child("answered").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    allAnswers = getQuestionsWithIDs((ArrayList) dataSnapshot.getValue());

                    //print("\(self.allDontKnow.count) questions rated as 'Don't Know'")

                } else {
                    allAnswers = new ArrayList<Object>();

                    //print("Nothing rated as 'Don't Know'")

                }
                EventBus.getDefault().post(new NotificationContent(NotificationKey.recievedAnswers,null,null));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    public ArrayList<LinkedHashMap> getQuestionsWithIDs(ArrayList ids){
        ArrayList<LinkedHashMap> array = new ArrayList<>();

        for (Object q : jsonManager.allQuestions){
            if(q instanceof LinkedHashMap) {
                if (ids.contains(((LinkedHashMap)q).get("id"))){
                    array.add((LinkedHashMap) q);
                }
            }
        }

        return array;

    }

    public ArrayList loadAnsweredFromCategory(String category){

        ArrayList array = new ArrayList();
        for (Object q:allAnswers){
            if(q instanceof ArrayList){
                if(((ArrayList)q).contains(category)){
                    array.add(q);
                }
            }else if(q instanceof LinkedHashMap){
                if(((LinkedHashMap) q).containsKey(category)){
                    array.add(q);
                }
            }
        }
        return array;
    }

    public ArrayList loadQuestionsFromCategory(String category){

        ArrayList array = new ArrayList();
        for (Object q:jsonManager.allQuestions){
            if(q instanceof ArrayList){
                if(((ArrayList)q).contains(category)){
                    array.add(q);
                }
            }else if(q instanceof LinkedHashMap){
                if(((LinkedHashMap) q).containsKey(category)){
                    array.add(q);
                }
            }
        }
        return array;
    }


    public ArrayList loadFavouritesFromCategory(String category){

        ArrayList array = new ArrayList();
        for (Object q:allFavourites){
            if(q instanceof ArrayList){
                if(((ArrayList)q).contains(category)){
                    array.add(q);
                }
            }else if(q instanceof LinkedHashMap){
                if(((LinkedHashMap) q).containsKey(category)){
                    array.add(q);
                }
            }
        }
        return array;
    }

    public boolean checkIfAnsweredBefore(Object question){
        for(Object a : allAnswers){
            if (((LinkedHashMap)a).get("id").equals(((LinkedHashMap)a).get("id"))) {
                return true;
            }
        }
        return false;
    }

    public boolean isHighScore(int score){
        if (score > highScore) {

            highScore = score;

            currentUserReference.child("highscore").setValue(highScore);

            return true;
        }
        return false;

    }

    public void newAnswer(final LinkedHashMap answer) {

        ArrayList array1 = loadAnsweredFromCategory(String.valueOf(answer.get("fromCategory")));
        ArrayList array2 = loadQuestionsFromCategory(String.valueOf(answer.get("fromCategory")));

        if (array1.size()+1 == array2.size()) {
            EventBus.getDefault().post(new NotificationContent(NotificationKey.categoryComplete,null,null));
        }
        currentUserReference.child("answered").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {

                    ArrayList<String> array = (ArrayList<String>) answer.get("id");
                    currentUserReference.child("answered").setValue(array, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                //print("Error: \(error!) \(error!.userInfo)")
                            } else {
                                //print("Answer successfully saved");
                                loadAnswered();
                            }
                        }
                    });

                } else {

                    ArrayList<String> array  = (ArrayList<String>) dataSnapshot.getValue();

                    array.addAll((Collection<? extends String>) answer.get("id"));

                    currentUserReference.child("answered").setValue(array, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                //print("Error: \(error!) \(error!.userInfo)")
                            } else {
                                //print("Answer successfully saved");
                                loadAnswered();
                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });


    }

    public boolean checkIfFavourited(Object question ){


        for(Object a :allFavourites) {
            if(((LinkedHashMap)a).get("id").equals(((LinkedHashMap)question).get("id"))){
                return true;
            }
        }

        return false;
    }

    public void favourite(final LinkedHashMap question) {

        currentUserReference.child("favourited").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    ArrayList<String> array = (ArrayList<String>) question.get("id");
                    currentUserReference.child("favourited").setValue(array, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                //print("Error: \(error!) \(error!.userInfo)")
                            } else {
                                //print("Favourite successfully saved");
                                loadFavourites();
                            }
                        }
                    });


                }else{
                    ArrayList array = (ArrayList) dataSnapshot.getValue();

                    if(checkIfFavourited(question)){

                        array.remove(question.get("id"));


                    }else{
                        array.add(question.get("id"));;
                    }

                    currentUserReference.child("favourited").setValue(array, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                //print("Error: \(error!) \(error!.userInfo)")
                            } else {
                                //print("Favourite successfully saved");
                                loadFavourites();
                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }
}
