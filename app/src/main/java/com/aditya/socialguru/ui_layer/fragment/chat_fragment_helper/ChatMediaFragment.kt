package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentChatBinding
import com.aditya.socialguru.databinding.FragmentChatMediaBinding
import com.aditya.socialguru.databinding.FragmentMyActivityBinding
import com.aditya.socialguru.databinding.FragmentUserChatProfileBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.ui_layer.adapter.NormalPagerAdapter
import com.aditya.socialguru.ui_layer.adapter.chat.ChatMessageAdapter
import com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.chat_media_type.AllMediaShowFragment
import com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.chat_media_type.ImageMediaShowFragment
import com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.chat_media_type.VideoMediaShowFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyCommentPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyLikedPostFragment
import com.aditya.socialguru.ui_layer.fragment.profile_part.my_activity.ShowMyPostFragment


class ChatMediaFragment : Fragment() {

    private var _binding: FragmentChatMediaBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private lateinit var chatRoomId: String

    // Don't use lazy it lead to memory leak and not leave old view when fragment switching and come back this view that time thi variable if initialize with lazy that not leave old view and crash app
    private var _pagerAdapter: NormalPagerAdapter?=null
    private val pagerAdapter get()=_pagerAdapter!!


    private val args by navArgs<ChatMediaFragmentArgs>()

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatMediaBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        chatRoomId=args.chatRoomId
        initUi()
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                profileImage.gone()
                icBack.myShow()
                tvHeaderUserName.text = "Media"
            }
            setUpViewPager()
            setListener()
        }
    }

    private fun FragmentChatMediaBinding.setUpViewPager() {
        _pagerAdapter = NormalPagerAdapter(
            listOf(
                AllMediaShowFragment(chatRoomId),
                ImageMediaShowFragment(chatRoomId),
                VideoMediaShowFragment(chatRoomId)
            ), childFragmentManager, viewLifecycleOwner.lifecycle
        )
        viewPagerHome.apply {
            adapter = pagerAdapter
        }

        tabHome.apply {
            attachTo(viewPagerHome)
        }
    }

    private fun FragmentChatMediaBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
    }

    override fun onDestroyView() {
        _pagerAdapter=null
        binding.viewPagerHome.adapter=null
        _binding=null
        super.onDestroyView()
    }



}