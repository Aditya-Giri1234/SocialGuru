package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.group_chat

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentEditGroupProfileBinding
import com.aditya.socialguru.databinding.FragmentGroupMembersBinding
import com.aditya.socialguru.databinding.PopUpGroupMemberActivityBinding
import com.aditya.socialguru.databinding.PopUpProfileSettingBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.ProfilePicEditDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.UserAdapter
import com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment.ProfileFragmentDirections
import com.aditya.socialguru.ui_layer.fragment.post.UserLikeLIstFragmentArgs
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class GroupMembersFragment : Fragment() , AlertDialogOption {

    private var _binding: FragmentGroupMembersBinding? = null
    private val binding get() = _binding!!
    private val tagChat = Constants.LogTag.Chats
    private lateinit var chatRoomId: String
    private val groupMembers: MutableList<FriendCircleData> = mutableListOf()
    private var groupMembersList : MutableList<GroupMember> = mutableListOf()
    private var defaultAlertDialogOption = GroupMemberScreenAlertDialogOption.REMOVE_USER
    private var currentUserId:String ?=null // This store which user  current tap
    private var myLoader: MyLoader? = null
    private var groupInfo: GroupInfo? = null

    private var _userAdapter: UserAdapter? = null
    private val userAdapter get() = _userAdapter!!

    private val chatViewModel by viewModels<ChatViewModel>()
    private val args by navArgs<GroupChatFragmentArgs>()

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatRoomId=args.chatRoomId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentGroupMembersBinding.inflate(layoutInflater)
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



    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.groupInfo.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            groupInfo = it
                            handleGroupInfo(it)
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            chatViewModel.groupMemberDetails.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            updateGroupMemberData(it)
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

            chatViewModel.sendGroupInfoMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Successfully Exit From Group!", isSuccess = true)

                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()


                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            showSnackBar(response.message?.toString())
                        }
                    }
                }
            }.launchIn(this)
        }
    }

    private fun handleGroupInfo(info: GroupInfo?) {
        if(info ==null) return

        userAdapter.setGroupInfo(info)

        if (groupMembers.isNotEmpty()){
            val sortedGroupMemberList = groupMembers.sortedWith(compareBy<FriendCircleData> { member ->
                when {
                    member.userId == info.creatorId -> 0 // Creator first
                    info.groupAdmins?.contains(member.userId) == true -> 1 // Admins second
                    else -> 2 // Others last
                }
            }.thenBy { member ->
                member.timeStamp // Sort by timestamp for non-creators and non-admins
            })
            userAdapter.submitList(sortedGroupMemberList)
        }
    }


    private fun initUi(){
        _userAdapter = UserAdapter {id,view ->
            handleUserClick(id, view)
        }
        binding.apply {
            myToolbar.apply {
                icBack.myShow()
                tvHeaderUserName.myShow()
                this.profileImage.gone()
                tvHeaderUserName.text = "Group Members"
            }

            rvUserList.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = userAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }

            setListener()
        }
    }


    private fun getData() {
        if(!chatViewModel.isDataLoaded){
            chatViewModel.getGroupMemberDetails(chatRoomId)
            chatViewModel.getGroupInfo(chatRoomId)
            chatViewModel.setDataLoadedStatus(true)
        }
    }

    private fun FragmentGroupMembersBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }

        linearBackToTop.setSafeOnClickListener {
            rvUserList.smoothScrollToPosition(0)
        }
        rvUserList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    MyLogger.v(tagChat, msg = "Idle State")
                    linearBackToTop.gone()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy < 0) {
                    MyLogger.v(tagChat, msg = "Up scroll")
                    showBackToTopView()
                } else {
                    MyLogger.v(tagChat, msg = "Down scroll")
                    linearBackToTop.gone()
                }
            }
        })
    }

    private fun updateGroupMemberData(it: List<GroupMemberDetails>) {
        groupMembersList.clear()
        groupMembers.clear()
        groupMembersList.addAll(
            it.map {
                it.member
            }
        )
        groupMembers.addAll(it.map {
            FriendCircleData(
                userId =it.member.memberId,
                user = it.memberInfo ,
                timeStamp = it.member.groupJoiningDateInTimeStamp
            )
        })

        if(groupMembers.isEmpty()){
            showNoDataView()
        }else{
            hideNoDataView()
            userAdapter.submitList(groupMembers)
            handleGroupInfo(groupInfo)
        }
    }

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@GroupMembersFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun handleUserClick(id: String, view: View) {
        if (id == AuthManager.currentUserId() !!) return

        currentUserId = id

        // Pop up menu take two thing first one context and second  is in which view is parent so it adjust size accordingly
        val layoutInflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val bindingPopUp = PopUpGroupMemberActivityBinding.inflate(layoutInflater)
        val popUp = PopupWindow(context)
        popUp.contentView = bindingPopUp.root
        popUp.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popUp.isFocusable = true
        popUp.isOutsideTouchable = true
        popUp.setBackgroundDrawable(ColorDrawable())
        popUp.animationStyle = R.style.popup_window_animation
        popUp.showAsDropDown(view)

        bindingPopUp.apply {
            if (isCreatorOfGroup(id)){
                tvMakeAdmin.gone()
                viewBelowMakeAdmin.gone()
                tvDismissAsAdmin.gone()
                viewBelowDismissAsAdmin.gone()
                tvRemoveUser.gone()
                viewBelowRemoveUser.gone()
            }

            if (isAdminOfGroup(id)){
                tvMakeAdmin.gone()
                viewBelowMakeAdmin.gone()
                if (!(isCreatorOfGroup(AuthManager.currentUserId()!!) ||isAdminOfGroup(AuthManager.currentUserId()!!)) ){
                    tvDismissAsAdmin.gone()
                    viewBelowDismissAsAdmin.gone()
                    tvRemoveUser.gone()
                    viewBelowRemoveUser.gone()
                }
            } else{
                tvDismissAsAdmin.gone()
                viewBelowDismissAsAdmin.gone()
                if (!(isCreatorOfGroup(AuthManager.currentUserId()!!) ||isAdminOfGroup(AuthManager.currentUserId()!!)) ){
                    tvMakeAdmin.gone()
                    viewBelowMakeAdmin.gone()
                    tvRemoveUser.gone()
                    viewBelowRemoveUser.gone()
                }
            }
        }


        try {
            bindingPopUp.apply {

                tvMakeAdmin.setSafeOnClickListener {
                    defaultAlertDialogOption = GroupMemberScreenAlertDialogOption.MAKE_ADMIN
                    popUp.dismiss()
                    AlertDialog(
                        "Are you sure to make this member as admin ?",
                        this@GroupMembersFragment,
                        isForShowDelete = false
                    ).show(
                        childFragmentManager,
                        "MyAlertDialog"
                    )
                }
                tvDismissAsAdmin.setSafeOnClickListener {
                    defaultAlertDialogOption = GroupMemberScreenAlertDialogOption.DISMISS_AS_ADMIN
                    popUp.dismiss()
                    AlertDialog(
                        "Are you sure to dismiss this member as admin ?",
                        this@GroupMembersFragment,
                        isForShowDelete = false
                    ).show(
                        childFragmentManager,
                        "MyAlertDialog"
                    )

                }
                tvRemoveUser.setSafeOnClickListener {
                    defaultAlertDialogOption = GroupMemberScreenAlertDialogOption.REMOVE_USER
                    popUp.dismiss()
                    AlertDialog(
                        "Are you sure to remove this member from group ?",
                        this@GroupMembersFragment,
                        isForShowDelete = false
                    ).show(
                        childFragmentManager,
                        "MyAlertDialog"
                    )
                }
                tvSendMessage.setSafeOnClickListener {
                    popUp.dismiss()
                    navigateToChatScreen(id)
                }
                tvViewUser.setSafeOnClickListener {
                    popUp.dismiss()
                    navigateToProfileScreen(id)
                }

            }
        }catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun navigateToProfileScreen(id: String) {
        val directions: NavDirections = BottomNavigationBarDirections.actionGlobalProfileViewFragment(id)
        navController.safeNavigate(directions)
    }

    private fun navigateToChatScreen(id: String) {
        val directions: NavDirections = BottomNavigationBarDirections.actionGlobalChatFragment(id)
        navController.safeNavigate(directions)
    }

    private fun isCreatorOfGroup(id: String) : Boolean{
        return id == groupInfo?.creatorId
    }
    private fun isAdminOfGroup(id: String): Boolean{
        return groupInfo?.groupAdmins?.contains(id) ?: false
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

    override fun onResult(isYes: Boolean) {
        if (isYes) {
            when(defaultAlertDialogOption){
                GroupMemberScreenAlertDialogOption.MAKE_ADMIN -> {
                    performMakeAdmin()
                }
                GroupMemberScreenAlertDialogOption.REMOVE_USER -> {
                    performRemoveUser()
                }
                GroupMemberScreenAlertDialogOption.DISMISS_AS_ADMIN -> {
                    performDismissAsAdmin()
                }
            }
        }
    }

    private fun performMakeAdmin() {
        if (currentUserId==null) return

        MyLogger.d(tagChat , isFunctionCall = true)

        val message = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Info.type,
            senderId = AuthManager.currentUserId()!!,
            infoMessageType = Constants.InfoType.MakeAdmin.name,
            addedOrRemovedUserId = currentUserId
        )
        val lastMessage = GroupLastMessage(
            senderId = AuthManager.currentUserId()!!,
            messageType = Constants.MessageType.Info.type,
            infoMessageType = Constants.InfoType.MakeAdmin.name,
        )

        val updatedGroupAdmins = groupInfo?.groupAdmins?.toMutableList()
        updatedGroupAdmins?.add(currentUserId!!)
        val updatedGroupInfo = groupInfo?.copy(
            groupAdmins = updatedGroupAdmins
        )

        chatViewModel.sentGroupInfoMessage(
            message,
            lastMessage,
            chatRoomId,
            groupMembersList,
            action = Constants.InfoType.MakeAdmin,
            addedOrRemovedUserId = currentUserId,
            groupInfo = updatedGroupInfo
        )
    }
    private fun performDismissAsAdmin() {
        if (currentUserId==null) return


        MyLogger.d(tagChat , isFunctionCall = true)

        val message = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Info.type,
            senderId = AuthManager.currentUserId()!!,
            infoMessageType = Constants.InfoType.RemoveFromAdmin.name,
            addedOrRemovedUserId = currentUserId
        )
        val lastMessage = GroupLastMessage(
            senderId = AuthManager.currentUserId()!!,
            messageType = Constants.MessageType.Info.type,
            infoMessageType = Constants.InfoType.RemoveFromAdmin.name,
        )

        val updatedGroupAdmins = groupInfo?.groupAdmins?.toMutableList()
        updatedGroupAdmins?.remove(currentUserId!!)
        val updatedGroupInfo = groupInfo?.copy(
            groupAdmins = updatedGroupAdmins
        )

        chatViewModel.sentGroupInfoMessage(
            message,
            lastMessage,
            chatRoomId,
            groupMembersList,
            action = Constants.InfoType.RemoveFromAdmin,
            addedOrRemovedUserId = currentUserId,
            groupInfo = updatedGroupInfo
        )
    }

    private fun performRemoveUser() {
        if (currentUserId==null) return

        MyLogger.d(tagChat , isFunctionCall = true)
        val message = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Info.type,
            senderId = AuthManager.currentUserId()!!,
            infoMessageType = Constants.InfoType.MemberRemoved.name,
            addedOrRemovedUserId = currentUserId ,
            text = groupMembers.find { it.userId==currentUserId }?.user?.userName
        )
        val lastMessage = GroupLastMessage(
            senderId = AuthManager.currentUserId()!!,
            messageType = Constants.MessageType.Info.type,
            infoMessageType = Constants.InfoType.MemberRemoved.name,
        )

        val updatedGroupAdmins = groupInfo?.groupAdmins?.toMutableList()
        updatedGroupAdmins?.remove(currentUserId !!)
        val updatedGroupInfo = groupInfo?.copy(
            groupAdmins = updatedGroupAdmins
        )

            chatViewModel.sentGroupInfoMessage(
                message,
                lastMessage,
                chatRoomId,
                groupMembersList.filter { it.memberId != currentUserId },
                action = Constants.InfoType.MemberRemoved,
                addedOrRemovedUserId = currentUserId,
                groupInfo = updatedGroupInfo
            )
    }



    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvUserList.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvUserList.myShow()
        }
    }

    override fun onDestroyView() {
        _userAdapter=null
        _binding = null
        super.onDestroyView()
    }


}

enum class GroupMemberScreenAlertDialogOption{
    MAKE_ADMIN,
    REMOVE_USER ,
    DISMISS_AS_ADMIN
}