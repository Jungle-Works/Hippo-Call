//package hipposdk.com.hipposdk.retrofit;
//
//import com.hippo.agent.model.FuguAgentGetMessageParams;
//import com.hippo.agent.model.FuguAgentGetMessageResponse;
//import com.hippo.agent.model.GetConversationResponse;
//import com.hippo.agent.model.LoginResponse;
//import com.hippo.agent.model.broadcastResponse.BroadcastModel;
//import com.hippo.agent.model.broadcastStatus.BroadcastResponseModel;
//import com.hippo.agent.model.createConversation.CreateConversation;
//import com.hippo.agent.model.unreadResponse.UnreadCountResponse;
//import com.hippo.agent.model.user_details.UserDetailsResponse;
//import com.hippo.constant.FuguAppConstant;
//import com.hippo.model.FuguUploadImageResponse;
//import okhttp3.MultipartBody;
//import okhttp3.RequestBody;
//import retrofit2.Call;
//import retrofit2.http.*;
//
//import java.util.Map;
//
//import static com.hippo.constant.FuguAppConstant.APP_VERSION;
//import static com.hippo.constant.FuguAppConstant.DEVICE_TYPE;
//
///**
// * Created by gurmail on 18/06/18.
// *
// * @author gurmail
// */
//
//public interface DemoApiInterface {
//
//    @FormUrlEncoded
//    @POST("/api/agent/v1/agentLogin")
//    Call<LoginResponse> login(@FieldMap Map<String, Object> map);
//
//}
