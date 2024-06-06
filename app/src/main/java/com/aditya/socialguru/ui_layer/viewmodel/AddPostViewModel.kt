package com.aditya.socialguru.ui_layer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.data_layer.model.post.Post
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.AddModelRepository
import kotlinx.coroutines.launch

class AddPostViewModel (val app:Application) : AndroidViewModel(app) {

    private val tagPost=Constants.LogTag.Post
    private val repository=AddModelRepository()

    private var _text = ""
    val text get() = _text
    private var _imageUri: Uri? = null
    val imageUri get() = _imageUri

    private var _videoUri :Uri?=null
    val videoUri get() = _videoUri

    private val _uploadPost=MutableLiveData<Resource<UpdateResponse>>()
    val uploadPost:LiveData<Resource<UpdateResponse>> get() = _uploadPost

    fun uploadPost(post: Post)=viewModelScope.launch {
        _uploadPost.postValue(Resource.Loading())
        if (SoftwareManager.isNetworkAvailable(app)){
            MyLogger.v(tagPost, msg = "Internet Available !")
              repository.uploadPost(post)
        }else{
            MyLogger.v(tagPost, msg = "Internet Not Available !")
            _uploadPost.postValue(Resource.Error("No Internet  Available!"))
        }
    }


    //region::Handle config and theme change

    fun setText(content:String){
        _text=content
    }

    fun setImageUri(uri:Uri?){
        _imageUri=uri
    }
    fun setVideoUri(uri:Uri?){
        _videoUri=uri
    }

    //endregion
}