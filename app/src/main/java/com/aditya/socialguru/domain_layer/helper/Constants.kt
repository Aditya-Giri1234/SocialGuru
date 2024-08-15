package com.aditya.socialguru.domain_layer.helper


object Constants {

    const val TYPE = "TYPE"
    const val DATA = "DATA"

    //Notification  Constant

    const val BASE_URL="https://fcm.googleapis.com/"
    const val FIREBASE_MESSAGING_SCOPE="https://www.googleapis.com/auth/firebase.messaging"
    const val CONTENT_TYPE="application/json"
    const val FCM_ACCESS_TOKEN="fa5759d13c4d96797883ba02d1e0053c9193d0bc"

    //User Table Constant

    /*Use ctrl + shift + u to make lowercase to uppercase or vice-versa */
    const val USER = "User"
    const val COMMENT = "Comment"
    const val POST = "Post"
    const val STORY = "Story"
    const val FOLLOWER = "Follower"
    const val FOLLOWING = "Following"
    const val FRIEND = "Friend"
    const val REQUEST_FRIEND = "RequestFriend"


    enum class AppBroadCast{
        LogIn,
        LogOut
    }




    enum class FolderName {
        StoryImage,
        StoryVideo,
        PostImage,
        PostVideo,
        ProfilePic,
        ChatImage,
        ChatVideo
    }




    enum class BroadcastType {
        StoryUploading
    }

    enum class StoryUploadState(val status:String) {
        StartUploading("Starting Uploading"),
        Uploading("Uploading"),
        UploadingFail("Uploading Fail"),
        UrlNotGet("Url Not Get"),
        SavingStory("Story Saving"),
        StoryUploadedSuccessfully("Story Uploaded Successfully !")
    }

    enum class StoryEmitType {
        Starting,
        Added,
        Removed
    }

    enum class ListenerEmitType {
        Starting,
        Added,
        Removed ,
        Modify
    }

    enum class PostUploadState(val status:String) {
        VideoUploading("Video Uploading"),
        VideoUploaded("Video Uploaded"),
        ImageUploading("Image Uploading"),
        ImageUploaded("Image Uploaded"),
        PostUploading("Post Uploading"),
        PostUploaded("Post Uploaded Successfully!"),
        Error("Post Uploading Failed!")
    }

    enum class Table {
        Stories,
        User,
        Post,
        Like,
        Comment,
        Follower,
        Following,
        Friend,
        FriendRequest ,
        PendingRequest,
        Notification,
        MyLikedPost,
        Chats ,
        Messages,
        LastMessage,
        RecentChat,
        Media,
        ChatMuteNotification ,
        GroupChat ,
        GroupMember ,
        GroupInfo,
        UnSeenMessage
    }

    enum class FriendStatus {
        FRIEND ,FRIEND_REQUEST, PENDING_REQUEST, NOT_FRIEND
    }



    enum class UserTable(val fieldName: String) {
        USERID("userId"),
        USERNAME("userName"),
        USER_BIO("userBio"),
        USER_PROFESSION("userProfession"),
        USER_EMAIL_ID("userEmailId"),
        USER_PASSWORD("userPassword"),
        USER_PROFILE_IMAGE("userProfileImage"),
        LOG_IN_TIME_IN_TEXT("loginTimeInText"),
        LOG_IN_TIME_IN_TIMESTAMP("loginTimeInTimeStamp"),
        FCM_TOKEN("fcmToken"),
        POST_COUNT("postCount"),
        USER_AVAILABLE("userAvailable")
    }

    enum class StoryTable(val fieldName: String) {
        USERID("userId"),
        STORY_ID("storyId"),
        STORY_UPLOADING_TIME_IN_TEXT("storyUploadingTimeInText"),
        STORY_UPLOADING_TIME_IN_TIMESTAMP("storyUploadingTimeInTimeStamp"),
        STORY_TYPE("storyType"),
        STORY_URI("storyUri"),
        TEXT("text"),
        TEXT_BACKGROUND_COLOR("textBackGroundColor"),
        TEXT_FONT_FAMILY("textFontFamily"),
        STORY_VIEW_COUNT("storyViewCount"),
        STORY_SEEN_USERS_ID("storySeenUsersId")
    }


    enum class PostTable(val fieldName: String) {
        POST_ID("postId"),
        COMMENT_ID("commentId"),
        USER_ID("userId"),
        POST_TYPE("postType"),
        TEXT("text"),
        IMAGE_URL("imageUrl"),
        VIDEO_URL("videoUrl"),
        POST_UPLOADING_TIME_IN_TEXT("postUploadingTimeInText"),
        POST_UPLOADING_TIME_IN_TIMESTAMP("postUploadingTimeInTimeStamp"),
        COMMENT_COUNT("commentCount"),
        LIKE_COUNT("likeCount"),
        LIKED_USER_LIST("likedUserList")
    }

    enum class MessageTable(val fieldName: String){
        MESSAGE_ID("messageId"),
        MESSAGE_TYPE("messageType"),
        TEXT("text"),
        IMAGE_URI("imageUri"),
        VIDEO_URI("videoUri"),
        SENDER_ID("senderId"),
        RECEIVER_ID("receiverId"),
        MESSAGE_SENT_TIME_IN_TIMESTAMP("messageSentTimeInTimeStamp"),
        MESSAGE_SEND_TIME_IN_TEXT("messageSendTimeInText"),
        SEEN_STATUS("seenStatus")
    }

    //This is for chat seen status help to set tick status on message
    enum class SeenStatus(val status:String){
        Sending("Message is now sending to server"),
        Send("Message Sent To Server"),
        Received("Message Received To User"),
        MessageSeen("Message seen by receiver")
    }

    enum class LastMessageTable(val fieldName:String){
        SENDER_ID("senderId"),
        RECEIVER_ID("receiverId"),
        MESSAGE_TYPE("messageType"),
        MESSAGE("message"),
        CHAT_TYPE("chatType"),
        UNSEEN_MESSAGE_COUNT("unSeenMessageCount"),
        LAST_MESSAGE_SENT_TIME_IN_TIMESTAMP("lastMessageSentTimeInTimeStamp"),
        LAST_MESSAGE_SENT_TIME_IN_TEXT("lastMessageSentTimeInText"),
        IS_USER_1_ONLINE("isUser1Online"),
        IS_USER_2_ONLINE("isUser2Online")
    }

    enum class RecentChatTable(val fieldName:String){
        CHAT_ROOM_ID("chatRoomId"),
        LAST_MESSAGE_TIME_IN_TIMESTAMP("lastMessageTimeInTimeStamp"),
        LAST_MESSAGE_TIME_IN_TEXT("lastMessageTimeInText"),
        UNSEEN_MESSAGE_COUNT("unSeenMessageCount"),
        MESSAGE("message"),
        LAST_MESSAGE_TYPE("lastMessageType"),
        RECEIVER_ID("receiverId"),
        LAST_MESSAGE_SEEN("lastMessageSeen"),
        SENDER_ID("senderId"),
        USER_ID("userId")
    }

    enum class GroupMessageTable(val fieldName: String){
        MESSAGE_ID("messageId"),
        MESSAGE_TYPE("messageType"),
        CHAT_TYPE("chatType"),
        TEXT("text"),
        IMAGE_URI("imageUri"),
        VIDEO_URI("videoUri"),
        SENDER_ID("senderId"),
        MESSAGE_SENT_TIME_IN_TIMESTAMP("messageSentTimeInTimeStamp"),
        MESSAGE_SEND_TIME_IN_TEXT("messageSendTimeInText"),
        SEEN_STATUS("seenStatus"),
        INFO_MESSAGE_TYPE("infoMessageType"),
        ADDED_OR_REMOVED_USER_ID("addedOrRemovedUserId"),
        SEND_TIME_USERS("sendTimeUsers"),
        MESSAGE_RECEIVED_BY_USERS("messageReceivedByUsers"),
        MESSAGE_SEEN_BY_USERS("messageSeenByUsers")
    }

    //Message Type in chat which help to differentiate between date header and message

    enum class MessageType(val type:String){
        Chat("Chat Message"),
        DateHeader("Date Header"),
        Info("Info Message")
    }
    enum class LastMessageType(val type:String){
        Text("TextMessage"),
        Media("MediaMessage"),
        Info("InfoMessage")
    }

    // This is for group message where we need to extra info

    enum class InfoType{
        MemberAdded,
        MemberRemoved,
        GroupCreated,
        GroupDetailsChanged,
        NewGroupCreator,
        MakeAdmin,
        RemoveFromAdmin,
        MemberExit
    }


    //Post type

    enum class PostType() {
        OnlyText,
        OnlyImage,
        OnlyVideo,
        TextAndImage,
        TextAndVideo,
        ImageAndVideo,
        All  //Text,Image,Video
    }

    enum class StoryType {
        Text,
        Image,
        Video
    }

    //Storage Manager State
    enum class StorageManagerState {
        InProgress,
        Error,
        UrlNotGet,
        Success
    }

    // Notification Type

    enum class NotificationType{
        NEW_FOLLOWER,
        ACCEPT_FRIEND_REQUEST,
        FRIEND_REQUEST_COME,
        LIKE_IN_POST,
        COMMENT_IN_POST,
        TextChat,
        MediaChat,
        GroupTextChat,
        GroupMediaChat
    }

    // NotificationPriority
    enum class NotificationPriority(val priority:String){
        Normal("normal"),
        High("high")
    }

    //This is for which action invoke dialog in chat fragment
    enum class ChatDialogInvokeAction(val invokeReason:String){
        ClearChat("Clear all chat!"),
        DeleteSingleChat("Delete Single Chat !")
    }

    //For Json Handling
    enum class DataSource {
        FCM
    }

    // LogLevel
    enum class LogLevel {
        Error,
        Warning,
        Debug,
        Info,
        Verbose
    }

    enum class LogTag(val icon: String) {
        Error("❌"),
        Warning("⚠️"),
        Debug("🐞"),
        Info("ℹ️"),
        Verbose("🔊"),
        AppFlow("🌀"),
        Incoming("📥"),
        Outgoing("📤"),
        Calling("📞"),
        CdrFetching("📑"),
        NumberPurchase("☎️"),
        InsertDataInDB("💾"),
        Switching("🔄"),
        FCMToken("🔑"),
        Realm("🌍"),
        BlockUnblock("🚫"),
        Setting("⚙️"),
        DialCall("📞"),
        DeleteVoiceMailApi("🗑️"),
        DeleteCallRecordingApi("🗑️"),
        DeleteRecentCallCdrApi("🗑️"),
        DeleteMissedCallCdrApi("🗑️"),
        SipRegistration("📝"),
        Profile("👤"),
        ActivityLifeCycle("♾️"),
        Socket("🔌"),
        ContactSync("🔄"),
        Network("🌐"),
        LogIn("🔓"), // Unicode for open lock
        Logout("🔒"), // Unicode for closed lock
        ForceLogout("⛔"), // Unicode for no entry sign
        Pagination("🔢"),
        ButtonSwipe("👆"), // Unicode for upward pointing hand
        ForceUpdate("🔥"),
        AudioPlayer("🎵"),
        JobManager("🚀"),
        Story("📸"),
        Post("✍️"),
        Chats("💬"),
        Notification("🔔")
    }
}