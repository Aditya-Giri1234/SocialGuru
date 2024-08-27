package com.aditya.socialguru

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.databinding.ActivityMainBinding
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.worker.MyWorker
import com.aditya.socialguru.domain_layer.manager.FCMTokenManager
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.viewmodel.MainViewModel
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Duration

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemReselectedListener {

    private lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController


    private var bottomMargin: Int = 0
    private val tagStory = Constants.LogTag.Story

    private val mainViewModel by viewModels<MainViewModel>()
    private val pref by lazy {
        SharePref(this@MainActivity)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.AppBroadCast.LogIn.name -> {
                    MyLogger.i(msg = "User login event come !")
                    mainViewModel.setDataLoadedStatus(false)
                    mainViewModel.setListenerSetStatus(false)
                    handleStoryChange(2)
                    getData()
                }

                Constants.AppBroadCast.LogOut.name -> {
                    MyLogger.i(msg = "User logout event come !")
                    handleStoryChange(2)
                    lifecycleScope.launch {
                        delay(100)
                        killActivityAndCreateNewOne()
                    }
                }

                Constants.AppBroadCast.StoryChange.name -> {
                    MyLogger.i(msg = "Story change event come !")
                    handleStoryChange(intent.getIntExtra(Constants.DATA,0))
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

        //region:: This prevent to os not  to update ui your self when key board open
        WindowCompat.setDecorFitsSystemWindows(window, false)

//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
//            @Suppress("DEPRECATION")
//            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
//        }

        //endregion

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val systemBarsIMEInsets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.ime())
            bottomMargin = -systemBars.bottom

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBarsIMEInsets.bottom
            )
            insets
        }
        window.navigationBarColor = Color.BLACK

        MyLogger.v(isFunctionCall = true)
        handleInitialization()
        subscribeToBroadcast()
    }

    private fun subscribeToBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.AppBroadCast.LogIn.name)
        intentFilter.addAction(Constants.AppBroadCast.LogOut.name)
        intentFilter.addAction(Constants.AppBroadCast.StoryChange.name)

        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
        subscribeToDestinationChanges()
        getData()
        handleIntent()
    }

    private fun handleIntent() {
        when {
            intent.getBooleanExtra(
                Constants.IntentTable.LogOutOrDeleteAccountAcitivityHappend.name,
                false
            ) -> {
                navController.currentDestination?.id?.let {
                    navController.safeNavigate(
                        it,
                        R.id.onboardingScreenFragment,
                        Helper.giveAnimationNavOption(it, true)
                    )
                }
            }

            else -> {
                // Don't do anything
            }
        }
    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                navController.currentBackStack.onEach {
//
//                }.launchIn(this)
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

    @SuppressLint("RestrictedApi")
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
                    /*  val currentDestinationId = destination.id
                      val backStack = navController.currentBackStack.value.toList()

                      // Count the number of instances of the current destination in the back stack
                      val instanceCount = backStack.count { it.destination.id == currentDestinationId }

                      if (instanceCount > 1) {
                          // Pop the back stack up to the current destination
                          navController.popBackStack(currentDestinationId, true)
                          navController.popBackStack(currentDestinationId, false)
                      }*/
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
            bottomNavigationView.setOnItemReselectedListener(this@MainActivity)
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
                navController.safeNavigate(
                    it.id,
                    R.id.addPostFragment,
                    Helper.giveUpAndBottomAnimationNavOption()
                )
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
        if (AuthManager.currentUserId() != null) {
            if (!mainViewModel.isDataLoaded) {
                getFCMToken()
                mainViewModel.setDataLoadedStatus(true)
            }
            if (!mainViewModel.isListenerSet) {
                mainViewModel.getUser()
                mainViewModel.listenMySavedPost()
                mainViewModel.listenMyLikedPost()
                mainViewModel.setListenerSetStatus(true)
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

    private fun killActivityAndCreateNewOne() {
//        viewModelStore.clear()
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(Constants.IntentTable.LogOutOrDeleteAccountAcitivityHappend.name, true)
        )
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

    }


    override fun onResume() {
        AuthManager.isUserLogin().takeIf { it }?.let {
            mainViewModel.updateUserAvailability(true)
        }
        super.onResume()
        MyLogger.v(isFunctionCall = true)

    }

    override fun onStop() {
        AuthManager.isUserLogin().takeIf { it }?.let {
            mainViewModel.updateUserAvailability(false)
        }
        super.onStop()
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
        val navHostFragment = supportFragmentManager.fragments.first()
        MyLogger.w(msg = "Back pressed , current entry is ${navHostFragment.childFragmentManager.backStackEntryCount}")
        when {
            navHostFragment.childFragmentManager.backStackEntryCount == 0 -> {
                MyLogger.w(msg = "back stack entry is 0 but this is not top bottom animation fragment !")
                finish()
            }

            else -> {
                MyLogger.w(msg = "Normal back stack removal !")
                navController.popBackStack()
            }
        }
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        // Don't do any thing
    }

    private fun handleStoryChange(isUserWantToRemoveStory: Int) {
        launchCoroutineInIOThread {
            if (isUserWantToRemoveStory!=2) {
                if (isUserWantToRemoveStory == 1){
                    runWorker()
                }else{
                    MyLogger.w(
                        Constants.LogTag.JobManager,
                        msg = "User setting doesn't want to remove story so that i cancel my worker !"
                    )
                    cancelWorker()
                }

            } else {
                pref.getPrefUser().first()?.let {
                    it.userSetting?.let { setting ->
                        if (setting.isStoryRemoveAfter24HourActive == true) {
                            runWorker()
                        } else {
                            MyLogger.w(
                                Constants.LogTag.JobManager,
                                msg = "User setting doesn't want to remove story so that i cancel my worker !"
                            )
                            cancelWorker()
                        }
                    }
                } ?: run {
                    MyLogger.w(
                        Constants.LogTag.JobManager,
                        msg = "User setting is null and so that i cancel my worker !"
                    )
                    cancelWorker()
                }
            }
        }
    }

    private fun runWorker() {
        MyLogger.i(
            Constants.LogTag.JobManager,
            msg = "User setting  want to remove story so that i start my worker !"
        )
        val workRequest = PeriodicWorkRequestBuilder<MyWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.HOURS
        ).apply {
            setBackoffCriteria(
                backoffPolicy = BackoffPolicy.LINEAR,
                duration = Duration.ofSeconds(15)
            )
        }.build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            Constants.MY_CUSTOM_WORKER,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelWorker() {
        WorkManager.getInstance(applicationContext).cancelUniqueWork(Constants.MY_CUSTOM_WORKER)
    }

    override fun onDestroy() {
        mainViewModel.removeAllListener()
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

}