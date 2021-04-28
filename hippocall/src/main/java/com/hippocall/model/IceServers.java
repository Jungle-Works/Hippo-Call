
package com.hippocall.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class IceServers {

    @SerializedName("stun")
    @Expose
    private List<String> stun = new ArrayList<>();
    @SerializedName("turn")
    @Expose
    private List<String> turn = new ArrayList<>();

    public List<String> getStun() {
        return stun;
    }

    public void setStun(List<String> stun) {
        this.stun = stun;
    }

    public List<String> getTurn() {
        return turn;
    }

    public void setTurn(List<String> turn) {
        this.turn = turn;
    }

}
