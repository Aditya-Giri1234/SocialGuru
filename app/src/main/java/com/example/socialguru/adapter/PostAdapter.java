package com.example.socialguru.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.CommentActivity;
import com.example.socialguru.R;
import com.example.socialguru.databinding.DashboardRvSampleBinding;
import com.example.socialguru.model.Notification;
import com.example.socialguru.model.Post;
import com.example.socialguru.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    Context context;
    ArrayList<Post> list;

    public PostAdapter(Context context, ArrayList<Post> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.dashboard_rv_sample,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = list.get(position);
        Picasso.get().load(post.getPostImg()).placeholder(R.drawable.image).into(holder.binding.postImg);
        holder.binding.like.setText(post.getPostLike()+"");
        String description = post.getPostDescription();
        holder.binding.comment.setText(post.getCommentCount()+"");

        if (description.equals("")) {
            holder.binding.postDescription.setVisibility(View.GONE);
        } else {
            holder.binding.postDescription.setText(post.getPostDescription());
            holder.binding.postDescription.setVisibility(View.VISIBLE);
        }

            FirebaseDatabase.getInstance().getReference().child("Users").child(post.getPostedBy()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    Picasso.get().load(user.getProfile()).placeholder(R.drawable.image).into(holder.binding.profileImage);
                    holder.binding.userName.setText(user.getName());
                    holder.binding.about.setText(user.getProfession());


                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("Likes").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {

         //    setOnClickListener on like button
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                        holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.heart,0,0,0);
                } else {
                    holder.binding.like.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("Likes").child(FirebaseAuth.getInstance().getUid()).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("postLike").setValue(post.getPostLike() + 1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            holder.binding.like.setCompoundDrawablesWithIntrinsicBounds(R.drawable.heart, 0, 0, 0);
                                            Notification notification=new Notification();
                                            notification.setNotificationBy(FirebaseAuth.getInstance().getUid());
                                            notification.setNotificationAt(new Date().getTime());
                                            notification.setPostId(post.getPostId());
                                            notification.setPostBy(post.getPostedBy());
                                            notification.setType("Like");

                                            FirebaseDatabase.getInstance().getReference().child("Notifications").child(post.getPostedBy()).push().setValue(notification).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
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

        holder.binding.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context,CommentActivity.class);
                intent.putExtra("postId",post.getPostId());
                intent.putExtra("postedBy",post.getPostedBy());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("save").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    if(snapshot.getValue(Boolean.class)){
                        holder.binding.save.setImageResource(R.drawable.fill_save);
                        holder.binding.save.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                holder.binding.save.setImageResource(R.drawable.save);
                                FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("save").child(FirebaseAuth.getInstance().getUid()).setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(context, "Post is Saved !", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }
                    else{
                        holder.binding.save.setImageResource(R.drawable.save);
                       holder.binding.save.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View view) {
                               holder.binding.save.setImageResource(R.drawable.fill_save);
                               FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("save").child(FirebaseAuth.getInstance().getUid()).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                   @Override
                                   public void onSuccess(Void unused) {
                                       Toast.makeText(context, "Post is Saved !", Toast.LENGTH_SHORT).show();
                                   }
                               });
                           }
                       });
                    }
                }
                else{
                    holder.binding.save.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.binding.save.setImageResource(R.drawable.fill_save);
                            FirebaseDatabase.getInstance().getReference().child("Posts").child(post.getPostId()).child("save").child(FirebaseAuth.getInstance().getUid()).setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(context, "Post is Saved !", Toast.LENGTH_SHORT).show();
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

        DashboardRvSampleBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
           binding=DashboardRvSampleBinding.bind(itemView);
        }
    }
}
