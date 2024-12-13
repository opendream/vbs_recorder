package th.co.opendream.vbs_recorder.processors

import junit.framework.TestCase.assertTrue
import org.junit.Test
import th.co.opendream.vbs_recorder.processors.post.EveryNOutOneChunkPostProcessor


class EveryNOutOneChunkPostProcessorTest {

    @Test
    fun testProcess() {
        val sampleRate = 44100
        val chunkSizeInMs = 200
        val nValue = 4
        var processor = EveryNOutOneChunkPostProcessor(sampleRate, chunkSizeInMs, nValue, 0)
        val sampleFor200msInShort = (sampleRate * chunkSizeInMs) / 1000
        val sampleFor200msInBytes = sampleFor200msInShort * 2

        val input = ByteArray(sampleFor200msInBytes * 4)
        for (i in 0 until 4) {
            for (j in 0 until sampleFor200msInBytes) {
                input[(i * sampleFor200msInBytes) + j] = i.toByte()
            }
        }
        val output = processor.process(input)
        assertTrue(output.all { it == 0.toByte() })

        processor = EveryNOutOneChunkPostProcessor(sampleRate, chunkSizeInMs, nValue, 1)
        val output1 = processor.process(input)
        assertTrue(output1.all { it == 1.toByte() })

        processor = EveryNOutOneChunkPostProcessor(sampleRate, chunkSizeInMs, nValue, 2)
        val output2 = processor.process(input)
        assertTrue(output2.all { it == 2.toByte() })

        processor = EveryNOutOneChunkPostProcessor(sampleRate, chunkSizeInMs, nValue, 3)
        val output3 = processor.process(input)
        assertTrue(output3.all { it == 3.toByte() })

    }
}