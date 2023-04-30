package com.example.socialguru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.example.socialguru.adapter.chatAdapter.ChatMainAdapter;
import com.example.socialguru.databinding.ActivityChatMainBinding;
import com.example.socialguru.model.ChatModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ChatMainActivity extends AppCompatActivity {

    ActivityChatMainBinding binding;
    FirebaseDatabase database;
    ArrayList<ChatModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar4);
        ChatMainActivity.this.setTitle("ChatDetails");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        database=FirebaseDatabase.getInstance();
        list=new ArrayList<>();

        ChatMainAdapter chatMainAdapter=new ChatMainAdapter(this,list);

        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    list.clear();
                    for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                        ChatModel model=dataSnapshot.getValue(ChatModel.class);
                        list.add(model);
                    }
                    chatMainAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    binding.chatMainRv.setLayoutManager(new LinearLayoutManager(this));
    binding.chatMainRv.setAdapter(chatMainAdapter);

    binding.addChat.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(new Intent(getApplicationContext(), ShowFriendActivity.class));
        }
    });

    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}