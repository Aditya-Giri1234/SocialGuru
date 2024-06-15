package com.aditya.socialguru.domain_layer.helper.json_event_handling

import android.content.Context
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.Constants.DataSource

interface HandleJsonEvent {
    fun handle(dataSource: DataSource, jsonData: String)
}

class DataProcessor(private val context: Context) : HandleJsonEvent {
    override fun handle(dataSource: DataSource, jsonData: String) {
        when (dataSource) {
            DataSource.FCM -> {
                handleFcmData(jsonData)
            }
        }
    }
    private fun handleFcmData(jsonData: String) {
        FcmDataHandling.handle(context, jsonData)
    }
}