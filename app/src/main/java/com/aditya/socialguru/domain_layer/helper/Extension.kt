package com.aditya.socialguru.domain_layer.helper

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.core.view.isGone
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException


//region:: View related extension come here
fun View.gone(){
    isGone=true
}

fun View.show(){
    isGone=false
}

fun EditText.getStringText():String{
    return text.toString()
}

//endregion


fun NavController.safeNavigate(direction: NavDirections) {
    currentDestination?.getAction(direction.actionId)?.run { navigate(direction) }
}

fun NavController.safeNavigate(direction: NavDirections, navOptions: NavOptions) {
    currentDestination?.getAction(direction.actionId)?.run { navigate(direction, navOptions) }
}

fun NavController.safeNavigate(
    @IdRes currentDestinationId: Int,
    @IdRes id: Int,
    args: Bundle? = null
) {
    if (currentDestinationId == currentDestination?.id) {
        navigate(id, args)
    }
}


fun TextInputLayout.removeErrorOnTextChanged() {
    val watcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            error = null
        }
    }

    editText?.apply {
        addTextChangedListener(watcher)
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View) {
                removeTextChangedListener(watcher)
            }
            override fun onViewAttachedToWindow(v: View) {}
        })

    }
}

 fun TextInputLayout.customError(msg:String):Boolean{
     error=msg
     requestFocus()
    return false
}


fun EditText.getQueryTextChangeStateFlow(): StateFlow<String> {

    val query = MutableStateFlow("")


    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int,
        ) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            query.value = s.toString()
        }
    })
    return query

}


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> Task<T>.await():T{
    return suspendCancellableCoroutine {cons->
        addOnCompleteListener {
            if (it.exception!=null){
                cons.resumeWithException(it.exception!!)
            }else{
                cons.resume(it.result,null)
            }
        }
    }
}


suspend fun CoroutineScope.delay(time:Long,run:()->Unit){
    kotlinx.coroutines.delay(time)
    run()
}

