package com.aditya.socialguru.ui_layer.activity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.ActivityIntroBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.manager.MyLogger


class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    var navController : NavController?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.dark(
                    Color.BLACK
                )
        )
        binding=ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                0,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
        window.navigationBarColor= Color.BLACK

        MyLogger.v(isFunctionCall = true)
        handleInitialization()
    }





    private fun handleInitialization() {
        MyLogger.v(isFunctionCall = true)
        initUi()
    }

    private fun initUi() {
        MyLogger.v(isFunctionCall = true)
        binding.apply {

            navController=(supportFragmentManager.findFragmentById(R.id.introNavHostFragment) as NavHostFragment).navController



            setListener()
        }
    }

    private fun ActivityIntroBinding.setListener() {

    }


    override fun onDestroy() {
        super.onDestroy()
        MyLogger.v(isFunctionCall = true)
    }


}