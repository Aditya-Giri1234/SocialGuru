package com.example.socialguru.adapter;

import android.content.Context;
import android.media.Image;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.model.Notification2Model;

import java.util.ArrayList;

import kotlin.jvm.internal.Lambda;

public class Notification2Adapter extends RecyclerView.Adapter<Notification2Adapter.ViewHolder> {
    Context context;
    ArrayList<Notification2Model> list;

    public Notification2Adapter(Context context, ArrayList<Notification2Model> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
       View view= LayoutInflater.from(context).inflate(R.layout.notification2sample,parent,false);
       return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      Notification2Model model=list.get(position);
      holder.profile_image.setImageResource(model.getProfile_image());
      holder.notification.setText(Html.fromHtml(model.getNotification()));
      holder.time.setText(model.getTime());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        ImageView profile_image;
        TextView notification,time;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            profile_image=itemView.findViewById(R.id.profile_image);
            notification=itemView.findViewById(R.id.notification);
            time=itemView.findViewById(R.id.time);
        }
    }
}
