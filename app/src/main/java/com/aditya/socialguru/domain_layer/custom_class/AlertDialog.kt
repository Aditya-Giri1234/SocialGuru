package com.aditya.socialguru.domain_layer.custom_class

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.databinding.DeleteAlertDialogBinding
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption

class AlertDialog(val message: String, val option: AlertDialogOption , val isForShowDelete:Boolean=true) : DialogFragment() {

    private var _binding: DeleteAlertDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DeleteAlertDialogBinding.inflate(layoutInflater)
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
            tvMessage.text=message
            dialog?.apply {
                setCancelable(true)
                setCanceledOnTouchOutside(true)
                window?.setBackgroundDrawableResource(android.R.drawable.screen_background_dark_transparent)
            }

            icDelete.isGone=isForShowDelete==false
            linearAppName.isGone=isForShowDelete
            setListener()
        }
    }

    private fun DeleteAlertDialogBinding.setListener() {
        btnCancel.setOnClickListener {
            option.onResult(false)
            dismiss()
        }
        btnYes.setOnClickListener {
            option.onResult(true)
            dismiss()
        }

        dialog?.setOnDismissListener {
            option.onResult(false)
        }
    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}