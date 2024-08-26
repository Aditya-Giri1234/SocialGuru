package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.story.StoryText
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.databinding.PopUpHomeSettingBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.bufferWithDelay
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.StoryTypeOptions
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.adapter.StoryAdapter
import com.aditya.socialguru.ui_layer.fragment.dialog_fragment.StoryTypeOptionDialog
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeDiscoverPostFragment
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeFollowingPostFragment
import com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar.HomeViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), StoryTypeOptions {

    //region:: Variables
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val tagStory = Constants.LogTag.Story
    private val tagPost = Constants.LogTag.Post
    private val userData: MutableList<UserStories> = mutableListOf<UserStories>()
    private val MAX_VIDEO_SIZE_MB = 30f
    private var myLoader: MyLoader? = null
    private var isFragmentSwitchHappen = true

    private var _storyAdapter: StoryAdapter? = null
    private val storyAdapter get() = _storyAdapter!!


    // Don't use lazy it lead to memory leak and not leave old view when fragment switching and come back this view that time thi variable if initialize with lazy that not leave old view and crash app
    private var _pagerAdapter: NormalPagerAdapter? = null
    private val pagerAdapter get() = _pagerAdapter!!


    private val navController get() = (requireActivity() as MainActivity).navController

    private val pref by lazy {
        SharePref(requireContext())
    }


    private val homeViewModel: HomeViewModel by navGraphViewModels(R.id.homeFragment) {
        ViewModelProvider.AndroidViewModelFactory(requireActivity().application)
    }

    val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            MyLogger.v(tagStory, msg = "User select pic now send to server !")
            handleUserSelectedMedia(Constants.StoryType.Image, uri)

        } else {
            MyLogger.v(tagStory, msg = "User revoke or cancel upload story !")
        }
    }

    val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {


            val videoLength = Helper.getVideoSize(requireActivity(), uri)

            when {
                videoLength == -1f -> {
                    MyLogger.e(
                        tagStory,
                        msg = "User selected video length is -1 means some error occurred !"
                    )
                    Helper.showSnackBar(
                        (requireActivity() as MainActivity).findViewById(
                            R.id.coordLayout
                        ),
                        "Some error occurred during video fetching  , may be file size to large!"
                    )
                }

                videoLength >= MAX_VIDEO_SIZE_MB -> {
                    MyLogger.e(
                        tagStory,
                        msg = "User selected video length is $videoLength and max lenght :- $MAX_VIDEO_SIZE_MB  which exceeded ! "
                    )
                    Helper.showSnackBar(
                        (requireActivity() as MainActivity).findViewById(
                            R.id.coordLayout
                        ), "Video length exceeded , max length is $MAX_VIDEO_SIZE_MB mb !"
                    )
                }

                else -> {
                    MyLogger.v(
                        tagStory,
                        msg = "User selected video length is $videoLength and max length :- $MAX_VIDEO_SIZE_MB  which is not exceeded 😁! "
                    )
                    MyLogger.v(tagStory, msg = "User select video now send to server !")
                    handleUserSelectedMedia(Constants.StoryType.Video, uri)
                }
            }


        } else {
            MyLogger.v(tagStory, msg = "User revoke or cancel upload story !")
        }
    }

    private val broadcastManager = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Constants.BroadcastType.StoryUploading.name -> {
                    MyLogger.i(tagStory, msg = "Story Uploading event come !")
                    val storyText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getSerializableExtra(Constants.DATA, StoryText::class.java)
                    } else {
                        intent.getSerializableExtra(Constants.DATA) as StoryText?
                    }
                    handleUserSelectedMedia(Constants.StoryType.Text, text = storyText)

                }
            }
        }

    }

    private var isUserStorySnackBarShouldShow = true


    //endregion

    //region:: Top level function
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeToBroadcastReceiver()
        MyLogger.v(isFunctionCall = true)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(isFunctionCall = true)

        handleInitialization()

    }

    private fun handleInitialization() {
        MyLogger.v(isFunctionCall = true)

        initUi()
        subscribeToObserver()
        //This flag help me when i navigate to other app and come back to this fragment then fragment state maintain so that if frament scrolled already then fragment go to previous scroll position which trigger to nested scroll listener down scroll logic. So below help me to skip first time down scroll when navigate happen between fragment.
        isFragmentSwitchHappen = true
        if (!homeViewModel.isDataLoaded) {
            getData()
            homeViewModel.setDataLoadedStatus(true)
        }

    }

    private fun subscribeToBroadcastReceiver() {
        val filter = IntentFilter()
        filter.addAction(Constants.BroadcastType.StoryUploading.name)

        ContextCompat.registerReceiver(
            requireContext(),
            broadcastManager,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun subscribeToObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.userStories.onEach { response ->
                    response.let {
                        MyLogger.i(msg = "Response coming in ui screen !")
                        when (response) {
                            is Resource.Success -> {
                                response.hasBeenMessagedToUser = true
                                userData.clear()
                                response.data?.let {
                                    setData(it)
                                } ?: run {
                                    setData()
                                    if (isUserStorySnackBarShouldShow) {
                                        Helper.showSnackBar(
                                            (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                            response.message.toString()
                                        )
                                    }
                                }


                                isUserStorySnackBarShouldShow = false

                            }

                            is Resource.Loading -> {
                                isUserStorySnackBarShouldShow = true
                            }

                            is Resource.Error -> {
                                response.hasBeenMessagedToUser = true
                                if (isUserStorySnackBarShouldShow) {
                                    Helper.showSnackBar(
                                        (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                        response.message.toString()
                                    )
                                    isUserStorySnackBarShouldShow = false
                                }
                            }
                        }
                    }

                }.launchIn(this)

                // bufferWithDelay is because collector is slow ( MyLoader take some  millisecond time to show ui  and publisher is fast. So it throw / public item and collector collect item and call loader but loader is not initialize at. So when you access it ui , it throw null pointer exception because binding is not initialize.
                AppBroadcastHelper.uploadStories.bufferWithDelay(100).onEach {
                    MyLogger.v(tagStory, isFunctionCall = true)
                    when (it.first) {
                        Constants.StoryUploadState.StartUploading, Constants.StoryUploadState.Uploading, Constants.StoryUploadState.SavingStory -> {
                            myLoader?.setLoadingStatus(it.first.status, it.second ?: 0)
                        }

                        Constants.StoryUploadState.UploadingFail, Constants.StoryUploadState.UrlNotGet -> {
                            MyLogger.e(
                                tagStory,
                                msg = "Something went wrong :- ${it.first.name} occurred !"
                            )
                            hideLoader()
                            Helper.showSnackBar(
                                (requireActivity() as MainActivity).findViewById(
                                    R.id.coordLayout
                                ), "Story uploading failed !"
                            )
                        }

                        Constants.StoryUploadState.StoryUploadedSuccessfully -> {
                            MyLogger.v(
                                tagStory,
                                msg = "Loader is show with  StoryUploadedSuccessfully state ..."
                            )
                            hideLoader()
                            Helper.showSuccessSnackBar(
                                (requireActivity() as MainActivity).findViewById(
                                    R.id.coordLayout
                                ), "Story uploaded successfully !"
                            )
                        }
                    }
                }.launchIn(this)

                homeViewModel.uploadStories.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            // Do nothing here
                        }

                        is Resource.Loading -> {
                            MyLogger.v(
                                tagStory,
                                msg = "Story uploading now and now loader is showing !"
                            )
                            showLoader()
                        }

                        is Resource.Error -> {
                            MyLogger.e(
                                tagStory,
                                msg = "Some error occurred during post uploading :- ${response.message.toString()}"
                            )
                            hideLoader()
                            Helper.showSnackBar(
                                (requireActivity() as MainActivity).findViewById(
                                    R.id.coordLayout
                                ),
                                response.message.toString()
                            )
                            response.hasBeenMessagedToUser = true
                        }
                    }
                }.launchIn(this)

                AppBroadcastHelper.homeFragmentBackToTopShow.onEach {
                    if (it) {
                        showBackToTopView()
                    } else {
                        binding.linearBackToTop.gone()
                    }
                }.launchIn(this)

                AppBroadcastHelper.homeScrollBackToTopClick.onEach {
                    if (it) {
                        binding.nestedRVHome.smoothScrollTo(0, 0)
                    }
                }.launchIn(this)
            }
        }

    }


    //endregion

    //region:: Ui initialization
    private fun initUi() {
        MyLogger.v(isFunctionCall = true)
        _storyAdapter = StoryAdapter({
            MyLogger.v(tagStory, msg = "User want upload story !")
            StoryTypeOptionDialog(this@HomeFragment).show(childFragmentManager, "MyStoryDialogView")
        }, { stories ->
            MyLogger.v(tagStory, msg = "User want to show story !")
            navigateToStoryShow(stories)
        })

        binding.apply {
            rvStories.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL, false
                )
                myToolbar.icSetting.myShow()
                adapter = storyAdapter

                myToolbar.apply {
                    lifecycleScope.myLaunch {
                        pref.getPrefUser().first()?.let {
                            runOnUiThread {
                                tvHeaderUserName.text = it.userName

                                if (it.userProfileImage == null) {
                                    tvInitial.myShow()
                                    profileImage.gone()
                                    tvInitial.text = it.userName?.get(0).toString()
                                    tvInitial.setCircularBackground(Helper.setUserProfileColor(it))

                                } else {
                                    profileImage.myShow()
                                    tvInitial.gone()
                                    Glide.with(profileImage).load(it.userProfileImage)
                                        .placeholder(R.drawable.ic_user).error(R.drawable.ic_user)
                                        .into(profileImage)
                                }

                            }
                        }
                    }
                }

            }




            setUpViewPager()
            setListener()
        }
        setData()
    }

    private fun FragmentHomeBinding.setUpViewPager() {
        _pagerAdapter = NormalPagerAdapter(
            listOf(
                HomeFollowingPostFragment(),
                HomeDiscoverPostFragment()
            ), childFragmentManager, viewLifecycleOwner.lifecycle
        )
        viewPagerHome.apply {
            adapter = pagerAdapter
        }

        tabHome.apply {
            attachTo(viewPagerHome)
        }
    }

    private fun FragmentHomeBinding.setListener() {
        myToolbar.profileImage.setSafeOnClickListener {
            navigateToProfileViewFragment()
        }
        myToolbar.tvInitial.setSafeOnClickListener {
            navigateToProfileViewFragment()
        }
        myToolbar.icSetting.setOnClickListener {
            showPopupMenu()
        }
        linearBackToTop.setSafeOnClickListener {
            AppBroadcastHelper.setHomeScrollBackToTopClick(true)
        }

        nestedRVHome.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                //Scroll Down
                if (!isFragmentSwitchHappen) {
//                    MyLogger.v(tagPost, msg = "Down scroll")
                    AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                    AppBroadcastHelper.setMainActivityBottomNavHideByScroll(true)
                } else {
                    isFragmentSwitchHappen = false
                }

            }
            if (scrollY < oldScrollY) {
                //Scroll Up
//                MyLogger.v(tagPost, msg = "Up scroll")
                AppBroadcastHelper.setHomeFragmentBackToTopShow(true)
                AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)

            }

            if (scrollY == 0) {
                //Top Scroll
//                MyLogger.v(tagPost, msg = "Top Scroll")
                AppBroadcastHelper.setHomeFragmentBackToTopShow(false)
                AppBroadcastHelper.setMainActivityBottomNavHideByScroll(false)
            }

        }
    }

    //endregion

    //region:: UI utility function

    private fun getData() {
        homeViewModel.getAllStory(AuthManager.currentUserId()!!)
    }

    private fun setData(userStories: List<UserStories> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

//        lifecycleScope.launch {
//            val user = pref.getPrefUser().first()

            if (userData.isEmpty()) {
                userData.add(0, UserStories(null,null))
                userData.addAll(userStories)
            }

            MyLogger.v(msg = "Now data is set into homeFragment !")
            storyAdapter.submitList(userData.toList())
//        }

    }

    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpHomeSettingBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.myToolbar.icSetting)

        bindingPopUp.linearItemStatus.setOnClickListener {
            navigateToShowMyStatus()
            popUp.dismiss()
        }

        bindingPopUp.linearSearchUser.setOnClickListener {
            navigateToSearchUserScreen()
            popUp.dismiss()
        }


    }

    private fun navigateToSearchUserScreen() {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalSearchUserGlobalFragment()
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun navigateToShowMyStatus() {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalShowMyStoryFragment(AuthManager.currentUserId()!!)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun navigateToStoryShow(stories: UserStories) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalStoryShowFragment(stories)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun navigateToProfileViewFragment() {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalProfileViewFragment(AuthManager.currentUserId()!!)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@HomeFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }


    //region:: Upload story helper

    private fun handleUserSelectedMedia(
        type: Constants.StoryType,
        uri: Uri? = null,
        text: StoryText? = null
    ) {

        lifecycleScope.myLaunch {
            try {
                pref.getPrefUser().first()?.let {
                    MyLogger.v(tagStory, msg = "User data is retrieved !")
                    homeViewModel.uploadStory(type, uri, user = it, text = text)
                } ?: run {
                    MyLogger.v(tagStory, msg = "User not found !")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                MyLogger.e(
                    tagStory,
                    msg = "Some error occurred during uploading story  :- ${e.message}"
                )
                Helper.showSnackBar(
                    (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                    e.message.toString()
                )

            }

        }
    }


    private fun showLoader() {
        MyLogger.v(tagStory, isFunctionCall = true)
        if (myLoader == null) {
            myLoader = MyLoader()
        }
        myLoader?.show(childFragmentManager, "My Loader")
    }


    private fun hideLoader() {
        MyLogger.w(tagStory, isFunctionCall = true)
        lifecycleScope.myLaunch {
            delay(100)
            myLoader?.dismiss()
            myLoader = null
        }
    }


    //endregion

    //endregion

    //region:: Override function

    override fun onResume() {
        MyLogger.v(isFunctionCall = true)
        super.onResume()
    }

    override fun onStoryTypeSelect(selectedStoryType: Constants.StoryType) {
        when (selectedStoryType) {
            Constants.StoryType.Text -> {
                MyLogger.v(tagStory, msg = "User select text story !")
                val navDirections: NavDirections =
                    HomeFragmentDirections.actionHomeFragmentToWriteTextStoryFragment()
                navController?.safeNavigate(navDirections, Helper.giveAnimationNavOption())
            }

            Constants.StoryType.Image -> {
                MyLogger.v(tagStory, msg = "User select image story !")
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

            }

            Constants.StoryType.Video -> {
                MyLogger.v(tagStory, msg = "User select video story !")
                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))

            }
        }
    }


    //endregion

    //region:: Cleaning Part

    override fun onDestroyView() {
        MyLogger.v(isFunctionCall = true)
        _storyAdapter = null
        _pagerAdapter = null
        binding.rvStories.adapter = null
        binding.viewPagerHome.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MyLogger.v(isFunctionCall = true)
        requireContext().unregisterReceiver(broadcastManager)
        super.onDestroy()
    }
    //endregion

}