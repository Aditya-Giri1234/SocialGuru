package com.aditya.socialguru.ui_layer.viewmodel.bottom_navigation_bar

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.HomeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel(val app: Application) : AndroidViewModel(app) {


    private val repository = HomeRepository()
    private val _uploadStories = MutableLiveData<Resource<String>>()
    val uploadStories: LiveData<Resource<String>> get() = _uploadStories

    fun storeImageInFirebaseStorage(image: Uri) = viewModelScope.launch {

        _uploadStories.postValue(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.storeImageInFirebaseStorage(image)
        } else {
            _uploadStories.postValue(Resource.Error(message = "Internet not available ."))
        }

    }
}