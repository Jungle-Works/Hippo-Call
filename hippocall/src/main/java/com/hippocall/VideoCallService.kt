package com.hippocall

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.text.TextUtils
import android.widget.RemoteViews
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.*
import com.hippo.eventbus.BusProvider
import com.hippo.helper.BusEvents
import com.hippo.helper.FayeMessage
import com.hippo.utils.HippoLog
import com.hippocall.WebRTCCallConstants.Companion.CALL_STATUS
import com.hippocall.WebRTCCallConstants.Companion.ONGOING_AUDIO_CALL
import com.hippocall.WebRTCCallConstants.Companion.ONGOING_VIDEO_CALL
import com.squareup.otto.Subscribe
import org.json.JSONObject
import org.webrtc.*
import java.util.*

@Suppress("DEPRECATION")
/**
 * Created by rajatdhamija
 * 06/09/18.
 */

class VideoCallService : Service(), WebRTCFayeCallbacks, WebRTCCallCallbacks {

    private var signal: Signal? = null
    private var connection: Connection? = null
    private var videoCallModel: VideoCallModel? = null
    private var mBinder: IBinder = LocalBinder()
    private var status: String = ""
    var peerConnection: PeerConnection? = null
    private var remoteVideoStream: MediaStream? = null
    private var localVieoStream: MediaStream? = null
    var webRTCSignallingClient: WebRTCSignallingClient? = null
    var webRTCCallClient: WebRTCCallClient? = null
    var isCallConnected: Boolean? = false
    var isReadyForConnection: Boolean? = false
    var isCallInitiated: Boolean? = false
    var fuguCallActivity: FuguCallActivity? = null
    private var rootEglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var intent: Intent? = null
    var callDisconnectTime: CountDownTimer? = null
    var startTime: Long? = null
    var callTimer: CountDownTimer? = null
    var timer: Timer? = null
    var mediaPlayer: MediaPlayer? = null
    var isCallFailed = true
    var mListener: AudioManager.OnAudioFocusChangeListener? = null

    var isAudioEnabled = true

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    inner class LocalBinder : Binder() {
        val serverInstance: VideoCallService
            get() = this@VideoCallService
    }


    override fun onDestroy() {
        super.onDestroy()
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoCallReciever)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val hungupIntent = Intent(VIDEO_CALL_HUNGUP)
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(hungupIntent)
        CommonData.setVideoCallModel(null)
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_RINGTONE
        if (am.isBluetoothScoOn) {
            am.startBluetoothSco()
            am.stopBluetoothSco()
        }
        am.abandonAudioFocus(mListener)
        callTimer = null
        try {
            resiterBus = false
            BusProvider.getInstance().unregister(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val mVideoCallReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onBroadCastrecieved(intent)
        }
    }

    var seconds: Int = 0
    var minutes: Int = 0
    var hours: Int = 0

    var resiterBus = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.getAction().equals("com.fuguchat.stop")) {
            HippoLog.i("LOG_TAG", "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        } else {
            if (mListener == null) {
                mListener = AudioManager.OnAudioFocusChangeListener { }
            }
            try {
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoCallReciever)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                if(!resiterBus) {
                    resiterBus = true
                    BusProvider.getInstance().register(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
                mVideoCallReciever,
                IntentFilter(VIDEO_CALL_INTENT)
            )

            if (intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL) {
                if (startTime == null) {
                    startTime = System.currentTimeMillis()
                    seconds = 0
                    minutes = 0
                    hours = 0
                }

                if (timer == null) {
                    timer = Timer(true)
                    timer?.scheduleAtFixedRate(MyTimerTask(object : TimerUpdate {
                        override fun update() {
                            seconds += 1
                            if (seconds > 59) {
                                seconds = 0
                                minutes += 1
                            }
                            if (minutes > 59) {
                                minutes = 0
                                hours += 1
                            }


                            var secondstext = ""
                            if (seconds < 10) {
                                secondstext = "0$seconds"
                            } else {
                                secondstext = "$seconds"
                            }

                            //HippoLog.i("timerStr", "$timerStr <~~~> $timerMills")

                            //val time = TimeUnit.MICROSECONDS.
                            if (fuguCallActivity != null) {
                                if (hours > 0) {
                                    fuguCallActivity!!.updateCallTimer("$hours:$minutes:$secondstext")
                                } else {
                                    fuguCallActivity!!.updateCallTimer("$minutes:$secondstext")
                                }
                            }
                        }

                    }), 1000, 1000)
                }

                HippoLog.i("TAG", "callTimer closed here")
            }
            this.intent = intent
            if (intent.hasExtra(INIT_FULL_SCREEN_SERVICE)) {
                val notificationIntent = Intent(this, FuguCallActivity::class.java)

                if (videoCallModel == null) {
                    videoCallModel = CommonData.getVideoCallModel()
                }
                val customView = RemoteViews(packageName, R.layout.hippo_cutom_call_notification)
                customView.setTextViewText(R.id.name, videoCallModel?.channelName)

                notificationIntent.action = Intent.ACTION_MAIN
                notificationIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
                notificationIntent.putExtra("videoCallModel", videoCallModel)
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                if (status == WebRTCCallConstants.AcitivityLaunchState.KILLED.toString()) {
                    notificationIntent.putExtra("activitylaunchState", status)
                }

                val hungupIntent = Intent(this, FuguCallActivity::class.java)
                hungupIntent.action = Intent.ACTION_DELETE
                //hungupIntent.action = "hippo.intent.action.DELETE"
                hungupIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
                hungupIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                hungupIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)


                val answerIntent = Intent(this, FuguCallActivity::class.java)
                answerIntent.action = Intent.ACTION_ANSWER
                answerIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
                answerIntent.putExtra("videoCallModel", videoCallModel)
                if(intent.hasExtra("messageJson")) {
                    HippoLog.e("messageJson", "$%^&*&^%$$%^&*&$%^&*&^%^&*^%^&*%^&")
                    answerIntent.putExtra("video_offer", intent.getStringExtra("messageJson"))
                }
                answerIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                answerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

                val pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                val hungupPendingIntent = PendingIntent.getActivity(this, 0,
                    hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                val answerPendingIntent = PendingIntent.getActivity(this, 0,
                    answerIntent, PendingIntent.FLAG_UPDATE_CURRENT)

                customView.setOnClickPendingIntent(R.id.btnAnswer, answerPendingIntent)
                customView.setOnClickPendingIntent(R.id.btnDecline, hungupPendingIntent)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    val notification = NotificationCompat.Builder(this, "IncomingCall")
                    notification.setContentTitle(intent.getStringExtra(CHANNEL_NAME))
                    if (intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL) {
                        notification.setUsesChronometer(true)
                        notification.setShowWhen(false)
                    }
                    notification.setTicker(intent.getStringExtra(CALL_STATUS))
                    notification.setContentText(intent.getStringExtra(CALL_STATUS))
                    notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
                    notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))
                    notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    notification.setCategory(NotificationCompat.CATEGORY_CALL)
                    notification.setVibrate(null)
                    notification.setOngoing(true)
                    notification.setFullScreenIntent(pendingIntent, true)
                    notification.priority = getPriority()
                    notification.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    notification.setCustomContentView(customView)
                    notification.setCustomBigContentView(customView)

                    startForeground(1122, notification.build())
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        var notificationChannel = NotificationChannel("IncomingCall",
                            "IncomingCall", NotificationManager.IMPORTANCE_HIGH)
                        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        notificationChannel.setSound(uri, null)
                        notificationManager.createNotificationChannel(notificationChannel)
                    }
                } else {

                    val notification = NotificationCompat.Builder(this)
                    notification.setContentTitle(intent.getStringExtra(CHANNEL_NAME))
                    notification.setTicker(intent.getStringExtra(CALL_STATUS))
                    notification.setContentText(intent.getStringExtra(CALL_STATUS))
                    if (intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL) {
                        notification.setUsesChronometer(true)
                        notification.setShowWhen(false)
                    }
                    notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
                    notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))
                    notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    notification.setVibrate(null)
                    notification.setContentIntent(pendingIntent)
                    notification.setOngoing(true)
                    notification.setCategory(NotificationCompat.CATEGORY_CALL)
                    notification.priority = getPriority()
                    val hangupAction = NotificationCompat.Action.Builder(
                        android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent)
                        .build()
                    notification.addAction(hangupAction)
                    startForeground(1122, notification.build())
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                }
            } else {
                val notificationIntent = Intent(this, FuguCallActivity::class.java)
                notificationIntent.action = Intent.ACTION_MAIN
                notificationIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                notificationIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                if (status == WebRTCCallConstants.AcitivityLaunchState.KILLED.toString()) {
                    notificationIntent.putExtra("activitylaunchState", status)
                }

                val hungupIntent = Intent(this, FuguCallActivity::class.java)
                hungupIntent.action = Intent.ACTION_DELETE
                hungupIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
                hungupIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                hungupIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                val pendingIntent = PendingIntent.getActivity(
                    this, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )

                val hungupPendingIntent = PendingIntent.getActivity(
                    this, 0,
                    hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    val notification = NotificationCompat.Builder(this, "VideoCall")
                    notification.setContentTitle(intent.getStringExtra(CHANNEL_NAME))
                    if (intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL) {
                        notification.setUsesChronometer(true)
                        notification.setShowWhen(false)
                    }
                    notification.setTicker(intent.getStringExtra(CALL_STATUS))
                    notification.setContentText(intent.getStringExtra(CALL_STATUS))

                    notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
                    notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))

                    notification.setContentIntent(pendingIntent)
                    notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    notification.setVibrate(null)
                    notification.setOngoing(true)

                    notification.priority = getPriority()
                    val hangupAction = NotificationCompat.Action.Builder(
                        android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent
                    )
                        .build()
                    notification.addAction(hangupAction)

                    startForeground(1122, notification.build())
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        var notificationChannel = NotificationChannel(
                            "VideoCall",
                            "VideoCall", NotificationManager.IMPORTANCE_LOW
                        )
                        notificationChannel.setSound(null, null)
                        notificationManager.createNotificationChannel(notificationChannel)
                    }
                    notificationManager.notify(1122, notification.build())
                } else {

                    val notification = NotificationCompat.Builder(this)
                    notification.setContentTitle(intent.getStringExtra(CHANNEL_NAME))
                    notification.setTicker(intent.getStringExtra(CALL_STATUS))
                    notification.setContentText(intent.getStringExtra(CALL_STATUS))
                    if (intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL) {
                        notification.setUsesChronometer(true)
                        notification.setShowWhen(false)
                    }
                    notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
                    notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))

                    notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    notification.setVibrate(null)
                    notification.setContentIntent(pendingIntent)
                    notification.setOngoing(true)

                    notification.priority = getPriority()
                    val hangupAction = NotificationCompat.Action.Builder(
                        android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent
                    )
                        .build()
                    notification.addAction(hangupAction)
                    startForeground(1122, notification.build())
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(1122, notification.build())
                }
            }


            if (callDisconnectTime == null) {
                callDisconnectTime = object : CountDownTimer(30000, 1000) {

                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        if (!isCallConnected!!) {
                            webRTCSignallingClient?.cancelCounter()
                            hungUpCall()
                            fuguCallActivity?.onCallHungUp(null, false)
                        }
                    }
                }.start()
            } else {
                callDisconnectTime?.cancel()
            }
        }
        return Service.START_STICKY
    }

    /*override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action.equals("com.fuguchat.stop")) {
            HippoLog.i("LOG_TAG", "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        } else {
            if (mListener == null) {
                mListener = AudioManager.OnAudioFocusChangeListener { }
            }
            try {
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).unregisterReceiver(mVideoCallReciever)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).registerReceiver(
                mVideoCallReciever,
                IntentFilter(VIDEO_CALL_INTENT)
            )

            if (intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL) {
                if (startTime == null) {
                    startTime = System.currentTimeMillis()
                    seconds = 0
                    minutes = 0
                    hours = 0
                }

                if (timer == null) {
                    timer = Timer(true)
                    timer?.scheduleAtFixedRate(MyTimerTask(object : TimerUpdate {
                        override fun update() {
                            seconds += 1
                            if (seconds > 59) {
                                seconds = 0
                                minutes += 1
                            }
                            if (minutes > 59) {
                                minutes = 0
                                hours += 1
                            }


                            var secondstext = ""
                            if (seconds < 10) {
                                secondstext = "0$seconds"
                            } else {
                                secondstext = "$seconds"
                            }

                            //HippoLog.i("timerStr", "$timerStr <~~~> $timerMills")

                            //val time = TimeUnit.MICROSECONDS.
                            if (fuguCallActivity != null) {
                                if (hours > 0) {
                                    fuguCallActivity!!.updateCallTimer("$hours:$minutes:$secondstext")
                                } else {
                                    fuguCallActivity!!.updateCallTimer("$minutes:$secondstext")
                                }
                            }
                        }

                    }), 1000, 1000)
                }

                HippoLog.i("TAG", "callTimer closed here")
            }
            this.intent = intent
            val notificationIntent = Intent(this, FuguCallActivity::class.java)
            notificationIntent.action = Intent.ACTION_MAIN
            notificationIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            if (status == WebRTCCallConstants.AcitivityLaunchState.KILLED.toString()) {
                notificationIntent.putExtra("activitylaunchState", status)
            }

            val hungupIntent = Intent(this, FuguCallActivity::class.java)
            hungupIntent.action = Intent.ACTION_DELETE
            hungupIntent.putExtra(CHANNEL_NAME, videoCallModel?.channelName)
            hungupIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            hungupIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            val pendingIntent = PendingIntent.getActivity(
                this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val hungupPendingIntent = PendingIntent.getActivity(
                this, 0,
                hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val notification = NotificationCompat.Builder(this, "VideoCall")
                notification.setContentTitle(intent.getStringExtra(CHANNEL_NAME))
                if (intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL) {
                    notification.setUsesChronometer(true)
                    notification.setShowWhen(false)
                }
                notification.setTicker(intent.getStringExtra(CALL_STATUS))
                notification.setContentText(intent.getStringExtra(CALL_STATUS))

                notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
                notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))

                notification.setContentIntent(pendingIntent)
                notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                notification.setVibrate(null)
                notification.setOngoing(true)

                notification.priority = getPriority()
                val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent
                )
                    .build()
                notification.addAction(hangupAction)

                startForeground(1122, notification.build())
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    var notificationChannel = NotificationChannel(
                        "VideoCall",
                        "VideoCall", NotificationManager.IMPORTANCE_LOW
                    )
                    notificationChannel.setSound(null, null)
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                notificationManager.notify(1122, notification.build())
            } else {

                val notification = NotificationCompat.Builder(this)
                notification.setContentTitle(intent.getStringExtra(CHANNEL_NAME))
                notification.setTicker(intent.getStringExtra(CALL_STATUS))
                notification.setContentText(intent.getStringExtra(CALL_STATUS))
                if (intent.getStringExtra(CALL_STATUS) == ONGOING_VIDEO_CALL || intent.getStringExtra(CALL_STATUS) == ONGOING_AUDIO_CALL) {
                    notification.setUsesChronometer(true)
                    notification.setShowWhen(false)
                }
                notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
                notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))

                notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                notification.setVibrate(null)
                notification.setContentIntent(pendingIntent)
                notification.setOngoing(true)

                notification.priority = getPriority()
                val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent
                )
                    .build()
                notification.addAction(hangupAction)
                startForeground(1122, notification.build())
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1122, notification.build())
            }
            if (callDisconnectTime == null) {
                callDisconnectTime = object : CountDownTimer(30000, 1000) {

                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        if (!isCallConnected!!) {
                            webRTCSignallingClient?.cancelCounter()
                            hungUpCall()
                            fuguCallActivity?.onCallHungUp(null, false)
                        }
                    }
                }.start()
            } else {
                callDisconnectTime?.cancel()
            }
        }
        return Service.START_STICKY
    }*/

    private fun getPriority(): Int {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            Notification.PRIORITY_MAX
        }
    }

    fun createWebRTCSignallingConnection(videoCallModel: VideoCallModel?, signal: Signal?) {
        this.videoCallModel = videoCallModel
        this.signal = signal
        webRTCSignallingClient =
            WebRTCSignallingClient(this, videoCallModel?.channelId, videoCallModel?.activityLaunchState)
        webRTCSignallingClient?.setSignalRequirementModel(signal)
        webRTCSignallingClient?.setUpFayeConnection()
    }

    fun createWebRTCSignallingConnection(jsonObject: JSONObject?, videoCallModel: VideoCallModel?, signal: Signal?) {
        this.videoCallModel = videoCallModel
        this.signal = signal
        webRTCSignallingClient =
            WebRTCSignallingClient(this, videoCallModel?.channelId, videoCallModel?.activityLaunchState)
        webRTCSignallingClient?.setSignalRequirementModel(signal)
        webRTCSignallingClient?.passServiceCall(this, videoCallModel?.channelId!!)
        onVideoOfferRecieved(jsonObject)

    }

    fun setConnectionModel(connection: Connection?) {
        this.connection = connection
    }

    fun setSignalModel(signal: Signal?) {
        this.signal = signal
    }

    fun setActivityContext(fuguCallActivity: FuguCallActivity?) {
        this.fuguCallActivity = fuguCallActivity
    }

    fun createWebRTCCallConnection() {
        try {
            webRTCCallClient = WebRTCCallClient(this)
            if (videoCallModel?.channelId!!.toInt() > 0)
                webRTCSignallingClient =
                    WebRTCSignallingClient(this, videoCallModel?.channelId, videoCallModel?.activityLaunchState)
        } catch (e: Exception) {
        }
    }

    fun isFayeConnected(): Boolean {
        if (webRTCSignallingClient != null) {
            return webRTCSignallingClient!!.isFayeConnected()
        } else {
            return false
        }
    }

    fun hungUpCall() {
        hungUpCall(true)
    }

    fun hungUpCall(flag: Boolean) {
        isCallFailed = false
        if (runnable != null) {
            handle.removeCallbacks(runnable!!)
        }
        if (callDisconnectTime != null) {
            callDisconnectTime?.cancel()
        }
        if (callTimer != null)
            callTimer?.cancel()

        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }

        if (flag)
            webRTCSignallingClient?.hangUpCall()
    }

    fun hungUpCallLocally() {
        isCallFailed = false
        if (runnable != null) {
            handle.removeCallbacks(runnable!!)
        }
        if (callDisconnectTime != null) {
            callDisconnectTime?.cancel()
        }
    }

    fun rejectCall() {
        webRTCSignallingClient?.rejectCall()
    }

    fun saveOfferAndAnswer(videoOfferjson: JSONObject?) {

        webRTCCallClient?.saveOfferAndAnswer(videoOfferjson, connection)
    }

    fun saveIceCandidate(jsonObject: JSONObject?) {
        webRTCCallClient?.saveIceCandidate(jsonObject)
    }

    fun createPeerConnection(connection: Connection?) {
        webRTCCallClient = WebRTCCallClient(this)
        peerConnection = webRTCCallClient?.createPeerConnection(connection)
        HippoLog.e("peerConnection>>>>>>>>>>", peerConnection.toString())
    }

    fun getRemoteVideoStream(): MediaStream? {
        return remoteVideoStream
    }

    fun setLocalVideoStream(localVideoStream: MediaStream?) {
        this.localVieoStream = localVideoStream
    }

    fun getLocalVideoStream(): MediaStream? {
        return localVieoStream
    }

    fun getPeerconnection(): PeerConnection? {
        return peerConnection
    }

    fun createOffer(connection: Connection?) {
        webRTCCallClient?.createOffer(connection)
    }

    fun setVideoModel(videoCallModel: VideoCallModel?) {
        this.videoCallModel = videoCallModel
        Thread {
            kotlin.run {
                CommonData.setVideoCallModel(videoCallModel)
            }
        }.start()
    }

    fun closePeerConnection() {
        if (peerConnection != null) {
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
        }
    }

    fun closePeerSwitchConnection() {
        if (peerConnection != null) {
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
        }
    }

    override fun onIceCandidateRecieved(jsonObject: JSONObject?) {
        fuguCallActivity?.onIceCandidateRecieved(jsonObject)
    }

    override fun onVideoOfferRecieved(jsonObject: JSONObject?) {
        fuguCallActivity?.onVideoOfferRecieved(jsonObject)
    }

    override fun onVideoAnswerRecieved(jsonObject: JSONObject?) {
        if (callDisconnectTime != null) {
            callDisconnectTime?.cancel()
        }
        fuguCallActivity?.onVideoAnswerRecieved(jsonObject)
    }

    fun onCallConnected() {
        fuguCallActivity?.onCustomActionClicked("call_connected")
    }

    override fun onReadyToConnectRecieved(jsonObject: JSONObject?) {
        fuguCallActivity?.onReadyToConnectRecieved(jsonObject)
    }

    override fun onCallHungUp(jsonObject: JSONObject?, showFeedback: Boolean) {
        fuguCallActivity?.onCustomActionClicked("callEnded")
        isCallFailed = false
        if (callTimer != null) {
            callTimer!!.cancel()
        }

        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
        if (!foregrounded() || !isCallConnected!!) {
            fuguCallActivity?.onCallHungUp(jsonObject, false)
        } else {
            fuguCallActivity?.onCallHungUp(jsonObject, true)
        }
    }

    fun onCallFailed() {
        mediaPlayer?.stop()
        if (isCallFailed) {
            if (callTimer != null)
                callTimer!!.cancel()

            if (timer != null) {
                timer!!.cancel()
                timer!!.purge()
                timer = null
            }
            onCallHungUp(JSONObject(), false)
            /*fuguCallActivity?.runOnUiThread {
                fuguCallActivity?.onCallFailed()
//                fuguCallActivity?.stopForegroundService(false)
//                stopSelf()
//                fuguCallActivity?.unbindServiceConnection()
//                fuguCallActivity?.stopVideoAudio()
//                fuguCallActivity?.onCallHungUp(null, false)
            }*/
        }
    }


    var handle: Handler = Handler()
    var runnable: Runnable? = null

    var handleConenct: Handler = Handler()
    var runnableConnect: Runnable? = null

    private fun disconnectRunnable(): Runnable {
        return Runnable {
            disconnectView()
        }
    }

    private fun connectRunnable(): Runnable {
        return Runnable {
            connectView()
        }
    }

    fun onDisconnected() {
        runnable = disconnectRunnable()
        handle.postDelayed(runnable!!, 1000)
    }

    private fun disconnectView() {
        try {
            if (isCallFailed) {
                var aa = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                if (videoCallModel?.callType.equals("VIDEO")) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.busy_tone)
                } else {
                    mediaPlayer = MediaPlayer.create(this, R.raw.busy_tone, aa, 1)
                }
                mediaPlayer?.setLooping(true)
                mediaPlayer?.start()

                fuguCallActivity?.onCallDisconnectEvent()
            }
        } catch (e: Exception) {
        }
    }

    private fun connectView() {
        mediaPlayer?.stop()
        fuguCallActivity?.onCallConnectEvent()
    }

    fun onConnected() {
        if (runnable != null) {
            handle.removeCallbacks(runnable!!)
        }

        runnableConnect = connectRunnable()
        handleConenct.postDelayed(runnableConnect!!, 1000)
//        mediaPlayer?.stop()
//        fuguCallActivity?.onCallConnectEvent()
    }


    override fun onCallRejected(jsonObject: JSONObject?) {
        if (callTimer != null)
            callTimer?.cancel()

        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
        fuguCallActivity?.runOnUiThread {
            fuguCallActivity?.onCallRejected(jsonObject)
            if (callDisconnectTime != null) {
                callDisconnectTime?.cancel()
            }
        }

    }

    override fun onUserBusyRecieved(jsonObject: JSONObject?) {
        fuguCallActivity?.onUserBusyRecieved(jsonObject)
    }

    override fun onErrorRecieved(error: String?) {
        fuguCallActivity?.onErrorRecieved(error)
    }

    override fun onAddStream(mediaStream: MediaStream?) {
        fuguCallActivity?.onAddStream(mediaStream)
    }

    override fun onIceCandidate(iceCandidate: IceCandidate?) {
        onIceCandidate(iceCandidate)
    }


    override fun onVideoOfferScreenSharingRecieved(jsonObject: JSONObject?) {

        fuguCallActivity?.onVideoOfferScreenSharingRecieved(jsonObject)
    }

    fun getConnectionModel(): Connection? {
        return connection
    }

    fun getSignal(): Signal? {
        return signal
    }

    fun getVideoModel(): VideoCallModel? {
        return videoCallModel
    }

    fun createPeerConnectionFactory(rootEglBase: EglBase?): PeerConnectionFactory? {
        if (peerConnectionFactory == null) {

            val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)
            val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(

                rootEglBase?.eglBaseContext, /* enableIntelVp8Encoder */true, /* enableH264HighProfile */true
            )
            val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
            val options = PeerConnectionFactoryOptions()//PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory()
            return peerConnectionFactory
        } else {
            return peerConnectionFactory
        }

    }

    fun setRemoteStream(mediaStream: MediaStream?) {
        this.remoteVideoStream = mediaStream
    }

    fun setEgl(eglBase: EglBase?) {
        this.rootEglBase = eglBase
    }

    fun getEgl(): EglBase? {
        return rootEglBase
    }

    fun onBroadCastrecieved(intent: Intent) {
        webRTCSignallingClient?.onBroadcastRecieved(intent)
    }

    override fun onFayeConnected() {
        fuguCallActivity?.onFayeConnected()
    }


    fun foregrounded(): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                || appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
    }


    fun onHungupSent() {
        fuguCallActivity?.onHungupSent()
    }

    fun cancelCallDisconnectTimer() {
        callDisconnectTime?.cancel()
    }

    fun cancelStartCallTimer() {
        webRTCSignallingClient?.cancelCounter()
    }

    fun reInitSocket(flag: Boolean) {
        //if(!flag && webRTCSignallingClient?.isConnected()!!)
        webRTCSignallingClient?.setUpFayeConnection()
    }

    fun cancelCalltimer() {
        callTimer?.cancel()

        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
    }

    fun restartTimer() {
        try {
            if (callDisconnectTime != null) {
                callDisconnectTime?.cancel()
                callDisconnectTime = null
                callDisconnectTime = object : CountDownTimer(30000, 1000) {

                    override fun onTick(millisUntilFinished: Long) {}

                    override fun onFinish() {
                        hungUpCall()
                        fuguCallActivity?.onCallHungUp(null, false)
                    }
                }.start()
            }
        } catch (e: Exception) {
        }
    }

    fun sendCustomData(jsonObject: String) {
        try {
            if(fuguCallActivity != null) {
                fuguCallActivity?.publishMessage(jsonObject)
            } else {
                val intent = Intent()
                intent.putExtra("HIPPO_CALL_ACTION_PAYLOAD", "CUSTOM_DATA")
                intent.putExtra("data", jsonObject)
                intent.action = "HIPPO_CALL_ACTION_SELECTED"
                sendBroadcast(intent)
            }
        } catch (e: Exception) {
        }
    }

    var screenStatus: String = ""
    var isMute: Boolean = false
    var isVideoPause: Boolean = false
    var isCameraClosed: Boolean = false

    fun sendUserAction(jsonObject: JSONObject) {
        screenStatus = ""
        isMute = false
        isVideoPause = false
        isCameraClosed = false
        val stringAttributes = HippoCallConfig.getInstance().getStringAttributes()

        if (jsonObject.optBoolean("is_mute")) {
            isMute = true
            screenStatus = stringAttributes.muteString
        }
        if (jsonObject.optBoolean("is_camera_closed")) {
            isCameraClosed = true
            if(!TextUtils.isEmpty(screenStatus)) {
                screenStatus = "$screenStatus ${stringAttributes.andString} ${stringAttributes.cameraOffString}"
            } else {
                screenStatus = stringAttributes.cameraOffString
            }
        }
        if(jsonObject.optBoolean("is_video_paused")) {
            isVideoPause = true
            screenStatus = stringAttributes.videoPaused
        }
        if(stringAttributes.isShowUserName &&  !TextUtils.isEmpty(screenStatus) && !TextUtils.isEmpty(getVideoModel()?.fullName))
            screenStatus = getVideoModel()?.fullName+" "+screenStatus

        fuguCallActivity?.setUserAction(screenStatus)
        fuguCallActivity?.chechScreenAction()
    }

    class MyTimerTask(var timerUpdate: TimerUpdate) : TimerTask() {
        override fun run() {
            timerUpdate.update()
        }
    }

    @Subscribe
    public fun onFayeMessage(events: FayeMessage) {
        when(events.type) {
            FuguAppConstant.FayeBusEvent.MESSAGE_RECEIVED.toString()            -> webRTCSignallingClient?.onReceivedMessage(events.message, events.channelId)
            BusEvents.DISCONNECTED_SERVER.toString()                            -> webRTCSignallingClient?.onDisconnectedServer()
            BusEvents.PONG_RECEIVED.toString()                                  -> webRTCSignallingClient?.onPongReceived()
            BusEvents.WEBSOCKET_ERROR.toString()                                -> webRTCSignallingClient?.onWebSocketError()
            BusEvents.ERROR_RECEIVED.toString()                                 -> webRTCSignallingClient?.onErrorReceived(events.message, events.channelId)
        }
    }
}