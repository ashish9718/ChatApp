package com.ashish.chatapp;


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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView mRequestsList;
    private FirebaseAuth mAuth;
    private String mCurrentUserId;
    private DatabaseReference mFriendReqDatabase;
    private List<UserRequest> userRequests=new ArrayList<>();
    private Query query;
    private DatabaseReference mUsersDatabase;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mMainView= inflater.inflate(R.layout.fragment_requests, container, false);
        mRequestsList=mMainView.findViewById(R.id.requests_list);

        mAuth=FirebaseAuth.getInstance();
        mCurrentUserId=mAuth.getCurrentUser().getUid();
        query= FirebaseDatabase.getInstance().getReference()
                .child("Friend_req").child(mCurrentUserId).orderByChild("request_type").equalTo("received");
        mUsersDatabase= FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);
        mRequestsList.setHasFixedSize(true);
        mRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        return mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();

        //to store every friend's request_type into model class->UserRequest
        FirebaseRecyclerOptions<UserRequest> options =
                new FirebaseRecyclerOptions.Builder<UserRequest>()
                        .setQuery(query, new SnapshotParser<UserRequest>() {
                            @NonNull
                            @Override
                            public UserRequest parseSnapshot(@NonNull DataSnapshot snapshot) {
                                return new UserRequest(snapshot.child("request_type").getValue().toString());
                            }
                        })
                        .build();


        FirebaseRecyclerAdapter<UserRequest,UsersViewHolder> adapter=new FirebaseRecyclerAdapter<UserRequest, UsersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final UsersViewHolder holder, int position, @NonNull UserRequest model) {

                final String list_user_id=getRef(position).getKey();


                if(list_user_id!=null) {
                    mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            final String userName = dataSnapshot.child("name").getValue().toString();
                            String userThumbImage = dataSnapshot.child("thumb_img").getValue().toString();
                            String userStatus = dataSnapshot.child("status").getValue().toString();

                            holder.setUserName(userName);
                            holder.setThumbImage(userThumbImage);
                            holder.setStatus(userStatus);
                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                                Intent profileIntent = new Intent(getContext(), ProfileActivity.class);
                                                profileIntent.putExtra("user_id", list_user_id);
                                                startActivity(profileIntent);
                                            }

                            });

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }



            }

            @NonNull
            @Override
            public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.users_single_layout, viewGroup, false);

                return new UsersViewHolder(view);
            }
        };

        //setting all user's requset data to recyclerview
        mRequestsList.setAdapter(adapter);
        adapter.startListening();

    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder{


        View mView;
        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            mView=itemView;
        }


        public void setUserName(String name){
            TextView mName=mView.findViewById(R.id.users_single_name);
            mName.setText(name);
        }


        public void setThumbImage(String img){
            ImageView imgView=mView.findViewById(R.id.users_single_image);
            Picasso.get().load(img).placeholder(R.drawable.defaultimg).into(imgView);
        }

        public void setStatus(String status) {
            TextView status_view=mView.findViewById(R.id.users_single_status);
            status_view.setText(status);
        }
    }
}
