package com.hippocall;

import com.hippocall.model.IceServers;
import com.hippocall.model.Message;

import java.util.ArrayList;

/**
 * Created by gurmail on 04/01/19.
 *
 * @author gurmail
 */
class AppContants {

    AppContants() {

    }

    public Message getTurnCredentials() {
        Message turnCredentials = new Message();
        turnCredentials.setCredentials("3FXCGBCnDfqsrOqs");
        turnCredentials.setUsername("fuguadmin");

        IceServers iceServers = new IceServers();
        ArrayList<String> stringStun = new ArrayList<>();
        stringStun.add("stun:turnserver.fuguchat.com:19305");
        iceServers.setStun(stringStun);

        ArrayList<String> stringTurn = new ArrayList<>();
        stringTurn.add("turn:turnserver.fuguchat.com:19305?transport=UDP");
        stringTurn.add("turn:turnserver.fuguchat.com:19305?transport=TCP");
        stringTurn.add("turns:turnserver.fuguchat.com:5349?transport=UDP");
        stringTurn.add("turns:turnserver.fuguchat.com:5349?transport=TCP");
        iceServers.setTurn(stringTurn);
        turnCredentials.setIceServers(iceServers);

        turnCredentials.setTurnApiKey("VPlwuCJcizZye2znMflmJ75x0IraJ5cQ");


//        turnCredentials.setCredentials("s0LLXpZ/nA6ZcqE1yqmTui6bmgA=");
//        turnCredentials.setUsername("CI7m++gFEgZWtYHDrWMYzc/s6OMTIICjBQ");
//
//        IceServers iceServers = new IceServers();
//        ArrayList<String> stringStun = new ArrayList<>();
//        stringStun.add("stun:172.217.31.30:19302");
//        stringStun.add("stun:[2404:6800:4002:802::201e]:19302");
//
//        iceServers.setStun(stringStun);
//
//        ArrayList<String> stringTurn = new ArrayList<>();
//        stringTurn.add("turn:74.125.200.127:19305?transport=udp");
//        stringTurn.add("turn:[2404:6800:4003:c00::7f]:19305?transport=udp");
//        stringTurn.add("turn:74.125.200.127:19305?transport=tcp");
//        stringTurn.add("turn:[2404:6800:4003:c00::7f]:19305?transport=tcp");
//        iceServers.setTurn(stringTurn);
//        turnCredentials.setIceServers(iceServers);
//
//
//
//        turnCredentials.setTurnApiKey("");
//        turnCredentials.setBlockStatus("NOT_BLOCKED");
//        turnCredentials.setLifetimeDuration("86400s");
//        turnCredentials.setIceTransportPolicy("all");



        return turnCredentials;
    }
}
