package com.aditya.socialguru.domain_layer.manager

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Helper
import com.aditya.socialguru.domain_layer.helper.await
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.service.SharePref
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.collect.Lists
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.min
import kotlin.math.pow

object FCMTokenManager {
    private var fcmToken = ""
    private val tagFcm = Constants.LogTag.FCMToken

    //This is for getting fcm token
    private const val MAX_RETRIES = 7
    private const val INITIAL_BACKOFF_DELAY = 1000L // 1 second
    private const val MAX_BACKOFF_DELAY = 60000L // 1 minute

    private var previousSendTokenTime: Long = 0

    fun generateToken() = callbackFlow<String?> {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                trySend(it.result)
            } else {
                trySend(null)
            }
        }.await()

        awaitClose {
            close()
        }
    }


    /**
     * Here use of back off algo to tackle error to generate token , max  [MAX_RETRIES] is 7 . */

    fun generateFcmTokenByBackOfAlgo(
        activity: Activity,
        retryCount: Int = 1,
        onCompletion: (token: String) -> Unit
    ) {
        MyLogger.v(tagFcm, isFunctionCall = true)
        val sharedPref = getSharedPreference(activity)
        MyLogger.v(tagFcm, isFunctionCall = true)
        if (retryCount > MAX_RETRIES) {
            MyLogger.e(
                Constants.LogTag.FCMToken,
                msg = "Failed to retrieve FCM token after maximum retries."
            )
            Helper.showFcmNotSendDialog(
                activity,
                message = "Kindly check your internet connection , something went wrong !"
            )
            return
        }


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            CoroutineScope(Dispatchers.Default).launch {
                fcmToken = if (task.isSuccessful) {
                    MyLogger.i(
                        Constants.LogTag.FCMToken,
                        msg = "Fcm Token generated successfully !"
                    )
                    task.result
                } else {
                    MyLogger.e(
                        Constants.LogTag.FCMToken,
                        msg = "Fetching FCM registration token failed due to ${task.exception}"
                    )
                    if (retryCount == 1) {
                        Helper.showFcmNotSendDialog(
                            activity,
                            message = "Kindly check your internet connection , something went wrong !"
                        )
                    }
                    sharedPref.getFcmToken() ?: ""
                }

                if (fcmToken.isNotEmpty()) {
                    onCompletion(fcmToken)
                    MyLogger.d(Constants.LogTag.FCMToken, msg = "Fcm token is $fcmToken !")
                } else {
                    val backoffDelay = calculateBackoffDelay(retryCount)
                    MyLogger.w(
                        Constants.LogTag.FCMToken,
                        msg = "FCM Token not sending due to some reason so that ${retryCount + 1}\'th retry attempt continue and with delay :- $backoffDelay milliseconds !"
                    )
                    delay(backoffDelay)
                    generateFcmTokenByBackOfAlgo(activity, retryCount + 1, onCompletion)
                }
            }
        }
    }

    fun sendTokenToServer(send: () -> Unit) {
        val currentTime = SystemClock.elapsedRealtime()
        val timeInterval = currentTime - previousSendTokenTime
        if (timeInterval < 1000) {
            MyLogger.w(
                tagFcm,
                msg = "Token sending request come within 1 second so that just ignore it !"
            )
        } else {
            MyLogger.i(tagFcm, msg = "Token now sending to server  !")
            send()
        }
        previousSendTokenTime = currentTime
    }

    private fun getSharedPreference(context: Context): SharePref = SharePref(context)


    private fun calculateBackoffDelay(retryCount: Int): Long {
        val backoffDelay = INITIAL_BACKOFF_DELAY * 2.0.pow(retryCount.toDouble()).toLong()
        return min(backoffDelay, MAX_BACKOFF_DELAY)
    }


    suspend fun getAccessToken(): String? {
        return try {
            val serviceString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"socialguru-706ba\",\n" +
                    "  \"private_key_id\": \"bb1e6076dd0411fb440bfa9b6a22d0f0b1a0f23e\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCse6yDuFwm/2r9\\n9MOKsFqm8c6gIRyjESD4ltTDNydPTW3aQjYSVHhomQFYf/0Wb+L3iUSwxywSTFyR\\nxIa2Apw8Yif8hLOssyoneq4XVR5Tl2c3Pw76pMxMdNmZ8Rf3f2bo1IHCpGQgS+KD\\nRnMhjlv/0d3jQBZ4dlEvbTmZgMZl9gomm5TRDWWzNNeVEht7B/7v1tQ06AFHWflz\\nDXGQMra5Zx0eZ2vxj4oTl34SpCP/VKxtO61rpbEymx+c3hqApeyzsvxqDAade1yC\\nOLbnsc+J2DTkMSu//UpIvnBOZQGRKkqG35PiXaYDyncw5QHqmHXawrgK52TOepaW\\nrHN3rRFtAgMBAAECggEAHVwJ3/v8N6kneaX1uQBO1fb4EHrCaKFsRtRJ0Bbtyopo\\nBgxaLuGRA2D0AS/8ERsne7IybpLglhbIdQmKUqX8GS0uGEYwXVe7969ivdb/zAPM\\nDnPd5V0y51rGhXUr+rWFe2Fc2xhncLXqDyVpSI92aoO7LMkGaifdpuCFOvNYEhe4\\nj7JvLPtk+bgRoJFoLPM13pdIH/m1xDs7zcaYszq28vtW1TlIZ8eF0JIVOcUoq0ik\\niyCqwq20oir6khoqOk7cE8Rj/JWmDr1mactOg9myM6SKvODh+h7YLlZPJqH7iAv8\\nOBPtTIFO5SQFhnnD6jE0/SJKxjCVnrdE9inewJGcEQKBgQDza9gWg4uuecL+5VX0\\nY05CxPYBDHVV7vrpT2hoLDo52LpJYKysIxpmenP/GW1Ojy9kzjeTfIHnwNha99gU\\nM3ss+irsFuJxoCmCI5C28zC1GULiSRnrslA2M1c/+Ud+KifawpMQnp68hXCW0MDs\\n0N3tXipro9MvII3Lk7Glh+mAGQKBgQC1ZWhPppcpcnKD9UdxhHIhAckpAHPqGUZh\\nnfYl6GVZvT9s3MOyNUpoORc/CvelnXYeg7bTTYEKrYnin+9g5usyOPPqQRCH17s6\\nal3HIaZ5MnTjUdwmbnnogxW2B95AojFfDCh/iX6lySQ+D+gtp/uExdEKT9jV1C6v\\nXzZ+tXp2dQKBgALN5EBmJY7OAkwTckkNd9JXGIpsjVF/Hk7fxlEk6UrPT14XCgY1\\npVE26e0vas3z4lTj90nwrkJwO03Y+xlIeovLGgF8RgGPGctRA5LH/HCpqNaYhv7r\\nH/dv4fpvsb4nWCwx/6W6XhPH6cMjBoXz5gqdAhoZWg557ohA3FQO28bJAoGAXzjb\\nmCxoX76PdMv2dBXF36PTWG2/a51W//lu9JlZUtFwkRIWvN8Sr0GA/XwhYlQVZJFa\\nEPQ3kEZnAotYmvK9doDFMixzpAvQYiriDZ2RiT++cnJPcfE+l5rwc70Po7hA2JdG\\nTyT7UcYT+2xYQldTRnCdep9NwXoAA9mkfsF/ht0CgYEAv1gXFjvoI4qJ2QsOq7i+\\nOOBHg4+ac9MgO5T9k/Bb64Ii+OwpFvukfTzRZRMyL6zVtXHyINH0hRd3+YkV/lc0\\nmqy/jqFOcBDAhe7r0Z+SI1tTHF+U7cyHtJh8LYoirReA3Dd7+Pv8uQr//0akYBSs\\nGBVHwyfZZ3P4c/QZkI8hBGw=\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-4h23q@socialguru-706ba.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"108236120162036896302\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-4h23q%40socialguru-706ba.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}"

            serviceString.byteInputStream(Charsets.UTF_8).use {
                val googleCredentials = GoogleCredentials.fromStream(it)
                    .createScoped(Lists.newArrayList(Constants.FIREBASE_MESSAGING_SCOPE))
                googleCredentials.refresh()
                googleCredentials.accessToken.tokenValue
            }


        } catch (e: IOException) {
            MyLogger.v(
                tagFcm,
                msg = giveMeErrorMessage("Generate Fcm access token", e.message.toString())
            )
            null
        }

    }
}