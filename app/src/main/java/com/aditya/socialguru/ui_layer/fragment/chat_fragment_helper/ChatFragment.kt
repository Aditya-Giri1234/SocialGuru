package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
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
import com.aditya.socialguru.domain_layer.custom_class.dialog.chat.AttachmentDialog
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.MessageType
import com.aditya.socialguru.domain_layer.helper.Constants.SeenStatus
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.helper.showKeyboard
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.remote_service.chat.OnAttachmentItemListener
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.ChatMessageAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.listeners.OnEmojiPopupShownListener
import com.vanniktech.ui.hideKeyboardAndFocus
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


class ChatFragment : Fragment(), AlertDialogOption, ChatMessageOption, OnAttachmentItemListener {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null

    private val tagChat = Constants.LogTag.Chats

    private lateinit var receiverId: String  //This is for receive userId
    private var dialogInvokeReason = Constants.ChatDialogInvokeAction.DeleteSingleChat
    private var isUserAppOpen = false
    private var isUserActiveOnCurrentChat = false
    private var isFirstTimeDataSetOnUi = true
    private var attachmentImageUri: Uri? = null
    private var attachmentVideoUri: Uri? = null
    private val emojiKeyboardTag = 0
    private val emojiPopup by lazy {
        EmojiPopup(
            binding.root,
            binding.etMessage
        )
    }

    private var imageUri: String? = null
    private var videoUri: String? = null
    private var deleteMessage: Message? = null

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

    private val cameraIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val uri = it.data?.extras?.get
                if (uri != null) {
                    setImageOnAttachmentUi(uri)
                    MyLogger.i(tagChat, msg = "User capture image and now it show to ui !")
                } else {
                    MyLogger.e(
                        tagChat,
                        msg = "User select image but some error occurred so that uri is null and ${it.data?.data} !"
                    )
                }
            } else {
                MyLogger.v(tagChat, msg = "User cancel image capturing !")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        etMessage.setOnTouchListener { v, event ->
            MyLogger.v(tagChat, msg = "Now user touch on edit status view !")
            v.onTouchEvent(event)
            emojiPopup.dismiss()
            true
        }

        icEmoji.setSafeOnClickListener {
            //https://johncodeos.com/move-view-with-keyboard-in-android-using-kotlin/
            // Use this article to solve keyboard issue

            if (it.tag.toString().toInt() == emojiKeyboardTag) {
                emojiPopup.toggle()
                icEmoji.tag = "1"  // Important tag set into string else get unexpected result
                icEmoji.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_keybaord
                    )
                )
            } else {
                icEmoji.tag = "0"
                emojiPopup.dismiss()
                icEmoji.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.emoji
                    )
                )
            }
        }
        btnSend.setSafeOnClickListener {
            sendMessage()
        }

        icAttachment.setSafeOnClickListener {
            AttachmentDialog(this@ChatFragment).show(childFragmentManager, "My_Attchment_Dialog")
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
            dialogInvokeReason = Constants.ChatDialogInvokeAction.ClearChat
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

    private fun deleteThisMessage(deleteMessage: Message?) {
        deleteMessage?.let {
            if (chatAdapter.itemCount == 1) {
                clearChat()
            } else {
                when {
                    chatAdapter.findMessageIndex(it) == chatAdapter.itemCount - 1 -> {
                        //User want to delete  last message so need to update last message on chat room and recent chat data in both parties
                        val secondLastMessage = chatAdapter.giveMeSecondLastMessage()
                        val lastMessage = LastMessage(
                            senderId = secondLastMessage.senderId,
                            receiverId = secondLastMessage.receiverId,
                            messageType = secondLastMessage.messageType,
                            chatType = secondLastMessage.chatType,
                            message = secondLastMessage.text,
                            lastMessageSentTimeInTimeStamp = secondLastMessage.messageSentTimeInTimeStamp,
                            lastMessageSentTimeInText = secondLastMessage.messageSendTimeInText,
                            isUser1Online = findUserAvailability(true),
                            isUser2Online = findUserAvailability(false),
                        )

                        chatViewModel.deleteMessage(
                            it,
                            chatRoomId,
                            receiverId,
                            lastMessage,
                            secondLastMessage
                        )

                    }

                    else -> {
                        chatViewModel.deleteMessage(it, chatRoomId, receiverId, null, null)
                    }
                }
            }
        }
    }

    private fun setImageOnAttachmentUi(data: Uri) {
        attachmentImageUri = data
        binding.apply {
            linearAttachment.myShow()
            cardImageMessage.myShow()
            ivImageSendMessage.setImageURI(data)
            ivCancleImage.setSafeOnClickListener {
                attachmentImageUri=null
                cardImageMessage.gone()
                hideLinearAttachmentLayout()
            }
        }
    }

    private fun setVideoOnAttachmentUi(data: Uri) {
        attachmentVideoUri = data
        binding.apply {
            linearAttachment.myShow()
            cardVideoMessage.myShow()
            ivVideoSendMessage.setImageURI(data)
            ivCancleVideo.setSafeOnClickListener {
                attachmentVideoUri=null
                cardVideoMessage.gone()
                hideLinearAttachmentLayout()
            }
        }
    }

    private fun hideLinearAttachmentLayout() {
        binding.apply {
            if (cardImageMessage.isGone && cardVideoMessage.isGone){
                linearAttachment.gone()
            }
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
            when (dialogInvokeReason) {
                Constants.ChatDialogInvokeAction.ClearChat -> {
                    clearChat()
                }

                Constants.ChatDialogInvokeAction.DeleteSingleChat -> {
                    deleteThisMessage(deleteMessage)
                }
            }
        }
    }


    override fun onImageClick(): (Uri) -> Unit = {

    }


    override fun onVideoClick(): (Uri) -> Unit = {

    }

    override fun onMessageClick(message: Message) {

    }

    override fun onLongMessageClick(message: Message) {
        deleteMessage = message
        dialogInvokeReason = Constants.ChatDialogInvokeAction.DeleteSingleChat
        AlertDialog("Are you sure delete this chat ?", this, true).show(
            childFragmentManager,
            "My_Delete_Dialog"
        )
    }

    override fun onAttachmentImageClick() {
        cameraIntent.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    override fun onAttachmentGalleryClick() {

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}

typealias ChatType = Constants.PostType