package th.co.opendream.vbs_recorder.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.models.Record
import th.co.opendream.vbs_recorder.utils.SettingsUtil
import th.co.opendream.vbs_recorder.utils.DateUtil
import java.io.File

class S3UploaderBulkSyncService : Service() {
    private lateinit var transferUtility: TransferUtility

    private var db: VBSDatabase? = null

    private var s3Client: AmazonS3Client? = null
    private var settingsUtil: SettingsUtil? = null

    private var syncRecords: ArrayList<Record> = ArrayList()
    private var syncedSet: MutableSet<String> = mutableSetOf()
    private var bucketName: String? = null


    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(
            applicationContext,
            VBSDatabase::class.java,
            "vbs_database"
        ).build()

        settingsUtil = SettingsUtil(applicationContext)

        // init s3client only when we have the necessary credentials
        bucketName = settingsUtil!!.getS3BucketName()
        if (bucketName == null) {
            Log.e("S3UploaderBulkSyncService", "S3 bucket name is not set")
            return
        }
        val credentials = BasicAWSCredentials(settingsUtil!!.getS3AccessKey(), settingsUtil!!.getS3SecretKey())
        val regionName = settingsUtil!!.getS3Region()
        if (regionName == null) {
            Log.e("S3UploaderBulkSyncService", "S3 region is not set")
            return
        }

        s3Client = AmazonS3Client(credentials, Region.getRegion(Regions.fromName(regionName)))
        transferUtility = TransferUtility.builder().s3Client(s3Client).context(applicationContext).build()

        createNotificationChannel()
        startForeground(SERVICE_ID, createNotification())
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Sync Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Syncing data Service")
            .setContentText("Data syncing in progress")
            .setSmallIcon(R.drawable.baseline_record_voice_over_24)
            .build()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val canUpload = settingsUtil!!.getCanUploadToS3()

        if (!canUpload || bucketName == null || s3Client == null) {
            Log.e("S3UploaderBulkSyncService", "Upload to S3 is disabled")
            stopSelf()
            return START_NOT_STICKY
        }


        // Perform the sync operation here
        CoroutineScope(Dispatchers.IO).launch {

            val fetchData = db!!.recordDao().filterBySynced(false)
            if (fetchData.isNotEmpty()) {

                for (record in fetchData) {
                    syncRecords.add(record)
                }

                for (record in fetchData) {
                    val filePath = "${record.filePath}${SettingsUtil.AUDIO_EXTENTION}"
                    val fileKey = "${record.filePath}${SettingsUtil.AUDIO_EXTENTION}"
                    val bucketName = settingsUtil!!.getS3BucketName()

                    Log.i("S3UploaderBulkSyncService", "Syncing file: $filePath")
                    uploadFileToS3(filePath, bucketName!!, fileKey, record)
                }

            } else {
                Log.e("S3UploaderService", "No data to sync")
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun uploadFileToS3(filePath: String, bucketName: String, key: String, record: Record?) {

        val uploadFilePath = readAudioFileFromMediaStore(contentResolver, filePath) ?: return

        val file = File(uploadFilePath)
        val syncUrl = s3Client!!.getUrl(bucketName, key).toString()

        Log.e("S3UploaderService", "Uploading file: $filePath")
        Log.e("S3UploaderService", "Uploading file: $uploadFilePath")
        Log.e("S3UploaderService", "To: $syncUrl")

        val metadataText = settingsUtil!!.getMetadata()
        val metadata = ObjectMetadata()

        metadata.addUserMetadata("metadata", metadataText)
        metadata.addUserMetadata("KeepEveryNthChunk", settingsUtil!!.getKeepEveryNthChunk().toString())
        metadata.addUserMetadata("ChunkLength", settingsUtil!!.getChunkSizeMs().toString())


        val uploadObserver = transferUtility.upload(bucketName, key, file, metadata)
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    // Handle successful upload
                    saveToDB(syncUrl, record)
                    Log.e("S3UploaderBulkSyncService", "Upload completed: $uploadFilePath")

                    syncedSet.add(record!!.filePath!!)
                    if (syncedSet.size == syncRecords.size) {
                        Log.e("S3UploaderBulkSyncService", "BulkSyncService COMPLETED")

                        sendBroadcast(Intent("SYNC_COMPLETED"))
                        stopSelf() // Stop the service after sync completed
                    }


                } else if (state == TransferState.FAILED) {
                    // Handle failed upload
                    Log.e("S3UploaderService", "Upload failed")
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                // Handle progress update
            }

            override fun onError(id: Int, ex: Exception?) {
                // Handle error
                Log.e("S3UploaderBulkSyncService", "Error during upload", ex)

                syncedSet.add(record!!.filePath!!)

                Log.e("S3UploaderBulkSyncService", "Error during upload " + syncedSet.size.toString() + " " + syncRecords.size.toString())

                if (syncedSet.size == syncRecords.size) {
                    Log.e("S3UploaderBulkSyncService", "BulkSyncService COMPLETED")

                    sendBroadcast(Intent("SYNC_COMPLETED"))
                    stopSelf() // Stop the service after sync completed
                }
            }
        })
    }

    private fun readAudioFileFromMediaStore(contentResolver: ContentResolver, fileName: String): String? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
        )
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                return path
            }
        }
        return ""
    }

    private fun saveToDB(syncFilePath: String, record: Record?) {
        if (record == null) {
            return
        }

        record.syncedPath = syncFilePath
        record.syncedAt = DateUtil.currentTimeToLong()
        record.isSynced = true


        CoroutineScope(Dispatchers.IO).launch {
            db!!.recordDao().update(record)
        }


    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        var CHANNEL_ID = "th.co.opendream.vbs_recorder.NotificationServiceChannel"
        var SERVICE_ID = 102
        var ACTION_SYNC_COMPLETED = "SYNC_COMPLETED"
    }

}