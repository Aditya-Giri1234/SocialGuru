package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.animation.Animator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.LastMessage
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.databinding.FragmentChatBinding
import com.aditya.socialguru.databinding.PopUpChatScreenBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.MessageType
import com.aditya.socialguru.domain_layer.helper.Constants.SeenStatus
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.ChatMessageAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class ChatFragment : Fragment(), AlertDialogOption, ChatMessageOption {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null

    private val tagChat = Constants.LogTag.Chats

    private lateinit var receiverId: String  //This is for receive userId
    private var isUserAppOpen = false
    private var isUserActiveOnCurrentChat = false
    private var isFirstTimeDataSetOnUi = true

    private var imageUri: String? = null
    private var videoUri: String? = null

    private var _chatAdapter: ChatMessageAdapter? = null
    private val chatAdapter get() = _chatAdapter!!

    private var myDetails: User? = null

    private val isIAmUser1 by lazy {
        val list = listOf(AuthManager.currentUserId()!!, receiverId).sorted()
        list[0] == AuthManager.currentUserId()!!
    }

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }
    private val chatViewModel by viewModels<ChatViewModel>()

    private val senderId by lazy {
        AuthManager.currentUserId()!!
    }

    private val args by navArgs<ChatFragmentArgs>()

    private val chatRoomId by lazy {
        Helper.getChatRoomId(receiverId)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        receiverId = args.userId
        _chatAdapter = ChatMessageAdapter(this@ChatFragment)
        initUi()
        subscribeToObserver()
        getData()
    }


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun subscribeToObserver() {
        viewLifecycleOwner.observeFlow {

            chatViewModel.userDetails.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            setUserProfile(it)
                        }
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)
            chatViewModel.lastMessage.onEach {
                updateUserAvailability(it)
            }.launchIn(this)
            chatViewModel.chatMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            setChatMessageList(it)
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

            chatViewModel.sendMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        binding.etMessage.text.clear()
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

            binding.etMessage.getQueryTextChangeStateFlow().debounce(100).distinctUntilChanged()
                .flatMapLatest {
                    if (it.startsWith(" ")) {
                        runOnUiThread {
                            binding.etMessage.setText(binding.etMessage.text.trim())
                            return@runOnUiThread
                        }
                    }
                    flow {
                        emit(it)
                    }
                }.onEach {
                    runOnUiThread {
                        if (it.isBlank()) {
                            binding.btnSend.disabled()
                        } else {
                            binding.btnSend.enabled()
                        }
                    }
                }.launchIn(this)


        }
    }


    private fun initUi() {
        binding.apply {
            rvChats.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = chatAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }
    }

    private fun FragmentChatBinding.setListener() {

        backToBottom.setSafeOnClickListener {
            rvChats.scrollToPosition(chatAdapter.itemCount - 1)
        }

        rvChats.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            lifecycleScope.launch {
                val layoutManager = rvChats.layoutManager as? LinearLayoutManager
                val lastItemPosition = layoutManager?.findLastVisibleItemPosition() ?: 0

                if (scrollY > oldScrollY) {
                    //Scroll Down
//                    MyLogger.v(tagChat, msg = "Down scroll")
                    isFirstTimeDataSetOnUi = false
                    backToBottom.myShow()

                }
                if (scrollY < oldScrollY) {
                    //Scroll Up
//                    MyLogger.v(tagChat, msg = "Up scroll")
                    isFirstTimeDataSetOnUi = false
                    backToBottom.myShow()
                }

                if (scrollY == 0 && lastItemPosition == chatAdapter.itemCount - 1) {
                    //Top Scroll
//                    MyLogger.v(tagChat, msg = "At The Bottom")
                    isFirstTimeDataSetOnUi = true
                    backToBottom.gone()
                }
            }

        }

        icMore.setSafeOnClickListener {
            showPopupMenu()
        }

        icBack.setOnClickListener {
            navController.navigateUp()
        }

        rvChats.setSafeOnClickListener {
            etMessage.clearFocus()
            etMessage.hideKeyboard()
        }

        btnSend.setSafeOnClickListener {
            sendMessage()
        }

    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.setDataLoadedStatus(true)
            chatViewModel.getUser(receiverId)
            chatViewModel.getChatMessage(chatRoomId)
            chatViewModel.listenLastMessage(chatRoomId)
        }
    }


    private fun setChatMessageList(message: List<Message>) {
        if (message.isNotEmpty()) {
            hideNoDataView()
            chatAdapter.submitList(message)

            if (isFirstTimeDataSetOnUi) {
                isFirstTimeDataSetOnUi = false
                lifecycleScope.launch {
                    delay(100)
                    binding.rvChats.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            //Update seen status of message of receiver not my  message
            chatViewModel.updateMessageSeenAvailability(
                message.filter { it.seenStatus != Constants.SeenStatus.MessageSeen.status && it.senderId != senderId },
                chatRoomId
            )
        } else {
            showNoDataView()
        }
    }


    private fun sendMessage() {
        isFirstTimeDataSetOnUi = true
        val message = binding.etMessage.text.toString()

        val timeStamp = System.currentTimeMillis()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val chatType = getChatType(message)
        val chatData = Message(
            messageId = Helper.getMessageId(),
            messageType = MessageType.Chat.type,
            chatType = chatType,
            text = message,
            imageUri = imageUri,
            videoUri = videoUri,
            senderId = senderId,
            receiverId = receiverId,
            messageSentTimeInTimeStamp = timeStamp,
            messageSendTimeInText = timeInText,
            seenStatus = SeenStatus.Sending.status,
            senderProfileImage = myDetails?.userProfileImage
        )
        val lastMessage = LastMessage(
            senderId = senderId,
            receiverId = receiverId,
            messageType = MessageType.Chat.type,
            chatType = chatType,
            message = message,
            lastMessageSentTimeInTimeStamp = timeStamp,
            lastMessageSentTimeInText = timeInText,
            isUser1Online = findUserAvailability(true),
            isUser2Online = findUserAvailability(false),
        )
        chatViewModel.sendMessage(
            chatData,
            lastMessage,
            chatRoomId,
            isUserAppOpen && isUserActiveOnCurrentChat
        )

    }

    private fun getChatType(message: String): String {
        return when {
            message.isNotEmpty() && imageUri != null && videoUri != null -> ChatType.All.name
            message.isNotEmpty() && imageUri != null -> ChatType.TextAndImage.name
            message.isNotEmpty() && videoUri != null -> ChatType.TextAndVideo.name
            imageUri != null && videoUri != null -> ChatType.ImageAndVideo.name
            imageUri != null -> ChatType.OnlyImage.name
            videoUri != null -> ChatType.OnlyVideo.name
            else -> ChatType.OnlyText.name
        }
    }


    private fun showPopupMenu() {
        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpChatScreenBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(binding.icMore)



        bindingPopUp.linearItemDeleteAll.setSafeOnClickListener {
            AlertDialog("Are your sure clear Chats ?", this@ChatFragment, true).show(
                childFragmentManager,
                "MY_Dialog"
            )
            popUp.dismiss()
        }


    }

    private fun setUserProfile(receiver: User) {
        myDetails = receiver
        binding.apply {
            receiver.run {
                Glide.with(ivProfileImage).load(userProfileImage).placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user).into(ivProfileImage)
                tvUserName.text = userName
                isUserAppOpen = userAvailable ?: false
            }

        }


    }

    private fun updateUserAvailability(message: LastMessage) {
        val userAvailable = if (isIAmUser1) {
            message.isUser2Online ?: false
        } else {
            message.isUser1Online ?: false
        }
        updateOnlineStatus(userAvailable)
    }

    private fun updateOnlineStatus(userAvailable: Boolean) {
        if (userAvailable) {
            showOnline()
        } else {
            hideOnline()
        }
        isUserActiveOnCurrentChat = userAvailable
    }

    private fun showOnline() {

        binding.tvOnlineStatus.animate().apply {
            duration = 200
            alpha(1f)
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.tvOnlineStatus.myShow()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
        }

    }

    private fun hideOnline() {
        binding.tvOnlineStatus.animate().apply {
            duration = 200
            alpha(0f)
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    binding.tvOnlineStatus.gone()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
        }

    }

    private fun findUserAvailability(forUser1: Boolean): Boolean {
        return if (forUser1) {
            if (isIAmUser1) {
                true
            } else {
                isUserActiveOnCurrentChat
            }
        } else {
            if (isIAmUser1) {
                isUserActiveOnCurrentChat
            } else {
                true
            }
        }
    }

    private fun clearChat() {

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

    private fun showNoDataView() {
        binding.apply {
            rvChats.gone()
            noDataView.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvChats.myShow()
            noDataView.gone()
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

    override fun onResume() {
        chatViewModel.updateUserAvailabilityForChatRoom(chatRoomId, isIAmUser1, true)
        super.onResume()
    }

    override fun onStop() {
        chatViewModel.updateUserAvailabilityForChatRoom(chatRoomId, isIAmUser1, false)
        super.onStop()
    }


    override fun onResult(isYes: Boolean) {
        if (isYes) {
            clearChat()
        }
    }


    override fun onDestroyView() {
//        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        _binding = null
        super.onDestroyView()
    }

    override fun onImageClick(): (Uri) -> Unit = {

    }

    override fun onVideoClick(): (Uri) -> Unit = {

    }

    override fun onMessageClick(message: Message) {
    }

    override fun onLongMessageClick(message: Message) {
    }


}

typealias ChatType = Constants.PostType