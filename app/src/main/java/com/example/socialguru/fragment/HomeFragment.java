package com.example.socialguru.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.adapter.PostAdapter;
import com.example.socialguru.adapter.StoryAdapter;
import com.example.socialguru.model.Post;
import com.example.socialguru.model.StoryModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class HomeFragment extends Fragment {

    RecyclerView storyRv,postRv;
    ArrayList<StoryModel> list;
    ArrayList<Post> postList;
    PostAdapter postAdapter;

    FirebaseDatabase database;
    FirebaseAuth auth;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_home, container, false);
        storyRv=view.findViewById(R.id.storyRV);
        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();
        list=new ArrayList<>();
        list.add(new StoryModel(R.drawable.pic1,R.drawable.camera,R.drawable.pic2,"Aditya"));
        list.add(new StoryModel(R.drawable.pic2,R.drawable.camera,R.drawable.pic1,"Raju"));
        list.add(new StoryModel(R.drawable.pic3,R.drawable.live,R.drawable.pic2,"Akash"));
        list.add(new StoryModel(R.drawable.pic4,R.drawable.camera,R.drawable.pic2,"Ritu"));
        list.add(new StoryModel(R.drawable.pic1,R.drawable.live,R.drawable.pic4,"Pravin"));
        list.add(new StoryModel(R.drawable.pic4,R.drawable.camera,R.drawable.pic3,"Upendra"));


        StoryAdapter storyAdapter=new StoryAdapter(list,getContext());
        storyRv.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(storyAdapter);


        // Dashboard RecyclerView

        postRv=view.findViewById(R.id.dashboardRv);
        postList=new ArrayList<>();

        database.getReference().child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Post post=dataSnapshot.getValue(Post.class);
                    post.setPostId(dataSnapshot.getKey());
                    postList.add(post);
                }
                Collections.reverse(postList);
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


         postAdapter=new PostAdapter(getContext(),postList);
        postRv.setLayoutManager(new LinearLayoutManager(getContext()));
        postRv.setNestedScrollingEnabled(false);
        postRv.setAdapter(postAdapter);
        return view;
    }
}