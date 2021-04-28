package com.hippocall

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by rajatdhamija
 * 21/09/18.
 */
@Parcelize
data class VideoCallModel(
    var channelId: Long,
    val userThumbnailImage: String = "",
    val channelName: String = "",
    val userId: Long = -1L,
    val otherUserId: Long = -1L,
    var fullName: String = "",
    var turnApiKey: String = "",
    var turnUserName: String = "",
    var turnCredential: String = "",
    var stunServers: ArrayList<String> = ArrayList(),
    var turnServers: ArrayList<String> = ArrayList(),
    var activityLaunchState: String? = "",
    var signalUniqueId: String? = "",
    val callType: String? = "",
    var inviteLink: String? = "",
    var jitsiLink: String? = "",
    var roomName: String? = "",
    var myname: String? = "",
    var myImagePath: String? = "",
    var hasGroupCall: Boolean? = false,
    var isVideoCall: Boolean = false,
    var isAudioCall: Boolean = false
) : Parcelable