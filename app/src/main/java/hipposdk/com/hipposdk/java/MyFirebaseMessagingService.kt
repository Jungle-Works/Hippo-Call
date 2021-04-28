package hipposdk.com.hipposdk.java

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hippo.HippoNotificationConfig
import android.util.Log
import hipposdk.com.hipposdk.R
import org.json.JSONObject


/**
 * Created by gurmail on 02/01/19.
 * @author gurmail
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onNewToken(tokan: String) {
        super.onNewToken(tokan)

    }

    private fun getPriority(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_DEFAULT
        } else {
            Notification.PRIORITY_MAX
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        try {
            Log.e("TAG", "onMessageReceived Payload: " + remoteMessage?.data.toString())
        } catch (e: Exception) {
        }

        val fuguNotificationConfig = HippoNotificationConfig()
        if (fuguNotificationConfig.isHippoNotification(remoteMessage?.getData())) {
            fuguNotificationConfig.setSmallIcon(R.mipmap.ic_launcher)
            fuguNotificationConfig.setPriority(getPriority())
            fuguNotificationConfig.setNotificationSoundEnabled(true)
            fuguNotificationConfig.showNotification(applicationContext, remoteMessage?.getData())
        } else {
            // Your logic goes here
        }
    }

}