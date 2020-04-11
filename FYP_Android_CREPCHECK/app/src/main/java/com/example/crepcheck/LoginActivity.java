package com.example.crepcheck;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    FirebaseAuth mFireBaseAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    EditText email, password;
    Button btnLogin;
    TextView tvSignUp;
    Toaster myToast = new Toaster(this);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mFireBaseAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.LoginEmail);
        password = findViewById(R.id.LoginPass);
        tvSignUp = findViewById(R.id.SignUpHere);
        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(i);
            }
        });
        btnLogin = findViewById(R.id.btnLogIn);
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser mFireBaseUser = mFireBaseAuth.getCurrentUser();
                if(mFireBaseUser != null){
                    myToast.makeToast(getResources().getString(R.string.already_logged_in));
                    SendToMainActivity();
                }
                else{
                    myToast.makeToast(getResources().getString(R.string.prompt_login));
                }
            }
        };

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strEmail = email.getText().toString();
                String pwd = password.getText().toString();

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
                else if(strEmail.isEmpty() && pwd.isEmpty())
                {
                    myToast.makeToast(getResources().getString(R.string.empty_fields));
                }
                else if(!(strEmail.isEmpty() && pwd.isEmpty()))
                {
                    mFireBaseAuth.signInWithEmailAndPassword(strEmail, pwd).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                myToast.makeToast(getResources().getString(R.string.login_unsuccessful));
                            }
                            else {
                                SendToMainActivity();
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

    @Override
    protected void onStart() {
        super.onStart();
        mFireBaseAuth.addAuthStateListener(mAuthStateListener);
    }

    public void SendToMainActivity()
    {
        Intent OpenMainActivity = new Intent(LoginActivity.this, MainActivity.class);
        // Choose which fragment to open when loading the main activity.
        OpenMainActivity.putExtra("fragToLoad", "ACCOUNT");
        // Prevent users from being able to go back to the login screen once they are logged in.
        OpenMainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(OpenMainActivity);
        // Finish the current activity to prevent the back button
        // from allowing users to return to the login screen.
        finish();
    }
}
