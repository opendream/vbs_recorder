package th.co.opendream.vbs_recorder.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import th.co.opendream.vbs_recorder.db.VBSDatabase
import uk.me.berndporr.iirj.Butterworth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.ceil


class RecordFileUtil(path: String, val db: VBSDatabase, contentResolver: ContentResolver, prefix : String = "vbs") {
    val title: String
    val filePath: String

    private val outputStream : FileOutputStream
    var currentFileSize: Long = 0L
    val butterworth = Butterworth()
    val highFrequencyCutOff = 1000.0
    val highOrder = 6 // Common orders include 2nd, 4th, 6th, and 8th, but they can be even higher.

    var displayName: String = "audio_recorder"

    private val contentResolver: ContentResolver

    init {
        val timestamp = System.currentTimeMillis()
        val fileName = "${prefix}_$timestamp.pcm"
        val file = File("${path}/$fileName")

        Log.e("RecordFile", "File path: ${file.absolutePath}")

        this.title = fileName
        this.displayName = fileName.replace(".pcm", "")
        this.filePath = file.absolutePath
        this.outputStream = FileOutputStream(file)

        this.contentResolver = contentResolver

        butterworth.highPass(highOrder, AudioUtil.sampleRate, highFrequencyCutOff)
    }

    @Throws(IOException::class)
    fun write(b: ByteArray?, len: Int) {
        // Write the data to the file
        this.currentFileSize += len
        outputStream.write(b)

        /*
        // Filter the input data
        val doubleInput = RecordUtil.byteToDouble(b!!)
        outputStream.write(filterByteArray, 0, filterByteArray.size)

        // Remove quite noise
        val noiseThreshold = 0.01 // Adjust this value as needed
        val filteredDoubleInput = doubleInput.filter { sample ->
            Math.abs(sample) > noiseThreshold
        }.toDoubleArray()

        val filterByteArray = RecordUtil.doubleToByte(filteredDoubleInput)
        if (filterByteArray.isNotEmpty()) {
            outputStream.write(filterByteArray, 0, filterByteArray.size)
        }

        this.currentFileSize += filterByteArray.size
        */

        /*
        for (i in 0 until len step 2) {
            val sample = (b!![i + 1].toInt() shl 8) or (b[i].toInt() and 0xFF)
            // use butterworth filter
            val data = butterworth.filter(sample.toDouble())
            // convert back to two bytes in little endian
            outputStream.write(data.toInt() and 0xFF)
            outputStream.write((data.toInt() shr 8) and 0xFF)
        }
        */
    }



    fun close() {
        outputStream.close()

        val wavFilePath = filePath.replace(".pcm", ".wav")
        RecordUtil.convertPcmToWav(filePath, wavFilePath)

        displayName = title.replace(".pcm", "")

        Log.e("RecordFile", "File path: $displayName")
        saveToMediaStore(wavFilePath, displayName)

        save(displayName)
    }

    fun saveToMediaStore(filePath: String, displayName: String) {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, CommonUtil.AUDIO_MINETYPE)
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
        }

        val resolver = contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                val inputStream = File(filePath).inputStream()
                inputStream.copyTo(outputStream!!)
                inputStream.close()
            }
        }
    }

    private fun save(filePath: String) {
        val seconds = ceil(currentFileSize / (AudioUtil.sampleRate * 2)).toInt()
        val record = th.co.opendream.vbs_recorder.models.Record(
            id=0,
            title = title.replace(".pcm", ".wav"),
            description = "",
            filePath = filePath,
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
        CoroutineScope(Dispatchers.IO).launch {
            db.recordDao().insert(record)
        }
    }



}