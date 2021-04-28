package com.hippocall.confcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hippo.BuildConfig
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.langs.Restring
import com.hippo.utils.UniqueIMEIID
import com.hippocall.CommonData
import com.hippocall.HippoCallConfig
import com.hippocall.R
import com.hippocall.WebRTCCallConstants
import com.hippocall.WebRTCCallConstants.Companion.DEVICE_PAYLOAD
import faye.ConnectionManager
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.json.JSONObject

class HungUpBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent?.getStringExtra("action").equals("rejectCall")) {
            val startIntent = Intent(context, OngoingCallService::class.java)
            context?.stopService(startIntent)
            HippoCallConfig.getInstance().stopTimerTask()
            val userId = com.hippo.database.CommonData.getUserDetails().data.userId
            val devicePayload = JSONObject()
            try {
                devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(context))
                devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
                devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
                devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(context))
            } catch (e: Exception) {
                e.printStackTrace()
            }


            val startCallJson = JSONObject()
            startCallJson.put(FuguAppConstant.IS_SILENT, true)
            if(intent?.getBooleanExtra("has_group_call", false)!!) {
                startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.REJECT_GROUP_CALL.toString())
                startCallJson.put(WebRTCCallConstants.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
            } else {
                startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.REJECT_CONFERENCE.toString())
                startCallJson.put(WebRTCCallConstants.MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
            }
            startCallJson.put(FuguAppConstant.USER_ID, userId)
            startCallJson.put(FuguAppConstant.CHANNEL_ID, intent?.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L))

            startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
            startCallJson.put(WebRTCCallConstants.MESSAGE_UNIQUE_ID, intent?.getStringExtra(FuguAppConstant.MESSAGE_UNIQUE_ID))
            startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, devicePayload)
            startCallJson.put(WebRTCCallConstants.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(context))
            startCallJson.put(FuguAppConstant.INVITE_LINK, intent?.getStringExtra(FuguAppConstant.INVITE_LINK))

            startCallJson.put("message", "")
            startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)

            if (intent?.hasExtra(DEVICE_PAYLOAD)!!) {
                //if(!OngoingCallService.NotificationServiceState.hasGroupCall)
                    HippoCallConfig.getInstance().sendMessage(intent?.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), startCallJson)
            }

            if(intent.getBooleanExtra("has_group_call", false)) {
                startCallJson.put("server_push", true)
                ConnectionManager.publish("/"+com.hippo.database.CommonData.getUserDetails().data.userChannel, startCallJson)
            }

            HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
            OngoingCallService.NotificationServiceState.isConferenceServiceRunning = false
            OngoingCallService.NotificationServiceState.isConferenceConnected = false
            OngoingCallService.CallState.readyToConnect = 0
            OngoingCallService.CallState.muid = ""
        }  else if(intent?.getStringExtra("action").equals("hungupCall")) {
            var hasGroupCall: Boolean = false
            if(OngoingCallService.NotificationServiceState.hasGroupCall) {
                hasGroupCall = true
            }

            try {
                val mIntent = Intent("CALL_HANGUP")
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(mIntent)
            } catch (e: Exception) {
            }

            val startIntent = Intent(context, OngoingCallService::class.java)
            context?.stopService(startIntent)
            HippoCallConfig.getInstance().stopTimerTask()

            val userId = com.hippo.database.CommonData.getUserDetails().data.userId
            val devicePayload = JSONObject()
            try {
                devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(context))
                devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
                devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
                devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(context))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val startCallJson = JSONObject()
            startCallJson.put(FuguAppConstant.IS_SILENT, true)
            startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.HUNGUP_CONFERENCE.toString())
            startCallJson.put(FuguAppConstant.USER_ID, userId)
            startCallJson.put(FuguAppConstant.CHANNEL_ID, intent?.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L))
            startCallJson.put(WebRTCCallConstants.MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
            startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
            startCallJson.put(WebRTCCallConstants.MESSAGE_UNIQUE_ID, intent?.getStringExtra(FuguAppConstant.MESSAGE_UNIQUE_ID))
            startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, devicePayload)
            startCallJson.put(WebRTCCallConstants.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(context))
            startCallJson.put(FuguAppConstant.INVITE_LINK, intent?.getStringExtra(FuguAppConstant.INVITE_LINK))

            startCallJson.put("message", "")
            startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
            if (!hasGroupCall && intent?.hasExtra(DEVICE_PAYLOAD)!!) {
                HippoCallConfig.getInstance().sendMessage(intent?.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), startCallJson)
            }
        } else if(intent?.getStringExtra("action").equals("endSession")) {
            val startIntent = Intent(context, OngoingCallService::class.java)
            context?.stopService(startIntent)
            HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
            OngoingCallService.NotificationServiceState.isConferenceServiceRunning = false
            OngoingCallService.NotificationServiceState.isConferenceConnected = false
            OngoingCallService.NotificationServiceState.transactionId = ""
            OngoingCallService.NotificationServiceState.hasGroupCall = false
            OngoingCallService.NotificationServiceState.channelId = -1L
            OngoingCallService.NotificationServiceState.muid = ""
            OngoingCallService.NotificationServiceState.inviteLink = ""
            OngoingCallService.CallState.readyToConnect = 0
            OngoingCallService.CallState.muid = ""
        } else if(intent?.getStringExtra("action").equals("openCall")) {
            JitsiMeetActivity.launch(HippoCallConfig.getInstance().context, intent?.getStringExtra(FuguAppConstant.INVITE_LINK), Restring.getString(HippoCallConfig.getInstance().context, R.string.hippo_calling_connection))
        } else if(intent?.getStringExtra("action").equals("leaveSession")) {
            var hasGroupCall: Boolean = false
            if(OngoingCallService.NotificationServiceState.hasGroupCall) {
                hasGroupCall = true
            }
            if(OngoingCallService.NotificationServiceState.transactionId != intent?.getStringExtra("transactionId")) {
                return
            }

            try {
                val mIntent = Intent("CALL_HANGUP")
                LocalBroadcastManager.getInstance(context!!).sendBroadcast(mIntent)
            } catch (e: Exception) {
            }

            val startIntent = Intent(context, OngoingCallService::class.java)
            context?.stopService(startIntent)
            HippoCallConfig.getInstance().stopTimerTask()

        }
    }

}