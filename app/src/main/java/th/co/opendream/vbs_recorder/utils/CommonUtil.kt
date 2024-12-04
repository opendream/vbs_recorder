package th.co.opendream.vbs_recorder.utils

import android.app.ActivityManager
import android.content.Context
import th.co.opendream.vbs_recorder.BuildConfig

class CommonUtil(private val context: Context) {
    private var sharedPreferencesUtil: SharedPreferencesUtil = SharedPreferencesUtil()

    fun getFilePrefix(): String {
        var value = sharedPreferencesUtil.getString(context, SharedPreferencesUtil.KEY_FILE_PREFIX)
        if (value == null) {
            value = "vbs"
        }
        return value
    }
    fun setFilePrefix(value: String) {
        sharedPreferencesUtil.putString(context, SharedPreferencesUtil.KEY_FILE_PREFIX, value)
    }

    fun getCanUploadToS3(): Boolean {
        return sharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.KEY_CAN_UPLOAD_TO_S3, true)
    }
    fun setUploadToS3(value: Boolean) {
        sharedPreferencesUtil.putBoolean(context, SharedPreferencesUtil.KEY_CAN_UPLOAD_TO_S3, value)
    }

    fun getMetadata(): String {
        var value = sharedPreferencesUtil.getString(context, SharedPreferencesUtil.KEY_METADATA)
        if (value == null) {
            value = ""
        }
        return value

    }
    fun setMetadata(value: String) {
        sharedPreferencesUtil.putString(context, SharedPreferencesUtil.KEY_METADATA, value)
    }

    fun getS3BucketName(): String? {
        return sharedPreferencesUtil.getString(context, SharedPreferencesUtil.KEY_AWS_S3_BUCKET_NAME)
    }
    fun setS3BucketName(value: String) {
        sharedPreferencesUtil.putString(context, SharedPreferencesUtil.KEY_AWS_S3_BUCKET_NAME, value)
    }

    fun getS3Region(): String? {
        return sharedPreferencesUtil.getString(context, SharedPreferencesUtil.KEY_AWS_S3_REGION)
    }
    fun setS3Region(value: String) {
        sharedPreferencesUtil.putString(context, SharedPreferencesUtil.KEY_AWS_S3_REGION, value)
    }

    fun getS3AccessKey(): String? {
        return sharedPreferencesUtil.getString(context, SharedPreferencesUtil.KEY_AWS_S3_ACCESS_KEY)
    }
    fun setS3AccessKey(value: String) {
        sharedPreferencesUtil.putString(
            context,
            SharedPreferencesUtil.KEY_AWS_S3_ACCESS_KEY,
            value
        )
    }

    fun getS3SecretKey(): String? {
        return sharedPreferencesUtil.getString(context, SharedPreferencesUtil.KEY_AWS_S3_SECRET_KEY)
    }
    fun setS3SecretKey(value: String) {
        sharedPreferencesUtil.putString(
            context,
            SharedPreferencesUtil.KEY_AWS_S3_SECRET_KEY,
            value
        )
    }


    companion object {
        val AUDIO_PREFIX = "VBS_audio_"

        val AUDIO_MINETYPE = "audio/x-wav"
        val AUDIO_EXTENTION = ".wav"

        val AUDIO_SEGMENT_DURATION: Long = 10000
        val AUDIO_FILE_SIZE_LIMIT: Long = 1 * 1024 * 1024 // 1MB

        var S3_BUCKET_NAME: String = ""
        var S3_REGION: String = ""
        var S3_ACCESS_KEY: String = ""
        var S3_SECRET_KEY: String = ""

        fun zeroPadding (number: Int): String {
            val paddedNumber = String.format("%05d", number)
            return paddedNumber
        }

        fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}