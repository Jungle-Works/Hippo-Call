package com.hippocall

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.hippo.BuildConfig
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.INVALID_VIDEO_CALL_CREDENTIALS
import com.hippo.constant.FuguAppConstant.TYPING_SHOW_MESSAGE
import com.hippo.eventbus.BusProvider
import com.hippo.helper.BusEvents
import com.hippo.helper.FayeMessage
import com.hippocall.WebRTCCallConstants.Companion.CALL_TYPE
import com.hippocall.WebRTCCallConstants.Companion.CREDENTIAL
import com.hippocall.WebRTCCallConstants.Companion.CUSTOM_DATA
import com.hippocall.WebRTCCallConstants.Companion.DEVICE_PAYLOAD
import com.hippocall.WebRTCCallConstants.Companion.FULL_NAME
import com.hippocall.WebRTCCallConstants.Companion.HUNGUP_TYPE
import com.hippocall.WebRTCCallConstants.Companion.IS_SILENT
import com.hippocall.WebRTCCallConstants.Companion.IS_TYPING
import com.hippocall.WebRTCCallConstants.Companion.MESSAGE_TYPE
import com.hippocall.WebRTCCallConstants.Companion.MESSAGE_UNIQUE_ID
import com.hippocall.WebRTCCallConstants.Companion.STUN
import com.hippocall.WebRTCCallConstants.Companion.TURN
import com.hippocall.WebRTCCallConstants.Companion.TURN_API_KEY
import com.hippocall.WebRTCCallConstants.Companion.TURN_CREDENTIALS
import com.hippocall.WebRTCCallConstants.Companion.USER_ID
import com.hippocall.WebRTCCallConstants.Companion.USER_NAME
import com.hippocall.WebRTCCallConstants.Companion.VIDEO_CALL
import com.hippocall.WebRTCCallConstants.Companion.VIDEO_CALL_HUNGUP_FROM_NOTIFICATION
import com.hippocall.WebRTCCallConstants.Companion.VIDEO_CALL_TYPE
import com.hippocall.model.FayeVideoCallResponse
import com.squareup.otto.Subscribe
import faye.ConnectionManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.PeerConnection
import java.util.*

/**
 * Created by rajatdhamija
 * 20/09/18.
 */

class WebRTCSignallingClient(
    private var videoCallService: VideoCallService, private var channelId: Long?,
    private val activitylaunchState: String?
) {

    var flag: Boolean = false

    @Subscribe
    public fun onFayeMessage(events: FayeMessage) {
        when (events.type) {
            //BusEvents.CONNECTED_SERVER.toString()       -> subscribeChannels()
            BusEvents.RECEIVED_MESSAGE.toString() -> onReceivedMessage(
                events.message,
                events.channelId
            )
            BusEvents.DISCONNECTED_SERVER.toString() -> onDisconnectedServer()
            BusEvents.PONG_RECEIVED.toString() -> onPongReceived()
            BusEvents.WEBSOCKET_ERROR.toString() -> onWebSocketError()
            BusEvents.ERROR_RECEIVED.toString() -> onErrorReceived(events.message, events.channelId)
        }
    }

    fun onConnectedServer() {
        flag = false
        //mClient = fc
        if (channelId!!.toInt() > 0) {
            //fc!!.subscribeChannel("/$channelId")
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                if (activitylaunchState?.equals(WebRTCCallConstants.AcitivityLaunchState.SELF.toString())!!) {
                    //initiateVideoCall(false)
                    initOtherCalls()
                } else if (activitylaunchState.equals(WebRTCCallConstants.AcitivityLaunchState.OTHER.toString())) {
                    replyWithReadyToConnect()
                }
            }, 1000)
        }
        //hasPendingIntent()
    }

    fun onDisconnectedServer() {
        if (!flag) {
            fayeConnectionRetry()
            flag = true
        }
    }

    fun onReceivedMessage(msg: String?, channel: String?) {
        flag = false
        if (!TextUtils.isEmpty(msg))
            callRecieved(msg!!, channel)
    }

    fun onPongReceived() {
        flag = false
    }

    fun onWebSocketError() {
        if (!flag) {
            fayeConnectionRetry()
            flag = true
        }
    }

    fun onErrorReceived(msg: String?, channel: String?) {
        flag = false
        if (msg != null) {
            Log.e("Faye Message Error", msg)
        }
        try {
            val fayeVideoCallResponse = Gson().fromJson(msg, FayeVideoCallResponse::class.java)
            if (fayeVideoCallResponse.statusCode == INVALID_VIDEO_CALL_CREDENTIALS) {
                isErrorEncountered = true
                val iceServers = ArrayList<PeerConnection.IceServer>()
                signal?.turnApiKey = fayeVideoCallResponse.message.turnApiKey
                signal?.turnUserName = fayeVideoCallResponse.message.username
                signal?.turnCredential = fayeVideoCallResponse.message.credentials
                signal?.stunServers =
                    fayeVideoCallResponse.message.iceServers.stun as ArrayList<String>
                signal?.turnServers =
                    fayeVideoCallResponse.message.iceServers.turn as ArrayList<String>

                object : Thread() {
                    override fun run() {
                        super.run()
                        val appContants = AppContants()
                        val turnCreds = appContants.turnCredentials
                        turnCreds.credentials = signal?.turnCredential
                        turnCreds.username = signal?.turnUserName
                        turnCreds.turnApiKey = signal?.turnApiKey
                        turnCreds.iceServers.stun = signal?.stunServers
                        turnCreds.iceServers.turn = signal?.turnServers
                    }
                }.start()
                for (i in signal?.stunServers?.indices!!) {
                    val stunIceServer =
                        PeerConnection.IceServer.builder(signal?.stunServers?.get(i))
                            .createIceServer()
                    iceServers.add(stunIceServer)
                }
                for (i in signal?.turnServers?.indices!!) {
                    val turnIceServer =
                        PeerConnection.IceServer.builder(signal?.turnServers?.get(i))
                            .setUsername(fayeVideoCallResponse.message.username)
                            .setPassword(fayeVideoCallResponse.message.credentials)
                            .createIceServer()
                    iceServers.add(turnIceServer)
                }
                if (activitylaunchState?.equals(WebRTCCallConstants.AcitivityLaunchState.SELF.toString())!!) {
                    initiateVideoCall(false)
                    initalCalls = 1
                    initOtherCalls()
                } else if (activitylaunchState.equals(WebRTCCallConstants.AcitivityLaunchState.OTHER.toString())) {
                    replyWithReadyToConnect()
                }
            }
        } catch (e: Exception) {

        }
    }

    private fun callRecieved(messageJson: String, channel: String?) {
        try {
            val mngr = videoCallService.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val taskList = mngr.getRunningTasks(10)
            val json = JSONObject(messageJson)
            val myUserId = json.optLong(USER_ID, -1)
            if (taskList[0].topActivity!!.className == "com.hippocall.FuguCallActivity" &&
                channel == "/$channelId" &&
                json.has(MESSAGE_TYPE) && json.getInt(MESSAGE_TYPE) == 18
                && myUserId.compareTo(signal?.signalUniqueUserId!!) != 0
                && json.getString(MESSAGE_UNIQUE_ID) == signal?.signalUniqueId!!) {

                Log.e("Type---->", json.getString(VIDEO_CALL_TYPE))
                when (json.getString(VIDEO_CALL_TYPE)) {
                    WebRTCCallConstants.Companion.VideoCallType.READY_TO_CONNECT.toString() -> {
                        if (!videoCallService.isCallConnected!! && activitylaunchState?.equals(
                                WebRTCCallConstants.AcitivityLaunchState.SELF.toString()
                            )!!
                            && !videoCallService.isCallInitiated!!
                        ) {
                            videoCallService.isCallInitiated = true
                            videoCallService.onReadyToConnectRecieved(json)
                        } else {
                            sendOfferToRemoteUser(videoCallService.webRTCCallClient?.videoOffer!!)
                        }
                    }
                    WebRTCCallConstants.Companion.VideoCallType.NEW_ICE_CANDIDATE.toString() -> {
                        videoCallService.onIceCandidateRecieved(json)
                    }
                    WebRTCCallConstants.Companion.VideoCallType.VIDEO_OFFER.toString() -> {
                        if (!videoCallService.isCallConnected!!) {
                            if (!isOfferrecieved) {
                                isOfferrecieved = true
                                videoCallService.onVideoOfferRecieved(json)
                            }
                        } else if (videoCallService.isCallConnected!! && json.has("is_screen_share")) {
                            if (videoCallService.peerConnection != null) {
                                videoCallService.onVideoOfferScreenSharingRecieved(json)
                            }
                        }
                    }
                    WebRTCCallConstants.Companion.VideoCallType.VIDEO_ANSWER.toString() -> {
                        if (!videoCallService.isCallConnected!!) {
                            mInitiateStartCalltimer?.cancel()
                            videoCallService.isReadyForConnection = true
                            videoCallService.isCallConnected = true
                            videoCallService.webRTCCallClient?.saveAnswer(json)
                            videoCallService.onVideoAnswerRecieved(json)
                            videoCallService.onCallConnected()
                        }
                    }
                    WebRTCCallConstants.Companion.VideoCallType.USER_BUSY.toString() -> {
                        if (!videoCallService.isCallConnected!!) {
                            videoCallService.onUserBusyRecieved(json)
                            userBusyRecieved = true
                        }
                    }
                    WebRTCCallConstants.Companion.VideoCallType.CALL_HUNG_UP.toString() -> {
                        if (json.has(HUNGUP_TYPE) && json.getString(HUNGUP_TYPE).equals("DEFAULT")) {
                            videoCallService.onCallHungUp(json, false)
                        } else {
                            videoCallService.onCallHungUp(json, false)
                        }
                    }
                    WebRTCCallConstants.Companion.VideoCallType.CALL_REJECTED.toString() -> {
                        if (!videoCallService.isCallConnected!!) {
                            mInitiateStartCalltimer?.cancel()
                            videoCallService.isReadyForConnection = true
                            videoCallService.onCallRejected(json)
                        }
                    }
                    WebRTCCallConstants.VideoCallType.CUSTOM_DATA.toString() -> {
                        videoCallService.sendCustomData(json.getJSONObject(CUSTOM_DATA).toString())
                    }
                }
            } else if (channel == "/$channelId" &&
                json.has(MESSAGE_TYPE) && json.getInt(MESSAGE_TYPE) == 18
                && myUserId.compareTo(signal?.signalUniqueUserId!!) == 0
                && json.getString(MESSAGE_UNIQUE_ID) == signal?.signalUniqueId
            ) {
                if (json.getString(VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.CALL_REJECTED.toString()) {
                    videoCallService.onCallRejected(json)
                } else if (json.getString(VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.USER_BUSY.toString()) {
                    if (!videoCallService.isCallConnected!!) {
                        videoCallService.onUserBusyRecieved(json)
                        userBusyRecieved = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private final var TAG = WebRTCSignallingClient::class.simpleName
    //private var mClient: FayeClient? = null
    private var signal: Signal? = null
    private var initalCalls = 1
    private var maxCalls = 15
    private var isErrorEncountered = false
    private var pendingsSignalJson: JSONObject? = null
    private var userBusyRecieved = false

    /*fun isConnected(): Boolean {
        return mClient?.isConnectedServer!!
    }*/

    fun setUpFayeConnection() {
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(videoCallService)
            .registerReceiver(mHungUp, IntentFilter(VIDEO_CALL_HUNGUP_FROM_NOTIFICATION))


        ConnectionManager.initFayeConnection()
        ConnectionManager.subScribeChannel("/$channelId")
        BusProvider.getInstance().register(this)
        onConnectedServer()

        /*MyApplication.getInstance().getExistingClient(object : fayeClient{
            override fun Listener(client: FayeClient) {
                setListener(client)
            }
        })*/
    }

    fun unregsiterBus() {
        BusProvider.getInstance().unregister(this)
    }

    private fun setListener() {
//        mClient = client
//        mClient!!.connectServer()
//        mClient!!.setmVideoFayeListener(this)
    }

    fun passServiceCall(videoCallService: VideoCallService, channelId: Long) {
        this.videoCallService = videoCallService
        this.channelId = channelId

        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(videoCallService)
            .registerReceiver(
                mHungUp, IntentFilter(VIDEO_CALL_HUNGUP_FROM_NOTIFICATION)
            )

//        this.mClient = HippoCallConfig.getInstance().getClient()
//        mClient!!.setmVideoFayeListener(this)

        ConnectionManager.initFayeConnection()
        ConnectionManager.subScribeChannel("/$channelId")
        BusProvider.getInstance().register(this)
        onConnectedServer()


        isOfferrecieved = true
    }

    fun initOtherCalls() {
        mInitiateStartCalltimer = object : CountDownTimer(300000, 2000) {
            override fun onFinish() {
            }

            override fun onTick(millisUntilFinished: Long) {
                if (initalCalls <= maxCalls && !videoCallService.isReadyForConnection!! && !isErrorEncountered && !userBusyRecieved) {
                    if (initalCalls == 1) {
                        initiateVideoCall(false)
                    } else {
                        initiateVideoCall(true)
                    }
                    initalCalls += 1
                }
            }

        }.start()
    }

    private fun replyWithReadyToConnect() {
        val readyToConnectJson = JSONObject()
        readyToConnectJson.put(
            VIDEO_CALL_TYPE,
            WebRTCCallConstants.VideoCallType.READY_TO_CONNECT.toString()
        )
        readyToConnectJson.put(IS_SILENT, true)
        readyToConnectJson.put(USER_ID, signal?.signalUniqueUserId)
        readyToConnectJson.put(FULL_NAME, signal?.fullNameOfCalledPerson)
        readyToConnectJson.put(MESSAGE_TYPE, VIDEO_CALL)
        readyToConnectJson.put(IS_TYPING, TYPING_SHOW_MESSAGE)
        readyToConnectJson.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
        addTurnCredentialsAndDeviceDetails(readyToConnectJson)
    }

    fun setSignalRequirementModel(signal: Signal?) {
        this.signal = signal
    }

    fun initiateVideoCall(isSignalSilent: Boolean) {
        try {
            val startCallJson = JSONObject()
            startCallJson.put(
                VIDEO_CALL_TYPE,
                WebRTCCallConstants.VideoCallType.START_CALL.toString()
            )
            startCallJson.put(IS_SILENT, isSignalSilent)
            startCallJson.put(USER_ID, signal?.signalUniqueUserId)
            startCallJson.put(FULL_NAME, signal?.fullNameOfCalledPerson)
            startCallJson.put(MESSAGE_TYPE, VIDEO_CALL)
            startCallJson.put(IS_TYPING, TYPING_SHOW_MESSAGE)
            startCallJson.put(CALL_TYPE, signal?.callType)
            startCallJson.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)

            addTurnCredentialsAndDeviceDetails(startCallJson)
        } catch (e: Exception) {
            //Log.e("TAG", "In Init Error")
            e.printStackTrace()
        }

    }

    fun sendOfferToRemoteUser(jsonObject: JSONObject): JSONObject? {
        val offerJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(offerJson)
        return offerJson
    }

    fun sendAnswerToRemoteUser(jsonObject: JSONObject): JSONObject? {
        val offerJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(offerJson)
        return offerJson
    }

    fun sendIceCandidates(jsonObject: JSONObject) {
        val iceCandidateJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(iceCandidateJson)
    }

    fun hangUpCall() {
        val jsonObject = JSONObject()
        jsonObject.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.CALL_HUNG_UP.toString())
        val hangupJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(hangupJson)
        ConnectionManager.unsubScribeChannel("/$channelId")
        //mClient?.unsubscribeAll()
    }

    fun rejectCall() {
        val jsonObject = JSONObject()
        jsonObject.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.CALL_REJECTED.toString())
        val rejectedJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(rejectedJson)
        ConnectionManager.unsubScribeChannel("/$channelId")
        //mClient?.unsubscribeAll()
    }

    fun addCommonuserDetails(jsonObject: JSONObject): JSONObject {
        jsonObject.put(IS_SILENT, true)
        jsonObject.put(USER_ID, signal?.signalUniqueUserId)
        jsonObject.put(MESSAGE_TYPE, VIDEO_CALL)
        jsonObject.put(IS_TYPING, 0)
        jsonObject.put(IS_SILENT, true)
        jsonObject.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
        jsonObject.put(CALL_TYPE, signal?.callType)
        return jsonObject
    }

    fun addTurnCredentialsAndDeviceDetails(jsonObject: JSONObject) {
        val stunServers = JSONArray()
        val turnServers = JSONArray()
        val videoCallCredentials = JSONObject()

        videoCallCredentials.put(TURN_API_KEY, signal?.turnApiKey)
        videoCallCredentials.put(USER_NAME, signal?.turnUserName)
        videoCallCredentials.put(CREDENTIAL, signal?.turnCredential)
        for (i in signal?.stunServers!!.indices) {
            stunServers.put(signal?.stunServers!!.get(i))
        }
        for (i in signal?.turnServers!!.indices) {
            turnServers.put(signal?.turnServers!!.get(i))
        }

        videoCallCredentials.put(STUN, stunServers)
        videoCallCredentials.put(TURN, turnServers)

        jsonObject.put(TURN_CREDENTIALS, videoCallCredentials)
        jsonObject.put(DEVICE_PAYLOAD, signal?.deviceDetails)
        jsonObject.put(CALL_TYPE, signal?.callType)
        publishSignalToFaye(jsonObject)
    }

    fun publishSignalToFaye(signalJson: JSONObject) {
        ConnectionManager.publish("/$channelId", signalJson)
        /*if (mClient != null) {
            mClient?.publish("/$channelId", signalJson, object : ConnectionError {
                override fun onError(signalJson: JSONObject) {
                    pendingsSignalJson = signalJson
                    fayeConnectionRetry()
                }
            })
        }*/
    }

    var pendingJson: JSONObject? = null
    var pendingcChannelId: Long? = null
    private fun hasPendingIntent() {
        if (pendingJson != null && pendingsSignalJson != null) {
            //mClient?.publish("/" + pendingcChannelId, pendingJson)
            ConnectionManager.publish("/$pendingcChannelId", pendingJson!!)
            pendingJson = null
            pendingcChannelId = null
        }
    }

    fun onBroadcastRecieved(intent: Intent) {
        if (intent.hasExtra(FuguAppConstant.CHANNEL_ID) && intent.getStringExtra(FuguAppConstant.MESSAGE_UNIQUE_ID) != signal?.signalUniqueId) {
            if (intent.getStringExtra(FuguAppConstant.VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.START_CALL.toString()) {
                try {
                    val json = JSONObject()
                    json.put(
                        FuguAppConstant.VIDEO_CALL_TYPE,
                        WebRTCCallConstants.VideoCallType.USER_BUSY.toString()
                    )
                    json.put(FuguAppConstant.IS_SILENT, true)
                    json.put(
                        FuguAppConstant.USER_ID,
                        intent.getLongExtra(FuguAppConstant.USER_ID, -1L)
                    )
                    json.put(FuguAppConstant.FULL_NAME, signal?.fullNameOfCalledPerson)
                    json.put(FuguAppConstant.MESSAGE_TYPE, FuguAppConstant.VIDEO_CALL)
                    json.put(FuguAppConstant.IS_TYPING, TYPING_SHOW_MESSAGE)
                    json.put(
                        FuguAppConstant.MESSAGE_UNIQUE_ID,
                        intent.getStringExtra(FuguAppConstant.MESSAGE_UNIQUE_ID)
                    )
                    val devicePayload = JSONObject()
                    devicePayload.put(
                        FuguAppConstant.DEVICE_ID,
                        CommonData.getUniqueIMEIId(videoCallService)
                    )
                    devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
                    devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
                    devicePayload.put(
                        FuguAppConstant.DEVICE_DETAILS,
                        CommonData.deviceDetails(videoCallService)
                    )
                    json.put("device_payload", devicePayload)

                    pendingcChannelId = intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L)
                    pendingJson = json

                    ConnectionManager.publish(
                        "/" + intent.getLongExtra(
                            FuguAppConstant.CHANNEL_ID,
                            -1L
                        ), json
                    )
//                    mClient?.publish(intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), "/" + intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), json)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        } else if (intent.getStringExtra(FuguAppConstant.VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.CALL_HUNG_UP.toString()) {
            videoCallService.onCallHungUp(null, false)
            if (videoCallService.callDisconnectTime != null) {
                videoCallService.callDisconnectTime?.cancel()
            }
            ConnectionManager.unsubScribeChannel(
                "/" + intent.getLongExtra(
                    FuguAppConstant.CHANNEL_ID,
                    -1
                )
            )
            //mClient?.unsubscribeAll()
        } else if (intent.hasExtra(CUSTOM_DATA)) {
            val data: String? = intent.getStringExtra("data")
            publishMessage(JSONObject(data))
        }
    }

    fun publishMessage(data: JSONObject) {
        val jsonObject = JSONObject()
        jsonObject.put(CUSTOM_DATA, data)
        jsonObject.put(IS_SILENT, true)
        jsonObject.put(USER_ID, signal?.signalUniqueUserId)
        jsonObject.put(MESSAGE_TYPE, VIDEO_CALL)
        jsonObject.put(IS_TYPING, 0)
        jsonObject.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
        jsonObject.put(CALL_TYPE, signal?.callType)
        jsonObject.put(VIDEO_CALL_TYPE, "CUSTOM_DATA")
        jsonObject.put("server_push", 1)
        ConnectionManager.publish("/$channelId", jsonObject)
        /*if (mClient != null) {
            mClient?.publish("/$channelId", jsonObject)
        }*/
    }

    private var mHungUp: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            hangUpCall()
        }
    }


    public fun fayeConnectionRetry() {
        if (!HippoCallConfig.getInstance().isNetworkConnected)
            return

        ConnectionManager.initFayeConnection()

        /*if (mClient == null) {
            MyApplication.getInstance().getExistingClient(object : fayeClient{
                override fun Listener(client: FayeClient) {
                    setListener(client)
                }
            })
        } else {
            mClient!!.setmVideoFayeListener(this)
            mClient!!.connectServer()
        }*/
    }

    private var mInitiateStartCalltimer: CountDownTimer? = null
    private var isOfferrecieved = false

    fun cancelCounter() {
        mInitiateStartCalltimer?.cancel()
    }

    fun isFayeConnected(): Boolean {
        return isOfferrecieved
    }

    fun publishOperationMessage(data: JSONObject) {
        val jsonObject = JSONObject()
        jsonObject.put(CUSTOM_DATA, data)
        jsonObject.put(IS_SILENT, true)
        jsonObject.put(USER_ID, signal?.signalUniqueUserId)
        jsonObject.put(MESSAGE_TYPE, VIDEO_CALL)
        jsonObject.put(IS_TYPING, 0)
        jsonObject.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
        jsonObject.put(CALL_TYPE, signal?.callType)
        jsonObject.put(VIDEO_CALL_TYPE, "CALL_ACTION")
        jsonObject.put("server_push", 1)
        ConnectionManager.publish("/$channelId", jsonObject)
    }

}

//class WebRTCSignallingClient(private var videoCallService: VideoCallService, private var channelId: Long?,
//        private val activitylaunchState: String?) {
//
//    var flag: Boolean = false
//
//    /*@Subscribe
//    public fun onFayeMessage(events: FayeMessage) {
//        when(events.type) {
//            BusEvents.RECEIVED_MESSAGE.toString()       -> onReceivedMessage(events.message, events.channelId)
//            BusEvents.DISCONNECTED_SERVER.toString()          -> onDisconnectedServer()
//            BusEvents.PONG_RECEIVED.toString()          -> onPongReceived()
//            BusEvents.WEBSOCKET_ERROR.toString()          -> onWebSocketError()
//            BusEvents.ERROR_RECEIVED.toString()          -> onErrorReceived(events.message, events.channelId)
//        }
//    }*/
//
//    fun onConnectedServer() {
//        flag = false
//        //mClient = fc
//        if(channelId!!.toInt()>0) {
//            //fc!!.subscribeChannel("/$channelId")
//            android.os.Handler(Looper.getMainLooper()).postDelayed({
//                if (activitylaunchState?.equals(WebRTCCallConstants.AcitivityLaunchState.SELF.toString())!!) {
//                    //initiateVideoCall(false)
//                    initOtherCalls()
//                } else if (activitylaunchState.equals(WebRTCCallConstants.AcitivityLaunchState.OTHER.toString())) {
//                    replyWithReadyToConnect()
//                }
//            }, 1000)
//        }
//        //hasPendingIntent()
//    }
//
//    fun onDisconnectedServer() {
//        if(!flag) {
//            fayeConnectionRetry()
//            flag = true
//        }
//    }
//
//    fun onReceivedMessage(msg: String?, channel: String?) {
//        flag = false
//        if (!TextUtils.isEmpty(msg))
//            callRecieved(msg!!, channel)
//    }
//
//    fun onPongReceived() {
//        flag = false
//    }
//
//    fun onWebSocketError() {
//        if(!flag) {
//            fayeConnectionRetry()
//            flag = true
//        }
//    }
//
//    fun onErrorReceived(msg: String?, channel: String?) {
//        flag = false
//        Log.e("Faye Message Error", msg)
//        try {
//            val fayeVideoCallResponse = Gson().fromJson(msg, FayeVideoCallResponse::class.java)
//            if (fayeVideoCallResponse.statusCode == INVALID_VIDEO_CALL_CREDENTIALS) {
//                isErrorEncountered = true
//                val iceServers = ArrayList<PeerConnection.IceServer>()
//                signal?.turnApiKey = fayeVideoCallResponse.message.turnApiKey
//                signal?.turnUserName = fayeVideoCallResponse.message.username
//                signal?.turnCredential = fayeVideoCallResponse.message.credentials
//                signal?.stunServers = fayeVideoCallResponse.message.iceServers.stun as ArrayList<String>
//                signal?.turnServers = fayeVideoCallResponse.message.iceServers.turn as ArrayList<String>
//
//                object : Thread() {
//                    override fun run() {
//                        super.run()
//                        val appContants = AppContants()
//                        val turnCreds = appContants.turnCredentials
//                        turnCreds.credentials = signal?.turnCredential
//                        turnCreds.username = signal?.turnUserName
//                        turnCreds.turnApiKey = signal?.turnApiKey
//                        turnCreds.iceServers.stun = signal?.stunServers
//                        turnCreds.iceServers.turn = signal?.turnServers
//
////                        turnCreds.blockStatus = "NOT_BLOCKED"
////                        turnCreds.lifetimeDuration = "86400s"
////                        turnCreds.iceTransportPolicy = "all"
//                    }
//                }.start()
//                for (i in signal?.stunServers?.indices!!) {
//                    val stunIceServer = PeerConnection.IceServer.builder(signal?.stunServers?.get(i))
//                        .createIceServer()
//                    iceServers.add(stunIceServer)
//                }
//                for (i in signal?.turnServers?.indices!!) {
//                    val turnIceServer = PeerConnection.IceServer.builder(signal?.turnServers?.get(i))
//                        .setUsername(fayeVideoCallResponse.message.username)
//                        .setPassword(fayeVideoCallResponse.message.credentials)
//                        .createIceServer()
//                    iceServers.add(turnIceServer)
//                }
//                if (activitylaunchState?.equals(WebRTCCallConstants.AcitivityLaunchState.SELF.toString())!!) {
//                    initiateVideoCall(false)
//                    initalCalls = 1
//                    initOtherCalls()
//                } else if (activitylaunchState.equals(WebRTCCallConstants.AcitivityLaunchState.OTHER.toString())) {
//                    replyWithReadyToConnect()
//                }
//            }
//        } catch (e: Exception) {
//
//        }
//    }
//
//    private fun callRecieved(messageJson: String, channel: String?) {
//        try {
//            val mngr = videoCallService.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//            val taskList = mngr.getRunningTasks(10)
//            val json = JSONObject(messageJson)
//            val myUserId = json.optLong(USER_ID, -1)
//            if (taskList[0].topActivity.className == "com.hippocall.FuguCallActivity" &&
//                channel == "/$channelId" &&
//                json.has(MESSAGE_TYPE) && json.getInt(MESSAGE_TYPE) == 18
//                && myUserId.compareTo(signal?.signalUniqueUserId!!) != 0
//                && json.getString(MESSAGE_UNIQUE_ID) == signal?.signalUniqueId!!) {
//
//                Log.e("Type---->", json.getString(VIDEO_CALL_TYPE))
//                when (json.getString(VIDEO_CALL_TYPE)) {
//                    WebRTCCallConstants.Companion.VideoCallType.READY_TO_CONNECT.toString() -> {
//                        HippoLog.e(TAG, "videoCallService.isCallConnected = "+videoCallService.isCallConnected)
//                        HippoLog.e(TAG, "activitylaunchState = "+activitylaunchState)
//                        HippoLog.e(TAG, "videoCallService.isCallInitiated = "+videoCallService.isCallInitiated)
//
//                        if (!videoCallService.isCallConnected!! && activitylaunchState?.equals(WebRTCCallConstants.AcitivityLaunchState.SELF.toString())!!
//                            && !videoCallService.isCallInitiated!!
//                        ) {
//                            videoCallService.isCallInitiated = true
//                            videoCallService.onReadyToConnectRecieved(json)
//                        } else {
//                            try {
//                                if(videoCallService.webRTCCallClient?.videoOffer != null)
//                                    sendOfferToRemoteUser(videoCallService.webRTCCallClient?.videoOffer!!)
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                            }
//                        }
//                    }
//                    WebRTCCallConstants.Companion.VideoCallType.NEW_ICE_CANDIDATE.toString() -> {
//                        videoCallService.onIceCandidateRecieved(json)
//                    }
//                    WebRTCCallConstants.Companion.VideoCallType.VIDEO_OFFER.toString() -> {
//                        if (!videoCallService.isCallConnected!!) {
//                            if (!isOfferrecieved) {
//                                isOfferrecieved = true
//                                videoCallService.onVideoOfferRecieved(json)
//                            }
//                        } else if (videoCallService.isCallConnected!! && json.has("is_screen_share")) {
//                            if (videoCallService.peerConnection != null) {
//                                videoCallService.onVideoOfferScreenSharingRecieved(json)
//                            }
//                        }
//                    }
//                    WebRTCCallConstants.Companion.VideoCallType.VIDEO_ANSWER.toString() -> {
//                        if (!videoCallService.isCallConnected!!) {
//                            mInitiateStartCalltimer?.cancel()
//                            videoCallService.isReadyForConnection = true
//                            videoCallService.isCallConnected = true
//                            videoCallService.webRTCCallClient?.saveAnswer(json)
//                            videoCallService.onVideoAnswerRecieved(json)
//                            videoCallService.onCallConnected()
//                            videoCallService.webRTCCallClient?.videoOffer = null
//                        }
//                    }
//                    WebRTCCallConstants.Companion.VideoCallType.USER_BUSY.toString() -> {
//                        if (!videoCallService.isCallConnected!!) {
//                            videoCallService.onUserBusyRecieved(json)
//                            userBusyRecieved = true
//                        }
//                    }
//                    WebRTCCallConstants.Companion.VideoCallType.CALL_HUNG_UP.toString() -> {
//                        if (json.has(HUNGUP_TYPE) && json.getString(HUNGUP_TYPE).equals("DEFAULT")) {
//                            videoCallService.onCallHungUp(json, false)
//                        } else {
//                            videoCallService.onCallHungUp(json, false)
//                        }
//                    }
//                    WebRTCCallConstants.Companion.VideoCallType.CALL_REJECTED.toString() -> {
//                        if (!videoCallService.isCallConnected!!) {
//                            mInitiateStartCalltimer?.cancel()
//                            videoCallService.isReadyForConnection = true
//                            videoCallService.onCallRejected(json)
//                        }
//                    }
//                    WebRTCCallConstants.VideoCallType.CUSTOM_DATA.toString() -> {
//                        videoCallService.sendCustomData(json.getJSONObject(CUSTOM_DATA).toString())
//                    }
//                    WebRTCCallConstants.VideoCallType.CALL_ACTION.toString() -> {
//                        videoCallService.sendUserAction(json.optJSONObject(CUSTOM_DATA))
//                    }
//
//                }
//            } else if(channel == "/$channelId" &&
//                json.has(MESSAGE_TYPE) && json.getInt(MESSAGE_TYPE) == 18
//                && myUserId.compareTo(signal?.signalUniqueUserId!!) != 0
//                && json.getString(MESSAGE_UNIQUE_ID) == signal?.signalUniqueId!!) {
//                if(json.getString(VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.CALL_ACTION.toString()) {
//                    videoCallService.sendUserAction(json.optJSONObject(CUSTOM_DATA))
//                }
//            } else if (channel == "/$channelId" &&
//                json.has(MESSAGE_TYPE) && json.getInt(MESSAGE_TYPE) == 18
//                && myUserId.compareTo(signal?.signalUniqueUserId!!) == 0
//                && json.getString(MESSAGE_UNIQUE_ID) == signal?.signalUniqueId
//            ) {
//                if (json.getString(VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.CALL_REJECTED.toString()) {
//                    videoCallService.onCallRejected(json)
//                } else if (json.getString(VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.USER_BUSY.toString()) {
//                    if (!videoCallService.isCallConnected!!) {
//                        videoCallService.onUserBusyRecieved(json)
//                        userBusyRecieved = true
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private final var TAG = WebRTCSignallingClient::class.simpleName
//    private var mClient: FayeClient? = null
//    private var signal: Signal? = null
//    private var initalCalls = 1
//    private var maxCalls = 15
//    private var isErrorEncountered = false
//    private var pendingsSignalJson: JSONObject? = null
//    private var userBusyRecieved = false
//
//    fun isConnected(): Boolean {
//        return mClient?.isConnectedServer!!
//    }
//
//    fun setUpFayeConnection() {
//        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(videoCallService)
//            .registerReceiver(mHungUp, IntentFilter(VIDEO_CALL_HUNGUP_FROM_NOTIFICATION))
//
//        ConnectionManager.initFayeConnection()
//        ConnectionManager.subScribeChannel("/$channelId")
//        //BusProvider.getInstance().register(this)
//        onConnectedServer()
//
//        /*HippoConfig.getExistingClient {
//            mClient = it
//            mClient!!.connectServer()
//            mClient!!.callListener = this
//        }*/
//    }
//
//    fun passServiceCall(videoCallService: VideoCallService, channelId: Long) {
//        this.videoCallService = videoCallService
//        this.channelId = channelId
//
//        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(videoCallService).registerReceiver(
//            mHungUp, IntentFilter(VIDEO_CALL_HUNGUP_FROM_NOTIFICATION))
//
//        ConnectionManager.initFayeConnection()
//        ConnectionManager.subScribeChannel("/$channelId")
//        //BusProvider.getInstance().register(this)
//        onConnectedServer()
//
//
//        isOfferrecieved = true
//    }
//
//    fun initOtherCalls() {
//        mInitiateStartCalltimer = object : CountDownTimer(300000, 2000) {
//            override fun onFinish() {
//            }
//
//            override fun onTick(millisUntilFinished: Long) {
//                if (initalCalls <= maxCalls && !videoCallService.isReadyForConnection!! && !isErrorEncountered && !userBusyRecieved) {
//                    if (initalCalls == 1) {
//                        initiateVideoCall(false)
//                    } else {
//                        initiateVideoCall(true)
//                    }
//                    initalCalls += 1
//                }
//            }
//
//        }.start()
//    }
//
//    private fun replyWithReadyToConnect() {
//        val readyToConnectJson = JSONObject()
//        readyToConnectJson.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.READY_TO_CONNECT.toString())
//        readyToConnectJson.put(IS_SILENT, true)
//        readyToConnectJson.put(USER_ID, signal?.signalUniqueUserId)
//        readyToConnectJson.put(FULL_NAME, signal?.fullNameOfCalledPerson)
//        readyToConnectJson.put(MESSAGE_TYPE, VIDEO_CALL)
//        readyToConnectJson.put(IS_TYPING, TYPING_SHOW_MESSAGE)
//        readyToConnectJson.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
//        addTurnCredentialsAndDeviceDetails(readyToConnectJson)
//    }
//
//    fun setSignalRequirementModel(signal: Signal?) {
//        this.signal = signal
//    }
//
//    fun initiateVideoCall(isSignalSilent: Boolean) {
//        try {
//            val startCallJson = JSONObject()
//            startCallJson.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.START_CALL.toString())
//            startCallJson.put(IS_SILENT, isSignalSilent)
//            startCallJson.put(USER_ID, signal?.signalUniqueUserId)
//            startCallJson.put(FULL_NAME, signal?.fullNameOfCalledPerson)
//            startCallJson.put(MESSAGE_TYPE, VIDEO_CALL)
//            startCallJson.put(IS_TYPING, TYPING_SHOW_MESSAGE)
//            startCallJson.put(CALL_TYPE, signal?.callType)
//            startCallJson.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
//
//            addTurnCredentialsAndDeviceDetails(startCallJson)
//        } catch (e: Exception) {
//            HippoLog.e("TAG", "In Init Error")
//            if(signal != null) {
//                HippoLog.e("TAG", "In Init Error signal = "+Gson().toJson(signal))
//            } else {
//                HippoLog.e("TAG", "In Init Error signal is null")
//            }
//            e.printStackTrace()
//        }
//
//    }
//
//    fun sendOfferToRemoteUser(jsonObject: JSONObject): JSONObject? {
//        val offerJson = addCommonuserDetails(jsonObject)
//        addTurnCredentialsAndDeviceDetails(offerJson)
//        return offerJson
//    }
//
//    fun sendAnswerToRemoteUser(jsonObject: JSONObject): JSONObject? {
//        val offerJson = addCommonuserDetails(jsonObject)
//        addTurnCredentialsAndDeviceDetails(offerJson)
//        return offerJson
//    }
//
//    fun sendIceCandidates(jsonObject: JSONObject) {
//        val iceCandidateJson = addCommonuserDetails(jsonObject)
//        addTurnCredentialsAndDeviceDetails(iceCandidateJson)
//    }
//
//    fun hangUpCall() {
//        val jsonObject = JSONObject()
//        jsonObject.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.CALL_HUNG_UP.toString())
//        val hangupJson = addCommonuserDetails(jsonObject)
//        addTurnCredentialsAndDeviceDetails(hangupJson)
//        mClient?.unsubscribeAll()
//    }
//
//    fun rejectCall() {
//        val jsonObject = JSONObject()
//        jsonObject.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.CALL_REJECTED.toString())
//        val rejectedJson = addCommonuserDetails(jsonObject)
//        addTurnCredentialsAndDeviceDetails(rejectedJson)
//        mClient?.unsubscribeAll()
//    }
//
//    fun addCommonuserDetails(jsonObject: JSONObject): JSONObject {
//        jsonObject.put(IS_SILENT, true)
//        jsonObject.put(USER_ID, signal?.signalUniqueUserId)
//        jsonObject.put(MESSAGE_TYPE, VIDEO_CALL)
//        jsonObject.put(IS_TYPING, 0)
//        jsonObject.put(IS_SILENT, true)
//        jsonObject.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
//        jsonObject.put(CALL_TYPE, signal?.callType)
//        return jsonObject
//    }
//
//    fun addTurnCredentialsAndDeviceDetails(jsonObject: JSONObject) {
//        val stunServers = JSONArray()
//        val turnServers = JSONArray()
//        val videoCallCredentials = JSONObject()
//
//        HippoLog.e(TAG, "signal = "+Gson().toJson(signal))
//
//        videoCallCredentials.put(TURN_API_KEY, signal?.turnApiKey)
//        videoCallCredentials.put(USER_NAME, signal?.turnUserName)
//        videoCallCredentials.put(CREDENTIAL, signal?.turnCredential)
//        for (i in signal?.stunServers!!.indices) {
//            stunServers.put(signal?.stunServers!!.get(i))
//        }
//        for (i in signal?.turnServers!!.indices) {
//            turnServers.put(signal?.turnServers!!.get(i))
//        }
//
//        videoCallCredentials.put(STUN, stunServers)
//        videoCallCredentials.put(TURN, turnServers)
//
////        videoCallCredentials.put("blockStatus", "NOT_BLOCKED")
////        videoCallCredentials.put("lifetimeDuration", "86400s")
////        videoCallCredentials.put("iceTransportPolicy", "all")
//
//
//        jsonObject.put(TURN_CREDENTIALS, videoCallCredentials)
//        jsonObject.put(DEVICE_PAYLOAD, signal?.deviceDetails)
//        jsonObject.put(CALL_TYPE, signal?.callType)
//        publishSignalToFaye(jsonObject)
//    }
//
//    fun publishSignalToFaye(signalJson: JSONObject) {
//        if (mClient != null) {
//            mClient?.publish("/$channelId", signalJson, object : ConnectionError {
//                override fun onError(signalJson: JSONObject) {
//                    pendingsSignalJson = signalJson
//                    fayeConnectionRetry()
//                }
//            })
//        }
//    }
//
//    var pendingJson: JSONObject? = null
//    var pendingcChannelId: Long? = null
//    private fun hasPendingIntent() {
//        if(pendingJson != null && pendingsSignalJson != null) {
//            mClient?.publish("/" + pendingcChannelId, pendingJson)
//            pendingJson = null
//            pendingcChannelId = null
//        }
//    }
//    fun onBroadcastRecieved(intent: Intent) {
//        if (intent.hasExtra(FuguAppConstant.CHANNEL_ID) && intent.getStringExtra(FuguAppConstant.MESSAGE_UNIQUE_ID) != signal?.signalUniqueId) {
//            if (intent.getStringExtra(FuguAppConstant.VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.START_CALL.toString()) {
//                try {
//                    val json = JSONObject()
//                    json.put(FuguAppConstant.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.USER_BUSY.toString())
//                    json.put(FuguAppConstant.IS_SILENT, true)
//                    json.put(FuguAppConstant.USER_ID, intent.getLongExtra(FuguAppConstant.USER_ID, -1L))
//                    json.put(FuguAppConstant.FULL_NAME, signal?.fullNameOfCalledPerson)
//                    json.put(FuguAppConstant.MESSAGE_TYPE, FuguAppConstant.VIDEO_CALL)
//                    json.put(FuguAppConstant.IS_TYPING, TYPING_SHOW_MESSAGE)
//                    json.put(
//                        FuguAppConstant.MESSAGE_UNIQUE_ID,
//                        intent.getStringExtra(FuguAppConstant.MESSAGE_UNIQUE_ID)
//                    )
//                    val devicePayload = JSONObject()
//                    devicePayload.put(FuguAppConstant.DEVICE_ID, CommonData.getUniqueIMEIId(videoCallService))
//                    devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
//                    devicePayload.put(FuguAppConstant.APP_VERSION, BuildConfig.VERSION_NAME)
//                    devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(videoCallService))
//                    json.put("device_payload", devicePayload)
//
//                    pendingcChannelId = intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L)
//                    pendingJson = json
//
//                    mClient?.publish("/" + intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), json)
////                    mClient?.publish(intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), "/" + intent.getLongExtra(FuguAppConstant.CHANNEL_ID, -1L), json)
//                } catch (e: JSONException) {
//                    e.printStackTrace()
//                }
//
//            }
//        } else if (intent.getStringExtra(FuguAppConstant.VIDEO_CALL_TYPE) == WebRTCCallConstants.VideoCallType.CALL_HUNG_UP.toString()) {
//            videoCallService.onCallHungUp(null, false)
//            if (videoCallService.callDisconnectTime != null) {
//                videoCallService.callDisconnectTime?.cancel()
//            }
//            mClient?.unsubscribeAll()
//        } else if (intent.hasExtra(CUSTOM_DATA)) {
//            val data: String = intent.getStringExtra("data")
//            publishMessage(JSONObject(data))
//        }
//    }
//
//    fun publishMessage(data: JSONObject) {
//        val jsonObject = JSONObject()
//        jsonObject.put(CUSTOM_DATA, data)
//        jsonObject.put(IS_SILENT, true)
//        jsonObject.put(USER_ID, signal?.signalUniqueUserId)
//        jsonObject.put(MESSAGE_TYPE, VIDEO_CALL)
//        jsonObject.put(IS_TYPING, 0)
//        jsonObject.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
//        jsonObject.put(CALL_TYPE, signal?.callType)
//        jsonObject.put(VIDEO_CALL_TYPE, "CUSTOM_DATA")
//        jsonObject.put("server_push", 1)
//        if (mClient != null) {
//            mClient?.publish("/$channelId", jsonObject)
//        }
//    }
//
//    fun publishOperationMessage(data: JSONObject) {
//        val jsonObject = JSONObject()
//        jsonObject.put(CUSTOM_DATA, data)
//        jsonObject.put(IS_SILENT, true)
//        jsonObject.put(USER_ID, signal?.signalUniqueUserId)
//        jsonObject.put(MESSAGE_TYPE, VIDEO_CALL)
//        jsonObject.put(IS_TYPING, 0)
//        jsonObject.put(MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
//        jsonObject.put(CALL_TYPE, signal?.callType)
//        jsonObject.put(VIDEO_CALL_TYPE, "CALL_ACTION")
//        jsonObject.put("server_push", 1)
//        if (mClient != null) {
//            mClient?.publish("/$channelId", jsonObject)
//        }
//    }
//
//    private var mHungUp: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            hangUpCall()
//        }
//    }
//
//
//    public fun fayeConnectionRetry() {
//        if (!HippoCallConfig.getInstance().isNetworkConnected)
//            return
//
//        ConnectionManager.initFayeConnection()
//    }
//
//    private var mInitiateStartCalltimer: CountDownTimer? = null
//    private var isOfferrecieved = false
//
//    fun cancelCounter() {
//        mInitiateStartCalltimer?.cancel()
//    }
//
//    fun isFayeConnected(): Boolean {
//        return isOfferrecieved
//    }
//}
