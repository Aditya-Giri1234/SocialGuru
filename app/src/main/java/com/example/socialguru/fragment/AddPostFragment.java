package com.example.socialguru.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.socialguru.R;
import com.example.socialguru.databinding.FragmentAddPostBinding;
import com.example.socialguru.model.Post;
import com.example.socialguru.model.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iammert.library.readablebottombar.ReadableBottomBar;
import com.squareup.picasso.Picasso;

import java.util.Date;


public class AddPostFragment extends Fragment {

    FragmentAddPostBinding binding;
    Uri uri;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    ProgressDialog dialog;


    public AddPostFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth=FirebaseAuth.getInstance();
        database=FirebaseDatabase.getInstance();
        storage=FirebaseStorage.getInstance();
        dialog=new ProgressDialog(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding=FragmentAddPostBinding.inflate(inflater,container,false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Post Uploading");
        dialog.setMessage("Please wait ..");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
      database.getReference().child("Users").child(auth.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
          @Override
          public void onDataChange(@NonNull DataSnapshot snapshot) {
              if(snapshot.exists()){
                  User user=snapshot.getValue(User.class);
                  Picasso.get().load(user.getProfile()).placeholder(R.drawable.image).into(binding.profileImage);
                  binding.name.setText(user.getName());
                  binding.profession.setText(user.getProfession());
              }
          }

          @Override
          public void onCancelled(@NonNull DatabaseError error) {

          }
      });

  binding.postDescription.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

          String str=binding.postDescription.getText().toString();
          if(!str.isEmpty()){
              binding.postBtn.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.follow_btn_bg));
              binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.white));
              binding.postBtn.setEnabled(true);
          }
          else{
              binding.postBtn.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.follow_active_btn));
              binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.grey));
              binding.postBtn.setEnabled(false);
          }
      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
  });
  binding.addImg.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
          Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
          intent.setType("image/*");
          startActivityForResult(intent,11);
      }
  });

  binding.postBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
          dialog.show();
          final StorageReference storageReference=storage.getReference().child("Posts").child(FirebaseAuth.getInstance().getUid()).child(new Date().getTime()+"");


            if(uri!=null)
          storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
              @Override
              public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                  storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                      @Override
                      public void onSuccess(Uri uri) {
                          Post post=new Post();
                          post.setPostImg(uri.toString());
                          post.setPostedBy(FirebaseAuth.getInstance().getUid());
                          post.setPostDescription(binding.postDescription.getText().toString());
                          post.setPostedAt(new Date().getTime());
                          database.getReference().child("Posts").push().setValue(post).addOnSuccessListener(new OnSuccessListener<Void>() {
                              @Override
                              public void onSuccess(Void unused) {
                                  dialog.dismiss();
                                  Toast.makeText(getContext(), "Posted Successfully !", Toast.LENGTH_SHORT).show();
                                  FragmentTransaction transaction=getActivity().getSupportFragmentManager().beginTransaction();
                                  transaction.replace(R.id.container,new HomeFragment());
                                  transaction.addToBackStack(null);
                                  transaction.commit();
                                  ReadableBottomBar readableBottomBar=getActivity().findViewById(R.id.bottom_bar);
                                  readableBottomBar.selectItem(0);

                              }
                          });

                      }
                  });
              }
          });
            else{
                Post post=new Post();
                post.setPostedBy(FirebaseAuth.getInstance().getUid());
                post.setPostDescription(binding.postDescription.getText().toString());
                post.setPostedAt(new Date().getTime());
                database.getReference().child("Posts").push().setValue(post).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                int postCount=0;
                                if(snapshot.child("postCount").exists())
                                 postCount=snapshot.child("postCount").getValue(Integer.class);

                                FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("postCount").setValue(postCount+1).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Posted Successfully !", Toast.LENGTH_SHORT).show();
                                        FragmentTransaction transaction=getActivity().getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.container,new HomeFragment());
                                        transaction.addToBackStack(null);
                                        transaction.commit();
                                        ReadableBottomBar readableBottomBar=getActivity().findViewById(R.id.bottom_bar);
                                        readableBottomBar.selectItem(0);
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
      }
  });

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data.getData()!=null){
             uri=data.getData();
            binding.postImg.setImageURI(uri);
            binding.postImg.setVisibility(View.VISIBLE);
            binding.postBtn.setBackground(ContextCompat.getDrawable(getContext(),R.drawable.follow_btn_bg));
            binding.postBtn.setTextColor(getContext().getResources().getColor(R.color.white));
            binding.postBtn.setEnabled(true);
        }
    }
}