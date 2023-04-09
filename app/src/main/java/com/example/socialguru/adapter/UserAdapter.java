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

        Picasso.get().load(user.getProfile()).placeholder(R.drawable.change_cover_photo).into(holder.profile_image);
       holder.profession.setText(user.getProfession());
       holder.name.setText(user.getName());

        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("followers").child(user.getUserID()).addListenerForSingleValueEvent(new ValueEventListener() {
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
                            followModel.setProfile(user.getProfile());
                            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("followers").child(user.getUserID()).setValue(followModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(snapshot!=null){
                                                User user=snapshot.getValue(User.class);
                                                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("followerCount").setValue(user.getFollowerCount() + 1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        holder.followBtn.setBackground(ContextCompat.getDrawable(context, R.drawable.follow_active_btn));
                                                        holder.followBtn.setText("Following");
                                                        holder.followBtn.setTextColor(context.getResources().getColor(R.color.grey));
                                                        holder.followBtn.setEnabled(false);
                                                        Toast.makeText(context, "You followed " + user.getName(), Toast.LENGTH_SHORT).show();

                                                    }
                                                });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

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


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
         ImageView profile_image;
         TextView name,profession;
         Button followBtn;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile_image=itemView.findViewById(R.id.profile_image);
            name=itemView.findViewById(R.id.name);
            profession=itemView.findViewById(R.id.profession);
            followBtn=itemView.findViewById(R.id.followBtn);
        }
    }
}
