package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.navigateUp
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeDiscoverPostFragment
import com.aditya.socialguru.ui_layer.fragment.home_tab_layout.HomeFollowingPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyCommentPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyLikedPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyPostFragment


class MyActivityFragment : Fragment() {

    private var _binding:FragmentMyActivityBinding?=null
    private val binding get() = _binding!!

    private val navController get() = (requireActivity() as MainActivity).navController

    // Don't use lazy it lead to memory leak and not leave old view when fragment switching and come back this view that time thi variable if initialize with lazy that not leave old view and crash app
    private var _pagerAdapter:NormalPagerAdapter?=null
    private val pagerAdapter get()=_pagerAdapter!!

    private val args by navArgs<MyActivityFragmentArgs>()
    private lateinit var userId:String
    private lateinit var userDetails:User


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       _binding=FragmentMyActivityBinding.inflate(layoutInflater)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        userId=args.userId
        userDetails = args.userDetail
        initUi()
    }

    private fun initUi() {
        binding.apply {

            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text= if (userId==AuthManager.currentUserId()!!)"My Activity" else userDetails.userName
            }

            setUpViewPager()
            setListener()
        }
    }

    private fun FragmentMyActivityBinding.setUpViewPager() {
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

    private fun FragmentMyActivityBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController?.navigateUp()
        }


    }

    override fun onDestroyView() {
        _pagerAdapter=null
        binding.viewPagerHome.adapter = null
        _binding = null
        super.onDestroyView()
    }

}


