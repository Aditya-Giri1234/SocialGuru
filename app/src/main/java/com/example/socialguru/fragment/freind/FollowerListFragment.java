package com.example.socialguru.fragment.freind;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.socialguru.R;
import com.example.socialguru.adapter.freindAdpater.FollowAdapter;
import com.example.socialguru.model.FollowModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FollowerListFragment extends Fragment {


    ArrayList<FollowModel> list;
    FirebaseDatabase database;
    RecyclerView followerRv;
    FollowAdapter followAdapter;

    public FollowerListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        list=new ArrayList<>();
        database=FirebaseDatabase.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_follower_list, container, false);
        followerRv=view.findViewById(R.id.followerRv);

        followAdapter=new FollowAdapter(getContext(),list);

        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("followers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    FollowModel followModel=dataSnapshot.getValue(FollowModel.class);
                    list.add(followModel);
                }
                followAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        followerRv.setLayoutManager(new LinearLayoutManager(getContext()));
        followerRv.setNestedScrollingEnabled(false);
        followerRv.setAdapter(followAdapter);

        return view;
    }
}