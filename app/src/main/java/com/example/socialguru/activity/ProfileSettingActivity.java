package com.example.socialguru.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.socialguru.R;
import com.example.socialguru.databinding.ActivityProfileSettingBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileSettingActivity extends AppCompatActivity {
    ActivityProfileSettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityProfileSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar6);
        binding.toolbar6.setTitle("Profile Setting");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        binding.changeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.nameET.getText().toString().isEmpty()) {
                    binding.nameET.setError("Enter your Name");
                    return;
                } else {
                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                            .child("name").setValue(binding.nameET.getText().toString());
                    Toast.makeText(ProfileSettingActivity.this, "Name Change ", Toast.LENGTH_SHORT).show();
                    binding.nameET.setText("");
                }
            }
        });
        binding.changeProfession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.professionET.getText().toString().isEmpty()){
                    binding.professionET.setError("Enter your Profession");
                    return;
                }
                else{
                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                            .child("profession").setValue(binding.professionET.getText().toString());
                    Toast.makeText(ProfileSettingActivity.this, "Profession Change ", Toast.LENGTH_SHORT).show();
                    binding.professionET.setText("");
                }
            }
        });

        binding.changeDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.profileDescriptionET.getText().toString().isEmpty()){
                    binding.profileDescriptionET.setError("Enter your Description");
                    return;
                }
                else{
                    FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getUid())
                            .child("description").setValue(binding.profileDescriptionET.getText().toString());
                    Toast.makeText(ProfileSettingActivity.this, "Profile Description Change ", Toast.LENGTH_SHORT).show();
                    binding.profileDescriptionET.setText("");
                }
            }
        });










    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}