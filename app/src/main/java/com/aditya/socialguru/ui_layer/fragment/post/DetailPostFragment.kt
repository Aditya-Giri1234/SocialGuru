package com.aditya.socialguru.ui_layer.fragment.post

import android.content.Intent
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
import com.aditya.socialguru.ui_layer.activity.ContainerActivity
import com.aditya.socialguru.ui_layer.adapter.post.PostImageVideoAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.DetailPostViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class DetailPostFragment : Fragment() {

    private var _binding: FragmentDetailPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post

    private var myLoader: MyLoader? = null
    private lateinit var postId: String
    private lateinit var userId:String
    private val navController get() = (requireActivity() as MainActivity).navController?.value

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
        postId = args.postId
        initUi()
        subscribeToObserver()
        if (!detailPostViewModel.isDataLoaded) {
            getData()
            detailPostViewModel.setDataLoadedStatus(true)
        }
    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
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
            navController?.navigateUp()
        }

        ivPostUserImage.setSafeOnClickListener {
            navigateToProfileViewScreen()
        }
        tvPostUserName.setSafeOnClickListener {
            navigateToProfileViewScreen()
        }

        ivLike.setSafeOnClickListener {

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
                    userId=it
                }
                Glide.with(ivPostUserImage).load(it.userProfileImage).into(ivPostUserImage)
                tvPostUserName.text = it.userName
            }
            it.post?.let {

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

                tvPost.text=it.text
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

    private fun navigateToProfileViewScreen() {
            val directions:NavDirections=DetailPostFragmentDirections.actionDetailPostFragmentToProfileViewFragment2(userId)
        navController?.safeNavigate(directions,Helper.giveAnimationNavOption())
    }

    private fun onImageClick(imageUri: Uri) {
        Intent(requireActivity(),ContainerActivity::class.java).apply {
            putExtra(Constants.IntentTable.MediaUri.name,imageUri.toString())
            putExtra(Constants.IntentTable.FragmentNavigation.name,Constants.FragmentNavigation.ImageFragment.name)
            startActivity(this)
            requireActivity().overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }
    }

    private fun onVideoClick(videoUri: Uri) {
        Intent(requireActivity(),ContainerActivity::class.java).apply {
            putExtra(Constants.IntentTable.MediaUri.name,videoUri.toString())
            putExtra(Constants.IntentTable.FragmentNavigation.name,Constants.FragmentNavigation.VideoFragment.name)
            startActivity(this)
            requireActivity().overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        }
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