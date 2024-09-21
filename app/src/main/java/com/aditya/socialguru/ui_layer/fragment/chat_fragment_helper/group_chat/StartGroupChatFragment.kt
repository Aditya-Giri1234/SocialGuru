package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.group_chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.data_layer.model.Users
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMembersList
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.databinding.FragmentStartGroupChatBinding
import com.aditya.socialguru.databinding.SampleStartGroupChatUserChipBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.MembersAddType
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.monitorInternet
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.chat.StartChatAdapter
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class StartGroupChatFragment : Fragment() {


    private var _binding: FragmentStartGroupChatBinding? = null
    private val binding get() = _binding!!

    private val tagChat = Constants.LogTag.Chats

    private val allUser = mutableListOf<FriendCircleData>()
    private val selectedUser = mutableListOf<User>()
    private val notSelectedUser = mutableListOf<FriendCircleData>()
    private val jobQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private var isCreatorOfGroupIsRemoveApiCall = false
    private lateinit var memberAddType: String
    private var groupInfo: GroupInfo? = null
    private var groupAlreadyMembers: GroupMembersList? = null
    private var chatRoomId: String? = null

    private var _friendAdapter: StartChatAdapter? = null
    private val friendAdapter get() = _friendAdapter!!
    private var myLoader: MyLoader? = null

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val chatViewModel by viewModels<ChatViewModel>()
    private val args by navArgs<StartGroupChatFragmentArgs>()

    private val pref by lazy {
        SharePref(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memberAddType = args.type
        groupInfo = args.groupInfo
        groupAlreadyMembers = args.groupAlreadyMembers
        chatRoomId = args.chatRoomId
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentStartGroupChatBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        _friendAdapter = StartChatAdapter {
            onItemClick(it)
        }
        initUi()
        subscribeToObserver()
        getDataWithValidation()
    }


    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.friendList.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        MyLogger.i(tagChat, msg = "Friend List response come !")
                        MyLogger.d(
                            tagChat, msg = response.data, isJson = true, jsonTitle = "Friend List"
                        )
                        handleFriendList(response.data)
                    }

                    is Resource.Loading -> {
                        MyLogger.v(tagChat, msg = "Friend List is fetching ...")
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        MyLogger.e(
                            tagChat, msg = giveMeErrorMessage(
                                "Fetching Friend List", response.message.toString()
                            )
                        )

                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.clear()
                                    chatViewModel.setDataLoadedStatus(false)
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
            chatViewModel.userDetailsByIds.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        MyLogger.i(tagChat, msg = "Friend List response come !")
                        handleFriendList(response.data)
                    }

                    is Resource.Loading -> {
                        MyLogger.v(tagChat, msg = "Friend List is fetching ...")
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        MyLogger.e(
                            tagChat, msg = giveMeErrorMessage(
                                "Fetching Friend List", response.message.toString()
                            )
                        )

                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            when (response.message) {
                                Constants.ErrorMessage.InternetNotAvailable.message -> {
                                    jobQueue.clear()
                                    chatViewModel.setDataLoadedStatus(false)
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
            chatViewModel.sendGroupInfoMessage.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        MyLogger.i(tagChat, msg = "Add member or choose creator response come !")
                        if (memberAddType == MembersAddType.MembersAdd.name) {
                            showSnackBar("Members Added Successfully !", isSuccess = true)
                            navController.navigateUp()
                        } else {
                            if (isCreatorOfGroupIsRemoveApiCall) {
                                showSnackBar("Successfully Exit From Group!", isSuccess = true)
                                navController.navigateUp()
                            } else {
                                performExitUser()
                            }
                        }
                    }

                    is Resource.Loading -> {
                        showDialog()
                        MyLogger.v(tagChat, msg = "Add member or choose creator ongoing ...")
                    }

                    is Resource.Error -> {
                        hideDialog()
                        MyLogger.e(
                            tagChat, msg = giveMeErrorMessage(
                                "Add member or choose creator", response.message.toString()
                            )
                        )

                        if (!response.hasBeenMessagedToUser) {
                            response.hasBeenMessagedToUser = true
                            showSnackBar(response.message?.toString())
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
        }
    }


    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                profileImage.gone()
                icBack.myShow()
                tvHeaderUserName.text = getScreenTitle()
            }

            rvAllUsers.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = friendAdapter
                setHasFixedSize(true)
                isMotionEventSplittingEnabled = false
            }
            setListener()
        }

        checkAndShowSelectedUser()

    }

    private fun getScreenTitle(): String {
        return when (memberAddType) {
            MembersAddType.StartGroup.name -> {
                "Choose Friends"
            }

            MembersAddType.MembersAdd.name -> {
                binding.btnNext.setImageResource(R.drawable.ic_message_sent)
                "Add Friends"
            }

            MembersAddType.CreatorChoose.name -> {
                binding.btnNext.setImageResource(R.drawable.ic_message_sent)
                "Choose Creator"
            }

            else -> {
                "Choose Friends"
            }
        }
    }


    private fun FragmentStartGroupChatBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }
        btnNext.setOnClickListener {
            handleNextClick()
        }
    }

    /**
     * [checkAndShowSelectedUser] help me  when user navigate to other fragment then come back again then show previous selected user.
     * */
    private fun checkAndShowSelectedUser() {
        selectedUser.forEach {
            addIntoChip(it)
        }
    }

    private fun validateUserAndSetData(data: List<FriendCircleData>?) {
        allUser.clear()
        notSelectedUser.clear()
        if (data == null) {
            selectedUser.clear()
            setData()
            MyLogger.w(tagChat, msg = "Friend list is empty !")
        } else {
            allUser.addAll(data)
            selectedUser.forEachIndexed { index, user ->
                // Just remove view which is not a friend now
                if (data.none { it.user?.userId == user.userId }) {
                    binding.chipSelectedUsers.removeViewAt(index)
                    selectedUser.remove(user)
                }
            }
            notSelectedUser.addAll(
                allUser.filter { user ->
                    selectedUser.none { it.userId == user.user?.userId }
                }
            )
            setData(notSelectedUser)
        }
    }

    private fun handleFriendList(data: List<FriendCircleData>?) {
        if (data != null) {
            val validData = when (memberAddType) {
                Constants.MembersAddType.StartGroup.name -> {
                    // Show all friend list
                    data
                }

                Constants.MembersAddType.MembersAdd.name -> {
                    // Only show who is not participant of group
                    val alreadyMemberUserId =
                        groupAlreadyMembers?.members?.mapNotNull { it.memberId }
                    MyLogger.i(tagChat , msg = alreadyMemberUserId , isJson = true , jsonTitle = "Already Member Ids")
                    data.filter {
                        if (it.userId != null) {
                            (alreadyMemberUserId?.contains(it.userId) == false) ?: true
                        } else {
                            false
                        }
                    }
                }

                Constants.MembersAddType.CreatorChoose.name -> {
                    // Only show who is participant of group
                    val alreadyMemberUserId =
                        groupAlreadyMembers?.members?.mapNotNull { it.memberId }
                    data.filter {
                        if (it.userId != null) {
                            alreadyMemberUserId?.contains(it.userId) ?: false
                        } else {
                            false
                        }
                    }
                }

                else -> {
                    // As default case  go for new group
                    data
                }
            }
            validateUserAndSetData(validData)
        } else {
            validateUserAndSetData(data)
        }
    }

    private fun setData(
        list: List<FriendCircleData> = emptyList()
    ) {
        if (allUser.isEmpty()) {
            showNoDataViewForChip()
        }

        if (list.isEmpty()) {
            showNoDataView()
        } else {
            hideNoDataView()
            friendAdapter.submitList(list)
        }
    }

    private fun getDataWithValidation() {
        if (!chatViewModel.isDataLoaded) {
            getData()
            chatViewModel.setDataLoadedStatus(true)
        }
    }
    private fun getData(){
        if (memberAddType == MembersAddType.CreatorChoose.name) {
            chatViewModel.getAllUserByIds(groupAlreadyMembers!!.members.mapNotNull { it.memberId })
        } else {
            chatViewModel.getFriendListAndListenChange()
        }
    }

    private fun handleNextClick() {
        try {
            if (selectedUser.isEmpty()) {
                showSnackBar("At least 1 friend need to be selected !")
            } else {
                when (memberAddType) {
                    MembersAddType.StartGroup.name -> {
                        navigateToGroupMakingScreen()
                    }

                    MembersAddType.MembersAdd.name -> {
                        performAddMember()
                    }

                    MembersAddType.CreatorChoose.name -> {
                        performCreateChooser()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showSnackBar("Some error occurred , Please try after some time !")
        }

    }

    private fun performAddMember() {
        val message = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Info.type,
            senderId = AuthManager.currentUserId()!!,
            infoMessageType = Constants.InfoType.MemberAdded.name,
            newMembers = selectedUser.mapNotNull { it.userId } ,
            newMembersName = selectedUser.mapNotNull { it.userName }
        )
        val lastMessage = GroupLastMessage(
            senderId = AuthManager.currentUserId()!!,
            messageType = Constants.MessageType.Info.type,
            infoMessageType = Constants.InfoType.MemberAdded.name
        )


        chatViewModel.sentGroupInfoMessage(
            message,
            lastMessage,
            chatRoomId!!,
            groupAlreadyMembers!!.members,
            action = Constants.InfoType.MemberAdded,
            groupInfo = groupInfo,
            newMembers = selectedUser.mapNotNull { it.userId }
        )
    }

    private fun performCreateChooser() {
        val message = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Info.type,
            senderId = AuthManager.currentUserId()!!,
            infoMessageType = Constants.InfoType.NewGroupCreator.name,
            addedOrRemovedUserId = selectedUser[0].userId
        )
        val lastMessage = GroupLastMessage(
            senderId = AuthManager.currentUserId()!!,
            messageType = Constants.MessageType.Info.type,
            infoMessageType = Constants.InfoType.GroupDetailsChanged.name,
        )

        val updatedGroupInfo = groupInfo?.copy(
            creatorId = selectedUser[0].userId
        )
        chatViewModel.sentGroupInfoMessage(
            message,
            lastMessage,
            chatRoomId!!,
            groupAlreadyMembers!!.members,
            action = Constants.InfoType.NewGroupCreator,
            groupInfo = updatedGroupInfo,
        )
    }

    private fun performExitUser() {
        isCreatorOfGroupIsRemoveApiCall=true
        lifecycleScope.launch {
            val message = GroupMessage(
                messageId = Helper.getMessageId(),
                messageType = Constants.MessageType.Info.type,
                senderId = AuthManager.currentUserId()!!,
                infoMessageType = Constants.InfoType.MemberExit.name,
                text = pref.getPrefUser().first()?.userName
            )
            val lastMessage = GroupLastMessage(
                senderId = AuthManager.currentUserId()!!,
                messageType = Constants.MessageType.Info.type,
                infoMessageType = Constants.InfoType.MemberExit.name,
            )

            chatViewModel.sentGroupInfoMessage(
                message,
                lastMessage,
                chatRoomId!!,
                groupAlreadyMembers!!.members.filter { it.memberId !=AuthManager.currentUserId()!! },
                action = Constants.InfoType.MemberExit,
                addedOrRemovedUserId = AuthManager.currentUserId()!!
            )
        }

    }

    private fun navigateToGroupMakingScreen() {
        val directions: NavDirections =
            StartGroupChatFragmentDirections.actionStartGroupChatFragmentToMakingGroupFragment(
                Users(selectedUser)
            )
        navController.navigate(directions, Helper.giveAnimationNavOption())
    }

    private fun onItemClick(user: User) {
        if (memberAddType == MembersAddType.CreatorChoose.name) {
            if (selectedUser.size == 1) {
                showSnackBar("Only one friend can be chosen as creator !")
                return
            }
        }

        selectedUser.add(user)
        notSelectedUser.remove(allUser.find { it.user?.userId == user.userId })
        addIntoChip(user)
    }

    private fun addIntoChip(user: User) {
        val chipBinding = SampleStartGroupChatUserChipBinding.inflate(layoutInflater)
        chipBinding.apply {
            chipText.text = user.userName
            if (user.userProfileImage==null){
                tvInitial.myShow()
                tvInitial.text = user.userName?.get(0).toString()
                tvInitial.setCircularBackground(Helper.setUserProfileColor(user))
            }else{
                tvInitial.gone()
                Glide.with(circularImage).load(user.userProfileImage).placeholder(R.drawable.ic_user).error(
                    R.drawable.ic_user
                ).into(circularImage)
            }
            binding.chipSelectedUsers.addView(this.root)
            closeIcon.setOnClickListener {
                selectedUser.remove(user)
                allUser.find { it.user?.userId == user.userId }
                    ?.let { it1 -> notSelectedUser.add(it1) }
                binding.chipSelectedUsers.removeView(this.root)
                updateSelectedView()
            }
        }
        updateSelectedView()
    }

    private fun updateSelectedView() {
        binding.apply {
            if (selectedUser.isEmpty()) {
                showNoDataViewForChip()
            } else {
                hideNoDataViewForChip()
            }
        }
        setData(notSelectedUser)
    }

    private fun showNoDataViewForChip() {
        binding.apply {
            chipSelectedUsers.removeAllViews()
            scrollChipGroup.gone()
            tvNoDataViewForSelectedUser.myShow()
        }
    }

    private fun hideNoDataViewForChip() {
        binding.apply {
            scrollChipGroup.myShow()
            tvNoDataViewForSelectedUser.gone()
        }
    }

    private fun showNoDataView() {
        binding.apply {
            rvAllUsers.gone()
            tvNoDataViewForAllUser.myShow()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            rvAllUsers.myShow()
            tvNoDataViewForAllUser.gone()
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
        _friendAdapter = null
        _binding = null
        super.onDestroyView()
    }


}