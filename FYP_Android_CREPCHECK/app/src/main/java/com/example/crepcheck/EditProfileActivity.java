package com.example.crepcheck;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    private CircleImageView editProfilePic;
    private EditText editBio;
    private Button saveChangesBtn;
    private DatabaseReference UserRef, ProfileOwnerRef;
    private String ProfileOwnerID, Bio, newImageName, image_name;
    private Uri imgURI;
    private static final int Gallery_Choice = 1;
    private ProgressDialog loadingBar;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        editProfilePic = findViewById(R.id.edit_profile_pic);
        editBio = findViewById(R.id.edit_profile_bio);
        saveChangesBtn = findViewById(R.id.edit_profile_save_changes_btn);
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");
        loadingBar = new ProgressDialog(this);
        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.EditProfileActivity_Title));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ProfileOwnerID = extras.getString("UID");
            ProfileOwnerRef = UserRef.child(ProfileOwnerID);
            ProfileOwnerRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String imgUri = dataSnapshot.child("profile_pic").getValue().toString();
                        image_name = dataSnapshot.child("profile_pic_name").getValue().toString();
                        Bio = dataSnapshot.child("user_bio").getValue().toString();

                        editBio.setText(Bio);
                        if (imgUri.isEmpty()) {
                            Picasso.get().load(R.drawable.img_profile_template).into(editProfilePic);
                        } else {
                            Picasso.get().load(imgUri).placeholder(R.drawable.img_profile_template).into(editProfilePic);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ProfileError", databaseError.toString());
                    myToast.makeToast("Could not locate profile");
                    EditProfileActivity.this.finish();
                }
            });
        }

        editProfilePic.setTag("Unchanged");
        editProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenUserGallery();
            }
        });

        saveChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidateEdit();
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
            editProfilePic.setImageURI(imgURI);
            editProfilePic.setTag("Changed");
        }
    }

    public void ValidateEdit()
    {
        String CurrentCaption = editBio.getText().toString();
        String Tag = editProfilePic.getTag().toString();

        // If no changes were made
        if((CurrentCaption.equals(Bio)) && (Tag.equals("Unchanged")))
        {
            myToast.makeToast("No Changes Were Made");
        }
        // If only the image was changed
        else if ((CurrentCaption.equals(Bio)) && (!Tag.equals("Unchanged")))
        {
            SaveNewImageToFBStorage();
        }
        // If only the bio was changed
        else if ((!CurrentCaption.equals(Bio)) && (Tag.equals("Unchanged")))
        {
            ProfileOwnerRef.child("user_bio").setValue(CurrentCaption);
            goBack();
        }
        // If the image and the bio were changed
        else if ((!CurrentCaption.equals(Bio)) && (!Tag.equals("Unchanged")))
        {
            ProfileOwnerRef.child("user_bio").setValue(CurrentCaption);
            SaveNewImageToFBStorage();
        }
    }

    private void SaveNewImageToFBStorage()
    {
        // Date
        Calendar calFordDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
        String CurrentDate = currentDate.format(calFordDate.getTime());
        // Time
        Calendar calFordTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        String CurrentTime = currentTime.format(calFordTime.getTime());
        newImageName = imgURI.getLastPathSegment() + ProfileOwnerID + CurrentDate+CurrentTime + ".jpg";

        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("user_profile_pictures");
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
                        ProfileOwnerRef.child("profile_pic").setValue(uri.toString());
                        ProfileOwnerRef.child("profile_pic_name").setValue(newImageName);
                        myToast.makeToast("New Image Uploaded To Storage Successfully...");

                        // Remove the old image from FireBase Storage
                        if (!image_name.isEmpty()) {
                            RemovePostImageFromFbStorage();
                        }
                        goBack();
                    }
                });
            }
        });
    }

    public void RemovePostImageFromFbStorage()
    {
        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("user_profile_pictures").child(image_name);
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

    public void goBack() {
        this.finish();
    }
}
