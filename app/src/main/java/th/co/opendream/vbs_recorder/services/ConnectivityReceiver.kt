package th.co.opendream.vbs_recorder.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import th.co.opendream.vbs_recorder.utils.NetworkUtil

class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (NetworkUtil.isInternetConnected(context)) {
            // Start the sync service
            val syncIntent = Intent(context, S3UploaderBulkSyncService::class.java)
            context.startService(syncIntent)
        }
    }

    companion object {
        fun registerReceiver(context: Context) {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.registerReceiver(ConnectivityReceiver(), filter)
        }
    }
}