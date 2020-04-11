package com.example.crepcheck;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


public class ImageProcessor extends AppCompatActivity
{
    Context c;
    Toaster myToast;
    private VolleyError HTTPError;
    private String UploadURL, shoeName, shoePrice, ImageName;
    private Bitmap imgBitmap;
    FirebaseAuth mFireBaseAuth;
    FirebaseUser FbUser;
    String currentUser, imgString;

    public ImageProcessor(Context context){
        c = context;
        myToast = new Toaster(c);
        setUploadURL(c.getResources().getString(R.string.heroku_server_url));
        mFireBaseAuth = FirebaseAuth.getInstance();
        FbUser = mFireBaseAuth.getCurrentUser();
        currentUser = FbUser.getUid();  // Get the current user's UID
    }

    public void ProcessImage(Bitmap bitmap)
    {
        myToast.makeToast("Identifying Image...");
        setImgBitmap(bitmap);
        imgString = imageToString(bitmap);
        uploadImage(imgString);
    }

    public String imageToString(Bitmap bitmap)
    {
        byte[] imgBytes = imageToBytes(bitmap);
        return Base64.encodeToString(imgBytes,Base64.DEFAULT);
    }

    public byte[] imageToBytes(Bitmap bitmap) {
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOS);
        byte[] imgBytes = byteOS.toByteArray();
        return imgBytes;
    }

    public void uploadImage(final String imgData) {
        Log.i("uploadImage:", "Started");
        myToast.makeToast("Sending Request to Server...");
        RequestQueue requestQueue= Volley.newRequestQueue(c);
        // Create new String Request
        StringRequest strReq = new StringRequest(Request.Method.POST, getUploadURL(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        Log.d("uploadImage", "REST Response: "+response);
                        try
                        {
                            JSONObject jObj = new JSONObject(response);
                            setShoeName(jObj.getString("shoeName"));
                            setShoePrice(jObj.getString("shoePrice"));
                        } catch (JSONException e)
                        {
                            e.printStackTrace();
                            Log.e("Volley Error",e.toString());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("Volley Error: ",error.toString());
                        setHTTPError(error);
                    }
                })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                params.put("shoe",imgData);
                return params;
            }
        };
        // Set a custom 10 second timeout, as default was too fast for this data transfer.
        strReq.setRetryPolicy(new DefaultRetryPolicy(10 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(strReq);
        requestQueue.addRequestFinishedListener(new RequestQueue.RequestFinishedListener<String>() {
            @Override
            public void onRequestFinished(Request<String> request) {
                VolleyError VE = getHTTPError();

                if( VE instanceof NetworkError) {
                    myToast.makeToast(c.getResources().getString(R.string.NetworkError));
                } else if( VE instanceof ServerError) {
                    myToast.makeToast(c.getResources().getString(R.string.ServerError));
                } else if( VE instanceof AuthFailureError) {
                    myToast.makeToast(c.getResources().getString(R.string.AuthFailureError));
                } else if( VE instanceof ParseError) {
                    myToast.makeToast(c.getResources().getString(R.string.ParseError));
                } else if( VE instanceof NoConnectionError) {
                    myToast.makeToast(c.getResources().getString(R.string.NoConnectionError));
                } else if( VE instanceof TimeoutError) {
                    myToast.makeToast(c.getResources().getString(R.string.TimeoutError));
                } else {
                    // Show the shoe details in a pop-up dialog.
                    openDialog(getShoeName(), getShoePrice());
                }
            }
        });
    }

    public void openDialog(String shoe, String price)
    {
        final Dialog dialog = new Dialog(this.c);
        dialog.setContentView(R.layout.shoe_result_dialog);
        dialog.setCancelable(false);
        dialog.show();

        ImageButton btnFav, btnShare, btnSearch, btnClose;
        TextView tvshoe, tvprice;
        ImageView img;
        btnFav = dialog.findViewById(R.id.btnFav);
        btnShare = dialog.findViewById(R.id.btnShare);
        btnSearch = dialog.findViewById(R.id.btnSearch);
        btnClose = dialog.findViewById(R.id.btnClose);
        tvshoe = dialog.findViewById(R.id.shoeName);
        tvprice = dialog.findViewById(R.id.shoePrice);
        img = dialog.findViewById(R.id.img);

        img.setImageBitmap(getImgBitmap());
        tvshoe.setText(shoe);
        tvprice.setText(price);
        btnFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Post shoe data to user's firebase favourites in the real time database
                SaveFavouriteToStorage();
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent ShareFav = new Intent(dialog.getContext(), PostActivity.class);
                ShareFav.putExtra("Share", "NEW_FAV");
                ShareFav.putExtra("ShoeName", shoeName);
                ShareFav.putExtra("ShoePrice", shoePrice);
                ShareFav.putExtra("imgBitmap", imgString);
                c.startActivity(ShareFav);
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // Google search the recognized shoe using its name.
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, shoeName); // query by the shoe name
                    c.startActivity(intent);
                }
                catch (ActivityNotFoundException e) {
                    myToast.makeToast("Error: "+e);
                }
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private void SaveFavouriteToStorage()
    {
        ImageName = shoeName+"_"+currentUser;
        StorageReference StoreRef = FirebaseStorage.getInstance().getReference("favourites_images");
        final StorageReference ImgName = StoreRef.child(ImageName);


        ImgName.getDownloadUrl().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                byte[] imgData = imageToBytes(getImgBitmap());
                ImgName.putBytes(imgData).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        myToast.makeToast("Upload Error: " + exception);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        ImgName.getDownloadUrl().addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                myToast.makeToast("Error Adding favourite to storage: " + e);
                            }
                        }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                SaveFavouriteToRealTimeDB(uri);
                            }
                        });

                    }
                });
            }
        }).addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                myToast.makeToast(c.getResources().getString(R.string.fav_already));
            }
        });
    }

    public void SaveFavouriteToRealTimeDB(Uri uri)
    {
        // Set a random key for each favourite using .push()
        DatabaseReference FavRef = FirebaseDatabase.getInstance().getReference("favourites").push();
        String key = FavRef.getKey();  // Save the key to the db

        // Save the favourites info to a hash-map
        HashMap fav = new HashMap();
        fav.put("favourite_id", key);
        fav.put("UID", currentUser);
        fav.put("shoe_name", shoeName);
        fav.put("shoe_price", shoePrice);
        fav.put("shoe_image_url", uri.toString());
        fav.put("shoe_img_fileName", ImageName);


        FavRef.setValue(fav).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                myToast.makeToast(c.getResources().getString(R.string.added_to_fav));
            }
        });
    }

    public void setShoeName(String shoe)
    {
        shoeName = shoe;
    }
    public String getShoeName()
    {
        return shoeName;
    }

    public void setShoePrice(String price)
    {
        shoePrice = "€"+price;
    }
    public String getShoePrice()
    {
        return shoePrice;
    }

    public void setImgBitmap(Bitmap bm)
    {
        imgBitmap = bm;
    }
    public Bitmap getImgBitmap()
    {
        return imgBitmap;
    }

    public void setHTTPError(VolleyError e)
    {
        HTTPError = e;
    }
    public VolleyError getHTTPError() { return HTTPError; }

    public void setUploadURL(String url)
    {
        UploadURL = url;
    }
    public String getUploadURL()
    {
        return UploadURL;
    }
}

