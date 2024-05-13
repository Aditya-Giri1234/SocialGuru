package com.aditya.socialguru

import android.animation.Animator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.aditya.socialguru.databinding.ActivityMainBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.IntentTable
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.setupWithNavController
import com.aditya.socialguru.domain_layer.helper.show
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.activity.ContainerActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var navController: LiveData<NavController>? = null
    private var loader: MyLoader? = null

    private var bottomMargin: Int = 0
    private val tagStory = Constants.LogTag.Story
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


    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
        subscribeToDestinationChanges()

    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
            AppBroadcastHelper.uploadStories.collect {
                MyLogger.v(tagStory, isFunctionCall = true)
                when (it.first) {
                    Constants.StoryUploadState.StartUploading -> {
                        MyLogger.v(tagStory, msg = "Loader is show with start uploading state ...")
                        showLoader(
                            "Uploading",
                            state = Constants.StoryUploadState.StartUploading.name
                        )
                    }

                    Constants.StoryUploadState.Uploading -> {
                        MyLogger.v(
                            tagStory,
                            msg = "Loader is show with  uploading state ... ${it.second}"
                        )
                        updateLoader(
                            "Uploading",
                            it.second ?: 0,
                            Constants.StoryUploadState.Uploading.name
                        )
                    }

                    Constants.StoryUploadState.UploadingFail -> {
                        MyLogger.v(tagStory, msg = "Loader is show with  UploadingFail state ...")
                        hideLoader(
                            "Uploading Fail",
                            state = Constants.StoryUploadState.UploadingFail.name
                        )
                    }

                    Constants.StoryUploadState.SavingStory -> {
                        MyLogger.v(tagStory, msg = "Loader is show with  SavingStory state ...")
                        updateLoader(
                            "Saving Story",
                            state = Constants.StoryUploadState.SavingStory.name
                        )
                    }

                    Constants.StoryUploadState.StoryUploadedSuccessfully -> {
                        MyLogger.v(
                            tagStory,
                            msg = "Loader is show with  StoryUploadedSuccessfully state ..."
                        )
                        hideLoader(
                            "",
                            state = Constants.StoryUploadState.StoryUploadedSuccessfully.name
                        )
                        Helper.showSuccessSnackBar(binding.coordLayout,"Story uploaded successfully !")
                    }

                    Constants.StoryUploadState.UrlNotGet -> {
                        MyLogger.v(tagStory, msg = "Loader is show with  UrlNotGet state ...")
                        hideLoader("", state = Constants.StoryUploadState.UrlNotGet.name)
                    }
                }
            }
        }

    }


    private fun subscribeToDestinationChanges() {
        MyLogger.v(isFunctionCall = true)
        navController?.observe(this) {
            MyLogger.v(msg = "Nav Controller is change ${it.currentDestination}")
            it.addOnDestinationChangedListener { controller, destination, arguments ->
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
            bottomNavigationView.menu[2].isEnabled = false

            setListener()
        }
        setUpBottomNav()
    }

    private fun setUpBottomNav() {
        MyLogger.v(isFunctionCall = true)
        binding.apply {
            val graphIds = listOf(
                R.navigation.home_fragment_bar,
                R.navigation.rescent_chat_fragment_bar,
                R.navigation.notification_fragment_bar,
                R.navigation.profile_fragment_bar
            )
            val controller = bottomNavigationView.setupWithNavController(
                graphIds, supportFragmentManager, R.id.localNavHostFragment, intent
            )
            MyLogger.i(msg = "Nav Controller is now set !")
            navController = controller
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

            Intent(this@MainActivity, ContainerActivity::class.java).apply {
                putExtra(
                    Constants.FRAGMENT_NAVIGATION,
                    Constants.FragmentNavigation.AddPostFragment.name
                )
                startActivity(this)
                overridePendingTransition(
                    R.anim.slide_in_top,
                    R.anim.slide_out_top
                )
            }
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
                    binding.bottomApp.show()
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

    private fun getStoryIntent(message: String, percentage: Int = 0, state: String): Intent {
        return Intent(Constants.BroadcastType.StoryUploading.name).apply {
            putExtra(Constants.TYPE,Constants.BroadcastType.StoryUploading.name)
            putExtra(IntentTable.UploadMessage.name, message)
            putExtra(IntentTable.UploadProgress.name, percentage)
            putExtra(IntentTable.UploadState.name, state)
        }
    }

    private fun showLoader(message: String, percentage: Int? = null, state: String) {
        if (loader == null) {
            loader = MyLoader(message)
        }
        loader?.show(supportFragmentManager, "My Loader")
    }

    private fun updateLoader(message: String, percentage: Int = 0, state: String) {
        sendBroadcast(
            getStoryIntent(
                message,
                percentage,
                state
            )
        )
    }

    private fun hideLoader(message: String, state: String) {
        sendBroadcast(
            getStoryIntent(
                message,
                0,
                state
            )
        )
        lifecycleScope.launch {
            delay(200)
            loader?.dismiss()
            loader = null
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
        return navController?.value?.navigateUp() ?: super.onSupportNavigateUp()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
        setUpBottomNav()
    }
}