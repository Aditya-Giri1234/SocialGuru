package com.aditya.socialguru.ui_layer.viewmodel.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aditya.socialguru.data_layer.model.Resource
import com.aditya.socialguru.data_layer.model.user_action.FriendCircleData
import com.aditya.socialguru.data_layer.shared_model.ListenerEmissionType
import com.aditya.socialguru.domain_layer.helper.Constants
import com.aditya.socialguru.domain_layer.helper.myLaunch
import com.aditya.socialguru.domain_layer.manager.MyLogger
import com.aditya.socialguru.domain_layer.manager.SoftwareManager
import com.aditya.socialguru.domain_layer.repository.chat.ChatRepo
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ChatViewModel ( val app : Application) : AndroidViewModel(app) {
    private val tagChat=Constants.LogTag.Chats

    private val repository= ChatRepo()

    private var _isDataLoaded=false

    val isDataLoaded get() = _isDataLoaded

    private val _friendList =
        MutableSharedFlow<Resource<List<FriendCircleData>>>(
            1, 64, BufferOverflow.DROP_OLDEST
        )
    val friendList get() = _friendList.asSharedFlow()

    //region:: Friend Operation
    fun getFriendListAndListenChange() = viewModelScope.myLaunch{
        _friendList.tryEmit(Resource.Loading())

        if (SoftwareManager.isNetworkAvailable(app)) {
            repository.getFriendList().onEach {
                _friendList.tryEmit(handleFriendResponse(it))
            }.launchIn(this)

        } else {
            _friendList.tryEmit(Resource.Error("No Internet Available !"))
        }
    }

    private fun handleFriendResponse(listenerHandling: ListenerEmissionType<FriendCircleData, FriendCircleData>): Resource<List<FriendCircleData>> {
        MyLogger.v(tagChat, isFunctionCall = true)

        val friendList =
            friendList.replayCache[0].data?.toMutableList() ?: mutableListOf<FriendCircleData>()

        when (listenerHandling.emitChangeType) {


            Constants.ListenerEmitType.Starting -> {
                MyLogger.v(
                    tagChat,
                    msg = listenerHandling.responseList, isJson = true,
                    jsonTitle = "This is starting friend type"
                )

                friendList.clear()
                listenerHandling.responseList?.let {
                    friendList.addAll(it.toMutableList() as ArrayList<FriendCircleData>)
                    friendList.sortByDescending { it.timeStamp }
                }
            }


            Constants.ListenerEmitType.Added -> {
                MyLogger.v(
                    tagChat,
                    msg = listenerHandling.singleResponse, isJson = true,
                    jsonTitle = "This is added friend type"
                )

                listenerHandling.singleResponse?.let {
                    friendList.add(it)
                    friendList.sortByDescending { it.timeStamp }
                }
            }

            Constants.ListenerEmitType.Removed -> {
                MyLogger.v(tagChat, msg = "This is removed friend type")

                listenerHandling.singleResponse?.let { follower ->
                    follower.userId?.let { userId ->
                        friendList.forEach { temp ->
                            if (temp.userId == userId) {
                                friendList.remove(temp)
                                friendList.sortBy { it.timeStamp }
                                return@let
                            }
                        }
                    }
                }
            }


            else -> {}
        }

        return Resource.Success(friendList.toList())
    }

    //endregion

    fun setDataLoadedStatus(status:Boolean){
        _isDataLoaded=status
    }
}