package com.hippocall;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hippo.HippoConfig;
import com.hippo.model.FuguDeviceDetails;
import com.hippo.utils.HippoLog;
import io.paperdb.Paper;

import java.util.HashMap;
import java.util.TreeMap;

import static com.hippo.constant.FuguAppConstant.HIPPO_PAPER_NAME;

/**
 * Created by gurmail on 21/02/19.
 *
 * @author gurmail
 */
public final class CommonData {

    static String PAPER_CALL_STATUS = "hippo_call_status";
    static String PAPER_VIDEO_CALL = "hippo_video_call_cred";
    static String PAPER_CALL_TYPE = "hippo_call_type";
    static String PAPER_USER_ID = "hippo_user_id";
    static String PAPER_CHANEL_ID = "hippo_channel_id";
    private static final String VIDEO_CALL_MODEL = "video_call_model_new";

    public static String getCallStatus() {
        return Paper.book(HIPPO_PAPER_NAME).read(PAPER_CALL_STATUS);
    }

    public static String getCallType() {
        return Paper.book(HIPPO_PAPER_NAME).read(PAPER_CALL_TYPE);
    }

    public static JsonObject deviceDetails(Context context) {
        Gson gson = new GsonBuilder().create();
        JsonObject deviceDetailsJson = null;
        try {
            deviceDetailsJson = gson.toJsonTree(new FuguDeviceDetails(
                    getAppVersion(context)).getDeviceDetails()).getAsJsonObject();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return deviceDetailsJson;

    }

    public static void setCallStatus(String callStatus) {
        Paper.book(HIPPO_PAPER_NAME).write(PAPER_CALL_STATUS, callStatus);
    }

    public static void setCallAnswered(boolean b) {
        Paper.book(HIPPO_PAPER_NAME).write("hippo_call_answer", b);
    }

    public static boolean isCallAnswered() {
        return Paper.book(HIPPO_PAPER_NAME).read("hippo_call_answer", false);
    }

    public static void setCallType(String callType) {
        Paper.book(HIPPO_PAPER_NAME).write(PAPER_CALL_TYPE, callType);
    }

    public static Long getUserId() {
        return Long.parseLong(Paper.book(HIPPO_PAPER_NAME).read(PAPER_USER_ID));
    }

    public static int getAppVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getPackageName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getUniqueIMEIId(Context activity) {
        String android_id = "";
        try {
            try {
                android_id = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception e) {
                if(HippoConfig.DEBUG)
                    e.printStackTrace();
            }
            android_id = TextUtils.isEmpty(android_id) ? Build.SERIAL : android_id;
        } catch (Exception e) {
            if(HippoConfig.DEBUG)
                e.printStackTrace();
        }

        android_id = TextUtils.isEmpty(android_id) ? "12345" : android_id;

        try {
            android_id = android_id + CommonData.getPackageName(activity);
        } catch (Exception e) {

        }
        return android_id;
    }

    public static boolean hasExtraView() {
        return Paper.book(HIPPO_PAPER_NAME).read("hippo_call_extra_view", false);
    }

    public static void setExtraView(boolean b) {
        Paper.book(HIPPO_PAPER_NAME).write("hippo_call_extra_view", b);
    }

    public static HashMap<String, Long> CHANNEL_IDS = new HashMap<>();

    public static void setChannelIds(String transactionId, Long channelId) {
        try {
            CHANNEL_IDS =  getChannelId();
            if(CHANNEL_IDS.size()>2500)
                CHANNEL_IDS.clear();

            CHANNEL_IDS.put(transactionId, channelId);
            Paper.book(HIPPO_PAPER_NAME).write(PAPER_CHANEL_ID, CHANNEL_IDS);
        } catch (Exception e) {

        }
    }

    public static HashMap<String, Long> getChannelId() {
        try {
            return Paper.book(HIPPO_PAPER_NAME).read(PAPER_CHANEL_ID, new HashMap<>());
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static Long getChannelId(String transactionId) {
        try {
            CHANNEL_IDS =  getChannelId();
            return CHANNEL_IDS.get(transactionId);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setVideoCallModel(VideoCallModel videoCallModel) {
        if (videoCallModel == null) {
            Paper.book(HIPPO_PAPER_NAME).write(VIDEO_CALL_MODEL, "");
        } else {
            Paper.book(HIPPO_PAPER_NAME).write(VIDEO_CALL_MODEL, videoCallModel);
        }
    }

    public static VideoCallModel getVideoCallModel() {
        return Paper.book(HIPPO_PAPER_NAME).read(VIDEO_CALL_MODEL);
    }

    public static void setScreenActionString(StringAttributes actionString) {
        Paper.book(HIPPO_PAPER_NAME).write("hippo_screen_action", actionString);
    }


    public static StringAttributes getScreenActionString() {
        return Paper.book(HIPPO_PAPER_NAME).read("hippo_screen_action", null);
    }

    public static void setMirrorStatus(String key, boolean surfaceMirror) {
        Paper.book(HIPPO_PAPER_NAME).write(key, surfaceMirror);
    }

    public static boolean getMirroeStatus(String key) {
        return Paper.book(HIPPO_PAPER_NAME).read(key, false);
    }

    private static String PAPER_NOTIFICATION_IMAGES_MAP = "paper_notification_images_map";
    private static TreeMap<String, String> notificationImagesMap = new TreeMap<>();

    public static void setNotificationImagesMap(String link, String path) {
        try {
            notificationImagesMap = Paper.book().read(PAPER_NOTIFICATION_IMAGES_MAP);
            if (notificationImagesMap == null) {
                notificationImagesMap = new TreeMap<>();
            }
            notificationImagesMap.put(link, path);
            Paper.book().write(PAPER_NOTIFICATION_IMAGES_MAP, notificationImagesMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getNotificationImage(String link) {
        try {
            notificationImagesMap = Paper.book().read(PAPER_NOTIFICATION_IMAGES_MAP);
            return notificationImagesMap.get(link);
        } catch (Exception e) {
            return "";
        }
    }
}
