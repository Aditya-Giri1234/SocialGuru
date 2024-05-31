package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.databinding.FragmentAddPostBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.AppBroadcastHelper
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.PostType
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.bufferWithDelay
import com.aditya.socialguru.domain_layer.helper.enabled
import com.aditya.socialguru.domain_layer.helper.getQueryTextChangeStateFlow
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.activity.ContainerActivity
import com.aditya.socialguru.ui_layer.viewmodel.AddPostViewModel
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch


class AddPostFragment : Fragment() {


    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private var interpolator: OvershootInterpolator = OvershootInterpolator()

    private val openFabMenu = 1
    private val translationY = 100f
    private val tagPost = Constants.LogTag.Post
    private val pref by lazy {
        SharePref(requireContext())
    }


    private val MAX_VIDEO_SIZE_MB = 50f

    private val myLoader = MyLoader()

    private val addPostViewModel: AddPostViewModel by viewModels()

    private val navController get() = (requireActivity() as ContainerActivity).navController

    val pickImageMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                MyLogger.v(tagPost, msg = "User select pic now send to server !")
                addPostViewModel.setImageUri(uri)
                binding.apply {
                    Glide.with(requireContext()).asBitmap().load(uri).into(ivImageCreatePost)
                    toggleContentVisibility(linearImage, fabImage, false)
                }

            } else {
                addPostViewModel.setImageUri(null)
                MyLogger.v(tagPost, msg = "User revoke image selection !")
            }
        }

    val pickVideoMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                val videoLength = Helper.getVideoSize(requireActivity(), uri)

                when {
                    videoLength == -1f -> {
                        MyLogger.e(
                            tagPost,
                            msg = "User selected video length is -1 means some error occurred !"
                        )
                        Helper.showSnackBar(
                            (requireActivity() as ContainerActivity).findViewById(
                                R.id.coordLayout
                            ),
                            "Some error occurred during video fetching  , may be file size to large!"
                        )
                    }

                    videoLength >= MAX_VIDEO_SIZE_MB -> {
                        MyLogger.e(
                            tagPost,
                            msg = "User selected video length is $videoLength and max lenght :- $MAX_VIDEO_SIZE_MB  which exceeded ! "
                        )
                        Helper.showSnackBar(
                            (requireActivity() as ContainerActivity).findViewById(
                                R.id.coordLayout
                            ), "Video length exceeded , max length is $MAX_VIDEO_SIZE_MB mb !"
                        )
                    }

                    else -> {
                        MyLogger.v(
                            tagPost,
                            msg = "User selected video length is $videoLength and max length :- $MAX_VIDEO_SIZE_MB  which is not exceeded 😁! "
                        )
                        addPostViewModel.setVideoUri(uri)
                        binding.apply {
                            Glide.with(requireContext()).asBitmap().load(uri)
                                .into(ivVideoCreatePost)
                            toggleContentVisibility(linearVideo, fabVideo, false)
                        }
                    }
                }

            } else {
                addPostViewModel.setVideoUri(null)
                MyLogger.w(tagPost, msg = "User revoke video selection !")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(tagPost, isFunctionCall = true)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddPostBinding.inflate(layoutInflater)
        MyLogger.v(tagPost, isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MyLogger.v(tagPost, isFunctionCall = true)
        handleInitialization()
    }

    private fun handleInitialization() {
        MyLogger.v(tagPost, isFunctionCall = true)
        initUi()
        subscribeToObserver()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun subscribeToObserver() {

        viewLifecycleOwner.lifecycleScope.launch {

            binding.etCreatePost.getQueryTextChangeStateFlow().debounce(100).distinctUntilChanged()
                .flatMapLatest {
                    if (it.startsWith(" ")) {
                        runOnUiThread {
                            binding.etCreatePost.setText(it.trim())
                        }
                    }
                    flow {
                        emit(it.trim())
                    }
                }.flowOn(Dispatchers.Default).collect {
                    runOnUiThread {
                        addPostViewModel.setText(it)
                        checkNowPostCanSend()
                    }
                }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            //buffer with delay use to delay for upcoming event due to when loader.show call it take some in millisecond the time duration between  loader.show and loader show that time duration between if some event come that event go to buffer for some time which i mention it.
            AppBroadcastHelper.uploadPost.bufferWithDelay(100).collect { response ->
                MyLogger.i(tagPost, msg = "Post Uploading event come ${response.state}")
                val progress = response.progress ?: 0
                val message: String? = when (response.state) {
                    Constants.PostUploadState.VideoUploading -> {
                        "Video Uploading"
                    }

                    Constants.PostUploadState.VideoUploaded -> {
                        "Video Uploaded"
                    }

                    Constants.PostUploadState.ImageUploading -> {
                        "Image Uploading"
                    }

                    Constants.PostUploadState.ImageUploaded -> {
                        "Image Uploaded"
                    }

                    Constants.PostUploadState.PostUploading -> {
                        "Post Uploading"
                    }

                    Constants.PostUploadState.PostUploaded -> {
                        resetUiScreen()
                        Helper.showSuccessSnackBar(
                            (requireActivity() as ContainerActivity).findViewById(
                                R.id.coordLayout
                            ),
                            "Post uploaded successfully !"
                        )

                        null
                    }

                    Constants.PostUploadState.Error -> {
                        Helper.showSnackBar(
                            (requireActivity() as ContainerActivity).findViewById(
                                R.id.coordLayout
                            ),
                            response.errorMessage.toString()
                        )
                        null
                    }
                }
                myLoader.updateUiByPostUpload(message, progress, message == null)

            }


        }



        addPostViewModel.uploadPost.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        //Do nothing here
                    }

                }

                is Resource.Loading -> {
                    MyLogger.v(tagPost, msg = "Post uploading now and now laoder is showing !")
                    myLoader.show(childFragmentManager, "My_Post_Uploading_Loader")
                }

                is Resource.Error -> {
                    MyLogger.e(
                        tagPost,
                        msg = "Some error occurred during post uploading :- ${response.message.toString()}"
                    )
                    myLoader.dismiss()
                    Helper.showSnackBar(
                        (requireActivity() as ContainerActivity).findViewById(
                            R.id.coordLayout
                        ),
                        response.message.toString()
                    )
                }
            }
        }

    }


    private fun initUi() {
        MyLogger.v(tagPost, isFunctionCall = true)
        /* ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

             val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
             v.setPadding(
                 0,
                 systemBars.top,
                 0,
                 0
             )
             insets
         }*/
        binding.apply {
            addPostViewModel.imageUri?.let {
                Glide.with(requireContext()).asBitmap().load(it).into(ivImageCreatePost)
                toggleContentVisibility(linearImage, fabImage, false)
            }
            addPostViewModel.videoUri?.let {
                Glide.with(requireContext()).asBitmap().load(it).into(ivVideoCreatePost)
                toggleContentVisibility(linearVideo, fabVideo, false)
            }
            etCreatePost.setText(addPostViewModel.text)
            setFab()
            setListener()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentAddPostBinding.setListener() {
        MyLogger.v(tagPost, isFunctionCall = true)

        constMain.setOnTouchListener { v, event ->
            MyLogger.v(tagPost, msg = "Now touch on const main view !")
            v.onTouchEvent(event)
            requireActivity().window.decorView.clearFocus()
            constMain.hideKeyboard()
            closeMenu()
            true
        }
        //Just for making edittext scrollable means pass touch event to edittext so that it consume
        etCreatePost.setOnTouchListener { v, event ->
            if (v.id == R.id.etCreatePost) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        fabMain.setOnClickListener {
            toggleFabMenu()
        }

        fabImage.setOnClickListener {
            MyLogger.v(tagPost, msg = "User click on image menu !")
            pickImageMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        fabVideo.setOnClickListener {
            MyLogger.v(tagPost, msg = "User click on video menu !")
            pickVideoMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }

        ivCancleImage.setOnClickListener {
            MyLogger.v(tagPost, msg = "User cancel image selection !")
            addPostViewModel.setImageUri(null)
            toggleContentVisibility(linearImage, fabImage, true)
        }
        ivCancleVideo.setOnClickListener {
            MyLogger.v(tagPost, msg = "User cancel video selection !")
            addPostViewModel.setVideoUri(null)
            toggleContentVisibility(linearVideo, fabVideo, true)
        }


        linearVideo.viewTreeObserver.addOnGlobalLayoutListener {
            MyLogger.v(tagPost, msg = "Video  layout is change !")
            checkNowPostCanSend()

        }
        linearImage.viewTreeObserver.addOnGlobalLayoutListener {
            MyLogger.v(tagPost, msg = "Image  layout  is change !")
            checkNowPostCanSend()
        }

        linearImage.setSafeOnClickListener {
            addPostViewModel.imageUri?.let {
                navigateToImageViewFragment(it)
            }
        }

        linearVideo.setSafeOnClickListener {
            addPostViewModel.videoUri?.let {
                navigateToVideoViewFragment(it)
            }
        }


        btnPost.setOnClickListener {
            MyLogger.v(tagPost, msg = "Post button is click !")
            uploadPost()
        }

        icClose.setOnClickListener {
            requireActivity().onBackPressed()
        }

    }

    private fun navigateToImageViewFragment(it: Uri) {
        val directions: NavDirections =
            AddPostFragmentDirections.actionAddPostFragmentToShowImageFragment(it)
        navController?.safeNavigate(directions, Helper.giveAnimationNavOption())

    }

    private fun navigateToVideoViewFragment(it: Uri) {
        val directions: NavDirections =
            AddPostFragmentDirections.actionAddPostFragmentToShowVideoFragment(it)
        navController?.safeNavigate(directions, Helper.giveAnimationNavOption())
    }

    private fun uploadPost() {
        MyLogger.d(tagPost, msg = "Image uri := ${addPostViewModel.imageUri}  -  Video uri := ${addPostViewModel.videoUri}")
        viewLifecycleOwner.lifecycleScope.launch {
            pref.getPrefUser().first()?.let { user ->
                val timeStamp = System.currentTimeMillis()
                val postId = Helper.getPostId()
                addPostViewModel.uploadPost(
                    Post(
                        postId = postId,
                        commentId = Helper.getCommentId(postId),
                        userId = user.userId,
                        postType = getPostType(),
                        text = binding.etCreatePost.text.toString(),
                        imageUrl = addPostViewModel.imageUri?.toString(),
                        videoUrl = addPostViewModel.videoUri?.toString(),
                        postUploadingTimeInText = Helper.formatTimestampToDateAndTime(timeStamp),
                        postUploadingTimeInTimeStamp = timeStamp
                    )
                )
            }
        }

    }

    private fun resetUiScreen() {
        binding.apply {
            addPostViewModel.setVideoUri(null)
            addPostViewModel.setImageUri(null)
            addPostViewModel.setText("")
            etCreatePost.text?.clear()
            closeMenu()  // This help to convert main fab tag value to 0 means close
            toggleContentVisibility(linearImage, fabImage, true)
            toggleContentVisibility(linearVideo, fabVideo, true)
        }
    }

    private fun getPostType(): String {
        binding.apply {
            val hasText = etCreatePost.text.toString().isNotEmpty()
            val hasImage = linearImage.isVisible
            val hasVideo = linearVideo.isVisible

            return when {
                hasText && !hasImage && !hasVideo -> PostType.OnlyText
                !hasText && hasImage && !hasVideo -> PostType.OnlyImage
                !hasText && !hasImage && hasVideo -> PostType.OnlyVideo
                hasText && hasImage && !hasVideo -> PostType.TextAndImage
                hasText && !hasImage && hasVideo -> PostType.TextAndVideo
                !hasText && hasImage && hasVideo -> PostType.ImageAndVideo
                hasText && hasImage && hasVideo -> PostType.All
                else -> PostType.OnlyText
            }.name
        }
    }


    private fun checkNowPostCanSend() {
        MyLogger.v(tagPost, isFunctionCall = true)

        //This condition used because this method can call after onDestroyView called
        if (_binding == null) return
        binding.apply {
            if (etCreatePost.text?.isNotEmpty() == true || linearImage.isVisible || linearVideo.isVisible) {
                makeButtonEnable()
            } else {
                makeButtonDisable()
            }
        }


    }

    private fun makeButtonDisable() {
        binding.apply {
            btnPost.setBackgroundColor(resources.getColor(R.color.lightBlack, null))
            btnPost.setTextColor(resources.getColor(R.color.lightWhite, null))
            btnPost.isEnabled = false
        }
    }

    private fun makeButtonEnable() {
        binding.apply {
            btnPost.setBackgroundColor(resources.getColor(R.color.deep_dark_orange, null))
            btnPost.setTextColor(resources.getColor(R.color.white, null))
            btnPost.enabled()
        }
    }


    //region:: Fab menu toggle visibility and animation section

    private fun setFab() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            fabImage.alpha = 0f
            fabVideo.alpha = 0f
            fabImage.translationY = translationY
            fabVideo.translationY = translationY
        }
    }

    private fun toggleFabMenu() {
        binding.apply {
            if (isMenuOpen()) {
                MyLogger.i(tagPost, msg = "Now close fab menu 😭")
                closeMenu()
            } else {
                MyLogger.i(tagPost, msg = "Now open fab menu 😎")
                openMenu()
            }
        }

    }

    /**
     *[validateFabHide] is check if both linear visible then hide main fab button other wise show.
     * */
    private fun validateFabHide() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            if (linearImage.isVisible && linearVideo.isVisible) {
                linearFab.hideMainFab()
            } else {
                linearFab.myShow()
            }
        }
    }

    private fun closeMenu() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            fabMain.tag = 0

            linearFab.background = ContextCompat.getDrawable(
                requireContext(),
                android.R.drawable.screen_background_dark_transparent
            )
            fabMain.animate().setInterpolator(interpolator).rotation(0f).setDuration(300)
                .start()

            fabImage.hideFab()
            fabVideo.hideFab()

        }
    }

    private fun openMenu() {
        MyLogger.v(tagPost, isFunctionCall = true)
        binding.apply {
            fabMain.tag = 1
            linearFab.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.shape_fab_bg)
            fabMain.animate().setInterpolator(interpolator).rotation(45f).setDuration(300)
                .start()
            if (linearImage.isGone) {
                MyLogger.d(tagPost, msg = "Image part is now showing !")
                fabImage.showFab()
            }
            if (linearVideo.isGone) {
                MyLogger.d(tagPost, msg = "Video part is now showing !")
                fabVideo.showFab()
            }
        }
    }

    private fun LinearLayout.hideMainFab() {
        closeMenu()
        gone()
    }

    private fun toggleContentVisibility(
        linearLayout: LinearLayout,
        fabButton: FloatingActionButton,
        isFabShowing: Boolean
    ) {
        if (isFabShowing) {
            linearLayout.gone()
            //Only if menu open then animation happen and other menu show
            if (isMenuOpen()) {
                MyLogger.v(tagPost, msg = "Menu is open !")
                if (linearLayout.isGone) {
                    fabButton.showFab()
                }
            } else {
                fabButton.myShow() // Only show
            }
        } else {
            linearLayout.myShow()
            fabButton.hideFab() // proper hide with animation
        }
        validateFabHide()
    }


    private fun FloatingActionButton.hideFab() {
        animate().translationY(translationY).alpha(0f)
            .setInterpolator(interpolator).setDuration(300).start()
        gone()
    }

    private fun FloatingActionButton.showFab() {
        animate().translationY(0f).alpha(1f).setInterpolator(interpolator)
            .setDuration(300).start()
        myShow()
    }

    private fun isMenuOpen(): Boolean = binding.fabMain.tag.toString().toInt() == openFabMenu

    //endregion

    override fun onDestroyView() {
        MyLogger.v(tagPost, isFunctionCall = true)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        MyLogger.v(tagPost, isFunctionCall = true)
    }

}