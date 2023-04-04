package com.example.socialguru.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.adapter.StoryAdapter;
import com.example.socialguru.model.StoryModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    RecyclerView storyRv;
    ArrayList<StoryModel> list;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_home, container, false);
        storyRv=view.findViewById(R.id.storyRV);
        list=new ArrayList<>();
        list.add(new StoryModel(R.drawable.pic1,R.drawable.camera,R.drawable.pic2,"Aditya"));
        list.add(new StoryModel(R.drawable.pic2,R.drawable.camera,R.drawable.pic1,"Raju"));
        list.add(new StoryModel(R.drawable.pic3,R.drawable.live,R.drawable.pic2,"Akash"));
        list.add(new StoryModel(R.drawable.pic1,R.drawable.camera,R.drawable.pic2,"Ritu"));
        list.add(new StoryModel(R.drawable.pic2,R.drawable.live,R.drawable.pic4,"Pravin"));
        list.add(new StoryModel(R.drawable.pic4,R.drawable.camera,R.drawable.pic3,"Upendra"));


        StoryAdapter storyAdapter=new StoryAdapter(list,getContext());
        storyRv.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(storyAdapter);
        return view;
    }
}