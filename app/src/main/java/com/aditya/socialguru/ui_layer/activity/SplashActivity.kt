package com.aditya.socialguru.ui_layer.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.ActivitySplashBinding
import com.aditya.socialguru.domain_layer.service.FirebaseManager


class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            insets
        }
        window.navigationBarColor= Color.BLACK

        handleInitialization()
    }



    private fun handleInitialization() {
        binding.apply {
            Handler(Looper.getMainLooper()).postDelayed({
                val intent=if (FirebaseManager.isUserLogin()){
                    Intent(this@SplashActivity,MainActivity::class.java)
                }else{
                    Intent(this@SplashActivity,ContainerActivity::class.java)
                }
               intent.also(::startActivity)
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
                finish()

            }, 1000)
            setListener()
        }
    }

    private fun ActivitySplashBinding.setListener() {

    }


}