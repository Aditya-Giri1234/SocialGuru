package com.example.socialguru.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.model.FollowModel;
import com.example.socialguru.model.Notification;
import com.example.socialguru.model.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class UserAdapter  extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    Context context;
    ArrayList<User> list;
    String currentUserProfile="";

    public UserAdapter(Context context, ArrayList<User> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.user_sample,parent,false);
        return  new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
       User user=list.get(position);

       FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot snapshot) {
               currentUserProfile=snapshot.child("profile").getValue(String.class);
           }

           @Override
           public void onCancelled(@NonNull DatabaseError error) {

           }
       });

        Picasso.get().load(user.getProfile()).placeholder(R.drawable.change_cover_photo).into(holder.profile_image);
       holder.profession.setText(user.getProfession());
       holder.name.setText(user.getName());

        FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUserID()).child("followers").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    holder.followBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                    holder.followBtn.setText("Following");
                    holder.followBtn.setTextColor(context.getResources().getColor(R.color.grey));
                    holder.followBtn.setEnabled(false);

                }
                else {
                    holder.followBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            FollowModel followModel = new FollowModel();
                            followModel.setFollowedBy(FirebaseAuth.getInstance().getUid());
                            followModel.setFollowsAt(new Date().getTime());
                            followModel.setProfile(currentUserProfile);

                            FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUserID()).child("followers").child(FirebaseAuth.getInstance().getUid()).setValue(followModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUserID()).child("followerCount").setValue(user.getFollowerCount()+1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            holder.followBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                                            holder.followBtn.setText("Following");
                                            holder.followBtn.setTextColor(context.getResources().getColor(R.color.grey));
                                            holder.followBtn.setEnabled(false);
                                            Toast.makeText(context, "You followed " + user.getName(), Toast.LENGTH_SHORT).show();

                                            Notification notification=new Notification();
                                            notification.setNotificationBy(FirebaseAuth.getInstance().getUid());
                                            notification.setNotificationAt(new Date().getTime());
                                            notification.setType("follow");

                                            FirebaseDatabase.getInstance().getReference().child("Notifications").child(user.getUserID()).push().setValue(notification);
                                        }
                                    });


                                }
                            });
                        }
                    });
                }

                }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUserID()).child("friendRequest").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    holder.addFriend.setBackground(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                    holder.addFriend.setText("Friend Request Send");
                    holder.addFriend.setTextColor(context.getResources().getColor(R.color.grey));
                    holder.addFriend.setEnabled(false);

                }
                else {
                    FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUserID()).child("friend").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                holder.addFriend.setBackground(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                                holder.addFriend.setText("Already  Friend ");
                                holder.addFriend.setTextColor(context.getResources().getColor(R.color.grey));
                                holder.addFriend.setEnabled(false);
                            }
                            else{
                                holder.addFriend.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {

                                        FollowModel followModel = new FollowModel();
                                        followModel.setFollowedBy(FirebaseAuth.getInstance().getUid());
                                        followModel.setFollowsAt(new Date().getTime());
                                        followModel.setProfile(currentUserProfile);

                                        FirebaseDatabase.getInstance().getReference().child("Users").child(user.getUserID()).child("friendRequest").child(FirebaseAuth.getInstance().getUid()).setValue(followModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                holder.addFriend.setBackground(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                                                holder.addFriend.setText("Friend Request Sent");
                                                holder.addFriend.setTextColor(context.getResources().getColor(R.color.grey));
                                                holder.addFriend.setEnabled(false);
                                                Toast.makeText(context, "Freind Request Sent " + user.getName(), Toast.LENGTH_SHORT).show();

                                                Notification notification=new Notification();
                                                notification.setNotificationBy(FirebaseAuth.getInstance().getUid());
                                                notification.setNotificationAt(new Date().getTime());
                                                notification.setType("friendRequest");

                                                FirebaseDatabase.getInstance().getReference().child("Notifications").child(user.getUserID()).push().setValue(notification);

                                            }
                                        });
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
         ImageView profile_image;
         TextView name,profession;
         Button followBtn,addFriend;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile_image=itemView.findViewById(R.id.profile_image);
            name=itemView.findViewById(R.id.nameET);
            profession=itemView.findViewById(R.id.profession);
            followBtn=itemView.findViewById(R.id.followBtn);
            addFriend=itemView.findViewById(R.id.addFriend);
        }
    }
}
