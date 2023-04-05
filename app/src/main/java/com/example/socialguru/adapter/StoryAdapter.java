package com.example.socialguru.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.model.StoryModel;

import java.util.ArrayList;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.ViewHolder> {

    ArrayList<StoryModel> list;
    Context context;

    public StoryAdapter(ArrayList<StoryModel> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.story_rv_design,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
     StoryModel model=list.get(position);
     holder.profile_image.setImageResource(model.getProfile_image());
     Drawable drawable=context.getResources().getDrawable(model.getStory());
     Bitmap bitmap=((BitmapDrawable)drawable).getBitmap();
     holder.story.setImageBitmap(Bitmap.createScaledBitmap(bitmap,bitmap.getWidth(),bitmap.getHeight(),false));
     holder.storyType.setImageResource(model.getStoryType());
     holder.name.setText(model.getName());

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
    ImageView story,storyType,profile_image;
    TextView name;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            story=itemView.findViewById(R.id.story);
            storyType=itemView.findViewById(R.id.storyType);
            profile_image=itemView.findViewById(R.id.profile_image);
            name=itemView.findViewById(R.id.name);

        }
    }
}
