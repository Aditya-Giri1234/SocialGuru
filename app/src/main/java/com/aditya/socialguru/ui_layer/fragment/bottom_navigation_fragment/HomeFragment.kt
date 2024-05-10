package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.data_layer.model.Stories
import com.aditya.socialguru.databinding.FragmentHomeBinding
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.ui_layer.adapter.StoryAdapter
import com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar.HomeViewModel

class HomeFragment : Fragment() {


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val storyAdapter by lazy { StoryAdapter(){
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    } }

    private val navController get() = (requireActivity() as MainActivity).navController?.value

    companion object {
        var isHomeVisited = false
    }

    private val homeViewModel : HomeViewModel by viewModels()

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
                homeViewModel.storeImageInFirebaseStorage(uri)
        } else {

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isHomeVisited
        ) {
            navController?.navigateUp()
        }else{
            isHomeVisited =true
            handleInitialization()
        }
    }

    private fun handleInitialization() {
        initUi()
    }



    private fun initUi() {
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
        getData()
    }

    private fun FragmentHomeBinding.setListener() {

    }

    private fun getData() {
        val list=ArrayList<Stories>().apply {
            add(Stories(Constants.StoryType.UploadStory))
        }
        storyAdapter.submitList(list)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}