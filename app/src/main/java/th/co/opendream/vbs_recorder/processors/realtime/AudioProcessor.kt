package th.co.opendream.vbs_recorder.processors.realtime

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
 * @property sampleRate Audio sample rate in Hz
 * @property maxFileSize Maximum size for individual audio files in bytes (default: 1MB)
 *
 * @constructor Creates an AudioProcessor with specified parameters and initializes the first audio file
 */
class AudioProcessor(
    private val fileWriter: IFileWriter,
    private val audioRepository: IAudioRepository,
    private val sampleRate: Int,
    private val maxFileSize: Int,
    private val onSave: OnSaveCallback? = null,
) : IAudioProcessor {
    companion object {
        const val TAG = "AudioProcessor"
    }

    private var chunkProcessor: IChunkProcessor = ChunkProcessorComposer(
        listOf(
            PassthroughProcessor(),
//            RandomDropProcessor(0.1f),
//            ButterworthFilterProcessor(sampleRate, 300.0, 2),
//            RescaleProcessor(),
        )
    )

    init {
        fileWriter.createNewFile()
    }

    override fun close() {
        fileWriter.close()
        val savedName = audioRepository.add(fileWriter.getFilePath(), fileWriter.getCurrentFileSize())
        onSave?.invoke(savedName)

    }

    override suspend fun processAudioChunk(buffer: ShortArray, size: Int) {
        val processBuffer = buffer.copyOf(size)
        val filteredData = chunkProcessor.processChunk(processBuffer)

        // Convert to bytes and write to file
        val byteBuffer = ByteBuffer.allocate(size * 2) // 2 bytes per short
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        filteredData.forEach { sample ->
            byteBuffer.putShort(sample)
        }

        // Write to file with size management
        val currentFileSize = fileWriter.write(byteBuffer.array())
        Log.d(TAG, "Current file size: $currentFileSize")

        if (currentFileSize >= maxFileSize) {
            val filePath = fileWriter.switchToNextFile()
            val savedName = audioRepository.add(filePath, currentFileSize)
            onSave?.invoke(savedName)
        }
    }
}

