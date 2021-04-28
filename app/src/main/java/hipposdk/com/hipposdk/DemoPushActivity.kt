package hipposdk.com.hipposdk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hippo.utils.filepicker.ToastUtil

/**
 * Created by gurmail on 2020-01-08.
 * @author gurmail
 */
class DemoPushActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ToastUtil.getInstance(this@DemoPushActivity).showToast("This is a demo class")
    }
}