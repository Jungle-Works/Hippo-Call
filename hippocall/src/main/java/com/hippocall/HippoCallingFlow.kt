package com.hippocall

import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.CHANNEL_ID
import com.hippo.constant.FuguAppConstant.MESSAGE_UNIQUE_ID
import com.hippo.eventbus.BusProvider
import com.hippo.helper.FayeMessage
import com.hippo.langs.Restring
import com.hippo.utils.HippoLog
import com.hippocall.confcall.HippoAudioManager
import com.hippocall.confcall.HungUpBroadcast
import com.hippocall.confcall.OngoingCallService
import com.hippocall.confcall.PushReceiver
import com.hippocall.model.FragmentFlow
import faye.ConnectionManager
import faye.ConnectionUtils
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Created by gurmail on 2020-04-28.
 * @author gurmail
 */
object HippoCallingFlow {

    var callStatus: Int = 0
    var answerFor: String = ""

    public fun callingFlow(data: JSONObject, msg: String?, channel: String?) {
        Log.e("VIDEO_CALL_TYPE", "VIDEO_CALL_TYPE = "+data.getString(FuguAppConstant.VIDEO_CALL_TYPE))
        when(data.getString(FuguAppConstant.VIDEO_CALL_TYPE)) {
            FuguAppConstant.JitsiCallType.START_CONFERENCE.toString() -> startConference(data)
            FuguAppConstant.JitsiCallType.OFFER_CONFERENCE.toString() -> offerConference(data)
            FuguAppConstant.JitsiCallType.REJECT_CONFERENCE.toString() -> rejectConference(data)
            FuguAppConstant.JitsiCallType.HUNGUP_CONFERENCE.toString() -> hungupConference(data)
            FuguAppConstant.JitsiCallType.USER_BUSY_CONFERENCE.toString() -> {
                if(!TextUtils.isEmpty(msg))
                    BusProvider.getInstance().post(FayeMessage(FuguAppConstant.FayeBusEvent.MESSAGE_RECEIVED.toString(), channel, msg))
                val finalUserId1 = HippoConfig.getInstance().userData.userId
                val deviceId = CommonData.getUniqueIMEIId(HippoCallConfig.getInstance().context)
                val remoteDeviceId = data.getJSONObject("device_payload").optString("device_id")
                if(finalUserId1.toInt() == data.optInt("user_id", -1) && deviceId != remoteDeviceId) {
                    val mIntent2 = Intent(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
                    mIntent2.putExtra(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
                    mIntent2.putExtra(FuguAppConstant.JITSI_URL, data.optString(FuguAppConstant.JITSI_URL))
                    LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent2)
                    if(ConnectionUtils.isMyServiceRunning(HippoCallConfig.getInstance().context, OngoingCallService::class.java)) {
                        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
                        val startIntent = Intent(HippoCallConfig.getInstance().context, OngoingCallService::class.java)
                        HippoCallConfig.getInstance().context.stopService(startIntent)
                    }
                    if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
                        ConnectionManager.onClose()
                    }
                    //OngoingCallService.CallState.status = 0
                }

            }
            FuguAppConstant.JitsiCallType.ANSWER_CONFERENCE.toString()  -> {
                val finalUserId1 = HippoConfig.getInstance().userData.userId
                val deviceId = CommonData.getUniqueIMEIId(HippoCallConfig.getInstance().context)
                var remoteDeviceId = ""
                try {
                    remoteDeviceId = data.getJSONObject("device_payload").optString("device_id")
                } catch (e: Exception) {
                }
                if(finalUserId1.toInt() == data.optInt("user_id", -1) && deviceId != remoteDeviceId) {
                    val mIntent2 = Intent(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
                    mIntent2.putExtra(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
                    mIntent2.putExtra(FuguAppConstant.JITSI_URL, data.optString(FuguAppConstant.JITSI_URL))
                    LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent2)
                    if(ConnectionUtils.isMyServiceRunning(HippoCallConfig.getInstance().context, OngoingCallService::class.java)) {
                        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
                        val startIntent = Intent(HippoCallConfig.getInstance().context, OngoingCallService::class.java)
                        HippoCallConfig.getInstance().context.stopService(startIntent)
                    }
                    if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
                        ConnectionManager.onClose()
                    }
                    //OngoingCallService.CallState.status = 0
                } else {

                    if(!TextUtils.isEmpty(msg))
                        BusProvider.getInstance().post(FayeMessage(FuguAppConstant.FayeBusEvent.MESSAGE_RECEIVED.toString(), channel, msg))
                }

            }
            WebRTCCallConstants.VideoCallType.START_CALL.toString()             -> HippoCallConfig.getInstance().oldSDKCall(msg, channel)
            WebRTCCallConstants.VideoCallType.VIDEO_OFFER.toString()            -> HippoCallConfig.getInstance().oldSDKCall(msg, channel)
            WebRTCCallConstants.VideoCallType.START_GROUP_CALL.toString()       -> openGroupCall(data)
            WebRTCCallConstants.VideoCallType.REJECT_GROUP_CALL.toString()      -> rejectGroupConference(data)
            WebRTCCallConstants.VideoCallType.END_GROUP_CALL.toString()         -> endSessionConference(data)
            WebRTCCallConstants.VideoCallType.JOIN_GROUP_CALL.toString()        -> closeConference(data)

            WebRTCCallConstants.JitsiCallType.READY_TO_CONNECT_CONFERENCE.toString()        -> {
                BusProvider.getInstance().post(FayeMessage(FuguAppConstant.FayeBusEvent.MESSAGE_RECEIVED.toString(), channel, msg))
                if(OngoingCallService.CallState.muid.equals(data.optString("muid"), ignoreCase = true)) {
                    OngoingCallService.CallState.readyToConnect = 1
                }
            }
            else -> {
                if(!TextUtils.isEmpty(msg)) {
                    BusProvider.getInstance().post(FayeMessage(FuguAppConstant.FayeBusEvent.MESSAGE_RECEIVED.toString(), channel, msg))
                }
            }
        }
    }


    //
    private fun startConference(data: JSONObject) {
        val finalUserId1 = HippoConfig.getInstance().userData.userId
        println(finalUserId1.compareTo(data.optLong("user_id", -1)) != 0)
        println(!OngoingCallService.NotificationServiceState.isConferenceConnected)

        if(finalUserId1.compareTo(data.optLong("user_id", -1)) != 0
            && !OngoingCallService.NotificationServiceState.isConferenceConnected) {
            var flag = true
//
//            println("1111111    "+OngoingCallService.CallState.muid)
//            println("2222222    "+data.optString("muid"))
//            println("3333333    "+data.optString("muid"))
//            println("4444444    "+(OngoingCallService.CallState.readyToConnect == 1))

            if(OngoingCallService.CallState.muid.equals(data.optString("muid"), ignoreCase = true) && OngoingCallService.CallState.readyToConnect == 1) {
                flag = false
            }
            if(flag) {
                emitReadyToConnect(data, finalUserId1.toInt())
            }
            return
        }
//        println(">>>>>>>>>>>>>>>>")
//        println(OngoingCallService.NotificationServiceState.isConferenceConnected)
//        println(answerFor)
//        println(!answerFor.equals(data.optString("muid")))
//        println("<<<<<<<<<<<<<<<<")

        if(OngoingCallService.NotificationServiceState.isConferenceConnected && !answerFor.equals(data.optString("muid"))) {
            emitUserBusy(data, finalUserId1.toInt())
        }
    }

    private fun offerConference(data: JSONObject) {
        val finalUserId1 = HippoConfig.getInstance().getUserData().getUserId()
        if(finalUserId1.compareTo(data.optLong("user_id", -1)) != 0
            && !OngoingCallService.NotificationServiceState.isConferenceConnected) {
            val turnCredentials = AppContants().turnCredentials
            val myName = HippoConfig.getInstance().getUserData().fullName
            val myImage = com.hippo.database.CommonData.getImagePath()

            var chnl: Long = 0
            val channel = data.optString("channel")

            if(!TextUtils.isEmpty(channel) && !data.has("channel_id")) {
                chnl = channel.replace("/", "").toLong()
                //val channelId = data.optLong("channel_id")
            }
            val userId = data.optLong("user_id", finalUserId1)
            val fullname = data.optString("full_name")
            val channelId = data.optLong("channel_id", chnl)
            val messageUniqueId = data.optString("muid")
            val videoCallType = data.optString("video_call_type", "")

            val callType = data.optString("call_type", "")
            val activityLaunchState = WebRTCCallConstants.AcitivityLaunchState.OTHER.toString()
            val userImage = data.optString("user_thumbnail_image")
            val jitsiUrl = data.optString(FuguAppConstant.JITSI_URL)

            val videoCallModel = VideoCallModel(channelId,
                data.optString("user_thumbnail_image"),
                data.optString("full_name"),
                userId,
                -1,
                fullname,
                turnCredentials.getTurnApiKey(),
                turnCredentials.getUsername(),
                turnCredentials.getCredentials(),
                ArrayList(),
                ArrayList(),
                activityLaunchState,
                messageUniqueId,
                callType.toUpperCase(), "", jitsiUrl, "",
                myName, myImage, false, false)

            if(OngoingCallService.NotificationServiceState.muid == data.getString(MESSAGE_UNIQUE_ID))
                return
            OngoingCallService.NotificationServiceState.muid = data.getString(MESSAGE_UNIQUE_ID)
            OngoingCallService.NotificationServiceState.inviteLink = data.getString("invite_link")
            val fuguNotificationConfig = PushReceiver().getInstance();
            fuguNotificationConfig.incomingCallNotification(HippoCallConfig.getInstance().context, data, videoCallModel)
        }
    }

    private fun closeConference(data: JSONObject) {
        val finalUserId1 = HippoConfig.getInstance().userData.userId
        val deviceId = CommonData.getUniqueIMEIId(HippoCallConfig.getInstance().context)
        val remoteDeviceId = data.getJSONObject("device_payload").optString("device_id")
        if(finalUserId1.toInt() == data.optInt("user_id", -1) && deviceId != remoteDeviceId) {
            val mIntent2 = Intent(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
            mIntent2.putExtra(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent2)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
                val startIntent = Intent(HippoCallConfig.getInstance().context, OngoingCallService::class.java)
                HippoCallConfig.getInstance().context.stopService(startIntent)
            }
            if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
                ConnectionManager.onClose()
            }
        }
    }

    private fun endSessionConference(data: JSONObject) {
        //HippoCallConfig.getInstance().stopTimerTask()
        if(data.optString("video_call_type").equals("END_GROUP_CALL")) {
            if(HippoConfig.getInstance().groupSessionListener != null){
                HippoConfig.getInstance().groupSessionListener.onSessionEnded(data.optString("transaction_id"))
            }
        }
        if (OngoingCallService.NotificationServiceState.transactionId == data.optString("transaction_id")) {
            val hungupIntent = Intent(HippoCallConfig.getInstance().context, HungUpBroadcast::class.java)
            hungupIntent.putExtra("action", "endSession")
            HippoCallConfig.getInstance().context.sendBroadcast(hungupIntent)
            val mIntent = Intent("CALL_HANGUP")
            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent)

            val mIntent2 = Intent(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
            mIntent2.putExtra(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent2)

            if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
                ConnectionManager.onClose()
            }

            HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
        }
//
//        if (OngoingCallService.NotificationServiceState.transactionId == data.optString("transaction_id")) {
//            val hungupIntent = Intent(HippoCallConfig.getInstance().context, HungUpBroadcast::class.java)
//            hungupIntent.putExtra("action", "endSession")
//            HippoCallConfig.getInstance().context.sendBroadcast(hungupIntent)
//            val mIntent = Intent("CALL_HANGUP")
//            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent)
//
//            val mIntent2 = Intent(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
//            mIntent2.putExtra(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
//            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent2)
//        } else if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
//            ConnectionManager.onClose()
//        }
//
//        if(data.optString("video_call_type").equals("END_GROUP_CALL")) {
//            if(HippoConfig.getInstance().groupSessionListener != null){
//                HippoConfig.getInstance().groupSessionListener.onSessionEnded(data.optString("transaction_id"))
//            }
//        }
//
//        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
    }

    private fun rejectConference(data: JSONObject) {
        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
        HippoCallConfig.getInstance().stopTimerTask()
        if(ConnectionUtils.isMyServiceRunning(HippoCallConfig.getInstance().context, OngoingCallService::class.java)) {
            val startIntent = Intent(HippoCallConfig.getInstance().context, OngoingCallService::class.java)
            HippoCallConfig.getInstance().context.stopService(startIntent)
        } else if (OngoingCallService.NotificationServiceState.inviteLink == data.optString("invite_link")) {
            BusProvider.getInstance().post(FayeMessage(WebRTCCallConstants.BusFragmentType.CALL_HUNGUP.toString(), "", ""))
            if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
                ConnectionManager.onClose()
            }
        }
    }

    private fun rejectGroupConference(data: JSONObject) {
        if(data.optLong("user_id") != com.hippo.database.CommonData.getUserDetails().data.userId) {
            return
        }
        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
        HippoCallConfig.getInstance().stopTimerTask()
        if(ConnectionUtils.isMyServiceRunning(HippoCallConfig.getInstance().context, OngoingCallService::class.java)) {
            val startIntent = Intent(HippoCallConfig.getInstance().context, OngoingCallService::class.java)
            HippoCallConfig.getInstance().context.stopService(startIntent)
        } else if (OngoingCallService.NotificationServiceState.inviteLink == data.optString("invite_link")) {
            BusProvider.getInstance().post(FayeMessage(WebRTCCallConstants.BusFragmentType.CALL_HUNGUP.toString(), "", ""))
            if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
                ConnectionManager.onClose()
            }
        }
    }

    private fun hungupConference(data: JSONObject) {
        //OngoingCallService.CallState.status = 0
        HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
        HippoCallConfig.getInstance().stopTimerTask()
        if (OngoingCallService.NotificationServiceState.inviteLink == data.optString("invite_link")) {
            val hungupIntent = Intent(HippoCallConfig.getInstance().context, HungUpBroadcast::class.java)
            hungupIntent.putExtra("action", "rejectCall")
            hungupIntent.putExtra(FuguAppConstant.MESSAGE_UNIQUE_ID, data.getString(FuguAppConstant.MESSAGE_UNIQUE_ID))
            HippoCallConfig.getInstance().context.sendBroadcast(hungupIntent)
            val mIntent = Intent("CALL_HANGUP")
            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent)

            val mIntent2 = Intent(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
            mIntent2.putExtra(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
            LocalBroadcastManager.getInstance(HippoCallConfig.getInstance().context).sendBroadcast(mIntent2)
        } else if (!ConnectionUtils.isAppRunning(HippoCallConfig.getInstance().context)) {
            ConnectionManager.onClose()
        } else {
            // do nothing
        }

    }

    private fun emitReadyToConnect(data: JSONObject, userId: Int?) {
        try {
            OngoingCallService.CallState.muid = data.getString(FuguAppConstant.MESSAGE_UNIQUE_ID)
            val startCallJson = JSONObject()
            startCallJson.put(FuguAppConstant.IS_SILENT, true)
            startCallJson.put(FuguAppConstant.VIDEO_CALL_TYPE, FuguAppConstant.JitsiCallType.READY_TO_CONNECT_CONFERENCE)
            startCallJson.put(FuguAppConstant.USER_ID, userId)
            startCallJson.put(FuguAppConstant.CHANNEL_ID, data.getString(FuguAppConstant.CHANNEL_ID))
            startCallJson.put(FuguAppConstant.MESSAGE_TYPE, FuguAppConstant.VIDEO_CALL)
            startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
            try {
                startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, CommonData.deviceDetails(HippoCallConfig.getInstance().context))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            startCallJson.put(FuguAppConstant.INVITE_LINK, data.getString(FuguAppConstant.INVITE_LINK))
            startCallJson.put(FuguAppConstant.JITSI_URL, data.optString(FuguAppConstant.JITSI_URL))
            startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, data.getString(FuguAppConstant.MESSAGE_UNIQUE_ID))

            startCallJson.put(WebRTCCallConstants.FULL_NAME, data.optString("full_name", ""))
            startCallJson.put("message", "")
            startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)

            val channelId = "/" + data.optLong(FuguAppConstant.CHANNEL_ID)
            ConnectionManager.publish(channelId, startCallJson)
            //HippoLog.e("Video_CONF-->", startCallJson.toString())
        } catch (e: Exception) {

        }
    }

    private fun emitUserBusy(jsonObject: JSONObject, userId: Int?) {
        try {
            val startCallJson = JSONObject()
            startCallJson.put(FuguAppConstant.IS_SILENT, false)
            startCallJson.put(FuguAppConstant.VIDEO_CALL_TYPE, FuguAppConstant.JitsiCallType.USER_BUSY_CONFERENCE)
            startCallJson.put(FuguAppConstant.USER_ID, userId)
            startCallJson.put(FuguAppConstant.CHANNEL_ID, jsonObject.optString(FuguAppConstant.CHANNEL_ID))
            startCallJson.put(FuguAppConstant.MESSAGE_TYPE, FuguAppConstant.VIDEO_CALL)
            startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
            startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, CommonData.deviceDetails(HippoCallConfig.getInstance().context))
            startCallJson.put(FuguAppConstant.INVITE_LINK, jsonObject.optString(FuguAppConstant.INVITE_LINK))
            startCallJson.put(FuguAppConstant.JITSI_URL, jsonObject.optString(FuguAppConstant.JITSI_URL))
            startCallJson.put(
                FuguAppConstant.MESSAGE_UNIQUE_ID, jsonObject.optString(
                    FuguAppConstant.MESSAGE_UNIQUE_ID))

            startCallJson.put(WebRTCCallConstants.FULL_NAME, jsonObject.optString("full_name", ""))
            startCallJson.put("message", "")
            startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)

            val channelId = "/"+jsonObject.optLong(CHANNEL_ID)
            ConnectionManager.publish(channelId, startCallJson)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun removeCallView(data: JSONObject) {

    }

    var msgFlag = false
    private fun openGroupCall(data: JSONObject) {
        val finalUserId1 = HippoConfig.getInstance().getUserData().getUserId()
        if(finalUserId1.compareTo(data.optLong("user_id", -1)) != 0
            && !OngoingCallService.NotificationServiceState.isConferenceConnected) {
            val turnCredentials = AppContants().turnCredentials
            val myName = HippoConfig.getInstance().getUserData().fullName
            val myImage = com.hippo.database.CommonData.getImagePath()


            val userId = data.optLong("user_id", finalUserId1)
            val fullname = data.optString("full_name")
            val channelId = data.optLong("channel_id")
            val messageUniqueId = data.optString("muid")
            val videoCallType = data.optString("video_call_type", "")

            val callType = data.optString("call_type", "")
            val activityLaunchState = WebRTCCallConstants.AcitivityLaunchState.OTHER.toString()
            val userImage = data.optString("user_thumbnail_image")
            val jitsiUrl = data.optString("jitsi_url", "")

            var isVideoEnabled = data.optBoolean("is_video", true)
            var isAudioEnable = false//data.optBoolean("is_audio", true)

            if(jitsiUrl.contains("config.startWithVideoMuted=true", ignoreCase = true))
                isVideoEnabled = false
            if(jitsiUrl.contains("config.startWithAudioMuted=true", ignoreCase = true))
                isAudioEnable = true



            var message = data.optString("message", "")
            if(data.has("multi_lang_message")) {
                val pattern = Pattern.compile("\\{\\{\\{(.*?)\\}\\}\\}")
                val matcher = pattern.matcher(data.optString("multi_lang_message", ""))
                if (matcher.find()) {
                    val key = matcher.group(1)
                    val value = Restring.getString(key)
                    if (!TextUtils.isEmpty(value)) {
                        val oldStr = "{{{$key}}}"
                        message = data.optString("multi_lang_message", "").replace(oldStr, value)
                    }
                }
            }


            val videoCallModel = VideoCallModel(channelId,
                data.optString("user_thumbnail_image"),
                message,
                userId,
                -1,
                fullname,
                turnCredentials.getTurnApiKey(),
                turnCredentials.getUsername(),
                turnCredentials.getCredentials(),
                ArrayList(),
                ArrayList(),
                activityLaunchState,
                messageUniqueId,
                callType.toUpperCase(), "", jitsiUrl, "",
                myName, myImage, true, isVideoEnabled, isAudioEnable)

            if(TextUtils.isEmpty(message)) {
                msgFlag = true
            }

            if(OngoingCallService.NotificationServiceState.muid == data.getString(MESSAGE_UNIQUE_ID)) {
                if(msgFlag && !TextUtils.isEmpty(message)) {
                    msgFlag = false
                    BusProvider.getInstance().post(FragmentFlow(WebRTCCallConstants.BusFragmentType.UPDATE_INCOMIMG_CONFIG.toString(),
                    1, null, message))
                }
                return
            }



            if(HippoCallConfig.getInstance().listener != null)
                HippoCallConfig.getInstance().listener.callStatus(1)

            OngoingCallService.NotificationServiceState.muid = data.getString(MESSAGE_UNIQUE_ID)
            OngoingCallService.NotificationServiceState.inviteLink = data.getString("invite_link")
            OngoingCallService.NotificationServiceState.transactionId = data.optString("transaction_id")
            OngoingCallService.NotificationServiceState.hasGroupCall = true
            val fuguNotificationConfig = PushReceiver().getInstance();
            fuguNotificationConfig.incomingCallNotification(HippoCallConfig.getInstance().context, data, videoCallModel)
        }
        if(HippoConfig.getInstance().groupSessionListener != null) {
            HippoConfig.getInstance().groupSessionListener.onStartSession(data.optString("transaction_id", ""))
        }

    }



}