package com.example.socialguru.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import com.example.socialguru.R;
import com.example.socialguru.adapter.Notification2Adapter;
import com.example.socialguru.model.Notification2Model;

import java.util.ArrayList;


public class Notification2Fragment extends Fragment {


RecyclerView recyclerView;
ArrayList<Notification2Model> list;
    public Notification2Fragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_notification2, container, false);
        recyclerView=view.findViewById(R.id.notification2RV);
        list=new ArrayList<>();
        list.add(new Notification2Model(R.drawable.pic1," <b>Aditya</b> mention in story  ","1 min ago"));
        list.add(new Notification2Model(R.drawable.pic3,"<b>Raju</b> comment in story","1 day ago"));
        list.add(new Notification2Model(R.drawable.pic2,"<b>Ritu</b> mention in story","2 day ago"));
        list.add(new Notification2Model(R.drawable.pic4,"<b>Akash</b> mention in story","1 month ago"));

        Notification2Adapter notification2Adapter=new Notification2Adapter(getContext(),list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(notification2Adapter);
        return view;
    }
}