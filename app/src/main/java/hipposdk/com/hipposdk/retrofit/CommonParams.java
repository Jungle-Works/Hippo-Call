package hipposdk.com.hipposdk.retrofit;

import com.hippo.BuildConfig;
import com.hippo.HippoConfig;

import java.util.HashMap;

import static com.hippo.constant.FuguAppConstant.*;

/**
 * Created by cl-macmini-33 on 27/09/16.
 */

public class CommonParams {
    HashMap<String, Object> map = new HashMap<>();

    private CommonParams(Builder builder, int sourceType) {
        builder.map.put(APP_SOURCE_TYPE, String.valueOf(sourceType));
        builder.map.put(APP_VERSION, HippoConfig.getInstance().getVersionCode());
        //builder.map.put(APP_VERSION_CODE, BuildConfig.VERSION_CODE);
        builder.map.put(DEVICE_TYPE, ANDROID_USER);
        this.map = builder.map;
    }

    public HashMap<String, Object> getMap() {
        return map;
    }


    public static class Builder {
        HashMap<String, Object> map = new HashMap<>();

        public Builder() {
        }

        public Builder add(String key, Object value) {
            map.put(key, String.valueOf(value));
            return this;
        }

        public Builder addAll(HashMap<String, Object> objectHashMap) {
            map.putAll(objectHashMap);
            return this;
        }
        /**
         * Temp fix for future use of common key inside CommonParams constr.
         * @param map
         * @return
         */
        public Builder putMap(HashMap<String, Object> map) {
            this.map = map;
            return this;
        }

        public CommonParams build() {
            return new CommonParams(this, 1);
        }

        public CommonParams build(int sourceType) {
            return new CommonParams(this, sourceType);
        }

    }
}


