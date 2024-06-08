package com.aditya.socialguru.ui_layer.viewmodel.profile.friend_circle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.post.UserPostModel
import com.aditya.socialguru.data_layer.model.user_action.FollowerData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.data_layer.shared_model.UpdateResponse
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.giveMeErrorMessage
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.post.friend_circle.FollowerRepository
import com.aditya.socialguru.domain_layer.service.FirebaseManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class FollowerViewModel(val app: Application) : AndroidViewModel(app) {


    private val tagProfile = Constants.LogTag.Profile
    private val repository = FollowerRepository()

    private var _isDataLoaded = false
    val isDataLoaded get() = _isDataLoaded

    private val _followerList =
        MutableSharedFlow<Resource<List<FollowerData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val followerList get() = _followerList.asSharedFlow()




    private val _removeFollower =
        MutableSharedFlow<Resource<UpdateResponse>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val removeFollower get() = _removeFollower.asSharedFlow()


    fun getFollowerAndListenChangeEvent() = viewModelScope.launch {
        _followerList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFollowerAndListenChangeEvent().onEach {
                _followerList.tryEmit(handleFollowerResponse(it))
            }.launchIn(this)

        } else {
            _followerList.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleFollowerResponse(listenerHandling: ListenerEmissionType<FollowerData, FollowerData>): Resource<List<FollowerData>> {
        MyLogger.v(tagProfile, isFunctionCall = true)

        val followerList = followerList.replayCache[0].data?.toMutableList() ?: mutableListOf<FollowerData>()

        when (listenerHandling.emitChangeType) {
            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagProfile,
                    msg = "This is starting follower type :- ${listenerHandling.responseList}"
                )
                followerList.clear()
                listenerHandling.responseList?.let {
                    followerList.addAll(it.toMutableList() as ArrayList<FollowerData>)
                    followerList.sortByDescending { it.timeStamp }
                }
            }


            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagProfile,
                    msg = "This is added follower type :- ${listenerHandling.singleResponse}"
                )
                listenerHandling.singleResponse?.let {
                    followerList.add(it)
                    followerList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagProfile, msg = "This is removed follower type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        followerList.forEach { temp ->
                            if (temp.userId == userId) {
                                followerList.remove(temp)
                                followerList.sortBy { it.timeStamp}
                                return@let
                            }
                        }
                    }
                }
            }


            else->{}
        }

        return Resource.Success(followerList.toList())
    }

     fun removeFollower(userId: String)= viewModelScope.launch {
         _removeFollower.tryEmit(Resource.Loading())

         if(SoftwareManager.isNetworkAvailable(app)){
             repository.removeFollower(userId).onEach {
                 if (it.isSuccess){
                     _removeFollower.tryEmit(Resource.Success(it))
                 }else{
                     _removeFollower.tryEmit(Resource.Error(giveMeErrorMessage("Removing Follower" ,it.errorMessage.toString())))
                 }
             }.launchIn(this)
         }else{
             _removeFollower.tryEmit(Resource.Error("No Internet Available !"))
         }
     }

    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }

}