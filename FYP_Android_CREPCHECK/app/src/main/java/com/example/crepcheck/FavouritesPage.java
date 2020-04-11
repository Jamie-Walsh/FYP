package com.example.crepcheck;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class FavouritesPage extends AppCompatActivity {
    // Attributes
    Toaster myToast = new Toaster(this);
    private RecyclerView FavList;
    private DatabaseReference FavRef, CurrentFav;
    private Query FavQuery;
    private String CurrentUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseRecyclerAdapter<Favourites, FavouritesHolder> FBRecAdapter;

    // Methods
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        FavRef = FirebaseDatabase.getInstance().getReference().child("favourites");
        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser CurUser = mFirebaseAuth.getCurrentUser();
        CurrentUser = CurUser.getUid();

        // Set the FavList variable to the required xml RecyclerView
        FavList = findViewById(R.id.FavouritesList);
        // Give the layout a fixed size to avoid expensive layout operations
        FavList.setHasFixedSize(true);
        LinearLayoutManager LayoutManager= new LinearLayoutManager(FavouritesPage.this);
        LayoutManager.setReverseLayout(true);
        LayoutManager.setStackFromEnd(true);
        FavList.setLayoutManager(LayoutManager);

        // Add a back button to the title bar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.FavouritePage_Title));

        // Query to return the favourites created by the current user
        FavQuery = FavRef.orderByChild("UID").equalTo(CurrentUser);
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

        FirebaseRecyclerOptions<Favourites> options =
                new FirebaseRecyclerOptions.Builder<Favourites>()
                        .setQuery(FavQuery, Favourites.class)
                        .build();


        FBRecAdapter =
                new FirebaseRecyclerAdapter<Favourites, FavouritesHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final FavouritesHolder holder, final int position, @NonNull final Favourites model) {
                        // Save Favourites keys so you can edit and delete the correct favourite.
                        final String FavID = getRef(position).getKey();
                        CurrentFav = FavRef.child(FavID);
                        CurrentFav.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    final String image_name = dataSnapshot.child("shoe_img_fileName").getValue().toString();
                                    holder.shoeName.setText(model.getShoe_name());
                                    holder.shoePrice.setText(model.getShoe_price());
                                    Picasso.get().load(model.getShoe_image_url()).into(holder.shoeImage);

                                    holder.shareBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent ShareFav = new Intent(FavouritesPage.this, PostActivity.class);
                                            ShareFav.putExtra("Share", "FAV");
                                            ShareFav.putExtra("FavID", FavID);
                                            startActivity(ShareFav);
                                        }
                                    });

                                    holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            DeleteFavourite(position, FavID);
                                            RemoveFavImageFromFbStorage(image_name);
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
                    public FavouritesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.favourites_template, parent, false);
                        FavouritesHolder FavouritesHolder = new FavouritesHolder(mView);
                        return FavouritesHolder;
                    }
                };
        FavList.setAdapter(FBRecAdapter);
        FBRecAdapter.startListening();
    }

    public void DeleteFavourite(int position, String id) {
        FBRecAdapter.getRef(position).removeValue();
        FavRef.child(id).removeValue();
        myToast.makeToast("Favourite deleted successfully from Realtime DB.");
    }
    public void RemoveFavImageFromFbStorage(String imgName)
    {
        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("favourites_images").child(imgName);
        StoreRef.delete().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("RemovePostImageFromFbStorage", e.toString());
                myToast.makeToast("Storage Failed");

            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("RemovePostImageFromFbStorage", "Image deleted from storage successfully");
                myToast.makeToast("Storage Successful");
            }
        });
    }

    public static class FavouritesHolder extends RecyclerView.ViewHolder
    {
        TextView shoeName, shoePrice;
        ImageView shoeImage;
        ImageButton shareBtn, deleteBtn;

        private FavouritesHolder(View itemView)
        {
            super(itemView);

            shoeName = itemView.findViewById(R.id.favourite_name);
            shoePrice = itemView.findViewById(R.id.favourite_price);
            shoeImage = itemView.findViewById(R.id.favourite_image_view);
            shareBtn = itemView.findViewById(R.id.favourite_shareBtn);
            deleteBtn = itemView.findViewById(R.id.favourite_deleteBtn);
        }
    }
}