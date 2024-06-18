package com.aditya.socialguru.ui_layer.fragment.post

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.data_layer.model.post.PostImageVideoModel
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentDetailPostBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.safeNavigate
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.aditya.socialguru.ui_layer.adapter.post.PostImageVideoAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.DetailPostViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


class DetailPostFragment : Fragment() {

    private var _binding: FragmentDetailPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post

    private var myLoader: MyLoader? = null
    private lateinit var postId: String
    private lateinit var userId: String
    private lateinit var post: Post
    private var countExceptLoginUser by Delegates.notNull<Int>()

    private val imageNotLike = "0"
    private val imageLike = "1"
    private val navController get() = (requireActivity() as MainActivity).navController

    private val detailPostViewModel by viewModels<DetailPostViewModel>()
    private val args by navArgs<DetailPostFragmentArgs>()


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
        initUi()
        subscribeToObserver()
        if (!detailPostViewModel.isDataLoaded) {
            getData()
            detailPostViewModel.setDataLoadedStatus(true)
        }
    }

    private fun subscribeToObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailPostViewModel.postDetail.onEach { response ->

                    when (response) {
                        is Resource.Success -> {
                            hideDialog()
                            response.data?.let {
                                setData(it)
                            } ?: run {
                                showNoDataView()
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
            }
        }
    }


    private fun initUi() {
        binding.apply {
            myToolbar.apply {
                icBack.myShow()
                profileImage.gone()
                tvHeaderUserName.text = "Post"
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
        }

        ivSend.setSafeOnClickListener {
        }

        ivSetting.setSafeOnClickListener {
        }

    }




    private fun getData() {
        detailPostViewModel.getPostById(postId)
    }

    private fun setData(it: UserPostModel) {
        hideNoDataView()
        binding.apply {
            it.user?.let {
                it.userId?.let {
                    userId = it
                }
                Glide.with(ivPostUserImage).load(it.userProfileImage).into(ivPostUserImage)
                tvPostUserName.text = it.userName
            }
            it.post?.let {
                post = it
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

            }
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

    private fun navigateToLikeUserScreen() {
    navController.safeNavigate(R.id.detailPostFragment,R.id.userLikeLIstFragment,Helper.giveAnimationNavOption(),UserLikeLIstFragmentArgs(postId).toBundle())
    }
    private fun navigateToProfileViewScreen() {
        val directions: NavDirections =
            DetailPostFragmentDirections.actionDetailPostFragment2ToProfileViewFragment3(userId)
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
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            linearParent.myShow()
            noDataView.gone()
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}