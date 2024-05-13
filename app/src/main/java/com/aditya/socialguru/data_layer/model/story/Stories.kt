package com.aditya.socialguru.data_layer.model.story

data class Stories(
    val userId: String? = null,
    val storyId: String? = null, // This is uuid
    val userName: String? = null,
    val profileImage: String? = null,
    val image: String? = null,
    val storyCount: Int? = null,  // This is not use full so after some time removed it.
    val timeStamp: Long? = null
)
