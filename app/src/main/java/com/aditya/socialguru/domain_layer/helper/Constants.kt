package com.aditya.socialguru.domain_layer.helper

import androidx.annotation.NonNull

object Constants {

    const val TYPE="TYPE"
    const val FRAGMENT_NAVIGATION="Fragment Navigation"

    enum class FragmentNavigation{
        OnBoardingFragment,
        SignInFragment,
        AddPostFragment
    }


    enum class FolderName{
        StoryImage ,
        StoryVideo,
        PostImage,
        PostVideo
    }

    enum class IntentTable{
        UploadProgress,
        UploadMessage,
        UploadState
    }

    enum class BroadcastType{
        StoryUploading
    }

    enum class StoryUploadState{
        StartUploading,
        Uploading,
        UploadingFail ,
        UrlNotGet,
        SavingStory,
        StoryUploadedSuccessfully
    }

    enum class PostUploadState{
        VideoUploading,
        VideoUploaded,
        ImageUploading,
        ImageUploaded,
        PostUploading,
        PostUploaded
    }

    enum class Table{
        Stories ,
        User ,
        Post
    }


    //Post type

    enum class PostType(){
        OnlyText,
        OnlyImage,
        OnlyVideo,
        TextAndImage,
        TextAndVideo,
        ImageAndVideo,
        All  //Text,Image,Video
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
        JobManager("🚀") ,
        Story ("📸"),
        Post ("✍️"),
        Chats ("💬"),
        Notification ("🔔")
    }
}