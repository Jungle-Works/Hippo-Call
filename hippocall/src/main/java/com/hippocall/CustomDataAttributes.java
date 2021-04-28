package com.hippocall;

import android.text.TextUtils;

/**
 * Created by gurmail on 19/06/19.
 *
 * @author gurmail
 */
public class CustomDataAttributes {

    private String uniqueId;
    private String flag;
    private String message;

    public String getUniqueId() {
        return uniqueId;
    }

    public String getFlag() {
        return flag;
    }

    public String getMessage() {
        return message;
    }

    public static class Builder {

        private String uniqueId;
        private String flag;
        private String message;

        private CustomDataAttributes attributes = new CustomDataAttributes(this);

        public Builder setUniqueId(String uniqueId) {
            attributes.uniqueId = uniqueId;
            return this;
        }

        public Builder setFlag(String flag) {
            attributes.flag = flag;
            return this;
        }

        public Builder setMessage(String message) {
            attributes.message = message;
            return this;
        }

        public CustomDataAttributes build() {
            if (TextUtils.isEmpty(attributes.uniqueId)) {
                throw new IllegalStateException("UniqueId can not be empty!");
            } else if (TextUtils.isEmpty(attributes.flag)) {
                throw new IllegalStateException("Flag can not be empty!");
            }
            return attributes;
        }
    }

    private CustomDataAttributes(Builder builder) {
        this.uniqueId = builder.uniqueId;
        this.flag = builder.flag;
        this.message = builder.message;
    }
}
