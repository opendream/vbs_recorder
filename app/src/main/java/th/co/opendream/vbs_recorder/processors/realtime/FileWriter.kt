package th.co.opendream.vbs_recorder.processors.realtime

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.RandomAccessFile
import java.util.Date

class FileWriter(private val baseFilePath: String) : IFileWriter {
    private var filePath: String? = null
    private lateinit var currentFile: RandomAccessFile
    private var fileIndex = Date().time
    private var currentFileSize = 0
    private val fileMutex = Mutex()


    override fun createNewFile() {
        filePath = "${baseFilePath}_${fileIndex++}.pcm"
        currentFile = RandomAccessFile(File(filePath!!), "rw")
        currentFileSize = 0
        Log.d(TAG, "Created new file: $filePath")
    }

    override fun switchToNextFile() : String {
        currentFile?.close()
        val currentFilePath = filePath!!
        createNewFile()
        return currentFilePath
    }

    override suspend fun write(data: ByteArray): Int {
        Log.d(TAG, "Writing ${data.size} bytes to file: $filePath")
        fileMutex.withLock {
            currentFile?.write(data)
            currentFileSize += data.size
        }
        return currentFileSize
    }

    override fun close() {
        Log.d(TAG, "Closing file: $filePath")
        currentFile.close()
    }

    override fun getFilePath(): String {
        return filePath!!
    }

    override fun getCurrentFileSize(): Int {
        return currentFileSize
    }

    companion object {
        const val TAG = "FileWriter"
    }
}