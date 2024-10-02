package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.group_chat

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.databinding.FragmentMakingGroupBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.ProfilePicEditDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.resizeActivate
import com.aditya.socialguru.domain_layer.helper.resizeStop
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.profile.ProfilePicEditOption
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.GroupMemberChatAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import com.vanniktech.emoji.EmojiPopup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class MakingGroupFragment : Fragment(), ProfilePicEditOption {

    private var _binding: FragmentMakingGroupBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private val emojiKeyboardTag = 0
    private val emojiPopup by lazy {
        EmojiPopup(
            binding.root,
            binding.etGroupName
        )
    }
    private var groupProfileImage: String? = null
    private val chatRoomId by lazy {
        Helper.getGroupChatId()
    }

    private var _memberAdapter: GroupMemberChatAdapter? = null
    private var userList = mutableListOf<User>()
    private val memberAdapter get() = _memberAdapter!!
    private var myLoader: MyLoader? = null

    private val pref: SharePref by lazy {
        SharePref(requireContext())
    }
    private val args by navArgs<MakingGroupFragmentArgs>()


    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val chatViewModel by viewModels<ChatViewModel>()

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                MyLogger.v(tagChat, msg = "User select pic now set to profile :- $uri")

                setImageOnProfileView(uri)

            } else {
                MyLogger.v(tagChat, msg = "User revoke or cancel upload story !")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMakingGroupBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        userList = args.users.users.toMutableList()
        _memberAdapter = GroupMemberChatAdapter()
        initUi()
        subscribeToObserver()
        setData()
        requireActivity().resizeActivate()
    }


    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.sendGroupMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            if (it.isSuccess) {
                                hideDialog()
                                showSnackBar("Group Created Successfully !", true)
                                navigateToGroupChat()
                            }
                        }

                    }

                    is Resource.Loading -> {
                        MyLogger.d(tagChat, msg = "Group is making ...")
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)
        }
    }


    private fun initUi() {
        binding.apply {
            lifecycleOwner = this@MakingGroupFragment
            chatViewModel = this@MakingGroupFragment.chatViewModel

            rvMember.apply {
                layoutManager =
                    GridLayoutManager(requireContext(), 4, GridLayoutManager.VERTICAL, false)
                adapter = memberAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }

            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text = "New Group"
            }
            setListener()
        }
    }

    private fun FragmentMakingGroupBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        etGroupName.setOnTouchListener { v, event ->
            MyLogger.v(tagChat, msg = "Now user touch on edit group name view !")
            v.onTouchEvent(event)
            emojiPopup.dismiss()
            ivEmoji.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.emoji
                )
            )
            true
        }
        ivEmoji.setSafeOnClickListener {
            //https://johncodeos.com/move-view-with-keyboard-in-android-using-kotlin/
            // Use this article to solve keyboard issue

            if (it.tag.toString().toInt() == emojiKeyboardTag) {
                emojiPopup.toggle()
                ivEmoji.tag = "1"  // Important tag set into string else get unexpected result
                ivEmoji.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_keybaord
                    )
                )
            } else {
                ivEmoji.tag = "0"
                emojiPopup.dismiss()
                ivEmoji.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.emoji
                    )
                )
            }
        }

        ivAddImage.setSafeOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        ivGroupImage.setSafeOnClickListener {
            ProfilePicEditDialog(this@MakingGroupFragment).show(
                childFragmentManager,
                "My_Profile_Pic_Edit_Dialog"
            )
        }

        btnCreateGroup.setSafeOnClickListener {
            createGroup()
        }
    }


    private fun setData() {
        binding.apply {
            tvMembers.text = "Members : ${userList.size}"
            memberAdapter.submitList(userList)
        }
    }

    private fun setImageOnProfileView(uri: Uri) {
        binding.ivGroupImage.myShow()
        binding.ivAddImage.gone()
        groupProfileImage = uri.toString()
        Glide.with(binding.ivGroupImage).load(groupProfileImage).placeholder(R.drawable.ic_user)
            .into(binding.ivGroupImage)
    }

    private fun removeImageFromProfileView() {
        binding.apply {
            groupProfileImage = null  //Assure newImage should null and currentImage not change
            binding.ivGroupImage.gone()
            binding.ivAddImage.myShow()
        }
    }

    private fun createGroup() {
        if (binding.etGroupName.text.isNullOrBlank()) {
            showSnackBar("Please Enter Group Name !")
        } else {
            lifecycleScope.launch {
                val timestamp = System.currentTimeMillis()
                val timeStampInText = Helper.formatTimestampToDateAndTime(timestamp)
                val message = GroupMessage(
                    messageId = Helper.getMessageId(),
                    messageType = Constants.MessageType.Info.type,
                    senderId = AuthManager.currentUserId()!!,
                    infoMessageType = Constants.InfoType.GroupCreated.name,
                    senderUserName = pref.getPrefUser().first()?.userName
                )
                val lastMessage = GroupLastMessage(
                    senderId = AuthManager.currentUserId()!!,
                    messageType = Constants.MessageType.Info.type,
                    infoMessageType = Constants.InfoType.GroupCreated.name,
                )
                val groupInfo = GroupInfo(
                    chatRoomId = chatRoomId,
                    groupPic = groupProfileImage,
                    groupName = binding.etGroupName.text.toString(),
                    groupAdmins = listOf(),
                    creatorId = AuthManager.currentUserId()
                )
                chatViewModel.sendGroupMessage(
                    message = message,
                    lastMessage = lastMessage,
                    chatRoomId = chatRoomId,
                    userList.map { GroupMember(it.userId, false) }.toMutableList().apply {
                        add(
                            GroupMember(
                                AuthManager.currentUserId()!!,
                                true,
                                groupJoiningDateInTimeStamp = timestamp,
                                groupJoiningDateInText = timeStampInText
                            )
                        )
                    }.toList(),
                    Constants.InfoType.GroupCreated, groupInfo = groupInfo
                ) {
                    // Do Nothing
                }
            }
        }
    }

    private fun navigateToGroupChat() {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalGroupChatFragment(chatRoomId = chatRoomId)
        navController.safeNavigate(
            directions,
            Helper.giveAnimationNavOption(R.id.startGroupChatFragment, true)
        )
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

    override fun onDelete() {
        removeImageFromProfileView()
    }

    override fun onUpload() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onDestroyView() {
        requireActivity().resizeStop()
        _binding = null
        super.onDestroyView()
    }


}