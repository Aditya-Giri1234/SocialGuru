package com.example.socialguru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.socialguru.adapter.chatAdapter.ChatAdapter;
import com.example.socialguru.databinding.ActivityChatBinding;
import com.example.socialguru.databinding.ChatActivitySampleBinding;
import com.example.socialguru.model.ChatModel;
import com.github.marlonlom.utilities.timeago.TimeAgo;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {
    ActivityChatBinding binding;
    String senderId;
    String receiverId;
    ArrayList<ChatModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar3);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent=getIntent();
        ChatModel model=intent.getParcelableExtra("person");
        senderId= FirebaseAuth.getInstance().getUid();
        receiverId=model.getuId();
        list=new ArrayList<>();

        Picasso.get().load(model.getProfile()).placeholder(R.drawable.image).into(binding.profileImage);
        binding.userName.setText(model.getUserName());
        binding.lastSeen.setText(TimeAgo.using(model.getLastSeen()));

        final String senderRoom=senderId+receiverId;
        final String receiverRoom=receiverId+senderId;

        ChatAdapter chatAdapter=new ChatAdapter(list,this,receiverId);
        binding.chatRv.setLayoutManager(new LinearLayoutManager(this));
        binding.chatRv.setAdapter(chatAdapter);





        FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for (DataSnapshot dataSnapShot: snapshot.getChildren()) {
                    ChatModel model=dataSnapShot.getValue(ChatModel.class);
                    model.setMessageId(dataSnapShot.getKey());
                    list.add(model);
                }
                chatAdapter.notifyDataSetChanged();
                if(list.size()>0) {
                    binding.chatRv.smoothScrollToPosition(list.size() - 1);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.send.setOnClickListener(view->{
            if(binding.chatSender.getText().toString().isEmpty()){
                binding.chatSender.setError("Enter your message");
                return;
            }
            String message=binding.chatSender.getText().toString();
            ChatModel chatModel=new ChatModel();
            chatModel.setuId(senderId);
            chatModel.setMessage(message);
            chatModel.setLastSeen(new Date().getTime());
            binding.chatSender.setText("");

            FirebaseDatabase.getInstance().getReference().child("Chats").child(senderRoom).push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {

                    FirebaseDatabase.getInstance().getReference().child("Chats").child(receiverRoom).push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("chats").child(receiverId).setValue(intent.getParcelableExtra("person")).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            ChatModel model1=new ChatModel();
                                            model1.setuId(FirebaseAuth.getInstance().getUid());
                                            model1.setProfile(snapshot.child("profile").getValue(String.class));
                                            model1.setLastSeen(new Date().getTime());
                                            model1.setUserName(snapshot.child("name").getValue(String.class));
                                            FirebaseDatabase.getInstance().getReference().child("Users").child(receiverId).child("chats").child(FirebaseAuth.getInstance().getUid()).setValue(model1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {

                                                }
                                            });

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });

                                }
                            });
                        }
                    });
                }
            });
        });



    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}