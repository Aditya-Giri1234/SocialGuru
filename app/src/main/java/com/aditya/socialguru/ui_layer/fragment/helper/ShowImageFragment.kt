package com.aditya.socialguru.ui_layer.fragment.helper

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentImageShowBinding
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.helper.shareImage
import com.aditya.socialguru.ui_layer.activity.ContainerActivity


class ShowImageFragment : Fragment() {

    private var _binding:FragmentImageShowBinding?=null
    private val binding get() = _binding!!

    private val args:ShowImageFragmentArgs by navArgs()
    private lateinit var imageUri: Uri

    private val navController get() = (requireActivity() as ContainerActivity).navController
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= FragmentImageShowBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        imageUri=args.imageUri
        initUi()
    }

    private fun initUi() {
        binding.apply {
            ivImage.setImageURI(imageUri)
            setListener()
        }
    }


    private fun FragmentImageShowBinding.setListener() {
        icBack.setSafeOnClickListener {
            navController?.navigateUp()
        }

        icShare.setSafeOnClickListener {
            requireContext().shareImage(imageUri)
        }

    }


    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}


