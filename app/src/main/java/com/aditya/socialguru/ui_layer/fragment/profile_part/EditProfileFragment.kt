package com.aditya.socialguru.ui_layer.fragment.profile_part

import android.annotation.SuppressLint
import android.net.Uri
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
import com.aditya.socialguru.data_layer.model.User
import com.aditya.socialguru.databinding.FragmentUpdateProfileBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.custom_class.dialog.ProfilePicEditDialog
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.getBitmapByDrawable
import com.aditya.socialguru.domain_layer.helper.getStringText
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.helper.hideKeyboard
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.helper.myShow
import com.aditya.socialguru.domain_layer.helper.removeErrorOnTextChanged
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.helper.setCircularBackground
import com.aditya.socialguru.domain_layer.helper.setSafeOnClickListener
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.remote_service.profile.ProfilePicEditOption
import com.aditya.socialguru.domain_layer.service.SharePref
import com.aditya.socialguru.ui_layer.viewmodel.profile.EditProfileViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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

            setImageOnProfileView(uri)

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
        viewLifecycleOwner.lifecycleScope.launch {
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

                            navController.navigateUp()
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

            viewLifecycleOwner.lifecycleScope.launch {
                pref.getPrefUser().first()?.let { user ->
                    showInitial(user)
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
            navController.navigateUp()
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
            if (ivProfile.tag == imageUnAvailable) {
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
        lifecycleScope.myLaunch {
            pref.getPrefUser().first()?.let { user ->
                withContext(Dispatchers.Main){
                    val newData = user.copy(
                        userName = binding.tiEtName.getStringText(),
                        userNameLowerCase = binding.tiEtName.getStringText().lowercase(),
                        userBio = binding.tiEtBio.getStringText(),
                        userProfession = binding.tiEtProfession.getStringText(),
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

    }

    private fun validateDate(): Boolean {
        binding.apply {
            return when {
                tiEtName.text.isNullOrBlank()-> {
                    tilName.customError("Name must not null !")
                }

                tiEtProfession.text.isNullOrBlank() -> {
                    tilProfession.customError("Profession must not null !")
                }

                tiEtBio.text.isNullOrBlank()-> {
                    tilBio.customError("Bio must not null !")
                }

                else -> {
                    true
                }
            }
        }

    }

    private fun showInitial(user: User?=null, image:String?=null){
        binding.apply {
            when {
                user != null -> {
                    // Initial case when data load
                    runOnUiThread {
                        if (user.userProfileImage==null){
                            tvEditInitial.myShow()
                            tvEditInitial.text = user.userName?.get(0).toString()
                            tvEditInitial.setCircularBackground(Helper.setUserProfileColor(user))
                            ivProfile.tag= imageUnAvailable
                        }else{
                            tvEditInitial.gone()
                            Glide.with(ivProfile).load(user.userProfileImage).placeholder(R.drawable.ic_user).error(
                                R.drawable.ic_user).into(ivProfile)
                            ivProfile.tag = imageAvailable
                        }
                    }
                }

                else -> {
                    // this is case where user delete image or pick image
                    viewLifecycleOwner.lifecycleScope.launch {
                        pref.getPrefUser().first()?.let { user ->
                            runOnUiThread {
                                if (image==null){
                                    tvEditInitial.myShow()
                                    tvEditInitial.text = user.userName?.get(0).toString()
                                    tvEditInitial.setCircularBackground(Helper.setUserProfileColor(user))
                                    ivProfile.tag = imageUnAvailable
                                }else{
                                    tvEditInitial.gone()
                                    Glide.with(ivProfile).load(image).placeholder(R.drawable.ic_user).error(
                                        R.drawable.ic_user).into(ivProfile)
                                    ivProfile.tag = imageAvailable
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setImageOnProfileView(uri: Uri) {
        newImage = uri.toString()
        showInitial(image = uri.toString())
    }

    private fun removeImageFromProfileView(){
        binding.apply {
            newImage = null  //Assure newImage should null and currentImage not change
            showInitial()
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
        removeImageFromProfileView()
    }

    override fun onUpload() {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    override fun onPause() {
        hideKeyboard()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }


}