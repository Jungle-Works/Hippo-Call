package com.hippocall.confcall

import android.app.Dialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.hippocall.R

public class CallFeedbackActivity : AppCompatActivity() {
    private var dialog: Dialog? = null
    var mListener: AudioManager.OnAudioFocusChangeListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_feedback)
        val win = window
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        win.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        mListener = AudioManager.OnAudioFocusChangeListener { }
        showFeedBackDialog()
    }

    // Activity lifecycle method overrides
    //
    private fun showFeedBackDialog() {
        try {
            dialog = Dialog(this@CallFeedbackActivity, android.R.style.Theme_Translucent_NoTitleBar)
            dialog?.setContentView(R.layout.activity_calling_feed_back)
            val lp = dialog?.window!!.attributes
            lp.dimAmount = 0.5f
            dialog?.window!!.attributes = lp
            dialog?.window!!.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog?.setCancelable(false)
            dialog?.setCanceledOnTouchOutside(false)
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val ratingBar: CustomRatingBar? = dialog?.findViewById(R.id.ratingBar)
            val tvRating: TextView? = dialog?.findViewById(R.id.tvRating)
            val etFeedback: EditText? = dialog?.findViewById(R.id.etFeedback)
            val btnNotNow: AppCompatButton? = dialog?.findViewById(R.id.btnNotNow)
            val btnSubmit: AppCompatButton? = dialog?.findViewById(R.id.btnSubmit)

            ratingBar?.setOnScoreChanged { score ->

                if (score >= 0f) {
                    tvRating?.visibility = View.VISIBLE
                } else {
                    tvRating?.visibility = View.GONE
                }
                when (score) {

                    1f -> {
                        tvRating?.text = "Very Bad"
                    }
                    2f -> {
                        tvRating?.text = "Bad"
                    }
                    3f -> {
                        tvRating?.text = "Average"
                    }
                    4f -> {
                        tvRating?.text = "Good"
                    }
                    5f -> {
                        tvRating?.text = "Excellent"
                    }
                    else -> {

                    }
                }
            }


            btnNotNow?.setOnClickListener {
                dialog?.dismiss()
                //IncomingVideoConferenceActivity.IncomingCall.incomingConferenceStatus = false
                finishAndRemoveTask()
            }

            btnSubmit?.setOnClickListener {
                //apiSendFeedback(ratingBar?.score!!, FuguAppConstant.Feedback.VIDEO_CONFERENCE.toString(), etFeedback?.text.toString().trim())
                dialog = null

            }

            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.mode = AudioManager.MODE_RINGTONE
        if (am.isBluetoothScoOn){
            am.startBluetoothSco()
            am.stopBluetoothSco()
        }
        am.abandonAudioFocus(mListener)
    }
    private fun apiSendFeedback(rating: Float, type: String, feedback: String) {

        /*val jsonObject = JSONObject()
        val gson = Gson()
        val json = gson.toJson(com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().getUserInfo())
        try {
//            jsonObject.put("workspace_name", com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].workspaceName)
//            jsonObject.put("workspace_id", com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].workspaceId)
            jsonObject.put("type", type)
            jsonObject
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val commonParams = com.skeleton.mvp.data.network.CommonParams.Builder()
        if (!TextUtils.isEmpty(feedback)) {
            commonParams.add("feedback", feedback)
        }
        commonParams.add("workspace_id", com.skeleton.mvp.data.db.CommonData.getCommonResponse().getData().workspacesInfo[com.skeleton.mvp.data.db.CommonData.getCurrentSignedInPosition()].workspaceId)
        commonParams.add("type", type)
        commonParams.add("rating", rating.toInt())
        commonParams.add("extra_details", jsonObject.toString())

        RestClient.getApiInterface(true).sendFeedback(CommonData.getCommonResponse().getData().getUserInfo().getAccessToken(), BuildConfig.VERSION_CODE, FuguAppConstant.ANDROID, commonParams.build().map)
                .enqueue(object : ResponseResolver<CommonResponse>() {
                    override fun success(commonResponse: CommonResponse) {
                        Toast.makeText(this@CallFeedbackActivity, "Feedback Submitted", Toast.LENGTH_LONG).show()
                        IncomingVideoConferenceActivity.IncomingCall.incomingConferenceStatus = false
                        finishAndRemoveTask()
                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.mode = AudioManager.MODE_RINGTONE
                        if (am.isBluetoothScoOn){
                            am.startBluetoothSco()
                            am.stopBluetoothSco()
                        }
                        am.abandonAudioFocus(mListener)
                    }

                    override fun failure(error: APIError) {
                        Toast.makeText(this@CallFeedbackActivity, error.message, Toast.LENGTH_LONG).show()
                        IncomingVideoConferenceActivity.IncomingCall.incomingConferenceStatus = false
                        finishAndRemoveTask()
                        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.mode = AudioManager.MODE_RINGTONE
                        if (am.isBluetoothScoOn){
                            am.startBluetoothSco()
                            am.stopBluetoothSco()
                        }
                        am.abandonAudioFocus(mListener)
                    }
                })*/
    }
}
