package th.co.opendream.vbs_recorder.services

import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.processors.post.EveryNOutOneChunkPostProcessor
import th.co.opendream.vbs_recorder.utils.SettingsUtil
import th.co.opendream.vbs_recorder.utils.RecordUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RecordProcessorService : Service(), RecordProcessorServiceListener {
    companion object {
        const val EXTRA_MEDIA_DISPLAY_NAME = "display_name"
        private const val TAG = "RecordProcessorService"
    }

    private var filePath: String? = null
    private var displayName: String? = null

    private var db: VBSDatabase? = null
    private lateinit var settingsUtil: SettingsUtil;

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        settingsUtil = SettingsUtil(this)

        db = Room.databaseBuilder(
            applicationContext,
            VBSDatabase::class.java,
            "vbs_database"
        ).build()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val it = intent?.getStringExtra(EXTRA_MEDIA_DISPLAY_NAME)
        it?.let {
            val filePath = readAudioFileFromMediaStore(contentResolver, "${it}${SettingsUtil.AUDIO_EXTENTION}")
            if (filePath.isNullOrEmpty() || filePath == "") {
                Log.e(TAG, "File not found: $it")
                stopSelf()

                return START_STICKY
            }

            this.displayName = it
            this.filePath = filePath
            Log.d(TAG, "Received file path: $it >> ${(this.filePath)}")

            onStart()
        } ?: run {
            Log.e(TAG, "No file path provided")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onStart() {
        // Implement your logic to start processing the file at filePath
        Log.d(TAG, "Processing file: $filePath")
        val inputFilePath = filePath!!
        val outputFile = File.createTempFile("filtered_${displayName}", ".wav")

        val sampleRate = settingsUtil.getSampleRate()
        val chunkSizeInMs = settingsUtil.getChunkSizeMs()
        val keepEveryNthChunk = settingsUtil.getKeepEveryNthChunk()

        //val processor: IPostAudioProcessor = PassThroughPostProcessor()
        val processor = EveryNOutOneChunkPostProcessor(sampleRate, chunkSizeInMs, keepEveryNthChunk)

        // val processor: AudioProcessor2 = SilenceProcessor()
        var numberOfOutputBytes: Long = 0
        FileInputStream(inputFilePath).use{fis ->
            FileOutputStream(outputFile).use{fos ->
                val inBytes = fis.readBytes()
                val outBytes = processor.process(inBytes)
                numberOfOutputBytes = outBytes.size.toLong()
                fos.write(outBytes)
            }
        }

        // replace the original file with the filtered file
        Log.i(TAG, "Renaming file: ${outputFile.name} to ${inputFilePath}")
        outputFile.copyTo(File(inputFilePath), true)
        outputFile.delete()

        Log.e(TAG, "Filtered file path: ${filePath!!}")



        CoroutineScope(Dispatchers.IO).launch {
            val fetchData = db!!.recordDao().filterByFilePath(displayName!!)
            Log.e(TAG, "Found ${fetchData.size} records")
            if (fetchData.isNotEmpty()) {
                for (record in fetchData) {
                    record.duration = RecordUtil.getFileDuration(numberOfOutputBytes, sampleRate)
                    db!!.recordDao().update(record)
                }
            }
        }

        // Simulate processing
        onFinished()
    }

    override fun onFinished() {
        // Implement your logic to handle when processing is finished
        Log.d(TAG, "Finished processing file: $filePath")
        stopSelf()
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
}