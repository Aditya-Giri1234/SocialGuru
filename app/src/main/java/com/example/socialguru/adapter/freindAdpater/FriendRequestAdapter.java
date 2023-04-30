package com.example.socialguru.adapter.freindAdpater;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.databinding.FragmentFriendRequestBinding;
import com.example.socialguru.databinding.FreindRequestSampleBinding;
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

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>{
    Context context;
    ArrayList<FollowModel> list;

    public FriendRequestAdapter(Context context, ArrayList<FollowModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.freind_request_sample,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
       FollowModel model=list.get(position);

        Picasso.get().load(model.getProfile()).placeholder(R.drawable.image).into(holder.binding.profileImage);
        FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user=snapshot.getValue(User.class);
                holder.binding.userName.setText(user.getName());
                holder.binding.profession.setText(user.getProfession());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.binding.accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendRequest").child(model.getFollowedBy()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friend").child(model.getFollowedBy()).setValue(model).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                FollowModel temp=new FollowModel();
                                temp.setFollowedBy(FirebaseAuth.getInstance().getUid());
                                temp.setFollowsAt(new Date().getTime());
                                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        temp.setProfile(snapshot.child("profile").getValue(String.class));
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                                FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friend").child(FirebaseAuth.getInstance().getUid()).setValue(temp).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Notification notification=new Notification();
                                        notification.setNotificationBy(FirebaseAuth.getInstance().getUid());
                                        notification.setNotificationAt(new Date().getTime());
                                        notification.setType("accept_friend");

                                        FirebaseDatabase.getInstance().getReference().child("Notifications").child(model.getFollowedBy()).push().setValue(notification);
                                        FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friendCount").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists()){
                                                    int count=snapshot.getValue(Integer.class);
                                                    FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friendCount").setValue(count+1);
                                                }
                                                else{
                                                    FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friendCount").setValue(0);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendCount").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists()){
                                                    int count=snapshot.getValue(Integer.class);
                                                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendCount").setValue(count+1);
                                                }
                                                else{
                                                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendCount").setValue(0);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                                        list.remove(holder.getAdapterPosition());
                                        notifyDataSetChanged();

                                    }
                                });
                            }
                        });
                    }
                });


            }
        });
        holder.binding.reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list.remove(holder.getAdapterPosition());
                notifyDataSetChanged();

                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendRequest").child(model.getFollowedBy()).removeValue();




            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        FreindRequestSampleBinding binding;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=FreindRequestSampleBinding.bind(itemView);
        }
    }
}
