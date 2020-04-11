package com.example.crepcheck;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfilePage extends AppCompatActivity {
    // Attributes
    private Toaster myToast = new Toaster(this);
    private CircleImageView profilePic;
    private TextView profileUsername, profileBio;
    private RecyclerView profilePosts;
    private ImageButton editProfileBtn;
    private Button AddFriendBtn, DeclineFriendRequestBtn;

    private DatabaseReference PostRef, UserRef, ProfileOwnerRef, CurrentPost, LikesRef, FriendRequestRef, FriendsRef;
    private Query UserPostsQuery;
    private String CurrentUser, ProfileOwnerID, profile_pic, CurrentFriendState;
    private String CurrentDate, CurrentTime;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser CurUser;
    private FirebaseRecyclerAdapter<Posts, ProfilePage.ProfilePostsHolder> FBRecAdapter;
    private Boolean Liked = false;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        profilePic = findViewById(R.id.profile_pic);
        profileUsername = findViewById(R.id.profile_username);
        profileBio = findViewById(R.id.profile_bio);
        AddFriendBtn = findViewById(R.id.profile_add_friend_btn);
        DeclineFriendRequestBtn = findViewById(R.id.profile_decline_friend_btn);
        CurrentFriendState = "strangers";

        PostRef = FirebaseDatabase.getInstance().getReference().child("posts");
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("likes");
        FriendRequestRef = FirebaseDatabase.getInstance().getReference().child("friend_requests");
        FriendsRef = FirebaseDatabase.getInstance().getReference().child("friends");

        mFirebaseAuth = FirebaseAuth.getInstance();
        CurUser = mFirebaseAuth.getCurrentUser();
        CurrentUser = CurUser.getUid();
        editProfileBtn = findViewById(R.id.edit_profile_btn);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ProfileOwnerID = extras.getString("UID");
            ProfileOwnerRef = UserRef.child(ProfileOwnerID);
            ProfileOwnerRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        profile_pic = dataSnapshot.child("profile_pic").getValue().toString();

                        if (profile_pic.isEmpty()) {
                            Picasso.get().load(R.drawable.img_profile_template).into(profilePic);
                        } else {
                            Picasso.get().load(profile_pic).placeholder(R.drawable.img_profile_template).into(profilePic);
                        }
                        profileUsername.setText(dataSnapshot.child("username").getValue().toString());
                        profileBio.setText(dataSnapshot.child("user_bio").getValue().toString());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ProfileError", databaseError.toString());
                    myToast.makeToast("Could not locate profile");
                    ProfilePage.this.finish();
                }
            });

            // Query to return the posts created by the profile owner
            UserPostsQuery = PostRef.orderByChild("UID").equalTo(ProfileOwnerID);

            // Only show edit profile button if current user is on their own profile
            if (CurrentUser.equals(ProfileOwnerID)) {
                editProfileBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent EditProfile = new Intent(ProfilePage.this, EditProfileActivity.class);
                        EditProfile.putExtra("UID", ProfileOwnerID);
                        startActivity(EditProfile);
                    }
                });
            }
            else{
                // Hide and disable the edit button
                editProfileBtn.setVisibility(View.GONE);
                editProfileBtn.setEnabled(false);
            }
        }

        // Set the FavList variable to the required xml RecyclerView
        profilePosts = findViewById(R.id.profile_users_posts);
        // Give the layout a fixed size to avoid expensive layout operations
        profilePosts.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager( ProfilePage.this);
        LayoutManager.setReverseLayout(true);
        LayoutManager.setStackFromEnd(true);
        profilePosts.setLayoutManager(LayoutManager);

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.ProfilePage_Title));

        DeclineFriendRequestBtn.setVisibility(View.GONE);
        DeclineFriendRequestBtn.setEnabled(false);

        // Get the current friend request status between the current user and profile owner
        SetFriendButtons();

        // A user cannot send themselves friend requests
        if(CurrentUser.equals(ProfileOwnerID))
        {
            // Disable all friend buttons if current user is on their own profile
            AddFriendBtn.setVisibility(View.GONE);
            AddFriendBtn.setEnabled(false);
            DeclineFriendRequestBtn.setVisibility(View.GONE);
            DeclineFriendRequestBtn.setEnabled(false);
        }
        else
        {
            AddFriendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AddFriendBtn.setEnabled(false);
                    if(CurrentFriendState.equals("strangers"))
                    {
                        AddFriend();
                    }
                    if(CurrentFriendState.equals("request_sent"))
                    {
                        CancelRequest();
                    }
                    if(CurrentFriendState.equals("request_received"))
                    {
                        AcceptRequest();
                    }
                    if(CurrentFriendState.equals("friends"))
                    {
                        DeleteFriend();
                    }
                }
            });
        }
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
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Posts> options =
                new FirebaseRecyclerOptions.Builder<Posts>()
                        .setQuery(UserPostsQuery, Posts.class)
                        .build();


        FBRecAdapter =
                new FirebaseRecyclerAdapter<Posts, ProfilePage.ProfilePostsHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ProfilePage.ProfilePostsHolder holder, final int position, @NonNull final Posts model) {
                        // Save Favourites keys so you can edit and delete the correct favourite.
                        final String PostID = getRef(position).getKey();
                        CurrentPost = PostRef.child(PostID);
                        CurrentPost.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    UserRef.child(ProfileOwnerID).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                            {
                                                String profilePic = dataSnapshot.child("profile_pic").getValue().toString();

                                                if(profilePic.equals("")){
                                                    Picasso.get().load(R.drawable.img_profile_template).into(holder.ProfilePic);
                                                } else {
                                                    Picasso.get().load(profilePic).into(holder.ProfilePic);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                                    holder.username.setText(model.getUsername());
                                    holder.date.setText(model.getPost_date());
                                    holder.time.setText(model.getPost_time());
                                    holder.caption.setText(model.getPost_caption());

                                    Picasso.get().load(model.getPost_img_uri()).placeholder(R.drawable.img_profile_template).into(holder.post_img);
                                    String profilePicUri = model.getProfile_pic();
                                    // If the user has not uploaded a profile picture yet...
                                    // set the profile pic to the placeholder image
                                    if (profilePicUri.isEmpty()) {
                                        Picasso.get().load(R.drawable.img_profile_template).into(holder.ProfilePic);
                                    } else {
                                        Picasso.get().load(profilePicUri).placeholder(R.drawable.img_profile_template).into(holder.ProfilePic);
                                    }

                                    // Only allow users who own the post in question to edit or delete it.
                                    if (CurrentUser.equals(ProfileOwnerID)) {
                                        // Enable the button
                                        holder.editPost.setVisibility(View.VISIBLE);
                                        // Make the button visible
                                        holder.editPost.setEnabled(true);
                                        holder.editPost.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Intent EditPost = new Intent(ProfilePage.this, EditPostActivity.class);
                                                EditPost.putExtra("PostID", PostID);
                                                startActivity(EditPost);
                                            }
                                        });
                                    }

                                    holder.setLikeStatus(PostID);

                                    holder.likeBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            // Change like icon to red
                                            Liked = true;

                                            LikesRef.addValueEventListener(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (Liked)
                                                    {
                                                        // If the current user has already liked the current post
                                                        if(dataSnapshot.child(PostID).hasChild(CurrentUser))
                                                        {
                                                            // Unlike the post by removing the user id
                                                            LikesRef.child(PostID).child(CurrentUser).removeValue();
                                                            Liked = false; // Change icon back to white
                                                        }
                                                        else {
                                                            LikesRef.child(PostID).child(CurrentUser).setValue(true);
                                                            Liked = false;
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    });

                                    holder.commentBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent AddComment = new Intent(ProfilePage.this, CommentsActivity.class);
                                            AddComment.putExtra("TYPE", "POST");
                                            AddComment.putExtra("PostID", PostID);
                                            startActivity(AddComment);
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("CurrentPostListener", databaseError.toString());
                                myToast.makeToast("Couldn't Find Post");
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ProfilePage.ProfilePostsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_template, parent, false);
                        ProfilePage.ProfilePostsHolder postHolder = new ProfilePage.ProfilePostsHolder(mView);
                        return postHolder;
                    }
                };
        profilePosts.setAdapter(FBRecAdapter);
        FBRecAdapter.startListening();
    }

    public static class ProfilePostsHolder extends RecyclerView.ViewHolder
    {
        CircleImageView ProfilePic;
        TextView username, date, time, caption, likeCount;
        ImageView post_img;
        ImageButton editPost, likeBtn, commentBtn;
        int likeCounter;
        String currentUID;
        DatabaseReference LikeRef;

        private ProfilePostsHolder(View itemView)
        {
            super(itemView);

            ProfilePic = itemView.findViewById(R.id.post_profilePic);
            username = itemView.findViewById(R.id.post_userName);
            date = itemView.findViewById(R.id.post_date_tv);
            time = itemView.findViewById(R.id.post_time_tv);
            caption = itemView.findViewById(R.id.post_caption_tv);
            post_img = itemView.findViewById(R.id.post_image);
            likeBtn = itemView.findViewById(R.id.post_like_btn);
            commentBtn = itemView.findViewById(R.id.post_comment_btn);
            likeCount = itemView.findViewById(R.id.post_like_count);
            editPost = itemView.findViewById(R.id.post_options);
            editPost.setEnabled(false);
            editPost.setVisibility(View.INVISIBLE);

            LikeRef = FirebaseDatabase.getInstance().getReference().child("likes");
            currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        private void setLikeStatus(final String PostID)
        {
            LikeRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // If the user liked the post
                    if(dataSnapshot.child(PostID).hasChild(currentUID))
                    {
                        // Count the amount of users who liked the relevant post
                        likeCounter = (int) dataSnapshot.child(PostID).getChildrenCount();
                        likeBtn.setImageResource(R.drawable.ic_like);
                        if (likeCounter == 1){
                            likeCount.setText((Integer.toString(likeCounter)) + (" Like"));
                        }
                        else {
                            likeCount.setText((Integer.toString(likeCounter)) + (" Likes"));
                        }
                    }
                    else
                    {
                        // Count the amount of users who liked the relevant post
                        likeCounter = (int) dataSnapshot.child(PostID).getChildrenCount();
                        likeBtn.setImageResource(R.drawable.ic_unliked);

                        if (likeCounter == 1){
                            likeCount.setText((Integer.toString(likeCounter)) + (" Like"));
                        }
                        else {
                            likeCount.setText((Integer.toString(likeCounter)) + (" Likes"));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    public void AddFriend()
    {
        FriendRequestRef.child(CurrentUser).child(ProfileOwnerID).child("request_type").setValue("request_sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            FriendRequestRef.child(ProfileOwnerID).child(CurrentUser).child("request_type").setValue("request_received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                AddFriendBtn.setEnabled(true);
                                                CurrentFriendState = "request_sent";
                                                AddFriendBtn.setText(getResources().getString(R.string.cancel_request));

                                                DeclineFriendRequestBtn.setVisibility(View.GONE);
                                                DeclineFriendRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    public void SetFriendButtons()
    {
        FriendRequestRef.child(CurrentUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // If the current user has sent a request to the profile owner
                if(dataSnapshot.hasChild(ProfileOwnerID))
                {
                    // Get the request type
                    String currentRequestStatus = dataSnapshot.child(ProfileOwnerID)
                            .child("request_type").getValue().toString();

                    // If the request was sent, set the text to "Cancel Request" and hide the decline button
                    if(currentRequestStatus.equals("request_sent"))
                    {
                        CurrentFriendState = "request_sent";
                        AddFriendBtn.setText(getResources().getString(R.string.cancel_request));
                        DeclineFriendRequestBtn.setVisibility(View.GONE);
                        DeclineFriendRequestBtn.setEnabled(false);
                    }
                    else if(currentRequestStatus.equals("request_received"))
                    {
                        CurrentFriendState = "request_received";
                        AddFriendBtn.setText(getResources().getString(R.string.accept_request));
                        DeclineFriendRequestBtn.setVisibility(View.VISIBLE);
                        DeclineFriendRequestBtn.setEnabled(true);

                        DeclineFriendRequestBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                CancelRequest();
                            }
                        });
                    }
                }
                else {
                    FriendsRef.child(CurrentUser).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(ProfileOwnerID))
                            {
                                CurrentFriendState = "friends";
                                AddFriendBtn.setText(getResources().getString(R.string.unfriend));
                                DeclineFriendRequestBtn.setVisibility(View.GONE);
                                DeclineFriendRequestBtn.setEnabled(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void CancelRequest()
    {
        FriendRequestRef.child(CurrentUser).child(ProfileOwnerID).removeValue()
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        FriendRequestRef.child(ProfileOwnerID).child(CurrentUser).removeValue()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful())
                                        {
                                            AddFriendBtn.setEnabled(true);
                                            CurrentFriendState = "strangers";
                                            AddFriendBtn.setText(getResources().getString(R.string.add_friend_btn));

                                            DeclineFriendRequestBtn.setVisibility(View.GONE);
                                            DeclineFriendRequestBtn.setEnabled(false);
                                        }
                                    }
                                });
                    }
                }
            });
    }
    public void AcceptRequest()
    {
        // Date
        Calendar calDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
        CurrentDate = currentDate.format(calDate.getTime());

        // Add the profile owner to the current user's friends
        FriendsRef.child(CurrentUser).child(ProfileOwnerID).child("date").setValue(CurrentDate)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        // Add the current user to the profile owner's friends
                        FriendsRef.child(ProfileOwnerID).child(CurrentUser).child("date").setValue(CurrentDate)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful())
                                    {
                                        FriendRequestRef.child(CurrentUser).child(ProfileOwnerID).removeValue()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful())
                                                    {
                                                        FriendRequestRef.child(ProfileOwnerID).child(CurrentUser).removeValue()
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful())
                                                                    {
                                                                        AddFriendBtn.setEnabled(true);
                                                                        CurrentFriendState = "friends";
                                                                        AddFriendBtn.setText(getResources().getString(R.string.unfriend));

                                                                        DeclineFriendRequestBtn.setVisibility(View.GONE);
                                                                        DeclineFriendRequestBtn.setEnabled(false);
                                                                    }
                                                                }
                                                            });
                                                    }
                                                }
                                            });
                                    }
                                }
                            });
                    }
                }
            });
    }

    public void DeleteFriend()
    {
        // Delete the profile owner from the current user's friend list
        FriendsRef.child(CurrentUser).child(ProfileOwnerID).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            // Delete the current user from the profile owner's friend list
                            FriendsRef.child(ProfileOwnerID).child(CurrentUser).removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful())
                                            {
                                                AddFriendBtn.setEnabled(true);
                                                CurrentFriendState = "strangers";
                                                AddFriendBtn.setText(getResources().getString(R.string.add_friend_btn));

                                                DeclineFriendRequestBtn.setVisibility(View.GONE);
                                                DeclineFriendRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}