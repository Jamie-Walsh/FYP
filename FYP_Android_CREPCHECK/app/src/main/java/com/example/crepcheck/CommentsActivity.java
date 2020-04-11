package com.example.crepcheck;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.EditText;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class CommentsActivity extends AppCompatActivity {
    // Attributes
    private Toaster myToast = new Toaster(this);
    private EditText commentBar;
    private ImageButton addCommentBtn;
    private RecyclerView commentsList;

    private String CurrentDate, CurrentTime, CommentType;

    DatabaseReference DBRef, UserRef, CommentRef;
    private String CurrentUID;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        commentBar = findViewById(R.id.comment_bar);
        addCommentBtn = findViewById(R.id.send_comment_btn);
        commentsList = findViewById(R.id.comment_list);

        commentsList.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager(CommentsActivity.this);
        LayoutManager.setReverseLayout(true);
        LayoutManager.setStackFromEnd(true);
        commentsList.setLayoutManager(LayoutManager);

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.CommentsActivity_Title));

        CurrentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");

        addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UserRef.child(CurrentUID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            String Username = dataSnapshot.child("username").getValue().toString();
                            ValidateComment(Username);
                            commentBar.setText("");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id== android.R.id.home) {
            // Close the activity when the back button is clicked
            Intent close = new Intent(CommentsActivity.this, MainActivity.class);
            if(CommentType.toUpperCase().equals("POST")) {
                close.putExtra("fragToLoad", "ACCOUNT");
            }
            else{
                close.putExtra("fragToLoad", "FEED");
            }
            startActivity(close);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            CommentType = extras.getString("TYPE");

            if(CommentType.toUpperCase().equals("POST"))
            {
                String PostID = extras.getString("PostID");
                DBRef = FirebaseDatabase.getInstance().getReference().child("posts").child(PostID).child("comments");
                getIntent().removeExtra("PostID");
                getIntent().removeExtra("TYPE");
            }
            else
            {
                String ArticleID = extras.getString("ArticleID");
                DBRef = FirebaseDatabase.getInstance().getReference().child("articles").child(ArticleID).child("comments");
                getIntent().removeExtra("ArticleID");
                getIntent().removeExtra("TYPE");
            }
        }

        FirebaseRecyclerOptions<Comments> options =
                new FirebaseRecyclerOptions.Builder<Comments>()
                        .setQuery(DBRef, Comments.class)
                        .build();

        FirebaseRecyclerAdapter<Comments, CommentsActivity.CommentsHolder> Adapter =
                new FirebaseRecyclerAdapter<Comments, CommentsActivity.CommentsHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final CommentsActivity.CommentsHolder holder, int position, @NonNull final Comments model) {
                        // Save post keys so you can edit and delete the correct post.
                        final String CommentID = getRef(position).getKey();
                        // Get UID of the post owner to compare to current user to show or hide edit button
                        CommentRef = DBRef.child(CommentID);
                        CommentRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    holder.username.setText(model.getUsername());
                                    holder.date.setText(model.getComment_date());
                                    holder.time.setText(model.getComment_time());
                                    holder.commentBody.setText(model.getComment());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public CommentsActivity.CommentsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.comment_template, parent, false);
                        CommentsActivity.CommentsHolder commentHolder = new CommentsActivity.CommentsHolder(mView);
                        return commentHolder;
                    }
                };
        commentsList.setAdapter(Adapter);
        Adapter.startListening();
    }

    public void ValidateComment(String username)
    {
        String comment = commentBar.getText().toString();

        if(comment.isEmpty())
        {
            myToast.makeToast("Cannot upload an empty comment");
        }
        else
        {
            // Date
            Calendar calFordDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
            CurrentDate = currentDate.format(calFordDate.getTime());
            // Time
            Calendar calFordTime = Calendar.getInstance();
            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
            CurrentTime = currentTime.format(calFordTime.getTime());

            final String commentID = CurrentDate + CurrentTime + CurrentUID;

            // Save the favourites info to a hash-map
            HashMap commentData = new HashMap();
            commentData.put("comment_id", commentID);
            commentData.put("comment", comment);
            commentData.put("UID", CurrentUID);
            commentData.put("username", username);
            commentData.put("comment_time", CurrentTime);
            commentData.put("comment_date", CurrentDate);

            DBRef.child(commentID).updateChildren(commentData).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        myToast.makeToast("Comment Added!");
                    } else {
                        myToast.makeToast("Error Occurred, Please Try Again.");
                    }
                }
            });
        }
    }

    public static class CommentsHolder extends RecyclerView.ViewHolder
    {
        TextView username, date, time, commentBody;

        private CommentsHolder(View itemView)
        {
            super(itemView);

            username = itemView.findViewById(R.id.comment_username);
            commentBody = itemView.findViewById(R.id.comment_text);
            time = itemView.findViewById(R.id.comment_time);
            date = itemView.findViewById(R.id.comment_date);
        }
    }
}