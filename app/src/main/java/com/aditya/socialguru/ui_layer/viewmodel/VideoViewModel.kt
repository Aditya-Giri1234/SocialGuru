package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class VideoViewModel(app:Application) : AndroidViewModel(app) {
    private var _playerPosition = 0L

    val playerPosition: Long
        get() = _playerPosition

    private var _isFirstTimeFragmentCreated=true
    val isFirstTimeFragmentCreated get()=_isFirstTimeFragmentCreated
    fun setPlayerPosition(position: Long) {
        _playerPosition = position
    }

    fun setFalseToFragmentCreateFirstTime(){
        _isFirstTimeFragmentCreated=false
    }
}