package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.single_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.BottomNavigationBarDirections
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.databinding.FragmentUserChatProfileBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.convertParseUri
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class UserChatProfileFragment : Fragment() {

    private var _binding: FragmentUserChatProfileBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private lateinit var userId: String  //This is for  userId
    private lateinit var chatRoomId: String  //This is for  chatRoomId
    private var defaultMuteValueForReceiver: Boolean? = null

    private var myLoader: MyLoader? = null
    private var userData: User? = null

    private val args by navArgs<UserChatProfileFragmentArgs>()

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val chatViewModel by viewModels<ChatViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserChatProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        userId = args.userId
        chatRoomId = args.chatRoomId
        initUi()
        subscribeToObserver()
        getData()
    }

    private fun subscribeToObserver() {
        observeFlow {
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
                        MyLogger.e(tagChat, msg = "Internet Off")
                        loadDefaultSetting()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(response.message, isSuccess = false)
                    }
                }

            }.launchIn(this)

        }
    }

    private fun loadDefaultSetting() {
        defaultMuteValueForReceiver?.let {
            binding.switchMute.isChecked = it
        }
    }

    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                profileImage.gone()
                icBack.myShow()
                tvHeaderUserName.text = "Chat Profile"
            }
            setListener()
        }
    }

    private fun FragmentUserChatProfileBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        ivProfileImage.setSafeOnClickListener {
            navigateToImageViewScreen()
        }
        switchMute.setOnCheckedChangeListener { buttonView, isChecked ->
            chatViewModel.muteChatNotification(userId, isChecked)
        }
        ivMedia.setSafeOnClickListener {
            navigateToMediaScreen()
        }
        cardMedia.setSafeOnClickListener {
            navigateToMediaScreen()
        }
    }


    private fun setUserProfile(receiver: User) {
        userData = receiver
        binding.apply {
            receiver.run {
                Glide.with(ivProfileImage).load(userProfileImage).placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user).into(ivProfileImage)
                tvUserName.text = userName
            }
        }
    }

    private fun getData() {
        if (!chatViewModel.isDataLoaded) {
            chatViewModel.setDataLoadedStatus(true)
            chatViewModel.getUser(userId)
            chatViewModel.isUserMutedAndListen(userId)
        }
    }

    private fun navigateToImageViewScreen() {
        userData?.userProfileImage?.apply {
            val directions: NavDirections =
                BottomNavigationBarDirections.actionGlobalShowImageFragment(this.convertParseUri())
            navController.safeNavigate(directions, Helper.giveAnimationNavOption())
        }
    }

    private fun navigateToMediaScreen() {
        val directions: NavDirections =
            UserChatProfileFragmentDirections.actionUserChatProfileFragmentToChatMediaFragment(chatRoomId)
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
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


