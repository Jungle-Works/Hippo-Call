package hipposdk.com.hipposdk.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.hippo.HippoConfig
import com.hippo.UnreadCount
import com.hippo.payment.RazorPayData
import com.hippo.utils.HippoLog
import hipposdk.com.hipposdk.HomeFragmentActivity
import hipposdk.com.hipposdk.MainActivity
import hipposdk.com.hipposdk.R
import kotlinx.android.synthetic.main.fragment_home.*
import java.util.*

/**
 * Created by gurmail on 11/06/19.
 * @author gurmail
 */
class HomeFragment : Fragment() {

    var activity: MainActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        show_conversation.setOnClickListener {
                HippoConfig.getInstance().showConversations(activity, "")
//            HippoConfig.getInstance().isHideBackBtn = true
//            startActivity(Intent(activity!!, HomeFragmentActivity::class.java))
        }

        setId.setOnClickListener {
            try {
                if(!TextUtils.isEmpty(et_bot_id.text.toString().trim())) {
                    val data: Int = et_bot_id.text.toString().trim().toInt()
                    HippoConfig.getInstance().setBotGroupId(data)
                } else {
                    HippoConfig.getInstance().setBotGroupId(null)
                }
            } catch (e: Exception) {
                HippoConfig.getInstance().setBotGroupId(null)
            }

            //HippoConfig.getInstance().demo(mContext)

        }

        payBtn.setOnClickListener {
//            val data: String = et_bot_id.text.toString().trim()
//            val razorPayData = RazorPayData("order_FsEb41610aaU2e", 123141, "8146151596",
//                "gurmil@yopmail.com", "This is a test payment",
//                321241, 1.00, "INR", "Gurmail")
//            HippoConfig.getInstance().openRazorpaySDK(activity, razorPayData)

            HippoConfig.getInstance().getUnreadCount()
        }

        HippoConfig.getInstance().callbackListener = object : UnreadCount {
            override fun count(count: Int) {
                try {
                    HippoLog.v("count", "count = "+count)
                    if(unreadCountTxt != null) {
                        activity?.runOnUiThread {
                            unreadCountTxt.text = "" + count
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            override fun unreadAnnouncementsCount(count: Int) {
//                try {
//                    HippoLog.v("count", "count = "+count)
//                    if(unreadCountTxt != null) {
//                        activity?.runOnUiThread {
//                            unreadCountTxt.text = "" + count
//                        }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
            }
            override fun unreadCountFor(count: Int) {

            }
        }

    }

    var mContext: Context? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        activity = getActivity() as MainActivity?
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    override fun onResume() {
        super.onResume()
        HippoConfig.getInstance().getUnreadCount()
        //unreadCountTxt.text = ""+HippoConfig.getInstance().unreadAnnouncementsCount
    }
}