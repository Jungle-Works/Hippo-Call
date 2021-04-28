package com.hippocall.confcall

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.hippo.BuildConfig
import com.hippo.HippoConfig
import com.hippo.constant.FuguAppConstant
import com.hippo.constant.FuguAppConstant.*
import com.hippo.eventbus.BusProvider
import com.hippo.langs.Restring
import com.hippo.service.FuguPushIntentService
import com.hippo.utils.HippoLog
import com.hippo.utils.UniqueIMEIID
import com.hippo.utils.Utils
import com.hippocall.*
import com.hippocall.confcall.OngoingCallService.NotificationServiceState.channelId
import com.hippocall.confcall.OngoingCallService.NotificationServiceState.inviteLink
import com.hippocall.confcall.OngoingCallService.NotificationServiceState.isConferenceConnected
import com.hippocall.confcall.OngoingCallService.NotificationServiceState.isConferenceServiceRunning
import com.hippocall.WebRTCCallConstants.Companion.CALL_TYPE
import com.hippocall.WebRTCCallConstants.Companion.DEVICE_PAYLOAD
import com.hippocall.WebRTCCallConstants.Companion.FULL_NAME
import com.hippocall.WebRTCCallConstants.Companion.IS_SILENT
import com.hippocall.WebRTCCallConstants.Companion.MESSAGE_TYPE
import com.hippocall.WebRTCCallConstants.Companion.MESSAGE_UNIQUE_ID
import com.hippocall.WebRTCCallConstants.Companion.USER_ID
import com.hippocall.WebRTCCallConstants.Companion.USER_THUMBNAIL_IMAGE
import com.hippocall.confcall.OngoingCallService.NotificationServiceState.hasGroupCall
import com.hippocall.model.FragmentFlow
import faye.ConnectionManager
import org.json.JSONObject

class OngoingCallService : Service(), WebRTCCallConstants {
    private var mBinder: IBinder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    object NotificationServiceState {
        var isConferenceServiceRunning = false
        var isConferenceConnected = false
        var inviteLink = ""
        var channelId = -1L
        var muid = ""
        var transactionId = ""
        var hasGroupCall = false
    }

    object CallState {
        var muid = ""
        var readyToConnect = 0
    }

    inner class LocalBinder : Binder() {
        val serverInstance: OngoingCallService
            get() = this@OngoingCallService
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            var notificationChannel = NotificationChannel("IncomingCall", "IncomingCall", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        LocalBroadcastManager.getInstance(this@OngoingCallService).registerReceiver(mCallTerminated, IntentFilter("CALL TERMINATED"))
        LocalBroadcastManager.getInstance(this@OngoingCallService).registerReceiver(mInternetIssue, IntentFilter("INTERNET_ISSUE"))

    }


    private val mInternetIssue = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var mediaPlayer: MediaPlayer? = null
            var aa = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            mediaPlayer = MediaPlayer.create(this@OngoingCallService, R.raw.busy_tone)
            mediaPlayer?.setLooping(true)
            mediaPlayer?.start()

            Handler(Looper.getMainLooper()).postDelayed({
                mediaPlayer?.stop()
            }, 3000)

        }
    }

    private val mCallTerminated = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            HippoLog.e("CALL_TERMINATED", "CALL_TERMINATED")
            val userId = com.hippo.database.CommonData.getUserDetails().data.userId
            val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName

            val startCallJson = JSONObject()
            startCallJson.put(IS_SILENT, true)
            startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.JitsiCallType.HUNGUP_CONFERENCE.toString())
            startCallJson.put(USER_ID, userId)
            startCallJson.put(CHANNEL_ID, channelId)
            startCallJson.put(MESSAGE_TYPE, WebRTCCallConstants.VIDEO_CALL)
            startCallJson.put(CALL_TYPE, "VIDEO")
            startCallJson.put(DEVICE_PAYLOAD, getDeviceDetails())
            startCallJson.put(INVITE_LINK, inviteLink)
            startCallJson.put(MESSAGE_UNIQUE_ID, NotificationServiceState.muid)
            startCallJson.put(FULL_NAME, fullName)
            startCallJson.put("message", "")
            startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
            if (!hasGroupCall)
                HippoCallConfig.getInstance().sendMessage(channelId, startCallJson)

            isConferenceServiceRunning = false
            isConferenceConnected = false
            hasGroupCall = false
            stopSelf()
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isConferenceServiceRunning = true
        if (intent?.hasExtra(INCOMING_VIDEO_CONFERENCE)!!) {
            createIncomingCallNotification(intent)
            startTimerTask()
        } else {
            stopTimer()
            createOngoingCallNotification(intent)
        }

        return START_STICKY
    }

    private fun createIncomingCallNotification(intent: Intent) {
        val notificationIntent = Intent(this, MainCallingActivity::class.java)
        val customView = RemoteViews(packageName, R.layout.cutom_call_notification)
        customView.setTextViewText(R.id.name, getString(R.string.app_name))
        customView.setTextViewText(R.id.btnDecline, Restring.getString(this, R.string.hippo_reject))
        customView.setTextViewText(R.id.btnAnswer, Restring.getString(this, R.string.hippo_answer))
        notificationIntent.putExtra("room_name", intent?.getStringExtra("room_name"))
        inviteLink = intent?.getStringExtra(INVITE_LINK).toString()
        channelId = intent?.getLongExtra(CHANNEL_ID, -1L)
        var hasGroupCall: Boolean = false
        try {
            notificationIntent.putExtra("incomming_call", "incomming_call")
            val videoCallModel = intent.extras?.getParcelable<VideoCallModel>("videoCallModel")!! as VideoCallModel
            notificationIntent.putExtra("videoCallModel", videoCallModel)
            if (videoCallModel?.hasGroupCall!!) {
                hasGroupCall = true
            }
        } catch (e: Exception) {
        }

        notificationIntent.putExtra(INVITE_LINK, inviteLink)
        notificationIntent.putExtra(CHANNEL_ID, channelId)
        notificationIntent.putExtra(FULL_NAME, intent.getStringExtra(FULL_NAME))
        notificationIntent.putExtra(USER_THUMBNAIL_IMAGE, intent.getStringExtra(USER_THUMBNAIL_IMAGE))
        notificationIntent.putExtra(CALL_TYPE, intent.getStringExtra(CALL_TYPE))
        notificationIntent.putExtra(MESSAGE_UNIQUE_ID, intent.getStringExtra(MESSAGE_UNIQUE_ID))
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        val hungupIntent = Intent(this, HungUpBroadcast::class.java)
        hungupIntent.putExtra("action", "rejectCall")
        hungupIntent.putExtra(INVITE_LINK, inviteLink)
        hungupIntent.putExtra(CHANNEL_ID, channelId)
        hungupIntent.putExtra("has_group_call", hasGroupCall)
        hungupIntent.putExtra(MESSAGE_UNIQUE_ID, intent.getStringExtra(MESSAGE_UNIQUE_ID))
        hungupIntent.putExtra(DEVICE_PAYLOAD, getDeviceDetails().toString())

        val answerIntent = Intent(this, MainCallingActivity::class.java)
        answerIntent.action = Intent.ACTION_ANSWER
        answerIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        answerIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        answerIntent.putExtra("room_name", intent?.getStringExtra("room_name"))
        answerIntent.putExtra("call_type", intent?.getStringExtra("call_type"))
        answerIntent.putExtra(MESSAGE_UNIQUE_ID, intent.getStringExtra(MESSAGE_UNIQUE_ID))
        answerIntent.putExtra("answer_call", "answer_call")
        hungupIntent.putExtra("", hasGroupCall)
        answerIntent.putExtra(INVITE_LINK, inviteLink)
        answerIntent.putExtra(CHANNEL_ID, channelId)

        try {
            val videoCallModel = intent.extras?.getParcelable<VideoCallModel>("videoCallModel")!! as VideoCallModel
            answerIntent.putExtra("videoCallModel", videoCallModel)
        } catch (e: Exception) {
        }

        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val hungupPendingIntent = PendingIntent.getBroadcast(this, 0,
                hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val answerPendingIntent = PendingIntent.getActivity(this, 0,
                answerIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val name: String = intent?.getStringExtra(FULL_NAME).toString()
        customView.setTextViewText(R.id.name, intent?.getStringExtra(FULL_NAME))
        if (intent?.getStringExtra(CALL_TYPE).equals("VIDEO")) {
            val incomingCall = Restring.getString(this, R.string.hippo_call_incoming_video_call)
            customView.setTextViewText(R.id.callType, incomingCall)
        } else {
            val incomingCall = Restring.getString(this, R.string.hippo_call_incoming_audio_call)
            customView.setTextViewText(R.id.callType, incomingCall)
        }
        //customView.setImageViewBitmap(R.id.photo, Utils.getCircleBitmap(NotificationImageManager().getImageBitmap(intent?.getStringExtra(USER_THUMBNAIL_IMAGE))))

        customView.setOnClickPendingIntent(R.id.btnAnswer, answerPendingIntent)
        customView.setOnClickPendingIntent(R.id.btnDecline, hungupPendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val incomingCall = Restring.getString(this, R.string.hippo_call_incoming)
            val notification = NotificationCompat.Builder(this, "IncomingCall")
            notification.setContentTitle(getString(R.string.app_name))
            notification.setTicker("Call_STATUS")
            notification.setContentText(incomingCall)
            notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
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
                notificationChannel.setSound(null, null)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        } else {

            val notification = NotificationCompat.Builder(this)
            notification.setContentTitle(getString(R.string.app_name))
            notification.setTicker("Call_STATUS")
            val incomingCall = Restring.getString(this, R.string.hippo_call_incoming)
            notification.setContentText(incomingCall)

            notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
            notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))
            notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            notification.setVibrate(null)
            notification.setContentIntent(pendingIntent)
            notification.setOngoing(true)
            notification.setCategory(NotificationCompat.CATEGORY_CALL)
            notification.priority = getPriority()
            val hungup = Restring.getString(this, R.string.hippo_call_hungup)
            val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, hungup, hungupPendingIntent)
                    .build()
            notification.addAction(hangupAction)
            startForeground(1122, notification.build())
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    private fun createOngoingCallNotification(intent: Intent?) {

        try {
            inviteLink = intent?.getStringExtra(INVITE_LINK)!!
            channelId = intent?.getLongExtra(CHANNEL_ID, -1L)!!
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val notification: NotificationCompat.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = NotificationCompat.Builder(this, "NotificationService")
            notification.setContentTitle(getString(R.string.app_name))
            val outgoing = Restring.getString(this, R.string.hippo_call_outgoing)
            notification.setContentText(outgoing)
            notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
            notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))
            notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            notification.setVibrate(null)
            notification.setOngoing(true)
            notification.priority = getPriority()

            /*val notificationIntent = Intent(this, RejoinBroadcast::class.java)
            notificationIntent.putExtra("action", "openCall")
            notificationIntent.putExtra("from_calling_push", true)
            notificationIntent.putExtra(INVITE_LINK, inviteLink)

            val callPendingIntent = PendingIntent.getBroadcast(this, 1,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            notification.setContentIntent(callPendingIntent)*/

            val hungupIntent = Intent(this, HungUpBroadcast::class.java)
            hungupIntent.putExtra("action", "hungupCall")
            hungupIntent.putExtra(INVITE_LINK, inviteLink)
            hungupIntent.putExtra(CHANNEL_ID, channelId)
            hungupIntent.putExtra(MESSAGE_UNIQUE_ID, intent?.getStringExtra(MESSAGE_UNIQUE_ID))
            hungupIntent.putExtra(DEVICE_PAYLOAD, getDeviceDetails().toString())

            val hungupPendingIntent = PendingIntent.getBroadcast(this, 0,
                    hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val hungup = Restring.getString(this, R.string.hippo_call_hungup)
            val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, hungup, hungupPendingIntent)
                    .build()
            notification.addAction(hangupAction)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel("NotificationService",
                        "NotificationService", NotificationManager.IMPORTANCE_LOW)
                notificationChannel.setSound(null, null)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            startForeground(1124, notification.build())

        } else {
            val outgoning = Restring.getString(this, R.string.hippo_call_outgoing)
            notification = NotificationCompat.Builder(this)
            notification.setContentTitle(getString(R.string.app_name))
            notification.setContentText(outgoning)
            notification.setSmallIcon(HippoCallConfig.getInstance().hippoCallPushIcon)
            notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, HippoCallConfig.getInstance().hippoCallPushIcon))
            notification.setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
            notification.setVibrate(null)
            notification.setOngoing(true)

            /* val notificationIntent = Intent(this, CallingPushService::class.java)
             notificationIntent.putExtra("from_calling_push", true)
             notificationIntent.putExtra(INVITE_LINK, inviteLink)

             val callPendingIntent = PendingIntent.getBroadcast(this, 0,
                 notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

             notification.setContentIntent(callPendingIntent)*/


            val hungupIntent = Intent(this, HungUpBroadcast::class.java)
            hungupIntent.putExtra("action", "hungupCall")
            hungupIntent.putExtra(INVITE_LINK, inviteLink)
            hungupIntent.putExtra(CHANNEL_ID, channelId)
            hungupIntent.putExtra(MESSAGE_UNIQUE_ID, intent?.getStringExtra(MESSAGE_UNIQUE_ID))
            hungupIntent.putExtra(DEVICE_PAYLOAD, getDeviceDetails().toString())

            val hungupPendingIntent = PendingIntent.getBroadcast(this, 0,
                    hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val hungup = Restring.getString(this, R.string.hippo_call_hungup)
            val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, hungup, hungupPendingIntent)
                    .build()
            notification.addAction(hangupAction)

            notification.priority = getPriority()
            startForeground(1124, notification.build())
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //notificationManager.notify(1124, notification.build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConferenceServiceRunning = false
        isConferenceConnected = false
        stopTimer()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCallTerminated)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mInternetIssue)
    }

    private fun getPriority(): Int {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            Notification.PRIORITY_MAX
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

    var callDisconnectTime: CountDownTimer? = null
    private fun startTimerTask() {
        if (callDisconnectTime == null) {
            callDisconnectTime = object : CountDownTimer(64000, 2000) {
                override fun onTick(millisUntilFinished: Long) {}

                override fun onFinish() {
                    OngoingCallService.NotificationServiceState.isConferenceServiceRunning = false
                    OngoingCallService.NotificationServiceState.isConferenceConnected = false
                    OngoingCallService.NotificationServiceState.transactionId = ""
                    OngoingCallService.NotificationServiceState.inviteLink = ""
                    OngoingCallService.NotificationServiceState.muid = ""
                    OngoingCallService.NotificationServiceState.hasGroupCall = false
                    OngoingCallService.NotificationServiceState.channelId = -1L
                    HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false)
                    BusProvider.getInstance().post(
                            FragmentFlow(WebRTCCallConstants.BusFragmentType.UPDATE_INCOMIMG_CONFIG.toString(),
                                    2, null, "")
                    )
                    stopSelf()
                }
            }.start()
        }
    }

    private fun stopTimer() {
        if (callDisconnectTime != null) {
            callDisconnectTime?.cancel()
            callDisconnectTime = null
        }
    }

    /*private fun answerGroupCall() {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        //val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.JOIN_GROUP_CALL.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, inviteLink)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", fullName)

        sendMessage(videoCallModel?.channelId!!, startCallJson)
        sendMessageOnUserChannel("/"+com.hippo.database.CommonData.getUserDetails().data.userChannel)
    }

    private fun sendMessageOnUserChannel(channelIdd: String) {
        val userId = com.hippo.database.CommonData.getUserDetails().data.userId
        //val fullName = com.hippo.database.CommonData.getUserDetails().data.fullName
        val startCallJson = JSONObject()
        startCallJson.put(FuguAppConstant.IS_SILENT, true)
        startCallJson.put(WebRTCCallConstants.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.JOIN_GROUP_CALL.toString())
        startCallJson.put(FuguAppConstant.USER_ID, userId)
        startCallJson.put(FuguAppConstant.CHANNEL_ID, channelId)
        startCallJson.put(FuguAppConstant.MESSAGE_TYPE, WebRTCCallConstants.GROUP_CALL)
        startCallJson.put(WebRTCCallConstants.CALL_TYPE, "VIDEO")
        startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, OngoingCallService.NotificationServiceState.muid)
        startCallJson.put(WebRTCCallConstants.DEVICE_PAYLOAD, getDeviceDetails())
        startCallJson.put(FuguAppConstant.INVITE_LINK, inviteLink)
        startCallJson.put(FuguAppConstant.JITSI_URL, inviteLink)
        startCallJson.put("message", "")
        startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE)
        startCallJson.put("user_type", FuguAppConstant.ANDROID_USER)
        startCallJson.put("full_name", fullName)
        startCallJson.put("server_push", true)
        ConnectionManager.publish(channelIdd, startCallJson)
    }*/

}