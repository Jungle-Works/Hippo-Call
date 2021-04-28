package hipposdk.com.hipposdk.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 */

public class CommonResponse {

    @SerializedName("statusCode")
    @Expose
    private String statusCode;
    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("data")
    @Expose
    private Object data;


    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return statusCode + " " + message + "\n" + data;
    }
}
