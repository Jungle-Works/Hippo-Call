package hipposdk.com.hipposdk

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.hippo.utils.HippoLog
import java.util.concurrent.TimeUnit

/**
 * Created by gurmail on 06/06/19.
 * @author gurmail
 */
class CallBroadCastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val payload = intent?.getStringExtra("HIPPO_CALL_ACTION_PAYLOAD")
        Toast.makeText(context, payload, Toast.LENGTH_LONG).show()
        Log.i("edddddddd","gwyfhfgwefguwgufuwuwhefh  ~~~~~~> $payload")
//        if(payload.equals("callEnded", ignoreCase = true)) {
//            Log.i("edddddddd","gwyfhfgwefguwgufuwuwhefh  ~~~~~~> $payload")
//            Toast.makeText(context, "edddddddd", Toast.LENGTH_LONG).show()
//        }
        when(payload) {
            "need_fragment" -> {
                updateFragment()
            }
            "callEnded" -> {
                Toast.makeText(context, "call Ended", Toast.LENGTH_LONG).show()
            }
//            "call_connected" -> {
//                HippoCallConfig.getInstance().hasExtraView(true)
//                updateFragment()
//                //setTimer()
//                Toast.makeText(context, "call Connected", Toast.LENGTH_LONG).show()
//            }
            "CUSTOM_DATA" -> {
//                val data = intent?.getStringExtra("data")
//                Log.i("edddddddd","data  ~~~~~~> $data")

            }
            "call_reconnected" -> {
                Toast.makeText(context, "call Re-Connected", Toast.LENGTH_LONG).show()
            }
            "call_connectivity_issues" -> {
                Toast.makeText(context, "call connectivity issues", Toast.LENGTH_LONG).show()
            }
            "UI_ACTION" -> {
                HippoLog.e("DATA", "data = "+intent?.getStringExtra("button_data"))
            }
        }
    }

    /*private fun setTimer() {
        Handler(Looper.getMainLooper()).postDelayed({
            var countDown: CountDownTimer = object : CountDownTimer(30000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    //HippoLog.e("Socket Connection Timer", ""+ TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished));
                    HippoCallConfig.getInstance().setTimer(""+ TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished))
                }

                override fun onFinish() {
                    HippoCallConfig.getInstance().setTimer("Timesup")
                }
            }
            countDown.start()
        }, 0)

    }*/

    private fun updateFragment() {
        //HippoCallConfig.getInstance().setFragment(DemoFragment())
    }
}