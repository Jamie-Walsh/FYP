package com.example.crepcheck;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class EditPostActivity extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    private ImageButton EditPostImg;
    private EditText EditPostCaption;
    private Button SaveChangesBtn;
    private Button DeletePostBtn;

    private Uri imgURI;
    private static final int Gallery_Choice = 1;
    private DatabaseReference EditPostRef;

    private String CurrentDate;
    private String CurrentTime;
    private String PostID, image_uri, caption, image_name, newImageName;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        EditPostImg = findViewById(R.id.EditPost_choose_image_btn);
        EditPostCaption = findViewById(R.id.EditPost_EditText_caption);
        SaveChangesBtn = findViewById(R.id.EditPost_btn);
        DeletePostBtn = findViewById(R.id.DeletePost_btn);

        // Get the specific post
        PostID = getIntent().getExtras().get("PostID").toString();
        EditPostRef = FirebaseDatabase.getInstance().getReference().child("posts").child(PostID);

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.EditPostActivity_Title));

        EditPostRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    image_uri = dataSnapshot.child("post_img_uri").getValue().toString();
                    image_name = dataSnapshot.child("post_img_fileName").getValue().toString();
                    caption = dataSnapshot.child("post_caption").getValue().toString();
                    Picasso.get().load(image_uri).into(EditPostImg);
                    EditPostCaption.setText(caption);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DeletePostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(EditPostActivity.this);
                builder.setMessage("Are You Sure? Deleted Posts Cannot Be Recovered!")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DeletePost();
                            }
                        }).setNegativeButton("Cancel", null);
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        EditPostImg.setTag("Unchanged");
        EditPostImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenUserGallery();
            }
        });

        SaveChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidateEdit();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Close the activity when the back button is clicked
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void OpenUserGallery()
    {
        Intent openGallery = new Intent();
        openGallery.setAction(Intent.ACTION_GET_CONTENT);
        openGallery.setType("image/*");
        startActivityForResult(openGallery, Gallery_Choice);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==Gallery_Choice && resultCode==RESULT_OK && data!=null)
        {
            // Display the chosen image in the ImageView by setting its URI to the chosen URI
            imgURI = data.getData();
            EditPostImg.setImageURI(imgURI);
            EditPostImg.setTag("Changed");
        }
    }

    public void DeletePost()
    {
        EditPostRef.removeValue();
        RemovePostImageFromFbStorage();
        myToast.makeToast("Post deleted successfully.");
        goToTimeline();
    }

    public void RemovePostImageFromFbStorage()
    {
        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("posts_images").child(image_name);
        StoreRef.delete().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("RemovePostImageFromFbStorage", e.toString());

            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("RemovePostImageFromFbStorage", "Image deleted from storage successfully");
            }
        });
    }

    public void ValidateEdit()
    {
        String CurrentCaption = EditPostCaption.getText().toString();
        String Tag = EditPostImg.getTag().toString();

        // If no changes were made
        if((CurrentCaption.equals(caption)) && (Tag.equals("Unchanged")))
        {
            myToast.makeToast("No Changes Were Made");
        }
        // If only the image was changed
        else if ((CurrentCaption.equals(caption)) && (!Tag.equals("Unchanged")))
        {
            SaveNewImageToFBStorage();
            goToTimeline();
        }
        // If only the caption was changed
        else if ((!CurrentCaption.equals(caption)) && (Tag.equals("Unchanged")))
        {
            EditPostRef.child("post_caption").setValue(CurrentCaption);
            goToTimeline();
        }
        // If the image and the caption were changed
        else if ((!CurrentCaption.equals(caption)) && (!Tag.equals("Unchanged")))
        {
            EditPostRef.child("post_caption").setValue(CurrentCaption);
            SaveNewImageToFBStorage();
            goToTimeline();
        }
    }

    private void SaveNewImageToFBStorage()
    {
        // Date
        Calendar calFordDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
        CurrentDate = currentDate.format(calFordDate.getTime());
        // Time
        Calendar calFordTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        CurrentTime = currentTime.format(calFordTime.getTime());

        String postName = CurrentDate + CurrentTime;
        newImageName = imgURI.getLastPathSegment() + postName + ".jpg";

        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("posts_images");
        final StorageReference FileName = StoreRef.child(newImageName);

        FileName.putFile(imgURI).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                myToast.makeToast("Upload Error: " + exception);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                FileName.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        //Update the URI and fileName fields in the database
                        EditPostRef.child("post_img_uri").setValue(uri.toString());
                        EditPostRef.child("post_img_fileName").setValue(newImageName);
                        EditPostRef.child("post_time").setValue(CurrentTime);
                        myToast.makeToast("New Image Uploaded To Storage Successfully...");
                        // Remove the old image from FireBase Storage
                        RemovePostImageFromFbStorage();
                    }
                });
            }
        });
    }

    public void goToTimeline()
    {
        this.finish();
    }
}
