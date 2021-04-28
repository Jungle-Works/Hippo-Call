package com.hippocall

import com.hippo.constant.FuguAppConstant
import java.util.*

/**
 * Created by gurmail on 09/03/21.
 * @author gurmail
 */
object Links {

    fun getLink(type: String, roomName: String): String {
        //val linkArray = randomVideoConferenceLink()
        var jistsiLink = HippoCallConfig.getInstance().jitsiURL + "/" + roomName
        //var inviteLink = linkArray[0] + "/" + linkArray[1]
        if (type == "AUDIO") {
            jistsiLink += "#config.startWithVideoMuted=true"
        }

        return jistsiLink

//        if (type == "AUDIO") {
//            inviteLink += "#config.startWithVideoMuted=true"
//        }
//        videoCallModel?.jitsiLink = jistsiLink
//        videoCallModel?.inviteLink = inviteLink
    }

    fun randomVideoConferenceLink(): String {
        //val linkArray = ArrayList<String>()
        val ALLOWED_CHARACTERS = "qwertyuiopasdfghjklzxcvbnm"
        val random = Random()
        val sb = StringBuilder(10)
        for (i in 0 until 10)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
//        linkArray.add(FuguAppConstant.CONFERENCING_LIVE)
//        linkArray.add(sb.toString())

        return sb.toString()
    }
}