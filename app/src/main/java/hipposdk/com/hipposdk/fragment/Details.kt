package hipposdk.com.hipposdk.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hippo.HippoConfig
import hipposdk.com.hipposdk.InitActivity
import hipposdk.com.hipposdk.MainActivity
import hipposdk.com.hipposdk.R
import io.paperdb.Paper
import kotlinx.android.synthetic.main.fragment_details.*
import org.w3c.dom.Text

/**
 * Created by gurmail on 11/06/19.
 * @author gurmail
 */
class Details : Fragment() {

    var email: String? = null
    var secretKey: String? = null
    var uniqueKey: String? = null

    var activity: MainActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = getActivity() as MainActivity?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            email = arguments?.getString("email")
            secretKey = arguments?.getString("secretKey")
            uniqueKey = arguments?.getString("uniqueKey")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logout.setOnClickListener {
            //Paper.init(activity)
            Paper.book("demo").destroy()
            HippoConfig.clearHippoData(activity)
            activity?.startActivity(Intent(activity, InitActivity::class.java))
            activity?.finish()
        }

        email_card.visibility = View.GONE
        secret_key_card.visibility = View.GONE
        unique_key_card.visibility = View.GONE

        if(!TextUtils.isEmpty(email)) {
            email_card.visibility = View.VISIBLE
            txtemail.text = email
        }

        if(!TextUtils.isEmpty(secretKey)) {
            secret_key_card.visibility = View.VISIBLE
            secret_key.text = secretKey
        }

        if(!TextUtils.isEmpty(uniqueKey)) {
            unique_key_card.visibility = View.VISIBLE
            unique_key.text = uniqueKey
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()

    }
}