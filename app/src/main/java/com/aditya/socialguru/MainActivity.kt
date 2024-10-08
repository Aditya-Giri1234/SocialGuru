package com.aditya.socialguru

import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.isVisible
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
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.git.GitHubRelease
import com.aditya.socialguru.databinding.ActivityMainBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.ApkFolderName
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.giveMeColor
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInDefaultThread
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.worker.MyWorker
import com.aditya.socialguru.domain_layer.manager.FCMTokenManager
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.MyNotificationManager
import com.aditya.socialguru.domain_layer.manager.NetworkManager
import com.aditya.socialguru.domain_layer.manager.NetworkManager.monitorInternet
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.fragment.dialog_fragment.AppUpdateDialog
import com.aditya.socialguru.ui_layer.viewmodel.MainViewModel
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.time.Duration

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemReselectedListener,
    AlertDialogOption {

    private lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController


    private var bottomMargin: Int = 0
    private var noInternetHideWork: Job? = null
    private val tagStory = Constants.LogTag.Story
    private var appUpdateDialog: AppUpdateDialog? = null
    private val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
    private var dialogInvokeType = MainActivityDialogInvokation.ForRationDialog
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private val mainViewModel by viewModels<MainViewModel>()
    private val pref by lazy {
        SharePref(this@MainActivity)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Custom behavior for back press
            handleBackPressed()
        }
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
                    askPermission()
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
                    handleStoryChange(intent.getIntExtra(Constants.DATA, 0))
                }

                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    val downloadManager = getSystemService<DownloadManager>()

                    downloadManager?.query(DownloadManager.Query().setFilterById(downloadId))
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                when (cursor.getIntOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        cursor.getStringOrNull(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                                            ?.let { installApk(Uri.parse(it)) }
                                    }

                                    DownloadManager.STATUS_FAILED -> {
                                        MyLogger.e(
                                            msg = "Download failed: ${
                                                cursor.getString(
                                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                                                )
                                            }"
                                        )
                                    }
                                }
                            }
                        }
                }
            }

        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, you can post notifications
        } else {
            if (shouldShowRequestPermissionRationale(notificationPermission)) {
                dialogInvokeType = MainActivityDialogInvokation.ForRationDialog
                AlertDialog(
                    "Enable notifications to stay updated on comments, chat messages, likes, and friend requests. Stay connected!",
                    this,
                    isForShowDelete = false,
                    negativeMessage = "Cancel",
                    positiveMessage = "Yes"
                ).show(supportFragmentManager, "My_dialog")
            } else {
                dialogInvokeType = MainActivityDialogInvokation.ForAppSetting
                AlertDialog(
                    "It looks like notifications are disabled. Enable them in settings to stay updated on comments, messages, likes, and friend requests!",
                    this,
                    isForShowDelete = false,
                    negativeMessage = "Cancel",
                    positiveMessage = "Setting"
                ).show(supportFragmentManager, "My_dialog")
            }
        }
    }


    private val installApkLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Installation was successful
                Helper.customToast(this, "APK installed successfully.", Toast.LENGTH_SHORT)
            } else {
                // Installation failed
                Helper.customToast(this, "APK installation failed.", Toast.LENGTH_SHORT)
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
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)

        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun handleInitialization() {
        initUi()
        subscribeToInternetObserver()
        subscribeToObserver()
        subscribeToDestinationChanges()
        getData()
        handleIntent()
        askPermission()
        deleteOldApkFileFromAppPrivateFolder()
    }

    private fun subscribeToInternetObserver() {
        NetworkManager.registerNetworkCallback(this)
    }

    private fun unRegisterToInternetObserver(){
        NetworkManager.unregisterNetworkCallback()
    }

    private fun askPermission() {
        if (AuthManager.currentUserId() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        notificationPermission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestNotificationPermissionLauncher.launch(notificationPermission)
                }
            }
        }
    }

    private fun handleIntent() {
        when {
            intent.getBooleanExtra(
                Constants.IntentTable.LogOutOrDeleteAccountActivityHappened.name,
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
                handleDeepLink(intent)
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
                            if (!response.hasBeenMessagedToUser) {
                                response.hasBeenMessagedToUser = true
                                when (response.message) {
                                    Constants.ErrorMessage.InternetNotAvailable.message -> {
                                        jobQueue.add {
                                            mainViewModel.removeAllListener()
                                            mainViewModel.getUser()
                                            mainViewModel.listenMySavedPost()
                                            mainViewModel.listenMyLikedPost()
                                            mainViewModel.listenAuthOfUser()
                                        }
                                    }

                                    else -> {
                                        Helper.showSnackBar(
                                            binding.coordLayout,
                                            response.message.toString()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.launchIn(this)

                mainViewModel.appUpdating.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            appUpdateDialog?.dismissNow()
                            if(!response.hasBeenMessagedToUser){
                                response.hasBeenMessagedToUser =true
                                response.data?.let {
                                    installApk(it.toUri())
                                }
                            }
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            appUpdateDialog?.dismissNow()
                            if (!response.hasBeenMessagedToUser) {
                                response.hasBeenMessagedToUser = true
                                Helper.showSnackBar(
                                    binding.coordLayout,
                                    response.message.toString()
                                )
                            }
                        }
                    }
                }.launchIn(this)

                mainViewModel.appUpdate.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            MyLogger.i(
                                Constants.LogTag.FCMToken,
                                msg = response.data,
                                isJson = true,
                                jsonTitle = "App Update Info Come !"
                            )
                            handleAppUpdate(response.data)
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            if (!response.hasBeenMessagedToUser) {
                                response.hasBeenMessagedToUser = true
                                when (response.message) {
                                    Constants.ErrorMessage.InternetNotAvailable.message -> {
                                        jobQueue.add {
                                            mainViewModel.getLatestUpdate(packageName)
                                        }
                                    }

                                    else -> {
                                        Helper.showSnackBar(
                                            binding.coordLayout,
                                            response.message.toString()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.launchIn(this)
                monitorInternet().onEach { isInternetAvailable ->
                    if (isInternetAvailable) {
                        hideNoInternet()
                        jobQueue.forEach {
                            it.invoke()
                        }
                        jobQueue.clear()

                    } else {
                        showNoInternet()
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
                            mainViewModel.getLatestUpdate(packageName)
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            if (!response.hasBeenMessagedToUser) {
                                response.hasBeenMessagedToUser = true
                                when (response.message) {
                                    Constants.ErrorMessage.InternetNotAvailable.message -> {
                                        jobQueue.add {
                                            getFCMToken()
                                            mainViewModel.getLatestUpdate(packageName)
                                        }
                                    }

                                    else -> {
                                        Helper.showSnackBar(
                                            binding.coordLayout,
                                            response.message.toString()
                                        )
                                    }
                                }
                            }
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
        onBackPressedDispatcher.addCallback(this@MainActivity, backPressedCallback)
    }

    private fun showBottomNavigationFotScrollEffect() {
        binding.apply {
            val param =
                (binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
            param.setMargins(0, 0, 0, actionBar?.height ?: 56)
            binding.localNavHostFragment.layoutParams = param
            bottomApp.myShow()
            fab.show()
            coordinateBottomAppBar.myShow()
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
            coordinateBottomAppBar.gone()
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
                    binding.coordinateBottomAppBar.gone()
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
            binding.coordinateBottomAppBar.myShow()
            duration = 300
            translationY(-binding.bottomApp.height.toFloat()) // Move the bottom navigation bar below the screen
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
                mainViewModel.listenAuthOfUser()
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
                .putExtra(
                    Constants.IntentTable.LogOutOrDeleteAccountActivityHappened.name,
                    true
                )
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

    fun handleBackPressed() {
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
                binding.apply {
                    if(coordinateBottomAppBar.isVisible){
                        MyLogger.w(msg = "Special back stack removal !")
                        val currentFragmentId = navController.currentDestination?.id
                        // Check if the current fragment is not the HomeFragment
                        if (currentFragmentId != R.id.homeFragment) {
                            // Clear backstack until HomeFragment is visible
                            navController.popBackStack(R.id.homeFragment, false)
                        } else {
                            // If HomeFragment is visible, execute the default behavior (exit app)
                            finish()
                        }
                    }else{
                        MyLogger.w(msg = "Normal back stack removal !")
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    override fun onNavigationItemReselected(item: MenuItem) {
        // Don't do any thing
    }

    private fun handleStoryChange(isUserWantToRemoveStory: Int) {
        launchCoroutineInIOThread {
            if (isUserWantToRemoveStory != 2) {
                if (isUserWantToRemoveStory == 1) {
                    runWorker()
                } else {
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

    private fun handleAppUpdate(data: GitHubRelease?) {
        data?.let {
            launchCoroutineInIOThread {
                if (it.isDownloadable(this@MainActivity)) {
                    com.aditya.socialguru.domain_layer.helper.runOnUiThread {
                        appUpdateDialog =
                            AppUpdateDialog(it, "Cancel", "Update", negativeAction = null) {
                                mainViewModel.updateApp(it) { progress ->
                                    runOnUiThread {
                                        if (progress == null) {
                                            appUpdateDialog?.updateDialog(
                                                "Apk Downloading",
                                                null,
                                                false,
                                                true
                                            )
                                        } else {
                                            appUpdateDialog?.updateDialog(
                                                "Downloading",
                                                progress,
                                                false,
                                                false
                                            )
                                        }
                                    }
                                }
                            }
                        appUpdateDialog?.show(supportFragmentManager, "Update_Dialog")
                    }
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

    private fun showNoInternet() {
        noInternetHideWork?.cancel()
        binding.apply {
            linearInternet.myShow() // Make it visible
            linearInternet.setBackgroundColor(giveMeColor(R.color.red))
            tvNoInternet.text = getString(R.string.no_internet_available)
            tvNoInternet.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_not_internet),
                null, null, null
            )
            slideUp(linearInternet)
        }
    }

    private fun hideNoInternet() {
        noInternetHideWork?.cancel()
        binding.apply {
            linearInternet.setBackgroundColor(giveMeColor(R.color.green))
            tvNoInternet.text = getString(R.string.internet_available)
            tvNoInternet.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_internet),
                null, null, null
            )
            noInternetHideWork = lifecycleScope.launch {
                delay(500)
                if (::binding.isInitialized) {
                    slideDown(binding.linearInternet)
                }
            }
        }
    }

    // Slide up animation (visible from bottom to top)
    private fun slideUp(view: View) {
        view.myShow() // Make sure the view is visible
        view.translationY = view.height.toFloat() // Start from below the view
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                val height = view.height.toFloat()
                ObjectAnimator.ofFloat(view, "translationY", height, 0f).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    start()
                }
                return true
            }
        })
    }

    // Slide down animation (hide from top to bottom)
    private fun slideDown(view: View) {
        val height = view.height.toFloat()
        ObjectAnimator.ofFloat(view, "translationY", 0f, height).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
            addListener(onEnd = {
                view.gone() // Hide the view after the animation ends
            })
        }
    }


    private fun handleDeepLink(intent: Intent) {
        MyNotificationManager.clearAllNotification(this)
        val isForFcmIntent = intent.getBooleanExtra(Constants.IS_FCM_INTENT, false)
        if (isForFcmIntent) {
            when (intent.getStringExtra(Constants.FCM_INTENT_FOR)) {
                Constants.FcmIntentFor.ProfileScreen.name -> {
                    val userId = intent.getStringExtra(Constants.DATA)
                    if (userId != null) {
                        val directions =
                            BottomNavigationBarDirections.actionGlobalProfileViewFragment(userId)
                        navController.safeNavigate(
                            directions,
                            Helper.giveAnimationNavOptionWithSingleTop()
                        )
                    }
                }

                Constants.FcmIntentFor.SingleChatScreen.name -> {
                    val userId = intent.getStringExtra(Constants.DATA)
                    if (userId != null) {
                        val directions =
                            BottomNavigationBarDirections.actionGlobalChatFragment(userId)
                        navController.safeNavigate(
                            directions,
                            Helper.giveAnimationNavOptionWithSingleTop()
                        )
                    }
                }

                Constants.FcmIntentFor.GroupChatScreen.name -> {
                    val groupId = intent.getStringExtra(Constants.DATA)
                    if (groupId != null) {
                        val directions =
                            BottomNavigationBarDirections.actionGlobalGroupChatFragment(groupId)
                        navController.safeNavigate(
                            directions,
                            Helper.giveAnimationNavOptionWithSingleTop()
                        )
                    }
                }

                Constants.FcmIntentFor.PostScreen.name -> {
                    val postId = intent.getStringExtra(Constants.DATA)
                    if (postId != null) {
                        val directions =
                            BottomNavigationBarDirections.actionGlobalDetailPostFragment(postId)
                        navController.safeNavigate(
                            directions,
                            Helper.giveAnimationNavOptionWithSingleTop()
                        )
                    }
                }
            }
        } else {
            intent.data?.let { uri ->
                val isUriHostMatchMyHost = intent.data?.host == packageName
                if (isUriHostMatchMyHost) {
                    uri.path?.let { path ->
                        when {
                            path.startsWith("/post") -> {
                                val pathSegments = uri.pathSegments
                                if (pathSegments.isNotEmpty() && pathSegments.size > 1) {
                                    val postId =
                                        pathSegments[1] // Assuming postId is the first path segment
                                    val directions =
                                        BottomNavigationBarDirections.actionGlobalDetailPostFragment(
                                            postId
                                        )
                                    navController.safeNavigate(
                                        directions,
                                        Helper.giveAnimationNavOptionWithSingleTop()
                                    )
                                }
                            }

                            path.startsWith("/image") -> {
                                val encodedUrl = path.removePrefix("/image/")
                                val directions =
                                    BottomNavigationBarDirections.actionGlobalShowImageFragment(
                                        (ShareManager.decodeUrl(encodedUrl)).toUri()
                                    )
                                navController.safeNavigate(
                                    directions,
                                    Helper.giveAnimationNavOptionWithSingleTop()
                                )
                            }

                            path.startsWith("/video") -> {
                                val encodedUrl = path.removePrefix("/video/")
                                val directions =
                                    BottomNavigationBarDirections.actionGlobalShowVideoFragment(
                                        (ShareManager.decodeUrl(encodedUrl)).toUri()
                                    )
                                navController.safeNavigate(
                                    directions,
                                    Helper.giveAnimationNavOptionWithSingleTop()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun installApk(downloadedUri: Uri) {

        // Save preferences in a coroutine
        launchCoroutineInDefaultThread {
            pref.setPrefLong(
                SharePref.PreferencesKeys.APK_INSTALLED_TIME,
                System.currentTimeMillis()
            )
            pref.setPrefString(
                SharePref.PreferencesKeys.APK_FILE_NAME,
                downloadedUri.lastPathSegment.toString()
            )
        }

        // Get the file from the downloaded URI
        val apkFile = File(
            downloadedUri.path ?: "${filesDir}/$ApkFolderName/${downloadedUri.lastPathSegment}"
        )

        // Get the content URI using FileProvider
        val installUri: Uri = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(installUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        installApkLauncher.launch(installIntent)
    }


    private fun deleteOldApkFileFromAppPrivateFolder() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            launchCoroutineInDefaultThread {
                val lastApkInstallTime =
                    pref.getPrefLong(SharePref.PreferencesKeys.APK_INSTALLED_TIME).first()
                val apkName =
                    pref.getPrefString(SharePref.PreferencesKeys.APK_FILE_NAME).first()
                if (lastApkInstallTime != 0L && apkName != null) {
                    val currentTime = System.currentTimeMillis()
                    val diffTime = currentTime - lastApkInstallTime

                    if (diffTime > 1 * 60 * 60 * 1000) {
                        val folderName = File(filesDir, ApkFolderName)
                        if (folderName.exists()) {
                            val isAllDeleted = folderName.deleteRecursively()
                            if (isAllDeleted) {
                                pref.deleteKey(SharePref.PreferencesKeys.APK_FILE_NAME)
                                pref.deleteKey(SharePref.PreferencesKeys.APK_INSTALLED_TIME)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onResult(isYes: Boolean) {
        when (dialogInvokeType) {
            MainActivityDialogInvokation.ForRationDialog -> {
                if (isYes) {
                    requestNotificationPermissionLauncher.launch(notificationPermission)
                }
            }

            MainActivityDialogInvokation.ForAppSetting -> {
                if (isYes) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onDestroy() {
        unRegisterToInternetObserver()
        backPressedCallback.remove()
        mainViewModel.removeAllListener()
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }


}

private enum class MainActivityDialogInvokation {
    ForRationDialog,
    ForAppSetting
}
