package com.ashish.chatapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;


public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private Toolbar mToolBar;
    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectiosPagerAdapter;
    private TabLayout mTabLayout;
    private DatabaseReference mUserDatabase;



    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==1){
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED &&ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }}}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        mToolBar=findViewById(R.id.main_page_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("ChatApp");

        if (mAuth.getCurrentUser() != null) {
            String current_uid = mAuth.getCurrentUser().getUid();

            mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users")
                    .child(current_uid);
        }

        //tabs
        mViewPager=findViewById(R.id.main_tab_pager);
        mSectiosPagerAdapter=new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectiosPagerAdapter);

        mTabLayout=findViewById(R.id.main_tabs);
        mTabLayout.setupWithViewPager(mViewPager);



        if(Build.VERSION.SDK_INT<23){

        }
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }

        }

    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser==null){//if user not logged in then we have to create an account by sending us to start activity
            sendToStart();
        }
        else {
            mUserDatabase.child("online").setValue("true");
        }


    }


    @Override
    protected void onPause() {
        super.onPause();
        mUserDatabase.child("online").setValue(ServerValue.TIMESTAMP);

    }

    private void sendToStart() {
        Intent startIntent =new Intent(getApplicationContext(),StartActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);

         getMenuInflater().inflate(R.menu.main_menu,menu);

         return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         super.onOptionsItemSelected(item);

         switch(item.getItemId()){
             case R.id.main_logout_btn:
                 FirebaseAuth.getInstance().signOut();
                 sendToStart();
                 return true;
             case R.id.main_settings_btn:
                 Intent SettingsIntent=new Intent(MainActivity.this, AccountSettingsActivity.class);
                 startActivity(SettingsIntent);
                 return true;

             case R.id.main_all_btn:
                 Intent AllUsersIntent=new Intent(MainActivity.this, AllUsersActivity.class);
                 startActivity(AllUsersIntent);
                 return true;

             default:return false;


         }

    }
}
