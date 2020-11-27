package com.ashish.chatapp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendsFragment extends Fragment {


    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;
    private RecyclerView mFriendsList;
    private FirebaseAuth mAuth;
    private View mMainView;
    private String mCurrentUserId;




    public FriendsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mMainView= inflater.inflate(R.layout.fragment_friends, container, false);

       mFriendsList=mMainView.findViewById(R.id.friends_list);
       mAuth=FirebaseAuth.getInstance();
       mCurrentUserId=mAuth.getCurrentUser().getUid();
       mFriendsDatabase= FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUserId);
       mFriendsDatabase.keepSynced(true);

       mUsersDatabase= FirebaseDatabase.getInstance().getReference().child("Users");
       mUsersDatabase.keepSynced(true);

       mFriendsList.setHasFixedSize(true);
       mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();




        //to store every friend's date into model class->Friends
        FirebaseRecyclerOptions<Friends> options =
                new FirebaseRecyclerOptions.Builder<Friends>()
                        .setQuery(mFriendsDatabase, new SnapshotParser<Friends>() {
                            @NonNull
                            @Override
                            public Friends parseSnapshot(@NonNull DataSnapshot snapshot) {
                                return new Friends(snapshot.child("date").getValue().toString());
                            }
                        })
                        .build();

        FirebaseRecyclerAdapter<Friends,FriendsViewHolder> adapter = new FirebaseRecyclerAdapter<Friends,FriendsViewHolder>(options) {
            @Override
            public FriendsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_single_layout, parent, false);

                return new FriendsViewHolder(view);
            }



            @Override
            protected void onBindViewHolder(final FriendsViewHolder holder, final int position, final Friends friends) {
                //setting user's name,Image,status to users_single_layout
                holder.setDate(friends.getDate());


                //getting list_user_id(profiles we are seeing on recyclerView) by position in friends_list recyclerciew
                final String list_user_id=getRef(position).getKey();
                if(list_user_id!=null) {
                    mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            final String userName = dataSnapshot.child("name").getValue().toString();
                            String userThumbImage = dataSnapshot.child("thumb_img").getValue().toString();
                            if(dataSnapshot.hasChild("online")){
                                String online= dataSnapshot.child("online").getValue().toString();
                                holder.setOnline(online);
                            }
                            holder.setUserName(userName);
                            holder.setThumbImage(userThumbImage);
                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //pop-up or alert dialog
                                    CharSequence[] options=new CharSequence[]{"Open Profile","Send Message"};
                                    AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
                                    builder.setTitle("Select Options");
                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if(which==0) {
                                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                                profileIntent.putExtra("user_id", list_user_id);
                                                startActivity(profileIntent);
                                            }
                                            if(which==1){
                                                Intent chatIntent = new Intent(getContext(), ChatActivty.class);
                                                chatIntent.putExtra("user_id", list_user_id);
                                                chatIntent.putExtra("user_name", userName);
                                                startActivity(chatIntent);
                                            }
                                        }
                                    });
                                    builder.show();
                                }
                            });

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }


            }

        };

        //setting all user's data to recyclerview
        mFriendsList.setAdapter(adapter);
        adapter.startListening();


    }
    public static class FriendsViewHolder extends RecyclerView.ViewHolder{


        View mView;
        public FriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
        }

        //methods for setting values in textviews in Friends
        public void setDate(String date){
            TextView mDate=mView.findViewById(R.id.users_single_status);
            mDate.setText(date);
        }


        public void setUserName(String name){
            TextView mName=mView.findViewById(R.id.users_single_name);
            mName.setText(name);
        }


        public void setThumbImage(String img){
            ImageView imgView=mView.findViewById(R.id.users_single_image);
            Picasso.get().load(img).placeholder(R.drawable.defaultimg).into(imgView);
        }


        public void setOnline(String online) {
            CircleImageView onlineIcon=mView.findViewById(R.id.user_single_online_icon);
            if(online.equals("true")){
                onlineIcon.setVisibility(View.VISIBLE);

            }
            else {
                onlineIcon.setVisibility(View.INVISIBLE);
            }
        }
    }
}


