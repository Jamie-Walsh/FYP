package com.example.crepcheck;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class PostActivity extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    private EditText PostDesc;
    private ImageButton ChoosePostImage;
    private Button PostBtn;
    private Uri imgURI;
    private String Caption;
    private static final int Gallery_Choice = 1;
    ProgressDialog loadingBar;

    FirebaseAuth mFireBaseAuth;
    FirebaseUser FbUser;
    String currentUser;
    DatabaseReference UserRef;
    StorageReference StoreRef;

    private String CurrentDate;
    private String CurrentTime;
    private String downloadUrl;
    private String imageName;
    private String FavID;
    private String ShareType, FavShoeName, FavShoePrice, FavBitmap;
    private byte[] imgData;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        PostDesc = findViewById(R.id.post_EditText_caption);
        ChoosePostImage = findViewById(R.id.post_choose_image_btn);
        PostBtn = findViewById(R.id.post_btn);
        loadingBar = new ProgressDialog(this);

        mFireBaseAuth = FirebaseAuth.getInstance();
        FbUser = mFireBaseAuth.getCurrentUser();
        currentUser = FbUser.getUid();  // Get the current user's UID
        UserRef = FirebaseDatabase.getInstance().getReference().child("users");
        StoreRef = FirebaseStorage.getInstance().getReference();

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.PostActivity_Title));

        ChoosePostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenUserGallery();
            }
        });


        PostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ValidatePost();
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Disable user's from selecting a new image if they are sharing an existing image.
            ChoosePostImage.setEnabled(false);
            ShareType = extras.getString("Share");
            switch (ShareType.toUpperCase()) {
                case "FAV":
                    FavID = extras.getString("FavID");
                    SetupFavouriteShare();
                    break;
                case "NEW_FAV":
                    FavShoeName = extras.getString("ShoeName");
                    FavShoePrice = extras.getString("ShoePrice");
                    FavBitmap = extras.getString("imgBitmap");
                    SetupFavouriteShare();
                    break;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // Close the activity when the back button is clicked
            Intent close = new Intent(PostActivity.this, MainActivity.class);
            close.putExtra("fragToLoad", "ACCOUNT");
            startActivity(close);
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Gallery_Choice && resultCode == RESULT_OK && data != null) {
            // Display the chosen image in the ImageView by setting its URI to the chosen URI
            imgURI = data.getData();
            ChoosePostImage.setImageURI(imgURI);
        }
    }

    public void OpenUserGallery() {
        Intent openGallery = new Intent();
        openGallery.setAction(Intent.ACTION_GET_CONTENT);
        openGallery.setType("image/*");
        startActivityForResult(openGallery, Gallery_Choice);
    }

    public void ValidatePost() {
        Caption = PostDesc.getText().toString();

        // If the user has not chosen an image or shared one
        if ((imgURI == null) && (imgData == null)) {
            myToast.makeToast("Please choose an image to post!");
        } else if (TextUtils.isEmpty(Caption)) {
            myToast.makeToast("Please write something about the image!");
        } else {
            loadingBar.setTitle("Uploading Post");
            loadingBar.setMessage("Please wait while the post is being uploaded...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            SavePostImageToFBStorage();
        }
    }

    private void SavePostImageToFBStorage() {
        // Date
        Calendar calDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMM-yyyy");
        CurrentDate = currentDate.format(calDate.getTime());
        // Time
        Calendar calTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        CurrentTime = currentTime.format(calTime.getTime());

        String postName = CurrentDate + CurrentTime;
        imageName = currentUser + postName + ".jpg";

        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("posts_images");
        final StorageReference FileName = StoreRef.child(imageName);

        // If it's a new post use the imgURL, otherwise use the imgBitmap
        if (imgData == null) {
            FileName.putFile(imgURI).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    myToast.makeToast("Upload Error: " + exception);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    FileName.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            downloadUrl = uri.toString();
                            myToast.makeToast("Image Uploaded To Storage Successfully...");
                            SavePostToFBRealTimeDB();
                        }
                    });
                }
            });
        } else
        //Share post
        {
            FileName.putBytes(imgData).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    myToast.makeToast("Upload Error: " + exception);
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    FileName.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            downloadUrl = uri.toString();
                            myToast.makeToast("Image Uploaded To Storage Successfully...");
                            SavePostToFBRealTimeDB();
                        }
                    });
                }
            });
        }
    }

    public void SavePostToFBRealTimeDB() {
        UserRef.child(currentUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Save username and Profile picture so they can be used in the post template
                    String username = dataSnapshot.child("username").getValue().toString();
                    String ProfilePic = dataSnapshot.child("profile_pic").getValue().toString();
                    // Set a random key for each post using .push()
                    DatabaseReference PostRef = FirebaseDatabase.getInstance().getReference("posts").push();
                    String key = PostRef.getKey(); // Get randomly generated ID

                    // Save the favourites info to a hash-map
                    HashMap post = new HashMap();
                    post.put("post_id", key);
                    post.put("UID", currentUser);
                    post.put("username", username);
                    post.put("profile_pic", ProfilePic);
                    post.put("post_caption", Caption);
                    post.put("post_img_uri", downloadUrl);
                    post.put("post_img_fileName", imageName);
                    post.put("post_time", CurrentTime);
                    post.put("post_date", CurrentDate);

                    PostRef.setValue(post).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            myToast.makeToast(getResources().getString(R.string.added_to_post));
                            goToTimeline();
                            loadingBar.dismiss();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Delete the image saved in storage since it will not be linked to the realtime DB

                            // Inform User
                            myToast.makeToast("Error uploading post, please try again.");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("SavePostToFBRealTimeDB", databaseError.toString());
            }
        });
    }

    public void goToTimeline() {
        Intent toTimeline = new Intent(PostActivity.this, MainActivity.class);
        toTimeline.putExtra("fragToLoad", "ACCOUNT");
        toTimeline.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(toTimeline);
        finish();
    }

    public void SetupFavouriteShare() {
        // If it a new favourite
        if (ShareType.equals("NEW_FAV")) {
            String shoeName, shoePrice;
            Bitmap imgBitmap;

            // If it a new favourite
            shoeName = FavShoeName;
            shoePrice = FavShoePrice;
            imgBitmap = StringToBitMap(FavBitmap);

            ChoosePostImage.setImageBitmap(imgBitmap);
            String DefaultCaption = "I found a pair of " + shoeName + " shoes worth " + shoePrice + " using CREPCHECK's shoe recognizer!";
            PostDesc.setText(DefaultCaption);

            imgData = imageToBytes(imgBitmap);
        }
        else // If it is a favourite from the database
        {
            DatabaseReference FavRef = FirebaseDatabase.getInstance().getReference().child("favourites").child(FavID);
            FavRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String FavImgURL, shoeName, shoePrice;
                        BitmapDrawable bmDrawable;
                        Bitmap imgBitmap;

                        FavImgURL = dataSnapshot.child("shoe_image_url").getValue().toString();
                        shoeName = dataSnapshot.child("shoe_name").getValue().toString();
                        shoePrice = dataSnapshot.child("shoe_price").getValue().toString();
                        Picasso.get().load(FavImgURL).into(ChoosePostImage);

                        //imgURI = Uri.parse(FavImgURL);
                        // Get the bitmap of the image button and convert it to a byte array
                        bmDrawable = (BitmapDrawable) ChoosePostImage.getDrawable();
                        imgBitmap = bmDrawable.getBitmap();

                        String DefaultCaption = "I found a pair of " + shoeName + " shoes worth " + shoePrice + " using CREPCHECK's shoe recognizer!";
                        PostDesc.setText(DefaultCaption);

                        imgData = imageToBytes(imgBitmap);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    public byte[] imageToBytes(Bitmap bitmap) {
        try {
            ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOS);
            byte[] imgBytes = byteOS.toByteArray();
            return imgBytes;
        }
        catch(Exception e)
        {
            Log.e("imageToBytes", e.getMessage());
            return null;
        }
    }

    public Bitmap StringToBitMap(String string) {
        try {
            byte[] encodeByte = Base64.decode(string, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        }
        catch(Exception e)
        {
            Log.e("StringToBitmap", e.getMessage());
            return null;
        }
    }
}