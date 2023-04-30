package com.example.socialguru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.socialguru.adapter.ShowFriendAdapter;
import com.example.socialguru.databinding.ActivityShowFriendBinding;
import com.example.socialguru.model.ChatModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class ShowFriendActivity extends AppCompatActivity {
    ActivityShowFriendBinding binding;
    ArrayList<ChatModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityShowFriendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar5);
        ShowFriendActivity.this.setTitle("Friends");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        list=new ArrayList<>();

        ShowFriendAdapter showFriendAdapter=new ShowFriendAdapter(this,list);

        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("friend").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    FirebaseDatabase.getInstance().getReference().child("Users").child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ChatModel model=new ChatModel();
                            model.setProfile(snapshot.child("profile").getValue(String.class));
                            model.setUserName(snapshot.child("name").getValue(String.class));
                            if(snapshot.child("lastSeen").exists())
                            model.setLastSeen(snapshot.child("lastSeen").getValue(Long.class));
                            else
                                model.setLastSeen(new Date().getTime());

                            list.add(model);
                            showFriendAdapter.notifyDataSetChanged();

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.showFreindRv.setLayoutManager(new LinearLayoutManager(this));
        binding.showFreindRv.setAdapter(showFriendAdapter);

    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}