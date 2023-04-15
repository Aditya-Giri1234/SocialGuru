package com.example.socialguru.adapter;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.databinding.CommentSampleBinding;
import com.example.socialguru.model.CommentModel;
import com.example.socialguru.model.User;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {
    Context context;
    ArrayList<CommentModel> list;

    public CommentAdapter(Context context, ArrayList<CommentModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.comment_sample,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
       CommentModel commentModel=list.get(position);
       holder.binding.time.setText(commentModel.getCommentedAt()+"");
        String text = TimeAgo.using(commentModel.getCommentedAt());
        holder.binding.time.setText(text);
        FirebaseDatabase.getInstance().getReference().child("Users").child(commentModel.getCommentedBy()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user=snapshot.getValue(User.class);
                Picasso.get().load(user.getProfile()).placeholder(R.drawable.image).into(holder.binding.profileImage);
                holder.binding.comment.setText(Html.fromHtml("<b>"+user.getName()+"</b> "+commentModel.getCommentedBody()));
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
        CommentSampleBinding binding;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            binding=CommentSampleBinding.bind(itemView);
        }
    }
}
