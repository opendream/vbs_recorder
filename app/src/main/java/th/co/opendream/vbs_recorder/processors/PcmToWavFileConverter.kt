package th.co.opendream.vbs_recorder.processors

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcmToWavFileConverter : IPcmToWavFileConverter {
    override fun convert(pcmFilePath: String, wavFilePath: String, sampleRate: Int, channels: Int, bitDepth: Int) {
        val wavFile = File(wavFilePath)
        val pcmData = FileInputStream(File(pcmFilePath)).use { it.readBytes() }
        val wavData = ByteArray(44 + pcmData.size)

        // RIFF header
        wavData[0] = 'R'.code.toByte()
        wavData[1] = 'I'.code.toByte()
        wavData[2] = 'F'.code.toByte()
        wavData[3] = 'F'.code.toByte()

        // File size
        val fileSize = 36 + pcmData.size
        ByteBuffer.wrap(wavData, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(fileSize)

        // WAVE header
        wavData[8] = 'W'.code.toByte()
        wavData[9] = 'A'.code.toByte()
        wavData[10] = 'V'.code.toByte()
        wavData[11] = 'E'.code.toByte()

        // fmt subchunk
        wavData[12] = 'f'.code.toByte()
        wavData[13] = 'm'.code.toByte()
        wavData[14] = 't'.code.toByte()
        wavData[15] = ' '.code.toByte()

        // Subchunk1 size (16 for PCM)
        ByteBuffer.wrap(wavData, 16, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(16)

        // Audio format (1 for PCM)
        ByteBuffer.wrap(wavData, 20, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(1)

        // Number of channels
        ByteBuffer.wrap(wavData, 22, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(channels.toShort())

        // Sample rate
        ByteBuffer.wrap(wavData, 24, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate)

        // Byte rate
        val byteRate = sampleRate * channels * bitDepth / 8
        ByteBuffer.wrap(wavData, 28, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate)

        // Block align
        val blockAlign = channels * bitDepth / 8
        ByteBuffer.wrap(wavData, 32, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(blockAlign.toShort())

        // Bits per sample
        ByteBuffer.wrap(wavData, 34, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(bitDepth.toShort())

        // data subchunk
        wavData[36] = 'd'.code.toByte()
        wavData[37] = 'a'.code.toByte()
        wavData[38] = 't'.code.toByte()
        wavData[39] = 'a'.code.toByte()

        // Subchunk2 size
        ByteBuffer.wrap(wavData, 40, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(pcmData.size)

        // PCM data
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)

        // Write to WAV file
        FileOutputStream(wavFile).use { it.write(wavData) }
    }
}