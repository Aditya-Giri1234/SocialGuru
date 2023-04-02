package com.example.socialguru;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.example.socialguru.databinding.ActivityMainBinding;
import com.example.socialguru.fragment.AddFragment;
import com.example.socialguru.fragment.HomeFragment;
import com.example.socialguru.fragment.NotificationFragment;
import com.example.socialguru.fragment.ProfileFragment;
import com.example.socialguru.fragment.SearchFragment;
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


        binding.bottomBar.setOnItemSelectListener(new ReadableBottomBar.ItemSelectListener() {
            @Override
            public void onItemSelected(int i) {
                FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();
                switch (i){
                    case 0:
                        transaction.replace(R.id.container,new HomeFragment());
                        break;
                    case 1:
                        transaction.replace(R.id.container,new NotificationFragment());
                        break;
                    case 2:
                        transaction.replace(R.id.container,new AddFragment());
                        break;
                    case 3:
                        transaction.replace(R.id.container,new SearchFragment());
                        break;
                   case 4:
                        transaction.replace(R.id.container,new ProfileFragment());
                        break;

                }
                transaction.commit();
            }
        });

    }
}