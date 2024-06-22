package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper

import android.animation.Animator
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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.chat.Message
import com.aditya.socialguru.databinding.FragmentChatBinding
import com.aditya.socialguru.databinding.FragmentUserChatProfileBinding
import com.aditya.socialguru.databinding.PopUpChatScreenBinding
import com.aditya.socialguru.databinding.PopUpNotificationFragmentBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.ChatMessageAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.properties.Delegates


class ChatFragment : Fragment() , AlertDialogOption , ChatMessageOption {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null

    private lateinit var userId:String
    private var imageUri:String?=null
    private var videoUri:String?=null

    private var _chatAdapter:ChatMessageAdapter?=null
    private val chatAdapter get() = _chatAdapter!!

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }
    private val chatViewModel by viewModels<ChatViewModel>()
    private var isUserAvailable by Delegates.notNull<Boolean>()



    private val args by navArgs<ChatFragmentArgs>()

    private val chatRoomId by lazy {
        Helper.getChatRoomId(userId)
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
        userId=args.userId
        _chatAdapter= ChatMessageAdapter(this@ChatFragment)
        initUi()
        subscribeToObserver()
        getData()
    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded){
            chatViewModel.setDataLoadedStatus(true)
            chatViewModel.getUser(userId)
            chatViewModel.getChatMessage(chatRoomId)
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun subscribeToObserver() {
        viewLifecycleOwner.observeFlow {

            chatViewModel.userDetails.onEach { response->
                when(response){
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
            chatViewModel.chatMessage.onEach { response->
                when(response){
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

            chatViewModel.sendMessage.onEach { response->
                when(response){
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

            binding.etMessage.getQueryTextChangeStateFlow().debounce(100).distinctUntilChanged().flatMapLatest {
                if (it.startsWith(" ")){
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
                    if (it.isBlank()){
                        binding.btnSend.disabled()
                    }else{
                        binding.btnSend.enabled()
                    }
                }
            }.launchIn(this)


        }
    }

    private fun setChatMessageList(message: List<Message>) {
        if (message.isNotEmpty()){
            hideNoDataView()
            chatAdapter.submitList(message)
            binding.rvChats.smoothScrollToPosition(message.size-1)
        }else{
            showNoDataView()
        }
    }


    private fun initUi() {
        binding.apply {
            rvChats.apply {
                layoutManager=LinearLayoutManager(requireContext())
                adapter=chatAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled=false
            }
            setListener()
        }
    }

    private fun FragmentChatBinding.setListener() {

        icMore.setSafeOnClickListener {
            showPopupMenu()
        }

        icBack.setOnClickListener {
            navController.navigateUp()
        }



        btnSend.setSafeOnClickListener {
            sendMessage()
        }

    }


    private fun sendMessage() {
        val message=binding.etMessage.text.toString()

        val timeStamp=System.currentTimeMillis()
        val chatData=Message(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Chat.type,
            chatType = getChatType(message),
            text=message,
            imageUri = imageUri,
            videoUri = videoUri,
            senderId = AuthManager.currentUserId()!!,
            receiverId = userId,
            messageSentTimeInTimeStamp = timeStamp,
            messageSendTimeInText = Helper.formatTimestampToDateAndTime(timeStamp),
            seenStatus = Constants.SeenStatus.Sending.status

        )
        chatViewModel.sendMessage(chatData,chatRoomId ,isUserAvailable )

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
            AlertDialog("Are your sure clear Chats ?",this@ChatFragment,true).show(childFragmentManager,"MY_Dialog")
            popUp.dismiss()
        }


    }

    private fun setUserProfile(receiver: User) {
        binding.apply {
            receiver.run {
                Glide.with(ivProfileImage).load(userProfileImage).placeholder(R.drawable.ic_user).error(R.drawable.ic_user).into(ivProfileImage)
                tvUserName.text=userName
                updateOnlineStatus(userAvailable ?: false)
            }

        }


    }

    private fun updateOnlineStatus(userAvailable: Boolean) {
        if (userAvailable){
            showOnline()
        }else{
            hideOnline()
        }
        isUserAvailable=userAvailable
    }

    private fun showOnline() {

        binding.tvOnlineStatus.animate().apply {
         duration=200
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
            duration=200
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


    override fun onResult(isYes: Boolean) {
        if (isYes){
            clearChat()
        }
    }


    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

    override fun onImageClick(): (Uri) -> Unit ={

    }

    override fun onVideoClick(): (Uri) -> Unit ={

    }
    override fun onMessageClick(message: Message) {
    }

    override fun onLongMessageClick(message: Message) {
    }


}

typealias ChatType = Constants.PostType