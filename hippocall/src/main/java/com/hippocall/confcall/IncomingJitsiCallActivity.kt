package com.hippocall.confcall

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
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
import com.hippo.constant.FuguAppConstant
import com.hippo.eventbus.BusProvider
import com.hippo.helper.BusEvents
import com.hippo.helper.FayeMessage
import com.hippo.langs.Restring
import com.hippo.utils.HippoLog
import com.hippocall.*
import com.hippocall.WebRTCCallConstants.Companion.CALL_TYPE
import com.hippocall.WebRTCCallConstants.Companion.FULL_NAME
import com.hippocall.WebRTCCallConstants.Companion.USER_THUMBNAIL_IMAGE
import com.hippocall.model.FragmentFlow
import com.squareup.otto.Subscribe
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONException
import org.json.JSONObject

public class IncomingJitsiCallActivity : Fragment(), CallTouchListener.OnCallItemTouchListener {

    private var tvCallType: AppCompatTextView? = null
    private var tvIncomingPersonName: AppCompatTextView? = null
    private var ivIncomingPersonImage: CircleImageView? = null
    private var ivIncomingPersonImageBig: AppCompatImageView? = null
    var llReject: LinearLayout? = null
    var llReply: LinearLayout? = null
    var llAnswer: LinearLayout? = null
    var answerRoot: LinearLayout? = null
    private val animTime = 150
    var answerImagesList = ArrayList<AppCompatImageView>()
    var rejectImagesList = ArrayList<AppCompatImageView>()
    var replyImagesList = ArrayList<AppCompatImageView>()
    val set = AnimatorSet()
    var tvRejectCall: AppCompatTextView? = null
    var tvReplyCall: AppCompatTextView? = null
    var tvAnswerCall: AppCompatTextView? = null
    var ivRejectCall: AppCompatImageView? = null
    var ivReplyCall: AppCompatImageView? = null
    var ivAnswerCall: AppCompatImageView? = null
    var image: ImageView? = null
    var countDownTimer: CountDownTimer? = null
    var dialog: Dialog? = null


    private var mainActivity: MainCallingActivity? = null
    private var callType: String = ""
    private var personName: String = ""
    private var imagePath: String = ""
    private var videoCallModel: VideoCallModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*if(arguments != null) {
            callType = arguments!!.getString(CALL_TYPE, "")
            personName = arguments!!.getString(FULL_NAME, "")
            imagePath = arguments!!.getString(USER_THUMBNAIL_IMAGE, "")
        }*/

        if (arguments != null) {
            videoCallModel = arguments!!.getParcelable<VideoCallModel>("videoCallModel")!! as VideoCallModel
            //Log.d("videoCallModel", "videoCallModel = "+Gson().toJson(videoCallModel))
            callType = videoCallModel?.callType!!
            personName = videoCallModel?.fullName!!
            imagePath = videoCallModel?.userThumbnailImage!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_incoming_jitsi_call, container, false)
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

        Glide.with(this)
            .load(imagePath)
            .placeholder(R.drawable.hippo_ic_call_placeholder)
            .into(ivIncomingPersonImage!!)

        Glide.with(this)
            .load(imagePath)
            .into(ivIncomingPersonImageBig!!)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainCallingActivity
    }

    private fun sendDate(type: Int) {
        BusProvider.getInstance().post(FragmentFlow(WebRTCCallConstants.BusFragmentType.INCOMMING_JITSI_CALL.toString(),
            type, JSONObject(), ""))
    }

    override fun onDestroy() {
        super.onDestroy()
        sendDate(WebRTCCallConstants.IncommintJitsiCall.UNREGISTER_BROADCAST)
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        mediaPlayer?.stop()
    }

    override fun onResume() {
        super.onResume()
        sendDate(WebRTCCallConstants.IncommintJitsiCall.REGISTER_BROADCAST)
    }


    override fun onStop() {
        sendDate(WebRTCCallConstants.IncommintJitsiCall.STOP)
        super.onStop()
    }

    private fun initViews(view: View) {
        tvCallType = view.findViewById(R.id.tvCallType)
        tvIncomingPersonName = view.findViewById(R.id.tvIncomingPersonName)
        ivIncomingPersonImage = view.findViewById(R.id.ivIncomingPersonImage)
        ivIncomingPersonImageBig = view.findViewById(R.id.ivIncomingPersonImageBig)
        llReject = view.findViewById(R.id.llReject)
        llReply = view.findViewById(R.id.llReply)
        llAnswer = view.findViewById(R.id.llAnswer)

        tvReplyCall = view.findViewById(R.id.tvReply)
        tvRejectCall = view.findViewById(R.id.tvReject)
        tvAnswerCall = view.findViewById(R.id.tvAnswer)

        tvAnswerCall?.text = Restring.getString(activity, R.string.hippo_swipe_accept)
        tvRejectCall?.text = Restring.getString(activity, R.string.hippo_swipe_reject)


        answerRoot = view.findViewById(R.id.answerRoot)

        answerImagesList.add(view.findViewById(R.id.pick_call_arrow_up_one))
        answerImagesList.add(view.findViewById(R.id.pick_call_arrow_up_two))
        answerImagesList.add(view.findViewById(R.id.pick_call_arrow_up_three))
        answerImagesList.add(view.findViewById(R.id.pick_call_arrow_up_four))

        replyImagesList.add(view.findViewById(R.id.reply_call_arrow_up_one))
        replyImagesList.add(view.findViewById(R.id.reply_call_arrow_up_two))
        replyImagesList.add(view.findViewById(R.id.reply_call_arrow_up_three))
        replyImagesList.add(view.findViewById(R.id.reply_call_arrow_up_four))

        rejectImagesList.add(view.findViewById(R.id.reject_call_arrow_up_one))
        rejectImagesList.add(view.findViewById(R.id.reject_call_arrow_up_two))
        rejectImagesList.add(view.findViewById(R.id.reject_call_arrow_up_three))
        rejectImagesList.add(view.findViewById(R.id.reject_call_arrow_up_four))

        ivRejectCall = view.findViewById(R.id.ivRejectCall)
        ivAnswerCall = view.findViewById(R.id.ivAnswerCall)
        ivReplyCall = view.findViewById(R.id.ivReplyCall)

        image = view.findViewById(R.id.ivAnswerCall)

        startAcceptAnimation(answerImagesList)
        startRejectAnimation(rejectImagesList)
        startReplyAnimation(replyImagesList)
        ivAnswerCall?.setOnTouchListener(CallTouchListener(answerRoot, ivAnswerCall, activity, this))
        ivReplyCall?.setOnTouchListener(CallTouchListener(answerRoot, ivReplyCall, activity, this))
        ivRejectCall?.setOnTouchListener(CallTouchListener(answerRoot, ivRejectCall, activity, this))
        onShakeImage()

        object : CountDownTimer(60000, 1000) {
            override fun onFinish() {
                Log.e("Timer","Done")
//                mediaPlayer?.stop()
                activity?.finish()
            }

            override fun onTick(millisUntilFinished: Long) {
            }

        }.start()
    }

    private fun startAcceptAnimation(imagesList: ArrayList<AppCompatImageView>) {

        try {
            set.cancel()
        } catch (e: Exception) {

        }

        for (image in imagesList) {
            image.clearAnimation()
        }
        val arrowOneFadeIn = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 0f, 1f)
        arrowOneFadeIn.duration = animTime.toLong()
        val arrowTwoFadeIn = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 0f, 1f)
        arrowTwoFadeIn.setDuration(animTime.toLong()).startDelay = animTime.toLong()
        val arrowOneFadeOut = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 1f, 0f)
        arrowOneFadeOut.setDuration(animTime.toLong()).startDelay = 375
        val arrowThreeFadeIn = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 0f, 1f)
        arrowThreeFadeIn.setDuration(animTime.toLong()).startDelay = 300
        val arrowTwoFadeOut = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 1f, 0f)
        arrowTwoFadeOut.setDuration(animTime.toLong()).startDelay = 525
        val arrowFourFadeIn = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 0f, 1f)
        arrowFourFadeIn.setDuration(animTime.toLong()).startDelay = 450
        val arrowThreeFadeOut = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 1f, 0f)
        arrowThreeFadeOut.setDuration(animTime.toLong()).startDelay = 675
        val arrowFourFadeOut = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 1f, 0f)
        arrowFourFadeOut.setDuration(animTime.toLong()).startDelay = 825
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                set.start()
            }
        })

        set.play(arrowOneFadeIn)
        set.play(arrowTwoFadeIn)
        set.play(arrowOneFadeOut)
        set.play(arrowThreeFadeIn)
        set.play(arrowTwoFadeOut)
        set.play(arrowFourFadeIn)
        set.play(arrowThreeFadeOut)
        set.play(arrowFourFadeOut)
        set.start()
    }

    private fun startReplyAnimation(imagesList: ArrayList<AppCompatImageView>) {

        try {
            set.cancel()
        } catch (e: Exception) {

        }

        for (image in imagesList) {
            image.clearAnimation()
        }
        val arrowOneFadeIn = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 0f, 1f)
        arrowOneFadeIn.duration = animTime.toLong()
        val arrowTwoFadeIn = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 0f, 1f)
        arrowTwoFadeIn.setDuration(animTime.toLong()).startDelay = animTime.toLong()
        val arrowOneFadeOut = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 1f, 0f)
        arrowOneFadeOut.setDuration(animTime.toLong()).startDelay = 375
        val arrowThreeFadeIn = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 0f, 1f)
        arrowThreeFadeIn.setDuration(animTime.toLong()).startDelay = 300
        val arrowTwoFadeOut = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 1f, 0f)
        arrowTwoFadeOut.setDuration(animTime.toLong()).startDelay = 525
        val arrowFourFadeIn = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 0f, 1f)
        arrowFourFadeIn.setDuration(animTime.toLong()).startDelay = 450
        val arrowThreeFadeOut = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 1f, 0f)
        arrowThreeFadeOut.setDuration(animTime.toLong()).startDelay = 675
        val arrowFourFadeOut = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 1f, 0f)
        arrowFourFadeOut.setDuration(animTime.toLong()).startDelay = 825
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                set.start()
            }
        })

        set.play(arrowOneFadeIn)
        set.play(arrowTwoFadeIn)
        set.play(arrowOneFadeOut)
        set.play(arrowThreeFadeIn)
        set.play(arrowTwoFadeOut)
        set.play(arrowFourFadeIn)
        set.play(arrowThreeFadeOut)
        set.play(arrowFourFadeOut)
        set.start()
    }

    private fun startRejectAnimation(imagesList: ArrayList<AppCompatImageView>) {

        try {
            set.cancel()
        } catch (e: Exception) {

        }

        for (image in imagesList) {
            image.clearAnimation()
        }
        val arrowOneFadeIn = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 0f, 1f)
        arrowOneFadeIn.duration = animTime.toLong()
        val arrowTwoFadeIn = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 0f, 1f)
        arrowTwoFadeIn.setDuration(animTime.toLong()).startDelay = animTime.toLong()
        val arrowOneFadeOut = ObjectAnimator.ofFloat(imagesList[3], View.ALPHA, 1f, 0f)
        arrowOneFadeOut.setDuration(animTime.toLong()).startDelay = 375
        val arrowThreeFadeIn = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 0f, 1f)
        arrowThreeFadeIn.setDuration(animTime.toLong()).startDelay = 300
        val arrowTwoFadeOut = ObjectAnimator.ofFloat(imagesList[2], View.ALPHA, 1f, 0f)
        arrowTwoFadeOut.setDuration(animTime.toLong()).startDelay = 525
        val arrowFourFadeIn = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 0f, 1f)
        arrowFourFadeIn.setDuration(animTime.toLong()).startDelay = 450
        val arrowThreeFadeOut = ObjectAnimator.ofFloat(imagesList[1], View.ALPHA, 1f, 0f)
        arrowThreeFadeOut.setDuration(animTime.toLong()).startDelay = 675
        val arrowFourFadeOut = ObjectAnimator.ofFloat(imagesList[0], View.ALPHA, 1f, 0f)
        arrowFourFadeOut.setDuration(animTime.toLong()).startDelay = 825
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                set.start()
            }
        })

        set.play(arrowOneFadeIn)
        set.play(arrowTwoFadeIn)
        set.play(arrowOneFadeOut)
        set.play(arrowThreeFadeIn)
        set.play(arrowTwoFadeOut)
        set.play(arrowFourFadeIn)
        set.play(arrowThreeFadeOut)
        set.play(arrowFourFadeOut)
        set.start()
    }

    private fun onShakeImage() {
        if(mainActivity != null) {
            val shake: Animation = AnimationUtils.loadAnimation(mainActivity, R.anim.shake)
            val slideUp: Animation =
                AnimationUtils.loadAnimation(mainActivity, R.anim.slide_up_call)

            image?.startAnimation(slideUp)

            Handler().postDelayed({
                image?.clearAnimation()
                image?.startAnimation(shake)
            }, 1500)

            Handler().postDelayed({ onShakeImage() }, 2700)
        }

    }

    override fun onItemTouch(swipeView: View) {

        when (swipeView.id) {
            R.id.ivAnswerCall -> {
                llAnswer?.visibility = View.INVISIBLE
                image?.clearAnimation()
            }
            R.id.ivRejectCall -> {
                llAnswer?.visibility = View.INVISIBLE
                llReply?.visibility = View.INVISIBLE
                llReject?.visibility = View.VISIBLE
                tvRejectCall?.visibility = View.VISIBLE
                tvReplyCall?.visibility = View.INVISIBLE
                tvAnswerCall?.visibility = View.INVISIBLE
                countDownTimer?.cancel()
                countDownTimer = object : CountDownTimer(2000, 1000) {
                    override fun onFinish() {
                        llAnswer?.visibility = View.VISIBLE
                        llReject?.visibility = View.INVISIBLE
                        llReply?.visibility = View.INVISIBLE
                        tvRejectCall?.visibility = View.INVISIBLE
                        tvReplyCall?.visibility = View.INVISIBLE
                        tvAnswerCall?.visibility = View.VISIBLE
                    }

                    override fun onTick(millisUntilFinished: Long) {
                    }

                }
                countDownTimer?.start()
            }

            R.id.ivReplyCall -> {
                llAnswer?.visibility = View.INVISIBLE
                llReply?.visibility = View.VISIBLE
                llReject?.visibility = View.INVISIBLE
                tvReplyCall?.visibility = View.VISIBLE
                tvAnswerCall?.visibility = View.INVISIBLE
                tvRejectCall?.visibility = View.INVISIBLE
                countDownTimer?.cancel()
                countDownTimer = object : CountDownTimer(2000, 1000) {
                    override fun onFinish() {
                        llAnswer?.visibility = View.VISIBLE
                        llReject?.visibility = View.INVISIBLE
                        llReply?.visibility = View.INVISIBLE
                        tvReplyCall?.visibility = View.INVISIBLE
                        tvAnswerCall?.visibility = View.VISIBLE
                        tvRejectCall?.visibility = View.INVISIBLE
                    }

                    override fun onTick(millisUntilFinished: Long) {
                    }

                }
                countDownTimer?.start()
            }
        }
    }

    override fun onItemTouchReleased(swipeView: View) {
        when (swipeView.id) {
            R.id.ivAnswerCall -> {
                llAnswer?.visibility = View.VISIBLE
                onShakeImage()
            }
        }
    }

    override fun onItemAnswered(swipeView: View) {
        when (swipeView.id) {
            R.id.ivAnswerCall -> {
                sendDate(WebRTCCallConstants.IncommintJitsiCall.ANSWERCALL)
            }
            R.id.ivRejectCall -> {
                sendDate(WebRTCCallConstants.IncommintJitsiCall.REJECTCALL)
            }
        }
    }
}