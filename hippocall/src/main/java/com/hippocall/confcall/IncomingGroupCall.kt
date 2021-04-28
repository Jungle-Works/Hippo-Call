package com.hippocall.confcall

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hippo.eventbus.BusProvider
import com.hippo.langs.Restring
import com.hippocall.*
import com.hippocall.model.FragmentFlow
import com.squareup.otto.Subscribe
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

/**
 * Created by gurmail on 2020-07-15.
 * @author gurmail
 */
class IncomingGroupCall : Fragment() {

    private var tvCallType: AppCompatTextView? = null
    private var tvIncomingPersonName: AppCompatTextView? = null
    private var title: AppCompatTextView? = null
    private var ivRejectCall: AppCompatImageView? = null
    private var ivAnswerCall: AppCompatImageView? = null

    var countDownTimer: CountDownTimer? = null
    var dialog: Dialog? = null


    private var mainActivity: MainCallingActivity? = null
    private var callType: String = ""
    private var personName: String = ""
    private var imagePath: String = ""
    private var videoCallModel: VideoCallModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            videoCallModel = arguments!!.getParcelable<VideoCallModel>("videoCallModel")!! as VideoCallModel
            Log.d("videoCallModel", "videoCallModel = "+ Gson().toJson(videoCallModel))
            callType = videoCallModel?.callType!!
            //personName = videoCallModel?.fullName!!
            personName = videoCallModel?.channelName!!
            imagePath = videoCallModel?.userThumbnailImage!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_incoming_group_call, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)

        if (callType.equals("VIDEO")) {
            tvCallType?.text = Restring.getString(activity, R.string.hippo_video_call)
        } else {
            tvCallType?.text = Restring.getString(activity, R.string.hippo_audio_call)
        }
        tvIncomingPersonName?.text = personName
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainCallingActivity
        BusProvider.getInstance().register(this)
    }

    private fun sendDate(type: Int) {
        BusProvider.getInstance().post(
            FragmentFlow(WebRTCCallConstants.BusFragmentType.INCOMMING_GROUP_CALL.toString(), type, JSONObject(), "")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        sendDate(WebRTCCallConstants.IncommintJitsiCall.UNREGISTER_BROADCAST)
        BusProvider.getInstance().unregister(this)
    }

    override fun onResume() {
        super.onResume()
        sendDate(WebRTCCallConstants.IncommintJitsiCall.REGISTER_BROADCAST)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        mediaPlayer?.stop()
    }


    override fun onStop() {
        sendDate(WebRTCCallConstants.IncommintJitsiCall.STOP)
        super.onStop()
    }

    private fun initViews(view: View) {
        tvCallType = view.findViewById(R.id.tvCallType)
        title = view.findViewById(R.id.title)
        tvIncomingPersonName = view.findViewById(R.id.tvIncomingPersonName)
        ivRejectCall = view.findViewById(R.id.ivRejectCall)
        ivAnswerCall = view.findViewById(R.id.ivAnswerCall)

        object : CountDownTimer(60000, 1000) {
            override fun onFinish() {
                Log.e("Timer","Done")
//                mediaPlayer?.stop()
                activity?.finish()
            }

            override fun onTick(millisUntilFinished: Long) {
            }

        }.start()

        ivAnswerCall?.setOnClickListener {
            sendDate(WebRTCCallConstants.IncommintJitsiCall.ANSWERCALL)
        }

        ivRejectCall?.setOnClickListener {
            sendDate(WebRTCCallConstants.IncommintJitsiCall.REJECTCALL)
        }
    }

    @Subscribe
    public fun onBusFragmentType(data: FragmentFlow) {
        when(data.fragmentType) {
            WebRTCCallConstants.BusFragmentType.UPDATE_INCOMIMG_CONFIG.toString() -> {
                if(data.type == 1 && !TextUtils.isEmpty(data.data) && TextUtils.isEmpty(personName)) {
                    activity?.runOnUiThread {
                        tvIncomingPersonName?.text = data.data
                    }

                } else if(data.type == 2) {
                    activity?.finish()
                }
            }
        }
    }
}