package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.UploadingResponse
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.AddModelRepository
import kotlinx.coroutines.launch

class AddPostViewModel (val app:Application) : AndroidViewModel(app) {

    private val tagPost=Constants.LogTag.Post
    private val repository=AddModelRepository()

    private val _uploadPost=MutableLiveData<Resource<UploadingResponse>>()
    val uploadPost:LiveData<Resource<UploadingResponse>> get() = _uploadPost

    fun uploadPost(post: Post)=viewModelScope.launch {
        _uploadPost.postValue(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            MyLogger.v(tagPost, msg = "Internet Available !")
              _uploadPost.postValue(Resource.Success(repository.uploadPost(post)))
        }else{
            MyLogger.v(tagPost, msg = "Internet Not Available !")
            _uploadPost.postValue(Resource.Error("No Internet  Available!"))
        }
    }
}