package com.aditya.socialguru.domain_layer.custom_class.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.DeleteAccountPasswordDialogBinding
import com.aditya.socialguru.domain_layer.custom_class.MyLoader
import com.aditya.socialguru.domain_layer.helper.customError
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.helper.removeErrorOnTextChanged
import com.aditya.socialguru.domain_layer.helper.runOnUiThread
import com.aditya.socialguru.domain_layer.remote_service.DeleteAccountPasswordResult
import com.aditya.socialguru.domain_layer.service.SharePref
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first

class DeleteAccountPasswordDialog(val option: DeleteAccountPasswordResult) : DialogFragment() {

    private var _binding: DeleteAccountPasswordDialogBinding? = null
    private val binding get() = _binding!!

    private var myLoader: MyLoader? = null

    private val pref by lazy {
        SharePref(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DeleteAccountPasswordDialogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleInitialization()
    }

    private fun handleInitialization() {
        initUi()
    }

    private fun initUi() {
        binding.apply {
            dialog?.apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawableResource(android.R.drawable.screen_background_dark_transparent)
            }
            setListener()
        }
    }

    private fun DeleteAccountPasswordDialogBinding.setListener() {
        btnCancel.setOnClickListener {
            option?.onResult(false, null)
            dismiss()
        }
        btnYes.setOnClickListener {
            showDialog()
            handleReAuthentication()
        }

        dialog?.setOnDismissListener {
            option?.onResult(false, null)
        }
        tilPassword.removeErrorOnTextChanged()
    }

    private fun handleReAuthentication() {
        launchCoroutineInIOThread {
            pref.getPrefUser().first()?.let {
                val userEmailId = it.userEmailId!!
                val userPassword = it.userPassword!!
                val enteredPassword = binding.tiEtPassword
                    .text.toString()
                if (enteredPassword.isEmpty()) {
                    runOnUiThread {
                        binding.tilPassword.customError("Password must not null !")
                    }
                    hideDialog()
                    return@let
                }
                if (enteredPassword == userPassword) {
                    val credential = EmailAuthProvider
                        .getCredential(userEmailId, userPassword)
                    Firebase.auth.currentUser!!.reauthenticate(credential).addOnCompleteListener {
                        if (it.isSuccessful) {
                            option.onResult(true, "")
                        } else {
                            option.onResult(false, it.exception?.message.toString())
                        }
                        hideDialog()
                        dismiss()
                    }
                } else {
                    hideDialog()
                    option.onResult(false, "Password is incorrect")
                    dismiss()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // Adjust the width of the dialog to 80% of the screen width
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        return dialog
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