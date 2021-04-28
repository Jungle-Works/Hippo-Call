package hipposdk.com.hipposdk;

import android.os.Bundle;

/**
 * Created by gurmail on 2020-03-02.
 *
 * @author gurmail
 */
public class DataClass {

    private static Bundle fuguBundle;
    public static Bundle getFuguBundle() {
        return fuguBundle;
    }

    public static void setFuguBundle(Bundle fuguBundle) {
        DataClass.fuguBundle = fuguBundle;
    }

}
