package com.hippocall.confcall;

import android.app.IntentService;
import android.content.Intent;

import com.hippo.langs.Restring;
import com.hippocall.R;

import org.jitsi.meet.sdk.JitsiMeetActivity;

import faye.ConnectionUtils;

/**
 * Created by gurmail on 2020-07-24.
 *
 * @author gurmail
 */
public class CallingPushService extends IntentService {

    private static final String TAG = "FuguPushIntentService";

    public CallingPushService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean isCallingPush = intent.getBooleanExtra("from_calling_push", false);
        if(isCallingPush) {
            String link = intent.getStringExtra("invite_link");
            if(ConnectionUtils.INSTANCE.isAppRunning(this)) {

            }
            JitsiMeetActivity.launch(this, link, Restring.getString(this, R.string.hippo_calling_connection));
        }
    }
}


/*if(!ConnectionUtils.INSTANCE.isAppRunning(context)) {*/