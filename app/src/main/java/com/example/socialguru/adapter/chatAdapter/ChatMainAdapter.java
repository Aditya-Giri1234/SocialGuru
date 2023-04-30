package com.example.socialguru.adapter.chatAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.ChatActivity;
import com.example.socialguru.R;
import com.example.socialguru.databinding.ChatMainActivitySampleBinding;
import com.example.socialguru.model.ChatModel;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChatMainAdapter extends RecyclerView.Adapter<ChatMainAdapter.ViewHolder>{

    Context context;
    ArrayList<ChatModel> list;
     Activity activity;
    public ChatMainAdapter(Context context, ArrayList<ChatModel> list) {
        this.context = context;
        this.list = list;
        activity=(Activity) context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=LayoutInflater.from(context).inflate(R.layout.chat_main_activity_sample,parent,false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
         ChatModel model=list.get(position);
        Picasso.get().load(model.getProfile()).placeholder(R.drawable.image).into(holder.binding.profileImage);
        holder.binding.userName.setText(model.getUserName());
        holder.binding.lastSeen.setText(TimeAgo.using(model.getLastSeen()));
        holder.binding.lastMessage.setText(model.getLastMessage());

        FirebaseDatabase.getInstance().getReference().child("Chats").child(FirebaseAuth.getInstance().getUid()+model.getuId()).limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.hasChildren()){
                    for (DataSnapshot ds:snapshot.getChildren()) {
                        holder.binding.lastMessage.setText(ds.child("message").getValue().toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        holder.binding.constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context, ChatActivity.class);
                intent.putExtra("person",model);
                context.startActivity(intent);
                activity.finish();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
    ChatMainActivitySampleBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            binding= ChatMainActivitySampleBinding.bind(itemView);
        }
    }
}
