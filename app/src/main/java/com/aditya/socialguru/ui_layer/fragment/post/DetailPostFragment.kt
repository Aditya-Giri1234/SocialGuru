package com.aditya.socialguru.ui_layer.fragment.post

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.aditya.socialguru.data_layer.model.post.Comment
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentDetailPostBinding
import com.aditya.socialguru.domain_layer.custom_class.AlertDialog
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.chat.AttachmentDialog
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.Helper.observeFlow
import com.aditya.socialguru.domain_layer.helper.disabled
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.resizeActivate
import com.aditya.socialguru.domain_layer.helper.resizeStop
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption
import com.aditya.socialguru.domain_layer.remote_service.chat.ChatMessageOption
import com.aditya.socialguru.domain_layer.remote_service.chat.OnAttachmentItemListener
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.CommentAdapter
import com.aditya.socialguru.ui_layer.adapter.post.PostImageVideoAdapter
import com.aditya.socialguru.ui_layer.fragment.chat_fragment_helper.single_chat.ChatType
import com.aditya.socialguru.ui_layer.viewmodel.comment.CommentViewModel
import com.aditya.socialguru.ui_layer.viewmodel.post.DetailPostViewModel
import com.bumptech.glide.Glide
import com.vanniktech.emoji.EmojiPopup
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates


class DetailPostFragment : Fragment(), AlertDialogOption, ChatMessageOption,
    OnAttachmentItemListener {

    private var _binding: FragmentDetailPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post
    private val tagComment = Constants.LogTag.Comment

    private var myLoader: MyLoader? = null
    private var imageUri: String? = null
    private var videoUri: String? = null
    private lateinit var postId: String
    private lateinit var userId: String
    private lateinit var post: Post
    private var isCreatorOnline = false
    private var countExceptLoginUser by Delegates.notNull<Int>()

    private val imageNotLike = "0"
    private val imageLike = "1"

    private val MAX_VIDEO_SIZE_MB = 50f

    private val emojiKeyboardTag = 0
    private val emojiPopup by lazy {
        EmojiPopup(
            binding.root,
            binding.etMessage
        )
    }

    private var _currentDeletedComment: Comment? = null

    private var _commentAdapter: CommentAdapter? = null
    private val commentAdapter get() = _commentAdapter!!

    private val navController get() = (requireActivity() as MainActivity).navController

    private val detailPostViewModel by viewModels<DetailPostViewModel>()
    private val commentViewModel by viewModels<CommentViewModel>()
    private val args by navArgs<DetailPostFragmentArgs>()
    private val pref by lazy {
        SharePref(requireContext())
    }

    private val cameraIntent =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result) {
                commentViewModel.imageUri?.let { setImageOnAttachmentUi(it) }
            } else {
                MyLogger.v(tagComment, msg = "User cancel image capturing !")
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
                        MyLogger.v(tagComment, msg = "User selected an image! URI: $selectedUri")
                        setImageOnAttachmentUi(selectedUri)
                    } else if (it.startsWith("video/")) {
                        // It's a video
                        MyLogger.v(tagComment, msg = "User selected a video! URI: $selectedUri")
                        handleVideoSelection(selectedUri)
                    } else {
                        // Unsupported media type
                        MyLogger.v(
                            tagComment,
                            msg = "User selected media with unsupported type! URI: $selectedUri"
                        )
                        // Handle unsupported media type
                    }
                } ?: run {
                    // MIME type could not be determined
                    MyLogger.v(
                        tagComment,
                        msg = "Could not determine MIME type for selected media! URI: $selectedUri"
                    )
                }
            } ?: run {
                // URI is null, user cancelled or selection failed
                MyLogger.v(tagComment, msg = "User cancelled media selection or selection failed!")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentDetailPostBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        MyLogger.w(tagPost, msg = "Post Id := ${args.postId}")
        postId = args.postId
        requireActivity().resizeActivate()
        initUi()
        subscribeToObserver()
        getData()

    }

    private fun subscribeToObserver() {
        observeFlow {
            detailPostViewModel.postDetail.onEach { response ->

                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.data?.let {
                            setData(it)
                        } ?: run {
                            isCreatorOnline = false
                            showNoDataView()
                        }
                        if (!commentViewModel.isDataLoaded) {
                            commentViewModel.getCommenters(postId)
                            commentViewModel.getComments(postId)
                            commentViewModel.setDataLoadedStatus(true)
                        }
                    }

                    is Resource.Loading -> {
                        showDialog()
                        MyLogger.v(tagPost, msg = "Post Detail is fetching ...")
                    }

                    is Resource.Error -> {
                        hideDialog()
                        showNoDataView()
                        showSnackBar(response.message, false)
                    }
                }

            }.launchIn(this)
            detailPostViewModel.likePost.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.hasBeenMessagedToUser = true
                    }

                    is Resource.Loading -> {

                    }

                    is Resource.Error -> {
                        response.hasBeenMessagedToUser = true
                        resetToPreviousIconOnLike()
                        Helper.showSnackBar(
                            (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                            response.message.toString()
                        )
                    }
                }
            }.launchIn(this)
            commentViewModel.sendComment.onEach { response ->
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
            commentViewModel.deleteComment.onEach { response ->
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
            commentViewModel.getComments.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            setComment(it)
                        } ?: run {
                            showNoCommentView()
                        }
                    }

                    is Resource.Loading -> {
                    }

                    is Resource.Error -> {
                        showSnackBar(response.message)
                    }
                }

            }.launchIn(this)

            commentViewModel.getCommenters.onEach { response ->

                when (response) {
                    is Resource.Success -> {
                        response.data?.let {
                            commentAdapter.submitUser(
                                it.map {
                                    it.userId!! to it
                                }.toMap()
                            )
                        }
                    }

                    is Resource.Loading -> {

                    }

                    is Resource.Error -> {

                    }
                }
            }.launchIn(this)
            detailPostViewModel.savePost.onEach { response ->
                when (response) {
                    is Resource.Success -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(
                            response.data?.errorMessage, isSuccess =
                            true
                        )
                    }

                    is Resource.Loading -> {
                        showDialog()
                    }

                    is Resource.Error -> {
                        hideDialog()
                        response.hasBeenMessagedToUser = true
                        showSnackBar(response.message)
                    }

                }
            }.launchIn(this)
            AppBroadcastHelper.savedPost.onEach {
                val isISavedThisPost = it.any { it.postId == postId }
                binding.icSave.setImageResource(
                    if (isISavedThisPost) {
                        R.drawable.ic_save
                    } else {
                        R.drawable.ic_un_save
                    }
                )
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
        _commentAdapter = CommentAdapter(this@DetailPostFragment)
        binding.apply {
            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text = "Post"
            }

            commentViewModel.imageUri?.apply {
                setImageOnAttachmentUi(this)
            }
            commentViewModel.videoUri?.apply {
                setVideoOnAttachmentUi(this)
            }
            rvComment.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = commentAdapter
                isMotionEventSplittingEnabled = false
            }

            setListener()
        }
    }

    private fun FragmentDetailPostBinding.setListener() {
        myToolbar.icBack.setSafeOnClickListener {
            navController.navigateUp()
        }

        ivPostUserImage.setSafeOnClickListener {
            navigateToProfileViewScreen()
        }
        tvPostUserName.setSafeOnClickListener {
            navigateToProfileViewScreen()
        }

        tvPost.setSafeOnClickListener {
            navigateToLikeUserScreen()
        }

        ivLike.setSafeOnClickListener {
            var isLiked = post.likedUserList?.contains(AuthManager.currentUserId()!!) ?: false
            isLiked = !isLiked
            updateTagOnLike(isLiked)
            post.run {
                detailPostViewModel.updateLikeCount(postId!!, userId!!, isLiked)
            }
        }

        tvLike.setSafeOnClickListener {
            navigateToLikeUserScreen()
        }

        ivComment.setSafeOnClickListener {
            // Do nothing
        }

        ivSend.setSafeOnClickListener {
            ShareManager.sharePost(requireContext(),postId)
        }

        icSave.setSafeOnClickListener {
            detailPostViewModel.updatePostSaveStatus(postId)
        }

        linearBackToTop.setSafeOnClickListener {
            binding.nestedScroll.smoothScrollTo(0, 0)
        }

        nestedScroll.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                //Scroll Down
                linearBackToTop.gone()

            }
            if (scrollY < oldScrollY) {
                //Scroll Up
                showBackToTopView()

            }

            if (scrollY == 0) {
                //Top Scroll or No Scroll
                linearBackToTop.gone()
            }

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

        etMessage.setOnTouchListener { v, event ->
            MyLogger.v(tagComment, msg = "Now user touch on edit status view !")
            v.onTouchEvent(event)
            emojiPopup.dismiss()
            true
        }


        btnSend.setSafeOnClickListener {
            sendMessage()
        }

        icAttachment.setSafeOnClickListener {
            AttachmentDialog(this@DetailPostFragment).show(
                childFragmentManager,
                "My_Attchment_Dialog"
            )
        }

        cardImageMessage.setSafeOnClickListener {
            navigateToImageViewScreen(commentViewModel.imageUri)
        }
        cardVideoMessage.setSafeOnClickListener {
            navigateToVideoViewScreen(commentViewModel.videoUri)
        }

    }


    private fun getData() {
        if (!detailPostViewModel.isDataLoaded) {
            detailPostViewModel.getPostById(postId)
            detailPostViewModel.setDataLoadedStatus(true)
        }
    }

    private fun setData(it: UserPostModel) {
        hideNoDataView()
        binding.apply {
            it.user?.let {
                it.userId?.let {
                    userId = it
                }
                if (it.userProfileImage==null){
                    ivPostUserImage.gone()
                    tvInitialMain.myShow()
                    tvInitialMain.text = it.userName?.get(0).toString()
                    tvInitialMain.setCircularBackground(Helper.setUserProfileColor(it))
                }else{
                    ivPostUserImage.myShow()
                    tvInitialMain.gone()
                    Glide.with(ivPostUserImage).load(it.userProfileImage).into(ivPostUserImage)
                }
                tvPostUserName.text = it.userName
            }
            it.post?.let {
                post = it
                isCreatorOnline = it.isCreatorOnline ?: false

                val postImageVideoModel: List<PostImageVideoModel>? = when (it.postType) {
                    Constants.PostType.OnlyText.name -> {
                        constMedia.gone()
                        null
                    }

                    Constants.PostType.OnlyImage.name -> {
                        dotsIndicator.gone()
                        tvPost.gone()
                        listOf(
                            PostImageVideoModel(
                                it.imageUrl, true
                            )
                        )

                    }

                    Constants.PostType.OnlyVideo.name -> {
                        dotsIndicator.gone()
                        tvPost.gone()
                        listOf(
                            PostImageVideoModel(
                                it.videoUrl, false
                            )
                        )
                    }

                    Constants.PostType.TextAndImage.name -> {
                        dotsIndicator.gone()
                        listOf(
                            PostImageVideoModel(
                                it.imageUrl, true
                            )
                        )
                    }

                    Constants.PostType.TextAndVideo.name -> {
                        dotsIndicator.gone()
                        listOf(
                            PostImageVideoModel(
                                it.videoUrl, false
                            )
                        )
                    }

                    Constants.PostType.ImageAndVideo.name -> {
                        tvPost.gone()
                        listOf(
                            PostImageVideoModel(
                                it.imageUrl, true
                            ),
                            PostImageVideoModel(
                                it.videoUrl, false
                            )
                        )
                    }

                    Constants.PostType.All.name -> {
                        listOf(
                            PostImageVideoModel(
                                it.imageUrl, true
                            ),
                            PostImageVideoModel(
                                it.videoUrl, false
                            )
                        )
                    }

                    else -> {
                        null
                    }
                }

                if (postImageVideoModel != null) {
                    val viewPagerAdapter = PostImageVideoAdapter(
                        onImageClick = {
                            onImageClick(it)
                        },
                        onVideoClick = {
                            onVideoClick(it)
                        }
                    )
                    viewPagerPost.adapter = viewPagerAdapter
                    dotsIndicator.attachTo(viewPagerPost)

                    viewPagerAdapter.submitList(postImageVideoModel)
                }

                val isLiked =
                    it.likedUserList?.contains(AuthManager.currentUserId()!!) ?: false

                ivLike.setImageResource(if (isLiked) R.drawable.like else R.drawable.not_like)

                // This is for when click like button then result show as soon as possible so that below calculation help to fast calculation
                countExceptLoginUser =
                    if (isLiked) (it.likeCount?.let { it - 1 } ?: 0) else it.likeCount ?: 0
                tvPost.text = it.text
                tvLike.text = "${it.likeCount} Likes"
                tvComment.text = "${it.commentCount} Comments"

                // set time
                if (it.postUploadingTimeInTimeStamp != null) {
                    tvPostTime.myShow()
                    tvPostTime.text =
                        Helper.getTimeForPostAndComment(it.postUploadingTimeInTimeStamp)
                } else {
                    tvPostTime.gone()
                }

            }
        }
    }

    private fun setComment(comments: List<Comment>) {
        if (comments.isEmpty()) {
            showNoCommentView()
        } else {
            hideNoCommentView()
        }
        commentAdapter.submitList(comments)
    }

    private fun sendMessage() {
        lifecycleScope.launch {
            imageUri = commentViewModel.imageUri?.toString()
            videoUri = commentViewModel.videoUri?.toString()
            val message = binding.etMessage.text.toString()

            val timeStamp = System.currentTimeMillis()
            val timeInText = Helper.formatTimestampToDateAndTime(timeStamp)
            val chatType = getChatType(message)
            val comment = Comment(
                commentId = Helper.getCommentId(postId),
                commentType = chatType,
                text = message,
                imageUri = imageUri,
                videoUri = videoUri,
                userId = AuthManager.currentUserId()!!,
                commentUploadingTimeInTimestamp = timeStamp,
                commentUploadingTimeInText = timeInText,
                postId = postId,
                postCreatorId = post.postId,
                userName = pref.getPrefUser().first()?.userName,

                )

            commentViewModel.sendMessage(
                comment, isCreatorOnline
            ) {
                runOnUiThread {
                    resetUiScreen()
                }
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

    private fun showBackToTopView() {
        binding.linearBackToTop.myShow()

        Handler(Looper.getMainLooper()).postDelayed({
            if (this@DetailPostFragment.isResumed) {
                binding.linearBackToTop.gone()
            }
        }, 2000)

    }

    private fun setImageOnAttachmentUi(data: Uri) {
        commentViewModel.setImageUriData(data)
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
                commentViewModel.setImageUriData(null)
                cardImageMessage.gone()
                hideLinearAttachmentLayout()
            }
        }
        setSendButtonState()
    }

    private fun setVideoOnAttachmentUi(data: Uri) {
        commentViewModel.setVideoUriData(data)
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
                commentViewModel.setVideoUriData(null)
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

    private fun setSendButtonState() {
        binding.apply {
            if (etMessage.text.isNotBlank() || !linearAttachment.isGone) {
                btnSend.enabled()
            } else {
                btnSend.disabled()
            }
        }
    }

    private fun getFileUri(): Uri {
        val folderName = "picFromCamera"
        val folder = File(requireContext().cacheDir, folderName)

        // Check if the directory exists and attempt to delete it
        if (folder.exists()) {
            val isDirDeleted = folder.deleteRecursively() // Deletes the directory and its contents
            MyLogger.v(tagComment, msg = "Pic Directory deleted: $isDirDeleted")
            if (!isDirDeleted) {
                MyLogger.e(tagComment, msg = "Failed to delete Pic Directory")
            }
        }

        // Attempt to create the directory
        val isDirCreated = folder.mkdirs()
        MyLogger.v(tagComment, msg = "File created: $isDirCreated")

        if (!isDirCreated) {
            MyLogger.e(tagComment, msg = "Failed to create Pic Directory")
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
                    tagComment,
                    msg = "User selected video length is -1 means some error occurred !"
                )
                showSnackBar("Some error occurred during video fetching  , may be file size to large!")
            }

            videoLength >= MAX_VIDEO_SIZE_MB -> {
                MyLogger.e(
                    tagComment,
                    msg = "User selected video length is $videoLength and max lenght :- $MAX_VIDEO_SIZE_MB  which exceeded ! "
                )
                showSnackBar("User selected video length is $videoLength and max lenght :- $MAX_VIDEO_SIZE_MB  which exceeded ! ")
            }

            else -> {
                MyLogger.v(
                    tagComment,
                    msg = "User selected video length is $videoLength and max length :- $MAX_VIDEO_SIZE_MB  which is not exceeded 😁! "
                )
                setVideoOnAttachmentUi(uri)
            }
        }

    }

    private fun hideMediaPanel() {
        binding.apply {
            commentViewModel.setImageUriData(null)
            commentViewModel.setVideoUriData(null)
            cardImageMessage.gone()
            cardVideoMessage.gone()
            hideLinearAttachmentLayout()
        }
    }

    private fun resetUiScreen() {
        hideDialog()
        binding.etMessage.text.clear()
        hideMediaPanel()
        binding.linearBackToTop.gone()
    }


    private fun resetToPreviousIconOnLike() {
        binding.apply {
            val tag = ivLike.tag
            when (tag) {
                imageLike -> {
                    updateTagOnLike(false)
                }

                imageNotLike -> {
                    updateTagOnLike(true)
                }
            }
        }
    }

    private fun updateTagOnLike(isLike: Boolean) {
        binding.apply {
            ivLike.tag = if (isLike) {
                imageLike
            } else {
                imageNotLike
            }
        }
        updateImageIconInLike()
    }

    private fun updateImageIconInLike() {
        binding.apply {
            val tempCount = when (ivLike.tag) {
                imageLike -> {
                    ivLike.setImageResource(R.drawable.like)
                    countExceptLoginUser + 1
                }

                imageNotLike -> {
                    ivLike.setImageResource(R.drawable.not_like)
                    countExceptLoginUser
                }

                else -> {
                    countExceptLoginUser
                }
            }

            tvLike.text = "${tempCount} Likes"

        }
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

    private fun navigateToLikeUserScreen() {
        navController.safeNavigate(
            R.id.detailPostFragment,
            R.id.userLikeLIstFragment,
            Helper.giveAnimationNavOption(),
            UserLikeLIstFragmentArgs(postId).toBundle()
        )
    }

    private fun navigateToProfileViewScreen() {
        val directions: NavDirections =
            DetailPostFragmentDirections.actionDetailPostFragment2ToProfileViewFragment3(userId)
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun navigateToProfileViewScreen(userId: String) {
        val directions: NavDirections =
            BottomNavigationBarDirections.actionGlobalProfileViewFragment(userId)
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun onImageClick(imageUri: Uri) {
        val directions: NavDirections =
            DetailPostFragmentDirections.actionDetailPostFragment2ToShowImageFragment(imageUri)
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun onVideoClick(videoUri: Uri) {
        val directions: NavDirections =
            DetailPostFragmentDirections.actionDetailPostFragment2ToShowVideoFragment(videoUri)
        navController.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun showNoDataView() {
        binding.apply {
            linearParent.gone()
            noDataView.myShow()
            linearFooter.gone()
            initialHideCommentView()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            linearParent.myShow()
            noDataView.gone()
            linearFooter.myShow()
            initialShowCommentView()
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

    private fun showLoader() {
        binding.apply {
            rvComment.gone()
            noCommentDataView.gone()
            progressBar.myShow()
        }
    }

    private fun hideLoader() {
        binding.apply {
            progressBar.gone()
        }
    }

    private fun initialHideCommentView() {
        binding.apply {
            noCommentDataView.gone()
            progressBar.gone()
            rvComment.gone()
        }
    }

    private fun initialShowCommentView() {
        binding.apply {

            // This is for when data is loaded and user perform ( add comment or remove comment which trigger post listener and event come in viewmodel and call this method which now to hide or show noCommentDataView base on how much item present . And one more thing this process might go fast and adapter may slow to change list this why i give some delay.
            lifecycleScope.launch {
                delay(100)
                noCommentDataView.isGone = commentAdapter.itemCount != 0
            }
        }
    }

    private fun showNoCommentView() {
        binding.apply {
            progressBar.gone()
            rvComment.gone()
            noCommentDataView.myShow()
        }
    }

    private fun hideNoCommentView() {
        binding.apply {
            progressBar.gone()
            rvComment.myShow()
            noCommentDataView.gone()
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
        if (isYes) {
            _currentDeletedComment?.let {
                commentViewModel.deleteMessage(
                    it
                )
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
        navigateToProfileViewScreen((message as Comment).userId!!)
    }

    override fun <T> onLongMessageClick(message: T) {
        val tempDelete = message as Comment
        if (tempDelete.userId == AuthManager.currentUserId()) {
            _currentDeletedComment = tempDelete
            AlertDialog("Are you sure delete this Comment ?", this, true).show(
                childFragmentManager,
                "My_Delete_Dialog"
            )
        }
    }

    override fun onProfileClick() {
        // Don't do any thing
    }

    override fun onAttachmentImageClick() {
        commentViewModel.setImageUriData(getFileUri())
        MyLogger.i(tagComment, msg = "Image uri for saving pic :- ${commentViewModel.imageUri}")
        cameraIntent.launch(commentViewModel.imageUri)
    }

    override fun onAttachmentGalleryClick() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
    }

    override fun onResume() {
        updateOnlineStatusOfCreatorOnPost(true)
        super.onResume()
    }

    private fun updateOnlineStatusOfCreatorOnPost(status: Boolean) {
        commentViewModel.updateMyOnlineStatus(
            postId, status, if (::post.isInitialized) {
                post
            } else {
                null
            }
        )

    }

    override fun onStop() {
        updateOnlineStatusOfCreatorOnPost(false)
        super.onStop()
    }

    override fun onDestroyView() {
        requireActivity().resizeStop()
        commentViewModel.removeAllListener()
        detailPostViewModel.removeAllListener()
        _binding = null
        super.onDestroyView()
    }

}