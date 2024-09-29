package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.navGraphViewModels
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.databinding.FragmentProfileBinding
import com.aditya.socialguru.databinding.PopUpProfileSettingBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.monitorInternet
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.viewmodel.profile.ProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment(), AlertDialogOption {


    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val imageAvailable = "0"
    private val imageUnAvailable = "1"
    private var myLoader: MyLoader? = null
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val profileViewModel: ProfileViewModel by activityViewModels()

    private val pref by lazy {
        SharePref(requireContext())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                if (it==Int.MIN_VALUE){
                    profileViewModel.setDataLoadedStatus(false)
                    jobQueue.add {
                        profileViewModel.subscribeToFollowerCount(AuthManager.currentUserId()!!)
                    }
                }else{
                    binding.tvFollowers.text = it.toString()
                }
            }.launchIn(this)
            profileViewModel.followingCount.onEach {
                if (it==Int.MIN_VALUE){
                    profileViewModel.setDataLoadedStatus(false)
                    jobQueue.add {
                        profileViewModel.subscribeToFollowingCount(AuthManager.currentUserId()!!)
                    }
                }else{
                    binding.tvFollowing.text = it.toString()
                }
            }.launchIn(this)
            profileViewModel.postCount.onEach {
                if (it==Int.MIN_VALUE){
                    profileViewModel.setDataLoadedStatus(false)
                    jobQueue.add {
                        profileViewModel.subscribeToPostCount(AuthManager.currentUserId()!!)
                    }
                }else{
                    binding.tvPost.text = it.toString()
                }
            }.launchIn(this)
            profileViewModel.likeCount.onEach {
                if (it==Int.MIN_VALUE){
                    profileViewModel.setDataLoadedStatus(false)
                    jobQueue.add {
                        profileViewModel.subscribeToLikeCount(AuthManager.currentUserId()!!)
                    }
                }else{
                    binding.tvLike.text = it.toString()
                }
            }.launchIn(this)

            requireContext().monitorInternet().onEach { isInternetAvailable ->
                if(isInternetAvailable){
                    jobQueue.forEach {
                        it.invoke()
                    }
                    profileViewModel.setDataLoadedStatus(true)
                    jobQueue.clear()
                }
            }.launchIn(this)

            profileViewModel.userSignOut.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        Helper.showSuccessSnackBar(
                            (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                                R.id.coordLayout
                            ), "LogOut Successfully!"
                        )
                        Helper.setLogout(requireContext())
                        navigateToOnboardingScreen()
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        Helper.showSnackBar(
                            (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                                R.id.coordLayout
                            ), response.message.toString()
                        )
                    }
                }
            }.launchIn(this)
        }
    }


    private fun initUI() {
        binding.apply {
            lifecycleScope.myLaunch {
                pref.getPrefUser().first()?.let { user ->
                    withContext(Dispatchers.Main) {
                        if (user.userProfileImage != null) {
                            tvInitialMain.gone()
                            ivProfile.myShow()
                            ivProfile.tag =
                                imageAvailable  // help to determine that image available not
                            Glide.with(ivProfile).load(user.userProfileImage)
                                .placeholder(R.drawable.ic_user).into(ivProfile)
                        } else {
                            tvInitialMain.myShow()
                            ivProfile.gone()
                            ivProfile.tag = imageUnAvailable
                            Glide.with(ivProfile).load(R.drawable.ic_user).into(ivProfile)
                            ivProfile.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

                            tvInitialMain.text = user.userName?.get(0).toString()
                            tvInitialMain.setCircularBackground(Helper.setUserProfileColor(user))
                        }

                        tvUserName.text = user.userName
                        tvProffesion.text = user.userProfession
                        tvBio.text = user.userBio
                    }
                }
            }

            setListener()
        }
    }

    private fun FragmentProfileBinding.setListener() {

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

        ivProfileOption.setOnClickListener {
            showPopupMenu()
        }
    }

    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpProfileSettingBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.ivProfileOption)


        bindingPopUp.apply {

            linearItemFriendCircle.setOnClickListener {
                popUp.dismiss()
                val directions: NavDirections =
                    ProfileFragmentDirections.actionProfileFragmentToFriendCircleFragment()
                navController.safeNavigate(directions, Helper.giveAnimationNavOption())
            }
            linearItemActivity.setSafeOnClickListener {
                popUp.dismiss()
                lifecycleScope.launch {
                    pref.getPrefUser().first()?.let {
                        val directions: NavDirections =
                            BottomNavigationBarDirections.actionGlobalMyActivityFragment(
                                AuthManager.currentUserId()!!,
                                it
                            )
                        navController.safeNavigate(
                            directions, Helper.giveAnimationNavOption()
                        )
                    }
                }
            }
            linearItemSavedPost.setSafeOnClickListener {
                popUp.dismiss()
                val directions: NavDirections =
                    BottomNavigationBarDirections.actionGlobalMySavedPostFragment()
                navController.safeNavigate(
                    directions, Helper.giveAnimationNavOption()
                )
            }
            linearItemEditProfile.setOnClickListener {
                popUp.dismiss()

                val directions: NavDirections =
                    ProfileFragmentDirections.actionProfileFragmentToUpdateProfileFragment2()
                navController.navigate(directions, Helper.giveAnimationNavOption())
            }
            linearItemLogOut.setOnClickListener {
                popUp.dismiss()
                AlertDialog(
                    "Are you sure log out ?",
                    this@ProfileFragment,
                    isForShowDelete = false
                ).show(
                    childFragmentManager,
                    "MyAlertDialog"
                )
            }
            linearItemSetting.setOnClickListener {
                popUp.dismiss()

                val directions: NavDirections =
                    ProfileFragmentDirections.actionProfileFragmentToSettingFragment()
                navController.navigate(directions, Helper.giveAnimationNavOption())
            }

        }


    }

    private fun getData() {
        lifecycleScope.myLaunch {
            pref.getPrefUser().first()?.let {
                it.userId?.apply {
                    profileViewModel.subscribeToFollowerCount(this)
                    profileViewModel.subscribeToFollowingCount(this)
                    profileViewModel.subscribeToPostCount(this)
                    profileViewModel.subscribeToLikeCount(this)
                }
            }
        }
    }

    private fun navigateToOnboardingScreen() {
        /*  val directions: NavDirections =
              BottomNavigationBarDirections.actionGlobalOnboardingScreenFragment()
          navController.safeNavigate(
              directions,
              Helper.giveAnimationNavOption(R.id.homeFragment, true)
          )*/

        requireActivity().sendBroadcast(Intent(Constants.AppBroadCast.LogOut.name).apply {
            setPackage(requireContext().packageName)
        })
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

    override fun onResult(isYes: Boolean) {
        if (isYes) {
            profileViewModel.singOutUser()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}