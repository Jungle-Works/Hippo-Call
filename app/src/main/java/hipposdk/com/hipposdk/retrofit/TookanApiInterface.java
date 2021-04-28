package hipposdk.com.hipposdk.retrofit;


import com.hippo.model.*;
import com.hippo.support.model.HippoSendQueryParams;
import com.hippo.support.model.SupportModelResponse;
import com.hippo.support.model.SupportResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;

import static com.hippo.constant.FuguAppConstant.*;

/**
 * TookanApiInterface
 */
public interface TookanApiInterface {

    @FormUrlEncoded
    @POST("/api/users/putUserDetails")
    Call<FuguPutUserDetailsResponse> putUserDetails(@FieldMap Map<String, Object> map);

}
