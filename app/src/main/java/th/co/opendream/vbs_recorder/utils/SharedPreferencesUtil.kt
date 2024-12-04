package th.co.opendream.vbs_recorder.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesUtil {
    private val PREFS_NAME = "th.co.opendream.vbs_recorder.app_prefs"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPreferences(context).getBoolean(key, defaultValue)
    }

    fun putString(context: Context, key: String, value: String) {
        val editor = getPreferences(context).edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(context: Context, key: String, defaultValue: String? = null): String? {
        return getPreferences(context).getString(key, defaultValue)
    }

    companion object {
        val KEY_FILE_PREFIX = "FILE_PREFIX"

        val KEY_AWS_S3_REGION = "AWS_S3_REGION"
        val KEY_AWS_S3_BUCKET_NAME = "AWS_S3_BUCKET_NAME"
        val KEY_AWS_S3_ACCESS_KEY = "AWS_S3_ACCESS_KEY"
        val KEY_AWS_S3_SECRET_KEY = "AWS_S3_SECRET_KEY"

        val KEY_CAN_UPLOAD_TO_S3 = "CAN_UPLOAD_TO_S3"
        val KEY_METADATA = "METADATA"

    }

}