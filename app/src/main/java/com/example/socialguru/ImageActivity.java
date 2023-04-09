package com.example.socialguru;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.socialguru.databinding.ActivityImageBinding;
import com.example.socialguru.fragment.ProfileFragment;
import com.squareup.picasso.Picasso;

public class ImageActivity extends AppCompatActivity {
     ActivityImageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent=getIntent();
        Picasso.get().load(intent.getStringExtra(ProfileFragment.image)).placeholder(R.drawable.change_cover_photo).into(binding.imageSee);
    }
}