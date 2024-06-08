package com.aditya.socialguru.domain_layer.helper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.cert.CertPathValidatorException.Reason
import kotlin.coroutines.resumeWithException
import kotlin.system.measureTimeMillis


//region:: View related extension come here
fun View.gone() {
    isGone = true
}

fun View.myShow() {
    isGone = false
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.enabled() {
    isEnabled = true
    alpha = 1f
}

fun View.disabled() {
    isEnabled = false
    alpha = .6f
}

fun ImageView.getBitmapByDrawable(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun AppCompatActivity.isResumed(): Boolean {
    return lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
}

/*fun ShimmerFrameLayout.isShimmerOn(): Boolean {

    // this property take some delay to update after stop shimmer stop (so not use this)
    // return isShimmerStarted || isShimmerVisible
    return isVisible
}*/

fun EditText.getStringText(): String {
    return text.toString()
}

fun Context.giveMeColor(colorId:Int) = resources.getColor(colorId,null)

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
    val safeClickListener = SafeClickListener {
        onSafeClick(it)
    }
    setOnClickListener(safeClickListener)
}

fun RecyclerView.safeScrollToPosition(position: Int) {
    if (position == -1) return
    postDelayed({ scrollToPosition(position) }, 300)
}

fun View.hideKeyboard() {
    val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    val inputMethodManager =
        this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    CoroutineScope(Dispatchers.Default).launch {
        myDelay(200)
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }
}

fun Fragment.isResume(): Boolean {
    return this.isAdded && this.isResumed
}

fun String.convertParseUri(): Uri {
    return Uri.parse(this)
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

fun TextInputLayout.customError(msg: String): Boolean {
    error = msg
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

//Safe Click

private var lastTimeClicked: Long =
    0  //Make global so that click handling possible to global else all view get own lastTimeClicked variable.

class SafeClickListener(
    private var defaultInterval: Int = 1000,
    private val onSafeCLick: (View) -> Unit
) : View.OnClickListener {

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        onSafeCLick(v)
    }
}


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> Task<T>.await(): T {
    return suspendCancellableCoroutine { cons ->
        addOnCompleteListener {
            if (it.exception != null) {
                cons.resumeWithException(it.exception!!)
            } else {
                cons.resume(it.result, null)
            }
        }
    }
}


suspend fun CoroutineScope.myDelay(time: Long, run: (() -> Unit) ?= null) {
        delay(time)
        run?.invoke()
}



fun runOnUiThread(run:()->Unit){
    CoroutineScope(Dispatchers.Main).launch {
        run()
    }
}
fun (() -> Any).getTime(): Long {
    return measureTimeMillis {
        invoke()
    }
}

fun Context.monitorInternet(): Flow<Boolean> = callbackFlow{

    val callback=object : ConnectivityManager.NetworkCallback(){
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            trySend(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            trySend(false)
        }
    }

    val manager=getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    manager.registerDefaultNetworkCallback(callback)

    awaitClose {
        manager.unregisterNetworkCallback(callback)
        channel.close()
    }


}



@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.bufferWithDelay(delay: Long): Flow<T> {
    return buffer(1) // Buffer one item at a time
        .flatMapConcat { item ->
            flow {
                delay(delay) // Delay for the specified duration
                emit(item)
            }
        }
}


fun giveMeErrorMessage(whatProcessRunning:String,reason: String)="Some error occurred during ${whatProcessRunning.lowercase()} :- $reason"




fun Context.shareImage(imageUri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, imageUri)
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, "Share Image"))
}

fun Context.shareVideo(videoUri: Uri) {
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, videoUri)
        type = "video/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, "Share Video"))
}


/**
 * Extension function to convert SSP (scaled size pixels) to pixels.
 * @param ssp A value in ssp (scaled size pixels) which we need to convert into pixels.
 * @return A float value representing pixels equivalent to the ssp value.
 */
fun Context.sspToPx(ssp: Float): Float {
    // Get the scaled density from the DisplayMetrics
    val scaledDensity = resources.displayMetrics.scaledDensity
    // Convert ssp to pixels
    return ssp * scaledDensity
}