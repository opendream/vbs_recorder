package th.co.opendream.vbs_recorder.processors

import android.media.AudioRecord
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import th.co.opendream.vbs_recorder.processors.realtime.AudioRecorderProcessor
import th.co.opendream.vbs_recorder.processors.realtime.IAudioProcessor
import kotlin.random.Random

class MockAudioProcessor : IAudioProcessor {
    var totalProcessedData = 0

    override suspend fun processAudioChunk(buffer: ShortArray, size: Int) {
        totalProcessedData += size
    }

    override fun close() {
        // No-op
    }
}

class AudioRecorderProcessorTest {

    @Test
    fun testAudioRecorderProcessor() = runBlocking {
        val bufferSizeInBytes = 2000
        val sampleFor200msInShort = 500
        val totalAudioRecordSize = 8000
        var currentAudioRecordSize = 0

        val mockAudioProcessor = MockAudioProcessor()
        val isActive = { currentAudioRecordSize < totalAudioRecordSize }
        val recordState = { AudioRecord.RECORDSTATE_RECORDING }
        val readFromRecorder = { buffer: ShortArray, offset: Int, size: Int ->
            val randomSize = Random.nextInt(0, size)
            val actualSize = minOf(randomSize, size)
            currentAudioRecordSize += actualSize * 2 // since buffer is in shorts, multiply by 2 for bytes
            actualSize
        }
        val handleAudioRecordError = { _: Int -> }

        val audioRecorderProcessor = AudioRecorderProcessor(
            audioProcessor = mockAudioProcessor,
            bufferSizeInBytes = bufferSizeInBytes,
            sampleFor200msInShort = sampleFor200msInShort,
            isActive = isActive,
            recordState = recordState,
            readFromRecorder = readFromRecorder,
            handleAudioRecordError = handleAudioRecordError
        )

        val job = audioRecorderProcessor.startRecording(this)
        job.join()

        assertEquals(totalAudioRecordSize / 2, mockAudioProcessor.totalProcessedData)
    }
}