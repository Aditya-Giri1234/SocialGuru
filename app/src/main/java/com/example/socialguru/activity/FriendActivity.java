package com.example.socialguru.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

import com.example.socialguru.adapter.FreindViewPagerAdapter;
import com.example.socialguru.adapter.ViewPagerAdapter;
import com.example.socialguru.databinding.ActivityFriendBinding;
import com.google.android.material.tabs.TabLayout;

public class FriendActivity extends AppCompatActivity {

    ActivityFriendBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityFriendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.viewPager.setAdapter(new FreindViewPagerAdapter(getSupportFragmentManager()));
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }
}