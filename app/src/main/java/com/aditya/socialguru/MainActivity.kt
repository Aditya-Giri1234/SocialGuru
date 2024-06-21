package com.aditya.socialguru

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.databinding.ActivityMainBinding
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants

import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.manager.FCMTokenManager
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.viewmodel.MainViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.apache.http.auth.AUTH

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController


    private var bottomMargin: Int = 0
    private val tagStory = Constants.LogTag.Story

    private val mainViewModel by viewModels<MainViewModel>()
    private val pref by lazy {
        SharePref(this@MainActivity)
    }

    private val broadcastReceiver= object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action){
                Constants.AppBroadCast.LogIn.name->{
                    MyLogger.i(msg = "User login event come !")
                    mainViewModel.setDataLoadedStatus(false)
                    getData()
                }
                Constants.AppBroadCast.LogOut.name->{
                    MyLogger.i(msg = "User logout event come !")
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
            SystemBarStyle.dark(
                Color.BLACK
            )
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

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
        window.navigationBarColor = Color.BLACK

        MyLogger.v(isFunctionCall = true)
        handleInitialization()
        subscribeToBroadcast()
    }

    private fun subscribeToBroadcast() {
        val intentFilter=IntentFilter()
        intentFilter.addAction(Constants.AppBroadCast.LogIn.name)
        intentFilter.addAction(Constants.AppBroadCast.LogOut.name)

        ContextCompat.registerReceiver(this,broadcastReceiver,intentFilter,ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
        subscribeToDestinationChanges()
        getData()
    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppBroadcastHelper.mainActivityBottomNavHideByScroll.onEach {
                    if (it) {
                        hideBottomNavigationFotScrollEffect()
                    } else {
                        showBottomNavigationFotScrollEffect()
                    }
                }.launchIn(this)
                mainViewModel.user.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data?.let {
                                pref.setPrefUser(it)
                                setUpFirebaseCrashlytics()
                                MyLogger.i(msg = it, isJson = true)
                            }
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            Helper.showSnackBar(binding.coordLayout, response.message.toString())
                        }
                    }
                }.launchIn(this)

                mainViewModel.fcmToken.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            MyLogger.i(Constants.LogTag.FCMToken, msg = "Fcm response come !")
                            Helper.customToast(
                                this@MainActivity, "Fcm Token Send Successfully !",
                                Toast.LENGTH_SHORT
                            )
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            Helper.showSnackBar(binding.coordLayout, response.message.toString())
                        }
                    }
                }.launchIn(this)
            }
        }

    }

    private fun subscribeToDestinationChanges() {
        MyLogger.v(isFunctionCall = true)
        lifecycleScope.launch {
            navController.addOnDestinationChangedListener { controller, destination, arguments ->
                val bottomBarDestination = setOf(
                    R.id.homeFragment,
                    R.id.recentChatFragment,
                    R.id.notificationFragment,
                    R.id.profileFragment
                )
                MyLogger.d(msg = "Destination is change occurred :- ${destination.label}")
                if (bottomBarDestination.contains(destination.id)) {
                    showBottomNavigation()

                } else {
                    val param =
                        (binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
                    param.setMargins(0, 0, 0, 0)
                    binding.localNavHostFragment.layoutParams = param
                    hideBottomNavigation()
                }
            }
        }
    }

    private fun initUi() {
        MyLogger.v(isFunctionCall = true)
        binding.apply {
            navController =
                (supportFragmentManager.findFragmentById(R.id.localNavHostFragment) as NavHostFragment).findNavController()
            bottomNavigationView.menu[2].isEnabled = false
            bottomNavigationView.setupWithNavController(navController)
            setListener()
        }
    }


    private fun ActivityMainBinding.setListener() {
        //Just avoid extra padding in bottom app bar
        bottomApp.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(bottom = bottomMargin)
            insets
        }
        coordLayout.setOnApplyWindowInsetsListener { view, insets ->
            // Need to give bottom padding because this same amount padding apply by top so that it will dismiss the effect of edgeToEdge top padding
            view.updatePadding(bottom = bottomMargin)
            insets
        }
        fab.setOnClickListener {

            navController.currentDestination?.let {
                navController.safeNavigate(it.id,R.id.addPostFragment ,Helper.giveUpAndBottomAnimationNavOption())
            }
        }
    }

    private fun showBottomNavigationFotScrollEffect() {
        binding.apply {
            val param =
                (binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
            param.setMargins(0, 0, 0, actionBar?.height ?: 56)
            binding.localNavHostFragment.layoutParams = param
            bottomApp.myShow()
            fab.show()
        }
    }

    private fun hideBottomNavigationFotScrollEffect() {
        binding.apply {
            val param =
                (binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
            param.setMargins(0, 0, 0, 0)
            binding.localNavHostFragment.layoutParams = param
            fab.hide()
            bottomApp.gone()
        }
    }

    private fun hideBottomNavigation() {
        binding.bottomApp.animate().apply {
            duration = 300
            translationY(binding.bottomApp.height.toFloat())
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                    binding.fab.gone()
                }

                override fun onAnimationEnd(p0: Animator) {
                    binding.bottomApp.gone()
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {

                }

            })
        }
    }

    private fun showBottomNavigation() {
        binding.bottomApp.animate().apply {
            duration = 300
            translationY(binding.bottomApp.height.toFloat()) // Move the bottom navigation bar below the screen
            translationY(0f)
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(p0: Animator) {
                }

                override fun onAnimationEnd(p0: Animator) {
                    binding.bottomApp.myShow()
                    binding.fab.show()
                    val param =
                        (binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
                    param.setMargins(0, 0, 0, actionBar?.height ?: 56)
                    binding.localNavHostFragment.layoutParams = param
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {

                }

            })
        }
    }

    private fun setUpFirebaseCrashlytics() {
        lifecycleScope.myLaunch {
            FirebaseCrashlytics.getInstance().apply {
                pref.getPrefUser().first()?.let {
                    setCustomKey("NAME", it.userName.toString())
                    setCustomKey("EMAIL", it.userEmailId.toString())
                }

            }
        }
    }

    private fun getData() {
        if (AuthManager.currentUserId()!=null) {
            if (!mainViewModel.isDataLoaded) {
                getFCMToken()
                mainViewModel.getUser()
                mainViewModel.setDataLoadedStatus(true)
            }
        }
    }

    private fun getFCMToken() {
        MyLogger.v(Constants.LogTag.FCMToken, isFunctionCall = true)
        lifecycleScope.myLaunch {
            FCMTokenManager.generateFcmTokenByBackOfAlgo(this@MainActivity, 1) { token ->
                mainViewModel.setFcmToken(token)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        MyLogger.v(isFunctionCall = true)

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        //These is for when add post fragment show bottom to up that time when it end need show top to bottom animation

        //For getting exact back stack entry count we need child fragment manager not supportFragment manager
        //Reason our fragment switch under nav host fragment , so we need nav host fragment means we need childFragment manager
        val navHostFragment=supportFragmentManager.fragments.first()
        MyLogger.w(msg = "Back pressed , current entry is ${navHostFragment.childFragmentManager.backStackEntryCount}")
        when{
            navHostFragment.childFragmentManager.backStackEntryCount==0->{
                MyLogger.w(msg = "back stack entry is 0 but this is not top bottom animation fragment !")
                finish()
            }
            else->{
                MyLogger.w(msg = "Normal back stack removal !")
                navController.popBackStack()
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

}