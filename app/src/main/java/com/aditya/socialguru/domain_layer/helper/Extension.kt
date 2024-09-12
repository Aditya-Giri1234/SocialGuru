package com.aditya.socialguru.domain_layer.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.RecyclerView
import com.aditya.socialguru.data_layer.model.chat.group.GroupInfo
import com.aditya.socialguru.domain_layer.service.firebase_service.AuthManager
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.WriteBatch
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
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
import kotlinx.coroutines.tasks.await
import java.lang.reflect.Field
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

fun Fragment.hideKeyboard() {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(requireView().windowToken, 0)
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
    navOptions: NavOptions?=null,
    args: Bundle? = null
) {
    if (currentDestinationId == currentDestination?.id) {
        navigate(id,args,navOptions)
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
    private var defaultInterval: Int = 500,
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




/**
* This function launch coroutine on IO thread.
* @param run This function run on IO Thread.
* */
fun CoroutineScope.myLaunch(run:suspend CoroutineScope.()->Unit) : Job{
   return  launch(Dispatchers.IO) {
        run()
    }
}


fun launchCoroutineInIOThread(run:suspend CoroutineScope.()->Unit) : Job{
   return CoroutineScope(Dispatchers.IO).launch {
        run()
    }
}
fun launchCoroutineInDefaultThread(run:suspend CoroutineScope.()->Unit) : Job{
    return CoroutineScope(Dispatchers.Default).launch {
        run()
    }
}


/**
 * Call this method (in onActivityCreated or later) to set
 * the width of the dialog to a percentage of the current
 * screen width.
 */
fun DialogFragment.setWidthPercent(percentage: Int) {
    val percent = percentage.toFloat() / 100
    val dm = Resources.getSystem().displayMetrics
    val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
    val percentWidth = rect.width() * percent
    dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
}

/**
 * Call this method (in onActivityCreated or later)
 * to make the dialog near-full screen.
 */
fun DialogFragment.setFullScreen() {
    dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}


/**
 * [toNormalMap] is convert data class to map. It just convert data class propery name as key in string data type in map .
 * */
fun <T:Any> T.toNormalMap(): Map<String, Any> {
    val gson = Gson()
    val json = gson.toJson(this)
    val type = object : TypeToken<Map<String, Any>>() {}.type
    return gson.fromJson(json, type)
}


/**
 * [toMapWithSerializedName] is convert data class to map. But  it give priority base on if [SerializedName] present it give priority else make key name from property name.
 * */
fun <T:Any> T.toMapWithSerializedName(): Map<String, Any> {
    val map = LinkedHashMap<String, Any>()
    val fields: Array<Field> = this.javaClass.declaredFields

    for (field in fields) {
        //This is for kotlin add this field extra , so avoid this to add into map
        if (field.name.equals("\$stable")) continue
        field.isAccessible = true
        val serializedName = field.getAnnotation(SerializedName::class.java)?.value ?: field.name
        val value = field.get(this) ?: ""
        map[serializedName] = value
    }
    return map
}


// For Group Extension

 fun GroupInfo.isIAmCreatorOfThisGroup() = creatorId == AuthManager.currentUserId()
 fun GroupInfo.isIAmAdminOfThisGroup() = groupAdmins?.contains(AuthManager.currentUserId()) ?: false


// For Resize Screen When keyboard open

fun Activity.resizeActivate() {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}

fun Activity.resizeStop() {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        @Suppress("DEPRECATION")
        // Set it back to its default mode, e.g., SOFT_INPUT_ADJUST_PAN
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }
}


fun WriteBatch.safeUpdate(documentReference: DocumentReference, data: Map<String, Any> , isExist:Boolean): WriteBatch? {
    return  if (isExist) update(documentReference ,data) else null
}

fun WriteBatch.safeUpdate(documentReference: DocumentReference , isExist:Boolean, field: String ,value : Any? ): WriteBatch? {
    return  if (isExist) update(documentReference , field ,value) else null
}
suspend fun DocumentReference.safeUpdate(field: String ,value: Any?) : Task<Void>?{
    return if(this.isExist()){
        update(field ,value)
    } else null
}


suspend fun DocumentReference.isExist(): Boolean {
    return get().await().exists()
}

fun View.setCircularBackground(color: Int) {
    // Create a GradientDrawable
    val drawable = GradientDrawable()
    drawable.shape = GradientDrawable.OVAL // Set shape to oval to make it circular
    drawable.setColor(color) // Set the dynamic color
    background = drawable // Apply the drawable as the background
}

suspend fun <T, R> List<T>.mapAsync(transform: suspend (T) -> R): List<R> = coroutineScope {
    map { item ->
        async {
            transform(item)
        }
    }.awaitAll() // Wait for all async operations to complete and return the results
}

// For Getting Screen width and height

fun getScreenWidth(): Int = Resources.getSystem().displayMetrics.widthPixels

fun getScreenHeight() : Int = Resources.getSystem().displayMetrics.heightPixels