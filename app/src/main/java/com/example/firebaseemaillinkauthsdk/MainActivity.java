package com.example.firebaseemaillinkauthsdk;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText emailAddressEditText = null;
    private Button signInButton = null;
    private String emailAddress = null;
    private String emailLink = null;
    private String mPendingEmail;

    String TAG = "EMAIL_AUTH";
    FirebaseAuth auth;

    private SharedPreferences pref;
    private static final String KEY_PENDING_EMAIL = "Key_pending_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailAddressEditText = (EditText) findViewById(R.id.emailAddressEditText);
        signInButton = (Button) findViewById(R.id.signInButton);
        signInButton.setEnabled(false);

        auth = FirebaseAuth.getInstance();

        pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        mPendingEmail = pref.getString(KEY_PENDING_EMAIL,null);

        if (mPendingEmail != null){
            emailAddressEditText.setText(mPendingEmail);
            Log.d(TAG,"Getting shared preferences: " + mPendingEmail);
        }

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null){
            emailLink = intent.getData().toString();
            Log.d(TAG,"Got an intent: " + emailLink);

            //Confirm the link is a sign-in with email link
            if (auth.isSignInWithEmailLink(emailLink)){
                signInButton.setEnabled(true);
            }
        }
    }

    public void onEmailClick(View view){
        emailAddress = emailAddressEditText.getText().toString();

        if (TextUtils.isEmpty(emailAddress)){
            //If email address empty, tell user to enter email address
            Toast.makeText(getApplicationContext(),"Enter email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        //Save email address
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(KEY_PENDING_EMAIL, emailAddress);
        editor.commit();

        //Specific to Firebase. For catching dynamic link
        ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                .setAndroidPackageName(
                        getPackageName(),
                        false, /* Install if not available? */
                        null)     /* Minimum app version */
                .setHandleCodeInApp(true)
                //The url is very vital, it is the url that was set in Auth section in Firebase
                .setUrl("https://auth.example2.com/emailSignInLink")
                .build();

                auth.sendSignInLinkToEmail(emailAddress, actionCodeSettings)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "Email sent", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
    }
    public void onSignInClickedButton(View view){
        emailAddress = emailAddressEditText.getText().toString();

        auth.signInWithEmailLink(emailAddress, emailLink)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //Remove shared preferences, set everything back to default
                        mPendingEmail = null;
                        SharedPreferences.Editor editor = pref.edit();
                        editor.remove(KEY_PENDING_EMAIL);
                        editor.commit();                    // Setting shared pref back to default
                        emailAddressEditText.setText("");

                        if (task.isSuccessful()){
                            Toast.makeText(getApplicationContext(),"Successfully signed in with email", Toast.LENGTH_LONG).show();
                        }else {
                            Toast.makeText(getApplicationContext(),"Error signing in with email link", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void reauthenticateUser(){
        // Construct the mail link credentials from the current URL
        AuthCredential credential = EmailAuthProvider.getCredentialWithLink(emailAddress, emailLink);

        //Link the credentials to the current user
        auth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG,"Successfully linked email link credentials!");

                            AuthResult result = task.getResult();
                            //You can access the new user via result.getUser()
                            //Additional user info profile *not* available Via:
                            //result.getAdditionalUserInfo().getProfile() == null
                            //You can check if the user is new or existing:
                            //result.getAdditionalUserInfo().isNewUser()
                        }else {
                            Log.e(TAG,"Error linking emailLink credential", task.getException());
                        }
                    }
                });
    }
}