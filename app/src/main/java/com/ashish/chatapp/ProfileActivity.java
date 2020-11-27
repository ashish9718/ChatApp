package com.ashish.chatapp;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {


    private TextView mProfileDisplayName,mProfileStatus,mProfileTotalFriends;

    private ImageView mProfileImage;

    private Button mProfileSendReqBtn,mProfileDeclineReqButton;

    private DatabaseReference mUsersDatabase;
    private DatabaseReference mFriendRequestDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationsDatabase;
    private DatabaseReference mRootRef;

    private FirebaseUser mCurrentUser;
    private FirebaseAuth mAuth;

    private ProgressDialog mProgressDialog;

    private int mCurrentState;//0=Not a Friend ,1=Request sent ,2=Request Received,3=Friends

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mProfileDisplayName=findViewById(R.id.profile_displayName);
        mProfileStatus=findViewById(R.id.profile_status);
        mProfileTotalFriends=findViewById(R.id.profile_totalfriends);
        mProfileImage=findViewById(R.id.profile_display_image);
        mProfileSendReqBtn=findViewById(R.id.profile_send_req_btn);
        mProfileDeclineReqButton=findViewById(R.id.profile_decline_req_btn);

        //progress dialog
        mProgressDialog=new ProgressDialog(this);
        mProgressDialog.setTitle("Loading Users Data");
        mProgressDialog.setMessage("Please wait while we fetching the user's data");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mAuth = FirebaseAuth.getInstance();

        mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
        mProfileDeclineReqButton.setEnabled(false);

        //user id of user's profile which we are currently viewing
        final String user_id=getIntent().getStringExtra("user_id");

        //database reference to current user id in database
        if (mAuth.getCurrentUser() != null) {
            mUsersDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);

        }


        //database reference to root in database
        mRootRef= FirebaseDatabase.getInstance().getReference();

        //database reference to Friend_req in database
        mFriendRequestDatabase= FirebaseDatabase.getInstance().getReference().child("Friend_req");

        //database reference to Friends in database
        mFriendDatabase=FirebaseDatabase.getInstance().getReference().child("Friends");

        //database reference to Notifications in database
        mNotificationsDatabase=FirebaseDatabase.getInstance().getReference().child("Notifications");

        //getting current user by which we are logged in
        mCurrentUser= FirebaseAuth.getInstance().getCurrentUser();


        //get values like name,status,image of current userid from database and set it to textfields in activity_profile
        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String display_name=dataSnapshot.child("name").getValue().toString();
                String status=dataSnapshot.child("status").getValue().toString();
                String image=dataSnapshot.child("image").getValue().toString();

                mProfileDisplayName.setText(display_name);
                mProfileStatus.setText(status);;
                Picasso.get().load(image).placeholder(R.drawable.defaultimg).into(mProfileImage);

                //----------FRIENDS LIST/REQUEST FEATURE OR DETECT IF THE PERSON'S PROFILE WHICH WE ARE VIEWING HAS SENT US THE REQUSET OR NOT AND DETECT IF IT IS FRIENDS WITH US OR NOT -------------------
                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)){

                            String req_type=dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if(req_type.equals("received")){

                                mCurrentState=2;//1=request Received
                                mProfileSendReqBtn.setText("Accept Friend Request");

                                mProfileDeclineReqButton.setVisibility(View.VISIBLE);
                                mProfileDeclineReqButton.setEnabled(true);
                            }
                            else if (req_type.equals("sent")) {
                                mCurrentState=1;//1=request sent
                                mProfileSendReqBtn.setText("Cancel Friend Request");

                                mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqButton.setEnabled(false);
                            }

                            mProgressDialog.dismiss();
                        }
                        else{// runs when already friend

                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(user_id)){
                                        mCurrentState=3;//3=friends
                                        mProfileSendReqBtn.setText("Unfriend this person");

                                        mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                                        mProfileDeclineReqButton.setEnabled(false);

                                    }
                                    mProgressDialog.dismiss();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                    mProgressDialog.dismiss();
                                }
                            });



                        }




                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        mProgressDialog.dismiss();
                    }
                });



            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mProgressDialog.dismiss();

            }
        });

        mProfileDeclineReqButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map declineMap =new HashMap();
                declineMap.put("Friend_req/" +mCurrentUser.getUid() + "/" +user_id ,null);
                declineMap.put("Friend_req/" +user_id + "/" +mCurrentUser.getUid() ,null);

                mRootRef.updateChildren(declineMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if(databaseError==null){

                            mCurrentState=0;//0=not friends
                            mProfileSendReqBtn.setText("Send Friend Request");
                            Toast.makeText(ProfileActivity.this, "Friend Request Declined", Toast.LENGTH_SHORT).show();

                            mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                            mProfileDeclineReqButton.setEnabled(false);
                        }
                        else{
                            Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        mProfileSendReqBtn.setEnabled(true);
                    }
                });

            }
        });

        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProfileSendReqBtn.setEnabled(false);

                //--------------------NOT FRIENDS STATE-------------------------
                if(mCurrentState==0){//0=not friends i.e. if not friends then we have to send friend request

                    //for getting pushId(unique code)
                    DatabaseReference newNotificattionRef=mRootRef.child("Notifications").child(user_id).push();
                    String newNotificationId=newNotificattionRef.getKey();

                    HashMap<String,String> notifications= new HashMap<>();
                    notifications.put("from",mCurrentUser.getUid());
                    notifications.put("type","request");


                    Map requestMap=new HashMap();
                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" +user_id +"/request_type","sent" );
                    requestMap.put("Friend_req/" + user_id + "/" +mCurrentUser.getUid() +"/request_type","received" );
                    requestMap.put("Notifications/" + user_id + "/" +newNotificationId ,notifications );

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError !=null){
                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_SHORT).show();
                            }

                            mCurrentState=1;//1=request sent
                            mProfileSendReqBtn.setText("Cancel Friend Request");
                            Toast.makeText(ProfileActivity.this, "Friend Request Sent Successfully", Toast.LENGTH_SHORT).show();

                            mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                            mProfileDeclineReqButton.setEnabled(false);

                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });

                }

                //---------------------CANCEL REQUEST STATE------------
                if(mCurrentState==1){//1=request sent i.e. if request is sent then we have to cancel the request

                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrentState=0;//0=not friends
                                    mProfileSendReqBtn.setText("Send Friend Request");
                                    Toast.makeText(ProfileActivity.this, "Friend Request Cancelled", Toast.LENGTH_SHORT).show();

                                    mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                                    mProfileDeclineReqButton.setEnabled(false);

                                }
                            });
                        }
                    });
                }


                //-------------------REQUEST RECEIVED STATE-----------
                if(mCurrentState==2){//2=request received if received then we accept it

                    final String current_date= DateFormat.getDateTimeInstance().format(new Date());

                    Map friendsMap =new HashMap();
                    friendsMap.put("Friends/" +mCurrentUser.getUid() + "/" +user_id +"/date",current_date);
                    friendsMap.put("Friends/" +user_id + "/" +mCurrentUser.getUid() +"/date",current_date);

                    friendsMap.put("Friend_req/" +mCurrentUser.getUid() + "/" +user_id ,null);
                    friendsMap.put("Friend_req/" +user_id + "/" +mCurrentUser.getUid() ,null);

                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError==null){
                                mProfileSendReqBtn.setEnabled(true);
                                mCurrentState=3;//3=friends
                                mProfileSendReqBtn.setText("Unfriend this person");
                                Toast.makeText(ProfileActivity.this, "You are now friends", Toast.LENGTH_SHORT).show();

                                mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqButton.setEnabled(false);
                            }
                            else{
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                //-------------------ALREADY FRIENDS STATE------------------------
                if(mCurrentState==3){//3=friends if already friends then we unfriend them

                    Map unfriendsMap =new HashMap();
                    unfriendsMap.put("Friends/" +mCurrentUser.getUid() + "/" +user_id ,null);
                    unfriendsMap.put("Friends/" +user_id + "/" +mCurrentUser.getUid() ,null);

                    mRootRef.updateChildren(unfriendsMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            if(databaseError==null){

                                mCurrentState=0;//0=not friends
                                mProfileSendReqBtn.setText("Send Friend Request");
                                Toast.makeText(ProfileActivity.this, "You are no longer friends", Toast.LENGTH_SHORT).show();

                                mProfileDeclineReqButton.setVisibility(View.INVISIBLE);
                                mProfileDeclineReqButton.setEnabled(false);
                            }
                            else{
                                Toast.makeText(ProfileActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });

                }

            }
        });







    }

    @Override
    protected void onStart() {
        super.onStart();


            mUsersDatabase.child("online").setValue("true");


    }

    @Override
    protected void onPause() {
        super.onPause();
        mUsersDatabase.child("online").setValue(ServerValue.TIMESTAMP);
    }
}
