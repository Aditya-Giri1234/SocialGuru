package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.navigation.NavDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentFriendCircleBinding
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.databinding.PopUpChatScreenBinding
import com.aditya.socialguru.databinding.PopUpFriendCircleBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle.FollowerFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle.FollowingFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle.FriendFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyCommentPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyLikedPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyPostFragment
import com.google.rpc.Help


class FriendCircleFragment : Fragment() {

    private var _binding:FragmentFriendCircleBinding?=null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as MainActivity).navController

    // Don't use lazy it lead to memory leak and not leave old view when fragment switching and come back this view that time thi variable if initialize with lazy that not leave old view and crash app
    private var _pagerAdapter: NormalPagerAdapter?=null
    private val pagerAdapter get()=_pagerAdapter!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentFriendCircleBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
    }

    private fun initUi() {
        binding.apply {

            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                icSetting.myShow()
                tvHeaderUserName.text="Friend Circle"
            }

            setUpViewPager()
            setListener()
        }
    }

    private fun FragmentFriendCircleBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController?.navigateUp()
        }
        myToolbar.icSetting.setSafeOnClickListener {
            showPopUp()
        }
    }



    private fun FragmentFriendCircleBinding.setUpViewPager() {
        _pagerAdapter = NormalPagerAdapter(
            listOf(
                FollowerFragment(),
                FollowingFragment(),
                FriendFragment()
            ), childFragmentManager, viewLifecycleOwner.lifecycle
        )
        viewPagerHome.apply {
            adapter = pagerAdapter
        }

        tabHome.apply {
            attachTo(viewPagerHome)
        }
    }

    private fun showPopUp() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpFriendCircleBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.myToolbar.icSetting)

        bindingPopUp.linearPendingFriendRequest.setSafeOnClickListener {
            navigateToPendingRequestScreen()
            popUp.dismiss()
        }
    }

    private fun navigateToPendingRequestScreen() {
        val direction: NavDirections = FriendCircleFragmentDirections.actionFriendCircleFragmentToPendingFriendRequestFragment()
        navController.safeNavigate(direction,Helper.giveAnimationNavOption())
    }

    override fun onDestroyView() {
        binding.viewPagerHome.adapter=null
        _pagerAdapter=null
        _binding=null
        super.onDestroyView()
    }

}


