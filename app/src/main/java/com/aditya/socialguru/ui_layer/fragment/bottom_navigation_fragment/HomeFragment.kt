package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.story.Stories
import com.aditya.socialguru.data_layer.model.story.UserStories
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.adapter.StoryAdapter
import com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar.HomeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val tagStory = Constants.LogTag.Story
    private val storyAdapter by lazy {
        StoryAdapter() {
            MyLogger.v(tagStory, msg = "User want upload story !")
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    private val navController get() = (requireActivity() as MainActivity).navController?.value

    private val pref by lazy {
        SharePref(requireContext())
    }


    private val homeViewModel: HomeViewModel by viewModels()

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            MyLogger.v(tagStory, msg = "User select pic now send to server !")
            lifecycleScope.launch {
                pref.getPrefUser().first()?.let {
                    MyLogger.v(tagStory, msg = "User data is retreived !")
                    homeViewModel.storeImageInFirebaseStorage(uri, it)
                } ?: run {
                    MyLogger.v(tagStory, msg = "User not found !")
                }
            }

        } else {
            MyLogger.v(tagStory, msg = "User revoke or cancel upload story !")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyLogger.v(isFunctionCall = true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MyLogger.v(isFunctionCall = true)

        handleInitialization()
    }

    private fun handleInitialization() {
        MyLogger.v(isFunctionCall = true)
        initUi()
        subscribeToObserver()
    }

    private fun subscribeToObserver() {
        homeViewModel.userStories.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        setData(it as ArrayList<UserStories>)
                    } ?: run {
                        setData(ArrayList())
                        Helper.showSnackBar((requireActivity() as MainActivity).findViewById(R.id.coordLayout) ,response.message.toString())
                    }
                }
                is Resource.Loading -> {}
                is Resource.Error -> {
                    Helper.showSnackBar((requireActivity() as MainActivity).findViewById(R.id.coordLayout) ,response.message.toString())
                }
            }
        }
    }


    private fun initUi() {
        MyLogger.v(isFunctionCall = true)

        binding.apply {
            rvStories.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL, false
                )
                adapter = storyAdapter
            }
            setListener()
        }
    }

    private fun FragmentHomeBinding.setListener() {

    }

    private fun setData(userData:ArrayList<UserStories>) {
        MyLogger.v(isFunctionCall = true)
        userData.add(0, UserStories(null , null))
        MyLogger.v(msg = "Now data is set into homeFragment !")
        storyAdapter.submitList(userData.toList())
    }

    override fun onResume() {
        MyLogger.v(isFunctionCall = true)
        super.onResume()
    }

    override fun onDestroyView() {
        MyLogger.v(isFunctionCall = true)
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MyLogger.v(isFunctionCall = true)
        super.onDestroy()
    }
}