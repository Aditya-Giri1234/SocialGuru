package com.aditya.socialguru.domain_layer.custom_class

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.setPadding
import androidx.fragment.app.DialogFragment
import com.aditya.socialguru.R
import com.aditya.socialguru.databinding.DeleteAlertDialogBinding
import com.aditya.socialguru.domain_layer.helper.giveMeColor
import com.aditya.socialguru.domain_layer.helper.gone
import com.aditya.socialguru.domain_layer.remote_service.AlertDialogOption

class AlertDialog(val message: String, val option: AlertDialogOption? , val isForShowDelete:Boolean=true , val negativeMessage:String?=null,val positiveMessage:String?=null) : DialogFragment() {

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
            if (option==null){
                btnView.gone()
                btnYes.gone()
            }
            negativeMessage?.let {
                btnCancel.text = it
                btnCancel.setPadding(15)
                btnCancel.setTextColor(requireContext().giveMeColor(R.color.red))
            }
            positiveMessage?.let {
                btnYes.text = it
                btnYes.setPadding(15)
                btnYes.setTextColor(requireContext().giveMeColor(R.color.green))
            }
            if(negativeMessage!=null || positiveMessage!=null){
                tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            }
            setListener()
        }
    }

    private fun DeleteAlertDialogBinding.setListener() {
        btnCancel.setOnClickListener {
            option?.onResult(false)
            dismissNow()
        }
        btnYes.setOnClickListener {
            option?.onResult(true)
            dismissNow()
        }

        dialog?.setOnDismissListener {
            option?.onResult(false)
        }
    }

    override fun onDestroyView() {
        _binding=null
        super.onDestroyView()
    }

}