package com.example.crepcheck;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFriendsActivity extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    EditText searchBar;
    ImageButton searchBtn;
    DatabaseReference UserRef, CurrentUser;
    RecyclerView userList;
    Query SearchQuery;

    // Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friends);

        searchBar = findViewById(R.id.user_search_bar);
        searchBtn = findViewById(R.id.user_search_btn);
        userList = findViewById(R.id.user_search_list);
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.AddFriends_Title));

        // Give the layout a fixed size to avoid expensive layout operations
        userList.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager(AddFriendsActivity.this);
        userList.setLayoutManager(LayoutManager);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String usernameSearched = searchBar.getText().toString();
                SearchUsers(usernameSearched);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id== android.R.id.home) {
            // Close the activity when the back button is clicked
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void SearchUsers(String searchString)
    {
        myToast.makeToast("Searching for Users...");

        SearchQuery = UserRef.orderByChild("username").startAt(searchString)
                .endAt(searchString + "\uf8ff");

        FirebaseRecyclerOptions<FriendSearch> options =
                new FirebaseRecyclerOptions.Builder<FriendSearch>()
                        .setQuery(SearchQuery, FriendSearch.class)
                        .build();


        FirebaseRecyclerAdapter<FriendSearch, AddFriendsActivity.UserHolder> FBRecAdapter =
                new FirebaseRecyclerAdapter<FriendSearch, AddFriendsActivity.UserHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final AddFriendsActivity.UserHolder holder, final int position, @NonNull final FriendSearch model) {
                        // Save Favourites keys so you can edit and delete the correct favourite.
                        final String UID = getRef(position).getKey();
                        CurrentUser = UserRef.child(UID);
                        CurrentUser.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    String profilePic = dataSnapshot.child("profile_pic").getValue().toString();

                                    if(profilePic.equals("")){
                                        Picasso.get().load(R.drawable.img_profile_template).into(holder.ProfilePic);
                                    } else {
                                        Picasso.get().load(profilePic).into(holder.ProfilePic);
                                    }
                                    holder.username.setText(model.getUsername());
                                    holder.user_bio.setText(model.getUser_bio());

                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            // Open the user's profile page.
                                            Intent toProfile = new Intent(AddFriendsActivity.this, ProfilePage.class);
                                            toProfile.putExtra("UID",UID);
                                            startActivity(toProfile);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public AddFriendsActivity.UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_template, parent, false);
                        AddFriendsActivity.UserHolder UserHolder = new AddFriendsActivity.UserHolder(mView);
                        return UserHolder;
                    }
                };
        userList.setAdapter(FBRecAdapter);
        FBRecAdapter.startListening();
    }

    public static class UserHolder extends RecyclerView.ViewHolder
    {
        CircleImageView ProfilePic;
        TextView username, user_bio;

        private UserHolder(View itemView)
        {
            super(itemView);

            ProfilePic = itemView.findViewById(R.id.user_profile_pic);
            username = itemView.findViewById(R.id.user_username);
            user_bio = itemView.findViewById(R.id.user_bio);
        }
    }
}
