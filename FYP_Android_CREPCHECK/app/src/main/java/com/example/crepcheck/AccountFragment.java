package com.example.crepcheck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountFragment extends Fragment
{
    // Attributes
    Toaster myToast;
    private RecyclerView Timeline;
    private Context CurrentContext;
    private DatabaseReference PostRef, SinglePostRef, LikesRef, UserRef;
    private String CurrentUser, PostOwner;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseRecyclerAdapter<Posts, PostsHolder> Adapter;
    private Boolean Liked = false;

    //Methods
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Get the context for the fragment before onCreateView to avoid memory leaks
        CurrentContext = context;
        myToast = new Toaster(CurrentContext);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View myView = inflater.inflate(R.layout.account_frag, container, false);

        UserRef = FirebaseDatabase.getInstance().getReference().child("users");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("likes");
        PostRef = FirebaseDatabase.getInstance().getReference().child("posts");

        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser CurUser = mFirebaseAuth.getCurrentUser();
        CurrentUser = CurUser.getUid();

        // Set the Timeline variable to the required xml RecyclerView
        Timeline = myView.findViewById(R.id.Timeline);
        // Give the layout a fixed size to avoid expensive layout operations
        Timeline.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager(CurrentContext);
        LayoutManager.setReverseLayout(true);
        LayoutManager.setStackFromEnd(true);
        Timeline.setLayoutManager(LayoutManager);

        return myView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Posts> options =
                new FirebaseRecyclerOptions.Builder<Posts>()
                .setQuery(PostRef, Posts.class)
                .build();

        Adapter =
                new FirebaseRecyclerAdapter<Posts, PostsHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final PostsHolder holder, int position, @NonNull final Posts model) {
                        // Save post keys so you can edit and delete the correct post.
                        final String PostID = getRef(position).getKey();
                        // Get UID of the post owner to compare to current user to show or hide edit button
                        SinglePostRef = FirebaseDatabase.getInstance().getReference().child("posts").child(PostID);
                        SinglePostRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    PostOwner = dataSnapshot.child("UID").getValue().toString();
                                    UserRef.child(PostOwner).addListenerForSingleValueEvent(new ValueEventListener() {
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
                                    Picasso.get().load(model.getPost_img_uri()).into(holder.post_img);

                                    // Only allow users who own the post in question to edit or delete it.
                                    if (CurrentUser.equals(PostOwner)) {
                                        // Enable the button
                                        holder.editPost.setVisibility(View.VISIBLE);
                                        // Make the button visible
                                        holder.editPost.setEnabled(true);
                                        holder.editPost.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Intent EditPost = new Intent(CurrentContext, EditPostActivity.class);
                                                EditPost.putExtra("PostID", PostID);
                                                startActivity(EditPost);
                                            }
                                        });
                                    }

                                    holder.setLikeStatus(PostID);

                                    holder.likeBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            // Change like icon to red (liked)
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
                                                            Liked = false; // Reset like boolean
                                                        }
                                                        else {
                                                            // Add the current user to the likes table under
                                                            // the current post
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
                                            Intent openComments = new Intent(CurrentContext, CommentsActivity.class);
                                            openComments.putExtra("TYPE", "POST");
                                            openComments.putExtra("PostID", PostID);
                                            startActivity(openComments);
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
                    public PostsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_template, parent, false);
                        PostsHolder postsHolder = new PostsHolder(mView);
                        return postsHolder;
                    }
                };
        Timeline.setAdapter(Adapter);
        Adapter.startListening();
    }

    public static class PostsHolder extends RecyclerView.ViewHolder
    {
        CircleImageView ProfilePic;
        TextView username, date, time, caption, likeCount;
        ImageView post_img;
        ImageButton editPost, likeBtn, commentBtn;
        int likeCounter;
        String currentUID;
        DatabaseReference LikeRef;

        private PostsHolder(View itemView)
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
                    // If the current user has liked the post
                    if(dataSnapshot.child(PostID).hasChild(currentUID))
                    {
                        // Count the amount of users who liked the relevant post
                        likeCounter = (int) dataSnapshot.child(PostID).getChildrenCount();
                        // Set the like icon to the red heart (liked)
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
                        // Set the like icon to the red heart (un-liked)
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
}
