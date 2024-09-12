package com.aditya.socialguru.ui_layer.fragment.helper

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.FragmentImageShowBinding
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.ShareManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target


class ShowImageFragment : Fragment() {

    private var _binding:FragmentImageShowBinding?=null
    private val binding get() = _binding!!

    private val args:ShowImageFragmentArgs by navArgs()
    private lateinit var imageUri: Uri

    private val navController get() = (requireActivity() as MainActivity).navController
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
            Glide.with(ivImage).load(imageUri).error(R.drawable.no_image_found).addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    linearloader.gone()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    linearloader.gone()
                    return false
                }
            }).into(ivImage)
            setListener()
        }
    }


    private fun FragmentImageShowBinding.setListener() {
        icBack.setSafeOnClickListener {
            requireActivity().onBackPressed()
        }

        icShare.setSafeOnClickListener {
            ShareManager.shareImage(requireContext(),imageUri)
        }

    }
    

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}


