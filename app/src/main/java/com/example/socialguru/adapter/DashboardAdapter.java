package com.example.socialguru.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.model.DashboardModel;

import java.util.ArrayList;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {
    Context context;
    ArrayList<DashboardModel> list;

    public DashboardAdapter(Context context, ArrayList<DashboardModel> list) {
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
      DashboardModel model=list.get(position);
      holder.profile_image.setImageResource(model.getProfile_image());
      holder.postImg.setImageResource(model.getPostImg());
      holder.save.setImageResource(model.getSave());
      holder.username.setText(model.getUsername());
      holder.about.setText(model.getAbout());
      holder.like.setText(model.getLike());
      holder.comment.setText(model.getComment());
      holder.share.setText(model.getShare());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        ImageView profile_image,save,postImg;
        TextView username,about,like,comment,share;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            profile_image=itemView.findViewById(R.id.profile_image);
            postImg=itemView.findViewById(R.id.postImg);
            save=itemView.findViewById(R.id.save);
            username=itemView.findViewById(R.id.userName);
            about=itemView.findViewById(R.id.about);
            like=itemView.findViewById(R.id.like);
            comment=itemView.findViewById(R.id.comment);
            share=itemView.findViewById(R.id.share);

        }
    }
}
