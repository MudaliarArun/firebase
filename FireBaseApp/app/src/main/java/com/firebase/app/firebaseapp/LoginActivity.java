package com.firebase.app.firebaseapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.paperdb.Paper;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity  {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private static final String USER_TOKEN = "userToken";
    private static final String PROVIDER = "firebaseProvider";

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    //all you can get help in detail is here
    Firebase myFirebaseRef;
    private SharedPreferences mSharedPreferences;
    private String USER_ID ="uid";
    private LinearLayout linearLayoutSignin;
    private LinearLayout linearLayoutData;
    private TextView mTextViewRead;
    private Button mButtonRead;
    private Button mButtonWrite;
    private EditText mEditTextWrite;
    String readDataTemp ="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Firebase declare
        Firebase.setAndroidContext(this);
        Paper.init(this);
        //myFirebaseRef = new Firebase("https://<YOUR-FIREBASE-APP>.firebaseio.com/");
        myFirebaseRef = new Firebase("https://demoonlivedata.firebaseio.com/");//.firebaseIO.com
        myFirebaseRef = new Firebase("https://dentaliq.firebaseio.com");//.firebaseIO.com
        //Firebase ends


        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);


        // UI for firebase
        linearLayoutSignin = (LinearLayout)findViewById(R.id.email_login_form);

        linearLayoutData = (LinearLayout)findViewById(R.id.ll_userdata);

        mTextViewRead = (TextView)findViewById(R.id.tv_data);
        mButtonRead = (Button)findViewById(R.id.btn_read_data);
        mButtonRead.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                readData("batters", new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //i have used the hashmap, but you can iterrate over dataSnapshot
                        // iterating method is dataSnapshot.getChildren()
                        // i.e like a for loop but its iteration not a for loop

                        HashMap<String,?> map = (HashMap<String, ?>) dataSnapshot.getValue();
                        // i am casting without checks as i know the Json structure

                        if(map.get("batter") instanceof ArrayList) {
                            ArrayList<HashMap<String, ?>> batters = (ArrayList<HashMap<String, ?>>) map.get("batter");
                            readDataTemp = "";
                            for (int i = 0; i < batters.size(); i++) {
                                HashMap<String, ?> batter = batters.get(i);
                                if(batter != null) {
                                    readDataTemp += (String) batter.get("id");
                                    readDataTemp += "   " + batter.get("type");
                                    readDataTemp += "\n";
                                }
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextViewRead.setText(readDataTemp);
                                }
                            });
                        }
                        System.out.println(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println(firebaseError);
                    }
                });
            }
        });

        mButtonWrite = (Button)findViewById(R.id.btn_write_data);
        mEditTextWrite = (EditText)findViewById(R.id.edt_data);
        mButtonWrite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
               final String dataToWrite = mEditTextWrite.getText().toString();

                // adding data
                Map<String, Object> dataBatter = new HashMap<>();
                dataBatter.put("id", "1005");
                dataBatter.put("type", dataToWrite);

                Map<String, Object> dataBatterWrap = new HashMap<>();
                dataBatterWrap.put("6",dataBatter); // here 6 must be maintained incremental


                Firebase refBatter  = myFirebaseRef.child("batters").child("batter").getRef();
                refBatter.updateChildren(dataBatterWrap, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        if(firebaseError!= null){
                            showAlert("Error Updating" + firebaseError);
                        }else{
                            showAlert("Updated " + firebase);
                        }

                    }
                });



                //Updating data
                //update a key already present named "name"
                /*Firebase refBatter  = myFirebaseRef.child("name");
                refBatter.setValue(dataToWrite, new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        showAlert("Updated " + firebaseError + " " + firebase);
                    }
                });*/


                //dont fall into this its for RnD yet ERRoRRRRRRRRRRRRR
                // adding batter working
                /*Firebase refBatter  = myFirebaseRef.child("batters").child("batter").getRef();
                refBatter.push().setValue(new batter("1005",dataToWrite), new Firebase.CompletionListener() {
                    @Override
                    public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                        showAlert("Updated " + firebaseError + " " + firebase);
                    }
                });*/

            }
        });



        
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }



    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

           createFireBaseUser(mEmail, mPassword, new Firebase.ValueResultHandler<Map<String, Object>>() {
               @Override
               public void onSuccess(Map<String, Object> result) {
                   saveStringToPref(USER_ID, (String) result.get("uid"));
                   showAuthenticatedAlert("User Created Successfully");
                   mAuthTask = null;
                   showProgress(false);
                   //finish();
               }

               @Override
               public void onError(FirebaseError firebaseError) {

                    // Just assumed, did not get time to create a new ui, so have assumed error may occur
                   // so attempting to login in case the user id already exist
                   AuthenticateFireBaseUser(mEmail, mPassword, new Firebase.AuthResultHandler() {
                       @Override
                       public void onAuthenticated(AuthData authData) {
                           mAuthTask = null;
                           showProgress(false);
                           saveStringToPref(USER_TOKEN,authData.getToken());
                           saveStringToPref(PROVIDER,authData.getProvider());
                           saveStringToPref(USER_ID, authData.getUid());
                           showAuthenticatedAlert("User Authenticated");

                       }

                       @Override
                       public void onAuthenticationError(FirebaseError firebaseError) {
                           mAuthTask = null;
                           showProgress(false);
                           mPasswordView.setError(firebaseError.getMessage());//getString(R.string.error_incorrect_password)
                           mPasswordView.requestFocus();
                       }
                   });

               }
           });

            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    /**
     * writes data to firebase server for the given key & store the given value
     * @param key
     * @param value
     */
    public void writeData(String key,String value, Firebase.CompletionListener completionListener){
        myFirebaseRef.child(key).setValue(value, completionListener);
    }

    /**
     *
     * @param key
     * @param valueEventListener
     */
    public void readData(String key,ValueEventListener valueEventListener){
        myFirebaseRef.child(key).addValueEventListener(valueEventListener);
    }
    public void readUserName(){
        readData("message",
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."
                    }

                    @Override
                    public void onCancelled(FirebaseError error) {
                    }

                });
    }

    public void createFireBaseUser(String userName, String password, Firebase.ValueResultHandler valueResultHandler){
        myFirebaseRef.createUser(userName, password, valueResultHandler);
    }

    public void AuthenticateFireBaseUser(String userName, String password, Firebase.AuthResultHandler authResultHandler){
        myFirebaseRef.authWithPassword(userName, password, authResultHandler);
    }

    /**
     * Init SharedPreferences
     */
    private void initPref(){
        if(mSharedPreferences  == null)
            mSharedPreferences  = getSharedPreferences(getPackageName(),MODE_PRIVATE);
    }
    public void saveStringToPref(String key,String value){
        initPref();
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString(key,value);
        mEditor.commit();
    }

    /**
     * get data based on key form preference
     * @param key
     * @return Object cast as per your need
     */
    public Object getPref(String key){
        initPref();
        return mSharedPreferences.getAll().get(key);
    }

    public void showAuthenticatedAlert(String message){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showDataScreen();
            }
        });
        alertDialog.create().show();
    }

    public void showAlert(String message){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.create().show();
    }

    /**
     * shows login screen just visibility changed
     */
    public void showLoginScreen(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayoutSignin.setVisibility(View.VISIBLE);
                linearLayoutData.setVisibility(View.GONE);
            }
        });

    }

    /**
     * shows data screen just visibility changed
     */
    public void showDataScreen(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayoutSignin.setVisibility(View.GONE);
                linearLayoutData.setVisibility(View.VISIBLE);
            }
        });

    }
}

