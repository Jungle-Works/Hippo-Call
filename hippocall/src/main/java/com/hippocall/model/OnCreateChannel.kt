package com.hippocall.model

import com.hippocall.VideoCallModel

/**
 * Created by gurmail on 21/01/21.
 * @author gurmail
 */
data class OnCreateChannel(val isChannel: Boolean, val channelId: Long, val videoCallModel: VideoCallModel)