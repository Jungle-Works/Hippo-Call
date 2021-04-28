package hipposdk.com.hipposdk

import android.content.Context
import com.hippo.LibApp
import com.hippo.activity.HippoActivityLifecycleCallback
import com.hippo.utils.HippoLog
import com.hippocall.HippoCallConfig
//import com.razorpay.Checkout
import io.paperdb.Paper

/**
 * Created by gurmail on 02/01/19.
 * @author gurmail
 */
public class MyApplication : LibApp() {
    override fun onCallBtnClick(
        context: Context?,
        callType: Int,
        channelId: Long?,
        userId: Long?,
        isAgentFlow: Boolean,
        isAllowCall: Boolean,
        fullname: String?,
        image: String?,
        myImagePath: String?
    ) {

    }

    override fun trackEvent(category: String?, action: String?, label: String?) {
        
    }


    /*override fun onCreate() {
        HippoActivityLifecycleCallback.register(this)
        HippoCallConfig.getInstance().setCallBackListener(this)
        super.onCreate()
        Paper.init(applicationContext)

        ..... your code
    }*/

    override fun onCreate() {
        HippoActivityLifecycleCallback.register(this)
        HippoCallConfig.getInstance().setCallBackListener(this)
        super.onCreate()
        Paper.init(applicationContext)

    }

    override fun screenOpened(screenName: String?) {

    }

    override fun openMainScreen() {

    }

    override fun onTerminate() {
        HippoLog.w("TAG", "MyApplication onTerminate")
        super.onTerminate()
    }


}