package com.example.socialguru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.socialguru.adapter.CommentAdapter;
import com.example.socialguru.databinding.ActivityCommentBinding;
import com.example.socialguru.model.CommentModel;
import com.example.socialguru.model.Notification;
import com.example.socialguru.model.Post;
import com.example.socialguru.model.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class CommentActivity extends AppCompatActivity {
ActivityCommentBinding binding;
Intent intent;
String postId,postedBy;
FirebaseAuth auth;
FirebaseDatabase database;
ArrayList<CommentModel> list=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        intent=getIntent();
        postId=intent.getStringExtra("postId");
        postedBy=intent.getStringExtra("postedBy");
        setSupportActionBar(binding.toolbar2);
        CommentActivity.this.setTitle("Comments");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();

        database.getReference().child("Posts").child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post post=snapshot.getValue(Post.class);
                Picasso.get().load(post.getPostImg()).placeholder(R.drawable.image).into(binding.postImg);
                binding.postDescription.setText(post.getPostDescription());
                binding.like.setText(post.getPostLike()+"");
                binding.comment.setText(post.getCommentCount()+"");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("Users").child(postedBy).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user=snapshot.getValue(User.class);
                Picasso.get().load(user.getProfile()).placeholder(R.drawable.image).into(binding.profileImage);
                binding.username.setText(user.getName());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.commentPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommentModel commentModel=new CommentModel();
                commentModel.setCommentedBody(binding.commentET.getText().toString());
                commentModel.setCommentedAt(new Date().getTime());
                commentModel.setCommentedBy(FirebaseAuth.getInstance().getUid());
                database.getReference().child("Posts").child(postId).child("Comments").push().setValue(commentModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("Posts").child(postId).child("commentCount").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                int commentCount=0;
                                if(snapshot.exists()){
                                    commentCount=snapshot.getValue(Integer.class);
                                }
                                database.getReference().child("Posts").child(postId).child("commentCount").setValue(commentCount+1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        binding.commentET.setText("");
                                        Toast.makeText(CommentActivity.this, "Commented", Toast.LENGTH_SHORT).show();
                                        Notification notification=new Notification();
                                        notification.setNotificationBy(FirebaseAuth.getInstance().getUid());
                                        notification.setNotificationAt(new Date().getTime());
                                        notification.setPostId(postId);
                                        notification.setPostBy(postedBy);
                                        notification.setType("comment");

                                        FirebaseDatabase.getInstance().getReference().child("Notifications").child(postedBy).push().setValue(notification);
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

        CommentAdapter commentAdapter=new CommentAdapter(this,list);
        binding.commentRV.setLayoutManager(new LinearLayoutManager(this));
        binding.commentRV.setAdapter(commentAdapter);
        database.getReference().child("Posts").child(postId).child("Comments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                    CommentModel commentModel=dataSnapshot.getValue(CommentModel.class);
                    list.add(commentModel);
                }
                Collections.reverse(list);
                commentAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}