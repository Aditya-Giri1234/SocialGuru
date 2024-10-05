package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.user_action.UserRelationshipStatus
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.databinding.FragmentProfileViewBinding
import com.aditya.socialguru.databinding.SampleProfileViewPopMenuBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.giveMeColor
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.NetworkManager.monitorInternet
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyCommentPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyLikedPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyPostFragment
import com.aditya.socialguru.ui_layer.viewmodel.profile.ProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class ProfileViewFragment : Fragment(), AlertDialogOption {


    private var _binding: FragmentProfileViewBinding? = null
    private val binding get() = _binding!!

    private val imageAvailable = "0"
    private val imageUnAvailable = "1"
    private val NO_DATA_VIEW_MESSAGE = "Oops! We couldn't find any user matching your search."
    private val DATA_LOADING_VIEW_MESSAGE = "Fetching user data... Hang tight!"
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private var myLoader: MyLoader? = null
    private var userDetails: User? = null
    private var _pagerAdapter: NormalPagerAdapter? = null
    private val pagerAdapter get() = _pagerAdapter!!
    private var defaultDialogOption: ProfileViewDialogOption =
        ProfileViewDialogOption.PendingRequest

    // Need to be change
    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val args by navArgs<ProfileViewFragmentArgs>()


    private lateinit var userId: String

    private val profileViewModel by viewModels<ProfileViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileViewBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = args.userId
        handleInitialization()
    }

    private fun handleInitialization() {
        initUI()
        subscribeToObserver()
        if (!profileViewModel.isDataLoaded) {
            profileViewModel.setDataLoadedStatus(true)
            getData()
        }
    }

    private fun subscribeToObserver() {
        observeFlow {
            profileViewModel.followerCount.onEach {
                binding.tvFollowers.text = it.toString()
            }.launchIn(this)
            profileViewModel.followingCount.onEach {
                binding.tvFollowing.text = it.toString()
            }.launchIn(this)
            profileViewModel.postCount.onEach {
                binding.tvPost.text = it.toString()
            }.launchIn(this)
            profileViewModel.followUser.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar("Successfully Following !", true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)
            profileViewModel.unFollow.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar("UnFollow Successfully!", true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            profileViewModel.sendFriendRequest.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar("Successfully  Friend Request Sent !", true)

                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            profileViewModel.acceptFriendRequest.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar("Friend Request Accepted !", true)

                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            profileViewModel.deleteFriendRequest.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(" Friend Request deleted  successfully !", true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            profileViewModel.userRelationshipStatus.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            if (!response.hasBeenMessagedToUser) {
                                response.hasBeenMessagedToUser = true
                                setUserRelational(it)
                            }
                        }
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.add {
                                        profileViewModel.getUserRelationshipStatus(userId)
                                    }
                                }

                                else -> {
                                    showSnackBar(message = response.message)
                                }
                            }
                        }
                    }
                }

            }.launchIn(this)
            profileViewModel.userRelationshipStatusUpdate.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            profileViewModel.getUserRelationshipStatus(userId!!)
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                    }
                }

            }.launchIn(this)

            profileViewModel.removeFriend.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar("Removed Successfully !", true)
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            profileViewModel.userDetails.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideNoDataView()
                        hideDialog()
                        response.data?.let {
                            setData(it)
                            userId?.let {
                                lifecycleScope.launch {
                                    profileViewModel.getUserRelationshipStatus(it)
                                    it.apply {
                                        profileViewModel.subscribeToFollowerCount(this)
                                        profileViewModel.subscribeToFollowingCount(this)
                                        profileViewModel.subscribeToPostCount(this)
                                        profileViewModel.subscribeToLikeCount(this)
                                    }
                                }
                            }
                        }

                    }

                    is Resource.Loading -> {
                        showNoDataView(DATA_LOADING_VIEW_MESSAGE)
                        showDialog()
                    }

                    is Resource.Error -> {
                        showNoDataView()
                        hideDialog()
                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.add {
                                        profileViewModel.getUser(userId)
                                    }
                                }

                                else -> {
                                    showSnackBar(message = response.message)
                                }
                            }
                        }
                    }
                }

            }.launchIn(this)
            requireContext().monitorInternet().onEach { isInternetAvailable ->
                if (isInternetAvailable) {
                    jobQueue.forEach {
                        it.invoke()
                    }
                    jobQueue.clear()
                }
            }.launchIn(this)
        }
    }


    private fun initUI() {
        binding.apply {
            myToolbar.apply {
                root.setBackgroundColor(requireContext().giveMeColor(R.color.lightBlack))
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text = "Profile"
                if (binding.btnFriend.text != getString(R.string.already_friend) && userId != AuthManager.currentUserId()!!) {
                    icSetting.gone()
                } else {
                    icSetting.myShow()
                }
            }

            setListener()
            setUpViewPager()
        }
    }

    private fun FragmentProfileViewBinding.setUpViewPager() {
        _pagerAdapter = NormalPagerAdapter(
            listOf(
                ShowMyPostFragment(userId),
                ShowMyCommentPostFragment(userId),
                ShowMyLikedPostFragment(userId)
            ), childFragmentManager, viewLifecycleOwner.lifecycle
        )
        viewPagerHome.apply {
            adapter = pagerAdapter
        }

        tabHome.apply {
            attachTo(viewPagerHome)
        }
    }

    private fun FragmentProfileViewBinding.setListener() {

        myToolbar.icSetting.setSafeOnClickListener {
            showPopupMenu()
        }

        nestedProfileView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                //Scroll Down
                linearBackToTop.gone()
            }
            if (scrollY < oldScrollY) {
                //Scroll Up
                showBackToTopView()
            }

            if (scrollY == 0) {
                //Top Scroll
                linearBackToTop.gone()
            }
        }


        ivProfile.setSafeOnClickListener {
            if (ivProfile.tag == imageAvailable) {
                Helper.showImageDialog(requireActivity(), ivProfile.getBitmapByDrawable())
            } else {
                Helper.showSnackBar(
                    (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                        R.id.coordLayout
                    ), "No Image Found !"
                )

            }

        }

        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }

        btnFollow.setSafeOnClickListener {

            if (btnFollow.text.toString().equals(getString(R.string.following))) {
                profileViewModel.unFollow(userId!!)
            } else {
                profileViewModel.followUser(userId!!)

            }

        }
        btnFriend.setSafeOnClickListener {
            when (btnFriend.text.toString()) {
                getString(R.string.connect) -> {
                    profileViewModel.sendFriendRequest(userId!!)
                }

                getString(R.string.accept) -> {
                    profileViewModel.acceptFriendRequest(userId!!)
                }

                getString(R.string.pending) -> {
                    defaultDialogOption = ProfileViewDialogOption.PendingRequest
                    AlertDialog(
                        "Are you sure delete friend request ?",
                        this@ProfileViewFragment,
                        false
                    ).show(childFragmentManager, "My_Dialog")
                }

                getString(R.string.connected) -> {
                    defaultDialogOption = ProfileViewDialogOption.AlreadyFriend
                    AlertDialog(
                        "Are you sure you want to remove this friend?",
                        this@ProfileViewFragment,
                        false
                    ).show(childFragmentManager, "My_Dialog")
                }

            }
        }


    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@ProfileViewFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun getData() {
        userId.let {
            profileViewModel.getUser(it)
            profileViewModel.listenUserRelationStatus(it)
        }
    }

    private fun setData(user: User) {
        userDetails = user

        binding.apply {
            myToolbar.icSetting.myShow()

            linearProfile.myShow()
            linearUserAction.myShow()
            linearFollower.myShow()
            noDataView.gone()

            userId.let {
                if (it == AuthManager.currentUserId()!!) {
                    linearUserAction.gone()
                }
            }

            if (user.userProfileImage != null) {
                tvInitialMain.gone()
                ivProfile.myShow()
                ivProfile.tag =
                    imageAvailable  // help to determine that image available or  not
                Glide.with(ivProfile).load(user.userProfileImage)
                    .placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(ivProfile)
            } else {
                tvInitialMain.myShow()
                ivProfile.gone()
                ivProfile.tag = imageUnAvailable
                Glide.with(ivProfile).load(R.drawable.ic_user).into(ivProfile)
                //NOt need below code :- Below code for tint color set on icon
//                ivProfile.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                tvInitialMain.text = user.userName?.get(0).toString()
                tvInitialMain.setCircularBackground(Helper.setUserProfileColor(user))
            }

            tvUserName.text = user.userName
            tvProffesion.text = user.userProfession
            tvBio.text = user.userBio

        }
    }

    private fun setUserRelational(relation: UserRelationshipStatus) {
        binding.apply {
            if (relation.isFollowing) {
                btnFollow.setFollowingBackground()
            } else {
                btnFollow.setFollowBackground()
            }

            when (relation.friendStatus) {
                Constants.FriendStatus.FRIEND -> {
                    btnFriend.setConnectedBackground()
                }

                Constants.FriendStatus.FRIEND_REQUEST -> {
                    btnFriend.setAcceptBackground()
                }

                Constants.FriendStatus.PENDING_REQUEST -> {
                    btnFriend.setPendingBackground()
                }

                Constants.FriendStatus.NOT_FRIEND -> {
                    btnFriend.setConnectBackground()
                }


            }
        }

    }


    private fun TextView.setFollowBackground() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.follow_bg)
        setTextColor(requireContext().giveMeColor(R.color.yellow))
        text = getString(R.string.follow)
    }

    private fun TextView.setFollowingBackground() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.following_bg)
        setTextColor(requireContext().giveMeColor(R.color.white))
        text = getString(R.string.following)
    }

    private fun TextView.setConnectBackground() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.connect_bg)
        setTextColor(requireContext().giveMeColor(R.color.white))
        text = getString(R.string.connect)
    }

    private fun TextView.setPendingBackground() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.pending_bg)
        setTextColor(requireContext().giveMeColor(R.color.lightWhite))
        text = getString(R.string.pending)
    }

    private fun TextView.setAcceptBackground() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.accept_bg)
        setTextColor(requireContext().giveMeColor(R.color.white))
        text = getString(R.string.accept)
    }

    private fun TextView.setConnectedBackground() {
        background = ContextCompat.getDrawable(requireContext(), R.drawable.connected_bg)
        setTextColor(requireContext().giveMeColor(R.color.white))
        text = getString(R.string.connected)
    }

    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = SampleProfileViewPopMenuBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.myToolbar.icSetting)

        bindingPopUp.linearItemStories.setSafeOnClickListener {
            navigateToStoriesScreen()
            popUp.dismiss()
        }
    }

    private fun navigateToStoriesScreen() {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalShowMyStoryFragment(userId)
        navController.safeNavigate(
            directions, Helper.giveAnimationNavOption()
        )
    }

    private fun navigateToPostScreen() {
        userDetails?.let {
            val directions: NavDirections =
                BottomNavigationBarDirections.actionGlobalMyActivityFragment(userId, it)
            navController.safeNavigate(
                directions, Helper.giveAnimationNavOption()
            )
        }
    }

    private fun showSnackBar(message: String?, isSuccess: Boolean = false) {
        if (isSuccess) {
            Helper.showSuccessSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                    R.id.coordLayout
                ), message.toString()
            )
        } else {
            Helper.showSnackBar(
                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                    R.id.coordLayout
                ), message.toString()
            )
        }
    }

    private fun showDialog() {
        myLoader?.dismiss()
        myLoader = MyLoader()
        myLoader?.show(childFragmentManager, "My_Loader")
    }

    private fun hideDialog() {
        myLoader?.dismiss()
        myLoader = null
    }

    private fun showNoDataView(message: String? = NO_DATA_VIEW_MESSAGE) {
        binding.apply {
           cardMain.gone()
            tabHome.gone()
            viewPagerHome.gone()
            noDataView.myShow()
            noDataView.text = message
        }
    }

    private fun hideNoDataView() {
        binding.apply {
           cardMain.myShow()
            tabHome.myShow()
            viewPagerHome.myShow()
            noDataView.gone()
            noDataView.text = DATA_LOADING_VIEW_MESSAGE
        }
    }

    override fun onDestroyView() {
        binding.linearBackToTop.gone()
        _binding = null
        super.onDestroyView()
    }

    override fun onResult(isYes: Boolean) {
        if (isYes) {
            when (defaultDialogOption) {
                ProfileViewDialogOption.PendingRequest -> {
                    profileViewModel.deleteFriendRequest(userId!!)
                }

                ProfileViewDialogOption.AlreadyFriend -> {
                    profileViewModel.removeFriend(userId!!)
                }
            }
        }
    }
}

private enum class ProfileViewDialogOption {
    PendingRequest,
    AlreadyFriend
}