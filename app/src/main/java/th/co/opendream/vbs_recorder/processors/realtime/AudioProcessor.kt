package th.co.opendream.vbs_recorder.processors.realtime

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.utils.DateUtil
import th.co.opendream.vbs_recorder.utils.RecordUtil
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date
import kotlin.math.ceil

/**
 * Callback for when a file is saved.
 * @property fileName Name of the saved file in media store (without extension)
 */
typealias OnSaveCallback = (fileName: String) -> Unit

/**
 * Processes and manages audio recording operations with real-time filtering and storage capabilities.
 * 
 * This class handles:
 * - Real-time audio processing through multiple filter chains
 * - File management for PCM audio recordings
 * - Automatic file size limiting and rotation
 * - Conversion from PCM to WAV format
 * - Media storage integration
 * - Database record management
 *
 * @property db VBSDatabase instance for storing recording metadata
 * @property contentResolver Android ContentResolver for media access
 * @property sampleRate Audio sample rate in Hz
 * @property baseFilePath Base directory path for storing audio files
 * @property maxFileSize Maximum size for individual audio files in bytes (default: 1MB)
 *
 * @constructor Creates an AudioProcessor with specified parameters and initializes the first audio file
 */
class AudioProcessor(
    private val db: VBSDatabase,
    private val contentResolver: ContentResolver,
    private val sampleRate: Int,
    private val baseFilePath: String,
    private val maxFileSize: Int,
    private val onSave: OnSaveCallback? = null
) {
    companion object {
        const val TAG = "AudioProcessor"
    }

    private var fileIndex = Date().time
    private var currentFile: RandomAccessFile? = null
    private var filePath: String? = null
    private var currentFileSize = 0
    private val fileMutex = Mutex()
    private var chunkProcessor: IChunkProcessor = ChunkProcessorComposer(
        listOf(
            PassthroughProcessor(),
//            RandomDropProcessor(0.1f),
//            ButterworthFilterProcessor(sampleRate, 300.0, 2),
//            RescaleProcessor(),
        )
    )

    init {
        newCurrentFile()
    }

    /**
     * Saves the current PCM audio file by:
     * 1. Converting it to WAV format
     * 2. Storing it in the MediaStore
     * 3. Creating a database record
     *
     * @throws IllegalStateException if filename is null
     * @private
     */
    private fun save() {
        val name = filePath!!.substringAfterLast("/")

        val newFilePath = filePath!!.replace(".pcm", ".wav")
        val newName = name.replace(".pcm", ".wav")

        RecordUtil.convertPcmToWav(filePath!!, newFilePath, sampleRate)
        Log.i(TAG, "Converted file to WAV: $newFilePath")

        val nameWithoutExtension = newName.substringBeforeLast(".wav")
        saveToMediaStore(newFilePath, nameWithoutExtension)
        saveToDb(nameWithoutExtension, newName)

        onSave?.invoke(nameWithoutExtension)

        // delete currentFile (we copy it to media store)
        File(filePath!!).delete()
        File(newFilePath).delete()
    }

    /**
     * Creates a new audio file and closes the current one if it exists.
     * Handles file rotation based on size limits.
     *
     * @private
     */
    private fun createNewFile() {
        closeCurrentFile()
        newCurrentFile()
    }

    private fun newCurrentFile() {
        filePath = "${baseFilePath}_${fileIndex++}.pcm"
        Log.i(TAG, "Creating new file: $filePath")
        currentFile = RandomAccessFile(File(filePath!!), "rw")
        currentFileSize = 0
    }

    fun closeCurrentFile() {
        Log.i(TAG, "close old file ${filePath}")
        currentFile?.close()
        save()
    }


    suspend fun processAudioChunk(buffer: ShortArray, size: Int) {

        val processBuffer = buffer.copyOf(size)
        val filteredData = chunkProcessor.processChunk(processBuffer)

        // Convert to bytes and write to file
        val byteBuffer = ByteBuffer.allocate(size * 2) // 2 bytes per short
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        filteredData.forEach { sample ->
            byteBuffer.putShort(sample)
        }

        // Write to file with size management
        fileMutex.withLock {
            val bytesToWrite = byteBuffer.array()
            currentFile?.write(bytesToWrite)
            currentFileSize += bytesToWrite.size
            Log.i(TAG, "writing ${bytesToWrite.size} bytes to file, total ${currentFileSize} bytes")


            // Check if writing this chunk would exceed max file size
            if (currentFileSize > maxFileSize) {
                Log.i(TAG, "close and Creating new file")
                createNewFile()
            }
        }
    }

    private fun saveToMediaStore(filePath: String, displayName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
            put(MediaStore.Audio.Media.IS_PENDING, 1) //
        }

        val resolver = contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                val inputStream = File(filePath).inputStream()
                inputStream.copyTo(outputStream!!)
                inputStream.close()
            }

            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0) // Mark as complete
            resolver.update(uri, values, null, null)
        }
    }

    /*
     *  @property filePath Name of the saved file in media store (without extension)
     *  @property title Title of the recording (with extension)
     */
    private fun saveToDb(fileName: String, title: String) {
        Log.i(TAG, "save to db: $fileName $title")
        val seconds = ceil((currentFileSize / (sampleRate * 2)).toDouble()).toInt()
        Log.i(TAG, "duration: $seconds, currentFileSize: $currentFileSize")
        val record = th.co.opendream.vbs_recorder.models.Record(
            id = 0,
            title = title,
            description = "",
            filePath = fileName,
            duration = seconds,
            isDeleted = false,
            isSynced = false,
            syncedPath = null,
            syncedAt = null,
            createdAt = DateUtil.currentTimeToLong(),
            updatedAt = DateUtil.currentTimeToLong(),
            highPassFilePath = null,
            lowPassFilePath = null
        )
        db.recordDao().insert(record)
    }
}

