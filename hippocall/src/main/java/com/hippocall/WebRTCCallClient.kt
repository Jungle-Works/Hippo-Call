package com.hippocall

import com.hippo.constant.FuguAppConstant
import com.hippo.utils.HippoLog
import com.hippocall.WebRTCCallConstants.Companion.CANDIDATE
import com.hippocall.WebRTCCallConstants.Companion.LOCAL_SET_REMOTE_DESC
import com.hippocall.WebRTCCallConstants.Companion.REMOTE_SET_LOCAL_DESC
import com.hippocall.WebRTCCallConstants.Companion.REMOTE_SET_REMOTE_DESC
import com.hippocall.WebRTCCallConstants.Companion.RTC_CANDIDATE
import com.hippocall.WebRTCCallConstants.Companion.SDP
import com.hippocall.WebRTCCallConstants.Companion.SDP_MID
import com.hippocall.WebRTCCallConstants.Companion.SDP_M_LINE_INDEX
import com.hippocall.WebRTCCallConstants.Companion.VIDEO_CALL_TYPE
import org.json.JSONObject
import org.webrtc.*
import java.util.*

/**`
 * Created by rajatdhamija
 * 20/09/18.
 */

class WebRTCCallClient(private val videoCallService: VideoCallService) {
    private var iceServers: MutableList<PeerConnection.IceServer> = ArrayList()
    var peerConnection: PeerConnection? = null
    var videoOffer: JSONObject? = null
    fun createPeerConnection(connection: Connection?): PeerConnection? {
        if (peerConnection == null) {
            //Logging.enableLogToDebugOutput(Logging.Severity.LS_VERBOSE)
            setIceServers(connection)
            val rtcConfiguration = getRTCConfig()
            peerConnection = connection?.peerConnectionFactory?.createPeerConnection(rtcConfiguration,
                object : CustomPeerConnectionObserver("PeerConnectionCallBack", videoCallService) {

                    override fun onIceCandidate(iceCandidate: IceCandidate?) {
                        super.onIceCandidate(iceCandidate)
                        //CommonData.setConnectionModel(connection)
                        val json = JSONObject()
                        json.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.NEW_ICE_CANDIDATE.toString())
                        val rtc_candidate = JSONObject()
                        rtc_candidate.put(SDP_M_LINE_INDEX, iceCandidate?.sdpMLineIndex)
                        rtc_candidate.put(SDP_MID, iceCandidate?.sdpMid)
                        rtc_candidate.put(CANDIDATE, iceCandidate?.sdp)
                        json.put(RTC_CANDIDATE, rtc_candidate)
                        videoCallService.webRTCSignallingClient?.sendIceCandidates(json)
                    }

                    override fun onAddStream(mediaStream: MediaStream?) {
                        super.onAddStream(mediaStream)
                        videoCallService.onAddStream(mediaStream)
                    }

                    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                        super.onIceConnectionChange(iceConnectionState)
                        HippoLog.e("onIceConnectionChange", "onIceConnectionChange = ${iceConnectionState!!.name}")
                        when (iceConnectionState!!.name) {
                            "CONNECTED", "COMPLETED", "connected", "completed" -> {
                                videoCallService.onConnected()
                            }
                            "DISCONNECTED", "disconnected" -> {
                                videoCallService.onDisconnected()
                            }
                            "FAILED", "failed", "CLOSED", "closed" -> {
                                videoCallService.onCallFailed()
                            }
                        }

                    }

                })!!
        }
        return peerConnection
    }

    fun createOffer(connection: Connection?): JSONObject? {
        try {
            if (videoOffer == null) {
                peerConnection?.createOffer(object : CustomSdpObserver("Create Offer Callback") {
                    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                        super.onCreateSuccess(sessionDescription)
                        peerConnection?.setLocalDescription(CustomSdpObserver(LOCAL_SET_REMOTE_DESC), sessionDescription)
                        val offerObject = JSONObject()
                        val sdpObject = JSONObject()
                        offerObject.put(VIDEO_CALL_TYPE, WebRTCCallConstants.Companion.VideoCallType.VIDEO_OFFER)
                        sdpObject.put(SDP, sessionDescription?.description)
                        sdpObject.put("type", "offer")
                        offerObject.put(SDP, sdpObject)
                        videoOffer = videoCallService.webRTCSignallingClient?.sendOfferToRemoteUser(offerObject)
                    }
                }, connection?.sdpConstraints)
            }
            return videoOffer
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun saveAnswer(answerJson: JSONObject) {
        val jsonObject = answerJson.getJSONObject(SDP)
        val sessionDescription: SessionDescription
        sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, jsonObject.getString(SDP))
        setRemoteDescription(sessionDescription)
    }

    fun saveOfferAndAnswer(jsonObject: JSONObject?, connection: Connection?) {
        val offerJson = jsonObject?.getJSONObject(SDP)
        if (peerConnection != null) {
            val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offerJson?.getString(SDP))
            peerConnection?.setRemoteDescription(CustomSdpObserver(REMOTE_SET_REMOTE_DESC), sessionDescription)
            peerConnection?.createAnswer(object : CustomSdpObserver("CreateAnswerCallBack") {
                override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                    super.onCreateSuccess(sessionDescription)
                    peerConnection?.setLocalDescription(CustomSdpObserver(REMOTE_SET_LOCAL_DESC), sessionDescription)
                    val json = JSONObject()
                    json.put(VIDEO_CALL_TYPE, WebRTCCallConstants.VideoCallType.VIDEO_ANSWER)
                    val sdpObject = JSONObject()
                    sdpObject.put("type", "answer")
                    sdpObject.put(SDP, sessionDescription?.description)
                    json.put(SDP, sdpObject)
                    videoCallService.webRTCSignallingClient?.sendAnswerToRemoteUser(json)
                }
            }, MediaConstraints())
        }
    }

    private fun setRemoteDescription(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(CustomSdpObserver(LOCAL_SET_REMOTE_DESC), sessionDescription)
    }

    private fun setIceServers(connection: Connection?) {
        for (stun in connection?.stunServers!!) {
            val stunIceServer = PeerConnection.IceServer.builder(stun)
                .createIceServer()
            iceServers.add(stunIceServer)
        }
        for (turn in connection.turnServers!!) {
            val turnIceServer = PeerConnection.IceServer.builder(turn)
                .setUsername(connection.turnUserName)
                .setPassword(connection.turnCredential)
                .createIceServer()
            iceServers.add(turnIceServer)
        }
    }

    private fun getRTCConfig(): PeerConnection.RTCConfiguration {
        val rtcConfiguration = PeerConnection.RTCConfiguration(iceServers)
        rtcConfiguration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.PLAN_B

        // This code getting from signal APP
        rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
        rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE

        return rtcConfiguration
    }

    fun saveIceCandidate(jsonObject: JSONObject?) {
        if (peerConnection != null) {
            val rtc_candidate = jsonObject?.getJSONObject(RTC_CANDIDATE)
            val iceCandidate = IceCandidate(rtc_candidate?.getString(SDP_MID),
                Integer.parseInt(rtc_candidate?.getString(SDP_M_LINE_INDEX)),
                rtc_candidate?.getString(CANDIDATE))
            peerConnection?.addIceCandidate(iceCandidate)
        }
    }
}
