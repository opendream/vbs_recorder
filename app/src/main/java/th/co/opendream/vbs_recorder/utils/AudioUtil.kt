package th.co.opendream.vbs_recorder.utils

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import uk.me.berndporr.iirj.Butterworth
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class AudioUtil {

    fun extractAudioData(filePath: String): Pair<Int, DoubleArray> {
        val file = File(filePath)
        val audioData = mutableListOf<Double>()
        var sampleRate = 0

        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                fis.read(header, 0, 44)

                // Extract sample rate from the header
                sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int

                val buffer = ByteArray(2)
                while (fis.read(buffer) != -1) {
                    val sample = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).short.toDouble() / Short.MAX_VALUE
                    audioData.add(sample)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Pair(sampleRate, audioData.toDoubleArray())
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        throw IllegalArgumentException("No audio track found in $extractor")
    }

    private fun rescalingData(filteredData: DoubleArray) {
        val filteredMin = filteredData.minOf { (it) }
        val filteredMax = filteredData.maxOf { (it) }

        for (i in filteredData.indices) {
            val v = filteredData[i]
            filteredData[i] = Short.MAX_VALUE * (v - filteredMin) / (filteredMax - filteredMin)
        }
    }

    fun applyHighPassFilter(audioData: DoubleArray, sampleRate: Double, cutoffFrequency: Double, order: Int): DoubleArray {
        val butterworth = Butterworth()
        butterworth.highPass(order, sampleRate, cutoffFrequency)

        val filteredData = DoubleArray(audioData.size)
        for (i in audioData.indices) {
            filteredData[i] = butterworth.filter(audioData[i])
        }
        return filteredData
    }

    fun applyLowPassFilter(audioData: DoubleArray, sampleRate: Double, cutoffFrequency: Double, order: Int): DoubleArray {
        val butterworth = Butterworth()
        butterworth.lowPass(order, sampleRate, cutoffFrequency)

        val filteredData = DoubleArray(audioData.size)
        for (i in audioData.indices) {
            filteredData[i] = butterworth.filter(audioData[i])
        }
        return filteredData
    }

    fun  applyAudioFileHighPassFilter(filePath: String, cutoffFrequency: Double, order: Int, outputFilePath: String) {
        val audioData = extractAudioData(filePath)
        val sampleRate = audioData.first
        val arrayData = audioData.second
        val filteredData = applyHighPassFilter(arrayData, sampleRate.toDouble(), cutoffFrequency, order)
        rescalingData(filteredData)

        val result = convertDoubleArrayToAudioFile(filteredData, sampleRate, outputFilePath)
        Log.e("AudioUtil", "Result: $result")
    }

    fun applyAudioFileLowPassFilter(filePath: String, cutoffFrequency: Double, order: Int, outputFilePath: String) {
        val audioData = extractAudioData(filePath)
        val sampleRate = audioData.first
        val arrayData = audioData.second
        val filteredData = applyLowPassFilter(arrayData, sampleRate.toDouble(), cutoffFrequency, order)
        rescalingData(filteredData)

        val result = convertDoubleArrayToAudioFile(filteredData, sampleRate, outputFilePath)
        Log.e("AudioUtil", "Result: $result")
    }

    fun convertDoubleArrayToAudioFile(audioData: DoubleArray, sampleRate: Int, outputFilePath: String): Boolean {
        val file = File(outputFilePath)
        val wavOutputStream = FileOutputStream(file)

        val byteBuffer = ByteBuffer.allocate(audioData.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (sample in audioData) {
            val shortSample = (sample * Short.MAX_VALUE).toInt().toShort()
            byteBuffer.putShort(shortSample)
        }



        // Write WAV header
        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + byteBuffer.capacity())
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1.toShort())
        header.putShort(1.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2.toShort())
        header.putShort(16.toShort())
        header.put("data".toByteArray())
        header.putInt(byteBuffer.capacity())
        wavOutputStream.write(header.array())

        // Write audio data
        wavOutputStream.write(byteBuffer.array())
        wavOutputStream.close()

        return true
    }

    companion object {
    }

}