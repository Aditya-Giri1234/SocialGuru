package com.example.socialguru.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.socialguru.R;
import com.example.socialguru.adapter.NotificationAdapter;
import com.example.socialguru.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;


public class Notification2Fragment extends Fragment {


RecyclerView recyclerView;
ArrayList<Notification> list;
FirebaseDatabase database;
    public Notification2Fragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database=FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_notification_notification, container, false);
        recyclerView=view.findViewById(R.id.notification2RV);
        list=new ArrayList<>();


        NotificationAdapter notification2Adapter=new NotificationAdapter(getContext(),list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
  
        recyclerView.setAdapter(notification2Adapter);
        database.getReference().child("Notifications").child(FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Notification notification=dataSnapshot.getValue(Notification.class);
                    notification.setNotificationId(dataSnapshot.getKey());
                    list.add(notification);
                }
                Collections.reverse(list);
                notification2Adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;
    }
}