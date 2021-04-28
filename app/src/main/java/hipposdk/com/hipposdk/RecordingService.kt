package hipposdk.com.hipposdk

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import android.util.Log
import java.io.IOException

/**
 * Created by gurmail on 11/04/19.
 * @author gurmail
 */
class RecordingService: Service() {

    var mBinder: IBinder = LocalBinder()
    private var intent: Intent? = null

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }
    inner class LocalBinder : Binder() {
        val serverInstance: RecordingService
            get() = this@RecordingService
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startRec(intent: Intent, flags: Int, mProjectionManager: MediaProjectionManager?, mScreenDensity: Int) {
        shareScreen(intent, flags, mProjectionManager, mScreenDensity)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        this.intent = intent

//        if (intent.getStringExtra("status").equals("done")) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                initRecorder()
//                shareScreen(intent)
//            }
//        }

        if(mMediaRecorder == null)
            initRecorder()

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

        val hungupIntent = Intent(this, MainActivity::class.java)
        hungupIntent.action = Intent.ACTION_DELETE
        hungupIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val hungupPendingIntent = PendingIntent.getActivity(this, 0,
                hungupIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = createNotificationChannel("VideoCall", "CHANNEL_NAME")

            val notification = NotificationCompat.Builder(this, channelId);
            notification.setContentTitle("Title")
            notification.setContentText("contentText")
            notification.setSmallIcon(R.mipmap.ic_launcher)
            notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher))
            notification.setContentIntent(pendingIntent)
            notification.setOngoing(true)
            notification.setPriority(getPriority())
            val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent)
                    .build()
            notification.addAction(hangupAction)

            startForeground(1122, notification.build())
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(NotificationChannel("VideoCall",
                        "VideoCall", NotificationManager.IMPORTANCE_HIGH))
            }
            notificationManager.notify(1122, notification.build())
        } else {
            val notification = NotificationCompat.Builder(this);
            notification.setContentTitle("setContentTitle")
            notification.setContentText("setContentText")
            notification.setSmallIcon(R.mipmap.ic_launcher)
            notification.setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher))
            notification.setContentIntent(pendingIntent)
            notification.setOngoing(true)
            notification.setPriority(getPriority())
            val hangupAction = NotificationCompat.Action.Builder(
                    android.R.drawable.sym_action_chat, "HANG UP", hungupPendingIntent)
                    .build()
            notification.addAction(hangupAction)
            startForeground(1122, notification.build())
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(1122, notification.build())
        }

        return Service.START_STICKY
    }

    private fun getPriority(): Int {
        val priority: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            priority = NotificationManager.IMPORTANCE_HIGH
        } else {
            priority = Notification.PRIORITY_MAX
        }
        return priority
    }

    private val DISPLAY_WIDTH = 720
    private val DISPLAY_HEIGHT = 1280
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaProjectionCallback: MediaProjectionCallback? = null
    private var mMediaRecorder: MediaRecorder? = null

//    static
//    {
//        ORIENTATIONS.append(Surface.ROTATION_0, 90);
//        ORIENTATIONS.append(Surface.ROTATION_90, 0);
//        ORIENTATIONS.append(Surface.ROTATION_180, 270);
//        ORIENTATIONS.append(Surface.ROTATION_270, 180);
//    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun shareScreen(intent: Intent, flags: Int, mProjectionManager: MediaProjectionManager?, mScreenDensity: Int) {
        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mProjectionManager?.getMediaProjection(flags, intent)
        mMediaProjection?.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay(mScreenDensity)
        mMediaRecorder?.start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createVirtualDisplay(mScreenDensity: Int): VirtualDisplay? {
        return mMediaProjection?.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder?.getSurface(), null, null
                /*Handler*/)/*Callbacks*/
    }

    private fun initRecorder() {
        try {
            mMediaRecorder = MediaRecorder()
            mMediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder?.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mMediaRecorder?.setOutputFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/video.mp4")
            mMediaRecorder?.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mMediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder?.setVideoEncodingBitRate(512 * 1000)
            mMediaRecorder?.setVideoFrameRate(30)
//            val rotation = getWindowManager().getDefaultDisplay().getRotation()
//            val orientation = ORIENTATIONS.get(rotation + 90)
//            mMediaRecorder.setOrientationHint(orientation)
            mMediaRecorder?.prepare()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            mMediaRecorder?.stop()
            mMediaRecorder?.reset()
            mMediaProjection = null
            //stopScreenSharing()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScreenSharing() {
        mMediaRecorder?.stop()
        mMediaRecorder?.reset()

        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay?.release()
        destroyMediaProjection()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection?.stop()
            mMediaProjection = null
        }
        Log.i("TAG", "MediaProjection Stopped")
    }


}