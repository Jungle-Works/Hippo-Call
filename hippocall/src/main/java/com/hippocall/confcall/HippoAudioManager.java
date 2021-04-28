package com.hippocall.confcall;

/**
 * Created by gurmail on 2020-04-21.
 *
 * @author gurmail
 */

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.utils.Utils;
import com.hippocall.R;

public class HippoAudioManager {

    @SuppressWarnings("unused")
    private static final String TAG = HippoAudioManager.class.getSimpleName();

    private final Context        context;
    private final IncomingRinger incomingRinger;
    private final OutgoingRinger outgoingRinger;

    private final SoundPool soundPool;
    private final int       connectedSoundId;
    private final int       disconnectedSoundId;

    private static HippoAudioManager instance = null;

    public static synchronized HippoAudioManager getInstance(Context context) {
        if(instance == null) {
            synchronized (HippoAudioManager.class) {
                if(instance == null) {
                    instance = new HippoAudioManager(context);
                }
            }
        }
        return instance;
    }

    public void distroyAudioManager() {
        if(instance != null)
            instance = null;
    }

    private HippoAudioManager(Context context) {
        this.context             = context.getApplicationContext();
        this.incomingRinger      = new IncomingRinger(context);
        this.outgoingRinger      = new OutgoingRinger(context);
        this.soundPool           = new SoundPool(1, AudioManager.STREAM_VOICE_CALL, 0);

        this.connectedSoundId    = this.soundPool.load(context, R.raw.webrtc_completed, 1);
        this.disconnectedSoundId = this.soundPool.load(context, R.raw.disconnet_call, 1);
    }

    public void initializeAudioForCall() {
        AudioManager audioManager = Utils.getAudioManager(context);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
    }

    public void startIncomingRinger() {
        initializeAudioForCall();
        Uri ringtone = getCallNotificationRingtone(context);
        startIncomingRinger(ringtone, isCallNotificationVibrateEnabled(context));

    }

    private boolean isCallNotificationVibrateEnabled(Context context) {
        boolean defaultValue = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            defaultValue = (Settings.System.getInt(context.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, 1) == 1);
        }

        return defaultValue;
    }

    private @NonNull Uri getCallNotificationRingtone(Context context) {
        String result = Settings.System.DEFAULT_RINGTONE_URI.toString();

        if (result != null && result.startsWith("file:")) {
            result = Settings.System.DEFAULT_RINGTONE_URI.toString();
        }

        return Uri.parse(result);
    }

    public void startIncomingRinger(@Nullable Uri ringtoneUri, boolean vibrate) {
        AudioManager audioManager = Utils.getAudioManager(context);
        boolean      speaker      = !audioManager.isWiredHeadsetOn() && !audioManager.isBluetoothScoOn();

        audioManager.setMode(AudioManager.MODE_RINGTONE);
        audioManager.setMicrophoneMute(false);
        audioManager.setSpeakerphoneOn(speaker);

        incomingRinger.start(ringtoneUri, vibrate);
    }

    public void startOutgoingRinger(OutgoingRinger.Type type) {
        AudioManager audioManager = Utils.getAudioManager(context);
        audioManager.setMicrophoneMute(false);

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        outgoingRinger.start(type);
    }

    public void silenceIncomingRinger() {
        incomingRinger.stop();
    }

    public void startCommunication(boolean preserveSpeakerphone) {
        AudioManager audioManager = Utils.getAudioManager(context);

        incomingRinger.stop();
        outgoingRinger.stop();

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        if (!preserveSpeakerphone) {
            audioManager.setSpeakerphoneOn(false);
        }

        soundPool.play(connectedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public void stop(boolean playDisconnected) {
        try {
            AudioManager audioManager = Utils.getAudioManager(context);

            incomingRinger.stop();
            outgoingRinger.stop();

            if (playDisconnected) {
                soundPool.play(disconnectedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
            }

            if (audioManager.isBluetoothScoOn()) {
                audioManager.setBluetoothScoOn(false);
                audioManager.stopBluetoothSco();
            }

            audioManager.setSpeakerphoneOn(false);
            audioManager.setMicrophoneMute(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.abandonAudioFocus(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
