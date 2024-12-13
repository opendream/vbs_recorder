package th.co.opendream.vbs_recorder.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.activities.MainActivity
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.processors.PcmToWavFileConverter
import th.co.opendream.vbs_recorder.processors.realtime.AudioProcessor
import th.co.opendream.vbs_recorder.processors.realtime.AudioRecorderProcessor
import th.co.opendream.vbs_recorder.processors.realtime.AudioRepository
import th.co.opendream.vbs_recorder.processors.realtime.FileWriter
import th.co.opendream.vbs_recorder.utils.SettingsUtil


class Record2Service : Service() {

    private lateinit var audioRecorderProcessor: AudioRecorderProcessor
    private lateinit var recorder: AudioRecord
    private lateinit var db: VBSDatabase
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRecording = false
    private lateinit var audioProcessor: AudioProcessor
    private val mBinder: IBinder = LocalBinder()

    private var sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var sampleFor200msInShort: Int = 0
    private var chunkSizeInMillisec: Int = 0
    private var maxFileSize = 3 * 1024 * 1024 // 3 MB

    override fun onCreate() {
        super.onCreate()
        val settingsUtil = SettingsUtil(applicationContext)

        sampleRate = settingsUtil.getSampleRate()
        chunkSizeInMillisec = settingsUtil.getChunkSizeMs()
        sampleFor200msInShort = (sampleRate * chunkSizeInMillisec) / 1000
        val maxFileSizeInMB = settingsUtil.getMaxFileSizeInMB()
        maxFileSize = maxFileSizeInMB * 1024 * 1024

        db = Room.databaseBuilder(
            applicationContext,
            VBSDatabase::class.java,
            "vbs_database"
        ).build()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_ID, createNotification())
        startRecording()
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    public fun startRecording() {
        // The value can vary depending on the device, but it is typically around 3584 bytes.
        val minBuffSizeInBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        ) // we process with short (2 bytes)

        val bufferSizeInBytes = maxOf(sampleFor200msInShort * 2, minBuffSizeInBytes)
        Log.i("Record2Service", "Audio record Buffer size: $bufferSizeInBytes")

        recorder = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeInBytes)
            .build()

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed")
            return
        }

        isRecording = true
        recorder.startRecording()
        val filePrefix = SettingsUtil(applicationContext).getFilePrefix()
        val fileWriter = FileWriter(externalCacheDir?.absolutePath!! + "/" + filePrefix)
        val pcmToWavFileConverter = PcmToWavFileConverter()
        val audioRepository = AudioRepository(
            db = db,
            contentResolver = contentResolver,
            pcmToWavFileConverter = pcmToWavFileConverter,
            sampleRate = sampleRate
        )

        audioProcessor = AudioProcessor(
            audioRepository = audioRepository,
            sampleRate = sampleRate,
            fileWriter = fileWriter,
            onSave = fun(nameInMediaStore: String) {
                Log.i(TAG, "Saved file: $nameInMediaStore")
                onPostFileCreated(nameInMediaStore)
            },
            maxFileSize = maxFileSize
        )

        audioRecorderProcessor = AudioRecorderProcessor(
            isActive = ::isActive,
            recordState = ::recordState,
            readFromRecorder = recorder::read,
            audioProcessor = audioProcessor,
            bufferSizeInBytes = bufferSizeInBytes,
            sampleFor200msInShort = sampleFor200msInShort,
            handleAudioRecordError = ::handleAudioRecordError
        )

        audioRecorderProcessor.startRecording(serviceScope)
    }

    private fun isActive(): Boolean {
        return isRecording
    }

    private fun recordState(): Int {
        return recorder.recordingState
    }

    public fun stopRecording() {
        isRecording = false
        serviceJob.cancel()
        recorder.stop()
        recorder.release()
        CoroutineScope(Dispatchers.IO).launch {
            audioProcessor.close()
        }
    }

    private fun handleAudioRecordError(error: Int) {
        val errorMessage = when (error) {
            AudioRecord.ERROR_INVALID_OPERATION -> "Recording not enabled"
            AudioRecord.ERROR_BAD_VALUE -> "Bad value"
            AudioRecord.ERROR_DEAD_OBJECT -> "Dead object"
            AudioRecord.ERROR -> "Generic error"
            else -> "Unknown error: $error"
        }
        Log.e("AudioProcessor", "AudioRecord error: $errorMessage")
    }

    private fun onPostFileCreated(nameInMediaStore: String) {
        val intent = Intent(this, RecordProcessorService::class.java).apply {
            putExtra(RecordProcessorService.EXTRA_MEDIA_DISPLAY_NAME, nameInMediaStore)
        }
        startService(intent)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MSR is recording")
            .setContentText("Audio recording in progress")
            .setSmallIcon(R.drawable.baseline_record_voice_over_24)
            .setContentIntent(createPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .build()
    }

    private fun createPendingIntent(): PendingIntent {
        // Create an intent to launch the main activity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        // Create pending intent
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Audio Record Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)

    }

    inner class LocalBinder : Binder() {
        fun getService(): Record2Service
        {
            return this@Record2Service
        }
    }

    // for unit testing
    // getter for sampleRate
    fun getSampleRate(): Int {
        return sampleRate
    }

    fun getSampleFor200msInShort(): Int {
        return sampleFor200msInShort
    }

    fun getChunkSizeMs(): Int {
        return chunkSizeInMillisec
    }

    fun getMaxFileSize(): Int {
        return maxFileSize
    }

    companion object {
        const val TAG = "Record2Service"
        const val SERVICE_ID = 102
        const val CHANNEL_ID = "th.co.opendream.vbs_recorder.NotificationServiceChannel"
    }
}