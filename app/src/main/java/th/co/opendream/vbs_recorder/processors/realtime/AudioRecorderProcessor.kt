package th.co.opendream.vbs_recorder.processors.realtime

import android.media.AudioRecord
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AudioRecorderProcessor(
    private val audioProcessor: IAudioProcessor,
    private val bufferSizeInBytes: Int,
    private val sampleFor200msInShort: Int,
    private val isActive: () -> Boolean,
    private val recordState: () -> Int,
    private val readFromRecorder: (ShortArray, Int, Int) -> Int,
    private val handleAudioRecordError: (Int) -> Unit
) {
    fun startRecording(scope: CoroutineScope): Job {
        return scope.launch {
            var offsetInShorts = 0
            val bufferInShort = ShortArray(bufferSizeInBytes / 2)
            Log.d(TAG, "Start recording")
            while (isActive() && recordState() == AudioRecord.RECORDSTATE_RECORDING) {
                val remaining = bufferInShort.size - offsetInShorts
                val shortsRead = readFromRecorder(bufferInShort, offsetInShorts, remaining)
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

    companion object {
        const val TAG = "AudioRecorderProcessor"
    }
}