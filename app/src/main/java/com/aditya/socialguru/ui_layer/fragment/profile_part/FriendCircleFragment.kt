package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentFriendCircleBinding
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle.FollowerFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle.FollowingFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.friend_circle.FriendFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyCommentPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyLikedPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyPostFragment


class FriendCircleFragment : Fragment() {

    private var _binding:FragmentFriendCircleBinding?=null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as MainActivity).navController?.value

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

    override fun onDestroyView() {
        super.onDestroyView()
    }

}


