package com.ashish.chatapp;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;


public class StatusActivity extends AppCompatActivity {


    private Toolbar mToolbar;
    TextInputLayout mStatusInput;
    Button mSaveChangesBtn;

    //FireBase
    private DatabaseReference mStatusdatabase;
    private DatabaseReference mUsersDatabase;
    private FirebaseUser mCurrentUser;

    //Progress
    ProgressDialog mProgress;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        mStatusInput = findViewById(R.id.status_input);
        mSaveChangesBtn = findViewById(R.id.status_save_btn);

        String status_value = getIntent().getStringExtra("status_value");
        mStatusInput.getEditText().setText(status_value);

        //FireBase
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String CurrentUID = mCurrentUser.getUid();
        mStatusdatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(CurrentUID);

        mAuth=FirebaseAuth.getInstance();
        mUsersDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());

        mToolbar = findViewById(R.id.status_appbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Status");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSaveChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Progress
                mProgress = new ProgressDialog(StatusActivity.this);
                mProgress.setTitle("Saving Changes");
                mProgress.setMessage("Please wait while we save the changes");
                mProgress.show();

                String Status = mStatusInput.getEditText().getText().toString();

                mStatusdatabase.child("status").setValue(Status).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mProgress.dismiss();
                        } else {
                            Toast.makeText(StatusActivity.this, "There is some error occured while saving changes", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
