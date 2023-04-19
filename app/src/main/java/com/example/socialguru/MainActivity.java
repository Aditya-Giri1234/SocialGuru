package com.example.socialguru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.socialguru.databinding.ActivityMainBinding;
import com.example.socialguru.fragment.AddPostFragment;
import com.example.socialguru.fragment.HomeFragment;
import com.example.socialguru.fragment.NotificationFragment;
import com.example.socialguru.fragment.ProfileFragment;
import com.example.socialguru.fragment.SearchFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.iammert.library.readablebottombar.ReadableBottomBar;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.container,new HomeFragment());
        transaction.commit();

        setSupportActionBar(binding.toolbar);
        MainActivity.this.setTitle("My Profile");
        binding.toolbar.setVisibility(View.GONE);

        binding.bottomBar.setOnItemSelectListener(new ReadableBottomBar.ItemSelectListener() {
            @Override
            public void onItemSelected(int i) {
                FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();
                switch (i){
                    case 0:
                        binding.toolbar.setVisibility(View.GONE);
                        transaction.replace(R.id.container,new HomeFragment());
                        break;
                    case 1:
                        binding.toolbar.setVisibility(View.GONE);

                        transaction.replace(R.id.container,new NotificationFragment());
                        break;
                    case 2:
                        binding.toolbar.setVisibility(View.GONE);
                        transaction.replace(R.id.container,new AddPostFragment());
                        break;
                    case 3:
                        binding.toolbar.setVisibility(View.GONE);
                        transaction.replace(R.id.container,new SearchFragment());
                        break;
                   case 4:
                       binding.toolbar.setVisibility(View.VISIBLE);
                       transaction.replace(R.id.container,new ProfileFragment());
                        break;

                }
                transaction.commit();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item,menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.signOut){
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this,LoginActivity.class));
            finish();
        }
        return true;
    }
}