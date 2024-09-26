package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.single_chat

import android.animation.Animator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
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
import com.aditya.socialguru.domain_layer.helper.monitorInternet
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.resizeActivate
import com.aditya.socialguru.domain_layer.helper.resizeStop
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.remote_service.chat.OnAttachmentItemListener
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.ChatMessageAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import com.vanniktech.emoji.EmojiPopup
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
import java.io.File


class ChatFragment : Fragment(), AlertDialogOption, ChatMessageOption, OnAttachmentItemListener {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private lateinit var receiverId: String  //This is for receive userId
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private var dialogInvokeReason = Constants.ChatDialogInvokeAction.DeleteSingleChat
    private var isUserAppOpen = false
    private var isUserActiveOnCurrentChat = false
    private var isFirstTimeDataSetOnUi = true
    private val MAX_VIDEO_SIZE_MB = 50f
    private var isReceiverOnlineStatusIsHide = false
    private var isMediaUploading =false

    private val emojiKeyboardTag = 0
    private val emojiPopup by lazy {
        EmojiPopup(
            binding.root,
            binding.etMessage
        )
    }

    private var myLoader: MyLoader? = null
    private var imageUri: String? = null
    private var videoUri: String? = null
    private var deleteMessage: Message? = null

    private var _chatAdapter: ChatMessageAdapter? = null
    private val chatAdapter get() = _chatAdapter!!


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
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result) {
                chatViewModel.imageUri?.let { setImageOnAttachmentUi(it) }
            } else {
                MyLogger.v(tagChat, msg = "User cancel image capturing !")
            }
        }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the photo picker.
            uri?.let { selectedUri ->
                val mimeType = requireContext().contentResolver.getType(selectedUri)
                mimeType?.let {
                    if (it.startsWith("image/")) {
                        // It's an image
                        MyLogger.v(tagChat, msg = "User selected an image! URI: $selectedUri")
                        setImageOnAttachmentUi(selectedUri)
                    } else if (it.startsWith("video/")) {
                        // It's a video
                        MyLogger.v(tagChat, msg = "User selected a video! URI: $selectedUri")
                        handleVideoSelection(selectedUri)
                    } else {
                        // Unsupported media type
                        MyLogger.v(
                            tagChat,
                            msg = "User selected media with unsupported type! URI: $selectedUri"
                        )
                        // Handle unsupported media type
                    }
                } ?: run {
                    // MIME type could not be determined
                    MyLogger.v(
                        tagChat,
                        msg = "Could not determine MIME type for selected media! URI: $selectedUri"
                    )
                }
            } ?: run {
                // URI is null, user cancelled or selection failed
                MyLogger.v(tagChat, msg = "User cancelled media selection or selection failed!")
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
        requireActivity().resizeActivate()
        initUi()
        subscribeToObserver()
        getDataWithValidation()
    }


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.userDetails.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            setUserProfile(it)
                        }
                      if (!chatViewModel.isDataLoaded){
                          getChatDataAndListen()
                      }
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.clear()
                                    jobQueue.add {
                                        getData()
                                    }
                                }
                                else ->{
                                    showSnackBar(message = response.message)
                                }
                            }
                        }
                    }
                }

            }.launchIn(this)

            chatViewModel.lastMessage.onEach {
                updateUserAvailability(it)
            }.launchIn(this)

            chatViewModel.chatMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        launch {
                            runOnUiThread {
                                response.data?.let {
                                    setChatMessageList(it)
                                }
                            }
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.add {
                                        getChatDataAndListen()
                                    }
                                }
                                else ->{
                                    showSnackBar(message = response.message)
                                }
                            }
                        }
                    }
                }

            }.launchIn(this)
            requireContext().monitorInternet().onEach { isInternetAvailable ->
                if (isInternetAvailable) {
                    jobQueue.forEach {
                        it.invoke()
                    }
                    jobQueue.clear()
                }
            }.launchIn(this)

            chatViewModel.sendMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            when {
                                it.isSending -> {
                                    myLoader?.setLoadingStatus(
                                        it.sendingMessage,
                                        it.progress ?: 0,
                                        false
                                    )
                                }

                                it.isSuccess -> {
                                    resetUiScreen()
                                }

                                else -> {
                                    resetUiScreen()
                                }
                            }
                        }

                    }

                    is Resource.Loading -> {
                        isMediaUploading=true
                        showDialog()
                    }

                    is Resource.Error -> {
                        isMediaUploading=false
                        hideDialog()
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

            chatViewModel.clearChat.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showNoDataView()
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message, false)
                    }
                }

            }.launchIn(this)
            chatViewModel.deleteMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message, false)
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
                        setSendButtonState()
                    }
                }.launchIn(this)


        }
    }


    private fun initUi() {
        binding.apply {
            chatViewModel.imageUri?.apply {
                setImageOnAttachmentUi(this)
            }
            chatViewModel.videoUri?.apply {
                setVideoOnAttachmentUi(this)
            }
            rvChats.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = chatAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
                this.setItemViewCacheSize(4)
            }
            setListener()
        }
    }

    private fun FragmentChatBinding.setListener() {

        ivProfileImage.setSafeOnClickListener {
            navigateToChatProfileScreen()
        }
        linearUserStatus.setSafeOnClickListener {
            navigateToChatProfileScreen()
        }

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

        cardImageMessage.setSafeOnClickListener {
            navigateToImageViewScreen(chatViewModel.imageUri)
        }
        cardVideoMessage.setSafeOnClickListener {
            navigateToVideoViewScreen(chatViewModel.videoUri)
        }

    }

    private fun getDataWithValidation() {
        if (!chatViewModel.isDataLoaded) {
            getData()
        }
    }
    private fun getData(){
        chatViewModel.getUser(receiverId)
    }
    private fun getChatDataAndListen(){
        chatViewModel.getChatMessage(chatRoomId)
        chatViewModel.listenLastMessage(chatRoomId)

        // Now set data loaded state to true
        chatViewModel.setDataLoadedStatus(true)
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
        imageUri = chatViewModel.imageUri?.toString()
        videoUri = chatViewModel.videoUri?.toString()
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
            seenStatus = SeenStatus.Sending.status
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
        ){
            runOnUiThread {
                resetUiScreen()
            }
        }

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
        chatAdapter.submitUser(receiver)
        binding.apply {
            receiver.run {
                if (userProfileImage==null){
                    tvInitial.myShow()
                    ivProfileImage.gone()
                    tvInitial.text = userName?.get(0).toString()
                    tvInitial.setCircularBackground(Helper.setUserProfileColor(this))
                }else{
                    tvInitial.gone()
                    ivProfileImage.myShow()
                    Glide.with(ivProfileImage).load(userProfileImage).placeholder(R.drawable.ic_user).error(
                        R.drawable.ic_user).into(ivProfileImage)
                }
                tvUserName.text = userName

                isUserAppOpen = userAvailable ?: false
                this@ChatFragment.isReceiverOnlineStatusIsHide = userSetting
                    ?.isMyOnlineStatusHideEnable ?: isReceiverOnlineStatusIsHide
            }
        }
    }

    private fun updateUserAvailability(message: LastMessage) {
        if (isReceiverOnlineStatusIsHide){
            hideOnline()
        }else{
            val userAvailable = if (isIAmUser1) {
                message.isUser2Online ?: false
            } else {
                message.isUser1Online ?: false
            }
            updateOnlineStatus(userAvailable , message)
        }

    }

    private fun updateOnlineStatus(userAvailable: Boolean, message: LastMessage) {
        if (userAvailable) {
            showOnline()
        } else {
            val lastTimeForReceiver = if (isIAmUser1) {
                message.user1LastOnlineTimeStamp
            } else {
                message.user2LastOnlineTimeStamp
            }
            if(lastTimeForReceiver==null){
                hideOnline()
            }else{
                showOnline(Helper.getTimeAgo(lastTimeForReceiver))
            }

        }
        isUserActiveOnCurrentChat = userAvailable
    }

    private fun showOnline(timeAgo: String? =null) {
        if(timeAgo!=null){
            binding.tvOnlineStatus.text = timeAgo
        }else{
            binding.tvOnlineStatus.text = "Online"
        }

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
        chatViewModel.clearChat(chatRoomId, receiverId)
    }

    private fun showDialog() {
        myLoader?.dismiss()
        myLoader = MyLoader()
        myLoader?.show(childFragmentManager, "My_Loader")
    }

    private fun hideDialog() {
        if(isMediaUploading) return
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
            if (chatAdapter.countNonHeaderMessages() == 1) {
                clearChat()
            } else {
                when {
                    chatAdapter.findMessageIndex(it) == chatAdapter.itemCount - 1 -> {
                        //User want to delete  last message so need to update last message on chat room and recent chat data in both parties
                        val secondLastMessage = chatAdapter.giveMeSecondLastMessage()
                        val lastMessage=if (secondLastMessage!=null){
                            LastMessage(
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
                        }else{
                            LastMessage()
                        }


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
        chatViewModel.setImageUriData(data)
        binding.apply {
            linearAttachment.myShow()
            cardImageMessage.myShow()
            ivImageSendMessage.setImageURI(data)
            if (!cardImageMessage.isGone) {
                val params = cardVideoMessage.layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                cardVideoMessage.layoutParams = params
            }
            ivCancleImage.setSafeOnClickListener {
                chatViewModel.setImageUriData(null)
                cardImageMessage.gone()
                hideLinearAttachmentLayout()
            }
        }
        setSendButtonState()
    }

    private fun setVideoOnAttachmentUi(data: Uri) {
        chatViewModel.setVideoUriData(data)
        binding.apply {
            linearAttachment.myShow()
            cardVideoMessage.myShow()
            Glide.with(ivVideoSendMessage).load(data).into(ivVideoSendMessage)
            if (cardImageMessage.isGone) {
                val params = cardVideoMessage.layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart = 0
                cardVideoMessage.layoutParams = params
            } else {
                val params = cardVideoMessage.layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                cardVideoMessage.layoutParams = params
            }
            ivCancleVideo.setSafeOnClickListener {
                chatViewModel.setVideoUriData(null)
                cardVideoMessage.gone()
                hideLinearAttachmentLayout()
            }
        }
        setSendButtonState()
    }

    private fun hideLinearAttachmentLayout() {
        binding.apply {
            if (cardImageMessage.isGone && cardVideoMessage.isGone) {
                linearAttachment.gone()
                setSendButtonState()
            }
        }
    }

    private fun getFileUri(): Uri {
        val folderName = "picFromCamera"
        val folder = File(requireContext().cacheDir, folderName)

        // Check if the directory exists and attempt to delete it
        if (folder.exists()) {
            val isDirDeleted = folder.deleteRecursively() // Deletes the directory and its contents
            MyLogger.v(tagChat, msg = "Pic Directory deleted: $isDirDeleted")
            if (!isDirDeleted) {
                MyLogger.e(tagChat, msg = "Failed to delete Pic Directory")
            }
        }

        // Attempt to create the directory
        val isDirCreated = folder.mkdirs()
        MyLogger.v(tagChat, msg = "File created: $isDirCreated")

        if (!isDirCreated) {
            MyLogger.e(tagChat, msg = "Failed to create Pic Directory")
        }

        // Create a new file within the directory
        val imageFile = File(folder, "${System.currentTimeMillis()}_my_pic")

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
    }

    private fun handleVideoSelection(uri: Uri) {
        val videoLength = Helper.getVideoSize(requireActivity(), uri)
        when {
            videoLength == -1f -> {
                MyLogger.e(
                    tagChat,
                    msg = "User selected video length is -1 means some error occurred !"
                )
                Helper.showSnackBar(
                    (requireActivity() as MainActivity).findViewById(
                        R.id.coordLayout
                    ),
                    "Some error occurred during video fetching  , may be file size to large!"
                )
            }

            videoLength >= MAX_VIDEO_SIZE_MB -> {
                MyLogger.e(
                    tagChat,
                    msg = "User selected video length is $videoLength and max lenght :- $MAX_VIDEO_SIZE_MB  which exceeded ! "
                )
                Helper.showSnackBar(
                    (requireActivity() as MainActivity).findViewById(
                        R.id.coordLayout
                    ), "Video length exceeded , max length is $MAX_VIDEO_SIZE_MB mb !"
                )
            }

            else -> {
                MyLogger.v(
                    tagChat,
                    msg = "User selected video length is $videoLength and max length :- $MAX_VIDEO_SIZE_MB  which is not exceeded 😁! "
                )
                setVideoOnAttachmentUi(uri)
            }
        }

    }

    private fun hideMediaPanel() {
        binding.apply {
            chatViewModel.setImageUriData(null)
            chatViewModel.setVideoUriData(null)
            cardImageMessage.gone()
            cardVideoMessage.gone()
            hideLinearAttachmentLayout()
        }
    }

    private fun resetUiScreen() {
        isMediaUploading=false
        hideDialog()
        binding.etMessage.text.clear()
        hideMediaPanel()
        binding.backToBottom.gone()
    }

    private fun setSendButtonState() {
        binding.apply {
            if (etMessage.text.isNotBlank() || !linearAttachment.isGone) {
                btnSend.enabled()
            } else {
                btnSend.disabled()
            }
        }
    }

    private fun navigateToChatProfileScreen(){
        val direction:NavDirections=ChatFragmentDirections.actionChatFragmentToUserChatProfileFragment(chatRoomId,receiverId)
        navController.safeNavigate(direction,Helper.giveAnimationNavOption())
    }

    private fun navigateToProfileViewScreen(userId:String){
        val direction:NavDirections=ChatFragmentDirections.actionGlobalProfileViewFragment(userId)
        navController.safeNavigate(direction,Helper.giveAnimationNavOption())
    }


    private fun navigateToVideoViewScreen(attachmentVideoUri: Uri?) {
        attachmentVideoUri?.apply {
            val direction: NavDirections =
                BottomNavigationBarDirections.actionGlobalShowVideoFragment(this,  true)
            navController.safeNavigate(direction, Helper.giveAnimationNavOption())
        }
    }

    private fun navigateToImageViewScreen(attachmentImageUri: Uri?) {
        attachmentImageUri?.apply {
            val direction: NavDirections =
                BottomNavigationBarDirections.actionGlobalShowImageFragment(this , true)
            navController.safeNavigate(direction, Helper.giveAnimationNavOption())
        }
    }

    override fun onResume() {
        chatViewModel.updateUserAvailabilityForChatRoom(chatRoomId, isIAmUser1, true)
        super.onResume()
    }

    override fun onPause() {
        hideKeyboard()
        super.onPause()
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
        navigateToImageViewScreen(it)
    }


    override fun onVideoClick(): (Uri) -> Unit = {
        navigateToVideoViewScreen(it)
    }

    override fun <T> onMessageClick(message: T) {

    }

    override fun <T> onLongMessageClick(message: T) {
        deleteMessage = message as Message
        dialogInvokeReason = Constants.ChatDialogInvokeAction.DeleteSingleChat
        AlertDialog("Are you sure delete this chat ?", this, true).show(
            childFragmentManager,
            "My_Delete_Dialog"
        )
    }

    override fun onProfileClick(userId:String?) {
        if (userId==null){
            navigateToChatProfileScreen()
        }else{
            navigateToProfileViewScreen(userId)
        }
    }

    override fun onAttachmentImageClick() {
        chatViewModel.setImageUriData(getFileUri())
        MyLogger.i(tagChat, msg = "Image uri for saving pic :- ${chatViewModel.imageUri}")
        cameraIntent.launch(chatViewModel.imageUri)
    }

    override fun onAttachmentGalleryClick() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    override fun onDestroyView() {
        requireActivity().resizeStop()
        _chatAdapter=null
        binding.rvChats.adapter = null
        _binding = null
        super.onDestroyView()
    }

}

typealias ChatType = Constants.PostType