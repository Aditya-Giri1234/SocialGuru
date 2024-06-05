package com.aditya.socialguru.ui_layer.fragment.home_tab_layout

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentHomeFollowingPostBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.ui_layer.adapter.post.PostAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.FollowingPostViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class HomeFollowingPostFragment : Fragment(), OnPostClick {
    private var _binding: FragmentHomeFollowingPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post

    private var _followingPostAdapter:PostAdapter?=null
    private val followingPostAdapter get() = _followingPostAdapter!!


    private val followingPostViewModel: FollowingPostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeFollowingPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()

        if (!followingPostViewModel.isDataLoaded) {
            getData()
            followingPostViewModel.setDataLoadedStatus(true)
        }
    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                followingPostViewModel.userPost.onEach { response ->
                    response.let {
                        MyLogger.d(msg = "Response coming and it was $response")
                        when (response) {
                            is Resource.Success -> {
                                response.hasBeenMessagedToUser = true
                                response.data?.let {
                                    setData(it)
                                } ?: run {
                                    setData()
                                    Helper.showSnackBar(
                                        (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                        response.message.toString()
                                    )
                                }

                            }

                            is Resource.Loading -> {

                            }

                            is Resource.Error -> {
                                response.hasBeenMessagedToUser = true
                                Helper.showSnackBar(
                                    (requireActivity() as MainActivity).findViewById(R.id.coordLayout),
                                    response.message.toString()
                                )
                            }
                        }
                    }

                }.launchIn(this)
            }

        }
    }

    private fun initUi() {
        _followingPostAdapter=PostAdapter(this@HomeFollowingPostFragment)
        binding.apply {
            rvFollowingPost.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = followingPostAdapter
            }
            setListener()
        }
    }


    private fun FragmentHomeFollowingPostBinding.setListener() {




    }


    private fun getData() {
        followingPostViewModel.getDiscoverPost()
    }

    private fun setData(userPosts: List<UserPostModel> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

        if (userPosts.isEmpty()) {
            MyLogger.w(tagPost, msg = "list is empty then show no data view !")
            showNoDataView()
        } else {
            MyLogger.v(msg = "Now data is set into homeFragment !")
            followingPostAdapter.submitList(userPosts.toList())
            hideNoDataView()
        }
    }

    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvFollowingPost.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvFollowingPost.myShow()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestLayout()
    }


    //region:: Override Part
    override fun onImageClick(): () -> Unit = {}

    override fun onVideoClick(): () -> Unit = {}

    override fun onLikeClick() {

    }

    override fun onCommentClick() {
    }

    override fun onSettingClick() {
    }

    override fun onSendClick() {
    }

    override fun onPostClick() {
    }

    //endregion

    override fun onDestroyView() {
        _followingPostAdapter=null
        binding.rvFollowingPost.adapter=null
        _binding = null
        super.onDestroyView()
    }
}