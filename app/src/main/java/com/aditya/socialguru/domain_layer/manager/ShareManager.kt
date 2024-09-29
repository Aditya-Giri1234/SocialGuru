package com.aditya.socialguru.domain_layer.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ShareManager {
    fun sharePost(context: Context, postId:String){
        context.apply {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "https://$packageName/post/$postId")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Post"))
        }
    }

    fun shareImage(context:Context,imageUri: Uri) {
        context.apply {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "https://$packageName/image/${encodeUrl(imageUri.toString())}")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        }
    }

    fun shareVideo(context: Context,videoUri: Uri) {
        context.apply {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "https://$packageName/video/${encodeUrl(videoUri.toString())}")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Video"))
        }
    }


    fun encodeUrl(originalUrl: String): String {
        val base64Encoded = Base64.encodeToString(originalUrl.toByteArray(), Base64.NO_WRAP)
        return URLEncoder.encode(base64Encoded, StandardCharsets.UTF_8.toString())
    }

    fun decodeUrl(encodedUrl: String): String {
        val base64Encoded = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
        return String(Base64.decode(base64Encoded, Base64.NO_WRAP))
    }
}