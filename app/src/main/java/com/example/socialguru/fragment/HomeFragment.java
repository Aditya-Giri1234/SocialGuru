package com.example.socialguru.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.socialguru.R;
import com.example.socialguru.adapter.DashboardAdapter;
import com.example.socialguru.adapter.StoryAdapter;
import com.example.socialguru.model.DashboardModel;
import com.example.socialguru.model.StoryModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    RecyclerView storyRv,dashboardRv;
    ArrayList<StoryModel> list;
    ArrayList<DashboardModel> dashboardList;



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
        list.add(new StoryModel(R.drawable.pic4,R.drawable.camera,R.drawable.pic2,"Ritu"));
        list.add(new StoryModel(R.drawable.pic1,R.drawable.live,R.drawable.pic4,"Pravin"));
        list.add(new StoryModel(R.drawable.pic4,R.drawable.camera,R.drawable.pic3,"Upendra"));


        StoryAdapter storyAdapter=new StoryAdapter(list,getContext());
        storyRv.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(storyAdapter);

        dashboardRv=view.findViewById(R.id.dashboardRv);
        dashboardList=new ArrayList<>();
        dashboardList.add(new DashboardModel(R.drawable.pic3,R.drawable.save,R.drawable.pic2,"Aditya","love travelling","100","50","25"));
        dashboardList.add(new DashboardModel(R.drawable.pic1,R.drawable.save,R.drawable.pic5,"Raju","Go to the hell","300","500","250"));
        dashboardList.add(new DashboardModel(R.drawable.pic4,R.drawable.save,R.drawable.pic1,"Upendra","gate Smasher","400","250","90"));
        dashboardList.add(new DashboardModel(R.drawable.pic2,R.drawable.save,R.drawable.pic3,"Ritu","Love to watch movie","70","90","10"));
        dashboardList.add(new DashboardModel(R.drawable.pic5,R.drawable.save,R.drawable.pic1,"Neha","Love Study","700","100","250"));

        DashboardAdapter dashboardAdapter=new DashboardAdapter(getContext(),dashboardList);
        dashboardRv.setLayoutManager(new LinearLayoutManager(getContext()));
        dashboardRv.setNestedScrollingEnabled(false);
        dashboardRv.setAdapter(dashboardAdapter);
        return view;
    }
}