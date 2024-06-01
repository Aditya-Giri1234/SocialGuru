package com.aditya.socialguru.ui_layer.fragment.home_tab_layout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.databinding.FragmentHomeDiscoverPostBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.post.OnPostClick
import com.aditya.socialguru.ui_layer.adapter.post.DiscoverPostAdapter
import com.aditya.socialguru.ui_layer.viewmodel.post.DiscoverPostViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class HomeDiscoverPostFragment : Fragment(), OnPostClick {


    private var _binding: FragmentHomeDiscoverPostBinding? = null
    private val binding get() = _binding!!

    private val tagPost = Constants.LogTag.Post


    private var _discoverPostAdapter:DiscoverPostAdapter?=null
    private val discoverPostAdapter get() = _discoverPostAdapter!!


    private val discoverPostViewModel: DiscoverPostViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeDiscoverPostBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()

        if (!discoverPostViewModel.isDataLoaded) {
            getData()
            discoverPostViewModel.setDataLoadedStatus(true)
        }
    }


    private fun subscribeToObserver() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                discoverPostViewModel.userPost.onEach { response ->
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
        _discoverPostAdapter= DiscoverPostAdapter(this@HomeDiscoverPostFragment)
        binding.apply {
            rvDiscoverPost.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = discoverPostAdapter
            }
            setListener()
        }
    }


    private fun FragmentHomeDiscoverPostBinding.setListener() {
    }


    private fun getData() {
        discoverPostViewModel.getDiscoverPost()
    }

    private fun setData(userPosts: List<UserPostModel> = mutableListOf()) {
        MyLogger.v(isFunctionCall = true)

        if (userPosts.isEmpty()) {
            MyLogger.w(tagPost, msg = "list is empty then show no data view !")
            showNoDataView()
        } else {
            MyLogger.v(msg = "Now data is set into homeFragment !")
            discoverPostAdapter.submitList(userPosts.toList())
            hideNoDataView()
        }
    }

    private fun showNoDataView() {
        binding.apply {
            tvNoDataView.myShow()
            rvDiscoverPost.gone()
        }
    }

    private fun hideNoDataView() {
        binding.apply {
            tvNoDataView.gone()
            rvDiscoverPost.myShow()
        }
    }

    override fun onResume() {

        // This done because when first viewpager have data and height increase then user come this fragment and this fragment data not loaded or not have data but it take previous fragment height so avoid this below line put.
        binding.root.requestLayout()
        super.onResume()
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
        _discoverPostAdapter=null
        binding.rvDiscoverPost.adapter=null
        _binding = null
        super.onDestroyView()
    }

}


