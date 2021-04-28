package com.hippocall.confcall

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.*
import com.hippocall.*
import com.hippocall.WebRTCCallConstants.Companion.CALL_TYPE
import com.hippocall.WebRTCCallConstants.Companion.DEVICE_PAYLOAD
import com.hippocall.WebRTCCallConstants.Companion.USER_THUMBNAIL_IMAGE
import com.hippocall.WebRTCCallConstants.Companion.VIDEO_CALL_TYPE
import org.json.JSONObject

/**
 * Created by gurmail on 2020-04-07.
 * @author gurmail
 */

class PushReceiver {

    private var pushReceiver: PushReceiver? = null

    fun getInstance(): PushReceiver {
        if (pushReceiver == null) {
            pushReceiver = PushReceiver()
            return pushReceiver as PushReceiver
        } else {
            return pushReceiver as PushReceiver
        }
    }

    @Synchronized fun incomingCallNotification(context: Context, messageJson: JSONObject, videoCallModel: VideoCallModel) {

        val userId = com.hippo.database.CommonData.getUserDetails().data.userId.toInt()
        if (messageJson.has("user_id") && messageJson.optInt("user_id") != userId) {
            val linkArray = messageJson.getString("invite_link").replace("#config.startWithVideoMuted=true", "").split("/")

            if (messageJson.getString(VIDEO_CALL_TYPE).equals(WebRTCCallConstants.JitsiCallType.HUNGUP_CONFERENCE.toString())
                || messageJson.getString(VIDEO_CALL_TYPE).equals(WebRTCCallConstants.JitsiCallType.REJECT_CONFERENCE.toString())) {

                HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
                HippoCallConfig.getInstance().stopTimerTask()

                val hungupIntent = Intent(context, HungUpBroadcast::class.java)
                hungupIntent.putExtra("action", "rejectCall")
                hungupIntent.putExtra(DEVICE_PAYLOAD, CommonData.deviceDetails(context).toString())
                hungupIntent.putExtra(INVITE_LINK, messageJson.optString(INVITE_LINK))
                hungupIntent.putExtra(CHANNEL_ID, messageJson.optLong(CHANNEL_ID))
                context.sendBroadcast(hungupIntent)
            } else {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (!OngoingCallService.NotificationServiceState.isConferenceServiceRunning
                        && (messageJson.getString(VIDEO_CALL_TYPE).equals(WebRTCCallConstants.JitsiCallType.OFFER_CONFERENCE.toString())
                                || messageJson.getString(VIDEO_CALL_TYPE).equals(JitsiCallType.START_GROUP_CALL.toString()))
                        && messageJson.getString(INVITE_LINK).equals(OngoingCallService.NotificationServiceState.inviteLink)) {

                        val startIntent = Intent(context, OngoingCallService::class.java)
                        startIntent.action = "com.hippochat.notification.start"
                        startIntent.putExtra(INCOMING_VIDEO_CONFERENCE, true)

                        startIntent.putExtra(BASE_URL, CONFERENCING_LIVE)
                        startIntent.putExtra(JITSI_URL, HippoCallConfig.getInstance().jitsiURL)
                        startIntent.putExtra(ROOM_NAME, linkArray[linkArray.size - 1])
                        startIntent.putExtra(CALL_TYPE, messageJson.optString(CALL_TYPE))
                        startIntent.putExtra(FULL_NAME, messageJson.optString(FULL_NAME))
                        startIntent.putExtra(USER_THUMBNAIL_IMAGE, messageJson.optString(USER_THUMBNAIL_IMAGE))
                        startIntent.putExtra(INVITE_LINK, messageJson.optString(INVITE_LINK))
                        startIntent.putExtra(CHANNEL_ID, messageJson.optLong(CHANNEL_ID))
                        startIntent.putExtra(MESSAGE_UNIQUE_ID, messageJson.optString(MESSAGE_UNIQUE_ID))

                        videoCallModel.roomName = linkArray[linkArray.size - 1]
                        videoCallModel.inviteLink = messageJson.optString(INVITE_LINK)
                        videoCallModel.fullName = messageJson.optString(FULL_NAME)
                        videoCallModel.channelId = messageJson.optLong(CHANNEL_ID)

                        startIntent.putExtra("videoCallModel", videoCallModel)
                        ContextCompat.startForegroundService(context, startIntent)

                        HippoCallConfig.getInstance().startTimerTask();
                        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).startIncomingRinger()
                    }
                } else {
                    val mngr = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val taskList = mngr.getRunningTasks(10)


                    if (taskList[0].topActivity!!.className != "com.hippocall.MainCallingActivity"
                        && !taskList[0].topActivity!!.className.contains("GrantPermissionsActivity")
                        && (messageJson.getString(VIDEO_CALL_TYPE).equals(JitsiCallType.OFFER_CONFERENCE.toString())
                                || messageJson.getString(VIDEO_CALL_TYPE).equals(JitsiCallType.START_GROUP_CALL.toString()))
                        && messageJson.getString(INVITE_LINK).equals(OngoingCallService.NotificationServiceState.inviteLink)) {
                        videoCallModel.roomName = linkArray[linkArray.size - 1]
                        videoCallModel.inviteLink = messageJson.optString(INVITE_LINK)
                        videoCallModel.fullName = messageJson.optString(FULL_NAME)
                        videoCallModel.channelId = messageJson.optLong(CHANNEL_ID)

                        val startIntent = Intent(context, MainCallingActivity::class.java)
                        startIntent.action = "com.hippochat.notification.start"
                        startIntent.putExtra(INCOMING_VIDEO_CONFERENCE, true)
                        startIntent.putExtra(BASE_URL, CONFERENCING_LIVE)
                        startIntent.putExtra(JITSI_URL, HippoCallConfig.getInstance().jitsiURL)
                        startIntent.putExtra(ROOM_NAME, linkArray[linkArray.size - 1])
                        startIntent.putExtra(CALL_TYPE, messageJson.optString(CALL_TYPE))
                        startIntent.putExtra(FULL_NAME, messageJson.optString(FULL_NAME))
                        startIntent.putExtra(USER_THUMBNAIL_IMAGE, messageJson.optString(USER_THUMBNAIL_IMAGE))
                        startIntent.putExtra(INVITE_LINK, messageJson.optString(INVITE_LINK))
                        startIntent.putExtra(CHANNEL_ID, messageJson.optLong(CHANNEL_ID))
                        startIntent.putExtra(MESSAGE_UNIQUE_ID, messageJson.optString(MESSAGE_UNIQUE_ID))
                        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        startIntent.putExtra("videoCallModel", videoCallModel)
                        startIntent.putExtra("startGroupCall", videoCallModel.hasGroupCall)
                        startIntent.putExtra("incomming_call", "incomming_call")
                        context.startActivity(startIntent)
                    }
                }
            }
        }
    }
}