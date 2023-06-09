package com.example.socialguru.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.activity.CommentActivity;
import com.example.socialguru.activity.ImageActivity;
import com.example.socialguru.R;
import com.example.socialguru.databinding.NotificationRvSampleBinding;
import com.example.socialguru.model.Notification;
import com.example.socialguru.model.User;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    Context context;
    ArrayList<Notification> list;
    final static String image="Image";

    public NotificationAdapter(Context context, ArrayList<Notification> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View view= LayoutInflater.from(context).inflate(R.layout.notification_rv_sample,parent,false);
       return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      Notification model=list.get(position);
      String type=model.getType();
      holder.binding.time.setText(TimeAgo.using(model.getNotificationAt()));
        FirebaseDatabase.getInstance().getReference().child("Users").child(model.getNotificationBy()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user=snapshot.getValue(User.class);

                Picasso.get().load(user.getProfile()).placeholder(R.drawable.image).into(holder.binding.profileImage);


                if(type.equals("Like")){
                    holder.binding.notification.setText(Html.fromHtml("<b>"+user.getName()+"</b> "+"Liked your post"));
                } else if (type.equals("comment")) {
                    holder.binding.notification.setText(Html.fromHtml("<b>"+user.getName()+"</b> "+"Commented your post"));

                }
                else{
                    if(type.equals("friendRequest")){
                        holder.binding.notification.setText(Html.fromHtml("<b>"+user.getName()+"</b> "+" sent friend request."));
                    }
                    else{
                if(type.equals("accept_friend")){
                    holder.binding.notification.setText(Html.fromHtml("<b>"+user.getName()+"</b> "+" accept friend request."));
                }
                else
                    holder.binding.notification.setText(Html.fromHtml("<b>"+user.getName()+"</b> "+" start Following you."));
                    }
                }

                holder.binding.profileImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(context, ImageActivity.class);
                        intent.putExtra(image,user.getProfile());
                        context.startActivity(intent);
                    }
                });

                holder.binding.notification.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(type.equals("comment")) {
                            Intent intent = new Intent(context, CommentActivity.class);
                            intent.putExtra("postId", model.getPostId());
                            intent.putExtra("postedBy", model.getPostBy());
                            context.startActivity(intent);
                        }
                    }
                });
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

    public class ViewHolder extends RecyclerView.ViewHolder{

        NotificationRvSampleBinding binding;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
          binding=NotificationRvSampleBinding.bind(itemView);
        }
    }
}
