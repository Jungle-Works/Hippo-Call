package com.hippocall

/**
 * Created by rajatdhamija on 21/09/18.
 */
interface WebRTCCallConstants {
    companion object {
        val USER_ID = "user_id"
        val FULL_NAME = "full_name"
        val MESSAGE_TYPE = "message_type"
        val VIDEO_CALL = 18
        val GROUP_CALL = 27
        val IS_TYPING = "is_typing"
        val MESSAGE_UNIQUE_ID = "muid"
        val DEVICE_ID = "device_id"
        val DEVICE_TYPE = "device_type"
        val DEVICE_DETAILS = "device_details"
        val VIDEO_CALL_TYPE = "video_call_type"
        val ANDROID_USER = 1
        val TURN_CREDENTIALS = "turn_creds"
        val SDP_M_LINE_INDEX = "sdpMLineIndex"
        val SDP_MID = "sdpMid"
        val CANDIDATE = "candidate"
        val RTC_CANDIDATE = "rtc_candidate"
        val IS_SILENT = "is_silent"
        val SDP = "sdp"
        val LOCAL_SET_REMOTE_DESC = "localSetRemoteDesc"
        val REMOTE_SET_REMOTE_DESC = "remoteSetRemoteDesc"
        val REMOTE_SET_LOCAL_DESC = "remoteSetLocalDesc"
        val OFFER_TO_RECEIVE_AUDIO = "offerToReceiveAudio"
        val OFFER_TO_RECEIVE_VIDEO = "offerToReceiveVideo"
        val LOCAL_CREATE_OFFER = "localCreateOffer"
        val REMOTE_CREATE_OFFER = "remoteCreateOffer"
        val USER_NAME = "username"
        val CREDENTIAL = "credential"
        val TURN_API_KEY = "turnApiKey"
        val STUN_SERVERS = "stunServers"
        val TURN_SERVERS = "turnServers"
        val CALL_STATUS = "call_status"
        val CALL_TIMER = "call_timer"
        val ONGOING_VIDEO_CALL = "Ongoing Video Call..."
        val ONGOING_AUDIO_CALL = "Ongoing Audio Call..."
        val DEVICE_PAYLOAD = "device_payload"
        val STUN = "stun"
        val TURN = "turn"
        val CONNECTING = "CONNECTING..."
        val CALLING = "CALLING..."
        val RINGING = "RINGING..."
        val DISCONNECTING = "DISCONNECTING..."
        val USER_BUSY = "Busy on another call..."
        val REJECTED = "Call Declined"
        val VIDEO_CALL_HUNGUP_FROM_NOTIFICATION = "VIDEO_CALL_HUNGUP_FROM_NOTIFICATION"

        val START_CALL="START_CALL"
        val READY_TO_CONNECT="READY_TO_CONNECT"
        val CALL_HUNG_UP="CALL_HUNG_UP"
        val CALL_REJECTED="CALL_REJECTED"
        val VIDEO_OFFER="VIDEO_OFFER"
        val NEW_ICE_CANDIDATE="NEW_ICE_CANDIDATE"
        val VIDEO_ANSWER="VIDEO_ANSWER"
        val SWITCH_TO_CONFERENCE="SWITCH_TO_CONFERENCE"
        val HUNGUP_TYPE = "hungup_type"
        val CALL_TYPE = "call_type"
        val CUSTOM_DATA: String = "custom_data"
        var USER_THUMBNAIL_IMAGE = "user_thumbnail_image"

        /*enum class BusFragmentType {
            INCOMMING_JITSI_CALL {
                override fun toString(): String {
                    return "INCOMMING_JITSI_CALL"
                }
            },
            JITSI_CALL {
                override fun toString(): String {
                    return "JITSI_CALL"
                }
            },
            INCOMING_VIDEO_CONF {
                override fun toString(): String {
                    return "INCOMING_VIDEO_CONF"
                }
            },
            VIDEO_CONF {
                override fun toString(): String {
                    return "VIDEO_CONF"
                }
            }
        }*/
        enum class VideoCallType {
            START_CALL {
                override fun toString(): String {
                    return "START_CALL"
                }
            },
            READY_TO_CONNECT {
                override fun toString(): String {
                    return "READY_TO_CONNECT"
                }
            },
            CALL_HUNG_UP {
                override fun toString(): String {
                    return "CALL_HUNG_UP"
                }
            },
            CALL_REJECTED {
                override fun toString(): String {
                    return "CALL_REJECTED"
                }
            },
            VIDEO_OFFER {
                override fun toString(): String {
                    return "VIDEO_OFFER"
                }
            },
            NEW_ICE_CANDIDATE {
                override fun toString(): String {
                    return "NEW_ICE_CANDIDATE"
                }
            },
            USER_BUSY {
                override fun toString(): String {
                    return "USER_BUSY"
                }
            },
            VIDEO_ANSWER {
                override fun toString(): String {
                    return "VIDEO_ANSWER"
                }
            },
            SWITCH_TO_CONFERENCE {
                override fun toString(): String {
                    return "SWITCH_TO_CONFERENCE"
                }
            }
        }
    }

    enum class AcitivityLaunchState {
        SELF {
            override fun toString(): String {
                return "SELF"
            }
        },
        OTHER {
            override fun toString(): String {
                return "OTHER"
            }
        },
        KILLED {
            override fun toString(): String {
                return "KILLED"
            }
        },
    }
    enum class CallStatus {
        IN_CALL {
            override fun toString(): String {
                return "IN_CALL"
            }
        },
        INCOMING_CALL {
            override fun toString(): String {
                return "INCOMING_CALL"
            }
        },
        OUTGOING_CALL {
            override fun toString(): String {
                return "OUTGOING_CALL "
            }
        },
    }

    enum class CallType {
        AUDIO {
            override fun toString(): String {
                return "AUDIO"
            }
        },
        VIDEO {
            override fun toString(): String {
                return "VIDEO"
            }
        },
        VOICE {
            override fun toString(): String {
                return "VOICE"
            }
        }
    }

    enum class VideoCallType {
        START_CALL {
            override fun toString(): String {
                return "START_CALL"
            }
        },
        READY_TO_CONNECT {
            override fun toString(): String {
                return "READY_TO_CONNECT"
            }
        },
        CALL_HUNG_UP {
            override fun toString(): String {
                return "CALL_HUNG_UP"
            }
        },
        CALL_REJECTED {
            override fun toString(): String {
                return "CALL_REJECTED"
            }
        },
        VIDEO_OFFER {
            override fun toString(): String {
                return "VIDEO_OFFER"
            }
        },
        NEW_ICE_CANDIDATE {
            override fun toString(): String {
                return "NEW_ICE_CANDIDATE"
            }
        },
        USER_BUSY {
            override fun toString(): String {
                return "USER_BUSY"
            }
        },
        VIDEO_ANSWER {
            override fun toString(): String {
                return "VIDEO_ANSWER"
            }
        },
        SWITCH_TO_CONFERENCE {
            override fun toString(): String {
                return "SWITCH_TO_CONFERENCE"
            }
        },
        CUSTOM_DATA {
            override fun toString(): String {
                return "CUSTOM_DATA"
            }
        },
        CALL_ACTION {
            override fun toString(): String {
                return "CALL_ACTION"
            }
        },
        START_GROUP_CALL {
            override fun toString(): String {
                return "START_GROUP_CALL"
            }
        },
        JOIN_GROUP_CALL {
            override fun toString(): String {
                return "JOIN_GROUP_CALL"
            }
        },
        REJECT_GROUP_CALL {
            override fun toString(): String {
                return "REJECT_GROUP_CALL"
            }
        },
        END_GROUP_CALL {
            override fun toString(): String {
                return "END_GROUP_CALL"
            }
        }

    }

    /*enum class VideoCallType {
        START_CALL {
            override fun toString(): String {
                return "START_CALL"
            }
        },
        READY_TO_CONNECT {
            override fun toString(): String {
                return "READY_TO_CONNECT"
            }
        },
        CALL_HUNG_UP {
            override fun toString(): String {
                return "CALL_HUNG_UP"
            }
        },
        CALL_REJECTED {
            override fun toString(): String {
                return "CALL_REJECTED"
            }
        },
        VIDEO_OFFER {
            override fun toString(): String {
                return "VIDEO_OFFER"
            }
        },
        NEW_ICE_CANDIDATE {
            override fun toString(): String {
                return "NEW_ICE_CANDIDATE"
            }
        },
        USER_BUSY {
            override fun toString(): String {
                return "USER_BUSY"
            }
        },
        VIDEO_ANSWER {
            override fun toString(): String {
                return "VIDEO_ANSWER"
            }
        },
        SWITCH_TO_CONFERENCE {
            override fun toString(): String {
                return "SWITCH_TO_CONFERENCE"
            }
        }
    }*/

    enum class JitsiCallType {
        START_CONFERENCE {
            override fun toString(): String {
                return "START_CONFERENCE"
            }
        },
        START_CONFERENCE_IOS {
            override fun toString(): String {
                return "START_CONFERENCE_IOS"
            }
        },
        OFFER_CONFERENCE{
            override fun toString(): String {
                return "OFFER_CONFERENCE"
            }
        },
        READY_TO_CONNECT_CONFERENCE {
            override fun toString(): String {
                return "READY_TO_CONNECT_CONFERENCE"
            }
        },
        READY_TO_CONNECT_CONFERENCE_IOS {
            override fun toString(): String {
                return "READY_TO_CONNECT_CONFERENCE"
            }
        },
        HUNGUP_CONFERENCE {
            override fun toString(): String {
                return "HUNGUP_CONFERENCE"
            }
        },
        REJECT_CONFERENCE {
            override fun toString(): String {
                return "REJECT_CONFERENCE"
            }
        },
        USER_BUSY_CONFERENCE {
            override fun toString(): String {
                return "USER_BUSY_CONFERENCE"
            }
        },
        ANSWER_CONFERENCE {
            override fun toString(): String {
                return "ANSWER_CONFERENCE"
            }
        }
    }

    enum class BusFragmentType {
        INCOMMING_JITSI_CALL {
            override fun toString(): String {
                return "INCOMMING_JITSI_CALL"
            }
        },
        INCOMMING_GROUP_CALL {
            override fun toString(): String {
                return "INCOMMING_GROUP_CALL"
            }
        },
        JITSI_CALL {
            override fun toString(): String {
                return "JITSI_CALL"
            }
        },
        INCOMING_VIDEO_CONF {
            override fun toString(): String {
                return "INCOMING_VIDEO_CONF"
            }
        },
        MAIN_CALL {
            override fun toString(): String {
                return "MAIN_CALL"
            }
        },
        CALL_HUNGUP {
            override fun toString(): String {
                return "CALL_HUNGUP"
            }
        },
        UPDATE_INCOMIMG_CONFIG {
            override fun toString(): String {
                return "UPDATE_INCOMIMG_CONFIG"
            }
        }
    }

    interface IncommintJitsiCall {
        companion object {
            val START_MEDIA = 0
            val REGISTER_BROADCAST = 1
            val UNREGISTER_BROADCAST = 2
            val STOP = 3
            val ANSWERCALL = 4
            val REJECTCALL = 5
        }
    }

    interface JitsiCallActivity {
        companion object {
            val POST_DATA = 0
            val OPEN_VIDEO_CONF = 1
            val PRE_LOAD_DATA = 2
            val OPEN_OLD_CALL = 3
        }
    }
}