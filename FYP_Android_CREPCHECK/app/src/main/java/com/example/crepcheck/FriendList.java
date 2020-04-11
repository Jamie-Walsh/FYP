package com.example.crepcheck;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import de.hdodenhof.circleimageview.CircleImageView;

public class FriendList extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    DatabaseReference FriendsRef, CurrentFriend, UserRef;
    String CurrentUID;
    RecyclerView friendsList;
    Query SearchQuery;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        friendsList = findViewById(R.id.friendsList);

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.FriendList_Title));

        CurrentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FriendsRef = FirebaseDatabase.getInstance().getReference().child("friends").child(CurrentUID);
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");

        // Give the layout a fixed size to avoid expensive layout operations
        friendsList.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager(FriendList.this);
        LayoutManager.setStackFromEnd(true);
        LayoutManager.setReverseLayout(true);
        friendsList.setLayoutManager(LayoutManager);
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

    @Override
    protected void onStart() {
        super.onStart();

        SearchQuery = FriendsRef;

        FirebaseRecyclerOptions<Friends> options =
                new FirebaseRecyclerOptions.Builder<Friends>()
                        .setQuery(SearchQuery, Friends.class)
                        .build();


        FirebaseRecyclerAdapter<Friends, FriendList.UserHolder> FBRecAdapter =
                new FirebaseRecyclerAdapter<Friends, FriendList.UserHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final FriendList.UserHolder holder, final int position, @NonNull final Friends model) {
                        // Save Favourites keys so you can edit and delete the correct favourite.
                        final String UID = getRef(position).getKey();
                        CurrentFriend = UserRef.child(UID);
                        CurrentFriend.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    String username = dataSnapshot.child("username").getValue().toString();
                                    String profilePic = dataSnapshot.child("profile_pic").getValue().toString();
                                    String bio = dataSnapshot.child("user_bio").getValue().toString();

                                    if(profilePic.equals("")){
                                        Picasso.get().load(R.drawable.img_profile_template).into(holder.ProfilePic);
                                    } else {
                                        Picasso.get().load(profilePic).into(holder.ProfilePic);
                                    }
                                    holder.username.setText(username);
                                    holder.user_bio.setText(bio);

                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            // Open the user's profile page.
                                            Intent toProfile = new Intent(FriendList.this, ProfilePage.class);
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
                    public FriendList.UserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_template, parent, false);
                        FriendList.UserHolder UserHolder = new FriendList.UserHolder(mView);
                        return UserHolder;
                    }
                };
        friendsList.setAdapter(FBRecAdapter);
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