package hipposdk.com.hipposdk.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hipposdk.com.hipposdk.MainActivity
import hipposdk.com.hipposdk.R
import kotlinx.android.synthetic.main.fragment_dashboard.*
import java.util.*
import android.os.CountDownTimer
import android.util.Log
import com.google.gson.Gson
import com.hippo.*
import com.hippo.activity.FuguChatActivity
import com.hippo.constant.FuguAppConstant
import com.hippo.model.FuguConversation
import com.hippo.model.payment.AddedPaymentGateway
import com.hippo.model.promotional.PromotionResponse
import com.hippo.utils.StringUtil
import com.hippo.utils.filepicker.ToastUtil
import com.hippocall.HippoCallConfig
import hipposdk.com.hipposdk.DemoFragment
import kotlin.collections.ArrayList


/**
 * Created by gurmail on 11/06/19.
 * @author gurmail
 */
class Dashboard : Fragment(), NotificationListener {

    var activity: MainActivity? = null
    var isManager: Boolean = false
    var userUniqueKey: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            isManager = arguments!!.getBoolean("isManager", false)
            userUniqueKey = arguments!!.getString("user_unique_key")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(isManager) {
            manager.visibility = View.VISIBLE
            customer.visibility = View.GONE
        } else {
            customer.visibility = View.VISIBLE
            manager.visibility = View.GONE
        }
        // for manager app p2p chat using email
//        var str: String = keys?.text.toString()
//        HippoConfig.getInstance().startOneToOneConversation(str);

        set_time?.setOnClickListener {

            //openHelpChannel(activity!!)
//            val sec = editTextTime.text.toString().toLong()
//            HippoConfig.getInstance().setRideTime(sec)

            val intent = newAppDetailsIntent("com.test.hipposdk")
            activity?.startActivity(intent)
        }

        stop_time.setOnClickListener {
            HippoConfig.getInstance().stopOnlineStatus(activity)
//            HippoConfig.getInstance().openDevicePermission(activity)
        }

        btnJoin.setOnClickListener {
            val image = "https://thumbs.dreamstime.com/b/cute-boy-cartoon-illustration-87282832.jpg"
            val transactionId: String = editTextJoin.text.toString().trim()
            HippoConfig.getInstance().joinGroupCall(activity, transactionId, image)
//            HippoConfig.getInstance().openDevicePermission(activity)
        }



        btnMgCreateChat.setOnClickListener {
            val transactionId: String = mg_transaction_id.text.toString().trim()
            val otheruserUniqueKey: String = mg_user_unique_key.text.toString().trim()


//            HippoConfig.getInstance().openConversationFor(activity, String.valueOf(order.getCustomerId()),
//                order.getCustomerId()+"-"+TextUtil.getTerminology().getPROJECT(),order.getJobId()+"_"+order.getCustomerId()+"_"+order.getMerchantId());

        }


        btnCampaign.setOnClickListener {
            val attributes: MobileCampaignBuilder = MobileCampaignBuilder.Builder()
                .setParseFormat("dd, MMM h:mm a")
//                .setClearText(<for_clear>)
//                .setDeleteMessage(<delete_message>)
//                .hideDownloadBtn(true)// for button visibility GONE/VISIBLE
//                .setNotificationTitle(<TITLE>)
//                .setEmptyNotificationText(<WHEN_NO_NOTIFICATION>)
                .build()
            HippoConfig.getInstance().openMobileCampaigns(activity, attributes)
        }



        btnCreateChat.setOnClickListener {
            val groupingTags: ArrayList<String> = ArrayList()
            groupingTags.add("Test1")
            groupingTags.add("Test2")

            val transactionId: String = et_transaction_id.text.toString().trim()
            val channelName: String = et_channel_name.text.toString().trim()
            val otheruserUniqueKey: String = et_user_unique_key.text.toString().trim()
            val groupTagsStr: String = et_chat_tags.text.toString().trim()

            val attrBUilder = ChatByUniqueIdAttributes.Builder()
            attrBUilder.setTransactionId(transactionId)
            attrBUilder.setChannelName(null)
            attrBUilder.setUserUniqueKey(userUniqueKey)
            attrBUilder.setInsertBotId(true)

            if(!TextUtils.isEmpty(otheruserUniqueKey)) {
                val stList = Arrays.asList(*otheruserUniqueKey.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                val otherUserUniqueKeys = ArrayList<String>()
                for (str in stList) {
                    otherUserUniqueKeys.add(str.trim())
                }
                attrBUilder.setOtherUserUniqueKeys(otherUserUniqueKeys)
            }

            if(!TextUtils.isEmpty(groupTagsStr)) {
                val stList = Arrays.asList(*groupTagsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                val groupingTags = ArrayList<String>()
                for (str in stList) {
                    groupingTags.add(str.trim())
                }
                attrBUilder.setGroupingTags(groupingTags)
            }

            HippoConfig.getInstance().openChatByUniqueId(activity,attrBUilder.build())
        }

        btnPrePayment.setOnClickListener {
//            val groupingTags: ArrayList<String> = ArrayList()
//            groupingTags.add("Test1")
//            groupingTags.add("Test2")

            val transactionId: String = et_transaction_id.text.toString().trim()
            val channelName: String = et_channel_name.text.toString().trim()
            val otheruserUniqueKey: String = et_user_unique_key.text.toString().trim()
            val groupTagsStr: String = et_chat_tags.text.toString().trim()
            val gatewayId: String = et_gateways.text.toString().trim()

            val currencySymbol = HippoConfig.getInstance().getCurrencySymbol(otheruserUniqueKey)
            val prePayment = HippoPrePaymentBuilder.Builder()
                .paymentGatewayId(gatewayId.toInt())
                .amount(channelName)
                .currency(otheruserUniqueKey)
                .currencySymbol(currencySymbol)
                .transactionId(transactionId)
                .paymentType(2)
                .description("This is a test description")
                .title(groupTagsStr)
                .message("This is a test message for testing")
                .build();

//            val attrBUilder = ChatByUniqueIdAttributes.Builder()
//            attrBUilder.setTransactionId(transactionId)
//            attrBUilder.setChannelName(null)
//            attrBUilder.setUserUniqueKey(userUniqueKey)
//            attrBUilder.setInsertBotId(true)
//
//            if(!TextUtils.isEmpty(otheruserUniqueKey)) {
//                val stList = Arrays.asList(*otheruserUniqueKey.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
//                val otherUserUniqueKeys = ArrayList<String>()
//                for (str in stList) {
//                    otherUserUniqueKeys.add(str.trim())
//                }
//                attrBUilder.setOtherUserUniqueKeys(otherUserUniqueKeys)
//            }
//
//            if(!TextUtils.isEmpty(groupTagsStr)) {
//                val stList = Arrays.asList(*groupTagsStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
//                val groupingTags = ArrayList<String>()
//                for (str in stList) {
//                    groupingTags.add(str.trim())
//                }
//                attrBUilder.setGroupingTags(groupingTags)
//            }



            HippoConfig.getInstance().openPrePaymentView(activity, prePayment)

        }

        btnSetLang.setOnClickListener {
            val transactionId: String = editTextTime.text.toString().trim()
            HippoConfig.getInstance().updateLanguage(transactionId)
        }
        btnUnreadCount.setOnClickListener {

            val transactionId: String = et_transaction_id.text.toString().trim()
            val OtheruserUniqueKey: String = et_user_unique_key.text.toString().trim()

            val attrBUilder = ChatByUniqueIdAttributes.Builder()
            attrBUilder.setTransactionId(transactionId)
            attrBUilder.setUserUniqueKey(userUniqueKey)

            if(!TextUtils.isEmpty(OtheruserUniqueKey)) {
                val stList = Arrays.asList(*OtheruserUniqueKey.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                val otherUserUniqueKeys = ArrayList<String>()
                for (str in stList) {
                    otherUserUniqueKeys.add(str.trim())
                }
                attrBUilder.setOtherUserUniqueKeys(otherUserUniqueKeys)
            }

            HippoConfig.getInstance().fetchUnreadCountForRequest(attrBUilder.build(), object : UnreadCountFor{
                override fun unreadCountFor(transactionId: String?, count: Int) {
                    println("Count = $count, transactionId = $transactionId")
                    Log.e("Count", "Count = $count, transactionId = $transactionId")
                    ToastUtil.getInstance(getActivity()).showToast("Count = $count, transactionId = $transactionId")
                }
            })
0
//            HippoConfig.getInstance().fetchUnreadCountForRequest(attrBUilder.build()) {
//                   transactionId, count -> ToastUtil.getInstance(getActivity()).showToast("Count = $count, transactionId = $transactionId")
//            }

//            val transactionId: String = et_transaction_id.text.toString().trim()
//            HippoConfig.getInstance().updateLanguage(transactionId)
            //getPaymentMethods()


            /*HippoCallConfig.getInstance().openOnGoingCall(activity, object : OnScreenChangeListener {
                override fun onScreenStatus(flag: Boolean) {
                    if(flag)
                        ToastUtil.getInstance(activity).showToast("Screen opened")
                    else
                        ToastUtil.getInstance(activity).showToast("Screen not opened")
                }
            })*/
        }

        audio_call.setOnClickListener {
            val data = et_call_user_unique_key.text.toString().trim()
            val stList = Arrays.asList(*data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            val otherUserUniqueKeys = ArrayList<String>()
            for (str in stList) {
                otherUserUniqueKeys.add(str.trim())
            }
            val otherUserName = et_call_user_name.text.toString().trim()
            val transactionId = et_call_transaction_id.text.toString().trim()
            val otherUserUniqueKey = et_call_user_unique_key.text.toString().trim()
            var path = et_call_image_url.text.toString().trim()
            if (TextUtils.isEmpty(path))
                path = "http://www.stickpng.com/assets/images/585e4bcdcb11b227491c3396.png"


            HippoCallConfig.getInstance().setCallBackListener()
//
//
            val agentEmail = et_call_email.text.toString().trim()

//            HippoCallConfig.getInstance().directAgentCallHooks(activity, "audio", transactionId,
//                userUniqueKey, agentEmail, "Test", path)

            val userKey = HippoConfig.getInstance().userData.userUniqueKey


            HippoConfig.getInstance().startCall(activity, "audio", transactionId, userKey, "otherUserName",
                    otherUserUniqueKeys, path, "myName", "myImagePath")

        }

        video_call.setOnClickListener {
            val transactionId = et_call_transaction_id.text.toString().trim()
            val otherUserName = et_call_user_unique_key.text.toString().trim()
            var path = et_call_image_url.text.toString().trim()

            val data = et_call_user_unique_key.text.toString().trim()
            val stList = Arrays.asList(*data.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            val otherUserUniqueKeys = ArrayList<String>()
            for (str in stList) {
                otherUserUniqueKeys.add(str.trim())
            }

            if (TextUtils.isEmpty(path))
                path = "http://www.stickpng.com/assets/images/585e4bcdcb11b227491c3396.png"

//            HippoCallConfig.getInstance().hasExtraView(false)
//            HippoCallConfig.getInstance().setFragment(DemoFragment())
//
////            HippoConfig.getInstance().startCall(activity, "video", transactionId,
////                userUniqueKey, otherUserName, otherUserUniqueKeys, path)
//
//
//            val agentEmail = et_call_email.text.toString().trim()
//            HippoCallConfig.getInstance().directAgentCallHooks(activity, "video", transactionId,
//                userUniqueKey, agentEmail, "Test", path)

            /*Handler().postDelayed({
                HippoCallConfig.getInstance().timerVisibility(View.VISIBLE)
                object : CountDownTimer(20000, 1000) {

                    override fun onTick(millisUntilFinished: Long) {
                        HippoCallConfig.getInstance().setTimer("Time: ${millisUntilFinished / 1000}")
                    }

                    override fun onFinish() {
                        HippoCallConfig.getInstance().timerVisibility(View.GONE)
                    }

                }.start()
            }, 5000)*/

        }

    }

    override fun onSucessListener(response: PromotionResponse) {

    }

    override fun onFailureListener() {

    }

    override fun onItemClickListener(data: String?) {
        ToastUtil.getInstance(activity).showToast("Clicked: $data")
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    private fun getPaymentMethods() {
        HippoConfig.getInstance().getPaymentMethods(activity!!, "", object: PrePaymentCallBack {
            override fun onPaymentSuccess() {
                ToastUtil.getInstance(activity).showToast("Payment success")
            }

            override fun onPaymentfailed() {
                ToastUtil.getInstance(activity).showToast("Payment failed")
            }

            override fun onMethodReceived(prepaymentList: List<AddedPaymentGateway>) {
                Log.v("List", "Data = "+Gson().toJson(prepaymentList))
            }
        })
    }

    fun openHelpChannel(mActivity: Activity) {
        val chatIntent = Intent(mActivity, FuguChatActivity::class.java)
        val conversation = FuguConversation()
        conversation.labelId = 5428602L

        conversation.label = ""
        conversation.isOpenChat = true
        conversation.userName =
            StringUtil.toCamelCase(HippoConfig.getInstance().userData.fullName)
        conversation.userId = HippoConfig.getInstance().userData.userId
        conversation.enUserId = HippoConfig.getInstance().userData.enUserId
        chatIntent?.putExtra(FuguAppConstant.CONVERSATION, Gson().toJson(conversation, FuguConversation::class.java))
        if (!(mActivity.isFinishing)) {
            mActivity.startActivity(chatIntent)
        }
    }

    public fun newAppDetailsIntent(packageName: String): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + packageName));
        return intent;
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.FROYO) {
        val intent = Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName("com.android.settings",
                "com.android.settings.InstalledAppDetails");
        intent.putExtra("pkg", packageName);
        return intent;
    }
    val intent = Intent(Intent.ACTION_VIEW);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setClassName("com.android.settings",
            "com.android.settings.InstalledAppDetails");
    intent.putExtra("com.android.settings.ApplicationPkgName", packageName);
    return intent;
}


}
