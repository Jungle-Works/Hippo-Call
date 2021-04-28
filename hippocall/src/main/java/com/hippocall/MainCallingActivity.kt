package com.hippocall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.hippo.BuildConfig
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.eventbus.BusProvider
import com.hippo.helper.BusEvents
import com.hippo.helper.FayeMessage
import com.hippo.langs.Restring
import com.hippo.model.FuguCreateConversationParams
import com.hippo.model.FuguCreateConversationResponse
import com.hippo.retrofit.APIError
import com.hippo.retrofit.ResponseResolver
import com.hippo.retrofit.RestClient
import com.hippo.utils.HippoLog
import com.hippo.utils.UniqueIMEIID
import com.hippocall.confcall.*
import com.hippocall.model.FragmentFlow
import com.hippocall.model.OnCreateChannel
import com.squareup.otto.Subscribe
import faye.ConnectionManager
import kotlinx.android.synthetic.main.new_hippo_activity_video_call.*
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by gurmail on 2020-04-09.
 * @author gurmail
 */

class MainCallingActivity: AppCompatActivity() {

    private var videoCallModel: VideoCallModel? = null
    private var needToStartCall: Boolean = false
    private var initOldCall = false
    private var isHungup = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hippo_activity_maincalling)
        val win = window
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        if (intent.hasExtra("videoCallModel")) {
            videoCallModel = intent.extras?.getParcelable<VideoCallModel>("videoCallModel")!! as VideoCallModel
        }

        ConnectionManager.initFayeConnection()

        if (intent.hasExtra(FuguAppConstant.PEER_CHAT_PARAMS)) {
            var fuguCreateConversationParams = Gson().fromJson(intent
                .getStringExtra(FuguAppConstant.PEER_CHAT_PARAMS), FuguCreateConversationParams::class.java)
            createConversation(fuguCreateConversationParams)
            openJitsiFragment(true)
        } else {
            if(videoCallModel != null && videoCallModel?.hasGroupCall!!) {
                if(intent.hasExtra("answer_call")) {
                    HippoCallConfig.getInstance().stopTimerTask()
                    answerGroupCall()
                    stopForegroundService()
                    HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
                    OngoingCallService.NotificationServiceState.isConferenceConnected = true

                    val serverURL: URL = getIndiaServerUrl()
                    joinGroupCall(serverURL)
                    startOngoingCallService()
                    finish()
                } else {
                    openIncommingGroupFragment()
                    startMedia()
                }

            } else {
                initOldCall = false
                if (videoCallModel != null) {
                    if(intent.hasExtra("answer_call")) {
                        needToStartCall = false
                        HippoCallConfig.getInstance().stopTimerTask()

                    } else if(intent.hasExtra("incomming_call")) {
                        // check the link if empty don't do anything
                        needToStartCall = false
                    } else {
                        needToStartCall = true
                        val linkArray = randomVideoConferenceLink()
                        var jistsiLink = HippoCallConfig.getInstance().jitsiURL + "/" + linkArray[1]
                        var inviteLink = linkArray[0] + "/" + linkArray[1]
                        if (videoCallModel?.callType!! == "AUDIO") {
                            jistsiLink += "#config.startWithVideoMuted=true"
                        }

                        if (videoCallModel?.callType!! == "AUDIO") {
                            inviteLink += "#config.startWithVideoMuted=true"
                        }
                        videoCallModel?.jitsiLink = jistsiLink
                        videoCallModel?.inviteLink = inviteLink
                    }
                }


                ConnectionManager.subScribeChannel("/${videoCallModel?.channelId!!}")

                if(intent.hasExtra("incomming_call")) {
                    openIncommingCallFragment()
                    startMedia()
                } else if(intent.hasExtra("answer_call")) {
                    onVideoConfActivityCreate(false)
                } else {
                    openJitsiFragment(false)
                    if(ConnectionManager.isConnected()) {
                        Handler().postDelayed({
                            onStartCall()
                        }, 200)
                    }
                }
            }
        }
    }

    private fun onStartCall() {
        if(needToStartCall) {
            Handler().postDelayed({
                BusProvider.getInstance().post(FragmentFlow(WebRTCCallConstants.BusFragmentType.MAIN_CALL.toString(),
                    1, JSONObject(), ""))
                needToStartCall = false
            }, 500)
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStart() {
        super.onStart()
        BusProvider.getInstance().register(this)
    }

    override fun onStop() {
        super.onStop()
        BusProvider.getInstance().unregister(this)
    }

    override fun onBackPressed() {

    }

    override fun onDestroy() {
        super.onDestroy()
        HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
    }


    private fun openJitsiFragment(isConnecting: Boolean) {
        var fragment = JitsiCallActivity()
        val bundle = Bundle()
        bundle.putBoolean("connecting", isConnecting)
        bundle.putParcelable("videoCallModel", videoCallModel)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .add(R.id.main_layout, fragment, JitsiCallActivity::class.java.simpleName)
            .addToBackStack(JitsiCallActivity::class.java.simpleName)
            .commitAllowingStateLoss()
    }

    private fun openIncommingCallFragment() {
        var fragment = IncomingJitsiCallActivity()
        val bundle = Bundle()
        //bundle.putString("data", data)
        bundle.putParcelable("videoCallModel", videoCallModel)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .add(R.id.main_layout, fragment, IncomingJitsiCallActivity::class.java.simpleName)
            .addToBackStack(IncomingJitsiCallActivity::class.java.simpleName)
            .commitAllowingStateLoss()
    }

    private fun openIncommingGroupFragment() {
        var fragment = IncomingGroupCall()
        val bundle = Bundle()
        bundle.putParcelable("videoCallModel", videoCallModel)
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .add(R.id.main_layout, fragment, IncomingGroupCall::class.java.simpleName)
            .addToBackStack(IncomingGroupCall::class.java.simpleName)
            .commitAllowingStateLoss()
    }

    public fun sendMessage(channelId: Long, jsonObject: JSONObject) {
        ConnectionManager.publish("/$channelId", jsonObject)
    }

    @Subscribe
    public fun onBusFragmentType(data: FragmentFlow) {
        when(data.fragmentType) {
            WebRTCCallConstants.BusFragmentType.INCOMMING_JITSI_CALL.toString() -> {
                incomingBusOpration(data.type)
            }
            WebRTCCallConstants.BusFragmentType.JITSI_CALL.toString() -> {
                jitsiActivityOperation(data)
            }
            WebRTCCallConstants.BusFragmentType.INCOMMING_GROUP_CALL.toString() -> {
                incomingGroupBusOpration(data.type)
            }

        }
    }


    @Subscribe
    public fun onFayeMessageEvent(event: FayeMessage) {
        HippoLog.d("onFayeMessageEvent", "onFayeMessageEvent -> "+event.type);
        when (event.type) {
            BusEvents.ERROR_RECEIVED.toString() -> {
                startOldCall(event)
                /*try {
                    if(!initOldCall) {
                        initOldCall = true
                        val fragment = supportFragmentManager.findFragmentByTag(JitsiCallActivity::class.java.simpleName) as JitsiCallActivity
                        fragment.onErrorRecieved(event.message)
                    }
                } catch (e: Exception) {
                }*/
            }
            WebRTCCallConstants.BusFragmentType.CALL_HUNGUP.toString() -> {
                HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
                finish()
            }
        }
    }

    @Synchronized
    private fun startOldCall(event: FayeMessage) {
        try {
            if(!initOldCall) {
                initOldCall = true
                val fragment = supportFragmentManager.findFragmentByTag(JitsiCallActivity::class.java.simpleName) as JitsiCallActivity
                fragment.onErrorRecieved(event.message)
            }
        } catch (e: Exception) {
        }
    }


    private fun randomVideoConferenceLink(): ArrayList<String> {
        val linkArray = ArrayList<String>()
        val ALLOWED_CHARACTERS = "qwertyuiopasdfghjklzxcvbnm"
        val random = Random()
        val sb = StringBuilder(10)
        for (i in 0 until 10)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
//        if(!TextUtils.isEmpty(HippoCallConfig.getInstance().jitsiURL)) {
//            linkArray.add(HippoCallConfig.getInstance().jitsiURL)
//        } else {
            linkArray.add(FuguAppConstant.CONFERENCING_LIVE)
//        }
        linkArray.add(sb.toString())
        videoCallModel?.roomName = sb.toString()

        return linkArray
    }


    //==============================================================================================
    // for IncomingJitsiCallActivity
    //==============================================================================================


    private fun incomingGroupBusOpration(type: Int) {
        when(type) {
            WebRTCCallConstants.IncommintJitsiCall.ANSWERCALL -> {
                answerGroupCall()
                //answerGroupCall()
                stopForegroundService()
                HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
                OngoingCallService.NotificationServiceState.isConferenceConnected = true
                OngoingCallService.NotificationServiceState.hasGroupCall = true


                val serverURL: URL = getIndiaServerUrl()
                joinGroupCall(serverURL)
                startOngoingCallService()
                finish()
            }
            WebRTCCallConstants.IncommintJitsiCall.REJECTCALL -> {

                //ConnectionManager.publish() send socket msg on active channel and user channel
                HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
                rejectGroupCall()
                val hungupIntent = Intent(this@MainCallingActivity, HungUpBroadcast::class.java)
                hungupIntent.putExtra("action", "rejectCall")
                hungupIntent.putExtra(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails().toString())
                hungupIntent.putExtra(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
                hungupIntent.putExtra(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
                hungupIntent.putExtra(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
                if (videoCallModel?.hasGroupCall!!) {
                    hungupIntent.putExtra("has_group_call", true)
                }

                sendBroadcast(hungupIntent)
                finish()
            }
            WebRTCCallConstants.IncommintJitsiCall.UNREGISTER_BROADCAST -> {
                //mediaPlayer?.stop()
                HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
                try {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoConferenceHungup)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WebRTCCallConstants.IncommintJitsiCall.REGISTER_BROADCAST -> {
                try {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoConferenceHungup)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                LocalBroadcastManager.getInstance(this).registerReceiver(mVideoConferenceHungup,
                    IntentFilter(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
                )
            }
        }
    }
    private fun incomingBusOpration(type: Int) {
        when(type) {
            WebRTCCallConstants.IncommintJitsiCall.START_MEDIA -> startMedia()
            WebRTCCallConstants.IncommintJitsiCall.UNREGISTER_BROADCAST -> {
                //mediaPlayer?.stop()
                HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
                try {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoConferenceHungup)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WebRTCCallConstants.IncommintJitsiCall.REGISTER_BROADCAST -> {
                try {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoConferenceHungup)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                LocalBroadcastManager.getInstance(this).registerReceiver(mVideoConferenceHungup,
                    IntentFilter(FuguAppConstant.VIDEO_CONFERENCE_HUNGUP_INTENT)
                )
            }
            WebRTCCallConstants.IncommintJitsiCall.STOP -> {
                try {
                    LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoConferenceHungup)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WebRTCCallConstants.IncommintJitsiCall.ANSWERCALL -> {
                onVideoConfActivityCreate(false)
            }
            WebRTCCallConstants.IncommintJitsiCall.REJECTCALL -> {

                HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)

                val hungupIntent = Intent(this@MainCallingActivity, HungUpBroadcast::class.java)
                hungupIntent.putExtra("action", "rejectCall")
                hungupIntent.putExtra(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails().toString())
                hungupIntent.putExtra(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
                hungupIntent.putExtra(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
                hungupIntent.putExtra(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
                sendBroadcast(hungupIntent)
                finish()
            }
        }
    }

    private fun startMedia() {
        HippoAudioManager.getInstance(this@MainCallingActivity).startIncomingRinger()
    }

    private val mVideoConferenceHungup = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //mediaPlayer?.stop()
            HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
            finish()
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

    // Ending IncommingJitsi Call data
    //==============================================================================================

    //for VideoConfActivity here

    //==============================================================================================


    private fun jitsiActivityOperation(data: FragmentFlow) {
        when(data.type) {
            WebRTCCallConstants.JitsiCallActivity.POST_DATA -> {
                //Log.d("POST_DATA", "POST_DATA = "+ Gson().toJson(data))
                sendMessage(videoCallModel?.channelId!!, data?.json!!)
            }
            WebRTCCallConstants.JitsiCallActivity.OPEN_VIDEO_CONF -> {
                onVideoConfActivityCreate(true)
            }
            WebRTCCallConstants.JitsiCallActivity.PRE_LOAD_DATA -> {
//                val serverURL: URL = getServerUrl(videoCallModel?.jitsiLink)
//                preInitCall(serverURL)
            }
            WebRTCCallConstants.JitsiCallActivity.OPEN_OLD_CALL -> {
                //Toast.makeText(this@MainCallingActivity, "Update your app", Toast.LENGTH_LONG).show()
                HippoCallConfig.getInstance().initOldCall(videoCallModel)
                finish()
            }
        }
    }

    private fun onVideoConfActivityCreate(hasAnswer: Boolean) {

        stopForegroundService()
        HippoAudioManager.getInstance(this@MainCallingActivity).stop(false)
        OngoingCallService.NotificationServiceState.isConferenceConnected = true
        if (!hasAnswer) {
            answerConference()
        }
        val serverURL: URL = getServerUrl(videoCallModel?.jitsiLink)
        if(!TextUtils.isEmpty(videoCallModel?.jitsiLink)) {
            options = null
        }
        initCall(serverURL)
        startOngoingCallService()
        finish()
    }

    private fun stopForegroundService() {
        try {
            val startIntent = Intent(this@MainCallingActivity, VideoCallService::class.java)
            startIntent.action = "com.hippochat.start"
            startIntent.putExtra("isHungUpToBeSent", false)
            stopService(startIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startOngoingCallService() {
        val startIntent = Intent(this, OngoingCallService::class.java)
        startIntent.action = "com.hippochat.notification.start"
        startIntent.putExtra(FuguAppConstant.MESSAGE_UNIQUE_ID, videoCallModel?.signalUniqueId)
        startIntent.putExtra(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
        startIntent.putExtra(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
        startIntent.putExtra(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        ContextCompat.startForegroundService(this, startIntent)
    }

    private fun answerConference() {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.ANSWER_CONFERENCE.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
        //ConnectionManager.sendMessage(intent?.getLongExtra(CHANNEL_ID, -1L)!!, startCallJson)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", videoCallModel?.fullName)
        HippoCallingFlow.answerFor = OngoingCallService.NotificationServiceState.muid
        sendMessage(videoCallModel?.channelId!!, startCallJson)

    }

    private fun getIndiaServerUrl(): URL {
        var serverURL: URL
        if (!TextUtils.isEmpty(HippoCallConfig.getInstance().getJitsiURL())) {
            try {
                serverURL = URL(HippoCallConfig.getInstance().getJitsiURL())
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

    private fun getServerUrl(link: String?): URL {
        var serverURL: URL
        if (!TextUtils.isEmpty(link) && !TextUtils.isEmpty(HippoCallConfig.getInstance().getJitsiURL())) {
            try {
                serverURL = URL(HippoCallConfig.getInstance().getJitsiURL())
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

    private fun initCall(serverURL: URL) {
        if(options == null) {
            val userInfo = JitsiMeetUserInfo()
            try {
                userInfo.displayName = videoCallModel?.myname
                if (!TextUtils.isEmpty(videoCallModel?.myImagePath))
                    userInfo.avatar = URL(videoCallModel?.myImagePath)
            } catch (e: Exception) {
                userInfo.displayName = "Fellow User"
            }
            var roomName = videoCallModel?.roomName

            try {
                if ((videoCallModel?.callType == "AUDIO")
                    || (videoCallModel?.inviteLink!!.contains("#config.startWithVideoMuted=true"))
                    || roomName?.contains("#config.startWithVideoMuted=true")!!
                ) {
                    val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setAudioOnly(true)
                        .setFeatureFlag("chat.enabled", false)
                        .setFeatureFlag("invite.enabled", false)
                        .setUserInfo(userInfo)
                        .build()
                    JitsiMeet.setDefaultConferenceOptions(defaultOptions)
                } else {
                    val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setAudioOnly(false)
                        .setFeatureFlag("chat.enabled", false)
                        .setFeatureFlag("invite.enabled", false)
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
                    .setFeatureFlag("invite.enabled", false)
                    .setUserInfo(userInfo)
                    .build()
                JitsiMeet.setDefaultConferenceOptions(defaultOptions)
            }
            roomName = roomName?.replace("#config.startWithVideoMuted=true", "")
            options = JitsiMeetConferenceOptions.Builder()
                .setRoom(roomName)
                .build()
        }
        JitsiMeetActivity.launch(this, options, Restring.getString(this, R.string.hippo_calling_connection))

    }

    var options: JitsiMeetConferenceOptions ?= null
    private fun preInitCall(serverURL: URL) {
        val userInfo = JitsiMeetUserInfo()
        try {
            userInfo.displayName = videoCallModel?.myname
            if(!TextUtils.isEmpty(videoCallModel?.myImagePath))
                userInfo.avatar = URL(videoCallModel?.myImagePath)
        } catch (e: Exception) {
            userInfo.displayName = "Fellow User"
        }


        var roomName = videoCallModel?.roomName

        try {
            if ((videoCallModel?.callType == "AUDIO") || (videoCallModel?.inviteLink!!.contains("#config.startWithVideoMuted=true")) || roomName?.contains("#config.startWithVideoMuted=true")!!) {
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
        roomName = roomName?.replace("#config.startWithVideoMuted=true", "")
        options = JitsiMeetConferenceOptions.Builder()
            .setRoom(roomName)
            .build()
    }

    // for GroupCall

    private fun joinGroupCall(serverURL: URL) {
        val userInfo = JitsiMeetUserInfo()
        try {
            userInfo.displayName = videoCallModel?.myname
            if(!TextUtils.isEmpty(videoCallModel?.myImagePath))
                userInfo.avatar = URL(videoCallModel?.myImagePath)
        } catch (e: Exception) {
            userInfo.displayName = "Fellow User"
        }

        var roomName = videoCallModel?.roomName

        try {
            if(videoCallModel?.isVideoCall!!) {
                val defaultOptions = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setWelcomePageEnabled(false)
                    .setAudioOnly(false)
                    .setAudioMuted(videoCallModel?.isAudioCall!!)
                    .setFeatureFlag("chat.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("add-people.enabled", false)
                    .setUserInfo(userInfo)
                    .build()
                JitsiMeet.setDefaultConferenceOptions(defaultOptions)
            } else {
                val defaultOptions = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setWelcomePageEnabled(false)
                    .setAudioOnly(true)
                    .setAudioMuted(videoCallModel?.isAudioCall!!)
                    .setFeatureFlag("chat.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("add-people.enabled", false)
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
                .setFeatureFlag("invite.enabled", false)
                .setFeatureFlag("add-people.enabled", false)
                .setUserInfo(userInfo)
                .build()
            JitsiMeet.setDefaultConferenceOptions(defaultOptions)
        }
        roomName = roomName?.replace("config.startWithVideoMuted=true", "")
        roomName = roomName?.replace("config.startWithAudioMuted=true", "")
        roomName = roomName?.replace("#", "")

        options = JitsiMeetConferenceOptions.Builder()
            .setRoom(roomName)
            .build()

        JitsiMeetActivity.launch(this, options, Restring.getString(this, R.string.hippo_calling_connection))
    }

    private fun answerGroupCall() {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        //val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.JOIN_GROUP_CALL.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", videoCallModel?.fullName)

        sendMessage(videoCallModel?.channelId!!, startCallJson)
        sendMessageOnUserChannel("/"+com.hippo.database.CommonData.getUserDetails().data.userChannel)
    }

    private fun sendMessageOnUserChannel(channelId: String) {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        //val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.JOIN_GROUP_CALL.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", videoCallModel?.fullName)
        startCallJson.put("server_push", true)
        ConnectionManager.publish(channelId, startCallJson)
    }

    private fun rejectGroupCall() {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.REJECT_GROUP_CALL.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", videoCallModel?.fullName)

        sendMessage(videoCallModel?.channelId!!, startCallJson)
        sendRejectOnUserChannel("/"+com.hippo.database.CommonData.getUserDetails().data.userChannel)

    }

    private fun sendRejectOnUserChannel(channelId: String) {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        //val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.REJECT_GROUP_CALL.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, videoCallModel?.jitsiLink)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", videoCallModel?.fullName)
        startCallJson.put("server_push", true)
        ConnectionManager.publish(channelId, startCallJson)
    }

    private fun getBaseUrl(url: String) {
        try {
            val url = URL(url)
            val baseUrl = url.protocol + "://" + url.host
        } catch (e: MalformedURLException) {
            // do something
        }
    }

    private fun createConversation(fuguCreateConversationParams: FuguCreateConversationParams) {
        try {
            RestClient.getApiInterface().createConversation(fuguCreateConversationParams)
                .enqueue(object : ResponseResolver<FuguCreateConversationResponse>(this, false, false) {
                    override fun success(t: FuguCreateConversationResponse?) {
                        try {
                            if(!isFinishing) {
                                val channelId: Long = t?.getData()?.getChannelId()!!
                                videoCallModel?.channelId = channelId
                                ConnectionManager.subScribeChannel("/${videoCallModel?.channelId!!}")
                                CommonData.setChannelIds(fuguCreateConversationParams.transactionId, channelId)
                                BusProvider.getInstance().post(OnCreateChannel(true, channelId, videoCallModel!!))
                            }
                        } catch (e: Exception) {
                        }
                    }

                    override fun failure(error: APIError?) {
                        Toast.makeText(this@MainCallingActivity, ""+error?.message, Toast.LENGTH_SHORT).show()
                    }

                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}