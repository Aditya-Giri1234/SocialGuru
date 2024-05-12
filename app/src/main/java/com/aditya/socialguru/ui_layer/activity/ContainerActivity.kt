package com.aditya.socialguru.ui_layer.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.ActivityContainerBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.fragment.intro_part.OnboardingScreenFragmentDirections


/**
 *[ContainerActivity] This class handle all screen which is not come under main activity or want show above main activity.
 * @param Constants.FRAGMENT_NAVIGATION value determine which fragment show first.
 * */
class ContainerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContainerBinding

    var navController : NavController?=null

    private var isTopAndBottomAnimation=true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.dark(
                    Color.BLACK
                )
        )
        binding=ActivityContainerBinding.inflate(layoutInflater)
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
        checkIntent()
    }

    private fun initUi() {
        MyLogger.v(isFunctionCall = true)
        binding.apply {

            navController=(supportFragmentManager.findFragmentById(R.id.introNavHostFragment) as NavHostFragment).navController


            setListener()
        }
    }

    private fun ActivityContainerBinding.setListener() {

    }

    private fun checkIntent() {
        when(intent.getStringExtra(Constants.FRAGMENT_NAVIGATION)){
            Constants.FragmentNavigation.SignInFragment.name->{
                handleSignInNavigation()
            }
            Constants.FragmentNavigation.AddPostFragment.name->{
                handleAddPostNavigation()
            }
            else->{
                // Do nothing default onBoarding fragment show
            }
        }
    }

    private fun handleSignInNavigation() {
        navController?.safeNavigate(
            OnboardingScreenFragmentDirections.actionOnboardingScreenFragmentToSignInFragment(),
            Helper.giveAnimationNavOption(R.id.onboardingScreenFragment,true)
        )
    }
    private fun handleAddPostNavigation() {
        isTopAndBottomAnimation=true
        navController?.safeNavigate(
            OnboardingScreenFragmentDirections.actionOnboardingScreenFragmentToAddPostFragment(),
            Helper.givePopUpNavOption(R.id.onboardingScreenFragment,true)
        )
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        //These is for when add post fragment show bottom to up that time when it end need show top to bottom animation
        if(supportFragmentManager.backStackEntryCount==0&&isTopAndBottomAnimation){
            finish()
            overridePendingTransition(0,R.anim.slide_out_bottom)
        }else{
            super.onBackPressed()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        MyLogger.v(isFunctionCall = true)
    }


}