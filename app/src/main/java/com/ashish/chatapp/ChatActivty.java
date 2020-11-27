package com.ashish.chatapp;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivty extends AppCompatActivity {

    private static final int TOTAL_ITEMS_TO_LOAD =10 ;
    private static final int GALLERY_PICK =1 ;
    private int mCurrentPage=1;
    private int noOfItems=0;
    //to load messages above it
    private String mLastkey="";
    //to prevent loading more messages above the last message because we arrrived at last message
    private String mPrevkey="";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mDisplayName;

    private TextView mLastSeen;

    private String mChatUser_sId;
    private CircleImageView mImageView;

    private Toolbar mChatToolbar;

    private DatabaseReference mRootRef;
    private DatabaseReference mUserDatabase;
    private String current_uid;
    private String chat_user_name;




    private ImageButton mChatAddBtn;
    private ImageButton mChatSendBtn;
    private EditText mChatMessageView;

    private MessageAdapter mAdapter;

    //RecyclerView
    private RecyclerView mMessageslist;

    private final List<Messages> messagesList=new ArrayList<>();//ArrayList of Messages
    private LinearLayoutManager mLinearlayout;
    private StorageReference mImageStorage;
    private boolean mChatSeen=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatToolbar=findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);


        ActionBar actionBar=getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setDisplayShowCustomEnabled(true);


        mImageStorage = FirebaseStorage.getInstance().getReference();
        mRootRef= FirebaseDatabase.getInstance().getReference();
        current_uid=FirebaseAuth.getInstance().getCurrentUser().getUid();
        mUserDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);

        //friend's user id with whom we are chatting
        mChatSeen=getIntent().getBooleanExtra("seen",false);

        //friend's user id with whom we are chatting
        mChatUser_sId=getIntent().getStringExtra("user_id");
        //friend's user name with whom we are chatting
        chat_user_name=getIntent().getStringExtra("user_name");
        actionBar.setTitle("");


        LayoutInflater inflater=LayoutInflater.from(this);
        View action_bar_view=inflater.inflate(R.layout.chat_custom_bar,null);
        actionBar.setCustomView(action_bar_view);


        //----------CUSTOM ACTION BAR ITEMS--------------------

        mDisplayName=findViewById(R.id.custom_app_bar_display_name);
        mLastSeen=findViewById(R.id.custom_app_bar_last_seen);
        mImageView=findViewById(R.id.custom_app_bar_image);

        //----------ADD BTN,SEND BTN,MESSAGE VIEW EDIT TEXT-------------
        mChatAddBtn=findViewById(R.id.chat_add_btn);
        mChatSendBtn=findViewById(R.id.chat_send_btn);
        mChatMessageView=findViewById(R.id.chat_message_view);

        //---RecyclerView-----
        mMessageslist=findViewById(R.id.messages_recycler_view);
        mAdapter=new MessageAdapter(messagesList,this);
        mLinearlayout=new LinearLayoutManager(this);
        mMessageslist.setHasFixedSize(true);
        mMessageslist.setLayoutManager(mLinearlayout);
        mMessageslist.setAdapter(mAdapter);


        mSwipeRefreshLayout=findViewById(R.id.message_swipe_layout);

        //loads all previous messages
        loadMessages();


        //for setting friends name at top
        mDisplayName.setText(chat_user_name);

        //for getting online state and getting thumb image for that user's id/friend with whom we are chatting
        mRootRef.child("Users").child(mChatUser_sId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String online= dataSnapshot.child("online").getValue().toString();
                String thumb_image=dataSnapshot.child("thumb_img").getValue().toString();

                if(online.equals("true")){
                    mLastSeen.setText("online");
                }
                else{
                    GetTimeAgo getTimeAgoObj=new GetTimeAgo();

                    long lastTime=Long.parseLong(online);

                    String lastSeenTime=getTimeAgoObj.getTimeAgo(lastTime,getApplicationContext());

                    mLastSeen.setText(lastSeenTime);


                }

                Picasso.get().load(thumb_image).placeholder(R.drawable.defaultimg).into(mImageView);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //for adding the chat details in database
        mRootRef.child("Chat").child(current_uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(mChatUser_sId)){
                    Map chatDetailMap=new HashMap();
                    chatDetailMap.put("seen",false);
                    chatDetailMap.put("timestamp",ServerValue.TIMESTAMP);

                    Map chatUserMap=new HashMap();
                    chatUserMap.put("Chat/"+current_uid+"/"+mChatUser_sId,chatDetailMap);
                    chatUserMap.put("Chat/"+mChatUser_sId+"/"+current_uid,chatDetailMap);
                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if(databaseError!=null){
                                Log.i("CHAT_LOG",databaseError.getMessage());
                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                mCurrentPage++;
                noOfItems=0;
                loadMoreMessages();
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent=new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent,"SELECT IMAGE"),GALLERY_PICK);
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==GALLERY_PICK && resultCode==RESULT_OK){

            Uri imageUri=data.getData();

            final String currentUserRef="Messages/"+current_uid+"/"+mChatUser_sId;
            final String chatUserRef="Messages/"+mChatUser_sId+"/"+current_uid;

            DatabaseReference userMessagePush=mRootRef.child("Messages")
                    .child(current_uid).child(mChatUser_sId).push();

            final String push_id=userMessagePush.getKey();

            StorageReference file_path=mImageStorage.child("Message_images").child(push_id +".jpg");
            file_path.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if(task.isSuccessful()){

                        mImageStorage.child("Message_images").child(push_id +".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                String downloadUrl=uri.toString();

                                Map messageMap=new HashMap();
                                messageMap.put("message",downloadUrl);
                                messageMap.put("seen",false);
                                messageMap.put("type","image");
                                messageMap.put("from",current_uid);
                                messageMap.put("time",ServerValue.TIMESTAMP);
                                messageMap.put("pushid",push_id);
                                messageMap.put("receiverid",mChatUser_sId);

                                Map messageUserMap=new HashMap();
                                messageUserMap.put(currentUserRef+"/"+push_id,messageMap);
                                messageUserMap.put(chatUserRef+"/"+push_id,messageMap);

                                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                        if(databaseError!=null){
                                            Log.i("CHAT_LOG",databaseError.getMessage());
                                        }
                                    }
                                });

                                mChatMessageView.setText("");

                            }
                        });

                    }
                }
            });
        }
    }

    private void loadMoreMessages() {

        DatabaseReference messageRef=mRootRef.child("Messages").child(current_uid).child(mChatUser_sId);


        Query message_query=messageRef.orderByKey().endAt(mLastkey).limitToLast(10);
        //get all the messages from database
        //addChildEventListener used because we need to work with child remove stuff like that
        message_query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                //get message details like from ,message,seen,type and store it in Messages Object
                //and add in messagesList and notify there is some changes in there recyclerview
                Messages messages= dataSnapshot.getValue(Messages.class);

                String messageKey=dataSnapshot.getKey();
                //to get the last key for loading messages above it
                if(noOfItems==0){
                    mLastkey=messageKey;
                }

                //dataSnapshot.getKey() gives current message key from top to bottom order one by one
                if(!mPrevkey.equals(messageKey)){
                    messagesList.add(noOfItems++,messages);
                }
                else{
                    //if mPrevkey is equals to dataSnapshot.getKey()(Current message key)
                    // do not add message object and meassage view instead
                    mPrevkey=mLastkey;
                }




                mAdapter.notifyDataSetChanged();
               //
                mLinearlayout.scrollToPositionWithOffset(noOfItems,0);
                mSwipeRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void loadMessages() {


        DatabaseReference messageRef=mRootRef.child("Messages").child(current_uid).child(mChatUser_sId);


        Query message_query=messageRef.limitToLast(mCurrentPage*TOTAL_ITEMS_TO_LOAD);
        //get all the messages from database
        //addChildEventListener used because we need to work with child remove stuff like that
        message_query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                //get message details like from ,message,seen,type and store it in Messages Object
                //and add in messagesList and notify there is some changes in there recyclerview
                Messages messages= dataSnapshot.getValue(Messages.class);
                //to get the last key for loading messages above it
                if(noOfItems==0){
                    mLastkey=dataSnapshot.getKey();
                    mPrevkey=dataSnapshot.getKey();
                }

                noOfItems++;
                messagesList.add(messages);
                mAdapter.notifyDataSetChanged();
                mMessageslist.scrollToPosition(messagesList.size()-1);

                mSwipeRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void sendMessage() {
        String message=mChatMessageView.getText().toString();

        if(!TextUtils.isEmpty(message)){
            String currentUserRef="Messages/"+current_uid+"/"+mChatUser_sId;
            String chatUserRef="Messages/"+mChatUser_sId+"/"+current_uid;

            //for getting push id (unique id)
            DatabaseReference messageUserPush=mRootRef.child("Messages")
                    .child(current_uid).child(mChatUser_sId).push();

            String pushId=messageUserPush.getKey();

            Map messageMap=new HashMap();
            messageMap.put("message",message);
            messageMap.put("seen",false);
            messageMap.put("type","text");
            messageMap.put("from",current_uid);
            messageMap.put("time",ServerValue.TIMESTAMP);
            messageMap.put("pushid",pushId);
            messageMap.put("receiverid",mChatUser_sId);


            DatabaseReference mConvDatabase1 = FirebaseDatabase.getInstance().getReference().child("Chat").child(mChatUser_sId);
            mConvDatabase1.child(current_uid).child("seen").setValue(false);

            Map messageUserMap=new HashMap();
            messageUserMap.put(currentUserRef+"/"+pushId,messageMap);
            messageUserMap.put(chatUserRef+"/"+pushId,messageMap);

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                    if(databaseError!=null){
                        Log.i("CHAT_LOG",databaseError.getMessage());
                    }
                }
            });
            mChatMessageView.setText("");
        }
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
}
