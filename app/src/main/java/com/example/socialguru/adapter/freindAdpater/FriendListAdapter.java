package com.example.socialguru.adapter.freindAdpater;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.databinding.FriendListSampleBinding;
import com.example.socialguru.model.FollowModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FriendListAdapter extends  RecyclerView.Adapter<FriendListAdapter.ViewHolder>{
    Context context;
    ArrayList<FollowModel> list;
    ProgressDialog dialog;
    String name;
    String profession;
    DataSnapshot dataSnapshot;

    public FriendListAdapter(Context context, ArrayList<FollowModel> list) {
        this.context = context;
        this.list = list;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view= LayoutInflater.from(context).inflate(R.layout.friend_list_sample,parent,false);
      return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
          FollowModel model=list.get(position);
          int pos=position;




        FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               name=snapshot.child("name").getValue(String.class);
             dataSnapshot=snapshot;
             profession=snapshot.child("profession").getValue(String.class);
                holder.binding.userName.setText(name);
                holder.binding.profession.setText(profession);
                Picasso.get().load(snapshot.child("profile").getValue(String.class)).placeholder(R.drawable.image).into(holder.binding.profileImage);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.binding.removeFollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog=new ProgressDialog(context);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setTitle("Removing Friend !");
                dialog.setMessage("Please wait ..");
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friend").child(model.getFollowedBy()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friend").child(FirebaseAuth.getInstance().getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friendCount").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if(snapshot.exists()){
                                            int count=snapshot.getValue(Integer.class);
                                            if(count>0)
                                                FirebaseDatabase.getInstance().getReference().child("Users").child(model.getFollowedBy()).child("friendCount").setValue(count-1);
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
                                            if(count>0) {
                                                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendCount").setValue(count - 1);
                                            }
                                        }
                                        else{
                                            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendCount").setValue(0);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                                dialog.dismiss();
                                Toast.makeText(context, " Removed successfully !", Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                });
                list.remove(pos);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public  static class ViewHolder extends RecyclerView.ViewHolder{
        FriendListSampleBinding binding;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=FriendListSampleBinding.bind(itemView);
        }
    }
}
