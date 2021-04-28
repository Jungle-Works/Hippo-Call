package com.hippocall.confcall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hippo.constant.FuguAppConstant
import com.hippo.langs.Restring
import com.hippocall.HippoCallConfig
import com.hippocall.R
import org.jitsi.meet.sdk.JitsiMeetActivity

/**
 * Created by gurmail on 2020-07-24.
 * @author gurmail
 */
class RejoinBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent?.getStringExtra("action").equals("openCall")) {
            JitsiMeetActivity.launch(HippoCallConfig.getInstance().context, intent?.getStringExtra(FuguAppConstant.INVITE_LINK), Restring.getString(HippoCallConfig.getInstance().context, R.string.hippo_calling_connection))
        }
    }
}