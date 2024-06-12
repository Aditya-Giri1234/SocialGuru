package com.aditya.socialguru.ui_layer.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.ActivityContainerBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.convertParseUri
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
    private var bottomMargin: Int = 0


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
            bottomMargin = -systemBars.bottom
            v.setPadding(
                systemBars.left,
                systemBars.top,
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
        coordLayout.setOnApplyWindowInsetsListener { view, insets ->
            // Need to give bottom padding because this same amount padding apply by top so that it will dismiss the effect of edgeToEdge top padding
            view.updatePadding(bottom = bottomMargin)
            insets
        }
    }

    private fun checkIntent() {
        when(intent.getStringExtra(Constants.IntentTable.FragmentNavigation.name)){
            Constants.FragmentNavigation.SignInFragment.name->{
                handleSignInNavigation()
            }
            Constants.FragmentNavigation.AddPostFragment.name->{
                handleAddPostNavigation()
            }
            Constants.FragmentNavigation.ImageFragment.name->{
                val uri=intent.getStringExtra(Constants.IntentTable.MediaUri.name)
                handleImageNavigation(uri!!.convertParseUri())
            }
            Constants.FragmentNavigation.VideoFragment.name->{
                val uri=intent.getStringExtra(Constants.IntentTable.MediaUri.name)
                handleVideoNavigation(uri!!.convertParseUri())
            }
            else->{
                // Do nothing default onBoarding fragment show
            }
        }
    }

    private fun handleSignInNavigation() {
        isTopAndBottomAnimation=false
        navController?.safeNavigate(
            OnboardingScreenFragmentDirections.actionOnboardingScreenFragmentToSignInFragment(),
            Helper.giveAnimationNavOption(R.id.onboardingScreenFragment,true)
        )
    }
    private fun handleImageNavigation(uri:Uri) {
        isTopAndBottomAnimation=false
        navController?.safeNavigate(
            OnboardingScreenFragmentDirections.actionOnboardingScreenFragmentToShowImageFragment(uri),
            Helper.giveAnimationNavOption(R.id.onboardingScreenFragment,true)
        )
    }
    private fun handleVideoNavigation(uri:Uri) {
        isTopAndBottomAnimation=false
        navController?.safeNavigate(
            OnboardingScreenFragmentDirections.actionOnboardingScreenFragmentToShowVideoFragment(uri),
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


    @SuppressLint("MissingSuperCall")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {

        //These is for when add post fragment show bottom to up that time when it end need show top to bottom animation

        //For getting exact back stack entry count we need child fragment manager not supportFragment manager
        //Reason our fragment switch under nav host fragment , so we need nav host fragment means we need childFragment manager
        val navHostFragment=supportFragmentManager.fragments.first()
        MyLogger.w(msg = "Back pressed , current entry is ${navHostFragment.childFragmentManager.backStackEntryCount}")
        when{
            navHostFragment.childFragmentManager.backStackEntryCount==0&&isTopAndBottomAnimation->{
                MyLogger.w(msg = "back stack entry is 0 and this is  top bottom animation fragment !")
                finish()
                overridePendingTransition(0,R.anim.slide_out_bottom)
            }
            navHostFragment.childFragmentManager.backStackEntryCount==0->{
                MyLogger.w(msg = "back stack entry is 0 but this is not top bottom animation fragment !")
                finish()
                overridePendingTransition(0,R.anim.slide_out_right)
            }
            else->{
                MyLogger.w(msg = "Normal back stack removal !")
                navController?.popBackStack()
            }
        }
    }

    override fun onNavigateUp(): Boolean {
        return navController?.navigateUp() ?: super.onNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLogger.v(isFunctionCall = true)
    }


}