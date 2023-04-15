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
import com.example.socialguru.adapter.SaveAdapter;
import com.example.socialguru.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class SaveFragment extends Fragment {

 RecyclerView saveRv;
 ArrayList<Post> list;

    public SaveFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_notification_save_post, container, false);

      saveRv=view.findViewById(R.id.saveRV);
      list=new ArrayList<>();
        SaveAdapter saveAdapter=new SaveAdapter(getContext(),list);
        FirebaseDatabase.getInstance().getReference().child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    if(dataSnapshot.child("save").exists()&&dataSnapshot.child("save").child(FirebaseAuth.getInstance().getUid())!=null){
                        Post post=dataSnapshot.getValue(Post.class);
                        post.setPostId(dataSnapshot.getKey());
                        list.add(post);
                    }
                }
                Collections.reverse(list);
                saveAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        saveRv.setLayoutManager(new LinearLayoutManager(getContext()));
        saveRv.setAdapter(saveAdapter);


        return view;
    }
}