package com.aditya.socialguru.ui_layer.fragment.bottom_navigation_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
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
        MyLogger.v(isFunctionCall = true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MyLogger.v(isFunctionCall = true)

        //These checking because homeFragment created twice so take old one and destroy new one
        if (isHomeVisited
        ) {
            navController?.popBackStack()
            handleInitialization()
        }else{
            isHomeVisited =true
        }
    }

    private fun handleInitialization() {
        MyLogger.v(isFunctionCall = true)
        initUi()
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
        getData()
    }

    private fun FragmentHomeBinding.setListener() {

    }

    private fun getData() {
        MyLogger.v(isFunctionCall = true)
        val list=ArrayList<Stories>().apply {
            add(Stories(Constants.StoryType.UploadStory ))
        }
        MyLogger.v(msg = "Now data is set into homeFragment !")
        storyAdapter.submitList(list)
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