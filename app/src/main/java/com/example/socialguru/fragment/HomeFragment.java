package com.example.socialguru.fragment;


import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.example.socialguru.MainActivity;
import com.example.socialguru.R;
import com.example.socialguru.adapter.PostAdapter;
import com.example.socialguru.adapter.StoryAdapter;
import com.example.socialguru.model.Post;
import com.example.socialguru.model.StoryModel;
import com.example.socialguru.model.UserStories;
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
import com.makeramen.roundedimageview.RoundedImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class HomeFragment extends Fragment {

    RecyclerView storyRv;
    ShimmerRecyclerView postRv;
    ArrayList<StoryModel> storyList;
    ArrayList<Post> postList;
    PostAdapter postAdapter;
    FirebaseStorage storage;

    FirebaseDatabase database;
    FirebaseAuth auth;
    ImageView profile_image,save;

    RoundedImageView addStoryImage;
    ActivityResultLauncher<String> galleryLauncher;
    ProgressDialog dialog;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialog=new ProgressDialog(getContext());
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Story Uploading ");
        dialog.setMessage("Please Wait");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_home, container, false);
        postRv=view.findViewById(R.id.dashboardRv);
        postRv.showShimmerAdapter();

        storyRv=view.findViewById(R.id.storyRV);
        profile_image=view.findViewById(R.id.profile_image);
        database=FirebaseDatabase.getInstance();
        auth=FirebaseAuth.getInstance();
        storyList=new ArrayList<>();
        storage=FirebaseStorage.getInstance();


        profile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction=getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.container,new ProfileFragment());
                transaction.commit();
                ReadableBottomBar readableBottomBar=view.findViewById(R.id.bottom_bar);
                readableBottomBar.selectItem(4);
            }
        });

       // Story adapter
        StoryAdapter storyAdapter=new StoryAdapter(storyList,getContext());
        storyRv.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false));
        storyRv.setNestedScrollingEnabled(false);
        storyRv.setAdapter(storyAdapter);

        database.getReference().child("Stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    storyList.clear();
                    for(DataSnapshot dataSnapshot:snapshot.getChildren()){
                        StoryModel storyModel=dataSnapshot.getValue((StoryModel.class));
                        storyModel.setStoryBy(dataSnapshot.getKey());
                        storyModel.setStoryAt(dataSnapshot.child("postedBy").getValue(Long.class));
                        ArrayList<UserStories> stories=new ArrayList<>();
                        for(DataSnapshot dataSnapshot1:dataSnapshot.child("UserStories").getChildren()){
                            UserStories userStories=dataSnapshot1.getValue(UserStories.class);
                            stories.add(userStories);
                        }
                        storyModel.setStories(stories);
                        storyList.add(storyModel);
                    }
                    storyAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        // Dashboard RecyclerView or Post RecyclerView


        postList=new ArrayList<>();

        database.getReference().child("Posts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot dataSnapshot:snapshot.getChildren()){
                    Post post=dataSnapshot.getValue(Post.class);
                    post.setPostId(dataSnapshot.getKey());
                    postList.add(post);
                }
                postRv.setAdapter(postAdapter);
                postRv.hideShimmerAdapter();
                Collections.reverse(postList);
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


         postAdapter=new PostAdapter(getContext(),postList);
        postRv.setLayoutManager(new LinearLayoutManager(getContext()));
        postRv.setNestedScrollingEnabled(false);


        addStoryImage=view.findViewById(R.id.story);
        addStoryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryLauncher.launch("image/*");
            }
        });

        galleryLauncher=registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                addStoryImage.setImageURI(result);
                dialog.show();
                final StorageReference reference=storage.getReference().child("stories").child(FirebaseAuth.getInstance().getUid()).child(new Date().getTime()+"");
                reference.putFile(result).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                StoryModel storyModel=new StoryModel();

                                storyModel.setStoryAt(new Date().getTime());
                                database.getReference().child("Stories").child(FirebaseAuth.getInstance().getUid()).child("postedBy").setValue(storyModel.getStoryAt()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        UserStories userStories=new UserStories(uri.toString(),storyModel.getStoryAt());
                                        database.getReference().child("Stories").child(FirebaseAuth.getInstance().getUid()).child("UserStories").push().setValue(userStories).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                dialog.dismiss();
                                                Toast.makeText(getContext(), "Story Uploaded !", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        return view;
    }
}