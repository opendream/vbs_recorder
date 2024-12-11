package th.co.opendream.vbs_recorder.utils

import android.app.ActivityManager
import android.content.Context

class SettingsUtil(private val context: Context) {
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

    fun setSampleRate(value: Int) {
        sharedPreferencesUtil.putInt(context, SharedPreferencesUtil.KEY_SAMPLE_RATE, value)
    }
    fun getSampleRate(): Int {
        return sharedPreferencesUtil.getInt(context, SharedPreferencesUtil.KEY_SAMPLE_RATE, 44100)
    }

    fun getChunkSizeMs(): Int {
        return sharedPreferencesUtil.getInt(context, SharedPreferencesUtil.KEY_CHUNK_SIZE_MS, 200)
    }
    fun setChunkSizeMs(value: Int) {
        sharedPreferencesUtil.putInt(context, SharedPreferencesUtil.KEY_CHUNK_SIZE_MS, value)
    }

    fun getMaxFileSizeInMB(): Int {
        return sharedPreferencesUtil.getInt(context, SharedPreferencesUtil.KEY_MAX_FILE_SIZE, 3)
    }
    fun setMaxFileSizeInMB(value: Int) {
        sharedPreferencesUtil.putInt(context, SharedPreferencesUtil.KEY_MAX_FILE_SIZE, value)
    }

    fun getKeepEveryNthChunk(): Int {
        return sharedPreferencesUtil.getInt(context, SharedPreferencesUtil.KEY_KEEP_EVERY_NTH_CHUNK, 8)
    }
    fun setKeepEveryNthChunk(value: Int) {
        sharedPreferencesUtil.putInt(context, SharedPreferencesUtil.KEY_KEEP_EVERY_NTH_CHUNK, value)
    }



    companion object {
        val AUDIO_PREFIX = "VBS_audio_"

        val AUDIO_EXTENTION = ".wav"

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