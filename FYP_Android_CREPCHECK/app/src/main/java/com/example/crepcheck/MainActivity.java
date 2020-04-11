package com.example.crepcheck;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // Attributes
    static final int REQUEST_CAPTURE = 1337;
    Toaster myToast = new Toaster(this);
    BottomNavigationView navBar;
    FirebaseAuth mFirebaseAuth;
    DatabaseReference UserRef;
    String CurrentUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navBar = findViewById(R.id.bottom_navigation);
        navBar.setOnNavigationItemSelectedListener(navListener);

        GetFragmentToShow(); // Gets extras from the intent that dictates which fragment to load

        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser CurUser = mFirebaseAuth.getCurrentUser();
        CurrentUser = CurUser.getUid();
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");
    }

    @Override
    protected void onStart() {
        super.onStart();

        GetFragmentToShow();
        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        // Ensure only authorized users can see this screen
        if(currentUser == null) {
            SendUserToLogin();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater Inflater = getMenuInflater();
        Inflater.inflate(R.menu.nav_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.nav_my_profile: {
                // Open the current user's profile page.
                Intent toProfile = new Intent(MainActivity.this, ProfilePage.class);
                toProfile.putExtra("UID",CurrentUser);
                startActivity(toProfile);
                return true;
            }
            case R.id.nav_new_post: {
                // Open the user's profile page.
                Intent toPost = new Intent(MainActivity.this, PostActivity.class);
                startActivity(toPost);
                return true;
            }
            case R.id.nav_friends: {
                // Open the user's friend list.
                Intent showFriends = new Intent(MainActivity.this, FriendList.class);
                startActivity(showFriends);
                return true;
            }
            case R.id.nav_add_friends: {
                // Open the user's profile page.
                Intent addFriends = new Intent(MainActivity.this, AddFriendsActivity.class);
                startActivity(addFriends);
                return true;
            }
            case R.id.nav_favourites: {
                // Open the Favourites page.
                Intent toFav = new Intent(MainActivity.this, FavouritesPage.class);
                startActivity(toFav);
                return true;
            }
            case R.id.nav_logout: {
                mFirebaseAuth.signOut();
                SendUserToLogin();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                    Fragment selectedFragment;
                    switch (menuItem.getItemId()) {
                        case R.id.btn_feed: {
                            selectedFragment = new FeedFragment();
                            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                            break;
                        }
                        case R.id.btn_camera: {
                            OpenUserCamera();
                            break;
                        }
                        case R.id.btn_account: {
                            selectedFragment = new AccountFragment();
                            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                            break;
                        }
                    }

                    return true;
                }
            };

    @Override
    public void onClick(View v)
    // Method that handles the button click events.
    {
        switch (v.getId())
        {
            case R.id.btn_camera:
            {
                OpenUserCamera();
                break;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // If the user captures an image and presses OK, save the bitmap.
        if (requestCode == REQUEST_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null){
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                // Send the bitmap to the ImageProcessor class to try identify the shoe.
                ImageProcessor Processor = new ImageProcessor(MainActivity.this);
                Processor.ProcessImage(imageBitmap);
            } else {
                myToast.makeToast("No URI Found");
            }
        }
    }

    public void SendUserToLogin()
    {
        Intent intToLogin = new Intent(MainActivity.this, LoginActivity.class);
        intToLogin.putExtra("finish", true); // checking for this in other Activities
        intToLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intToLogin);
        finish();
    }

    public void GetFragmentToShow() {
        // Find out which Fragment to load
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Fragment selectedFragment;
            String frag = extras.getString("fragToLoad");
            switch (frag.toUpperCase()) {
                case "FEED":
                    // Load the Feed fragment
                    selectedFragment = new FeedFragment();
                    navBar.setSelectedItemId(R.id.btn_feed);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                    break;
                case "ACCOUNT":
                    // Load the Account fragment
                    selectedFragment = new AccountFragment();
                    navBar.setSelectedItemId(R.id.btn_account);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
                    break;
            }
        }
    }

    private void OpenUserCamera()
    {
        Intent OpenCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (OpenCamera.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(OpenCamera, REQUEST_CAPTURE);
        } else {
            myToast.makeToast("Couldn't open camera.");
        }
    }
}
