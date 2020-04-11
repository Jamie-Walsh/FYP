package com.example.crepcheck;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class FeedFragment extends Fragment
{
    // Attributes
    Toaster myToast;
    private RecyclerView ArticleFeed;
    private Context CurrentContext;
    private DatabaseReference ArticleRef, SingleArticleRef, LikesRef;
    private String CurrentUser;
    private FirebaseAuth mFirebaseAuth;
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
        View myView = inflater.inflate(R.layout.feed_frag, container, false);

        ArticleRef = FirebaseDatabase.getInstance().getReference().child("articles");
        LikesRef = FirebaseDatabase.getInstance().getReference().child("likes");
        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser CurUser = mFirebaseAuth.getCurrentUser();
        CurrentUser = CurUser.getUid();

        // Set the Timeline variable to the required xml RecyclerView
        ArticleFeed = myView.findViewById(R.id.ArticleFeed);
        // Give the layout a fixed size to avoid expensive layout operations
        ArticleFeed.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager(CurrentContext);
        LayoutManager.setReverseLayout(true);
        LayoutManager.setStackFromEnd(true);
        ArticleFeed.setLayoutManager(LayoutManager);

        return myView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Articles> options =
                new FirebaseRecyclerOptions.Builder<Articles>()
                        .setQuery(ArticleRef, Articles.class)
                        .build();

        FirebaseRecyclerAdapter<Articles, ArticleHolder> FBRecAdapter =
                new FirebaseRecyclerAdapter<Articles, ArticleHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ArticleHolder holder, int position, @NonNull final Articles model) {
                        // Save post keys so you can edit and delete the correct post.
                        final String ArticleID = getRef(position).getKey();
                        // Get UID of the post owner to compare to current user to show or hide edit button
                        SingleArticleRef = FirebaseDatabase.getInstance().getReference().child("articles").child(ArticleID);
                        SingleArticleRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    //PostOwner = dataSnapshot.child("UID").getValue().toString();

                                    Picasso.get().load(model.getArticle_img_uri()).placeholder(R.drawable.img_profile_template).into(holder.article_img);
                                    holder.title.setText(model.getArticle_title());
                                    holder.date.setText(model.getArticle_date());
                                    holder.time.setText(model.getArticle_time());
                                    holder.body.setText(model.getArticle_body());

                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent openArticle = new Intent(CurrentContext, ArticleActivity.class);
                                            openArticle.putExtra("ARTICLE_ID", ArticleID);
                                            startActivity(openArticle);
                                        }
                                    });

                                    holder.setLikeStatus(ArticleID);

                                    holder.article_like.setOnClickListener(new View.OnClickListener() {
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
                                                        if(dataSnapshot.child(ArticleID).hasChild(CurrentUser))
                                                        {
                                                            // Unlike the post by removing the user id
                                                            LikesRef.child(ArticleID).child(CurrentUser).removeValue();
                                                            Liked = false; // Change icon back to white
                                                        }
                                                        else {
                                                            LikesRef.child(ArticleID).child(CurrentUser).setValue(true);
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

                                    holder.article_comment.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent AddComment = new Intent(CurrentContext, CommentsActivity.class);
                                            AddComment.putExtra("TYPE", "ARTICLE");
                                            AddComment.putExtra("ArticleID", ArticleID);
                                            startActivity(AddComment);
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
                    public ArticleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.article_template, parent, false);
                        ArticleHolder postsHolder = new ArticleHolder(mView);
                        return postsHolder;
                    }
                };
        ArticleFeed.setAdapter(FBRecAdapter);
        FBRecAdapter.startListening();
    }

    public static class ArticleHolder extends RecyclerView.ViewHolder
    {
        TextView title, date, time, body, likeCount;
        ImageView article_img, article_like, article_comment;
        int likeCounter;
        String currentUID;
        DatabaseReference LikeRef;

        private ArticleHolder(View itemView)
        {
            super(itemView);

            article_img = itemView.findViewById(R.id.article_preview_image);
            title = itemView.findViewById(R.id.article_preview_title);
            date = itemView.findViewById(R.id.article_preview_date_tv);
            time = itemView.findViewById(R.id.article_preview_time_tv);
            body = itemView.findViewById(R.id.article_preview_body);
            article_like = itemView.findViewById(R.id.article_like_btn);
            article_comment = itemView.findViewById(R.id.article_comment_btn);
            likeCount = itemView.findViewById(R.id.article_like_count);

            LikeRef = FirebaseDatabase.getInstance().getReference().child("likes");
            currentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        private void setLikeStatus(final String ArticleID)
        {
            LikeRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // If the user liked the post
                    if(dataSnapshot.child(ArticleID).hasChild(currentUID))
                    {
                        // Count the amount of users who liked the relevant post
                        likeCounter = (int) dataSnapshot.child(ArticleID).getChildrenCount();
                        article_like.setImageResource(R.drawable.ic_like);
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
                        likeCounter = (int) dataSnapshot.child(ArticleID).getChildrenCount();
                        article_like.setImageResource(R.drawable.ic_unliked);
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
