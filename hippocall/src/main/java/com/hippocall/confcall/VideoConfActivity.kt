package com.hippocall.confcall

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hippo.BuildConfig
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.*
import com.hippo.langs.Restring
import com.hippo.utils.UniqueIMEIID
import com.hippocall.*
import com.hippocall.WebRTCCallConstants.Companion.CALL_TYPE
import faye.ConnectionManager
import faye.FayeClient
import io.paperdb.Paper
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import org.jitsi.meet.sdk.log.JitsiMeetLogger
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL

public class VideoConfActivity : AppCompatActivity() {

    private var wiredHeadsetReceiver: BroadcastReceiver? = null
    private val STATE_UNPLUGGED = 0
    private val STATE_PLUGGED = 1
    private val HAS_NO_MIC = 0
    private val HAS_MIC = 1
    private var isWirelessHeadSetConnected = false
    private var dialog: Dialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_black)
        val win = window
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        stopForegroundService()
        // Initialize default options for Jitsi Meet conferences.
        OngoingCallService.NotificationServiceState.isConferenceConnected = true
        if (intent.hasExtra(CHANNEL_ID) && !intent.hasExtra("no_answer")) {
            answerConference()
        }
        val serverURL: URL = getServerUrl()
        initCall(serverURL)

        startOngoingCallService()
        try {
            apiUpdateConferenceCall()
        } catch (e: Exception) {

        }
        //ConnectionManager.initConnection()

        finish()
        wiredHeadsetReceiver = WiredHeadsetReceiver()
    }

    private fun apiUpdateConferenceCall() {
        /*val commonParams = CommonParams.Builder()
        if (intent.hasExtra(INVITE_LINK)) {
            commonParams.add("calling_link", intent.getStringExtra(INVITE_LINK))
        } else {
            commonParams.add("calling_link", intent.data.toString().replace("fuguChat://","https://"))
        }
        try {
            commonParams.add("user_id_in_call", MyApplication.getInstance().userData.userId)
        } catch (e: java.lang.Exception) {

        }
        RestClient.getApiInterface().updateConferenceCall(1, BuildConfig.VERSION_CODE, commonParams.build().map)
                .enqueue(object : ResponseResolver<CommonResponse>() {
                    override fun success(t: CommonResponse?) {
                    }

                    override fun failure(error: APIError?) {
                    }

                })*/
    }

    private fun startOngoingCallService() {
        val startIntent = Intent(this, OngoingCallService::class.java)
        startIntent.action = "com.hippochat.notification.start"
        startIntent.putExtra(INVITE_LINK, intent.getStringExtra(INVITE_LINK))
        startIntent.putExtra(CHANNEL_ID, intent.getLongExtra(CHANNEL_ID, -1L))
        ContextCompat.startForegroundService(this, startIntent)
    }

    private fun initCall(serverURL: URL) {
        val userInfo = JitsiMeetUserInfo()
        try {

            userInfo.displayName = "" //data.userInfo.fullName
            userInfo.avatar = URL("")
        } catch (e: Exception) {
            userInfo.displayName = "Fellow User"
        }


        var roomName = ""
        if (!TextUtils.isEmpty(intent.getStringExtra("room_name"))) {
            roomName = intent.getStringExtra("room_name")!!
        } else {
            roomName = intent?.data?.lastPathSegment!!

        }

        try {
            if ((intent.hasExtra("call_type") && intent.getStringExtra("call_type") == "AUDIO")
                    || (intent.hasExtra(INVITE_LINK) && intent.getStringExtra(INVITE_LINK)!!.contains("#config.startWithVideoMuted=true"))
                    || roomName.contains("#config.startWithVideoMuted=true")) {
                val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setAudioOnly(true)
                        .setFeatureFlag("chat.enabled", false)
                        .setUserInfo(userInfo)
                        .build()
                JitsiMeet.setDefaultConferenceOptions(defaultOptions)
            } else {
                val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setAudioOnly(false)
                        .setFeatureFlag("chat.enabled", false)
                        .setUserInfo(userInfo)
                        .build()
                JitsiMeet.setDefaultConferenceOptions(defaultOptions)
            }
        } catch (e: Exception) {
            val defaultOptions = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setWelcomePageEnabled(false)
                    .setAudioOnly(false)
                    .setFeatureFlag("chat.enabled", false)
                    .setUserInfo(userInfo)
                    .build()
            JitsiMeet.setDefaultConferenceOptions(defaultOptions)
        }
        roomName = roomName.replace("#config.startWithVideoMuted=true", "")
        val options = JitsiMeetConferenceOptions.Builder()
                .setRoom(roomName)
                .build()
        JitsiMeetActivity.launch(this, options, Restring.getString(this, R.string.hippo_calling_connection))
    }

    private fun getServerUrl(): URL {
        var serverURL: URL
        if (!TextUtils.isEmpty(HippoCallConfig.getInstance().jitsiURL)) {
            try {
                serverURL = URL(HippoCallConfig.getInstance().jitsiURL)
                return serverURL
            } catch (e: java.lang.Exception) {
            }
        }
        try {
            serverURL = URL(FuguAppConstant.CONFERENCING_LIVE)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            throw RuntimeException("Invalid server URL!")
        }
        return serverURL

    }

    private fun answerConference() {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.ANSWER_CONFERENCE.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, intent?.getLongExtra(CHANNEL_ID, -1L))
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
        startCallJson.put(CALL_TYPE, "VIDEO")
        startCallJson.put(MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, intent?.getStringExtra(INVITE_LINK))
        //ConnectionManager.sendMessage(intent?.getLongExtra(CHANNEL_ID, -1L)!!, startCallJson)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)




        sendMsg(intent?.getLongExtra(CHANNEL_ID, -1L)!!, startCallJson)

        /*try {
            Handler().postDelayed({
                try {
                    ConnectionManager.sendMessage(intent?.getLongExtra(CHANNEL_ID, -1L)!!, startCallJson)
                } catch (e: Exception) {

                }
            }, 1000)
        } catch (e: Exception) {

        }*/
    }

    private fun stopForegroundService() {
        try {
            val startIntent = Intent(this@VideoConfActivity, VideoCallService::class.java)
            startIntent.action = "com.hippochat.start"
            startIntent.putExtra("isHungUpToBeSent", false)
            stopService(startIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    override fun onResume() {
        super.onResume()
        //LocalBroadcastManager.getInstance(this@VideoConfActivity).registerReceiver(mCallTerminated, IntentFilter("CALL TERMINATED"))

    }

    private val mCallTerminated = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //showFeedBackDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //LocalBroadcastManager.getInstance(this@VideoConfActivity).unregisterReceiver(mCallTerminated)
    }

    private fun showFeedBackDialog() {
        try {
            dialog = Dialog(this@VideoConfActivity, R.style.Theme_AppCompat_Translucent)
            dialog?.setContentView(R.layout.activity_calling_feed_back)
            val lp = dialog?.window!!.attributes
            lp.dimAmount = 0.5f
            dialog?.window!!.attributes = lp
            dialog?.window!!.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog?.setCancelable(false)
            dialog?.setCanceledOnTouchOutside(false)
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val ratingBar: CustomRatingBar? = dialog?.findViewById(R.id.ratingBar)
            val tvRating: TextView? = dialog?.findViewById(R.id.tvRating)
            val etFeedback: EditText? = dialog?.findViewById(R.id.etFeedback)
            val btnNotNow: AppCompatButton? = dialog?.findViewById(R.id.btnNotNow)
            val btnSubmit: AppCompatButton? = dialog?.findViewById(R.id.btnSubmit)

            ratingBar?.setOnScoreChanged { score ->

                if (score >= 0f) {
                    tvRating?.visibility = View.VISIBLE
                } else {
                    tvRating?.visibility = View.GONE
                }
                when (score) {

                    1f -> {
                        tvRating?.text = "Very Bad"
                    }
                    2f -> {
                        tvRating?.text = "Bad"
                    }
                    3f -> {
                        tvRating?.text = "Average"
                    }
                    4f -> {
                        tvRating?.text = "Good"
                    }
                    5f -> {
                        tvRating?.text = "Excellent"
                    }
                    else -> {

                    }
                }
            }


            btnNotNow?.setOnClickListener {
                dialog?.dismiss()
                //IncomingVideoConferenceActivity.IncomingCall.incomingConferenceStatus = false
                finish()
            }

            btnSubmit?.setOnClickListener {
                apiSendFeedback(ratingBar?.score!!, FuguAppConstant.Feedback.VIDEO_CONFERENCE.toString(), etFeedback?.text.toString().trim())
                dialog = null

            }

            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun apiSendFeedback(rating: Float, type: String, feedback: String) {

        /*val jsonObject = JSONObject()
        val gson = Gson()
        val json = gson.toJson(com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().getUserInfo())
        try {
            jsonObject.put("workspace_name", com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].workspaceName)
            jsonObject.put("workspace_id", com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].workspaceId)
            jsonObject.put("type", type)

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val commonParams = com.skeleton.mvp.data.network.CommonParams.Builder()
        if (!TextUtils.isEmpty(feedback)) {
            commonParams.add("feedback", feedback)
        }
        commonParams.add("workspace_id", com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].workspaceId)
        commonParams.add("type", type)
        commonParams.add("rating", rating.toInt())
        commonParams.add("extra_details", jsonObject.toString())

        RestClient.getApiInterface(true).sendFeedback(com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().getUserInfo().getAccessToken(), com.skeleton.mvp.BuildConfig.VERSION_CODE, FuguAppConstant.ANDROID, commonParams.build().map)
                .enqueue(object : ResponseResolver<CommonResponse>() {
                    override fun success(commonResponse: CommonResponse) {
                        Toast.makeText(this@VideoConfActivity, "Feedback Submitted", Toast.LENGTH_LONG).show()
                        IncomingVideoConferenceActivity.IncomingCall.incomingConferenceStatus = false
                        finishAndRemoveTask()
                    }

                    override fun failure(error: APIError) {
                        Toast.makeText(this@VideoConfActivity, error.message, Toast.LENGTH_LONG).show()
                        IncomingVideoConferenceActivity.IncomingCall.incomingConferenceStatus = false
                        finishAndRemoveTask()
                    }
                })*/
    }

    internal inner class WiredHeadsetReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra("state", STATE_UNPLUGGED)
            val microphone = intent.getIntExtra("microphone", HAS_NO_MIC)
            val name = intent.getStringExtra("name")

            isWirelessHeadSetConnected = state == STATE_PLUGGED
            if (isWirelessHeadSetConnected) {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.mode = AudioManager.MODE_IN_CALL
                am.isSpeakerphoneOn = false
                am.isWiredHeadsetOn = true
            } else {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.mode = AudioManager.MODE_IN_CALL
                am.isSpeakerphoneOn = true
            }
        }
    }

    private fun getDeviceDetails(): JSONObject {
        val devicePayload = JSONObject()
        devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(this))
        devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
        devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
        devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(this))
        return devicePayload
    }


    private fun sendMsg(channelId: Long, jsonObject: JSONObject) {
        ConnectionManager.publish("/$channelId", jsonObject)
        /*HippoConfig.getExistingClient {
                //client -> mClient = client
            it?.publish("/$channelId", jsonObject)
        }*/
    }
}
