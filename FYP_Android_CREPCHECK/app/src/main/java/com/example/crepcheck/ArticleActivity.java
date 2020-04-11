package com.example.crepcheck;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

public class ArticleActivity extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    ImageView articleImage;
    TextView title, body;
    String ArticleID;
    DatabaseReference ArticleRef;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        articleImage = findViewById(R.id.article_image);
        title = findViewById(R.id.article_title);
        body = findViewById(R.id.article_body);

        body.setMovementMethod(new ScrollingMovementMethod());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ArticleID = extras.getString("ARTICLE_ID");
            ArticleRef = FirebaseDatabase.getInstance().getReference().child("articles").child(ArticleID);
            ArticleRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        String imgUri = dataSnapshot.child("article_img_uri").getValue().toString();

                        Picasso.get().load(imgUri).placeholder(R.drawable.logo).into(articleImage);
                        title.setText(dataSnapshot.child("article_title").getValue().toString());
                        body.setText(dataSnapshot.child("article_body").getValue().toString());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ArticleLoadError",databaseError.toString());
                }
            });
        }

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.Article_Title));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id== android.R.id.home) {
            // Close the activity when the back button is clicked
            Intent close = new Intent(ArticleActivity.this, MainActivity.class);
            close.putExtra("fragToLoad", "FEED");
            startActivity(close);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }
}