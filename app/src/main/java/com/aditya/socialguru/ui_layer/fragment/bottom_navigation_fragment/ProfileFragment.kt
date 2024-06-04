package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.content.Context
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentProfileBinding
import com.aditya.socialguru.databinding.PopUpProfileSettingBinding
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.profile.ProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {


    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val imageAvailable="0"
    private val imageUnAvailable="1"


    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val profileViewModel by viewModels<ProfileViewModel>()

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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.followerCount.onEach {
                    binding.tvFollowers.text = it.toString()
                }.launchIn(this)
                profileViewModel.followingCount.onEach {
                    binding.tvFollowing.text = it.toString()
                }.launchIn(this)
                profileViewModel.postCount.onEach {
                    binding.tvPost.text = it.toString()
                }.launchIn(this)
                profileViewModel.likeCount.onEach {
                    binding.tvLike.text = it.toString()
                }.launchIn(this)
            }
        }
    }

    private fun initUI() {
        binding.apply {
            lifecycleScope.launch {
                pref.getPrefUser().first()?.let { user ->

                    if (user.userProfileImage != null) {
                        ivProfile.tag=imageAvailable  // help to determine that image available not
                        Glide.with(ivProfile).load(user.userProfileImage).placeholder(R.drawable.ic_user).into(ivProfile)
                    } else {
                        ivProfile.tag=imageUnAvailable
                        Glide.with(ivProfile).load(R.drawable.ic_user).into(ivProfile)
                        ivProfile.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    }

                    tvUserName.text = user.userName
                    tvProffesion.text = user.userProfession
                    tvBio.text = user.userBio

                }
            }

            setListener()
        }
    }

    private fun FragmentProfileBinding.setListener() {

        ivProfile.setSafeOnClickListener {
            if (ivProfile.tag==imageAvailable){
                Helper.showImageDialog(requireActivity(),ivProfile.getBitmapByDrawable())
            }else{
                Helper.showSnackBar((requireActivity() as MainActivity).findViewById<CoordinatorLayout>(R.id.coordLayout) ,"No Image Found !")

            }

        }

        ivProfileOption.setSafeOnClickListener {
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

            linearItemFriendCircle.setSafeOnClickListener {
                popUp.dismiss()
            }
            linearItemActivity.setSafeOnClickListener {
                popUp.dismiss()

            }
            linearItemEditProfile.setSafeOnClickListener {
                popUp.dismiss()

                val directions: NavDirections =
                    ProfileFragmentDirections.actionProfileFragmentToUpdateProfileFragment2()
                navController?.value?.navigate(directions, Helper.giveAnimationNavOption())
            }
            linearItemLogOut.setSafeOnClickListener {
                popUp.dismiss()

            }
            linearItemSetting.setSafeOnClickListener {
                popUp.dismiss()


            }

        }


    }

    private fun getData() {
        lifecycleScope.launch {
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}