package th.co.opendream.vbs_recorder.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

class RecordUtil {
    companion object {
        fun byteToDouble(input: ByteArray): DoubleArray {
            val output = DoubleArray(input.size / 2)
            for (i in output.indices) {
                val sample = ((input[i * 2].toInt() and 0xFF) or (input[i * 2 + 1].toInt() shl 8)).toShort()
                output[i] = sample / 32768.0
            }
            return output
        }

        fun rescalingDouble(input: DoubleArray) {
            val filteredMin = input.minOf { (it) }
            val filteredMax = input.maxOf { (it) }

            for (i in input.indices) {
                val v = input[i]
                input[i] =  32768.0 * (v - filteredMin) / (filteredMax - filteredMin)
            }
        }

        fun doubleToByte(input: DoubleArray): ByteArray {
            val output = ByteArray(input.size * 2)
            for (i in input.indices) {
                val sample = (input[i] * 32768.0).toInt().toShort()
                output[i * 2] = (sample.toInt() and 0xFF).toByte()
                output[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            return output
        }

        fun getFileDuration(fileSize: Long): Int {
            val seconds = ceil(fileSize / (AudioUtil.sampleRate * 2)).toInt()
            return seconds
        }

        fun convertPcmToWav(filePath: String, wavFilePath: String, sampleRate: Int = AudioUtil.sampleRate.toInt(), channels: Int = 1, bitDepth: Int = 16) {
            val wavFile = File(wavFilePath)
            val pcmData = FileInputStream(File(filePath)).use { it.readBytes() }
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
}