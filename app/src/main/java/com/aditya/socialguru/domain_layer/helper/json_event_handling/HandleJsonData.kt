package com.aditya.socialguru.domain_layer.helper.json_event_handling

import android.content.Context

interface HandleJsonData {
    fun handle(context: Context, jsonData:String)
}