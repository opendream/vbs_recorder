package th.co.opendream.vbs_recorder.processors

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class PcmToWavFileConverterTest {

    @Test
    fun testConvert() {
        val converter = PcmToWavFileConverter()
        val pcmFilePath = "/tmp/test.pcm"
        val wavFilePath = "/tmp/test.wav"
        val sampleRate = 44100
        val channels = 2
        val bitDepth = 16

        // Create a dummy PCM file for testing
        val pcmData = ByteArray(100) { it.toByte() }
        File(pcmFilePath).writeBytes(pcmData)

        // Convert PCM to WAV
        converter.convert(pcmFilePath, wavFilePath, sampleRate, channels, bitDepth)

        // Verify the WAV file
        val wavFile = File(wavFilePath)
        assertTrue(wavFile.exists())
        val wavData = FileInputStream(wavFile).use { it.readBytes() }
        assertEquals(44 + pcmData.size, wavData.size)

        // Clean up
        File(pcmFilePath).delete()
        File(wavFilePath).delete()
    }
}