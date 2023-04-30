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
import com.example.socialguru.adapter.freindAdpater.FriendRequestAdapter;
import com.example.socialguru.model.FollowModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class FriendRequestFragment extends Fragment {
 RecyclerView friendRequestRv;
 ArrayList<FollowModel> list;

    public FriendRequestFragment() {
        // Required empty public constructor
    }




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_friend_request, container, false);
        list=new ArrayList<>();
        friendRequestRv=view.findViewById(R.id.freindRequestRv);
        FriendRequestAdapter friendRequestAdapter=new FriendRequestAdapter(getContext(),list);
        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friendRequest").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    list.clear();
                    for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                        FollowModel followModel=dataSnapshot.getValue(FollowModel.class);
                        list.add(followModel);
                    }
                    friendRequestAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        friendRequestRv.setLayoutManager(new LinearLayoutManager(getContext()));
        friendRequestRv.setNestedScrollingEnabled(false);
        friendRequestRv.setAdapter(friendRequestAdapter);

        return view;
    }
}