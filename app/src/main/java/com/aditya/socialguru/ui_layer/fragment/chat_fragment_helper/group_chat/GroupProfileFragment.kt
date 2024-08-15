package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.group_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupMembersList
import com.aditya.socialguru.databinding.FragmentGroupProfileBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class GroupProfileFragment : Fragment() {

    private var _binding: FragmentGroupProfileBinding? = null
    private val binding get() = _binding!!
    private val tagChat = Constants.LogTag.Chats

    private lateinit var chatRoomId: String
    private lateinit var groupMembers:GroupMembersList

    private var myLoader: MyLoader? = null
    private var defaultMuteValueForReceiver: Boolean? = null
    private var groupInfo: GroupInfo? = null

    private val args by navArgs<GroupProfileFragmentArgs>()
    private val chatViewModel by viewModels<ChatViewModel>()


    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatRoomId = args.chatRoomId
        groupMembers=args.groupMembers
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
        getData()
    }


    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.groupInfo.onEach { response ->

                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            setData(it)
                        }
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            chatViewModel.isMuted.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            defaultMuteValueForReceiver = it
                            binding.switchMute.isChecked = it
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        if (defaultMuteValueForReceiver == null) {
                            defaultMuteValueForReceiver = false
                        }
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)
            chatViewModel.muteOperation.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        loadDefaultSetting()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)
        }
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                icBack.myShow()
                tvHeaderUserName.myShow()
                this.profileImage.gone()
                tvHeaderUserName.text = "Group Profile"
            }
            setListener()
        }
    }

    private fun FragmentGroupProfileBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }

        ivEditGroupInfo.setSafeOnClickListener {
navigateToEditGroupScreen()
        }

        ivProfileImage.setSafeOnClickListener {
            navigateToImageViewScreen()
        }

        switchMute.setOnCheckedChangeListener { buttonView, isChecked ->
            chatViewModel.muteChatNotification(chatRoomId, isChecked)
        }

        cardMedia.setSafeOnClickListener {
            navigateToMediaScreen()
        }

        cardGroupMembers.setSafeOnClickListener {

        }

        btnLeaveGroup.setSafeOnClickListener {
            handleLeaveGroup()
        }
    }

    private fun handleLeaveGroup() {
        val isIAmCreatorOfThisGroup=groupInfo?.creatorId==AuthManager.currentUserId()
        if (isIAmCreatorOfThisGroup){

        }else{

        }
    }

    private fun navigateToEditGroupScreen() {
        if(groupInfo==null) return
        val directions: NavDirections =
            GroupProfileFragmentDirections.actionGroupProfileFragmentToEditGroupProfileFragment(
                chatRoomId ,groupInfo!!,groupMembers
            )
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.getGroupInfo(chatRoomId)
            chatViewModel.isUserMutedAndListen(chatRoomId)
            chatViewModel.setDataLoadedStatus(true)
        }
    }

    private fun setData(groupInfo: GroupInfo) {
        this.groupInfo = groupInfo
        binding.apply {
            if (groupInfo.groupPic != null) {
                Glide.with(ivProfileImage).load(groupInfo.groupPic).error(R.drawable.ic_user)
                    .placeholder(R.drawable.ic_user).into(ivProfileImage)
            }else{
                Glide.with(ivProfileImage).load(R.drawable.ic_user).into(ivProfileImage)
            }
            tvGroupDesc.isGone = groupInfo.groupDescription.isNullOrEmpty()
            tvGroupDesc.text = groupInfo.groupDescription
            tvGroupName.text = groupInfo.groupName
        }
    }

    private fun navigateToMediaScreen() {
        val directions: NavDirections =
            GroupProfileFragmentDirections.actionGroupProfileFragmentToChatMediaFragment(
                chatRoomId
            )
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun navigateToImageViewScreen() {
        if (groupInfo == null) {
            return
        }

        if (groupInfo!!.groupPic != null) {
            val directions: NavDirections =
                BottomNavigationBarDirections.actionGlobalShowImageFragment(
                    groupInfo!!.groupPic!!.convertParseUri()
                )
            navController.safeNavigate(directions, Helper.giveAnimationNavOption())
        } else {
            showSnackBar("Pic not found")
        }

    }

    private fun loadDefaultSetting() {
        defaultMuteValueForReceiver?.let {
            binding.switchMute.isChecked = it
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}