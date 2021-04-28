package com.hippocall.confcall

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.StrictMode
import android.text.TextUtils
import com.hippo.utils.Utils
import com.hippocall.CommonData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.net.URL
import java.util.*

class NotificationImageManager {
    fun getImageBitmap(link: String): Bitmap? {
        var notificationImageLink: String? = CommonData.getNotificationImage(link)
        var imageBitmap: Bitmap? = null
        try {
            val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)
            if (TextUtils.isEmpty(notificationImageLink) || !File(notificationImageLink).exists()) {
                val randomName = UUID.randomUUID();
                val imageUrl = URL(link)
                val bitmap: Bitmap = BitmapFactory
                    .decodeStream(imageUrl.openConnection().getInputStream())
                val bytes = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                val folder = File(
                    Environment.getExternalStorageDirectory(),
                    "Hippo")
                if (!folder.exists()) {
                    folder.mkdirs()
                }
                val noemedia = File(Environment.getExternalStorageDirectory().toString() + File.separator + "Hippo" + "/.nomedia")
                if (!noemedia.exists()) {
                    noemedia.createNewFile()
                }
                val f = File(Environment.getExternalStorageDirectory(), "Hippo${File.separator}$randomName.png")


                f.createNewFile()
                val fo = FileOutputStream(f)
                fo.write(bytes.toByteArray())
                fo.close()
                notificationImageLink = Environment.getExternalStorageDirectory().toString() + File.separator + "Hippo${File.separator}$randomName.png"
                CommonData.setNotificationImagesMap(link, notificationImageLink)

            }
        } catch (e: Exception) {
            notificationImageLink = ""
            e.printStackTrace()
        }
        imageBitmap = createBitmapFromLink(notificationImageLink!!)
        return imageBitmap
    }

    private fun createBitmapFromLink(link: String): Bitmap? {
        try {
            val bmOptions = BitmapFactory.Options()
            return Utils.getCircleBitmap(BitmapFactory.decodeFile(link, bmOptions))
        } catch (e: Exception) {
            return null
        }
    }
}