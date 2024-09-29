package com.aditya.socialguru.domain_layer.helper.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.launchCoroutineInIOThread
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MyWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val tagWork = Constants.LogTag.JobManager
    override suspend fun doWork(): Result {

        val count = FirebaseManager.listenMyStoryCount().first()
        if (count==0){
            MyLogger.e(tagWork , msg = "Stories is empty so that now i stop my worker !")
            WorkManager.getInstance(context).cancelUniqueWork(Constants.MY_CUSTOM_WORKER)
        }

        return try {
            // Use the flow directly in doWork
            val value =FirebaseManager.deleteStaleStories().first()
            if (value.isSuccess){
                MyLogger.i(tagWork , msg = "Stories is deleted!")
                Result.success()
            }else{
                MyLogger.e(tagWork , value.errorMessage.toString())
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            MyLogger.w(tagWork , msg = e.message)
            Result.failure() // Return failure in case of an error
        }
    }
}