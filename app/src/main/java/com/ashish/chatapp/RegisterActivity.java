package com.ashish.chatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ashish.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout mDisplayName,mEmail,mPassword ;
    private Button mCreateBtn;
    private FirebaseAuth mAuth;
    private Toolbar mToolBar;
    private DatabaseReference mDataBaseReference;


    //progress dialog
    private ProgressDialog mRegProgress;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //firebase initialization
        FirebaseApp.initializeApp(this);
        //initialize the firebaseauth instance
        mAuth = FirebaseAuth.getInstance();

        //toolbar
        mToolBar=findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDisplayName= findViewById(R.id.reg_display_name);
        mEmail=findViewById(R.id.login_email);
        mPassword=findViewById(R.id.login_password);
        mCreateBtn= findViewById(R.id.reg_create_button);

        mRegProgress=new ProgressDialog(this);



        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String display_name= mDisplayName.getEditText().getText().toString();
                String email= mEmail.getEditText().getText().toString();
                String password= mPassword.getEditText().getText().toString();

                if(!TextUtils.isEmpty(display_name)&&!TextUtils.isEmpty(email)&&!TextUtils.isEmpty(password)) {

                    mRegProgress.setTitle("Registering User");
                    mRegProgress.setMessage("Please wait while we create your account");
                    mRegProgress.setCanceledOnTouchOutside(false);
                    //show progress dialog
                    mRegProgress.show();
                    //function for  creating account
                    registerUser(display_name, email, password);
                }
            }
        });
    }

    private void registerUser(final String display_name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            FirebaseUser curreentUser=FirebaseAuth.getInstance().getCurrentUser();
                            String uid=curreentUser.getUid();
                            mDataBaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
                            HashMap<String,String> usermap=new HashMap<>();
                            usermap.put("name",display_name);
                            usermap.put("status","Hi there I'm using ChatApp");
                            usermap.put("image","default");
                            usermap.put("thumb_img","default");

                            mDataBaseReference.setValue(usermap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    mRegProgress.dismiss();
                                    // Sign in success, update UI with the signed-in user's information
                                    Intent mainIntent=new Intent(RegisterActivity.this,MainActivity.class);
                                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK);//it prevents us go back to the StartActivity without this we go back to StartActivity
                                    startActivity(mainIntent);
                                    finish();
                                }
                            });

                            //code for storing device token id to users database
                            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                @Override
                                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                    if(!task.isSuccessful()){
                                        Log.w("hg", "getInstanceId failed", task.getException());
                                        return;
                                    }
                                    // Get new Instance ID token
                                    String deviceToken = task.getResult().getToken();

                                    mDataBaseReference.child("deviceToken").setValue(deviceToken).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            mRegProgress.dismiss();

                                        }
                                    });


                                }
                            });



                        } else {

                            mRegProgress.hide();
                            Log.d("err", "onComplete() returned: " + task.getException());
                            Toast.makeText(RegisterActivity.this, "Can't Sign in Please Check Your Credentials!", Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }
}
