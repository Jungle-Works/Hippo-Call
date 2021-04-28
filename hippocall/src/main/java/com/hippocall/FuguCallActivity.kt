package com.hippocall


import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PictureInPictureParams
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.core.content.PermissionChecker
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import android.text.TextUtils
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.CHANNEL_NAME
import com.hippo.langs.Restring
import com.hippo.model.FuguCreateConversationParams
import com.hippo.model.FuguCreateConversationResponse
import com.hippo.retrofit.*
import com.hippo.utils.HippoLog
import com.hippo.utils.UniqueIMEIID
import com.hippocall.WebRTCCallConstants.Companion.CALLING
import com.hippocall.WebRTCCallConstants.Companion.CALL_STATUS
import com.hippocall.WebRTCCallConstants.Companion.CONNECTING
import com.hippocall.WebRTCCallConstants.Companion.DISCONNECTING
import com.hippocall.WebRTCCallConstants.Companion.OFFER_TO_RECEIVE_AUDIO
import com.hippocall.WebRTCCallConstants.Companion.OFFER_TO_RECEIVE_VIDEO
import com.hippocall.WebRTCCallConstants.Companion.ONGOING_AUDIO_CALL
import com.hippocall.WebRTCCallConstants.Companion.ONGOING_VIDEO_CALL
import com.hippocall.WebRTCCallConstants.Companion.REJECTED
import com.hippocall.WebRTCCallConstants.Companion.RINGING
import com.hippocall.WebRTCCallConstants.Companion.USER_BUSY
import com.hippocall.WebRTCCallConstants.Companion.VIDEO_CALL_HUNGUP_FROM_NOTIFICATION
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.new_hippo_activity_video_call.*
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*
import java.util.*

/**
 * Created by rajatdhamija
 * 20/09/18.
 */

class FuguCallActivity : AppCompatActivity(), View.OnClickListener, WebRTCFayeCallbacks, WebRTCCallCallbacks, CallTouchListener.OnCallItemTouchListener, UpdateView {

    var ivCalledPersonImage: CircleImageView? = null
    var ivIncomingPersonImage: CircleImageView? = null
    var ivIncomingPersonImageBig: AppCompatImageView? = null
    var tvCalledPersonName: AppCompatTextView? = null
//    var tvCallingStatus: AppCompatTextView? = null
    var tvCallType: AppCompatTextView? = null
    var tvCallTypeIncoming: AppCompatTextView? = null
    var ivHangUp: AppCompatImageView? = null
    var ivMuteAudio: AppCompatImageView? = null
    var ivSpeaker: AppCompatImageView? = null
    var ivSwitchCamera: AppCompatImageView? = null
    var ivMuteVideo: AppCompatImageView? = null
    var localSurfaceView: SurfaceViewRenderer? = null
    var tvIncomingPersonName: AppCompatTextView? = null
    var ivRejectCall: AppCompatImageView? = null
    var ivAnswerCall: AppCompatImageView? = null
    var remoteSurfaceview: SurfaceViewRenderer? = null
    var llLocalView: RelativeLayout? = null
    var activitylaunchState: String? = ""
    var signal: Signal? = null
    private var rootEglBase: EglBase? = null
    var connection: Connection? = null
    private var sdpConstraints: MediaConstraints? = null
    private var videoStream: MediaStream? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoCallModel: VideoCallModel? = null
    private var remoteVideoStream: MediaStream? = null
    private var videoOfferjson: JSONObject? = null
//    private var isAudioEnabled = true
    private var isVideoEnabled = true
    private var isFrontFacingCamera = true
    private var isLocalViewSmall = true
    private var isCallOptionsVisible = true
    var mediaPlayer: MediaPlayer? = null
    var callStatus: String = ""
    var isAlreadyHungUp = false
    var mBounded = false
    var videoCallService: VideoCallService? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var callDisconnectTimer: CountDownTimer? = null
    private var wiredHeadsetReceiver: BroadcastReceiver? = null
    private val STATE_UNPLUGGED = 0
    private val STATE_PLUGGED = 1
    private val HAS_NO_MIC = 0
    private val HAS_MIC = 1
    private var isWirelessHeadSetConnected = false
    private var tvCallTimer: AppCompatTextView? = null
    private var ivBack: AppCompatImageView? = null
    private var outgoingCallLayout: RelativeLayout? = null
    private var incomingCallLayout: RelativeLayout? = null
    private var vibrate: Vibrator? = null
    private var isOnSpeaker = false
    private var isOnBluetooth = false
    private var mProximityController: ProximitySensorController? = null
    private var mPowerManager: PowerManager? = null
    private var mWakeLoack: PowerManager.WakeLock? = null
    private var mProximityWakeLock: PowerManager.WakeLock? = null
    private var mPartialWakeLock: PowerManager.WakeLock? = null
    private var field = 0x00000020;
    private var isCallAnswered = false
    private var mainRoot: RelativeLayout? = null
    private var isAnswerClicked = false

    private var wirelessContext: Context? = null
    private var wirelessIntent: Intent? = null
    private var otherUserId = -1L

    var ivReplyCall: AppCompatImageView? = null
    var llReject: LinearLayout? = null
    var llReply: LinearLayout? = null
    var llAnswer: LinearLayout? = null
    var answerRoot: LinearLayout? = null
    private val animTime = 150
    var answerImagesList = ArrayList<AppCompatImageView>()
    var rejectImagesList = ArrayList<AppCompatImageView>()
    var replyImagesList = ArrayList<AppCompatImageView>()

    var tvRejectCall: AppCompatTextView? = null
    var tvReplyCall: AppCompatTextView? = null
    var tvAnswerCall: AppCompatTextView? = null
    var tvCallAction: AppCompatTextView? = null

    var ivMuteIcon: AppCompatImageView? = null
    var ivVideoIcon: AppCompatImageView? = null

    var image: ImageView? = null

    var countDownTimer: CountDownTimer? = null
    val set = AnimatorSet()
    var stopAudioVideo = false

    var ivBluetooth: AppCompatImageView? = null
    var rlCallingButtons: RelativeLayout? = null

    var llConnectivityIssues: LinearLayout? = null

    var ivHungupCallTimer: CountDownTimer? = null

    var hasAlreadyInitializedViews = false
    var mListener: AudioManager.OnAudioFocusChangeListener? = null
    var lowerCallOptions: LinearLayout? = null
    private var isConnecting: Boolean = false
    private var isHungup: Boolean = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_hippo_activity_video_call)
        val win = window
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        mListener = AudioManager.OnAudioFocusChangeListener { }
        if (intent.hasExtra("videoCallModel")) {
            videoCallModel = intent.extras?.getParcelable("videoCallModel")
            activitylaunchState = videoCallModel?.activityLaunchState
            askForPermissions()
            initializeViews()
            if (WebRTCCallConstants.AcitivityLaunchState.OTHER.toString() == activitylaunchState) {
                outgoingCallLayout?.visibility = View.GONE
                incomingCallLayout?.visibility = View.VISIBLE
                tvIncomingPersonName?.visibility = View.VISIBLE
                ivIncomingPersonImage?.visibility = View.VISIBLE
                ivIncomingPersonImageBig?.visibility = View.VISIBLE
                ivAnswerCall?.visibility = View.VISIBLE
                ivRejectCall?.visibility = View.VISIBLE
                tvIncomingPersonName?.text = videoCallModel?.channelName
                initiateIncomingRinging()
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    tvCallTypeIncoming?.text = "Video Call"
                    ivMuteVideo?.visibility = View.VISIBLE
                    ivSwitchCamera?.visibility = View.VISIBLE
                    ivSpeaker?.visibility = View.GONE
                } else {
                    tvCallTypeIncoming?.text = "Voice Call"
                    ivMuteVideo?.visibility = View.GONE
                    ivSwitchCamera?.visibility = View.GONE
                    ivSpeaker?.visibility = View.VISIBLE
                }
                tvCalledPersonName?.text = videoCallModel?.channelName


                val options = RequestOptions()
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_hippo_user_image)
                    .error(R.drawable.ic_hippo_user_image)
                    .fitCenter()
                    .priority(Priority.HIGH)
                    .transforms(CenterCrop(), RoundedCorners(1000))

                val optionsBigImage = RequestOptions()
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_hippo_user_image)
                    .error(R.drawable.ic_hippo_user_image)
                    .fitCenter()
                    .priority(Priority.HIGH)
                    .transforms(CenterCrop())
                Glide.with(this)
                    .asBitmap()
                    .apply(options)
                    .load(videoCallModel?.userThumbnailImage)
                    .into(ivIncomingPersonImage!!)
                Glide.with(this)
                    .asBitmap()
                    .apply(optionsBigImage)
                    .load(videoCallModel?.userThumbnailImage)
                    .into(ivIncomingPersonImageBig!!)
            } else if (WebRTCCallConstants.AcitivityLaunchState.SELF.toString() == activitylaunchState) {
                outgoingCallLayout?.visibility = View.VISIBLE
                incomingCallLayout?.visibility = View.GONE
                tvCalledPersonName?.visibility = View.VISIBLE
                ivCalledPersonImage?.visibility = View.VISIBLE
                ivMuteAudio?.visibility = View.VISIBLE
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    tvCallType?.text = "Video Call"
                    ivMuteVideo?.visibility = View.VISIBLE
                    ivSwitchCamera?.visibility = View.VISIBLE
                    ivSpeaker?.visibility = View.GONE
                } else {
                    tvCallType?.text = "Voice Call"
                    ivMuteVideo?.visibility = View.GONE
                    ivSwitchCamera?.visibility = View.GONE
                    ivSpeaker?.visibility = View.VISIBLE
                }

                ivHangUp?.visibility = View.VISIBLE

//                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
//                    localSurfaceView?.visibility = View.VISIBLE
//                }

//                tvCallingStatus?.visibility = View.VISIBLE
//                tvCallingStatus?.text = RINGING
                initiateOutgoingRinging()
                callStatus = WebRTCCallConstants.CallStatus.OUTGOING_CALL.toString()
                CommonData.setCallStatus(callStatus)

                tvCalledPersonName?.text = videoCallModel?.channelName


                val options = RequestOptions()
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_hippo_user_image)
                    .error(R.drawable.ic_hippo_user_image)
                    .fitCenter()
                    .priority(Priority.HIGH)
                    .transforms(CenterCrop(), RoundedCorners(1000))


                Glide.with(this)
                    .asBitmap()
                    .apply(options)
                    .load(videoCallModel?.userThumbnailImage)
                    .into(ivCalledPersonImage!!)
            }
        } else {
            askForPermissions()
        }

        HippoCallConfig.getInstance().setTimmerListener(this)
        if(CommonData.hasExtraView()) {
            addingView()
        }

    }

    private fun fetchIntentData(offers: String) {
        videoCallModel = intent.extras?.getParcelable("videoCallModel")
        videoCallService?.setVideoModel(videoCallModel)
        activitylaunchState = videoCallModel?.activityLaunchState
        val devicePayload = JSONObject()
        devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(this))
        devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
        devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
        devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(this))
        signal = Signal(videoCallModel?.userId, videoCallModel?.signalUniqueId, videoCallModel?.fullName,
            videoCallModel?.turnApiKey, videoCallModel?.turnUserName, videoCallModel?.turnCredential
            , videoCallModel?.stunServers, videoCallModel?.turnServers, devicePayload, videoCallModel?.callType!!)
        videoCallService?.setSignalModel(signal)
        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
            Handler().postDelayed({
                if (isCallOptionsVisible) {
                    isCallOptionsVisible = !isCallOptionsVisible
                    hidecallOptionsAnimation()
                }
            }, 3000)
        }
        when (activitylaunchState) {
            WebRTCCallConstants.AcitivityLaunchState.SELF.toString() -> {
                outgoingCallLayout?.visibility = View.VISIBLE
                incomingCallLayout?.visibility = View.GONE
                tvCalledPersonName?.visibility = View.VISIBLE
                ivCalledPersonImage?.visibility = View.VISIBLE
                ivMuteAudio?.visibility = View.VISIBLE
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    tvCallType?.text = "Video Call"
                    ivMuteVideo?.visibility = View.VISIBLE
                    ivSwitchCamera?.visibility = View.VISIBLE
                    ivSpeaker?.visibility = View.GONE
//                    if (videoCallModel?.callType.equals("VIDEO")) {
//                        tvCallTimer?.visibility = View.GONE
//                    }
                } else {
                    tvCallType?.text = "Voice Call"
                    ivMuteVideo?.visibility = View.GONE
                    ivSwitchCamera?.visibility = View.GONE
                    ivSpeaker?.visibility = View.VISIBLE
                }

                ivHangUp?.visibility = View.VISIBLE

                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    localSurfaceView?.visibility = View.VISIBLE
                }

//                tvCallingStatus?.visibility = View.VISIBLE
//                tvCallingStatus?.text = RINGING
//                initiateOutgoingRinging()
                callStatus = WebRTCCallConstants.CallStatus.OUTGOING_CALL.toString()
                CommonData.setCallStatus(callStatus)

                tvCalledPersonName?.text = videoCallModel?.channelName


                val options = RequestOptions()
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_hippo_user_image)
                    .error(R.drawable.ic_hippo_user_image)
                    .fitCenter()
                    .priority(Priority.HIGH)
                    .transforms(CenterCrop(), RoundedCorners(1000))


                Glide.with(this)
                    .asBitmap()
                    .apply(options)
                    .load(videoCallModel?.userThumbnailImage)
                    .into(ivCalledPersonImage!!)
            }
            WebRTCCallConstants.AcitivityLaunchState.OTHER.toString() -> {
                outgoingCallLayout?.visibility = View.GONE
                incomingCallLayout?.visibility = View.VISIBLE
                tvIncomingPersonName?.visibility = View.VISIBLE
                ivIncomingPersonImage?.visibility = View.VISIBLE
                ivIncomingPersonImageBig?.visibility = View.VISIBLE
                ivAnswerCall?.visibility = View.VISIBLE
                ivRejectCall?.visibility = View.VISIBLE
                tvIncomingPersonName?.text = videoCallModel?.channelName
//                initiateIncomingRinging()
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    tvCallTypeIncoming?.text = "Video Call"
                    ivMuteVideo?.visibility = View.VISIBLE
                    ivSwitchCamera?.visibility = View.VISIBLE
                    ivSpeaker?.visibility = View.GONE
//                    if (videoCallModel?.callType.equals("VIDEO")) {
//                        tvCallTimer?.visibility = View.GONE
//                    }
                } else {
                    tvCallTypeIncoming?.text = "Voice Call"
                    ivMuteVideo?.visibility = View.GONE
                    ivSwitchCamera?.visibility = View.GONE
                    ivSpeaker?.visibility = View.VISIBLE
                }
                callStatus = WebRTCCallConstants.CallStatus.INCOMING_CALL.toString()
                CommonData.setCallStatus(callStatus)
                tvCalledPersonName?.text = videoCallModel?.channelName


                val options = RequestOptions()
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_hippo_user_image)
                    .error(R.drawable.ic_hippo_user_image)
                    .fitCenter()
                    .priority(Priority.HIGH)
                    .transforms(CenterCrop(), RoundedCorners(1000))

                val optionsBigImage = RequestOptions()
                    .centerCrop()
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_hippo_user_image)
                    .error(R.drawable.ic_hippo_user_image)
                    .fitCenter()
                    .priority(Priority.HIGH)
                    .transforms(CenterCrop())
                Glide.with(this)
                    .asBitmap()
                    .apply(options)
                    .load(videoCallModel?.userThumbnailImage)
                    .into(ivIncomingPersonImage!!)
                Glide.with(this)
                    .asBitmap()
                    .apply(optionsBigImage)
                    .load(videoCallModel?.userThumbnailImage)
                    .into(ivIncomingPersonImageBig!!)
            }
        }
        if(TextUtils.isEmpty(offers))
            videoCallService?.createWebRTCSignallingConnection(videoCallModel, signal)
        else
            videoCallService?.createWebRTCSignallingConnection(JSONObject(offers), videoCallModel, signal)
        //videoCallService?.createWebRTCSignallingConnection(videoCallModel, signal)
    }

    /**
     * Initialization of Views
     */
    private fun initializeViews() {
        llConnectivityIssues = findViewById(R.id.llConnectivityIssues)
        ivCalledPersonImage = findViewById(R.id.ivCalledPersonImage)
        ivIncomingPersonImage = findViewById(R.id.ivIncomingPersonImage)
        ivIncomingPersonImageBig = findViewById(R.id.ivIncomingPersonImageBig)
        tvCalledPersonName = findViewById(R.id.tvCalledPersonName)
        tvCallTimer = findViewById(R.id.tvCallTimer)
//        tvCallingStatus = findViewById(R.id.tvCallingStatus)
        tvCallType = findViewById(R.id.tvCallType)
        tvCallTypeIncoming = findViewById(R.id.tvCallTypeIncoming)
        ivHangUp = findViewById(R.id.ivHangUp)
        ivMuteAudio = findViewById(R.id.ivMuteAudio)
        ivSpeaker = findViewById(R.id.ivSpeaker)
        ivSwitchCamera = findViewById(R.id.ivSwitchCamera)
        ivMuteVideo = findViewById(R.id.ivMuteVideo)
        localSurfaceView = findViewById(R.id.localSurfaceView)
        tvIncomingPersonName = findViewById(R.id.tvIncomingPersonName)
        ivRejectCall = findViewById(R.id.ivRejectCall)
        ivAnswerCall = findViewById(R.id.ivAnswerCall)
        remoteSurfaceview = findViewById(R.id.remoteSurfaceview)
        ivBack = findViewById(R.id.ivBack)
        outgoingCallLayout = findViewById(R.id.outgoingCallLayout)
        incomingCallLayout = findViewById(R.id.incomingCallLayout)
        mainRoot = findViewById(R.id.mainRoot)
        llLocalView = findViewById(R.id.llLocalView)
        wiredHeadsetReceiver = WiredHeadsetReceiver()
        remoteSurfaceview?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)

        tvTimerView = findViewById(R.id.call_timer)
        view = findViewById(R.id.custom_view)

        if (videoCallModel?.callType.equals("AUDIO", ignoreCase = true)) {
            lowerCallOptions = findViewById(R.id.lowerCallOptions)
            try {
                lowerCallOptions?.setBackgroundColor(com.hippo.database.CommonData.getColorConfig().hippoActionBarBg)
            } catch (e: Exception) {
            }
        }

        ivReplyCall = findViewById(R.id.ivReplyCall)
        ivBluetooth = findViewById(R.id.ivBluetooth)
        llReject = findViewById(R.id.llReject)
        llReply = findViewById(R.id.llReply)
        llAnswer = findViewById(R.id.llAnswer)

        tvReplyCall = findViewById(R.id.tvReply)
        tvRejectCall = findViewById(R.id.tvReject)
        tvAnswerCall = findViewById(R.id.tvAnswer)

        answerRoot = findViewById(R.id.answerRoot)
        tvCallAction = findViewById(R.id.tvCallAction)

        ivMuteIcon = findViewById(R.id.ivMuteIcon)
        ivVideoIcon = findViewById(R.id.ivVideoIcon)

        answerImagesList.add(findViewById(R.id.pick_call_arrow_up_one))
        answerImagesList.add(findViewById(R.id.pick_call_arrow_up_two))
        answerImagesList.add(findViewById(R.id.pick_call_arrow_up_three))
        answerImagesList.add(findViewById(R.id.pick_call_arrow_up_four))

        replyImagesList.add(findViewById(R.id.reply_call_arrow_up_one))
        replyImagesList.add(findViewById(R.id.reply_call_arrow_up_two))
        replyImagesList.add(findViewById(R.id.reply_call_arrow_up_three))
        replyImagesList.add(findViewById(R.id.reply_call_arrow_up_four))

        rejectImagesList.add(findViewById(R.id.reject_call_arrow_up_one))
        rejectImagesList.add(findViewById(R.id.reject_call_arrow_up_two))
        rejectImagesList.add(findViewById(R.id.reject_call_arrow_up_three))
        rejectImagesList.add(findViewById(R.id.reject_call_arrow_up_four))

        startAcceptAnimation(answerImagesList)
        startRejectAnimation(rejectImagesList)
        //startReplyAnimation(replyImagesList)
        rlCallingButtons = findViewById(R.id.rlCallingButtons)
        ivAnswerCall?.setOnTouchListener(CallTouchListener(answerRoot, ivAnswerCall, this))
        ivReplyCall?.setOnTouchListener(CallTouchListener(answerRoot, ivReplyCall, this))
        ivRejectCall?.setOnTouchListener(CallTouchListener(answerRoot, ivRejectCall, this))
        onShakeImage()
    }

    private fun startAcceptAnimation(imagesList: ArrayList<AppCompatImageView>) {

        try {
            set.cancel()
        } catch (e: Exception) {

        }

        for (image in imagesList) {
            image.clearAnimation()
        }
        val arrowOneFadeIn = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 0f, 1f)
        arrowOneFadeIn.duration = animTime.toLong()
        val arrowTwoFadeIn = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 0f, 1f)
        arrowTwoFadeIn.setDuration(animTime.toLong()).startDelay = animTime.toLong()
        val arrowOneFadeOut = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 1f, 0f)
        arrowOneFadeOut.setDuration(animTime.toLong()).startDelay = 375
        val arrowThreeFadeIn = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 0f, 1f)
        arrowThreeFadeIn.setDuration(animTime.toLong()).startDelay = 300
        val arrowTwoFadeOut = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 1f, 0f)
        arrowTwoFadeOut.setDuration(animTime.toLong()).startDelay = 525
        val arrowFourFadeIn = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 0f, 1f)
        arrowFourFadeIn.setDuration(animTime.toLong()).startDelay = 450
        val arrowThreeFadeOut = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 1f, 0f)
        arrowThreeFadeOut.setDuration(animTime.toLong()).startDelay = 675
        val arrowFourFadeOut = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 1f, 0f)
        arrowFourFadeOut.setDuration(animTime.toLong()).startDelay = 825
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                set.start()
            }
        })

        set.play(arrowOneFadeIn)
        set.play(arrowTwoFadeIn)
        set.play(arrowOneFadeOut)
        set.play(arrowThreeFadeIn)
        set.play(arrowTwoFadeOut)
        set.play(arrowFourFadeIn)
        set.play(arrowThreeFadeOut)
        set.play(arrowFourFadeOut)
        set.start()
    }

    private fun startReplyAnimation(imagesList: ArrayList<AppCompatImageView>) {

        try {
            set.cancel()
        } catch (e: Exception) {

        }

        for (image in imagesList) {
            image.clearAnimation()
        }
        val arrowOneFadeIn = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 0f, 1f)
        arrowOneFadeIn.duration = animTime.toLong()
        val arrowTwoFadeIn = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 0f, 1f)
        arrowTwoFadeIn.setDuration(animTime.toLong()).startDelay = animTime.toLong()
        val arrowOneFadeOut = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 1f, 0f)
        arrowOneFadeOut.setDuration(animTime.toLong()).startDelay = 375
        val arrowThreeFadeIn = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 0f, 1f)
        arrowThreeFadeIn.setDuration(animTime.toLong()).startDelay = 300
        val arrowTwoFadeOut = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 1f, 0f)
        arrowTwoFadeOut.setDuration(animTime.toLong()).startDelay = 525
        val arrowFourFadeIn = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 0f, 1f)
        arrowFourFadeIn.setDuration(animTime.toLong()).startDelay = 450
        val arrowThreeFadeOut = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 1f, 0f)
        arrowThreeFadeOut.setDuration(animTime.toLong()).startDelay = 675
        val arrowFourFadeOut = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 1f, 0f)
        arrowFourFadeOut.setDuration(animTime.toLong()).startDelay = 825
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                set.start()
            }
        })

        set.play(arrowOneFadeIn)
        set.play(arrowTwoFadeIn)
        set.play(arrowOneFadeOut)
        set.play(arrowThreeFadeIn)
        set.play(arrowTwoFadeOut)
        set.play(arrowFourFadeIn)
        set.play(arrowThreeFadeOut)
        set.play(arrowFourFadeOut)
        set.start()
    }

    private fun startRejectAnimation(imagesList: ArrayList<AppCompatImageView>) {

        try {
            set.cancel()
        } catch (e: Exception) {

        }

        for (image in imagesList) {
            image.clearAnimation()
        }
        val arrowOneFadeIn = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 0f, 1f)
        arrowOneFadeIn.duration = animTime.toLong()
        val arrowTwoFadeIn = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 0f, 1f)
        arrowTwoFadeIn.setDuration(animTime.toLong()).startDelay = animTime.toLong()
        val arrowOneFadeOut = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 1f, 0f)
        arrowOneFadeOut.setDuration(animTime.toLong()).startDelay = 375
        val arrowThreeFadeIn = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 0f, 1f)
        arrowThreeFadeIn.setDuration(animTime.toLong()).startDelay = 300
        val arrowTwoFadeOut = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 1f, 0f)
        arrowTwoFadeOut.setDuration(animTime.toLong()).startDelay = 525
        val arrowFourFadeIn = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 0f, 1f)
        arrowFourFadeIn.setDuration(animTime.toLong()).startDelay = 450
        val arrowThreeFadeOut = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 1f, 0f)
        arrowThreeFadeOut.setDuration(animTime.toLong()).startDelay = 675
        val arrowFourFadeOut = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 1f, 0f)
        arrowFourFadeOut.setDuration(animTime.toLong()).startDelay = 825
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                set.start()
            }
        })

        set.play(arrowOneFadeIn)
        set.play(arrowTwoFadeIn)
        set.play(arrowOneFadeOut)
        set.play(arrowThreeFadeIn)
        set.play(arrowTwoFadeOut)
        set.play(arrowFourFadeIn)
        set.play(arrowThreeFadeOut)
        set.play(arrowFourFadeOut)
        set.start()
    }

    private fun onShakeImage() {
        val shake: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.shake)
        val slideUp: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.slide_up_call)
        image = findViewById(R.id.ivAnswerCall)
        image?.startAnimation(slideUp)

        Handler().postDelayed({
            image?.clearAnimation()
            image?.startAnimation(shake)
        }, 1500)

        Handler().postDelayed({ onShakeImage() }, 2700)

    }

    /**
     * Add Click listeners to views
     */
    private fun initializeClickListeners() {
        ivHangUp?.setOnClickListener(this)
        ivMuteAudio?.setOnClickListener(this)
        ivSwitchCamera?.setOnClickListener(this)
        ivMuteVideo?.setOnClickListener(this)
        localSurfaceView?.setOnClickListener(this)
        ivRejectCall?.setOnClickListener(this)
        ivAnswerCall?.setOnClickListener(this)
        remoteSurfaceview?.setOnClickListener(this)
        ivBack?.setOnClickListener(this)
        ivSpeaker?.setOnClickListener(this)
        mainRoot?.setOnClickListener(this)
        ivBluetooth?.setOnClickListener(this)
    }

    fun onCallFailed() {
        isHungup = true
        stopAudioVideo = true
        val disconnect = Restring.getString(this@FuguCallActivity, R.string.hippo_disconnect)
        tvCallTimer?.text = DISCONNECTING
        videoCallService?.isReadyForConnection = true
        videoCallService?.hungUpCall(false)
        if (ivHungupCallTimer == null) {
            ivHungupCallTimer = object : CountDownTimer(1000, 500) {
                override fun onFinish() {
                    onHungupSent()
                }
                override fun onTick(millisUntilFinished: Long) {
                }
            }.start()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivHangUp -> {
                isHungup = true
                stopAudioVideo = true
                tvCallTimer?.text = DISCONNECTING
                videoCallService?.isReadyForConnection = true
                videoCallService?.hungUpCall()
                if (ivHungupCallTimer == null) {
                    ivHungupCallTimer = object : CountDownTimer(2000, 1000) {
                        override fun onFinish() {
                            onHungupSent()
                        }

                        override fun onTick(millisUntilFinished: Long) {
                        }

                    }.start()

                }
            }
            R.id.ivMuteAudio -> {
                videoStream?.audioTracks?.get(0)?.setEnabled(!videoCallService?.isAudioEnabled!!)
                val drawable = if (videoCallService?.isAudioEnabled!!) R.drawable.ic_mute_microphone_disabled else R.drawable.ic_mute_microphone_no_bg
                ivMuteAudio?.setImageResource(drawable)
                videoCallService?.isAudioEnabled = !videoCallService?.isAudioEnabled!!
                sendButtonStatus()
            }
            R.id.ivSwitchCamera -> {
                switchCameraRecorder()
            }
            R.id.ivMuteVideo -> {
                videoStream?.videoTracks?.get(0)?.setEnabled(!isVideoEnabled)
                val isFlip = CommonData.getMirroeStatus(HippoCallConfig.FLIP_CAMERA)
                if(isFlip) {
                    val drawable = if (isVideoEnabled) R.drawable.ic_mute_video_no_bg else R.drawable.ic_mute_video_disabled
                    ivMuteVideo?.setImageResource(drawable)
                } else {
                    val drawable = if (isVideoEnabled) R.drawable.ic_mute_video_disabled else R.drawable.ic_mute_video_no_bg
                    ivMuteVideo?.setImageResource(drawable)
                }
                //val drawable = if (isVideoEnabled) R.drawable.ic_mute_video_disabled else R.drawable.ic_mute_video_no_bg
                //ivMuteVideo?.setImageResource(drawable)
                isVideoEnabled = !isVideoEnabled
                sendButtonStatus()
            }
            R.id.ivSpeaker -> {

                if (isOnSpeaker) {
                    isOnSpeaker = false
                    isOnBluetooth = false
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    am.stopBluetoothSco()
                    am.isSpeakerphoneOn = false
                    am.isBluetoothScoOn = false
                    ivSpeaker?.setImageResource(R.drawable.ic_audio_speaker_no_bg)
                    mProximityController = null
                    setUpProximitySensor()
                } else if (isOnBluetooth) {
                    isOnSpeaker = true
                    isOnBluetooth = false
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    am.stopBluetoothSco()
                    am.isBluetoothScoOn = false
                    am.isSpeakerphoneOn = true
                    val drawable = R.drawable.bluetooth_without_bg
                    ivBluetooth?.setImageResource(drawable)
                    ivSpeaker?.setImageResource(R.drawable.ic_audio_speaker_disabled)
                    unregisterProximitySensor()
                } else {
                    isOnSpeaker = true
                    isOnBluetooth = false
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    am.stopBluetoothSco()
                    am.isBluetoothScoOn = false
                    am.isSpeakerphoneOn = true
                    val drawable = R.drawable.bluetooth_without_bg
                    ivBluetooth?.setImageResource(drawable)
                    ivSpeaker?.setImageResource(R.drawable.ic_audio_speaker_disabled)
                    unregisterProximitySensor()
                }
            }

            R.id.ivBluetooth -> {
                if (isOnBluetooth) {
                    isOnSpeaker = false
                    isOnBluetooth = false
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    am.stopBluetoothSco()
                    am.isBluetoothScoOn = false
                    am.isSpeakerphoneOn = !videoCallModel?.callType.equals("AUDIO", ignoreCase = true)
                    val drawable = R.drawable.bluetooth_without_bg
                    ivBluetooth?.setImageResource(drawable)
                    unregisterProximitySensor()
                } else if (isOnSpeaker) {
                    Toast.makeText(this@FuguCallActivity,"Connecting to Bluetooth",Toast.LENGTH_SHORT).show()
                    isOnSpeaker = false
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    setBluetooth(am)
                    ivSpeaker?.setImageResource(R.drawable.ic_audio_speaker_no_bg)
                    mProximityController = null
                    unregisterProximitySensor()
                } else {
                    Toast.makeText(this@FuguCallActivity,"Connecting to Bluetooth",Toast.LENGTH_SHORT).show()
                    isOnSpeaker = false
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    setBluetooth(am)
                    ivSpeaker?.setImageResource(R.drawable.ic_audio_speaker_no_bg)
                    mProximityController = null
                    unregisterProximitySensor()
                }
            }

            R.id.localSurfaceView -> {
                isLocalViewSmall = !isLocalViewSmall
                if (!isLocalViewSmall) {
                    localVideoTrack?.removeSink(localSurfaceView)
                    remoteVideoTrack?.removeSink(remoteSurfaceview)
                    localVideoTrack?.addSink(remoteSurfaceview)
                    remoteVideoTrack?.addSink(localSurfaceView)
                    chechScreenAction()
                } else {
                    localVideoTrack?.removeSink(remoteSurfaceview)
                    remoteVideoTrack?.removeSink(localSurfaceView)
                    localVideoTrack?.addSink(localSurfaceView)
                    remoteVideoTrack?.addSink(remoteSurfaceview)
                    updateScreenStatus()
                }


            }
            R.id.remoteSurfaceview -> {
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    if (isCallOptionsVisible) {
                        if (callDisconnectTimer != null) {
                            callDisconnectTimer?.cancel()
                        }
                        hidecallOptionsAnimation()
                    } else {
                        callDisconnectTimer = object : CountDownTimer(3000, 1000) {

                            override fun onTick(millisUntilFinished: Long) {}

                            override fun onFinish() {
                                if (isCallOptionsVisible) {
                                    hidecallOptionsAnimation()
                                    isCallOptionsVisible = !isCallOptionsVisible
                                }
                            }
                        }.start()
                        showCallOptionsAnimation()
                    }
                    isCallOptionsVisible = !isCallOptionsVisible
                }
            }
            R.id.mainRoot -> {
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    if (isCallOptionsVisible) {
                        if (callDisconnectTimer != null) {
                            callDisconnectTimer?.cancel()
                        }
                        hidecallOptionsAnimation()
                    } else {
                        callDisconnectTimer = object : CountDownTimer(3000, 1000) {

                            override fun onTick(millisUntilFinished: Long) {}

                            override fun onFinish() {
                                if (isCallOptionsVisible) {
                                    hidecallOptionsAnimation()
                                    isCallOptionsVisible = !isCallOptionsVisible
                                }
                            }
                        }.start()
                        showCallOptionsAnimation()
                    }
                    isCallOptionsVisible = !isCallOptionsVisible
                }
            }
            R.id.ivBack -> onBackPressed()
            else -> {

            }
        }
    }

    override fun onBackPressed() {
        if (!callStatus.equals(WebRTCCallConstants.CallStatus.IN_CALL.toString())) {
            callHungup()
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
            startPictureInPictureFeature()
            return
        } else {
            hasAlreadyInitializedViews = false
        }
        if(supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        unregisterrecievers()
        if (!isOnSpeaker) {
            setUpProximitySensor()
        }
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(mVideoCallReciever,
            IntentFilter(FuguAppConstant.VIDEO_CALL_INTENT))
        if (wiredHeadsetReceiver != null) {

            val filter1 = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
            val filter2 = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            val filter3 = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            val filter4 = IntentFilter(Intent.ACTION_HEADSET_PLUG)

            registerReceiver(wiredHeadsetReceiver, filter1)
            registerReceiver(wiredHeadsetReceiver, filter2)
            registerReceiver(wiredHeadsetReceiver, filter3)
            registerReceiver(wiredHeadsetReceiver, filter4)
        }
    }

    private fun unregisterrecievers() {
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoCallReciever)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(wiredHeadsetReceiver!!)
            //unregisterReceiver(wiredHeadsetReceiver!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        unregisterProximitySensor()
        try {
            mProximityController = null
            mWakeLoack?.release()
        } catch (e: Exception) {
        }
        try {
            mProximityController = null
            mPartialWakeLock?.release()
        } catch (e: Exception) {
        }
        try {
            mProximityController = null
            mProximityWakeLock?.release()
        } catch (e: Exception) {
        }
    }

    private val mVideoCallReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            videoCallService?.onBroadCastrecieved(intent)
        }
    }


    private fun hidecallOptionsAnimation() {
        val hungupGone = ObjectAnimator.ofFloat(ivHangUp, "translationY", 0f, 500f)
        hungupGone.setDuration(300)

        val muteAudioGone = ObjectAnimator.ofFloat(ivMuteAudio, "translationY", 0f, 250f)
        muteAudioGone.setDuration(180)

        val muteVideoGone = ObjectAnimator.ofFloat(ivMuteVideo, "translationY", 0f, 250f)
        muteVideoGone.setDuration(180)

        val switchCameraGone = ObjectAnimator.ofFloat(ivSwitchCamera, "translationY", 0f, 250f)
        switchCameraGone.setDuration(180)

        val bluetoothGone = ObjectAnimator.ofFloat(ivBluetooth, "translationY", 0f, 250f)
        bluetoothGone.setDuration(180)

        val localView = ObjectAnimator.ofFloat(localSurfaceView, "translationY", 0f, 250f)
        localView.setDuration(180)

        val llLocalView = ObjectAnimator.ofFloat(llLocalView, "translationY", 0f, 250f)
        localView.setDuration(180)

        hungupGone.start()
        muteAudioGone.start()
        muteVideoGone.start()
        switchCameraGone.start()
        bluetoothGone.start()
        localView.start()
        llLocalView.start()
    }

    private fun showCallOptionsAnimation() {
        val hungupVisible = ObjectAnimator.ofFloat(ivHangUp, "translationY", 500f, 0f)
        hungupVisible.setDuration(300)

        val muteAudioVisible = ObjectAnimator.ofFloat(ivMuteAudio, "translationY", 250f, 0f)
        muteAudioVisible.setDuration(180)

        val muteVideoVisible = ObjectAnimator.ofFloat(ivMuteVideo, "translationY", 250f, 0f)
        muteVideoVisible.setDuration(180)

        val switchCameraVisible = ObjectAnimator.ofFloat(ivSwitchCamera, "translationY", 250f, 0f)
        switchCameraVisible.setDuration(180)

        val bluetoothVisible = ObjectAnimator.ofFloat(ivBluetooth, "translationY", 250f, 0f)
        bluetoothVisible.setDuration(180)

        val localView = ObjectAnimator.ofFloat(localSurfaceView, "translationY", 250f, 0f)
        localView.setDuration(180)

        val llLocalView = ObjectAnimator.ofFloat(llLocalView, "translationY", 250f, 0f)
        llLocalView.setDuration(180)


        hungupVisible.start()
        muteAudioVisible.start()
        muteVideoVisible.start()
        switchCameraVisible.start()
        bluetoothVisible.start()
        localView.start()
        llLocalView.start()
    }

    private fun stopServiceCloseActivity(showFeedback: Boolean) {
        stopForegroundService(false)
        hangupVideoCall()

        Handler(Looper.getMainLooper()).postDelayed({
//            if (showFeedback) {
//                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//                am.mode = AudioManager.MODE_RINGTONE
//                if (am.isBluetoothScoOn) {
//                    am.startBluetoothSco()
//                    am.stopBluetoothSco()
//                }
//                am.abandonAudioFocus(mListener)
//                val intent = Intent(applicationContext, CallFeedbackActivity::class.java)
//                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
//                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//                startActivity(intent)
//                overridePendingTransition(0, 0)
//            }
            val mngr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val taskList = mngr.getRunningTasks(10)
            if (taskList[0].baseActivity?.className.toString().equals("com.hippocall.FuguCallActivity")) {
                finishAndRemoveTask()
                finishAffinity()
//                if (showFeedback) {
//                } else {
//                    System.exit(0)
//                }
                System.exit(0)
            } else {
                finish()
            }
        }, 300)
    }

    private fun stopServiceAndStartConf() {
        stopForegroundService(false)
        hangupVideoCall()
        Handler().postDelayed({
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_RINGTONE
            if (am.isBluetoothScoOn) {
                am.startBluetoothSco()
                am.stopBluetoothSco()
            }
            am.abandonAudioFocus(mListener)
            finish()
        }, 300)
    }


    private fun stopServiceAndCloseConnection() {

        stopForegroundService(true)
        hangupVideoCall()
        Handler().postDelayed({
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_RINGTONE
            if (am.isBluetoothScoOn) {
                am.startBluetoothSco()
                am.stopBluetoothSco()
            }
            am.abandonAudioFocus(mListener)
            val mngr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val taskList = mngr.getRunningTasks(10)
            if (taskList[0].baseActivity?.className.toString().equals("com.hippocall.FuguCallActivity")) {
                finishAndRemoveTask()
                finishAffinity()
                System.exit(0)
            } else {
                finish()
            }
        }, 500)
    }


    private fun switchCameraRecorder() {
        try {
            if (videoCallService?.getPeerconnection() != null) {
                ivSwitchCamera?.setOnClickListener(null)
                ivSwitchCamera?.alpha = 0.5f
                if (isLocalViewSmall) {
                    localVideoTrack?.removeSink(localSurfaceView)
                } else {
                    localVideoTrack?.removeSink(remoteSurfaceview)
                }
                videoCallService?.getPeerconnection()?.removeStream(videoStream)
                try {
                    videoCapturer?.stopCapture()

                    Handler().postDelayed({
                        ivSwitchCamera?.setOnClickListener(this)
                        ivSwitchCamera?.alpha = 1f

                        //ivMuteVideo?.setImageResource(R.drawable.ic_mute_video_no_bg)
                        isVideoEnabled = true
                        val isFlip = CommonData.getMirroeStatus(HippoCallConfig.FLIP_CAMERA)
                        val drawableVideo = if (isFlip) R.drawable.ic_mute_video_disabled else R.drawable.ic_mute_video_no_bg
                        ivMuteVideo?.setImageResource(drawableVideo)

                        val drawable = if (!videoCallService?.isAudioEnabled!!) R.drawable.ic_mute_microphone_disabled else R.drawable.ic_mute_microphone_no_bg
                        ivMuteAudio?.setImageResource(drawable)

                        val videoGrabberAndroid = createVideoGrabber(!isFrontFacingCamera)
                        val videoSource = peerConnectionFactory?.createVideoSource(false)
                        val cameraVideoCapturer = videoGrabberAndroid as CameraVideoCapturer
                        localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
                        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.getEglBaseContext())
                        cameraVideoCapturer.initialize(surfaceTextureHelper, this@FuguCallActivity, videoSource?.capturerObserver)
                        cameraVideoCapturer.startCapture(1000, 1000, 30)
                        sdpConstraints = MediaConstraints()
                        sdpConstraints?.mandatory?.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, "true"))
                        sdpConstraints?.mandatory?.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO, "true"))
                        val audioSource = peerConnectionFactory?.createAudioSource(sdpConstraints)
                        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)
                        videoStream = peerConnectionFactory?.createLocalMediaStream("102")
                        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                            videoStream?.addTrack(localVideoTrack)
                        }
                        videoStream?.addTrack(localAudioTrack)
                        videoCallService?.getPeerconnection()?.addStream(videoStream)
                        if (isLocalViewSmall) {
                            localVideoTrack?.addSink(localSurfaceView)
                        } else {
                            localVideoTrack?.addSink(remoteSurfaceview)
                        }
                        isFrontFacingCamera = !isFrontFacingCamera
//                        sendButtonStatus()
                        if(!videoCallService?.isAudioEnabled!!)
                            videoStream?.audioTracks?.get(0)?.setEnabled(videoCallService?.isAudioEnabled!!)
                        else
                            sendButtonStatus()
                    }, 1000)
                } catch (e: Exception) {

                }
            }
        } catch (e: Exception) {
        }
    }


    override fun onIceCandidateRecieved(jsonObject: JSONObject?) {
        videoStream = peerConnectionFactory?.createLocalMediaStream("102")
        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
            videoStream?.addTrack(localVideoTrack)
        }
        videoStream?.addTrack(localAudioTrack)
        videoCallService?.getPeerconnection()?.addStream(videoStream)
        videoCallService?.saveIceCandidate(jsonObject)
    }

    override fun onVideoOfferRecieved(jsonObject: JSONObject?) {
        if (videoStream == null) {
            videoStream = peerConnectionFactory?.createLocalMediaStream("102")
            videoStream?.addTrack(localAudioTrack)
            if (videoCallModel?.callType.equals("VIDEO")) {
                videoStream?.addTrack(localVideoTrack)
            }

        }
        videoCallService?.getPeerconnection()?.addStream(videoStream)
        videoOfferjson = jsonObject
        runOnUiThread {
            Handler().postDelayed({
                if (wirelessContext != null && wirelessIntent != null) {
                    WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                }
            }, 500)
            Handler().postDelayed({
                if (wirelessContext != null && wirelessIntent != null) {
                    WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                }
            }, 1500)
            onFayeConnected()
        }
    }

    override fun onVideoOfferScreenSharingRecieved(jsonObject: JSONObject?) {
        if (!isLocalViewSmall) {
            localVideoTrack?.removeSink(remoteSurfaceview)
            remoteVideoTrack?.removeSink(localSurfaceView)
            localVideoTrack?.addSink(localSurfaceView)
            remoteVideoTrack?.addSink(remoteSurfaceview)
            isLocalViewSmall = !isLocalViewSmall
        }

        videoCallService?.saveOfferAndAnswer(jsonObject)
        Handler().postDelayed({
            if (wirelessContext != null && wirelessIntent != null) {
                WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
            }
        }, 500)
        Handler().postDelayed({
            if (wirelessContext != null && wirelessIntent != null) {
                WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
            }
        }, 1500)
    }

    override fun onVideoAnswerRecieved(jsonObject: JSONObject?) {
        runOnUiThread {
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    ivBack?.visibility = View.VISIBLE
                    tvCallTimer?.visibility = View.GONE
                    tvCalledPersonName?.visibility = View.GONE
                    ivCalledPersonImage?.visibility = View.GONE
                }
            }
        }
    }

    override fun onReadyToConnectRecieved(jsonObject: JSONObject?) {
        videoStream = peerConnectionFactory?.createLocalMediaStream("102")
        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
            videoStream?.addTrack(localVideoTrack)
        }
        videoStream?.addTrack(localAudioTrack)
        videoStream?.audioTracks!![0].setEnabled(true)
        videoCallService?.getPeerconnection()?.addStream(videoStream)
        videoCallService?.createOffer(connection)
        runOnUiThread {
            tvCallTimer?.text = RINGING
        }
    }

    override fun onCallHungUp(jsonObject: JSONObject?, showFeedback: Boolean) {
        stopServiceCloseActivity(showFeedback)
    }


    override fun onCallRejected(jsonObject: JSONObject?) {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            var m_amAudioManager: AudioManager? = null
            m_amAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            m_amAudioManager.setMode(AudioManager.STREAM_MUSIC)
            m_amAudioManager?.isSpeakerphoneOn = videoCallModel?.callType.equals("VIDEO", ignoreCase = true)
            var aa = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                mediaPlayer = MediaPlayer.create(this, R.raw.busy_tone)
            } else {
                mediaPlayer = MediaPlayer.create(this, R.raw.busy_tone, aa, 1)
            }
            mediaPlayer?.setLooping(false)
            mediaPlayer?.start()
        }


        tvCallTimer?.text = REJECTED

        runOnUiThread {
            Handler().postDelayed({
                stopServiceCloseActivity(false)
            }, 3000)
        }
    }

    override fun onUserBusyRecieved(jsonObject: JSONObject?) {
        videoCallService?.cancelCallDisconnectTimer()
        runOnUiThread {
//            tvCallingStatus?.visibility = View.VISIBLE
//            tvCallingStatus?.text = USER_BUSY
            tvCallTimer?.text = USER_BUSY
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer = MediaPlayer.create(this@FuguCallActivity, R.raw.busy_tone)
                mediaPlayer?.setLooping(false)
                mediaPlayer?.start()
            }
            Handler().postDelayed({
                stopServiceCloseActivity(false)
            }, 3000)
        }
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        remoteVideoStream = mediaStream
        videoCallService?.setRemoteStream(mediaStream)
        if (mediaStream?.videoTracks != null && mediaStream.videoTracks?.size!! > 0) {
            remoteVideoTrack = mediaStream.videoTracks?.get(0)
        }
        runOnUiThread {
            try {
                callStatus = WebRTCCallConstants.CallStatus.IN_CALL.toString()
                CommonData.setCallStatus(WebRTCCallConstants.CallStatus.IN_CALL.toString())
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    setBluetooth(am)

                } else {
                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    am.isSpeakerphoneOn = false
                    setBluetooth(am)
                }

                if (remoteVideoTrack != null) {
                    remoteVideoTrack?.addSink(remoteSurfaceview)
                }
                outgoingCallLayout?.visibility = View.VISIBLE
                incomingCallLayout?.visibility = View.GONE

                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    remoteSurfaceview?.visibility = View.VISIBLE
                    localSurfaceView?.visibility = View.VISIBLE
                }
                ivBack?.visibility = View.VISIBLE
//                incomingRippleView?.visibility = View.GONE
                tvIncomingPersonName?.visibility = View.GONE
                ivIncomingPersonImage?.visibility = View.GONE
                ivIncomingPersonImageBig?.visibility = View.GONE
                ivRejectCall?.visibility = View.GONE
                ivAnswerCall?.visibility = View.GONE
//                tvCallingStatus?.visibility = View.GONE
                ivHangUp?.visibility = View.VISIBLE
                ivMuteAudio?.visibility = View.VISIBLE
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    ivMuteVideo?.visibility = View.VISIBLE
                    ivSwitchCamera?.visibility = View.VISIBLE
                    ivSpeaker?.visibility = View.GONE
                } else {
                    ivMuteVideo?.visibility = View.GONE
                    ivSwitchCamera?.visibility = View.GONE
                    ivSpeaker?.visibility = View.VISIBLE
                }
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    tvCallType?.visibility = View.VISIBLE
                    tvCallType?.text = "Video Call"
                    ivMuteVideo?.visibility = View.VISIBLE
                    ivSwitchCamera?.visibility = View.VISIBLE
                    ivSpeaker?.visibility = View.GONE
                    ivCalledPersonImage?.visibility = View.GONE
                    tvCalledPersonName?.visibility = View.GONE
                    tvCallType?.visibility = View.GONE
//                    ivFugu?.visibility = View.GONE
                } else {
                    tvCallType?.visibility = View.VISIBLE
                    tvCallType?.text = "Voice Call"
                    ivMuteVideo?.visibility = View.GONE
                    ivSwitchCamera?.visibility = View.GONE
                    ivSpeaker?.visibility = View.VISIBLE

                    val options = RequestOptions()
                        .centerCrop()
                        .dontAnimate()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_hippo_user_image)
                        .error(R.drawable.ic_hippo_user_image)
                        .fitCenter()
                        .priority(Priority.HIGH)
                        .transforms(CenterCrop(), RoundedCorners(1000))
                    Glide.with(this)
                        .asBitmap()
                        .apply(options)
                        .load(videoCallModel?.userThumbnailImage)
                        .into(ivCalledPersonImage!!)
                }

//                llVideoCall?.visibility = View.GONE
                isCallAnswered = true
                if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                    startForeGroundService(ONGOING_VIDEO_CALL)
                } else {
                    startForeGroundService(ONGOING_AUDIO_CALL)
                }
                EglRenderer.FrameListener { bitmap ->
                    HippoLog.e("Resolution", bitmap.width.toString())
                }
                videoCallService?.isCallConnected = true
                localSurfaceView?.setZOrderOnTop(true)
                remoteSurfaceview?.setZOrderMediaOverlay(true)
                HippoLog.e("Resolution", rootEglBase?.surfaceHeight().toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {

    }

    override fun onErrorRecieved(error: String?) {

    }


    private fun initiateOutgoingRinging() {
        Handler().postDelayed({
            val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.setMode(AudioManager.STREAM_MUSIC)
            if (audio.isBluetoothA2dpOn) {
                audio.startBluetoothSco()
                audio.isSpeakerphoneOn = false
                audio.isBluetoothScoOn = true
                audio.requestAudioFocus(mListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            } else {
                audio.stopBluetoothSco()
                audio.isBluetoothScoOn = false
                audio.isSpeakerphoneOn = videoCallModel?.callType.equals("VIDEO", ignoreCase = true)
            }

            var aa = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                mediaPlayer = MediaPlayer.create(this, R.raw.ringing)
            } else {
                mediaPlayer = MediaPlayer.create(this, R.raw.ringing, aa, 1)
            }
            mediaPlayer?.setLooping(true)
            mediaPlayer?.start()
        }, 100)
    }

    private fun initiateIncomingRinging() {
        Handler().postDelayed({
            try {
                val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

                if (audio.isBluetoothA2dpOn) {
                    audio.isSpeakerphoneOn = true
                    audio.isBluetoothScoOn = false
                    audio.isWiredHeadsetOn = false
                    audio.requestAudioFocus(mListener, AudioManager.MODE_IN_CALL,
                        AudioManager.AUDIOFOCUS_GAIN);
                } else {
                    audio.stopBluetoothSco()
                    audio.isBluetoothScoOn = false
                    audio.isSpeakerphoneOn = videoCallModel?.callType.equals("VIDEO", ignoreCase = true)
                }

                when (audio.ringerMode) {

                    AudioManager.RINGER_MODE_NORMAL -> {
                        mediaPlayer = MediaPlayer.create(this@FuguCallActivity,
                            Settings.System.DEFAULT_RINGTONE_URI)
                        mediaPlayer?.isLooping = true
                        mediaPlayer?.start()
                    }
                    AudioManager.RINGER_MODE_SILENT -> {

                    }
                    AudioManager.RINGER_MODE_VIBRATE -> {
                        vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                        val pattern = longArrayOf(0, 1000, 1000)
                        vibrate?.vibrate(pattern, 0)
                    }
                }
            } catch (e: Exception) {
                mediaPlayer = MediaPlayer.create(this@FuguCallActivity,
                    R.raw.video_call_ringtone)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
            }
        }, 100)
    }


    private fun setUpWebRTCViews() {
        hasAlreadyInitializedViews = true
        remoteSurfaceview = findViewById(R.id.remoteSurfaceview)
        localSurfaceView = findViewById(R.id.localSurfaceView)
        rootEglBase = EglBase.create()
        videoCallService?.setEgl(rootEglBase)
        localSurfaceView?.setMirror(CommonData.getMirroeStatus(HippoCallConfig.LOCAL_SURFACE))
        remoteSurfaceview?.setMirror(CommonData.getMirroeStatus(HippoCallConfig.REMOTE_SURFACE))
        localSurfaceView?.init(rootEglBase?.getEglBaseContext(), null)
        localSurfaceView?.setZOrderOnTop(true)
        remoteSurfaceview?.setZOrderMediaOverlay(true)
        remoteSurfaceview?.init(rootEglBase?.getEglBaseContext(), null)
        peerConnectionFactory = videoCallService?.createPeerConnectionFactory(rootEglBase)

        videoCallService?.setVideoModel(videoCallModel)
        videoCallService?.createWebRTCCallConnection()
        sdpConstraints = MediaConstraints()
        sdpConstraints?.mandatory?.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_AUDIO, "true"))
        sdpConstraints?.mandatory?.add(MediaConstraints.KeyValuePair(OFFER_TO_RECEIVE_VIDEO, "true"))
        connection = Connection(videoCallModel?.stunServers, videoCallModel?.turnServers,
            sdpConstraints, videoCallModel?.turnUserName, videoCallModel?.turnCredential, peerConnectionFactory)
        connection?.sdpConstraints = sdpConstraints
        videoCallService?.setConnectionModel(connection)
        videoCallService?.createPeerConnection(connection)

        val videoGrabberAndroid = createVideoGrabber(isFrontFacingCamera)
        val constraints = MediaConstraints()
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        val cameraVideoCapturer = videoGrabberAndroid as CameraVideoCapturer
        localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
        val audioSource = peerConnectionFactory?.createAudioSource(constraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)
        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
//            val videoGrabberAndroid = createVideoGrabber(isFrontFacingCamera)
//            val cameraVideoCapturer = videoGrabberAndroid as CameraVideoCapturer
            val surfaceTextureHelper = SurfaceTextureHelper
                .create("CaptureThread", rootEglBase?.getEglBaseContext())
            cameraVideoCapturer.initialize(surfaceTextureHelper,
                this, videoSource?.capturerObserver)
            cameraVideoCapturer.startCapture(1000, 1000, 30)
            videoStream = peerConnectionFactory?.createLocalMediaStream("102")
            videoCallService?.setLocalVideoStream(videoStream)
            videoStream?.addTrack(localVideoTrack)
        }
        videoStream?.addTrack(localAudioTrack)
        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
            localVideoTrack?.addSink(localSurfaceView)
        }
    }

    private fun setUpWebRTCViewsKilled() {
        rootEglBase = videoCallService?.getEgl()
        localSurfaceView?.setMirror(CommonData.getMirroeStatus(HippoCallConfig.LOCAL_SURFACE))
        localSurfaceView?.init(rootEglBase?.getEglBaseContext(), null)
        localSurfaceView?.setZOrderOnTop(true)
        peerConnectionFactory = videoCallService?.createPeerConnectionFactory(rootEglBase)
        videoStream = videoCallService?.getLocalVideoStream()
        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
            localSurfaceView?.visibility = View.VISIBLE
            localVideoTrack = videoStream?.videoTracks?.get(0)
            localVideoTrack?.addSink(localSurfaceView)
        }
    }

    private fun createVideoGrabber(isFrontFacing: Boolean): VideoCapturer? {
        videoCapturer = createCameraGrabber(Camera1Enumerator(false), isFrontFacing)
        return videoCapturer
    }

    fun createCameraGrabber(enumerator: CameraEnumerator, isFrontFacing: Boolean): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        if (isFrontFacing) {
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }
            for (deviceName in deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }
        } else {
            for (deviceName in deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }
        }
        return null
    }


    fun askForPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val MY_PERMISSIONS_REQUEST = 102
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST)
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST_RECORD_AUDIO)

        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            val MY_PERMISSIONS_REQUEST_CAMERA = 100
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSIONS_REQUEST_CAMERA)
        } else {
            initCall()
        }
    }

    private fun answerCall() {
        HippoLog.e("Faye Connection", videoCallService?.isFayeConnected().toString())
        vibrate?.cancel()
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
        }

        try {
            if (videoCallService?.isFayeConnected()!!) {
                videoCallService?.saveOfferAndAnswer(videoOfferjson)
                ivBack?.visibility = View.VISIBLE

                Handler().postDelayed({
                    if (wirelessContext != null && wirelessIntent != null) {
                        WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                    }
                }, 500)
                Handler().postDelayed({
                    if (wirelessContext != null && wirelessIntent != null) {
                        WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                    }
                }, 1500)

            } else {
                rlCallingButtons?.visibility = View.INVISIBLE
                tvConnecting?.visibility = View.VISIBLE
                isAnswerClicked = true
            }

            if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                tvCallTimer?.visibility = View.GONE
            }

        } catch (e: java.lang.Exception) {
            videoCallService?.saveOfferAndAnswer(videoOfferjson)
            ivBack?.visibility = View.VISIBLE
            Handler().postDelayed({
                if (wirelessContext != null && wirelessIntent != null) {
                    WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                }
            }, 500)
            Handler().postDelayed({
                if (wirelessContext != null && wirelessIntent != null) {
                    WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                }
            }, 1500)
        }
        if(!videoCallService?.isCallConnected!!) {
            onCustomActionClicked("call_connected")
        }
        //videoCallService?.isCallConnected = true
    }

    private fun startForeGroundService(status: String) {
        val startIntent = Intent(this@FuguCallActivity, VideoCallService::class.java)
        var channelName = ""
        if (!intent.hasExtra("videoCallModel")) {
            if (intent.hasExtra(CHANNEL_NAME)) {
                channelName = intent.getStringExtra(CHANNEL_NAME).toString()
            } else {
                channelName = "Hippo Call"
            }
        } else {
            videoCallModel = intent.extras?.getParcelable("videoCallModel")
            channelName = videoCallModel?.channelName!!
        }
        startIntent.action = "com.fuguchat.start"
        startIntent.putExtra(CALL_STATUS, status)
        startIntent.putExtra(CHANNEL_NAME, channelName)
        try {
            ContextCompat.startForegroundService(this, startIntent)
            bindService(startIntent, mConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    public fun stopForegroundService(isHungUpToBeSent: Boolean?) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_RINGTONE
        if (am.isBluetoothScoOn) {
            am.startBluetoothSco()
            am.stopBluetoothSco()
        }
        am.abandonAudioFocus(mListener)

//        videoCallService?.stopForeground(true)
//        videoCallService?.stopSelf()

        val startIntent = Intent(this@FuguCallActivity, VideoCallService::class.java)
        startIntent.action = "com.fuguchat.stop"
        stopService(startIntent)

        stopFayeClient()
    }

    // todo check why this
    fun stopFayeClient() {
        /*try {
            val thread = HandlerThread("TerminateThread")
            thread.start()
            Handler(thread.looper).post {
                try {
                    HippoConfig.getExistingClient {
                        if (it?.isConnectedServer()!!) {
                            it.disconnectServer()
                            it.setListener(null)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }

    public fun hangupVideoCall() {
        runOnUiThread {
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
            } else {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.stop()
            }
            if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                localVideoTrack?.removeSink(localSurfaceView)
            }
            videoCallService?.closePeerConnection()

            try {

                videoCapturer?.stopCapture()
                videoCapturer?.dispose()

            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            if (!isAlreadyHungUp) {
                isAlreadyHungUp = true
                var audio: AudioManager? = null
                Handler().postDelayed({
                    audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audio?.mode = AudioManager.STREAM_MUSIC
                    audio?.stopBluetoothSco()
                    audio?.isSpeakerphoneOn = videoCallModel?.callType.equals("VIDEO", ignoreCase = true)
                    val aa = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                        mediaPlayer = MediaPlayer.create(this, R.raw.disconnet_call)
                    } else {
                        audio?.mode = AudioManager.MODE_IN_CALL
                        mediaPlayer = MediaPlayer.create(this, R.raw.disconnet_call, aa, 1)
                    }

                    mediaPlayer?.isLooping = false
                    mediaPlayer?.start()
                }, 300)
            }
        }
    }


    fun stopVideoAudio() {
        runOnUiThread {
            stopAudioVideo = true
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
            } else {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.stop()
            }
            videoCallService?.closePeerConnection()
            try {
                videoCapturer?.stopCapture()
                videoCapturer?.dispose()

            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            try {

            } catch (e: Exception) {

            }
        }


    }

    /*override fun moveTaskToBack(nonRoot: Boolean): Boolean {
        return super.moveTaskToBack(nonRoot)
    }

    override fun finish() {
        super.finish()
        //Toast.makeText(this, "in finish", Toast.LENGTH_LONG).show()
    }*/

    override fun onStop() {
        super.onStop()
        if (mBounded) {
            unbindService(mConnection)
            mBounded = false
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterrecievers()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (callStatus.equals(WebRTCCallConstants.CallStatus.IN_CALL.toString())) {
            sendButtonStatus(true)
        }
        videoCapturer?.stopCapture()
        unregisterrecievers()
        try {
            vibrate?.cancel()

        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (mBounded) {
            unbindService(mConnection)
            mBounded = false
        }
        if (isLocalViewSmall) {
            remoteVideoTrack?.removeSink(remoteSurfaceview)
            localVideoTrack?.removeSink(localSurfaceView)
        } else {
            remoteVideoTrack?.removeSink(localSurfaceView)
            localVideoTrack?.removeSink(remoteSurfaceview)
        }

        localSurfaceView?.release()
        localSurfaceView?.pauseVideo()
        remoteSurfaceview?.release()
        remoteSurfaceview?.pauseVideo()

        videoCallService?.setRemoteStream(remoteVideoStream)
        if (!callStatus.equals(WebRTCCallConstants.CallStatus.IN_CALL.toString())) {
            videoCallService?.cancelStartCallTimer()
            videoCallService?.cancelCalltimer()
            videoCallService?.cancelCallDisconnectTimer()

            stopServiceAndCloseConnection()
        }
        HippoCallConfig.getInstance().setCallBackListener()

    }

    var mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            mBounded = false
            videoCallService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {

            runOnUiThread {
                mBounded = true
                val mLocalBinder = service as VideoCallService.LocalBinder
                videoCallService = mLocalBinder.serverInstance

                if (!intent.hasExtra("videoCallModel")) {
                    videoCallService?.setActivityContext(this@FuguCallActivity)
                    setUpWebRTCViewsKilled()
                    remoteSurfaceview?.setMirror(CommonData.getMirroeStatus(HippoCallConfig.REMOTE_SURFACE))
                    remoteSurfaceview?.setZOrderMediaOverlay(true)
                    remoteSurfaceview?.init(rootEglBase?.getEglBaseContext(), null)
                    connection = videoCallService?.getConnectionModel()
                    signal = videoCallService?.getSignal()
                    videoCallModel = videoCallService?.getVideoModel()
                    if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                        remoteVideoStream = videoCallService?.getRemoteVideoStream()
                        remoteVideoTrack = remoteVideoStream?.videoTracks?.get(0)
                        remoteSurfaceview?.visibility = View.VISIBLE
                        remoteVideoTrack?.addSink(remoteSurfaceview)
                    }
                    updateScreenStatus()
                    sendButtonStatus()
                } else {
                    videoCallService?.setActivityContext(this@FuguCallActivity)
                    if (!hasAlreadyInitializedViews) {
                        setUpWebRTCViews()
                        fetchCommonData()
                    }
                }
            }
        }
    }

    private fun fetchCommonData() {
        videoCallModel = intent.extras?.getParcelable("videoCallModel")

        if(intent.hasExtra("video_offer")) {
            var offers: String = intent.getStringExtra("video_offer").toString()
            fetchIntentData(offers)
        } else if(intent.hasExtra(FuguAppConstant.USER_AGENT_CALL)) {
            var fuguCreateConversationParams = Gson().fromJson(intent
                .getStringExtra(FuguAppConstant.PEER_CHAT_PARAMS), FuguCreateConversationParams::class.java)
            if(CommonData.getChannelId(fuguCreateConversationParams.transactionId) != null) {
                videoCallModel?.channelId = CommonData.getChannelId(fuguCreateConversationParams.transactionId);
                fetchIntentData("")
            } else {
                val text = Restring.getString(this@FuguCallActivity, R.string.hippo_call_connecting)
                tvCallTimer?.text = text
                createConversation(fuguCreateConversationParams)
            }
        } else if(intent.hasExtra(FuguAppConstant.PEER_CHAT_PARAMS)) {
            var fuguCreateConversationParams = Gson().fromJson(intent
                .getStringExtra(FuguAppConstant.PEER_CHAT_PARAMS), FuguCreateConversationParams::class.java)
            if(CommonData.getChannelId(fuguCreateConversationParams.transactionId) != null) {
                videoCallModel?.channelId = CommonData.getChannelId(fuguCreateConversationParams.transactionId);
                fetchIntentData("")
            } else {
                val text = Restring.getString(this@FuguCallActivity, R.string.hippo_call_connecting)
                tvCallTimer?.text = text
                createConversation(fuguCreateConversationParams)
            }
        } else {
            fetchIntentData("")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            if (intent.action == Intent.ACTION_DELETE) {
                isHungup = true
                stopAudioVideo = true
                tvCallTimer?.text = DISCONNECTING
                videoCallService?.isReadyForConnection = true
                videoCallService?.hungUpCall()
                if (ivHungupCallTimer == null) {
                    ivHungupCallTimer = object : CountDownTimer(2000, 1000) {
                        override fun onFinish() {
                            onHungupSent()
                        }

                        override fun onTick(millisUntilFinished: Long) {
                        }

                    }.start()

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class WiredHeadsetReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            wirelessContext = context
            wirelessIntent = intent

            val state = intent.getIntExtra("state", STATE_UNPLUGGED)
//            val microphone = intent.getIntExtra("microphone", HAS_NO_MIC)
//            val name = intent.getStringExtra("name")

            isWirelessHeadSetConnected = state == STATE_PLUGGED

            try {
                if (true || videoCallService?.isCallConnected!!) {
                    if (isWirelessHeadSetConnected) {
                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.mode = AudioManager.STREAM_MUSIC
                        am.isWiredHeadsetOn = true
                        am.isSpeakerphoneOn = false
                        setBluetooth(am)

                    } else {
                        if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            am.mode = AudioManager.STREAM_MUSIC
                            am.isSpeakerphoneOn = true
                            setBluetooth(am)
                        } else {
                            if (isOnSpeaker) {
                                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                am.mode = AudioManager.STREAM_MUSIC
                                setBluetooth(am)
                            } else {
                                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                am.mode = AudioManager.STREAM_MUSIC
                                am.isSpeakerphoneOn = false
                                setBluetooth(am)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun updateCallTimer(calltimer: String) {
        runOnUiThread {
            if (videoCallModel?.callType.equals("AUDIO", ignoreCase = true) && !stopAudioVideo) {
                tvCallTimer!!.visibility = View.VISIBLE
                tvCallTimer!!.text = calltimer
            }
        }
    }

    fun setUpProximitySensor() {
        if (mProximityController == null) {
            mProximityController = ProximitySensorController(applicationContext,
                object : ProximitySensorController.onProximitySensorCallback {
                    override fun onError(errorCode: Int, message: String?) {
                        HippoLog.e("ProximitySensorControllerCallBacks", String.format("onError() : %2s", message));
                    }

                    override fun onSensorRegister() {
                        HippoLog.i("ProximitySensorControllerCallBacks", String.format("onSensorUnregister"));
                    }

                    override fun onSensorUnregister() {
                        HippoLog.i("ProximitySensorControllerCallBacks", String.format("onSensorRegister"));
                    }

                    override fun onPlay() {
                        HippoLog.i("ProximitySensorControllerCallBacks", String.format("near"));
                        try {
                            if (!mWakeLoack!!.isHeld() && isCallAnswered && videoCallModel?.callType.equals("AUDIO", ignoreCase = true)) {
                                try {
                                    mWakeLoack?.acquire();
                                } catch (e: java.lang.Exception) {
                                }
                                try {
                                    mPartialWakeLock?.acquire();
                                } catch (e: java.lang.Exception) {
                                }
                                try {
                                    mProximityWakeLock?.acquire();
                                } catch (e: java.lang.Exception) {

                                }
                            }
                        } catch (e: Exception) {
                        }
                    }

                    override fun onPause() {
                        HippoLog.i("ProximitySensorControllerCallBacks", String.format("far"));
                    }
                })
        }
        registerProximitySensor();
    }


    /**
     * register the proximity sensor using proximity controller
     */
    private fun registerProximitySensor() {
        mProximityController?.registerListener()
    }

    /**
     * Unregister the proximity sensor using proximity controller
     */
    private fun unregisterProximitySensor() {
        mProximityController?.unregisterListener()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            if (requestCode == 100 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                initCall()
            } else if (requestCode == 101 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                initCall()
            } else if (requestCode == 102 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED &&
                grantResults[1] == PermissionChecker.PERMISSION_GRANTED) {
                initCall()
            } else if (requestCode == 100 && grantResults[0] == PermissionChecker.PERMISSION_DENIED) {
                showCouldntPlaceError()
            } else if (requestCode == 101 && grantResults[0] == PermissionChecker.PERMISSION_DENIED) {
                showCouldntPlaceError()
            } else if (requestCode == 102 && grantResults[0] == PermissionChecker.PERMISSION_DENIED ||
                grantResults[1] == PermissionChecker.PERMISSION_DENIED) {
                showCouldntPlaceError()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun showCouldntPlaceError() {
    }

    private fun initCall() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel("VideoCall",
                "VideoCall", NotificationManager.IMPORTANCE_LOW)
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        initializeViews()
        initializeClickListeners()

        try {
            field = PowerManager::class.java.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null)
        } catch (ignored: Throwable) {
        }
        mPowerManager = getSystemService(POWER_SERVICE) as PowerManager
        mWakeLoack = mPowerManager?.newWakeLock(PowerManager.FULL_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP, getLocalClassName())
        mWakeLoack?.acquire()
        mPartialWakeLock = mPowerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                or PowerManager.ON_AFTER_RELEASE, getLocalClassName())
        mPartialWakeLock?.acquire()
        if (PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK != 0x0) {
            mProximityWakeLock = mPowerManager?.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, getLocalClassName());
        }
        mPartialWakeLock?.acquire()


        if (intent.action == Intent.ACTION_DELETE) {
            isHungup = true
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_RINGTONE
            if (am.isBluetoothScoOn) {
                am.startBluetoothSco()
                am.stopBluetoothSco()
            }
            am.abandonAudioFocus(mListener)
            Handler().postDelayed({
                stopForegroundService(false)
            }, 500)
            val hungUpIntent = Intent(VIDEO_CALL_HUNGUP_FROM_NOTIFICATION)
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(hungUpIntent)
            val mngr = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val taskList = mngr.getRunningTasks(10)
            Handler().postDelayed({
                if (taskList[0].baseActivity?.className.toString().equals("com.hippocall.FuguCallActivity")) {
                    finishAffinity()
                    System.exit(0)
                } else {
                    finish()
                }
            }, 600)
        }

        if (!intent.hasExtra("videoCallModel")) {

            ivBack?.visibility = View.VISIBLE
            tvCallTimer?.visibility = View.GONE
            tvCalledPersonName?.visibility = View.GONE
            tvIncomingPersonName?.visibility = View.GONE
            videoCallModel = CommonData.getVideoCallModel()
            callStatus = CommonData.getCallStatus()
            when (callStatus) {
                WebRTCCallConstants.CallStatus.IN_CALL.toString() -> {
                    isCallAnswered = true
                    incomingCallLayout?.visibility = View.GONE
                    if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                        startForeGroundService(ONGOING_VIDEO_CALL)
                    } else {
                        startForeGroundService(ONGOING_AUDIO_CALL)
                    }
                    ivRejectCall?.visibility = View.GONE
                    ivAnswerCall?.visibility = View.GONE
                    ivHangUp?.visibility = View.VISIBLE
                    ivMuteAudio?.visibility = View.VISIBLE
                    if (videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {
                        tvCallType?.text = "Video Call"
                        ivMuteVideo?.visibility = View.VISIBLE
                        ivSwitchCamera?.visibility = View.VISIBLE
                        ivSpeaker?.visibility = View.GONE
                        ivIncomingPersonImage?.visibility = View.GONE
                        ivIncomingPersonImageBig?.visibility = View.GONE
                        tvIncomingPersonName?.visibility = View.GONE
                        ivCalledPersonImage?.visibility = View.GONE
                        tvIncomingPersonName?.visibility = View.GONE
                        tvCallType?.visibility = View.GONE
                        tvCallTypeIncoming?.visibility = View.GONE
//                        tvCallingStatus?.visibility = View.GONE

                    } else {
                        tvCallType?.text = "Voice Call"
                        ivMuteVideo?.visibility = View.GONE
                        ivSwitchCamera?.visibility = View.GONE
                        ivSpeaker?.visibility = View.VISIBLE
                        ivIncomingPersonImage?.visibility = View.VISIBLE
                        ivIncomingPersonImageBig?.visibility = View.VISIBLE
                        tvIncomingPersonName?.visibility = View.VISIBLE
                        tvCalledPersonName?.visibility = View.VISIBLE
                    }
                    if (videoCallModel?.callType.equals("AUDIO", ignoreCase = true)) {
//                        tvCallingStatus?.visibility = View.GONE
                        tvIncomingPersonName?.text = videoCallModel?.channelName
                        tvCalledPersonName?.text = videoCallModel?.channelName

                        val options = RequestOptions()
                            .centerCrop()
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_hippo_user_image)
                            .error(R.drawable.ic_hippo_user_image)
                            .fitCenter()
                            .priority(Priority.HIGH)
                            .transforms(CenterCrop(), RoundedCorners(1000))
                        Glide.with(this)
                            .asBitmap()
                            .apply(options)
                            .load(videoCallModel?.userThumbnailImage)
                            .into(ivCalledPersonImage!!)
                    } else {

                    }
                    videoCallService?.createWebRTCSignallingConnection(videoCallModel, signal)
                    Handler().postDelayed({

                        isFrontFacingCamera = !isFrontFacingCamera
                        switchCameraRecorder()
                    }, 1000)

                    val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.mode = AudioManager.STREAM_MUSIC
                    setBluetooth(am)
                }

                WebRTCCallConstants.CallStatus.INCOMING_CALL.toString() -> {

                }
                WebRTCCallConstants.CallStatus.OUTGOING_CALL.toString() -> {
                }


            }
        } else {
            startForeGroundService(RINGING)
        }

    }


    override fun onFayeConnected() {
        if (isAnswerClicked) {
            HippoLog.e("Faye Connection", videoCallService?.isFayeConnected().toString())
            vibrate?.cancel()
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
            }
            try {
                if (videoCallService?.isFayeConnected()!!) {
                    videoCallService?.saveOfferAndAnswer(videoOfferjson)
                    ivBack?.visibility = View.VISIBLE
                    tvConnecting?.visibility = View.GONE
                } else {
                    ivAnswerCall?.visibility = View.INVISIBLE
                    ivRejectCall?.visibility = View.INVISIBLE
                    isAnswerClicked = true
                }
            } catch (e: java.lang.Exception) {
                videoCallService?.saveOfferAndAnswer(videoOfferjson)
                ivBack?.visibility = View.VISIBLE
                tvConnecting?.visibility = View.GONE
            }
            if (isWirelessHeadSetConnected) {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.mode = AudioManager.STREAM_MUSIC
                am.isSpeakerphoneOn = false
                am.isWiredHeadsetOn = true
                setBluetooth(am)
            }
            Handler().postDelayed({
                if (wirelessContext != null && wirelessIntent != null) {
                    WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                }
            }, 500)
            Handler().postDelayed({
                if (wirelessContext != null && wirelessIntent != null) {
                    WiredHeadsetReceiver().onReceive(wirelessContext!!, wirelessIntent!!)
                }
            }, 1500)

        }
    }

    private fun setBluetooth(am: AudioManager) {
        Handler().postDelayed({
            try {
                if (am.isBluetoothA2dpOn) {
                    am.startBluetoothSco()
                    am.isBluetoothScoOn = true
                    am.isSpeakerphoneOn = false
                    val drawable = R.drawable.bluetooth_disabled
                    ivBluetooth?.setImageResource(drawable)
                    ivBluetooth?.visibility = View.VISIBLE
                    am.requestAudioFocus(mListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
                    isOnBluetooth = true
                } else {
                    am.stopBluetoothSco()
                    am.isBluetoothScoOn = false
                    if (videoCallModel?.callType.equals("AUDIO", ignoreCase = true)) {
                        am.isSpeakerphoneOn = isOnSpeaker
                    } else if(videoCallModel?.callType.equals("VIDEO", ignoreCase = true)
                        && am.isWiredHeadsetOn) {
                        am.isSpeakerphoneOn = false
                    } else {
                        am.isSpeakerphoneOn = true
                    }
                    ivBluetooth?.visibility = View.GONE
                    isOnBluetooth = true
                }
            } catch (e: Exception) {
            }

        }, 1000)

    }

    private fun setBluetoothAgain(am: AudioManager) {
        try {
            if (am.isBluetoothA2dpOn) {
                am.startBluetoothSco()
                am.isBluetoothScoOn = true
                val drawable = R.drawable.bluetooth_disabled
                ivBluetooth?.setImageResource(drawable)
                ivBluetooth?.visibility = View.VISIBLE
                am.requestAudioFocus(mListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
                isOnBluetooth = true
            } else {
                am.stopBluetoothSco()
                am.isBluetoothScoOn = false
                if (videoCallModel?.callType.equals("AUDIO", ignoreCase = true)) {
                    am.isSpeakerphoneOn = isOnSpeaker
                } else {
                    am.isSpeakerphoneOn = true
                }
                ivBluetooth?.visibility = View.GONE
                isOnBluetooth = true
            }
        } catch (e: Exception) {
        }

    }

    override fun onItemTouch(swipeView: View) {

        when (swipeView.id) {
            R.id.ivAnswerCall -> {
                llAnswer?.visibility = View.INVISIBLE
                image?.clearAnimation()
            }
            R.id.ivRejectCall -> {
                llAnswer?.visibility = View.INVISIBLE
                llReply?.visibility = View.INVISIBLE
                llReject?.visibility = View.VISIBLE
                tvRejectCall?.visibility = View.VISIBLE
                tvReplyCall?.visibility = View.GONE
                tvAnswerCall?.visibility = View.INVISIBLE
                countDownTimer?.cancel()
                countDownTimer = object : CountDownTimer(2000, 1000) {
                    override fun onFinish() {
                        llAnswer?.visibility = View.VISIBLE
                        llReject?.visibility = View.INVISIBLE
                        llReply?.visibility = View.INVISIBLE
                        tvRejectCall?.visibility = View.INVISIBLE
                        tvReplyCall?.visibility = View.GONE
                        tvAnswerCall?.visibility = View.VISIBLE
                    }

                    override fun onTick(millisUntilFinished: Long) {
                    }

                }
                countDownTimer?.start()
            }

            R.id.ivReplyCall -> {
                llAnswer?.visibility = View.INVISIBLE
                llReply?.visibility = View.VISIBLE
                llReject?.visibility = View.INVISIBLE
                tvReplyCall?.visibility = View.GONE
                tvAnswerCall?.visibility = View.INVISIBLE
                tvRejectCall?.visibility = View.INVISIBLE
                countDownTimer?.cancel()
                countDownTimer = object : CountDownTimer(2000, 1000) {
                    override fun onFinish() {
                        llAnswer?.visibility = View.VISIBLE
                        llReject?.visibility = View.INVISIBLE
                        llReply?.visibility = View.INVISIBLE
                        tvReplyCall?.visibility = View.GONE
                        tvAnswerCall?.visibility = View.VISIBLE
                        tvRejectCall?.visibility = View.INVISIBLE
                    }

                    override fun onTick(millisUntilFinished: Long) {
                    }

                }
                countDownTimer?.start()
            }
        }
    }

    override fun onItemTouchReleased(swipeView: View) {
        when (swipeView.id) {
            R.id.ivAnswerCall -> {
                llAnswer?.visibility = View.VISIBLE
                onShakeImage()
            }
        }
    }

    override fun onItemAnswered(swipeView: View) {
        when (swipeView.id) {
            R.id.ivAnswerCall -> {
                answerCall()
            }
            R.id.ivRejectCall -> {
                videoCallService?.rejectCall()
                Handler().postDelayed({
                    stopServiceAndCloseConnection()
                }, 500)
            }

//            R.id.ivReplyCall -> {
//                runOnUiThread {
//                    var dialog = Dialog(this@FuguCallActivity, android.R.style.Theme_Translucent_NoTitleBar)
//                    dialog.setContentView(R.layout.dialog_call_options)
//                    val lp = dialog?.window!!.attributes
//                    lp.dimAmount = 0.5f
//                    dialog.window!!.attributes = lp
//                    dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
//                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
//                    dialog.setCancelable(true)
//                    dialog.setCanceledOnTouchOutside(true)
//
//                    var optionOne = dialog.findViewById<AppCompatTextView>(R.id.option_one)
//                    var optionTwo = dialog.findViewById<AppCompatTextView>(R.id.option_two)
//                    var optionThree = dialog.findViewById<AppCompatTextView>(R.id.option_three)
//                    var optionFour = dialog.findViewById<AppCompatTextView>(R.id.option_four)
//                    var optionCustom = dialog.findViewById<AppCompatTextView>(R.id.custom_action)
//
//                    val intent = Intent(this@FuguCallActivity, ChatActivity::class.java)
//
//                    val conversation = FuguConversation()
//                    conversation.businessName = ""
//                    conversation.label = ""
//                    conversation.isOpenChat = true
//                    conversation.channelId = videoCallModel?.channelId
//                    conversation.chat_type = 2
//                    conversation.userName = StringUtil.toCamelCase(com.skeleton.mvp.data.db.CommonData.getCommonResponse()
//                    .getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].fullName)
//                    conversation.userId = java.lang.Long.valueOf(com.skeleton.mvp.data.db.CommonData.getCommonResponse()
//                    .getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].userId)
//                    conversation.enUserId = com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo
//                    [com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].enUserId
//                    conversation.unreadCount = 0
//                    intent.putExtra(FuguAppConstant.CONVERSATION, Gson().toJson(conversation, FuguConversation::class.java))
//                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
//                    optionOne.setOnClickListener {
//                        startChatActivity(intent, "Can't talk now. What's up?")
//
//                    }
//                    optionTwo.setOnClickListener {
//                        startChatActivity(intent, "I'll call you right back.")
//                    }
//                    optionThree.setOnClickListener {
//                        startChatActivity(intent, "I'll call you later.")
//                    }
//                    optionFour.setOnClickListener {
//                        startChatActivity(intent, "Can't talk now. Call me later?")
//                    }
//                    optionCustom.setOnClickListener {
//                        startChatActivity(intent, "")
//                    }
//                    dialog.show()
//                }
//            }
        }
    }

//    private fun startChatActivity(intent: Intent, text: String) {
//        videoCallService?.rejectCall()
//        stopServiceAndStartConf()
//        CommonData.setCustomText(text)
//        Handler().postDelayed({
//            intent.putExtra("SendCustomMessage", text)
//            startActivity(intent)
//        }, 300)
//    }

    fun unbindServiceConnection() {
        if (mBounded) {
            unbindService(mConnection)
            mBounded = false
        }
    }

    fun onCallDisconnectEvent() {
        runOnUiThread {
            llConnectivityIssues?.visibility = View.VISIBLE
        }
        onCustomActionClicked("call_connectivity_issues")
    }

    fun onCallConnectEvent() {
        if(llConnectivityIssues?.visibility == View.VISIBLE)
            onCustomActionClicked("call_reconnected")

        runOnUiThread {
            llConnectivityIssues?.visibility = View.GONE
        }
    }

    fun onHungupSent() {
        ivHungupCallTimer?.cancel()
        videoCallService?.cancelCalltimer()
        val jsonObject: JSONObject? = null
        jsonObject?.put("call_type", videoCallModel?.callType)
        videoCallService?.onCallHungUp(jsonObject, false)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        try {
            if (videoCallModel?.activityLaunchState!!.equals(WebRTCCallConstants.AcitivityLaunchState.OTHER.toString())) {
                mediaPlayer?.stop()
            }
        } catch (e: Exception) {
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        try {
            if (videoCallModel?.activityLaunchState!!.equals(WebRTCCallConstants.AcitivityLaunchState.OTHER.toString())) {
                mediaPlayer?.stop()
            }
        } catch (e: Exception) {
        }
        return super.onKeyDown(keyCode, event)
    }

    fun rejectCall() {
        val devicePayload = JSONObject()
        devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(this))
        devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
        devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
        devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(this))
        signal = Signal(videoCallModel?.userId, videoCallModel?.signalUniqueId, videoCallModel?.fullName,
            videoCallModel?.turnApiKey, videoCallModel?.turnUserName, videoCallModel?.turnCredential
            , videoCallModel?.stunServers, videoCallModel?.turnServers, devicePayload, videoCallModel?.callType!!)
        val jsonObject = JSONObject()
        jsonObject.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.Companion.VideoCallType.CALL_REJECTED.toString())
        val rejectedJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(rejectedJson)
    }

    fun callHungup() {
        val devicePayload = JSONObject()
        devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(this))
        devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER)
        devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().versionName)
        devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(this))
        signal = Signal(videoCallModel?.userId, videoCallModel?.signalUniqueId, videoCallModel?.fullName,
            videoCallModel?.turnApiKey, videoCallModel?.turnUserName, videoCallModel?.turnCredential
            , videoCallModel?.stunServers, videoCallModel?.turnServers, devicePayload, videoCallModel?.callType!!)
        val jsonObject = JSONObject()
        jsonObject.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.Companion.VideoCallType.CALL_HUNG_UP.toString())
        jsonObject.put(WebRTCCallConstants.HUNGUP_TYPE, "DEFAULT")
        val rejectedJson = addCommonuserDetails(jsonObject)
        addTurnCredentialsAndDeviceDetails(rejectedJson)
    }

    fun addCommonuserDetails(jsonObject: JSONObject): JSONObject {
        jsonObject.put(WebRTCCallConstants.IS_SILENT, true)
        jsonObject.put(WebRTCCallConstants.USER_ID, signal?.signalUniqueUserId)
        jsonObject.put(WebRTCCallConstants.MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
        jsonObject.put(WebRTCCallConstants.IS_TYPING, 0)
        jsonObject.put(WebRTCCallConstants.IS_SILENT, true)
        jsonObject.put(WebRTCCallConstants.MESSAGE_UNIQUE_ID, signal?.signalUniqueId)
        return jsonObject
    }

    fun addTurnCredentialsAndDeviceDetails(jsonObject: JSONObject) {
        val stunServers = JSONArray()
        val turnServers = JSONArray()
        val videoCallCredentials = JSONObject()

        videoCallCredentials.put(WebRTCCallConstants.TURN_API_KEY, signal?.turnApiKey)
        videoCallCredentials.put(WebRTCCallConstants.USER_NAME, signal?.turnUserName)
        videoCallCredentials.put(WebRTCCallConstants.CREDENTIAL, signal?.turnCredential)
        for (i in signal?.stunServers!!.indices) {
            stunServers.put(signal?.stunServers!!.get(i))
        }
        for (i in signal?.turnServers!!.indices) {
            turnServers.put(signal?.turnServers!!.get(i))
        }

        videoCallCredentials.put(WebRTCCallConstants.STUN, stunServers)
        videoCallCredentials.put(WebRTCCallConstants.TURN, turnServers)
        jsonObject.put(FuguAppConstant.CHANNEL_ID, videoCallModel?.channelId)
        jsonObject.put(WebRTCCallConstants.TURN_CREDENTIALS, videoCallCredentials)
        jsonObject.put(WebRTCCallConstants.DEVICE_PAYLOAD, signal?.deviceDetails)
        jsonObject.put("call_type", signal?.callType)
        videoCallService?.webRTCSignallingClient?.publishMessage(jsonObject)
    }

    private fun createConversation(fuguCreateConversationParams: FuguCreateConversationParams) {
        try {
            isConnecting = true
//            if (CommonData.getUpdatedDetails().getData().getMultiChannelLabelMapping() == 1) {
//                fuguCreateConversationParams.setMultiChannelLabelMapping(CommonData.getUpdatedDetails().getData().getMultiChannelLabelMapping())
//            }
            //showConnecting()
            RestClient.getApiInterface().createConversation(fuguCreateConversationParams)
                .enqueue(object : ResponseResolver<FuguCreateConversationResponse>(this, false, false) {
                    override fun success(t: FuguCreateConversationResponse?) {
                        if(!isHungup) {
                            videoCallService?.restartTimer()
                            var channelId: Long = t?.getData()?.getChannelId()!!
                            videoCallModel?.channelId = channelId;
                            isConnecting = false
                            runOnUiThread {
                                tvCallTimer?.text = CALLING
                            }
                            fetchIntentData("")
                            CommonData.setChannelIds(fuguCreateConversationParams.transactionId, channelId)
                        }
                    }

                    override fun failure(error: APIError?) {
                        Toast.makeText(this@FuguCallActivity, ""+error?.message, Toast.LENGTH_SHORT).show()
                    }

                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    //hippo customization

    private var view: LinearLayout? = null
    private var tvTimerView: AppCompatTextView? = null

    private fun addingView() {
        if(CommonData.hasExtraView()) {
            if(HippoCallConfig.getInstance().getFragment() == null) {
                onCustomActionClicked("need_fragment")
            } else {
                openFragment(HippoCallConfig.getInstance().getFragment())
            }
        } else {
            view?.visibility = View.GONE
        }
    }


    override fun updateFragment(fragment: androidx.fragment.app.Fragment?) {
        if(fragment == null) {
            view?.visibility = View.GONE
        } else {
            openFragment(fragment)
        }
    }

    override fun updateTimer(time: String) {
        try {
            runOnUiThread {
                if(!TextUtils.isEmpty(time))
                    tvTimerView?.visibility = View.VISIBLE
                tvTimerView?.text = time
            }
        } catch (e: Exception) {
        }
    }

    override fun timerVisibilityStatus(status: Int) {
        try {
            runOnUiThread {
                tvTimerView?.visibility = status
            }
        } catch (e: Exception) {
        }
    }

    private fun sendButtonStatus() {
        sendButtonStatus(false)
    }
    private fun sendButtonStatus(flag: Boolean) {
        if (!isHungup && callStatus.equals(WebRTCCallConstants.CallStatus.IN_CALL.toString())) {
            var json: JSONObject = JSONObject()
            json.put("is_video_paused", flag)
            json.put("is_mute", !videoCallService?.isAudioEnabled!!)
            json.put("is_camera_closed", !isVideoEnabled)
            videoCallService?.webRTCSignallingClient?.publishOperationMessage(json)

            publishData(json)
        }
    }


    override fun sendCustomData(jsonObject: JSONObject) {
        videoCallService?.webRTCSignallingClient?.publishMessage(jsonObject)
    }

    override fun onNetworkStatusChange(status: Int) {
        if(status > 0) {
            videoCallService?.webRTCSignallingClient?.fayeConnectionRetry()
        }
    }

    /**
     * Called when a custom action button is clicked
     *
     * @param buttonAction the action button object associated with this button
     */
    fun onCustomActionClicked(buttonAction: String) {
        val intent = Intent()
        intent.putExtra("HIPPO_CALL_ACTION_PAYLOAD", buttonAction)
        intent.action = "HIPPO_CALL_ACTION_SELECTED"
        sendBroadcast(intent)
    }


    var pictureInPictureParamsBuilder : PictureInPictureParams.Builder? = null

    fun getPipBuilder(): PictureInPictureParams.Builder {
        if(pictureInPictureParamsBuilder == null)
            pictureInPictureParamsBuilder = PictureInPictureParams.Builder()

        return pictureInPictureParamsBuilder!!
    }

    private fun startPictureInPictureFeature() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isInPictureInPictureMode && videoCallModel?.callType.equals("VIDEO", ignoreCase = true)) {

                var width: Int
                var height: Int
                try {
                    width = remoteSurfaceview?.width!!
                    height = remoteSurfaceview?.height!!
                } catch (e: Exception) {
                    width = window.decorView.width
                    height = window.decorView.height
                }

                val aspectRatio = Rational(width, height)
                getPipBuilder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(getPipBuilder().build())
            }
        }
    }

    public override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!isInPictureInPictureMode && callStatus.equals(WebRTCCallConstants.CallStatus.IN_CALL.toString())) {

                var width: Int
                var height: Int
                try {
                    width = remoteSurfaceview?.width!!
                    height = remoteSurfaceview?.height!!
                } catch (e: Exception) {
                    width = window.decorView.width
                    height = window.decorView.height
                }

                val aspectRatio = Rational(width, height)
                getPipBuilder().setAspectRatio(aspectRatio).build()
                enterPictureInPictureMode(getPipBuilder().build())
            }
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        HippoLog.e("TAG", "onMultiWindowModeChanged")
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if (isInPictureInPictureMode) {
            view?.visibility = View.GONE
            llDialingCallActions?.visibility = View.GONE
            llVideoCall?.visibility = View.GONE
            ivBack?.visibility = View.GONE
            tvTimerView?.visibility = View.GONE
            tvCallAction?.visibility = View.GONE
        } else {
            view?.visibility = View.VISIBLE
            llDialingCallActions?.visibility = View.VISIBLE
            llVideoCall?.visibility = View.VISIBLE
            ivBack?.visibility = View.VISIBLE
            updateScreenStatus()
        }
    }


    private fun openFragment(fragment: androidx.fragment.app.Fragment) {
        try {
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            if (supportFragmentManager.backStackEntryCount > 0) {
                for (i in 0 until fragmentManager.getBackStackEntryCount()) {
                    fragmentManager.popBackStack()
                }
            }
            transaction.add(R.id.custom_view, fragment, fragment.javaClass.name)
            transaction.addToBackStack(fragment.javaClass.name)
            if (supportFragmentManager.backStackEntryCount > 0) {
                transaction.hide(
                    supportFragmentManager.findFragmentByTag(
                        supportFragmentManager
                            .getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name
                    )!!
                )
            }
            transaction.commitAllowingStateLoss()
        } catch (e: Exception) {
        }
    }

    private fun hasPipEnable(): Boolean {
        val packageManager = applicationContext.packageManager
        val supportsPIP = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        return supportsPIP
    }

    fun publishMessage(customData: String) {
        val intent = Intent()
        intent.putExtra("HIPPO_CALL_ACTION_PAYLOAD", "CUSTOM_DATA")
        intent.putExtra("data", customData)
        intent.action = "HIPPO_CALL_ACTION_SELECTED"
        sendBroadcast(intent)
    }

    fun updateScreenStatus() {
        if(!isHungup) {
            ivMuteIcon?.visibility = View.GONE
            ivVideoIcon?.visibility = View.GONE
            val screenStatus = videoCallService?.screenStatus
            if (!TextUtils.isEmpty(screenStatus)) {
                tvCallAction?.visibility = View.VISIBLE
                tvCallAction?.text = screenStatus
            } else {
                tvCallAction?.visibility = View.GONE
            }
        }
    }

    fun chechScreenAction() {
        if(!isHungup) {
            runOnUiThread {
                ivMuteIcon?.visibility = View.GONE
                ivVideoIcon?.visibility = View.GONE
                if(!isLocalViewSmall) {
                    tvCallAction?.visibility = View.GONE
                    if (videoCallService?.isMute!!) {
                        ivMuteIcon?.visibility = View.VISIBLE
                    }
                    if (videoCallService?.isCameraClosed!!) {
                        ivVideoIcon?.visibility = View.VISIBLE
                    }
                    if (videoCallService?.isVideoPause!!) {
                        ivVideoIcon?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    fun setUserAction(screenStatus: String) {
        if (!isHungup) {
            runOnUiThread {
                if (isLocalViewSmall) {
                    ivMuteIcon?.visibility = View.GONE
                    ivVideoIcon?.visibility = View.GONE
                    if (!TextUtils.isEmpty(screenStatus)) {
                        tvCallAction?.visibility = View.VISIBLE
                        tvCallAction?.text = screenStatus
                    } else {
                        tvCallAction?.visibility = View.GONE
                    }
                } else {
                    tvCallAction?.visibility = View.GONE
                }
            }
        }
    }

    fun publishData(data: JSONObject) {
        val intent = Intent()
        intent.putExtra("HIPPO_CALL_ACTION_PAYLOAD", "UI_ACTION")
        intent.putExtra("button_data", data.toString())
        intent.action = "HIPPO_CALL_ACTION_SELECTED"
        sendBroadcast(intent)
    }

}


