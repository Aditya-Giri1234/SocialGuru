package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.group_chat

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMember
import com.aditya.socialguru.data_layer.model.chat.group.GroupMemberDetails
import com.aditya.socialguru.data_layer.model.chat.group.GroupMembersList
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.databinding.FragmentGroupChatBinding
import com.aditya.socialguru.databinding.PopUpChatScreenBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.chat.AttachmentDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.remote_service.chat.OnAttachmentItemListener
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.GroupChatAdapter
import com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.single_chat.ChatType
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


class GroupChatFragment : Fragment() , AlertDialogOption, ChatMessageOption,
    OnAttachmentItemListener {
    private var _binding: FragmentGroupChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private var dialogInvokeReason = Constants.ChatDialogInvokeAction.DeleteSingleChat
    private var isUserAppOpen = false
    private var isUserActiveOnCurrentChat = false
    private var isFirstTimeDataSetOnUi = true
    private val MAX_VIDEO_SIZE_MB = 50f

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
    private var deleteMessage: GroupMessage? = null

    private var _chatAdapter: GroupChatAdapter? = null
    private val chatAdapter get() = _chatAdapter!!

    private val groupMembers= mutableListOf<GroupMember>()  // This is list of all member including me
    private val onlyGroupMembers  get() = groupMembers.mapNotNull { it.memberId }.filter { it!=senderId }



    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }
    private val chatViewModel by navGraphViewModels<ChatViewModel>(R.id.groupChatFragment)

    private val senderId by lazy {
        AuthManager.currentUserId()!!
    }

    private val args by navArgs<GroupChatFragmentArgs>()

    private lateinit var chatRoomId:String


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
        _binding = FragmentGroupChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        chatRoomId = args.chatRoomId
        _chatAdapter = GroupChatAdapter(this@GroupChatFragment)
        initUi()
        subscribeToObserver()
        getData()
    }


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun subscribeToObserver() {
        viewLifecycleOwner.observeFlow {
            chatViewModel.groupChatMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            setChatMessageList(it)
                        }
                        chatViewModel.listenNewMessage(chatRoomId)

                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

            chatViewModel.groupInfo.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            setGroupInfo(it)
                        }
                        if (!chatViewModel.isDataLoaded){
                            chatViewModel.getGroupMemberDetails(chatRoomId)
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

            chatViewModel.groupMemberDetails.onEach { response ->
                MyLogger.w(tagChat , msg = "GroupMemberDetails response come !")
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            setGroupMemberDetails(it)
                            val isIAmExitFromGroup = it.all{it.member.memberId!=AuthManager.currentUserId()}
                            if(isIAmExitFromGroup) navController.navigateUp()
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
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

            chatViewModel.sendGroupMessage.onEach { response ->

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
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

            chatViewModel.clearGroupChat.onEach { response ->
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
            chatViewModel.deleteGroupMessage.onEach { response ->
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
            }
            setListener()
        }
    }

    private fun FragmentGroupChatBinding.setListener() {

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
            AttachmentDialog(this@GroupChatFragment).show(childFragmentManager, "My_Attchment_Dialog")
        }

        cardImageMessage.setSafeOnClickListener {
            navigateToImageViewScreen(chatViewModel.imageUri)
        }
        cardVideoMessage.setSafeOnClickListener {
            navigateToVideoViewScreen(chatViewModel.videoUri)
        }

    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.getGroupInfo(chatRoomId)
        }
    }
    private fun getChatDataAndListen(){
        chatViewModel.getGroupChatMessage(chatRoomId)

        // Now set data loaded state to true
        chatViewModel.setDataLoadedStatus(true)
    }

    private fun setChatMessageList(message: List<GroupMessage>) {
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
//            chatViewModel.updateMessageSeenAvailability(
//                message.filter { it.seenStatus != Constants.SeenStatus.MessageSeen.status && it.senderId != senderId },
//                chatRoomId
//            )
        } else {
            showNoDataView()
        }
    }

    private fun setGroupMemberDetails(groupMembers: List<GroupMemberDetails>) {
        //Clear list
        this.groupMembers.clear()

        val maxParticipants = 5
        val maxChars = 50
        var currentLength = 0
        val participants = mutableListOf<String>()

        for (member in groupMembers) {
            val username = member.memberInfo.userName!!
            if (participants.size < maxParticipants && currentLength + username.length <= maxChars) {
                participants.add(username)
                currentLength += username.length
            } else {
                break
            }
        }

        val members= if (participants.size < groupMembers.size) {
            participants.joinToString(", ") + "..."
        } else {
            participants.joinToString(", ")
        }

        MyLogger.d(tagChat, msg = members)
        binding.tvGroupMember.text = members

        this.groupMembers.addAll(groupMembers.map { it.member })
        chatAdapter.submitUser(groupMembers.associateBy({it.member.memberId!!},{it.memberInfo}))
    }

    private fun sendMessage() {
        isFirstTimeDataSetOnUi = true
        imageUri = chatViewModel.imageUri?.toString()
        videoUri = chatViewModel.videoUri?.toString()
        val message = binding.etMessage.text.toString()

        val timeStamp = System.currentTimeMillis()
        val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
        val chatType = getChatType(message)
        val chatData = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Chat.type,
            chatType = chatType,
            text = message,
            imageUri = imageUri,
            videoUri = videoUri,
            senderId = senderId,
            messageSentTimeInTimeStamp = timeStamp,
            messageSendTimeInText = timeInText,
            seenStatus = Constants.SeenStatus.Sending.status,
            sendTimeUsers = onlyGroupMembers

        )
        val lastMessage = GroupLastMessage(
            senderId = senderId,
            messageType = Constants.MessageType.Chat.type,
            chatType = chatType,
            message = message,
            lastMessageSentTimeInTimeStamp = timeStamp,
            lastMessageSentTimeInText = timeInText,
        )
        chatViewModel.sendGroupMessage(
            chatData,
            lastMessage,
            chatRoomId,
            groupMembers
        ){
            resetUiScreen()
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
            AlertDialog("Are your sure clear Chats ?", this@GroupChatFragment, true).show(
                childFragmentManager,
                "MY_Dialog"
            )
            popUp.dismiss()
        }


    }



    private fun setGroupInfo(groupInfo: GroupInfo){
        binding.apply {
            tvGroupName.text = groupInfo.groupName
            Glide.with(ivProfileImage).load(groupInfo.groupPic).placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user).into(ivProfileImage)
        }
    }

    private fun clearChat() {
        val secondLastMessage = chatAdapter.giveMeInfoMessageFromLast()!!
        val lastMessage= GroupLastMessage(
            senderId = secondLastMessage.senderId,
            messageType = secondLastMessage.messageType,
            chatType = secondLastMessage.chatType,
            message = secondLastMessage.text,
            lastMessageSentTimeInTimeStamp = secondLastMessage.messageSentTimeInTimeStamp,
            lastMessageSentTimeInText = secondLastMessage.messageSendTimeInText,
            infoMessageType = secondLastMessage.messageType,
            addedOrRemovedUserId = secondLastMessage.addedOrRemovedUserId
        )
        chatViewModel.clearGroupChat(chatRoomId, groupMembers.mapNotNull { it.memberId } ,lastMessage,secondLastMessage)

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

    private fun deleteThisMessage(deleteMessage: GroupMessage?) {
        deleteMessage?.let {
            if (chatAdapter.countNonHeaderAndNonInfoMessages() == 1) {
                clearChat()
            } else {
                when {
                    chatAdapter.findMessageIndex(it) == chatAdapter.itemCount - 1 -> {
                        //User want to delete  last message so need to update last message on chat room and recent chat data in both parties
                        val secondLastMessage = chatAdapter.giveMeSecondLastMessage()
                        val lastMessage=if (secondLastMessage!=null){
                            GroupLastMessage(
                                senderId = secondLastMessage.senderId,
                                messageType = secondLastMessage.messageType,
                                chatType = secondLastMessage.chatType,
                                message = secondLastMessage.text,
                                lastMessageSentTimeInTimeStamp = secondLastMessage.messageSentTimeInTimeStamp,
                                lastMessageSentTimeInText = secondLastMessage.messageSendTimeInText,
                                infoMessageType = secondLastMessage.infoMessageType,
                                addedOrRemovedUserId = secondLastMessage.addedOrRemovedUserId
                            )
                        }else{
                            GroupLastMessage()
                        }


                        chatViewModel.deleteGroupMessage(
                            it,
                            chatRoomId,
                            groupMembers.mapNotNull { it.memberId },
                            lastMessage,
                            secondLastMessage
                        )

                    }

                    else -> {
                        chatViewModel.deleteGroupMessage(it, chatRoomId , groupMembers.mapNotNull { it.memberId }, null, null)
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
        val direction: NavDirections =GroupChatFragmentDirections.actionGroupChatFragmentToGroupProfileFragment(chatRoomId , GroupMembersList(groupMembers) )
        navController.safeNavigate(direction, Helper.giveAnimationNavOption())
    }
    private fun navigateToVideoViewScreen(attachmentVideoUri: Uri?) {
        attachmentVideoUri?.apply {
            val direction: NavDirections =
                BottomNavigationBarDirections.actionGlobalShowVideoFragment(this)
            navController.safeNavigate(direction, Helper.giveAnimationNavOption())
        }
    }

    private fun navigateToImageViewScreen(attachmentImageUri: Uri?) {
        attachmentImageUri?.apply {
            val direction: NavDirections =
                BottomNavigationBarDirections.actionGlobalShowImageFragment(this)
            navController.safeNavigate(direction, Helper.giveAnimationNavOption())
        }
    }

    override fun onResume() {
        chatViewModel.updateGroupMemberOnlineStatus(chatRoomId, true)
        super.onResume()
    }

    override fun onStop() {
        chatViewModel.updateGroupMemberOnlineStatus(chatRoomId, false)
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
        deleteMessage = message as GroupMessage
        dialogInvokeReason = Constants.ChatDialogInvokeAction.DeleteSingleChat
        AlertDialog("Are you sure delete this chat ?", this, true).show(
            childFragmentManager,
            "My_Delete_Dialog"
        )
    }

    override fun onProfileClick() {
        navigateToChatProfileScreen()
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
        _chatAdapter=null
        binding.rvChats.adapter = null
        _binding = null
        super.onDestroyView()
    }

}

