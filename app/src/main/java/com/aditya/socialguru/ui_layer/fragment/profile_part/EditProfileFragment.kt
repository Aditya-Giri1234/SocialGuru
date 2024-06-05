package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aditya.socialguru.MainActivity
import com.aditya.socialguru.R
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.databinding.FragmentUpdateProfileBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.ProfilePicEditDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.removeErrorOnTextChanged
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.profile.ProfilePicEditOption
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.profile.EditProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class EditProfileFragment : Fragment(), ProfilePicEditOption {

    private var _binding: FragmentUpdateProfileBinding? = null
    private val binding get() = _binding!!

    private val tagProfile = Constants.LogTag.Profile

    private val editProfileViewModel by viewModels<EditProfileViewModel>()
    private val imageAvailable = "0"
    private val imageUnAvailable = "1"
    private var currentImage: String? = null
    private var newImage: String? = null

    private var myLoader: MyLoader? = null


    val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            MyLogger.v(tagProfile, msg = "User select pic now set to profile :- $uri")
            newImage = uri.toString()
            Glide.with(binding.ivProfile).load(uri).into(binding.ivProfile)

        } else {
            MyLogger.v(tagProfile, msg = "User revoke or cancel upload story !")
        }
    }

    private val pref by lazy {
        SharePref(requireActivity())
    }

    private val navController by lazy {
        (requireActivity() as MainActivity).navController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private val scaleDown: Animation by lazy {
        AnimationUtils.loadAnimation(requireContext(), R.anim.custom_button_bounce_anim)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentUpdateProfileBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
        subscribeToObserver()
    }

    private fun subscribeToObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                editProfileViewModel.userUpdateStatus.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            hideDialog()
                            Helper.showSuccessSnackBar(
                                (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                                    R.id.coordLayout
                                ), "Profile Updated !"
                            )

                            editProfileViewModel.getUser()
                        }

                        is Resource.Loading -> {
                            showDialog()
                        }

                        is Resource.Error -> {
                            hideDialog()
                            showSnackBar(response.message.toString())
                        }
                    }

                }.launchIn(this)

                editProfileViewModel.user.onEach { response ->
                    when (response) {
                        is Resource.Success -> {
                            response.data?.let {
                                MyLogger.v(tagProfile, msg = it, isJson = true, jsonTitle = "User")
                                pref.setPrefUser(it)
                            }
                            navController?.value?.navigateUp()
                        }

                        is Resource.Loading -> {

                        }

                        is Resource.Error -> {
                            MyLogger.e(
                                tagProfile,
                                msg = "Some error occurred :- ${response.message}"
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
                profileImage.gone()
                icBack.myShow()
                tvHeaderUserName.text = "Edit Profile"
            }

            lifecycleScope.launch {
                pref.getPrefUser().first()?.let { user ->
                    user.userProfileImage?.let {
                        ivProfile.tag = imageAvailable
                        currentImage = it
                        Glide.with(ivProfile).load(it).into(ivProfile)
                    } ?: run {
                        ivProfile.tag = imageUnAvailable
                    }
                    tiEtName.setText(user.userName)
                    tiEtProfession.setText(user.userProfession)
                    tiEtBio.setText(user.userBio)
                }
            }

            setListener()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentUpdateProfileBinding.setListener() {

        myToolbar.icBack.setSafeOnClickListener {
            navController?.value?.navigateUp()
        }

        root.setOnTouchListener { v, event ->
            MyLogger.v(tagProfile, msg = "Now touch on const main view !")
            v.onTouchEvent(event)
            requireActivity().window.decorView.clearFocus()
            root.hideKeyboard()
            true
        }

        ivProfile.setSafeOnClickListener {
            if (ivProfile.tag == imageAvailable) {
                Helper.showImageDialog(requireActivity(), ivProfile.getBitmapByDrawable())
            } else {
                Helper.showSnackBar(
                    (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                        R.id.coordLayout
                    ), "No Image Found !"
                )

            }

        }

        binding.icEdit.setSafeOnClickListener {
            if (currentImage == null) {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                ProfilePicEditDialog(this@EditProfileFragment).show(
                    childFragmentManager,
                    "My_Profile_Pic_Edit_Dialog"
                )
            }
        }



        btnSave.setSafeOnClickListener {
            MyLogger.v(tagProfile, msg = "Button is clicked !")
            if (validateDate()) {
                saveUser()
            }
        }

        btnSave.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.startAnimation(scaleDown)
                    scaleDown.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            // Perform the click action
                            v.performClick()
                        }

                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.clearAnimation()

                }
            }
            true
        }

        tilName.removeErrorOnTextChanged()
        tilProfession.removeErrorOnTextChanged()
        tilBio.removeErrorOnTextChanged()

    }

    private fun saveUser() {
        lifecycleScope.launch {
            pref.getPrefUser().first()?.let { user ->
                val newData = user.copy(
                    userName = binding.tiEtName.text.toString(),
                    userBio = binding.tiEtBio.text.toString(),
                    userProfession = binding.tiEtProfession.text.toString(),
                    userProfileImage = if (isProfilePicDeleted()) null else currentImage  // There is put old image reason if image present that is online image and if user delete image it set to null . New image should first store in storage and then get image.
                )
                user.userId?.let {
                    if (isProfilePicDeleted()) {
                        MyLogger.d(
                            tagProfile,
                            msg = "Profile pic is deleted , currentImage:- $currentImage  - newImage:- $newImage"
                        )
                        editProfileViewModel.updateProfile(newData, currentImage, newImage)
                    } else {
                        MyLogger.d(
                            tagProfile,
                            msg = "Profile pic is not deleted , currentImage:- $currentImage  - newImage:- $newImage"
                        )
                        editProfileViewModel.updateProfile(newData, newImage = newImage)
                    }
                }
            }
        }

    }

    private fun validateDate(): Boolean {
        binding.apply {
            return when {
                tiEtName.text.isNullOrEmpty() -> {
                    tilName.customError("Name must not null !")
                }

                tiEtProfession.text.isNullOrEmpty() -> {
                    tilProfession.customError("Profession must not null !")
                }

                tiEtBio.text.isNullOrEmpty() -> {
                    tilBio.customError("Bio must not null !")
                }

                else -> {
                    true
                }
            }
        }

    }

    private fun showSnackBar(message: String) {
        Helper.showSnackBar(
            (requireActivity() as MainActivity).findViewById<CoordinatorLayout>(
                R.id.coordLayout
            ), message
        )
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

    private fun isProfilePicDeleted(): Boolean {
        if (currentImage == null) return false

        //In this point one thing clear current image is not null
        return newImage != null || binding.ivProfile.tag == imageUnAvailable
    }

    override fun onDelete() {
        newImage = null  //Assure newImage should null and currentImage not change
        binding.ivProfile.setImageResource(R.drawable.ic_user)
        binding.ivProfile.tag = imageUnAvailable
    }

    override fun onUpload() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}