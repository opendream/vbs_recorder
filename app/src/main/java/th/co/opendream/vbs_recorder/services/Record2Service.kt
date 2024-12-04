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
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import th.co.opendream.vbs_recorder.R
import th.co.opendream.vbs_recorder.activities.MainActivity
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.processors.realtime.AudioProcessor
import th.co.opendream.vbs_recorder.utils.CommonUtil

class Record2Service : Service() {

    private lateinit var recorder: AudioRecord
    private lateinit var db: VBSDatabase
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isRecording = false
    private lateinit var audioProcessor: AudioProcessor

    val sampleRate = 44100
    val channelConfig = AudioFormat.CHANNEL_IN_MONO
    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    val sampleFor200msInShort = (sampleRate * 200) / 1000   // 8820 = 44100 * 200 / 1000

    override fun onCreate() {
        super.onCreate()
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
    private fun startRecording() {
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
        val filePrefix = CommonUtil(applicationContext).getFilePrefix()
        serviceScope.launch {
            audioProcessor = AudioProcessor(
                db = db,
                contentResolver = contentResolver,
                sampleRate = sampleRate,
                baseFilePath = externalCacheDir?.absolutePath!! + "/" + filePrefix,
                onSave = fun(nameInMediaStore: String) {
                    Log.i(TAG, "Saved file: $nameInMediaStore")
                    onPostFileCreated(nameInMediaStore)
                }
            )
            var offsetInShorts = 0
            val bufferInShort = ShortArray(bufferSizeInBytes / 2)
            while (isActive && recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val remaining = bufferInShort.size - offsetInShorts
                val shortsRead = recorder.read(bufferInShort, offsetInShorts, remaining)
                if (shortsRead > 0) {
                    offsetInShorts += shortsRead

                    if (offsetInShorts >= sampleFor200msInShort) {
                        audioProcessor.processAudioChunk(bufferInShort, sampleFor200msInShort)
                        val leftOver = offsetInShorts - sampleFor200msInShort
                        if (leftOver > 0) {
                            System.arraycopy(bufferInShort, sampleFor200msInShort, bufferInShort, 0, leftOver)
                            offsetInShorts = leftOver
                        } else {
                            offsetInShorts = 0
                        }
                    }
                } else {
                    handleAudioRecordError(shortsRead)
                }
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        serviceJob.cancel()
        recorder.stop()
        recorder.release()
        CoroutineScope(Dispatchers.IO).launch {
            audioProcessor.closeCurrentFile()
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
        return null
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

    companion object {
        const val TAG = "Record2Service"
        const val SERVICE_ID = 102
        const val CHANNEL_ID = "th.co.opendream.vbs_recorder.NotificationServiceChannel"
    }
}

/*
 for overlapping audio chunks
 class OverlappingAudioProcessor {
    private val overlap = 0.5 // 50% overlap
    private var previousBuffer: ShortArray? = null

    private fun processWithOverlap(buffer: ShortArray, size: Int) {
        previousBuffer?.let { previous ->
            // Combine previous and current buffers for overlapped processing
            val combined = ShortArray(previous.size + size)
            System.arraycopy(previous, 0, combined, 0, previous.size)
            System.arraycopy(buffer, 0, combined, previous.size, size)

            // Process overlapped data
            processOverlappedChunk(combined)
        }

        // Save current buffer for next overlap
        previousBuffer = buffer.copyOf(size)
    }
}
 */