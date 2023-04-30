package com.example.socialguru.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.socialguru.ChatMainActivity;
import com.example.socialguru.FriendActivity;
import com.example.socialguru.ImageActivity;
import com.example.socialguru.R;
import com.example.socialguru.databinding.FragmentProfileBinding;
import com.example.socialguru.model.FollowModel;
import com.example.socialguru.model.User;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.util.ArrayList;


public class ProfileFragment extends Fragment {

public final static String image="image";
ArrayList<FollowModel> list=new ArrayList<>();
FragmentProfileBinding binding;
FirebaseStorage storage;
FirebaseDatabase database;
User user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding=FragmentProfileBinding.inflate(inflater,container,false);
        storage=FirebaseStorage.getInstance();
        database=FirebaseDatabase.getInstance();

        database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                     user=snapshot.getValue(User.class);
                    Picasso.get().load(user.getCoverPhoto()).placeholder(R.drawable.change_cover_photo).into(binding.coverPhoto);
                    binding.profileUserName.setText(user.getName());
                    binding.profession.setText(user.getProfession());
                   Picasso.get().load(user.getProfile()).placeholder(R.drawable.change_cover_photo).into(binding.profileImage);
                   binding.followers.setText(user.getFollowerCount()+"");
                   binding.postCount.setText(user.getPostCount()+"");
                   binding.friendCount.setText(user.getFriendCount()+"");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.freindView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(getContext(), FriendActivity.class));
            }
        });

        binding.commentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().startActivity(new Intent(getContext(), ChatMainActivity.class));
            }
        });






        binding.changeCoverPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,11);
            }
        });
        binding.verifiedAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,12);
            }
        });
        binding.profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user!=null) {
                    Intent intent = new Intent(getContext(), ImageActivity.class);
                    intent.putExtra(image, user.getProfile());
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getContext(), "Please wait until profile image load !", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.coverPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(user!=null) {
                    Intent intent = new Intent(getContext(), ImageActivity.class);
                    intent.putExtra(image, user.getCoverPhoto());
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getContext(), "Please wait until Cover image load !", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==11){
            if(data!=null){
                Uri uri=data.getData();
                binding.coverPhoto.setImageURI(uri);
                final StorageReference reference=storage.getReference().child("cover_photo").child(FirebaseAuth.getInstance().getUid());
                reference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getContext(), "Cover Photo Saved !", Toast.LENGTH_SHORT).show();
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("coverPhoto").setValue(uri.toString());
                            }
                        });
                    }
                });
            }
        }
        else{
            if(data!=null){
                Uri uri=data.getData();
                binding.profileImage.setImageURI(uri);
                final StorageReference reference=storage.getReference().child("profile_image").child(FirebaseAuth.getInstance().getUid());
                reference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getContext(), "Profile Photo Saved !", Toast.LENGTH_SHORT).show();
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                database.getReference().child("Users").child(FirebaseAuth.getInstance().getUid()).child("profile").setValue(uri.toString());
                            }
                        });
                    }
                });
            }

        }

    }
    @Override
    public void onResume() {
        super.onResume();
        ReadableBottomBar readableBottomBar = getActivity().findViewById(R.id.bottom_bar);
        if (readableBottomBar != null) {
            readableBottomBar.selectItem(4);
        }
    }

}