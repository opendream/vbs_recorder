package th.co.opendream.vbs_recorder.processors.realtime

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import th.co.opendream.vbs_recorder.db.VBSDatabase
import th.co.opendream.vbs_recorder.processors.IPcmToWavFileConverter
import th.co.opendream.vbs_recorder.utils.DateUtil
import java.io.File
import kotlin.math.ceil

class AudioRepository(private val db: VBSDatabase,
                      private val contentResolver: ContentResolver,
                      private val pcmToWavFileConverter: IPcmToWavFileConverter,
                      private val sampleRate: Int) : IAudioRepository {

    companion object {
        const val TAG = "AudioRepository"
    }

    override fun add(filePath: String, currentFileSize: Int): String {
        val name = filePath.substringAfterLast("/")
        val newFilePath = filePath.replace(".pcm", ".wav")
        val newName = name.replace(".pcm", ".wav")
        pcmToWavFileConverter.convert(filePath, newFilePath, sampleRate)
        val nameWithoutExtension = newName.substringBeforeLast(".wav")
        saveToMediaStore(newFilePath, nameWithoutExtension)
        saveToDb(nameWithoutExtension, newName, currentFileSize)
        File(filePath).delete()
        File(newFilePath).delete()
        return nameWithoutExtension
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
    private fun saveToDb(fileName: String, title: String, currentFileSize: Int) {
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