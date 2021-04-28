
package com.hippocall.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("turn_api_key")
    @Expose
    private String turnApiKey = "";
    @SerializedName("ice_servers")
    @Expose
    private IceServers iceServers=new IceServers();
    @SerializedName("username")
    @Expose
    private String username = "";
    @SerializedName("credential")
    @Expose
    private String credentials = "";

//    @SerializedName("lifetimeDuration")
//    @Expose
//    private String lifetimeDuration;
//    @SerializedName("blockStatus")
//    @Expose
//    private String blockStatus;
//    @SerializedName("iceTransportPolicy")
//    @Expose
//    private String iceTransportPolicy;
//
//    public String getLifetimeDuration() {
//        return lifetimeDuration;
//    }
//
//    public void setLifetimeDuration(String lifetimeDuration) {
//        this.lifetimeDuration = lifetimeDuration;
//    }
//
//    public String getIceTransportPolicy() {
//        return iceTransportPolicy;
//    }
//
//    public void setIceTransportPolicy(String iceTransportPolicy) {
//        this.iceTransportPolicy = iceTransportPolicy;
//    }
//
//    public String getBlockStatus() {
//        return blockStatus;
//    }
//
//    public void setBlockStatus(String blockStatus) {
//        this.blockStatus = blockStatus;
//    }

    public String getTurnApiKey() {
        return turnApiKey;
    }

    public void setTurnApiKey(String turnApiKey) {
        this.turnApiKey = turnApiKey;
    }

    public IceServers getIceServers() {
        return iceServers;
    }

    public void setIceServers(IceServers iceServers) {
        this.iceServers = iceServers;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

}
