package com.hippocall;

import android.text.TextUtils;

/**
 * Created by gurmail on 19/06/19.
 *
 * @author gurmail
 */
public class StringAttributes {

    private String muteString;
    private String cameraOffString;
    private String andString;
    private String videoPaused;
    private boolean showUserName;

    public String getMuteString() {
        return muteString;
    }

    public String getCameraOffString() {
        return cameraOffString;
    }

    public String getAndString() {
        return andString;
    }

    public boolean isShowUserName() {
        return showUserName;
    }

    public String getVideoPaused() {
        return videoPaused;
    }

    public static class Builder {

        private String muteString;
        private String cameraOffString;
        private String andString;
        private String videoPaused;
        private boolean showUserName;

        private StringAttributes attributes = new StringAttributes(this);

        public Builder setMuteString(String muteString) {
            attributes.muteString = muteString;
            return this;
        }

        public Builder setCameraOffString(String cameraOffString) {
            attributes.cameraOffString = cameraOffString;
            return this;
        }

        public Builder setAndString(String andString) {
            attributes.andString = andString;
            return this;
        }

        public Builder setVideoPaused(String videoPaused) {
            attributes.videoPaused = videoPaused;
            return this;
        }

        public Builder showUserName(boolean showUserName) {
            attributes.showUserName = showUserName;
            return this;
        }

        public StringAttributes build() {
            return attributes;
        }
    }

    private StringAttributes(Builder builder) {
        this.muteString = builder.muteString;
        this.cameraOffString = builder.cameraOffString;
        this.andString = builder.andString;
        this.videoPaused = builder.videoPaused;
        this.showUserName = builder.showUserName;
    }
}
