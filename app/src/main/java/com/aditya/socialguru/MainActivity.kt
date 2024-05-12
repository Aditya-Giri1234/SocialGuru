package com.aditya.socialguru

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.marginBottom
import androidx.core.view.updatePadding
import androidx.lifecycle.LiveData
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import com.aditya.socialguru.databinding.ActivityMainBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.IntentTable
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setupWithNavController
import com.aditya.socialguru.domain_layer.helper.show
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.activity.ContainerActivity
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var navController: LiveData<NavController>? = null
    private var loader: MyLoader? = null

    private var bottomMargin:Int=0
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bottomMargin=-systemBars.bottom
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
        initUi()
        subscribeToObserver()
        subscribeToDestinationChanges()

    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
            AppBroadcastHelper.uploadStories.collect {
                MyLogger.v(isFunctionCall = true)
                when (it.first) {
                    Constants.StoryUploadState.StartUploading -> {
                        showLoader(
                            "Uploading",
                            state = Constants.StoryUploadState.StartUploading.name
                        )
                    }

                    Constants.StoryUploadState.Uploading -> {
                        updateLoader(
                            "Uploading",
                            it.second,
                            Constants.StoryUploadState.Uploading.name
                        )
                    }

                    Constants.StoryUploadState.UploadingFail -> {
                        hideLoader(
                            "Uploading Fail",
                            state = Constants.StoryUploadState.UploadingFail.name
                        )
                    }

                    Constants.StoryUploadState.SavingStory -> {
                        updateLoader(
                            "Saving Story",
                            state = Constants.StoryUploadState.SavingStory.name
                        )
                    }

                    Constants.StoryUploadState.StoryUploadedSuccessfully -> {
                        hideLoader(
                            "",
                            state = Constants.StoryUploadState.StoryUploadedSuccessfully.name
                        )
                    }

                    Constants.StoryUploadState.UrlNotGet -> {
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
                    val param=(binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
                    param.setMargins(0,0,0,0)
                    binding.localNavHostFragment.layoutParams= param
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
        fab.setOnClickListener {
            Intent(this@MainActivity,ContainerActivity::class.java).apply {
                putExtra(Constants.FRAGMENT_NAVIGATION,Constants.FragmentNavigation.AddPostFragment.name)
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
                    val param=(binding.localNavHostFragment.layoutParams as ViewGroup.MarginLayoutParams)
                    param.setMargins(0,0,0,actionBar?.height?:56)
                    binding.localNavHostFragment.layoutParams= param
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {

                }

            })
        }
    }

    private fun getStoryIntent(message: String, percentage: Int?, state: String): Intent {
        return Intent().apply {
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

    private fun updateLoader(message: String, percentage: Int? = null, state: String) {
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
                null,
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