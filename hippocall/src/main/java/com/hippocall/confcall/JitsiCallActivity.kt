package com.hippocall.confcall

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hippo.BuildConfig
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.*
import com.hippo.eventbus.BusProvider
import com.hippo.helper.BusEvents
import com.hippo.helper.FayeMessage
import com.hippo.langs.Restring
import com.hippo.langs.Restring.getString
import com.hippo.utils.HippoLog
import com.hippo.utils.UniqueIMEIID
import com.hippo.utils.filepicker.ToastUtil
import com.hippocall.*
import com.hippocall.WebRTCCallConstants.Companion.CALL_TYPE
import com.hippocall.WebRTCCallConstants.Companion.IS_SILENT
import com.hippocall.model.FayeVideoCallResponse
import com.hippocall.model.FragmentFlow
import com.hippocall.model.OnCreateChannel
import com.squareup.otto.Subscribe
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.jar.JarInputStream

/**
 * Created by gurmail on 2020-04-06.
 * @author gurmail
 */

public class JitsiCallActivity : Fragment(), FuguAppConstant {
    private var videoCallModel: VideoCallModel? = null
    private var ivHangUp: AppCompatImageView? = null
    private var inviteLink = ""
    private var jitsiUrl = ""
    private var mInitiateStartCalltimer: CountDownTimer? = null
    private var initalCalls = 1
    private var initalCallsIOS = 1
    private var maxCalls = 30
    private var isReadyForConnection = false
    private var isReadyForConnectionIOS = false
    private var tvCallStatus: AppCompatTextView? = null
    private var tvCalledPersonName: AppCompatTextView? = null
    private var tvCallType: AppCompatTextView? = null
    private var ivCalledPersonImage: CircleImageView? = null
    var mListener: AudioManager.OnAudioFocusChangeListener? = null
    var mediaPlayer: MediaPlayer? = null
    var muid = UUID.randomUUID().toString()
    var fagmentLoaded: Boolean = false

    private var initOldCall = false
    private var busyStatus = false

    private var connecting = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_jitsi_call, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fagmentLoaded = true;
        busyStatus = false
        initalCalls = 1
        initalCallsIOS = 1
        initViews(view)
        if(connecting) {
            val str = getString(activity, R.string.fugu_connecting)
            tvCallStatus?.text = str
            if (videoCallModel != null) {
                tvCalledPersonName?.text = videoCallModel?.channelName
                if (videoCallModel?.callType!! == "VIDEO") {
                    val str = getString(activity, R.string.hippo_video_call)
                    tvCallType?.text = str
                } else {
                    val str = getString(activity, R.string.hippo_audio_call)
                    tvCallType?.text = str
                }
                Glide.with(this)
                    .load(videoCallModel?.userThumbnailImage)
                    .placeholder(R.drawable.hippo_ic_call_placeholder)
                    .into(ivCalledPersonImage!!)
            }
            initiateOutgoingRinging()
        } else {
            startCall()
        }

        BusProvider.getInstance().register(this)
    }

    private fun startCall() {

        OngoingCallService.NotificationServiceState.muid = muid
        OngoingCallService.CallState.muid = muid
        //val linkArray = randomVideoConferenceLink()
        if (videoCallModel != null) {
            tvCalledPersonName?.text = videoCallModel?.channelName
            if (videoCallModel?.callType!! == "VIDEO") {
                val str = getString(activity, R.string.hippo_video_call)
                tvCallType?.text = str
            } else {
                val str = getString(activity, R.string.hippo_audio_call)
                tvCallType?.text = str
            }
            Glide.with(this)
                .load(videoCallModel?.userThumbnailImage)
                .placeholder(R.drawable.hippo_ic_call_placeholder)
                .into(ivCalledPersonImage!!)
        }

        //initCall()

        initiateOutgoingRinging()

        object : CountDownTimer(60000, 60000) {
            override fun onFinish() {
                //Log.e("Timer", "Done")
                if(fagmentLoaded) {
                    hangupCall(true)
                }
                activity?.finish()
            }

            override fun onTick(millisUntilFinished: Long) {
            }

        }.start()
    }

    @Subscribe
    public fun onBusFragmentType(data: FragmentFlow) {
        if(data.fragmentType == WebRTCCallConstants.BusFragmentType.MAIN_CALL.toString() && data.type == 1) {
            initCall()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            videoCallModel = arguments!!.getParcelable<VideoCallModel>("videoCallModel")!! as VideoCallModel
            if(arguments!!.containsKey("connecting"))
                connecting = arguments!!.getBoolean("connecting", false)
        }
    }

    private fun initCall() {
        Handler(Looper.getMainLooper()).post {
            mInitiateStartCalltimer = object : CountDownTimer(300000, 2000) {
                override fun onFinish() {
                    hangupCall(true)
                    activity?.finish()
                }

                override fun onTick(millisUntilFinished: Long) {
                    if (initalCalls <= maxCalls && !isReadyForConnection) {
                        if (initalCalls == 1) {
                            startCall(false)
                        } else {
                            startCall(true)
                        }
                        initalCalls += 1
                    }

                    if (initalCallsIOS <= maxCalls && !isReadyForConnectionIOS) {
                        if (initalCallsIOS == 1) {
                            startCallIOS(false)
                        } else {
                            startCallIOS(true)
                        }
                        initalCallsIOS += 1
                    }
                }

            }.start()
        }
    }

    @Subscribe
    public fun onCreateChannel(model: OnCreateChannel) {
        if(model.isChannel) {
            this.videoCallModel = model.videoCallModel
            OngoingCallService.NotificationServiceState.muid = muid
            OngoingCallService.CallState.muid = muid
            object : CountDownTimer(60000, 60000) {
                override fun onFinish() {
                    //Log.e("Timer", "Done")
                    if(fagmentLoaded) {
                        hangupCall(true)
                    }
                    activity?.finish()
                }

                override fun onTick(millisUntilFinished: Long) {
                }

            }.start()
            initCall()
            val str = Restring.getString(activity, R.string.hippo_call_calling)
            tvCallStatus?.text = str
        }
    }

    override fun onStop() {
        try {
            isReadyForConnection = true
            isReadyForConnectionIOS = true
            mInitiateStartCalltimer?.cancel()
            BusProvider.getInstance().unregister(this)
        } catch (e: Exception) {
        }
        super.onStop()
    }

    override fun onDestroy() {
        isReadyForConnection = true
        isReadyForConnectionIOS = true
        mInitiateStartCalltimer?.cancel()
        mediaPlayer?.stop()
        fagmentLoaded = false
        super.onDestroy()
    }

    private fun startCall(isSilent: Boolean) {
        val startCallJson = JSONObject()
        startCallJson.put(IS_SILENT, isSilent)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.START_CONFERENCE.toString())
        startCallJson.put(USER_ID, videoCallModel?.userId)
        startCallJson.put(CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
        startCallJson.put(CALL_TYPE, videoCallModel?.callType)
        startCallJson.put(MESSAGE_UNIQUE_ID, muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        inviteLink = videoCallModel?.inviteLink!!
        jitsiUrl = videoCallModel?.jitsiLink!!
        startCallJson.put(INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(JITSI_URL, videoCallModel?.jitsiLink)

        sendMessage(videoCallModel?.channelId!!, startCallJson)

    }

    private fun startCallIOS(isSilent: Boolean) {
        val startCallJson = JSONObject()
        startCallJson.put(IS_SILENT, isSilent)
        startCallJson.put(
            WebRTCCallConstants.VIDEO_CALL_TYPE,
            WebRTCCallConstants.JitsiCallType.START_CONFERENCE_IOS.toString()
        )
        startCallJson.put(USER_ID, videoCallModel?.userId)
        startCallJson.put(CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
        startCallJson.put(CALL_TYPE, videoCallModel?.callType)
        startCallJson.put(MESSAGE_UNIQUE_ID, muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        /*if (videoCallModel?.callType!! == "AUDIO") {
            inviteLink = linkArray[0] + "/" + linkArray[1] + "#config.startWithVideoMuted=true"
        } else {
            inviteLink = linkArray[0] + "/" + linkArray[1]
        }*/
        startCallJson.put(INVITE_LINK, videoCallModel?.inviteLink)
        startCallJson.put(JITSI_URL, videoCallModel?.jitsiLink)
        sendMessage(videoCallModel?.channelId!!, startCallJson)
    }

    private fun hangupCall(isSilent: Boolean) {
        val startCallJson = JSONObject()
        startCallJson.put(IS_SILENT, isSilent)
        startCallJson.put(
            WebRTCCallConstants.VIDEO_CALL_TYPE,
            WebRTCCallConstants.JitsiCallType.HUNGUP_CONFERENCE.toString()
        )
        startCallJson.put(USER_ID, videoCallModel?.userId)
        startCallJson.put(CHANNEL_ID, videoCallModel?.channelId)
        startCallJson.put(MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
        startCallJson.put(CALL_TYPE, videoCallModel?.callType)
        startCallJson.put(MESSAGE_UNIQUE_ID, muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(INVITE_LINK, inviteLink)
        startCallJson.put(JITSI_URL, jitsiUrl)
        sendMessage(videoCallModel?.channelId!!, startCallJson)
    }


    private fun initViews(view: View) {
        ivHangUp = view.findViewById(R.id.ivHangUp)
        tvCallStatus = view.findViewById(R.id.tvCallStatus)
        tvCalledPersonName = view.findViewById(R.id.tvCalledPersonName)
        tvCallType = view.findViewById(R.id.tvCallType)
        ivCalledPersonImage = view.findViewById(R.id.ivCalledPersonImage)
        val str = Restring.getString(activity, R.string.hippo_call_calling)
        tvCallStatus?.text = str
        ivHangUp?.setOnClickListener {
            hangupCall(true)
            activity?.finish()
        }
    }

    private fun initiateOutgoingRinging() {
        mListener = AudioManager.OnAudioFocusChangeListener { }
        Handler().postDelayed({
            val audio = activity?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.setMode(AudioManager.STREAM_MUSIC)
            if (audio.isBluetoothA2dpOn) {
                audio.startBluetoothSco()
                audio.isSpeakerphoneOn = false
                audio.isBluetoothScoOn = true
                audio.requestAudioFocus(
                    mListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                );
            } else {
                audio.stopBluetoothSco()
                audio.isBluetoothScoOn = false
                audio.isSpeakerphoneOn = videoCallModel?.callType.equals("VIDEO")
            }

            var aa = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            if (videoCallModel?.callType.equals("VIDEO")) {
                mediaPlayer = MediaPlayer.create(activity, R.raw.ringing)
            } else {
                mediaPlayer = MediaPlayer.create(activity, R.raw.ringing, aa, 1)
            }
            mediaPlayer?.setLooping(true)
            mediaPlayer?.start()
        }, 100)
    }

    fun onBackPressed() {

    }


    private fun getDeviceDetails(): JSONObject {
        val devicePayload = JSONObject()
        devicePayload.put(DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(activity))
        devicePayload.put(DEVICE_TYPE, ANDROID_USER)
        devicePayload.put(APP_VERSION, HippoConfig.getInstance().versionName)
        devicePayload.put(DEVICE_DETAILS, CommonData.deviceDetails(activity))
        return devicePayload
    }

    override fun onStart() {
        super.onStart()
        //BusProvider.getInstance().register(this)
    }


    @Subscribe
    public fun onFayeMessageEvent(event: FayeMessage) {
        //Log.d("onFayeMessageEvent", "onFayeMessageEvent "+event.type);
        when (event.type) {
            FuguAppConstant.FayeBusEvent.MESSAGE_RECEIVED.toString() -> onCalling(event.message)
            FuguAppConstant.FayeBusEvent.ERROR.toString(),
            BusEvents.ERROR_RECEIVED.toString()-> onErrorRecieved(event.message)
        }
    }

    private fun onCalling(messageJson: String) {
        try {
            val data = JSONObject(messageJson)
            //Log.e("Video_CONF Reply-->", messageJson.toString())
            if (data.optInt("message_type") != 18)
                return

            if (!data.optLong(USER_ID).equals(videoCallModel?.userId!!) && inviteLink == data.getString(INVITE_LINK)) {
                //System.out.println("==================================================================================")
                when (data.optString(VIDEO_CALL_TYPE)) {
                    FuguAppConstant.JitsiCallType.READY_TO_CONNECT_CONFERENCE.toString() -> {
                        OngoingCallService.NotificationServiceState.inviteLink = data.getString(INVITE_LINK)

                        activity?.runOnUiThread {
                            val str = Restring.getString(activity, R.string.hippo_call_ringing)
                            if(!busyStatus)
                                tvCallStatus?.text = str
                        }

                        if(TextUtils.isEmpty(data.optString(JITSI_URL))) {
                            isReadyForConnection = true
                        }
                        //isReadyForConnection = true

                        val startCallJson = JSONObject()
                        startCallJson.put(IS_SILENT, true)
                        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.OFFER_CONFERENCE.toString())
                        startCallJson.put(USER_ID, videoCallModel?.userId.toString())
                        startCallJson.put(CHANNEL_ID, videoCallModel?.channelId)
                        startCallJson.put(MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
                        startCallJson.put(CALL_TYPE, videoCallModel?.callType)
                        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
                        startCallJson.put(MESSAGE_UNIQUE_ID, muid)
                        startCallJson.put(INVITE_LINK, inviteLink)
                        if(!TextUtils.isEmpty(data.optString(JITSI_URL))) {
                            startCallJson.put(JITSI_URL, data.optString(JITSI_URL))
                        }
                        sendMessage(videoCallModel?.channelId!!, startCallJson)
//                        if(TextUtils.isEmpty(data.optString("jitsi_url"))) {
//                            videoCallModel?.jitsiLink = ""
//                        }
                    }
                    FuguAppConstant.JitsiCallType.READY_TO_CONNECT_CONFERENCE_IOS.toString() -> {
                        OngoingCallService.NotificationServiceState.inviteLink = data.getString(INVITE_LINK)

                        activity?.runOnUiThread {
                            val str = Restring.getString(activity, R.string.hippo_call_ringing)
                            if(!busyStatus)
                                tvCallStatus?.text = str
                        }
                        if(TextUtils.isEmpty(data.optString(JITSI_URL))) {
                            isReadyForConnectionIOS = true
                        }

                        val startCallJson = JSONObject()
                        startCallJson.put(IS_SILENT, true)
                        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE,WebRTCCallConstants.JitsiCallType.OFFER_CONFERENCE.toString())
                        startCallJson.put(USER_ID, videoCallModel?.userId.toString())
                        startCallJson.put(CHANNEL_ID, videoCallModel?.channelId)
                        startCallJson.put(MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
                        startCallJson.put(CALL_TYPE, videoCallModel?.callType)
                        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
                        startCallJson.put(MESSAGE_UNIQUE_ID, muid)
                        startCallJson.put(INVITE_LINK, inviteLink)
                        if(!TextUtils.isEmpty(data.optString(JITSI_URL))) {
                            startCallJson.put(JITSI_URL, data.optString(JITSI_URL))
                        }
                        sendMessage(videoCallModel?.channelId!!, startCallJson)

                        sendDate(WebRTCCallConstants.JitsiCallActivity.PRE_LOAD_DATA, null)
                    }
                    FuguAppConstant.JitsiCallType.ANSWER_CONFERENCE.toString() -> {
                        isReadyForConnectionIOS = true
                        isReadyForConnection = true


                        if(TextUtils.isEmpty(data.optString("jitsi_url"))) {
                            videoCallModel?.jitsiLink = ""
                        }

//                        val linkArray = data.getString("invite_link")
//                            .replace("#config.startWithVideoMuted=true", "").split("/")
                        if (!OngoingCallService.NotificationServiceState.isConferenceServiceRunning) {
                            OngoingCallService.NotificationServiceState.isConferenceServiceRunning = true
                            sendDate(WebRTCCallConstants.JitsiCallActivity.OPEN_VIDEO_CONF, data)
                        }
                    }
                    FuguAppConstant.JitsiCallType.REJECT_CONFERENCE.toString() -> {
                        mInitiateStartCalltimer?.cancel()
                        activity?.runOnUiThread {
                            val str = Restring.getString(activity, R.string.hippo_call_rejected)
                            tvCallStatus?.text = str
                        }
                        mediaPlayer?.stop()
                        val aa = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        if (videoCallModel?.callType.equals("VIDEO")) {
                            mediaPlayer = MediaPlayer.create(activity, R.raw.busy_tone)
                        } else {
                            mediaPlayer = MediaPlayer.create(activity, R.raw.busy_tone, aa, 1)
                        }

                        mediaPlayer?.isLooping = false
                        mediaPlayer?.start()

                        Handler(Looper.getMainLooper()).postDelayed({
                            activity?.finish()
                        }, 3000)
                    }
                    FuguAppConstant.JitsiCallType.USER_BUSY_CONFERENCE.toString() -> {
                        activity?.runOnUiThread {
                            val str = Restring.getString(activity, R.string.hippo_busy_on_call)
                            tvCallStatus?.text = str
                            busyStatus = true
                            if (mediaPlayer != null) {
                                mediaPlayer?.stop()
                                mediaPlayer = MediaPlayer.create(activity, R.raw.busy_tone)
                                mediaPlayer?.setLooping(false)
                                mediaPlayer?.start()
                            }
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            activity?.finish()
                        }, 3000)
                    }
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    public fun onErrorRecieved(messageJson: String) {
        activity?.runOnUiThread {
            try {
                val fayeVideoCallResponse =
                    Gson().fromJson(messageJson, FayeVideoCallResponse::class.java)
                if (fayeVideoCallResponse.statusCode == 415) {
                    //Log.e("Jitsi Error", messageJson)
                    mInitiateStartCalltimer?.cancel()
                    val name = tvCalledPersonName?.text
//                    ToastUtil.getInstance(activity).showToast("$name does not have the updated app, Calling using old SDK... ")
                    val str = Restring.getString(activity, R.string.hippo_calling_from_old)
                    Toast.makeText(activity,"$name $str",Toast.LENGTH_SHORT).show()
                    object : CountDownTimer(2000, 1000) {
                        override fun onFinish() {
                            if(!initOldCall) {
                                initOldCall = true
                                sendDate(WebRTCCallConstants.JitsiCallActivity.OPEN_OLD_CALL, null)
                            }
                            //HippoCallConfig.getInstance().initOldCall(videoCallModel)
                            //activity?.setResult(Activity.RESULT_OK)
                            //activity?.finish()
                        }

                        override fun onTick(millisUntilFinished: Long) {
                        }

                    }.start()
                }
            } catch (e: Exception) {

            }
        }
    }


    private fun sendMessage(channelId: Long, jsonObject: JSONObject) {
        jsonObject.put("message", "")
        jsonObject.put("is_typing", TYPING_SHOW_MESSAGE)
        jsonObject.put("user_type", FuguAppConstant.ANDROID_USER)
        jsonObject.put(WebRTCCallConstants.FULL_NAME, videoCallModel?.myname)
        jsonObject.put(WebRTCCallConstants.USER_THUMBNAIL_IMAGE, videoCallModel?.myImagePath)

        sendDate(WebRTCCallConstants.JitsiCallActivity.POST_DATA, jsonObject)
    }

    private fun sendDate(type: Int, jsonObject: JSONObject?) {
        try {
            BusProvider.getInstance().post(FragmentFlow(WebRTCCallConstants.BusFragmentType.JITSI_CALL.toString(), type, jsonObject, ""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}