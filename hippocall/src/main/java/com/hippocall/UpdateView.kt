package com.hippocall

import androidx.fragment.app.Fragment
import org.json.JSONObject

/**
 * Created by gurmail on 06/06/19.
 * @author gurmail
 */
interface UpdateView {

    fun updateFragment(fragment: androidx.fragment.app.Fragment?)

    fun timerVisibilityStatus(status: Int)
    fun updateTimer(time: String)
    fun sendCustomData(data: JSONObject)
    fun onNetworkStatusChange(status: Int)

}