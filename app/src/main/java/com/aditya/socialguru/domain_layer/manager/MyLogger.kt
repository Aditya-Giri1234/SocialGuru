package com.aditya.socialguru.domain_layer.manager

import android.util.Log
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.LogTag
import com.aditya.socialguru.domain_layer.helper.Constants.LogLevel
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MyLogger {

    // Log Info (By default calling tag is default)
    private var currentTag: String = Constants.LogTag.AppFlow.name
    private var currentLogTag: Constants.LogTag = Constants.LogTag.AppFlow

    //  private val gson = Gson()
    private val gson = GsonBuilder().setPrettyPrinting().create()


    //private var isLogCanShow = (AppMode.getAppMode() == Constants.DefineAppMode.Development)
    private var isLogCanShow = true


    //region:: Log use case with different scenario
    /*
     1) Log.e: This is for when bad stuff happens. Use this tag in places like inside a catch statement. You know that an error has occurred and therefore you're logging an error.

     2) Log.w: Use this when you suspect something shady is going on. You may not be completely in full on error mode, but maybe you recovered from some unexpected behavior. Basically, use this to log stuff you didn't expect to happen but isn't necessarily an error. Kind of like a "hey, this happened, and it's weird, we should look into it."

     3) Log.i: Use this to post useful information to the log. For example: that you have successfully connected to a server. Basically use it to report successes.

     4) Log.d: Use this for debugging purposes. If you want to print out a bunch of messages so you can log the exact flow of your program, use this. If you want to keep a log of variable values, use this.

     5) Log.v: Use this when you want to go absolutely nuts with your logging. If for some reason you've decided to log every little thing in a particular part of your app, use the Log.v tag.
    */

    //endregion


    fun setCurrentTag(tag: Constants.LogTag) {
        this.currentLogTag = tag
        this.currentTag = tag.name
    }


    private fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    fun e(
        logTag: Constants.LogTag? = null,
        className: String = Throwable().stackTrace[1].className.substring(
            Throwable().stackTrace[1].className.lastIndexOf(
                '.'
            ) + 1
        ),
        functionName: String = Throwable().stackTrace[1].methodName,
        lineNumber: Int = Throwable().stackTrace[1].lineNumber,
        jsonTitle: String? = null,
        msg: Any? = "",
        isFunctionCall: Boolean = false,
        isJson: Boolean = false
    ) {

        isLogCanShow.takeIf { it }?.let {
            val icon = getIconForTag(logTag ?: currentLogTag)
            val iconError = getIconForTag(Constants.LogTag.Error)
            val prefix = "[${getNotNullTag(logTag)} $icon $iconError ]"
            val sdf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
            val time = sdf.format(Date().time)
            val logLevel = LogLevel.Error

            runLog(
                logTag,
                className,
                functionName,
                lineNumber,
                msg,
                isFunctionCall,
                isJson,
                jsonTitle,
                time,
                prefix,
                logLevel
            )
        }

    }


    private fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    fun w(
        logTag: Constants.LogTag? = null,
        className: String = Throwable().stackTrace[1].className.substring(
            Throwable().stackTrace[1].className.lastIndexOf(
                '.'
            ) + 1
        ),
        functionName: String = Throwable().stackTrace[1].methodName,
        lineNumber: Int = Throwable().stackTrace[1].lineNumber,
        jsonTitle: String? = null,
        msg: Any? = "",
        isFunctionCall: Boolean = false,
        isJson: Boolean = false
    ) {

        isLogCanShow.takeIf { it }?.let {
            val icon = getIconForTag(logTag ?: currentLogTag)
            val iconWaring = getIconForTag(Constants.LogTag.Warning)
            val prefix = "[${getNotNullTag(logTag)} $icon $iconWaring ]"
            val sdf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
            val time = sdf.format(Date().time)
            val logLevel = LogLevel.Warning

            runLog(
                logTag,
                className,
                functionName,
                lineNumber,
                msg,
                isFunctionCall,
                isJson,
                jsonTitle,
                time,
                prefix,
                logLevel
            )
        }

    }

    private fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    fun i(
        logTag: Constants.LogTag? = null,
        className: String = Throwable().stackTrace[1].className.substring(
            Throwable().stackTrace[1].className.lastIndexOf(
                '.'
            ) + 1
        ),
        functionName: String = Throwable().stackTrace[1].methodName,
        lineNumber: Int = Throwable().stackTrace[1].lineNumber,
        jsonTitle: String? = null,
        msg: Any? = "",
        isFunctionCall: Boolean = false,
        isJson: Boolean = false
    ) {

        isLogCanShow.takeIf { it }?.let {
            val icon = getIconForTag(logTag ?: currentLogTag)
            val prefix = "[${getNotNullTag(logTag)} $icon]"
            val sdf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
            val time = sdf.format(Date().time)
            val logLevel = LogLevel.Info
            runLog(
                logTag,
                className,
                functionName,
                lineNumber,
                msg,
                isFunctionCall,
                isJson,
                jsonTitle,
                time,
                prefix,
                logLevel
            )
        }

    }


    private fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun d(
        logTag: Constants.LogTag? = null,
        className: String = Throwable().stackTrace[1].className.substring(
            Throwable().stackTrace[1].className.lastIndexOf(
                '.'
            ) + 1
        ),
        functionName: String = Throwable().stackTrace[1].methodName,
        lineNumber: Int = Throwable().stackTrace[1].lineNumber,
        jsonTitle: String? = null,
        msg: Any? = "",
        isFunctionCall: Boolean = false,
        isJson: Boolean = false
    ) {

        isLogCanShow.takeIf { it }?.let {
            val icon = getIconForTag(logTag ?: currentLogTag)
            val prefix = "[${getNotNullTag(logTag)} $icon]"
            val sdf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
            val time = sdf.format(Date().time)
            val logLevel = LogLevel.Debug
            runLog(
                logTag,
                className,
                functionName,
                lineNumber,
                msg,
                isFunctionCall,
                isJson,
                jsonTitle,
                time,
                prefix,
                logLevel
            )
        }
    }

    private fun v(tag: String, msg: String) {
        Log.v(tag, msg)
    }

    fun v(
        logTag: Constants.LogTag? = null,
        className: String = Throwable().stackTrace[1].className.substring(
            Throwable().stackTrace[1].className.lastIndexOf(
                '.'
            ) + 1
        ),
        functionName: String = Throwable().stackTrace[1].methodName,
        lineNumber: Int = Throwable().stackTrace[1].lineNumber,
        jsonTitle: String? = null,
        msg: Any? = "",
        isFunctionCall: Boolean = false,
        isJson: Boolean = false
    ) {
        isLogCanShow.takeIf { it }?.let {
            val icon = getIconForTag(logTag ?: currentLogTag)
            val prefix = "[${getNotNullTag(logTag)} $icon]"
            val sdf = SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault())
            val time = sdf.format(Date().time)
            val logLevel = LogLevel.Verbose

            runLog(
                logTag,
                className,
                functionName,
                lineNumber,
                msg,
                isFunctionCall,
                isJson,
                jsonTitle,
                time,
                prefix,
                logLevel
            )

        }

    }

    private fun runLog(
        logTag: Constants.LogTag?,
        className: String,
        functionName: String,
        lineNumber: Int,
        msg: Any?,
        isFunctionCall: Boolean,
        isJson: Boolean,
        jsonTitle: String?,
        time: String,
        prefix: String,
        logLevel: LogLevel
    ) {
        if (isJson) {
            val finalJsonTitle = jsonTitle ?: "Data"
            if (msg.toString().length > 300) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        printLog(
                            logTag,
                            "$time - $prefix - $className - $functionName - Line->$lineNumber - $finalJsonTitle :-> \n${
                                formatString(
                                    msg
                                )
                            }",
                            logLevel
                        )
                    } catch (e: Exception) {
                        printLog(
                            logTag,
                            "$time - $prefix - $className - $functionName - Line->$lineNumber - $finalJsonTitle :-> \n${msg}",
                            logLevel
                        )
                    }
                }
            } else {
                try {
                    printLog(
                        logTag,
                        "$time - $prefix - $className - $functionName - Line->$lineNumber - $finalJsonTitle :-> \n${
                            formatString(
                                msg
                            )
                        }",
                        logLevel
                    )
                } catch (e: Exception) {
                    printLog(
                        logTag,
                        "$time - $prefix - $className - $functionName - Line->$lineNumber - $finalJsonTitle :-> \n${msg}",
                        logLevel
                    )
                }
            }
        } else {
            if (isFunctionCall) {
                printLog(
                    logTag,
                    "$time - $prefix - $className - $functionName - Line->$lineNumber - Message :-> $functionName() is Call !",
                    logLevel
                )
            } else {
                var finalMsg = msg
                if (logLevel == LogLevel.Verbose) {
                    finalMsg = "\n\t\t\t\t$msg\t\t"
                }
                printLog(
                    logTag,
                    "$time - $prefix - $className - $functionName - Line->$lineNumber - Message :-> $finalMsg",
                    logLevel
                )
            }
        }
    }

    private fun formatString(str: Any?, isDeepCheck: Boolean = true): String {
        return try {
            str ?: return "Null Data !"
            val modifiedJsonString = gson.toJson(str, str::class.java as Type)
            if (modifiedJsonString.startsWith("[")) {
                JSONArray(modifiedJsonString).toString(4)
            } else {
                JSONObject(modifiedJsonString).toString(4)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e(Constants.LogTag.AppFlow, msg = e.message)
            JSONObject(str as String).toString(4)
        } catch (e: Error) {
            e.printStackTrace()
            e(
                Constants.LogTag.AppFlow,
                msg = "Out of memory error for this :=> ${str.toString()} and this size := ${str.toString().length}"
            )
            str.toString()
        }
    }

    private fun getNotNullTag(logTag: Constants.LogTag?): String {
        return logTag?.name ?: currentTag
    }

    private fun getIconForTag(tag: Constants.LogTag): String {
        return tag.icon
    }

    private fun printLog(tagName: Constants.LogTag?, msg: String, level: LogLevel) {
        val finalTag = getNotNullTag(tagName)
        when (level) {
            LogLevel.Error -> {
                e(finalTag, msg)
            }

            LogLevel.Warning -> {
                w(finalTag, msg)
            }

            LogLevel.Debug -> {
                d(finalTag, msg)
            }

            LogLevel.Info -> {
                i(finalTag, msg)
            }

            LogLevel.Verbose -> {
                v(finalTag, msg)
            }
        }
    }
}