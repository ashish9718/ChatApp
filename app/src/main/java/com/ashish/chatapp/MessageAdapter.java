package com.ashish.chatapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter {

    private List<Messages> mMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabase;
    private Context context;


    private String current_uid;
    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    public MessageAdapter(List<Messages> mMessagesList,Context context) {
        this.mMessagesList = mMessagesList;
        this.context=context;
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        Messages message =  mMessagesList.get(position);

        mAuth=FirebaseAuth.getInstance();
        current_uid=mAuth.getCurrentUser().getUid();
        if (message.getFrom().equals(current_uid)) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.message_sent_single_layout, viewGroup, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.message_received_single_layout, viewGroup, false);
            return new ReceivedMessageHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int i) {

        final Messages message =  mMessagesList.get(i);


        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                ((SentMessageHolder)holder).itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        //pop-up or alert dialog for deleting the message
                        final CharSequence[] options=new CharSequence[]{"Delete Message"};
                        AlertDialog.Builder builder=new AlertDialog.Builder(context);
                        builder.setTitle("Select Options");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(which==0) {
                                    try {
                                        FirebaseAuth Auth=FirebaseAuth.getInstance();
                                        final String cuid=Auth.getCurrentUser().getUid();
                                        FirebaseDatabase.getInstance().getReference().child("Messages")
                                                .child(cuid).child(message.getReceiverid()).child(message.getPushid()).removeValue();
                                        FirebaseDatabase.getInstance().getReference().child("Messages")
                                                .child(message.getReceiverid()).child(cuid).child(message.getPushid()).removeValue();
                                        Log.i("push",message.getPushid());
                                        Log.i("receiver id",message.getReceiverid());
                                        Log.i("current uid",cuid);

                                        delete(i);
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }



                                }
                            }
                        });
                        builder.show();

                        return true;
                    }
                });
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
        }

    }

    @Override
    public int getItemCount() {
        return mMessagesList.size();
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView imageView;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText =itemView.findViewById(R.id.message_sent_text_layout);
            imageView=itemView.findViewById(R.id.message_sent_image_layout);
        }

        void bind(Messages message) {
            if(message.getType().equals("text")){
                messageText.setText(message.getMessage());
            }else{
                Picasso.get().load(message.getMessage())
                        .placeholder(R.drawable.defaultimg).into(imageView);
                messageText.setBackgroundColor(Color.WHITE);

            }


        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        CircleImageView profileImage;
        ImageView imageView;

        ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.message_text_layout);
            profileImage = itemView.findViewById(R.id.message_profile_img_layout);
            imageView= itemView.findViewById(R.id.message_image_layout);
        }

        void bind(Messages message) {
            if(message.getType().equals("text")){
                messageText.setText(message.getMessage());

                String from_user_id=message.getFrom();

                mUsersDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child(from_user_id);

                mUsersDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String thumb_img=dataSnapshot.child("thumb_img").getValue().toString();
                        Picasso.get().load(thumb_img)
                                .placeholder(R.drawable.defaultimg).into(profileImage);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
            else{
                Picasso.get().load(message.getMessage())
                        .placeholder(R.drawable.defaultimg).into(imageView);
                messageText.setBackgroundColor(Color.WHITE);
            }



        }
    }
    public void delete(int position){
        mMessagesList.remove(position);
        notifyItemRemoved(position);
    }

}

