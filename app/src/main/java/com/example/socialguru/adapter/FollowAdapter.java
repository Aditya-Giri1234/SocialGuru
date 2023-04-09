package com.example.socialguru.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.databinding.FollowRvSampleBinding;
import com.example.socialguru.model.FollowModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.ViewHolder>{
    Context context;
    ArrayList<FollowModel> list;

    public FollowAdapter(Context context, ArrayList<FollowModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.follow_rv_sample,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
     FollowModel model=list.get(position);
        Picasso.get().load(model.getProfile()).placeholder(R.drawable.change_cover_photo).into(holder.binding.profileImage);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
       FollowRvSampleBinding binding;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            binding=FollowRvSampleBinding.bind(itemView);
        }
    }
}
