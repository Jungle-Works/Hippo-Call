package hipposdk.com.hipposdk

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.layout_bottom.*

/**
 * Created by gurmail on 07/06/19.
 * @author gurmail
 */
class DemoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_bottom, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        click.setOnClickListener {
//            val attributes: CustomDataAttributes = CustomDataAttributes.Builder()
//                .setFlag("demo flag")
//                .setUniqueId("Job123")
//                .setMessage("Start clicked")
//                .build()
//
//            HippoCallConfig.getInstance().sendCustomData(activity, attributes)
        }
        seekBar_start.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(progress>80) {
                    seekBar_start?.progress = 100
                    stop_translation.visibility = View.VISIBLE
                    start_translation.visibility = View.GONE
                    seekBar_start?.progress = 7
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })

    seekBar_stop.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(progress>80) {
                    seekBar_stop?.progress = 100
                    stop_translation.visibility = View.GONE
                    start_translation.visibility = View.VISIBLE
                    seekBar_stop?.progress = 7
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                stop_translation.visibility = View.GONE
                start_translation.visibility = View.VISIBLE
            }
        })

    }
}