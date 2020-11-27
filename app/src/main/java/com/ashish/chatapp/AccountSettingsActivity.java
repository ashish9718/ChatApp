package com.ashish.chatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class AccountSettingsActivity extends AppCompatActivity {

    private DatabaseReference mUserDatabase;
    private FirebaseUser mCurrentUser;

    private CircleImageView mDisplayImage;
    private TextView mDisplayName;
    private TextView mStatus;
    private Button mChangeStatusbtn;
    private Button mChangeImagebtn;
    private static final int GALLERY_PICK = 1;


    private ProgressDialog mProgress;

    //storage firebase for profile images
    private StorageReference mImageStorage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        mDisplayImage = findViewById(R.id.settins_image);
        mDisplayName = findViewById(R.id.settings_display_name);
        mStatus = findViewById(R.id.settings_status);
        mChangeStatusbtn = findViewById(R.id.settings_status_btn);
        mChangeImagebtn = findViewById(R.id.settings_img_btn);

        mImageStorage = FirebaseStorage.getInstance().getReference();//pointing to root of cloud storage


        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        String current_uid = mCurrentUser.getUid();

        //mUserDatabase currently pointing to current userid
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase.keepSynced(true);


        //get values of user details to show on account settings activity
        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Toast.makeText(AccountSettingsActivity.this, dataSnapshot.toString(), Toast.LENGTH_SHORT).show();

                String name = dataSnapshot.child("name").getValue().toString();
                final String image = dataSnapshot.child("image").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String thumb_img = dataSnapshot.child("thumb_img").getValue().toString();

                mDisplayName.setText(name);
                mStatus.setText(status);


                if (!image.equals("default")) {
                    //Picasso.get().load(image).placeholder(R.drawable.defaultimg).into(mDisplayImage);
                    Picasso.get().load(image).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.defaultimg).into(mDisplayImage, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get().load(image).placeholder(R.drawable.defaultimg).into(mDisplayImage);
                        }
                    });

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        mChangeStatusbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String status_value = mStatus.getText().toString();
                Intent status_intent = new Intent(AccountSettingsActivity.this, StatusActivity.class);
                status_intent.putExtra("status_value", status_value);
                startActivity(status_intent);
            }
        });
        mChangeImagebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();//image picker we created manaually
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);

                // start picker to get image for cropping and then use the image in cropping activity
              /*  CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(AccountSettingsActivity.this );*/
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUserDatabase.child("online").setValue("true");
        Log.i("onStart", "onStart ");

    }

    @Override
    protected void onPause() {
        super.onPause();
        mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);


    }






    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            // String image_URI=data.getDataString();
            Uri image_URI = data.getData();//image uri
            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity(image_URI)
                    .setAspectRatio(1, 1)
                    .start(this);
            //Toast.makeText(AccountSettingsActivity.this, image_URI, Toast.LENGTH_SHORT).show();
        }

        //for saving image on fire base storage
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {//makes sure that the results(cropped image) that we are getting is from crop activity

            CropImage.ActivityResult result = CropImage.getActivityResult(data);//cropped image stored in result

            if (resultCode == RESULT_OK) {

                mProgress = new ProgressDialog(AccountSettingsActivity.this);
                mProgress.setTitle("Uploading Image");
                mProgress.setMessage("Please wait while we upload and process the image");
                mProgress.setCanceledOnTouchOutside(false);
                mProgress.show();

                Uri resultUri = result.getUri();//uri of cropped image

                //converting uri of cropped image into file
                File thumb_file = new File(resultUri.getPath());

                final String current_user_id = mCurrentUser.getUid();//current user id

                //compressing actual cropped image
                try {
                    Bitmap compressedThumbBitmap = new Compressor(this)
                            .setMaxHeight(150)
                            .setMaxHeight(150)
                            .setQuality(60)
                            .compressToBitmap(thumb_file);

                    //converting compressed bitmap into byte format
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    compressedThumbBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    final byte[] thumb_byte = baos.toByteArray();


                    //creating file path, this is where we are going to store the file and naming the image file we are storing on cloud storage.
                    StorageReference file_path = mImageStorage.child("profile_images").child(current_user_id + ".jpg");

                    //creating thumbnail file path, this is where we are going to store the thumb file and naming the thumb image file we are storing on cloud storage.
                    final StorageReference thumb_file_path = mImageStorage.child("profile_images").child("thumbs").child(current_user_id + ".jpg");

                    //storing cropped image on file path inside cloud storage
                    file_path.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if (task.isSuccessful()) {


                                // String download_url=task.getResult().toString();
                                //get image url from cloud storage i.e, pointing to current user's image in profile_images folder
                                mImageStorage.child("profile_images").child(current_user_id + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                                    @Override
                                    public void onSuccess(Uri uri) {

                                        final String image_download_url = uri.toString();//converting image url in string

                                        //uploading thumbnail to thumb_file_path
                                        UploadTask uploadTask = thumb_file_path.putBytes(thumb_byte);

                                        //storing thumbnail on thumb_file_path inside cloud storage
                                        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> thumb_task) {

                                                //get thumb image url from cloud storage i.e, pointing to current user's thumb image in thumbs folder within profile_images folder
                                                mImageStorage.child("profile_images").child("thumbs").child(current_user_id + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {

                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        String thumb_download_url = uri.toString();//converting thumb image url in string


                                                        if (thumb_task.isSuccessful()) {


                                                            Map update_hashMap = new HashMap<>();
                                                            update_hashMap.put("image", image_download_url);
                                                            update_hashMap.put("thumb_img", thumb_download_url);
                                                            //storing image url and thumb url into database previously mUserDatabase pointing to current uid written on line 70,71
                                                            mUserDatabase.updateChildren(update_hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    mProgress.dismiss();
                                                                    Toast.makeText(AccountSettingsActivity.this, "Uploading Success", Toast.LENGTH_LONG).show();
                                                                }
                                                            });


                                                        } else {

                                                            Toast.makeText(AccountSettingsActivity.this, "Error in uploading thumbnail", Toast.LENGTH_LONG).show();
                                                            mProgress.dismiss();
                                                        }

                                                    }
                                                });
                                            }
                                        });


                                    }
                                });
                            } else {
                                Toast.makeText(AccountSettingsActivity.this, "Error in uploading", Toast.LENGTH_LONG).show();
                                mProgress.dismiss();

                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }






}
