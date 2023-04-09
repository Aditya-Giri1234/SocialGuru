package com.example.socialguru.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.socialguru.R;
import com.example.socialguru.adapter.UserAdapter;
import com.example.socialguru.databinding.FragmentSearchBinding;
import com.example.socialguru.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class SearchFragment extends Fragment {
    FragmentSearchBinding binding;
    ArrayList<User> list=new ArrayList<>();
    FirebaseAuth auth;
    FirebaseDatabase database;

    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding=FragmentSearchBinding.inflate(inflater,container,false);
        UserAdapter adapter=new UserAdapter(getContext(),list);
        binding.userRV.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.userRV.setNestedScrollingEnabled(false);
        binding.userRV.setAdapter(adapter);

        database.getReference().child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot!=null){
                    for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                        User user=dataSnapshot.getValue(User.class);
                        user.setUserID(dataSnapshot.getKey());
                        if(!dataSnapshot.getKey().equals(FirebaseAuth.getInstance().getUid()))
                        list.add(user);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return  binding.getRoot();
    }
}