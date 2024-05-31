package com.aditya.socialguru.domain_layer.helper


object Constants {

    const val TYPE = "TYPE"
    const val DATA = "DATA"
    const val FRAGMENT_NAVIGATION = "Fragment Navigation"

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


    enum class FragmentNavigation {
        OnBoardingFragment,
        SignInFragment,
        AddPostFragment
    }


    enum class FolderName {
        StoryImage,
        StoryVideo,
        PostImage,
        PostVideo
    }

    enum class IntentTable {
        UploadProgress,
        UploadMessage,
        UploadState
    }

    enum class BroadcastType {
        StoryUploading
    }

    enum class StoryUploadState {
        StartUploading,
        Uploading,
        UploadingFail,
        UrlNotGet,
        SavingStory,
        StoryUploadedSuccessfully
    }

    enum class StoryEmitType {
        Starting,
        Added,
        Removed
    }

    enum  class PostEmitType{
        Starting,
        Added,
        Removed
    }

    enum class PostUploadState {
        VideoUploading,
        VideoUploaded,
        ImageUploading,
        ImageUploaded,
        PostUploading,
        PostUploaded ,
        Error
    }

    enum class Table {
        Stories,
        User,
        Post,
        Comment,
        Follower,
        Following,
        Friend,
        RequestFriend
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
        POST_COUNT("postCount")
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

    enum class StoryTpye {
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