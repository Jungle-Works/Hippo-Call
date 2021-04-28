package com.hippocall;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.gson.Gson;
import com.hippo.CallData;
import com.hippo.FayeCallDate;
import com.hippo.HippoConfig;
import com.hippo.constant.FuguAppConstant;
import com.hippo.langs.Restring;
import com.hippo.model.FuguCreateConversationParams;
import com.hippo.utils.DateUtils;
import com.hippo.utils.HippoLog;
import com.hippo.utils.UniqueIMEIID;
import com.hippo.BuildConfig;
import com.hippocall.confcall.HippoAudioManager;
import com.hippocall.confcall.HungUpBroadcast;
import com.hippocall.confcall.OngoingCallService;
import com.hippocall.model.Message;

import faye.ConnectionManager;
import faye.ConnectionUtils;
import faye.FayeClient;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by gurmail on 04/01/19.
 *
 * @author gurmail
 */

public class HippoCallConfig implements CallData, FuguAppConstant, FayeCallDate {

    private static HippoCallConfig instance;
    private UpdateView updateView;
    private Boolean state = true;
    private StringAttributes actionString;
    private Context mContext;
    private Context context;
    private JSONObject jsonObject;
    boolean emitUserBusy;

    private int hippoCallPushIcon = R.drawable.hippo_default_notif_icon;

    public static final String LOCAL_SURFACE = "hippo_local_status";
    public static final String REMOTE_SURFACE = "hippo_remote_status";
    public static final String FLIP_CAMERA = "hippo_flip_camera";

    private String INCOMING_CALL = "incoming Call";
    private String jitsiURL = "";


    protected boolean isNetworkConnected() {
        return state;
    }

    private HippoCallConfig() {

    }

    public void hasExtraView(Boolean view) {
        CommonData.setExtraView(view);
    }

    public static HippoCallConfig getInstance() {
        if(instance == null) {
            synchronized (HippoCallConfig.class) {
                if(instance == null)
                    instance = new HippoCallConfig();
            }
        }
        return instance;
    }

    public String getJitsiURL() {
        if(TextUtils.isEmpty(jitsiURL)) {
            if(com.hippo.database.CommonData.getUserDetails() != null &&
                    com.hippo.database.CommonData.getUserDetails().getData() != null &&
                    !TextUtils.isEmpty(com.hippo.database.CommonData.getUserDetails().getData().getJitsiUrl())) {
                jitsiURL = com.hippo.database.CommonData.getUserDetails().getData().getJitsiUrl();
            }
        }
        return jitsiURL;
    }

    public Context getContext() {
        return this.mContext.getApplicationContext();
    }

    public void sendMessage(Long channelId, JSONObject jsonObject) {
        ConnectionManager.INSTANCE.publish("/"+channelId, jsonObject);
        //HippoConfig.getExistingClient(client -> client.publish("/"+channelId, jsonObject));
    }

    public void setScreenActionString(StringAttributes actionString) {
        this.actionString = actionString;
        CommonData.setScreenActionString(actionString);
    }

    public StringAttributes getStringAttributes() {
        if(actionString != null) {
            actionString = CommonData.getScreenActionString();
        }
        if(actionString ==null) {
            actionString = getScreenString();
        }
        return actionString;
    }

    protected void setTimmerListener(UpdateView updateView) {
        this.updateView = updateView;
    }

    public void setCallBackListener(Context context) {
        this.mContext = context;
        setCallBackListener();
    }

    public void setCallBackListener() {
        HippoConfig.getInstance().setCallListener(this);
        HippoConfig.getInstance().setFayeCallDate(this);
    }

    private void sendUserBusyBroadcast(Context context, long channelId, String messageUniqueId,
                                       String videoCallType, long userId) {
        Intent videoCallIntent = new Intent(VIDEO_CALL_INTENT);
        videoCallIntent.putExtra(CHANNEL_ID, channelId);
        videoCallIntent.putExtra(MESSAGE_UNIQUE_ID, messageUniqueId);
        videoCallIntent.putExtra(VIDEO_CALL_TYPE, videoCallType);
        videoCallIntent.putExtra(USER_ID, userId);
        LocalBroadcastManager.getInstance(context).sendBroadcast(videoCallIntent);
    }

    @Override
    public void onNotificationReceived(Context context, JSONObject data) {
        try {
            if(data.optInt("notification_type", 1) == 14) {
                onCallIncomming(context, data);
            } else {
                onStartConferenceCall(context, data);
            }
        } catch (Exception e) {
            if(HippoConfig.DEBUG)
                e.printStackTrace();
        }
    }

    @Override
    public void onConfNotificationReceived(Context context, JSONObject data) {
        try {
//            if(PowerManager.isIgnoringBatteryOptimizations()) {
//
//            }
            onStartConferenceCall(context, data);
        } catch (Exception e) {
            if(HippoConfig.DEBUG)
                e.printStackTrace();
        }
    }

    @Override
    public void networkStatus(int status) {
        this.state = status > 0;
        if(updateView != null) {
            HippoLog.e("TAG", ">>>>>>>>>>No internet connection<<<<<<<");
            updateView.onNetworkStatusChange(status);
        }
    }

    @Override
    public void onCallClick(Context context, int callType, Long channelId, Long userId, boolean isAgentFlow,
                            boolean isAllowCall, String fullname, String image, String myImagePath) {
        this.context = context;
        if(!isMyServiceRunning(context, OngoingCallService.class)) {
            onConfCallClick(context, callType, channelId, userId, isAgentFlow, isAllowCall, fullname, image, myImagePath);
        }
    }

    private void onConfCallClick(Context context, int callType, Long channelId, Long userId, boolean isAgentFlow,
                                 boolean isAllowCall, String fullname, String image, String myImagePath) {

        Intent videoIntent = new Intent(context.getApplicationContext(), MainCallingActivity.class);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        {
            Message turnCredentials = new AppContants().getTurnCredentials();
            String messageUniqueId = UUID.randomUUID().toString();

            String activityLaunchState = WebRTCCallConstants.AcitivityLaunchState.SELF.toString();

            String typeCall = WebRTCCallConstants.CallType.VIDEO.toString();
            if(callType == 2)
                typeCall = WebRTCCallConstants.CallType.AUDIO.toString();


            String myname = "";
            if(!isAgentFlow)
                myname = com.hippo.database.CommonData.getUpdatedDetails().getData().getFullName();

            VideoCallModel videoCallModel = new VideoCallModel(channelId,
                    image, fullname, userId, -1, fullname,
                    turnCredentials.getTurnApiKey(),
                    turnCredentials.getUsername(),
                    turnCredentials.getCredentials(),
                    (ArrayList<String>) (turnCredentials.getIceServers().getStun()),
                    (ArrayList<String>) (turnCredentials.getIceServers().getTurn()),
                    activityLaunchState,
                    messageUniqueId,
                    typeCall, "", "", "",
                    myname, myImagePath, false, false, false);
            videoIntent.putExtra("videoCallModel", videoCallModel);
            //videoIntent.putExtra("incomming_call", "incomming_call");
        }
        context.startActivity(videoIntent);
    }

    private Long timeInMillis(String timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        Long timeInMilliseconds = 0L;
        try {
            Date mDate = sdf.parse(DateUtils.getInstance().convertToLocal(timeStamp));
            timeInMilliseconds = mDate.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    private void sendBusyStatus(JSONObject jsonObject) {
        sendBusyStatus(jsonObject, 1);
    }
    private void sendBusyStatus(JSONObject jsonObject, int type) {
        try {
            Long userId = com.hippo.database.CommonData.getUserDetails().getData().getUserId();
            JSONObject startCallJson = new JSONObject();
            startCallJson.put(IS_SILENT, false);
            if(type == 1) {
                startCallJson.put(VIDEO_CALL_TYPE, FuguAppConstant.JitsiCallType.USER_BUSY_CONFERENCE);
                startCallJson.put(MESSAGE_TYPE, VIDEO_CALL);
            } else {
                startCallJson.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.REJECT_GROUP_CALL);
                startCallJson.put(MESSAGE_TYPE, 27);
            }
            startCallJson.put(USER_ID, userId);
            startCallJson.put(CHANNEL_ID, jsonObject.optString(CHANNEL_ID));

            startCallJson.put(WebRTCCallConstants.Companion.getCALL_TYPE(), "VIDEO");
            startCallJson.put(WebRTCCallConstants.Companion.getDEVICE_PAYLOAD(), getDeviceDetails(context));
            startCallJson.put(INVITE_LINK, jsonObject.optString(INVITE_LINK));
            startCallJson.put(MESSAGE_UNIQUE_ID, jsonObject.optString(MESSAGE_UNIQUE_ID));

            startCallJson.put(WebRTCCallConstants.Companion.getFULL_NAME(), jsonObject.optString("full_name", ""));
            startCallJson.put("message", "");
            startCallJson.put("is_typing", TYPING_SHOW_MESSAGE);
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER);

            //String channelId = "/"+jsonObject.optLong(CHANNEL_ID);
            sendMessage(jsonObject.optLong(CHANNEL_ID), startCallJson);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onStartConferenceCall(Context context, JSONObject jsonObject) throws Exception {
        HippoLog.v("TAG", "jsonObject = "+jsonObject.toString());
        this.context = context;
        this.jsonObject = jsonObject;

        if(jsonObject.has(VIDEO_CALL_TYPE)) {
            if(jsonObject.getString(VIDEO_CALL_TYPE).equals(FuguAppConstant.JitsiCallType.START_CONFERENCE.toString())) {
                if (OngoingCallService.NotificationServiceState.INSTANCE.isConferenceConnected()) {
                    emitUserBusy = true;
                    sendBusyStatus(jsonObject); // close connection is app killed
                } else {
                    Long channelId = jsonObject.optLong("channel_id");
                    ConnectionManager.INSTANCE.initFayeConnection();
                    //ConnectionManager.INSTANCE.subScribeChannel("/"+String.valueOf(channelId));
                    ConnectionManager.INSTANCE.subScribeChannel("/"+com.hippo.database.CommonData.getUserDetails().getData().getUserChannel());
                    ConnectionManager.INSTANCE.unsubScribeChannel("/"+channelId);
                    ConnectionManager.INSTANCE.subScribeChannel("/"+channelId);
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            ConnectionManager.INSTANCE.subScribeChannel("/"+channelId);
//                        }
//                    }, 100);

                }
            } else if(jsonObject.getString("video_call_type").equals(FuguAppConstant.JitsiCallType.HUNGUP_CONFERENCE.toString())) {
                if(OngoingCallService.NotificationServiceState.INSTANCE.getInviteLink().equals(jsonObject.optString("invite_link"))) {
                    Intent hungupIntent = new Intent(context, HungUpBroadcast.class);
                    hungupIntent.putExtra("action", "rejectCall");
                    hungupIntent.putExtra(MESSAGE_UNIQUE_ID, jsonObject.getString(MESSAGE_UNIQUE_ID));
                    context.sendBroadcast(hungupIntent);
                    Intent mIntent = new Intent("CALL_HANGUP");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(mIntent);

                    Intent mIntent2 = new Intent(VIDEO_CONFERENCE_HUNGUP_INTENT);
                    mIntent2.putExtra(INVITE_LINK, jsonObject.getString(INVITE_LINK));
                    mIntent2.putExtra(JITSI_URL, jsonObject.optString(JITSI_URL));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(mIntent2);
                } else {
                    if(!ConnectionUtils.INSTANCE.isAppRunning(context)) {
                        ConnectionManager.INSTANCE.onClose();
                    }
                }
            } else if(jsonObject.getString("video_call_type").equals(FuguAppConstant.JitsiCallType.REJECT_CONFERENCE.toString())) {
                if(OngoingCallService.NotificationServiceState.INSTANCE.getInviteLink().equals(jsonObject.optString("invite_link"))) {
                    Intent hungupIntent = new Intent(context, HungUpBroadcast.class);
                    hungupIntent.putExtra("action", "rejectCall");
                    hungupIntent.putExtra(MESSAGE_UNIQUE_ID, jsonObject.getString(MESSAGE_UNIQUE_ID));
                    context.sendBroadcast(hungupIntent);
                    Intent mIntent = new Intent("CALL_HANGUP");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(mIntent);

                    Intent mIntent2 = new Intent(VIDEO_CONFERENCE_HUNGUP_INTENT);
                    mIntent2.putExtra(INVITE_LINK, jsonObject.getString(INVITE_LINK));
                    mIntent2.putExtra(JITSI_URL, jsonObject.optString(JITSI_URL));
                    LocalBroadcastManager.getInstance(context).sendBroadcast(mIntent2);
                } else {
                    if(!ConnectionUtils.INSTANCE.isAppRunning(context)) {
                        ConnectionManager.INSTANCE.onClose();
                    }
                }
            } else if(jsonObject.getString(VIDEO_CALL_TYPE).equals(FuguAppConstant.JitsiCallType.START_GROUP_CALL.toString())) {
//                if (OngoingCallService.NotificationServiceState.INSTANCE.isConferenceConnected()) {
//                    emitUserBusy = true;
//                    sendBusyStatus(jsonObject); // close connection is app killed
//                } else {
                    Long channelId = jsonObject.optLong("channel_id");
                    ConnectionManager.INSTANCE.initFayeConnection();
                    ConnectionManager.INSTANCE.subScribeChannel("/"+com.hippo.database.CommonData.getUserDetails().getData().getUserChannel());
                    //ConnectionManager.INSTANCE.unsubScribeChannel("/"+channelId);
                    ConnectionManager.INSTANCE.subScribeChannel("/"+channelId);
                //}
            }
        }
    }

    private void onCallIncomming(Context context, JSONObject jsonObject) throws Exception {
        this.context = context;
        this.jsonObject = jsonObject;

        HippoLog.v("TAG", "jsonObject = "+jsonObject.toString());
        ActivityManager mngr = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);
        Long userId = com.hippo.database.CommonData.getUserDetails().getData().getUserId();

        String fullname = jsonObject.optString("full_name");
        Long channelId = jsonObject.optLong("channel_id");
        String messageUniqueId = jsonObject.optString("muid");
        String videoCallType = jsonObject.optString("video_call_type", "");

        if (!taskList.get(0).topActivity.getClassName().equals("com.hippocall.FuguCallActivity")
                && !isMyServiceRunning(context, VideoCallService.class)
                && jsonObject.getInt(NOTIFICATION_TYPE) == 14//VIDEO_CALL_NOTIFICATION
                && jsonObject.has(VIDEO_CALL_TYPE)
                && (jsonObject.optString(VIDEO_CALL_TYPE).equals("START_CALL"))
                && userId.compareTo(-1L) != 0
                && !jsonObject.optString(DEVICE_ID, "").equals(UniqueIMEIID.getUniqueIMEIId(context))
                && userId.compareTo(jsonObject.optLong(USER_ID)) != 0
                && System.currentTimeMillis() - timeInMillis(jsonObject.getString("date_time")) < 30000) {

            ConnectionManager.INSTANCE.initFayeConnection();
            ConnectionManager.INSTANCE.subScribeChannel("/"+com.hippo.database.CommonData.getUserDetails().getData().getUserChannel());
            ConnectionManager.INSTANCE.unsubScribeChannel("/"+channelId);
            ConnectionManager.INSTANCE.subScribeChannel("/"+channelId);

        } else {
            if(jsonObject.getInt(NOTIFICATION_TYPE) == 14//VIDEO_CALL_NOTIFICATION
                    && jsonObject.has(VIDEO_CALL_TYPE)
                    && (jsonObject.optString(VIDEO_CALL_TYPE).equals("START_CALL"))) {

                ConnectionManager.INSTANCE.initFayeConnection();
                try {
                    JSONObject json = new JSONObject();

                    json.put(FuguAppConstant.VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.USER_BUSY.toString());
                    json.put(FuguAppConstant.IS_SILENT, true);
                    json.put(FuguAppConstant.USER_ID, userId);
                    json.put(FuguAppConstant.FULL_NAME, fullname);
                    json.put(FuguAppConstant.MESSAGE_TYPE, FuguAppConstant.VIDEO_CALL);
                    json.put(FuguAppConstant.IS_TYPING, TYPING_SHOW_MESSAGE);
                    json.put(FuguAppConstant.MESSAGE_UNIQUE_ID, messageUniqueId);

                    JSONObject devicePayload = new JSONObject();
                    devicePayload.put(FuguAppConstant.DEVICE_ID, CommonData.getUniqueIMEIId(context));
                    devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER);
                    devicePayload.put(FuguAppConstant.APP_VERSION, HippoConfig.getInstance().getVersionName());
                    devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(context));
                    json.put("device_payload", devicePayload);

                    ConnectionManager.INSTANCE.publish("/" + channelId, json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
              sendUserBusyBroadcast(context, channelId, messageUniqueId, videoCallType, userId);
            }
        }
    }

    protected boolean isCallActive(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return manager.getMode() == AudioManager.MODE_IN_CALL;
    }

    public void oldSDKCall(String message, String channel) {
        Message turnCredentials = new AppContants().getTurnCredentials();
        Long userId = com.hippo.database.CommonData.getUserDetails().getData().getUserId();

        String fullname = jsonObject.optString("full_name");
        Long channelId = jsonObject.optLong("channel_id");
        String messageUniqueId = jsonObject.optString("muid");
        String videoCallType = jsonObject.optString("video_call_type", "");

        String callType = jsonObject.optString("call_type", "");
        String activityLaunchState = WebRTCCallConstants.AcitivityLaunchState.OTHER.toString();

        String userImage = !TextUtils.isEmpty(jsonObject.optString("user_image"))
                ? jsonObject.optString("user_image")
                : !TextUtils.isEmpty(jsonObject.optString("thumbnail_url"))
                ? jsonObject.optString("thumbnail_url")
                : jsonObject.optString("image_url", "");

        Intent videoIntent = new Intent(context.getApplicationContext(), FuguCallActivity.class);
        VideoCallModel videoCallModel = new VideoCallModel(channelId,
                userImage,
                fullname,
                userId,
                -1,
                fullname,
                turnCredentials.getTurnApiKey(),
                turnCredentials.getUsername(),
                turnCredentials.getCredentials(),
                (ArrayList<String>) (turnCredentials.getIceServers().getStun()),
                (ArrayList<String>) (turnCredentials.getIceServers().getTurn()),
                activityLaunchState,
                messageUniqueId,
                callType.toUpperCase(), "", "", "", "", "", false, false, false);

        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        videoIntent.putExtra("videoCallModel", videoCallModel);

        callRecieved(context, message, videoIntent, jsonObject, videoCallModel, userId);
    }

    private String openedMuid = "";
    private void callRecieved(Context context, String messageJson, Intent videoIntent, JSONObject notificationJson,
                              VideoCallModel videoCallModel, Long myUserId) {
        try {
            JSONObject json = new JSONObject(messageJson);
            if (json.has(MESSAGE_TYPE) && json.getInt(MESSAGE_TYPE) == 18 &&
                    json.getString("muid").equals(notificationJson.getString("muid"))) {
                HippoLog.e("VIDEO_JSON", json.toString());

                if (json.getString(VIDEO_CALL_TYPE).equals("START_CALL")) {
                    JSONObject readyToConnectJson = new JSONObject();
                    readyToConnectJson.put(VIDEO_CALL_TYPE, "READY_TO_CONNECT");
                    readyToConnectJson.put(IS_SILENT, true);
                    readyToConnectJson.put("user_id", myUserId);
                    readyToConnectJson.put(MESSAGE_TYPE, VIDEO_CALL);
                    readyToConnectJson.put(IS_TYPING, TYPING_SHOW_MESSAGE);
                    readyToConnectJson.put("muid", notificationJson.getString("muid"));
                    addTurnCredentialsAndDeviceDetails(context, readyToConnectJson, videoCallModel,
                            notificationJson.getString("channel_id"), videoCallModel.getCallType());
                } else if (json.getString(VIDEO_CALL_TYPE).equals("VIDEO_OFFER")) {
                    if (!isCallActive(context)) {
                        ActivityManager mngr = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);
                        //countDown.cancel();
                        if (!taskList.get(0).topActivity.getClassName().equals("com.hippocall.FuguCallActivity")
                                && !isMyServiceRunning(context, VideoCallService.class)
                                && !taskList.get(0).topActivity.getClassName().contains("GrantPermissionsActivity")) {
                            videoIntent.putExtra("video_offer", messageJson);
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                                if(!openedMuid.equalsIgnoreCase(json.getString("muid"))) {
                                    openedMuid = json.getString("muid");
                                    startCallForegroundService(context, INCOMING_CALL, videoCallModel, messageJson);
                                }
                            } else {
                                if(!openedMuid.equalsIgnoreCase(json.getString("muid"))) {
                                    openedMuid = json.getString("muid");
                                    context.startActivity(videoIntent);
                                }
                            }
                        }
                    } else {
                        JSONObject readyToConnectJson = new JSONObject();
                        readyToConnectJson.put(VIDEO_CALL_TYPE, "USER_BUSY");
                        readyToConnectJson.put(IS_SILENT, true);
                        readyToConnectJson.put("user_id", myUserId);
                        readyToConnectJson.put(MESSAGE_TYPE, VIDEO_CALL);
                        readyToConnectJson.put(IS_TYPING, TYPING_SHOW_MESSAGE);
                        readyToConnectJson.put("muid", notificationJson.getString("muid"));
                        addTurnCredentialsAndDeviceDetails(context, readyToConnectJson, videoCallModel,
                                notificationJson.getString("channel_id"), videoCallModel.getCallType());
                    }
                } else if(json.getString(VIDEO_CALL_TYPE).equals("CALL_HUNG_UP")) {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTurnCredentialsAndDeviceDetails(Context context, JSONObject readyToConnectJson,
                                                    VideoCallModel videoCallModel, String channelId, String callType) {
        try {
            JSONArray stunServers = new JSONArray();
            JSONArray turnServers = new JSONArray();
            JSONObject videoCallCredentials = new JSONObject();

            videoCallCredentials.put("turnApiKey", videoCallModel.getTurnApiKey());
            videoCallCredentials.put("username", videoCallModel.getTurnUserName());
            videoCallCredentials.put("credential", videoCallModel.getTurnCredential());
            for (int i = 0; i < videoCallModel.getStunServers().size(); i++) {
                stunServers.put(videoCallModel.getStunServers().get(i));
            }
            for (int i = 0; i < videoCallModel.getTurnServers().size(); i++) {
                turnServers.put(videoCallModel.getTurnServers().get(i));
            }


            JSONObject devicePayload = new JSONObject();
            devicePayload.put(FuguAppConstant.DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(context));
            devicePayload.put(FuguAppConstant.DEVICE_TYPE, FuguAppConstant.ANDROID_USER);
            devicePayload.put(FuguAppConstant.APP_VERSION, "2.0.5");
            devicePayload.put(FuguAppConstant.DEVICE_DETAILS, CommonData.deviceDetails(context));

            videoCallCredentials.put("stun", stunServers);
            videoCallCredentials.put("turn", turnServers);
            readyToConnectJson.put("turn_creds", videoCallCredentials);
            readyToConnectJson.put("device_payload", devicePayload);

            readyToConnectJson.put("call_type", callType);

            publishSignalToFaye(channelId, readyToConnectJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void publishSignalToFaye(String channelId, JSONObject signalJson) {
        ConnectionManager.INSTANCE.publish("/"+channelId, signalJson);
    }

    public void directAgentCallHooks(Context context, String callType, String transactionId, String userUniqueKey,
                                     String agentEmail, String agentName, String agentImage) {
        if(callType.equalsIgnoreCase("video") && !com.hippo.database.CommonData.getVideoCallStatus()) {
            Toast.makeText(context, "This feature not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if(callType.equalsIgnoreCase("audio") && !com.hippo.database.CommonData.getAudioCallStatus()) {
            Toast.makeText(context, "This feature not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(transactionId)) {
            Toast.makeText(context, "TransactionId can't be null", Toast.LENGTH_SHORT).show();
            return;
        }
        if(TextUtils.isEmpty(userUniqueKey)) {
            Toast.makeText(context, "User unique key can't be null", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String>  otherUserUniqueKeys = new ArrayList<>();
        otherUserUniqueKeys.add(userUniqueKey);
        FuguCreateConversationParams fuguPeerChatParams = new FuguCreateConversationParams(HippoConfig.getInstance().getAppKey(),
                transactionId, agentEmail, otherUserUniqueKeys);

        Long userId = com.hippo.database.CommonData.getUserDetails().getData().getUserId();

        onAgentCallInit(context, callType, userId, new Gson().toJson(fuguPeerChatParams, FuguCreateConversationParams.class)
        , agentName, agentImage);
    }

    private void onAgentCallInit(Context context, String callType, Long userId, String fuguPeerChatParams, String agentName, String agentImage) {
        int callValue = VIDEO_CALL_VIEW;
        String activityLaunchState = WebRTCCallConstants.AcitivityLaunchState.SELF.toString();
        if(callType.equalsIgnoreCase("audio")) {
            callValue = AUDIO_CALL_VIEW;
        } else if(callType.equalsIgnoreCase("video")) {
            callValue = VIDEO_CALL_VIEW;
        } else {
            Toast.makeText(context, "Call type should be audio/video", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent videoIntent = new Intent(context, FuguCallActivity.class);

        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        videoIntent.putExtra(FuguAppConstant.USER_AGENT_CALL, true);
        videoIntent.putExtra(FuguAppConstant.PEER_CHAT_PARAMS, fuguPeerChatParams);
        if(!isMyServiceRunning(context, VideoCallService.class)) {
            Message turnCredentials = new AppContants().getTurnCredentials();
            String messageUniqueId = UUID.randomUUID().toString();

            VideoCallModel videoCallModel = new VideoCallModel(-1,
                    agentImage,
                    "",
                    userId,
                    -1,
                    agentName,
                    turnCredentials.getTurnApiKey(),
                    turnCredentials.getUsername(),
                    turnCredentials.getCredentials(),
                    (ArrayList<String>) (turnCredentials.getIceServers().getStun()),
                    (ArrayList<String>) (turnCredentials.getIceServers().getTurn()),
                    activityLaunchState,
                    messageUniqueId,
                    callType.toUpperCase(), "", "", "",
                    "", "", false, false, false);
            videoIntent.putExtra("videoCallModel", videoCallModel);
        }
        context.startActivity(videoIntent);

    }

    @Override
    public void onExternalClick(Context context, String callType, Long userId, String otherUserName,
                                String fuguPeerChatParams, String otheruserImageUrl, String otherUserImage) {
        int callValue = VIDEO_CALL_VIEW;
        String activityLaunchState = WebRTCCallConstants.AcitivityLaunchState.SELF.toString();
        if(callType.equalsIgnoreCase("audio")) {
            callValue = AUDIO_CALL_VIEW;
        } else if(callType.equalsIgnoreCase("video")) {
            callValue = VIDEO_CALL_VIEW;
        } else {
            Toast.makeText(context, "Call type should be audio/video", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = otheruserImageUrl;
        if(TextUtils.isEmpty(path))
            path = "";

        Intent videoIntent = new Intent(context, MainCallingActivity.class);

        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        videoIntent.putExtra(FuguAppConstant.PEER_CHAT_PARAMS, fuguPeerChatParams);
        if(!isMyServiceRunning(context, OngoingCallService.class)) {
            Message turnCredentials = new AppContants().getTurnCredentials();
            String messageUniqueId = UUID.randomUUID().toString();

            String roomName = Links.INSTANCE.randomVideoConferenceLink();
            String link = Links.INSTANCE.getLink(callType, roomName);

            VideoCallModel videoCallModel = new VideoCallModel(-1,
                    path,
                    otherUserName,
                    userId,
                    -1,
                    otherUserName,
                    turnCredentials.getTurnApiKey(),
                    turnCredentials.getUsername(),
                    turnCredentials.getCredentials(),
                    (ArrayList<String>) (turnCredentials.getIceServers().getStun()),
                    (ArrayList<String>) (turnCredentials.getIceServers().getTurn()),
                    activityLaunchState,
                    messageUniqueId,
                    callType.toUpperCase(), link, link, roomName,
                    "", "", false, false, false);
            videoIntent.putExtra("videoCallModel", videoCallModel);
        }
        context.startActivity(videoIntent);

    }

    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
        if(updateView != null)
            updateView.updateFragment(fragment);
    }

    public void timerVisibility(int status) {
        if(updateView != null)
            updateView.timerVisibilityStatus(status);
    }

    public void setTimer(String txt) {
        if(updateView != null)
            updateView.updateTimer(txt);
    }

    public void openOnGoingCall(Context context, OnScreenChangeListener changeListener) {
        if(isMyServiceRunning(context, VideoCallService.class)) {
            Intent videoIntent = new Intent(context.getApplicationContext(), FuguCallActivity.class);
            videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//FLAG_ACTIVITY_NEW_TASK
            videoIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(videoIntent);
            if(changeListener != null)
                changeListener.onScreenStatus(true);
        } else {
            if(changeListener != null)
                changeListener.onScreenStatus(false);
        }
    }

    public void sendCustomData(Context context, CustomDataAttributes data) {
        if(updateView == null) {
            try {
                Intent videoCallIntent = new Intent(VIDEO_CALL_INTENT);
                videoCallIntent.putExtra("custom_data", "CUSTOM_DATA");
                JSONObject json = new JSONObject();
                json.put("unique_id", data.getUniqueId());
                json.put("flag", data.getFlag());
                if(!TextUtils.isEmpty(data.getMessage()))
                    json.put("message", data.getMessage());

                videoCallIntent.putExtra("data", json.toString());
                LocalBroadcastManager.getInstance(context).sendBroadcast(videoCallIntent);
            } catch (Exception e) {

            }
        } else {
            try {
                JSONObject json = new JSONObject();
                json.put("unique_id", data.getUniqueId());
                json.put("flag", data.getFlag());
                if(!TextUtils.isEmpty(data.getMessage()))
                    json.put("message", data.getMessage());
                updateView.sendCustomData(json);
            } catch (Exception e) {

            }
        }
    }

    public void setHippoCallPushIcon(int hippoCallPushIcon) {
        this.hippoCallPushIcon = hippoCallPushIcon;
    }

    public int getHippoCallPushIcon() {
        return hippoCallPushIcon;
    }

    private Fragment fragment;
    protected Fragment getFragment() {
        return fragment;
    }

    protected boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private StringAttributes getScreenString() {
        StringAttributes attributes = new StringAttributes.Builder()
                .setAndString("and")
                .setMuteString("sound mute")
                .setCameraOffString("camera closed")
                .setVideoPaused("Video paused")
                .showUserName(true)
                .build();

        return attributes;
    }

    public void setSurfaceMirror(boolean localSurfaceMirror, boolean remoteSurfaceMirror) {
        CommonData.setMirrorStatus(LOCAL_SURFACE, localSurfaceMirror);
        CommonData.setMirrorStatus(REMOTE_SURFACE, remoteSurfaceMirror);
    }

    public void setFlipCameraIcons(boolean flipCamera) {
        CommonData.setMirrorStatus(FLIP_CAMERA, flipCamera);
    }

    /*public FayeClient getmClient() {
        HippoConfig.getExistingClient(client -> {
            mClient = client;
        });
        return mClient;
    }*/

    private void emitReadyToConnect(FayeClient mClient, JSONObject data, Long userId) {
        try {
            JSONObject startCallJson = new JSONObject();
            startCallJson.put(IS_SILENT, true);
            startCallJson.put(VIDEO_CALL_TYPE, FuguAppConstant.JitsiCallType.READY_TO_CONNECT_CONFERENCE);
            startCallJson.put(USER_ID, userId);
            startCallJson.put(CHANNEL_ID, data.getString(CHANNEL_ID));
            startCallJson.put(MESSAGE_TYPE, VIDEO_CALL);
            startCallJson.put(WebRTCCallConstants.Companion.getCALL_TYPE(), "VIDEO");
            startCallJson.put(WebRTCCallConstants.Companion.getDEVICE_PAYLOAD(), getDeviceDetails(context));
            startCallJson.put(INVITE_LINK, data.getString(INVITE_LINK));
            startCallJson.put(JITSI_URL, data.optString(INVITE_LINK));
            startCallJson.put(MESSAGE_UNIQUE_ID, data.getString(MESSAGE_UNIQUE_ID));

            startCallJson.put(WebRTCCallConstants.Companion.getFULL_NAME(), data.optString("full_name", ""));
            startCallJson.put("message", "");
            startCallJson.put("is_typing", TYPING_SHOW_MESSAGE);
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER);

            String channelId = "/"+data.optLong(CHANNEL_ID);
            mClient.publish(channelId, startCallJson);
            //ConnectionManager.INSTANCE.sendMessage(data.optLong(CHANNEL_ID), startCallJson);
            android.util.Log.e("Video_CONF-->", startCallJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void emitUserBusy(FayeClient mClient, Context context, JSONObject data, Long userId) {
        try {
            JSONObject startCallJson = new JSONObject();
            startCallJson.put(IS_SILENT, false);
            startCallJson.put(VIDEO_CALL_TYPE, FuguAppConstant.JitsiCallType.USER_BUSY_CONFERENCE);
            startCallJson.put(USER_ID, userId);
            startCallJson.put(CHANNEL_ID, data.getString(CHANNEL_ID));
            startCallJson.put(MESSAGE_TYPE, VIDEO_CALL);
            startCallJson.put(WebRTCCallConstants.Companion.getCALL_TYPE(), "VIDEO");
            startCallJson.put(WebRTCCallConstants.Companion.getDEVICE_PAYLOAD(), getDeviceDetails(context));
            startCallJson.put(INVITE_LINK, data.getString(INVITE_LINK));
            startCallJson.put(JITSI_URL, data.optString(JITSI_URL));
            startCallJson.put(MESSAGE_UNIQUE_ID, data.getString(MESSAGE_UNIQUE_ID));

            startCallJson.put(WebRTCCallConstants.Companion.getFULL_NAME(), data.optString("full_name", ""));
            startCallJson.put("message", "");
            startCallJson.put("is_typing", TYPING_SHOW_MESSAGE);
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER);

            String channelId = "/"+data.optLong(CHANNEL_ID);
            mClient.publish(channelId, startCallJson);
            //ConnectionManager.INSTANCE.sendMessage(data.optLong(CHANNEL_ID), startCallJson);
            android.util.Log.e("Video_CONF-->", startCallJson.toString());
        } catch (Exception e) {

        }
    }

    public JSONObject getDeviceDetails(Context context) {
        try {
            JSONObject devicePayload = new JSONObject();
            devicePayload.put(DEVICE_ID, UniqueIMEIID.getUniqueIMEIId(context));
            devicePayload.put(DEVICE_TYPE, ANDROID_USER);
            devicePayload.put(APP_VERSION, HippoConfig.getInstance().getVersionName());
            devicePayload.put(DEVICE_DETAILS, CommonData.deviceDetails(context));
            return devicePayload;
        } catch (Exception e) {
            return new JSONObject();
        }
    }


    protected void initOldCall(VideoCallModel videoCallModel) {
        try {
            Intent videoIntent = new Intent(context.getApplicationContext(), FuguCallActivity.class);
            if (!isMyServiceRunning(context, VideoCallService.class)) {
                videoIntent.putExtra("videoCallModel", videoCallModel);
            }
            context.startActivity(videoIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startCallForegroundService(Context context, String status, VideoCallModel videoCallModel, String messageJson) {
        Intent startIntent = new Intent(context.getApplicationContext(), VideoCallService.class);
        String channelName = "";
        channelName = videoCallModel.getChannelName();
        startIntent.setAction("com.fuguchat.start");
        startIntent.putExtra(CALL_STATUS, status);
        startIntent.putExtra(CHANNEL_NAME, channelName);
        startIntent.putExtra(VIDEO_CALL_MODEL, videoCallModel);
        startIntent.putExtra(INIT_FULL_SCREEN_SERVICE, true);
        startIntent.putExtra("messageJson", messageJson);
        new Thread(new Runnable() {
            @Override
            public void run() {
                CommonData.setVideoCallModel(videoCallModel);
            }
        }).start();
        try {
            ContextCompat.startForegroundService(context, startIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // from faye messages flow
    @Override
    public void callingFlow(JSONObject data, String msg, String channel) {
        HippoCallingFlow.INSTANCE.callingFlow(data, msg, channel);
    }

    @Override
    public boolean isCallServiceRunning() {
        if(getContext() == null)
            return false;
        return isMyServiceRunning(getContext(), OngoingCallService.class);
    }

    @Override
    public void startGroupCall(JSONObject data, String msg, String channel) {
        HippoCallingFlow.INSTANCE.callingFlow(data, msg, channel);
    }

    @Override
    public void onGroupNotificationReceived(Context context, JSONObject data) {
        try {
            onGroupCall(context, data);
        } catch (Exception e) {

        }
    }

    private OnGroupCallListener listener;

    public OnGroupCallListener getListener() {
        return listener;
    }
    public void onGroupCallListener(OnGroupCallListener listener) {
        this.listener = listener;
    }

    private void onGroupCall(Context context, JSONObject jsonObject) throws Exception {
        if(jsonObject.has(VIDEO_CALL_TYPE)) {
            if(jsonObject.getString(VIDEO_CALL_TYPE).equals(FuguAppConstant.JitsiCallType.START_GROUP_CALL.toString())) {
                if (OngoingCallService.NotificationServiceState.INSTANCE.isConferenceConnected()) {
                    //emitUserBusy = true;
                    //sendBusyStatus(jsonObject, 2); // close connection is app killed
                } else {
                    ConnectionManager.INSTANCE.initFayeConnection();
                    ConnectionManager.INSTANCE.subScribeChannel("/"+com.hippo.database.CommonData.getUserDetails().getData().getUserChannel());
                }
            }
        }
    }

    @Override
    public void openDirectLink(Context context, String roomId, String userName, String callType, String imagePath,
                               Long channelId, String transactionId, int isAudioEnabled, int isVideoEnabled) {
        if(!TextUtils.isEmpty(com.hippo.database.CommonData.getUserDetails().getData().getEn_user_id()) && !TextUtils.isEmpty(roomId)) {
            joinSession(context, roomId, userName, callType, imagePath, channelId, transactionId, isAudioEnabled, isVideoEnabled);
        }
    }

    @Override
    public void leaveGroupCall(Context context, String transactionId) {
        Intent hungupIntent = new Intent(context, HungUpBroadcast.class);
        hungupIntent.putExtra("action", "leaveSession");
        hungupIntent.putExtra("transactionId", transactionId);
        context.sendBroadcast(hungupIntent);
    }

    private URL getServerUrl() {
        URL serverURL;
        if(!TextUtils.isEmpty(getJitsiURL())) {
            try {
                serverURL = new URL(getJitsiURL());
                return serverURL;
            } catch (Exception e) {

            }
        }
        try {
            serverURL = new URL(FuguAppConstant.CONFERENCING_LIVE);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid server URL!");
        }
        return serverURL;
    }

    private void joinSession(Context context, String roomName, String title, String callType,
                             String imagePath, Long channelId, String transactionId, int isAudioEnabled, int isVideoEnable) {
        if (OngoingCallService.NotificationServiceState.INSTANCE.isConferenceConnected()) {

            return;
        }

        String link = FuguAppConstant.CONFERENCING_LIVE +"/"+roomName;
        String jitsiLink = "";
        if(!TextUtils.isEmpty(getJitsiURL())) {
            jitsiLink = getJitsiURL() + "/" + roomName;
        }
        answerGroupCall(context, channelId, link, jitsiLink, title, transactionId);

        URL serverURL = getServerUrl();
        JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
        try {
            userInfo.setDisplayName(title);
            if (!TextUtils.isEmpty(imagePath))
                userInfo.setAvatar(new URL(imagePath));
        } catch (Exception e) {
            userInfo.setDisplayName("Fellow User");
        }

        try {
            if (isVideoEnable == 0) {
                JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setAudioOnly(true)
                        .setAudioMuted(isAudioEnabled == 0)
                        .setFeatureFlag("chat.enabled", false)
                        .setFeatureFlag("invite.enabled", false)
                        .setUserInfo(userInfo)
                        .build();
                JitsiMeet.setDefaultConferenceOptions(defaultOptions);
            } else {
                JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .setWelcomePageEnabled(false)
                        .setAudioOnly(false)
                        .setAudioMuted(isAudioEnabled == 0)
                        .setFeatureFlag("chat.enabled", false)
                        .setFeatureFlag("invite.enabled", false)
                        .setUserInfo(userInfo)
                        .build();
                JitsiMeet.setDefaultConferenceOptions(defaultOptions);
            }
        } catch (Exception e) {
            JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setWelcomePageEnabled(false)
                    .setAudioOnly(false)
                    .setFeatureFlag("chat.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setUserInfo(userInfo)
                    .build();
            JitsiMeet.setDefaultConferenceOptions(defaultOptions);
        }

        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setRoom(roomName)
                .build();

        JitsiMeetActivity.launch(context, options, Restring.getString(context, R.string.hippo_calling_connection));

        //ShowDialg.INSTANCE.hideDialog();
    }

    private void startOngoingCallService(Context context, String signalUniqueId, String inviteLink, Long channelId, String muid, String transactionId) {
        OngoingCallService.NotificationServiceState.INSTANCE.setConferenceConnected(true);
        OngoingCallService.NotificationServiceState.INSTANCE.setChannelId(channelId);
        OngoingCallService.NotificationServiceState.INSTANCE.setConferenceServiceRunning(true);
        OngoingCallService.NotificationServiceState.INSTANCE.setHasGroupCall(true);
        OngoingCallService.NotificationServiceState.INSTANCE.setInviteLink(inviteLink);
        OngoingCallService.NotificationServiceState.INSTANCE.setMuid(muid);
        OngoingCallService.NotificationServiceState.INSTANCE.setTransactionId(transactionId);

        Intent startIntent = new Intent(context, OngoingCallService.class);
        startIntent.setAction("com.hippochat.notification.start");
        startIntent.putExtra(FuguAppConstant.MESSAGE_UNIQUE_ID, signalUniqueId);
        startIntent.putExtra(FuguAppConstant.INVITE_LINK, inviteLink);
        startIntent.putExtra(FuguAppConstant.CHANNEL_ID, channelId);
        ContextCompat.startForegroundService(context, startIntent);

        try {
            if(HippoCallConfig.getInstance().context != null)
                HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false);
            else
                HippoAudioManager.getInstance(context).stop(false);
        } catch (Exception e) {

        }
    }

    private void answerGroupCall(Context context, Long channelId, String inviteLink, String jitsiLink, String fullName, String transactionId) {
        Long userId = com.hippo.database.CommonData.getUserDetails().getData().getUserId();
        String muid = UUID.randomUUID().toString();
        startOngoingCallService(context, muid, inviteLink, channelId, muid, transactionId);
        try {
            JSONObject startCallJson = new JSONObject();
            startCallJson.put(FuguAppConstant.IS_SILENT, true);
            startCallJson.put("video_call_type", WebRTCCallConstants.VideoCallType.JOIN_GROUP_CALL.toString());
            startCallJson.put(FuguAppConstant.USER_ID, userId);
            startCallJson.put(FuguAppConstant.CHANNEL_ID, channelId);
            startCallJson.put(FuguAppConstant.MESSAGE_TYPE, 27);
            startCallJson.put("call_type", "VIDEO");
            startCallJson.put(FuguAppConstant.MESSAGE_UNIQUE_ID, muid);
            startCallJson.put("device_payload", getDeviceDetails(context));
            startCallJson.put(FuguAppConstant.INVITE_LINK, inviteLink);
            startCallJson.put("message", "");
            startCallJson.put("is_typing", FuguAppConstant.TYPING_SHOW_MESSAGE);
            startCallJson.put("user_type", FuguAppConstant.ANDROID_USER);
            startCallJson.put("full_name", fullName);

            ConnectionManager.INSTANCE.publish("/"+channelId, startCallJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private CountDownTimer timerTask;
    public void startTimerTask() {
        /*try {
            if(timerTask == null) {
                timerTask = new CountDownTimer(62000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        try {
                            Intent startIntent = new Intent(context, OngoingCallService.class);
                            HippoCallConfig.getInstance().context.stopService(startIntent);
                            HippoAudioManager.getInstance(HippoCallConfig.getInstance().context).stop(false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public void stopTimerTask() {
        /*try {
            if(timerTask != null) {
                timerTask.cancel();
                timerTask = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

}
