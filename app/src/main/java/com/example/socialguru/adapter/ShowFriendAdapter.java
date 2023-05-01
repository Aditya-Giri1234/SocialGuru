package com.example.socialguru.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.activity.ChatActivity;
import com.example.socialguru.activity.ImageActivity;
import com.example.socialguru.R;
import com.example.socialguru.databinding.ChatActivitySampleBinding;
import com.example.socialguru.model.ChatModel;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ShowFriendAdapter extends RecyclerView.Adapter<ShowFriendAdapter.Viewholder> {
    Context context;
    ArrayList<ChatModel> list;

   Activity activity;

    public ShowFriendAdapter(Context context, ArrayList<ChatModel> list) {
        this.context = context;
        this.list = list;
        activity=(Activity) context;

    }

    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.chat_activity_sample,parent,false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {
            ChatModel model=list.get(position);
        Picasso.get().load(model.getProfile()).placeholder(R.drawable.image).into(holder.binding.profileImage);
        holder.binding.userName.setText(model.getUserName());
        holder.binding.lastSeen.setText(TimeAgo.using(model.getLastSeen()));
        holder.binding.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context, ChatActivity.class);
                intent.putExtra("person",model);
                context.startActivity(intent);
                activity.finish();
            }
        });
        holder.binding.profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context, ImageActivity.class);
                intent.putExtra("image",model.getProfile());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static  class Viewholder extends RecyclerView.ViewHolder{
     ChatActivitySampleBinding binding;
        public Viewholder(@NonNull View itemView) {
            super(itemView);
            binding=ChatActivitySampleBinding.bind(itemView);
        }
    }
}
