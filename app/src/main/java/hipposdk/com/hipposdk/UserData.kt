package hipposdk.com.hipposdk

/**
 * Created by gurmail on 12/06/19.
 * @author gurmail
 */
data class UserData @JvmOverloads constructor(
    var server: String,
    var appType: String?,
    var appSecretKey: String?,
    var userUniqueKey: String?,
    var userName: String?,
    var email: String?,
    var phoneNumber: String?,
    var tag: String?,
    var isPayment: Boolean?,
    var allFiles: Boolean?,
    var isManager: Boolean?,
    var imagePath: String?,
    var lang: String?,
    val isAnnouncementCount: Boolean?
) {

    constructor(server: String) : this(
        server, "", "", "", "", "", "",
        "", false,false, false, "", "", true
    )
}