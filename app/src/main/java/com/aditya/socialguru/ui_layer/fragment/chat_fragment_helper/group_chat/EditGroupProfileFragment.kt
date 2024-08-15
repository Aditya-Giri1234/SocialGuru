package com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.group_chat

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.data_layer.model.chat.group.GroupLastMessage
import com.aditya.socialguru.data_layer.model.chat.group.GroupMembersList
import com.aditya.socialguru.data_layer.model.chat.group.GroupMessage
import com.aditya.socialguru.databinding.FragmentEditGroupProfileBinding
import com.aditya.socialguru.databinding.FragmentGroupProfileBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.ProfilePicEditDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.profile.ProfilePicEditOption
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.viewmodel.chat.ChatViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class EditGroupProfileFragment : Fragment() , ProfilePicEditOption {
    private var _binding:FragmentEditGroupProfileBinding? = null
    private val binding get() = _binding!!
    private val tagChat = Constants.LogTag.Chats
    private val imageAvailable = "0"
    private val imageUnAvailable = "1"

    private lateinit var chatRoomId: String
    private lateinit var groupInfo: GroupInfo
    private lateinit var groupMembers:GroupMembersList

    private var myLoader: MyLoader? = null
    private var currentImage: String? = null
    private var newImage: String? = null

    private val args by navArgs<EditGroupProfileFragmentArgs>()
    private val chatViewModel by viewModels<ChatViewModel>()


    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            MyLogger.v(tagChat, msg = "User select pic now set to profile :- $uri")

            setImageOnProfileView(uri)

        } else {
            MyLogger.v(tagChat, msg = "User revoke or cancel upload story !")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatRoomId = args.chatRoomId
        groupInfo=args.groupInfo
        groupMembers=args.groupMembers
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditGroupProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
    }


    private fun subscribeToObserver() {
        observeFlow {
            chatViewModel.updateGroupDetails.onEach {response->
                when(response){
                    is Resource.Success -> {
                        hideDialog()
                        showSnackBar("Group Details Updated Successfully !" ,true)
                        navController.navigateUp()
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
        }
    }

    private fun initUi() {
        binding.apply {
            // this two property must be needed at the time of data binding
            lifecycleOwner = this@EditGroupProfileFragment
            chatViewModel = this@EditGroupProfileFragment.chatViewModel  // variable initiailization

            myToolbar.apply {
                icBack.myShow()
                tvHeaderUserName.myShow()
                this.profileImage.gone()
                this.icDone.myShow()
                tvHeaderUserName.text = "Edit Group Profile"
            }
            groupInfo.apply {
                groupPic?.let {
                    ivProfile.tag=imageAvailable
                    currentImage=it
                    Glide.with(ivProfile).load(it).into(ivProfile)
                } ?: run{
                    ivProfile.tag=imageUnAvailable
                }

                etGroupName.setText(groupName)
                etGroupDesc.setText(groupDescription)
            }
            setListener()
        }
    }



    private fun FragmentEditGroupProfileBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }

        myToolbar.icDone.setSafeOnClickListener {
            if(validateData()){
                updateGroupInfo()
            }
        }

        root.setOnTouchListener { v, event ->
            MyLogger.v(tagChat, msg = "Now touch on const main view !")
            v.onTouchEvent(event)
            requireActivity().window.decorView.clearFocus()
            root.hideKeyboard()
            true
        }

        ivProfile.setSafeOnClickListener {
            if (ivProfile.tag == imageAvailable) {
                Helper.showImageDialog(requireActivity(), ivProfile.getBitmapByDrawable())
            } else {
                Helper.showSnackBar(
                    (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                        R.id.coordLayout
                    ), "No Image Found !"
                )

            }

        }

        binding.icEdit.setSafeOnClickListener {
            if (currentImage == null) {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                ProfilePicEditDialog(this@EditGroupProfileFragment).show(
                    childFragmentManager,
                    "My_Profile_Pic_Edit_Dialog"
                )
            }
        }
    }

    private fun validateData(): Boolean {
        binding.apply {
            return when {
                etGroupName.text.isNullOrBlank()-> {
                    showSnackBar("Please Enter Group Name.")
                    false
                }
                else -> {
                    true
                }
            }
        }

    }

    private fun updateGroupInfo() {

        val message = GroupMessage(
            messageId = Helper.getMessageId(),
            messageType = Constants.MessageType.Info.type,
            senderId = AuthManager.currentUserId()!!,
            infoMessageType = Constants.InfoType.GroupCreated.name
        )
        val lastMessage = GroupLastMessage(
            senderId = AuthManager.currentUserId()!!,
            messageType = Constants.MessageType.Info.type,
            infoMessageType = Constants.InfoType.GroupCreated.name,
        )
        val updatedImage=if(isProfilePicDeleted()){
            newImage
        }else{
            currentImage
        }
        val updatedGroupInfo=groupInfo.copy(
            groupPic = updatedImage,
            groupDescription = binding.etGroupDesc.text.toString(),
            groupName = binding.etGroupName.text.toString(),
        )

        if(isProfilePicDeleted()){
            chatViewModel.updateGroupDetails(
                message,lastMessage,chatRoomId,groupMembers.members, groupInfo = updatedGroupInfo , deleteImage =currentImage , uploadingImage=newImage
            )
        }else{
            chatViewModel.updateGroupDetails(
                message,lastMessage,chatRoomId,groupMembers.members, groupInfo = updatedGroupInfo , uploadingImage=newImage
            )
        }


    }


    private fun setImageOnProfileView(uri: Uri) {
        newImage = uri.toString()
        Glide.with(binding.ivProfile).load(newImage).into(binding.ivProfile)
        binding.ivProfile.tag=imageAvailable
    }
    private fun removeImageFromProfileView(){
        binding.apply {
            newImage = null  //Assure newImage should null and currentImage not change
            ivProfile.setImageResource(R.drawable.ic_user)
            ivProfile.tag = imageUnAvailable
        }
    }

    private fun isProfilePicDeleted(): Boolean {
        if (currentImage == null) return false

        //In this point one thing clear current image is not null
        return newImage != null || binding.ivProfile.tag == imageUnAvailable
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
        _binding = null
        super.onDestroyView()
    }


}