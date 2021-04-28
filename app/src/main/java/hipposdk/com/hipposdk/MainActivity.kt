package hipposdk.com.hipposdk

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.iid.FirebaseInstanceId
import com.hippo.*
import com.hippo.HippoNotificationConfig.handleHippoPushNotification
import hipposdk.com.hipposdk.fragment.Dashboard
import hipposdk.com.hipposdk.fragment.Details
import hipposdk.com.hipposdk.fragment.HomeFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.logging.Level
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    var attributes: HippoConfigAttributes? = null
    var token: String? = null

    var server: String? = null
    var isManager: Boolean = false
    var app_type: String? = null
    var app_secret_key: String? = null
    var user_unique_key: String? = null
    var user_name: String = ""
    var email: String? = ""
    var phone: String? = null
    var tags: String? = null
    var image: String? = null
    var lang: String? = ""

    var isBroadcastEnabled: Boolean = true
    var isPaymentEnabled: Boolean = true
    var allFiles: Boolean = true
    var isAnnouncementCount: Boolean = true
    var isForking: Boolean = false
    val authToken: String = "42413.163469c2b50a0f1b1c152d3c10112c4018ecc2ff2f"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        server = intent.getStringExtra("server")
        isManager = intent.getBooleanExtra("is_manager", false)
        isPaymentEnabled = intent.getBooleanExtra("isPaymentEnabled", true)
        allFiles = intent.getBooleanExtra("allFiles", true)
        isAnnouncementCount = intent.getBooleanExtra("isAnnouncementCount", true)
        app_type = intent.getStringExtra("app_type")
        app_secret_key = intent.getStringExtra("app_secret_key")
        user_unique_key = intent.getStringExtra("user_unique_key")
        if (intent.hasExtra("user_name"))
            user_name = intent.getStringExtra("user_name").toString()
        if (intent.hasExtra("email"))
            email = intent.getStringExtra("email")
        if (intent.hasExtra("phone"))
            phone = intent.getStringExtra("phone")
        if (intent.hasExtra("tags"))
            tags = intent.getStringExtra("tags")
        if (intent.hasExtra("image_path"))
            image = intent.getStringExtra("lang")
        if (intent.hasExtra("image_path"))
            lang = intent.getStringExtra("lang")



        loadFragment(homeFragment())
        navigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> loadFragment(homeFragment())
                R.id.navigation_dashboard -> loadFragment(dashBoardFragment())
                R.id.navigation_details -> loadFragment(detailsFragment())
            }
            return@setOnNavigationItemSelectedListener true
        }

        FirebaseInstanceId.getInstance().instanceId
                .addOnSuccessListener(this@MainActivity) { instanceIdResult ->
                    val newToken = instanceIdResult.token
                    Log.e("newToken", newToken)
                    token = newToken
                    initSDK()
                }


    }


    override fun onResume() {
        super.onResume()
    }

    fun initSDK() {

        //hippoStatusBar

        val fuguColorConfig = HippoColorConfig.Builder()
//                .hippoSendBtnBg(FuguColorConfigStrings.FUGU_ACTION_BAR_BG)
//            .hippoActionBarBg(FuguColorConfigStrings.FUGU_ACTION_BAR_BG)
//            .hippoStatusBar(FuguColorConfigStrings.FUGU_STATUS_BAR)
//            .hippoActionBarText(FuguColorConfigStrings.FUGU_ACTION_BAR_TEXT)
//
//            .hippoFloatingBtnBg(FuguColorConfigStrings.FUGU_ACTION_BAR_BG)
//            .hippoFloatingBtnText(FuguColorConfigStrings.FUGU_ACTION_BAR_TEXT)

//            .hippoBgMessageYou(FuguColorConfigStrings.FUGU_BG_MESSAGE_YOU)
//            .hippoBgMessageFrom(FuguColorConfigStrings.FUGU_BG_MESSAGE_FROM)
//            .hippoPrimaryTextMsgYou(FuguColorConfigStrings.FUGU_PRIMARY_TEXT_MSG_YOU)
//            .hippoMessageRead(FuguColorConfigStrings.FUGU_MESSAGE_READ)
//            .hippoPrimaryTextMsgFrom(FuguColorConfigStrings.FUGU_PRIMARY_TEXT_MSG_FROM)
//            .hippoSecondaryTextMsgYou(FuguColorConfigStrings.FUGU_SECONDARY_TEXT_MSG_YOU)
//            .hippoSecondaryTextMsgFrom(FuguColorConfigStrings.FUGU_SECONDARY_TEXT_MSG_FROM)
//            .hippoTextColorPrimary(FuguColorConfigStrings.FUGU_TEXT_COLOR_PRIMARY)
//            .hippoChannelDateText(FuguColorConfigStrings.FUGU_CHANNEL_DATE_TEXT)
//            .hippoChatBg(FuguColorConfigStrings.FUGU_CHAT_BG)
//            .hippoBorderColor(FuguColorConfigStrings.FUGU_BORDER_COLOR)
//            .hippoChatDateText(FuguColorConfigStrings.FUGU_CHAT_DATE_TEXT)
//            .hippoThemeColorPrimary(FuguColorConfigStrings.FUGU_THEME_COLOR_PRIMARY)
//            .hippoThemeColorSecondary(FuguColorConfigStrings.FUGU_THEME_COLOR_SECONDARY)
//            .hippoTypeMessageBg(FuguColorConfigStrings.FUGU_TYPE_MESSAGE_BG)
//            .hippoTypeMessageHint(FuguColorConfigStrings.FUGU_TYPE_MESSAGE_HINT)
//            .hippoTypeMessageText(FuguColorConfigStrings.FUGU_TYPE_MESSAGE_TEXT)
//            .hippoChannelBg(FuguColorConfigStrings.FUGU_CHANNEL_BG)
//            .hippoSecondaryTextMsgFromName(FuguColorConfigStrings.FUGU_ACTION_BAR_BG)
                .build()


        // for woowa live driver
//        user_unique_key = "driver281522"
//        app_secret_key = "60e955e207e74fa1105cbf5a9d9104c5"
//        user_name = "test"
//        server = "beta-live"

        val captureUserData: CaptureUserData = CaptureUserData.Builder()
                .userUniqueKey(user_unique_key)
                .fullName(user_name)
                .phoneNumber(phone)
                .email(email)
//                .addressLine1("plot no 5")
//                .addressLine2("CDCL Building")
//                .city("Chandigarh")
//                .region("Chandigarh")
//                .country("India")
//                .zipCode("160019")
//                .latitude(30.16)
//                .longitude(76.34)
//                .fetchBusinessLang(true)
//                .setLang(lang)
                .build()

        image = "https://thumbs.dreamstime.com/b/cute-boy-cartoon-illustration-87282832.jpg"
        //image = "https://rukminim1.flixcart.com/image/704/704/poster/t/g/n/iron-man-paper-poster-12-x18-with-4-acrylic-sticker-free-original-imaej3pmqpcxzgjs.jpeg"

        val additionalInfo = AdditionalInfo.Builder()
//                .needDeviceOptimization(true)
//                .prePaymentMethodFetched(true)
                .hasChannelPager(true)
                .hasCreateNewChat(true)
                .replyOnFeedback(true)
                .showAgentImage(true)
                .showEmptyChatBtn(true)
//                .isAnnouncementCount(isAnnouncementCount)
                .build()

        val customData: HashMap<String, Any> = HashMap()
        customData["city"] = "Test"
        customData["first_name"] = "Android"
        customData["email"] = "test@yopmail.com"
        customData["order"] = 1
        customData["debt_amount"] = 100


        val attributes = HippoConfigAttributes.Builder()
            .setAppKey(app_secret_key)
            .setAppType(app_type)
            .setManager(isManager)
            .setAppType(app_type)
            .setCaptureUserData(captureUserData)
            .setEnvironment(server)
            .setProvider("com.test.hipposdk.provider")
            .setUnreadCount(true)
            .isShareAllFileTypes(allFiles)
            .setShowLog(true)
            .setDeviceToken(token)
            .setBroadcastEnabled(isBroadcastEnabled)
            .setPaymentEnabled(isPaymentEnabled)
            .isForking(isForking)
            .setAuthToken(app_secret_key)
            .setImagePath(image)
//            .setColorConfig(fuguColorConfig)
                .setAdditionalInfo(additionalInfo)
                .setCustomAttributes(customData)
                .build()


        HippoConfig.initHippoConfig(this, attributes, object : HippoInitCallback {
            override fun onPutUserResponse() {
                //Toast.makeText(this@MainActivity, "onPutUserResponse", Toast.LENGTH_LONG).show()
            }

            override fun onErrorResponse() {
                //Toast.makeText(this@MainActivity, "onErrorResponse", Toast.LENGTH_LONG).show()
            }

            override fun hasData() {
                //Toast.makeText(this@MainActivity, "hasData", Toast.LENGTH_LONG).show()
            }

        }, DataClass.getFuguBundle())
        HippoConfig.getInstance().isSetSkipNumber = false
        HippoConfig.getInstance().setShowCreateBtn(true)

        handleHippoPushNotification(this@MainActivity, DataClass.getFuguBundle())

        HippoConfig.getInstance().setApiListener(object : OnApiCallback {
            override fun onSucess() {
                Log.e("TAG", "API sucess")
            }

            override fun onFailure(errorMessage: String?) {
                Log.e("TAG", "API failed " + errorMessage)
            }

            override fun onProcessing() {
                Log.e("TAG", "API in progress")
            }

        })

        HippoConfig.getInstance().hippoAdditionalListener = object : HippoAdditionalListener {
            override fun onNoChatListener() {

            }
        }

        /*HippoCallConfig.getInstance().setCallBackListener()
        HippoCallConfig.getInstance().hippoCallPushIcon = R.drawable.ic_home_black_24dp
//        HippoCallConfig.getInstance().hasExtraView(true)
//        HippoCallConfig.getInstance().setFragment(DemoFragment())

        HippoConfig.getInstance().icSend = R.mipmap.ic_send_normal
        //HippoConfig.getInstance().setHomeUpIndicatorDrawableId(R.drawable.ic_home_black_24dp)
        HippoCallConfig.getInstance().setSurfaceMirror(true, false)*/


//        questions[1] = "How much time it will take to reach?"
//        questions[2] = "Are you coming?"
//        questions[3] = "Come to location shown in the app"
//        questions[4] = "Where are you"
//        questions[5] = "I am on my way"


        questions["How much time it will take to reach?"] = 1
        questions["Are you coming?"] = 2
        questions["Come to location shown in the app"] = 3
        questions["Where are you"] = 4
        questions["I am on my way"] = 5


        suggestions[1] = "How much time it will take to reach?"
        suggestions[2] = "Are you coming?"
        suggestions[3] = "Come to location shown in the app"
        suggestions[4] = "Where are you"
        suggestions[5] = "I am on my way"
        suggestions[6] = "Ok"
        suggestions[7] = "2 mins"
        suggestions[8] = "5 mins"
        suggestions[9] = "10 mins"
        suggestions[10] = "Yes, on my way"

        // for customer
//        mapping[-1] = arrayListOf(1, 2, 3)
//        mapping[0] = arrayListOf(6)
//        mapping[1] = arrayListOf(2, 3)
//        mapping[2] = arrayListOf(1, 3)
//        mapping[3] = arrayListOf(1, 2)
//        mapping[5] = arrayListOf(6)


        // for driver
        mapping[-1] = arrayListOf(4, 5)
        mapping[0] = arrayListOf(6)
        mapping[1] = arrayListOf(7, 8, 9)
        mapping[2] = arrayListOf(10)
        mapping[3] = arrayListOf(6)


        HippoConfig.getInstance().questions = questions
        HippoConfig.getInstance().suggestions = suggestions
        HippoConfig.getInstance().mapping = mapping
        HippoConfig.getInstance()
    }

    private var questions = HashMap<String, Int>()
    private var suggestions = HashMap<Int, String>()
    private var mapping = HashMap<Int, ArrayList<Int>>()

    private var homeFragment: HomeFragment? = null
    private var dashBoardFragment: Dashboard? = null
    private var detailsFragment: Details? = null

    private fun homeFragment(): Fragment {
        if (homeFragment == null) {
            val bdl = Bundle()
            bdl.putBoolean("isManager", isManager)
            homeFragment = HomeFragment()
            homeFragment!!.arguments = bdl
        }
        return homeFragment!!
    }

    private fun dashBoardFragment(): Dashboard {
        if (dashBoardFragment == null) {
            val bdl = Bundle()
            bdl.putBoolean("isManager", isManager)
            bdl.putString("user_unique_key", user_unique_key)
            dashBoardFragment = Dashboard()
            dashBoardFragment!!.arguments = bdl
        }
        return dashBoardFragment!!
    }

    private fun detailsFragment(): Details {
        if (detailsFragment == null) {
            val bdl = Bundle()
            bdl.putBoolean("isManager", isManager)
            bdl.putString("email", email)
            bdl.putString("secretKey", app_secret_key)
            bdl.putString("uniqueKey", user_unique_key)

            detailsFragment = Details()
            detailsFragment!!.arguments = bdl
        }
        return detailsFragment!!
    }

    // load fragment
    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
    }


    //==================================== for conversation type apps ==============================//


}