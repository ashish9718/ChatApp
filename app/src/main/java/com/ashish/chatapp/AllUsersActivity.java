package com.ashish.chatapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private RecyclerView mUsersList;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        mToolbar=findViewById(R.id.allusersAppBar);
        mUsersList=findViewById(R.id.users_list);

        mAuth=FirebaseAuth.getInstance();
        mUsersDatabase=FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());


        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mUsersList.setHasFixedSize(true);
        mUsersList.setLayoutManager(new LinearLayoutManager(this));




    }



    @Override
    protected void onStart() {
        super.onStart();
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users");

        //to store every user's name,image,status into model class->Users
        FirebaseRecyclerOptions<Users> options = new FirebaseRecyclerOptions.Builder<Users>()
                        .setQuery(query, new SnapshotParser<Users>() {
                            @NonNull
                            @Override
                            public Users parseSnapshot(@NonNull DataSnapshot snapshot) {
                                return new Users(snapshot.child("name").getValue().toString(),
                                        snapshot.child("image").getValue().toString(),
                                        snapshot.child("status").getValue().toString(),
                                        snapshot.child("thumb_img").getValue().toString());
                            }
                        })
                        .build();

        FirebaseRecyclerAdapter<Users,UsersViewHolder> adapter = new FirebaseRecyclerAdapter<Users, UsersViewHolder>(options) {
            @Override
            public UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);

                return new UsersViewHolder(view);
            }



            @Override
            protected void onBindViewHolder(UsersViewHolder holder, final int position, Users users) {
                //setting user's name,Image,status to users_single_layout
                holder.setName(users.getName());
                holder.setImage(users.getThumb_img());
                holder.setStatus(users.getStatus());

                //getting userid by position in users list recyclerciew
                final String user_id=getRef(position).getKey();
                //mView is user_single_layout
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent profileIntent =new Intent(AllUsersActivity.this,ProfileActivity.class);
                        profileIntent.putExtra("user_id",user_id);
                        startActivity(profileIntent);
                    }
                });

            }

        };

        //setting all user's data to recyclerview
        mUsersList.setAdapter(adapter);
        adapter.startListening();


            mUsersDatabase.child("online").setValue("true");

        Log.i("onStart", "onStart ");

    }

    @Override
    protected void onPause() {
        super.onPause();

        mUsersDatabase.child("online").setValue(ServerValue.TIMESTAMP);
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder{


        View mView;
        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
        }

        //methods for setting values in textviews in users_single_layout
        public void setName(String name){
            TextView mUserNameView=mView.findViewById(R.id.users_single_name);
            mUserNameView.setText(name);
        }
        public void setImage(String image){
            CircleImageView mUserImageView=mView.findViewById(R.id.users_single_image);
            Picasso.get().load(image).placeholder(R.drawable.defaultimg).into(mUserImageView);
        }
        public void setStatus(String status){
            TextView mUserStatusView=mView.findViewById(R.id.users_single_status);
            mUserStatusView.setText(status);

        }


    }
}
