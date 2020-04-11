package com.example.crepcheck;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {
    FirebaseAuth mFirebaseAuth;
    ProgressDialog loadingBar;
    DatabaseReference UserRef;
    FirebaseUser FbUser;
    EditText username, full_name, email, password, confirm_password;
    Button btnSignUp;
    TextView tvLogin;
    Toaster myToast = new Toaster(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mFirebaseAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);
        tvLogin = findViewById(R.id.LoginHere);
        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });
        username = findViewById(R.id.SUUsername);
        full_name = findViewById(R.id.SUFull_name);
        email = findViewById(R.id.SUEmail);
        password = findViewById(R.id.SUPass);
        confirm_password = findViewById(R.id.SUConfPass);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strUsername = username.getText().toString();
                String strFullName = full_name.getText().toString();
                String strEmail = email.getText().toString();
                String pwd = password.getText().toString();
                String confPwd = confirm_password.getText().toString();

                if(strUsername.isEmpty())
                {
                    username.setError(getResources().getString(R.string.empty_username));
                    username.requestFocus();
                }
                if(strFullName.isEmpty())
                {
                    full_name.setError(getResources().getString(R.string.empty_full_name));
                    full_name.requestFocus();
                }
                if(strEmail.isEmpty())
                {
                    email.setError(getResources().getString(R.string.empty_email));
                    email.requestFocus();
                }
                else if(pwd.isEmpty())
                {
                    password.setError(getResources().getString(R.string.empty_password));
                    password.requestFocus();
                }
                else if(confPwd.isEmpty())
                {
                    confirm_password.setError(getResources().getString(R.string.empty_password));
                    confirm_password.requestFocus();
                }
                else if(!pwd.equals(confPwd))
                {
                    myToast.makeToast(getResources().getString(R.string.mismatching_passwords));
                }
                else if((strUsername.isEmpty() && strFullName.isEmpty() && strEmail.isEmpty() && pwd.isEmpty() && confPwd.isEmpty()))
                {
                    myToast.makeToast(getResources().getString(R.string.empty_fields));
                }
                else if(!(strUsername.isEmpty() && strFullName.isEmpty() && strEmail.isEmpty() && pwd.isEmpty() && confPwd.isEmpty()))
                {
                    loadingBar.setTitle("Creating Account");
                    loadingBar.setMessage("Please wait while we set up your new Account...");
                    loadingBar.show();
                    loadingBar.setCanceledOnTouchOutside(true);

                    mFirebaseAuth.createUserWithEmailAndPassword(strEmail, pwd).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful())
                            {
                                myToast.makeLongToast("FireBase Error: " + task.getException().getMessage());
                                loadingBar.dismiss();
                            }
                            else {
                                // Add authenticated user to the realtime database
                                SaveUserToRealTimeDB();
                                SendUserToMainActivity();
                                myToast.makeToast("your Account is created Successfully.");
                                loadingBar.dismiss();
                            }
                        }
                    });
                }
                else{
                    myToast.makeToast(getResources().getString(R.string.unknown_error));
                }
            }
        });
    }

    public void SendUserToMainActivity()
    {
        Intent OpenMainActivity = new Intent(SignUpActivity.this,MainActivity.class);
        OpenMainActivity.putExtra("fragToLoad", "ACCOUNT");
        OpenMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(OpenMainActivity);
        finish();
    }

    private void SaveUserToRealTimeDB()
    {
        // Get the new user's Email and UID
        FbUser = mFirebaseAuth.getCurrentUser();
        String email = FbUser.getEmail();
        String UID = FbUser.getUid();

        // Save user's data to a HashMap
        HashMap data = new HashMap();
        data.put("username", username.getText().toString());
        data.put("fullname", full_name.getText().toString());
        data.put("email", email);
        data.put("profile_pic", "");
        data.put("profile_pic_name", "");
        data.put("user_bio", "Hey, I\'m using CREPCHECK!");

        // Add data to the real-time database
        UserRef = FirebaseDatabase.getInstance().getReference().child("users").child(UID);
        UserRef.setValue(data);
    }
}
