package th.co.opendream.vbs_recorder.processors.post

import kotlin.math.pow
import kotlin.math.sqrt

data class SilentRegion2(val start: Int, val end: Int)

class SilenceRemoverPostProcessor(
    private val sampleRate: Int = 44100,
    private val thresholdDb: Double = -40.0,
    private val minSilenceDuration: Double = 0.3,
    private val windowSize: Int = 1024
) : IPostAudioProcessor{
    private val hopSize = windowSize / 2
    private val thresholdAmp = 10.0.pow(thresholdDb / 20.0)



    /**
     * Removes silent segments from PCM audio data
     * @param audioData Input PCM audio data as FloatArray
     * @return Pair of processed audio and list of silent regions
     */
    fun removeSilence(audioData: FloatArray): Pair<FloatArray, List<SilentRegion2>> {
        // Calculate energy for each window
        val energy = calculateEnergy(audioData)

        // Find silent regions
        val silentRegions = findSilentRegions(energy, audioData.size)

        // Remove silent regions and create new audio
        val processedAudio = removeRegions(audioData, silentRegions)

        return Pair(processedAudio, silentRegions)
    }

    private fun calculateEnergy(audioData: FloatArray): List<Double> {
        val numWindows = (audioData.size - windowSize) / hopSize + 1
        return List(numWindows) { windowIndex ->
            val start = windowIndex * hopSize
            val end = minOf(start + windowSize, audioData.size)

            // Calculate RMS energy for current window
            sqrt(audioData.slice(start until end)
                .map { it * it }
                .average())
        }
    }

    private fun findSilentRegions(energy: List<Double>, audioLength: Int): List<SilentRegion2> {
        val silentRegions = mutableListOf<SilentRegion2>()
        var currentRegionStart: Int? = null

        energy.forEachIndexed { index, value ->
            val isSilent = value < thresholdAmp
            val sampleIndex = index * hopSize

            when {
                isSilent && currentRegionStart == null -> {
                    currentRegionStart = sampleIndex
                }
                !isSilent && currentRegionStart != null -> {
                    val duration = (sampleIndex - currentRegionStart!!) / sampleRate.toDouble()
                    if (duration >= minSilenceDuration) {
                        silentRegions.add(SilentRegion2(currentRegionStart!!, sampleIndex))
                    }
                    currentRegionStart = null
                }
            }
        }

        // Handle last region if it's silent
        currentRegionStart?.let { start ->
            val duration = (audioLength - start) / sampleRate.toDouble()
            if (duration >= minSilenceDuration) {
                silentRegions.add(SilentRegion2(start, audioLength))
            }
        }

        return silentRegions
    }

    private fun removeRegions(audioData: FloatArray, silentRegions: List<SilentRegion2>): FloatArray {
        if (silentRegions.isEmpty()) return audioData

        // Calculate total length of non-silent regions
        val newLength = audioData.size - silentRegions.sumOf { it.end - it.start }
        val result = FloatArray(newLength)
        var writePos = 0
        var readPos = 0

        silentRegions.forEach { region ->
            // Copy audio until silent region
            val copyLength = region.start - readPos
            audioData.copyInto(
                destination = result,
                destinationOffset = writePos,
                startIndex = readPos,
                endIndex = region.start
            )
            writePos += copyLength
            readPos = region.end
        }

        // Copy remaining audio after last silent region
        if (readPos < audioData.size) {
            audioData.copyInto(
                destination = result,
                destinationOffset = writePos,
                startIndex = readPos,
                endIndex = audioData.size
            )
        }

        return result
    }

    companion object {
        /**
         * Converts audio samples from ByteArray to FloatArray
         * Assumes 16-bit PCM audio
         */
        fun bytesToFloat(bytes: ByteArray): FloatArray {
            val floats = FloatArray(bytes.size / 2)
            for (i in floats.indices) {
                val sample = (bytes[i * 2 + 1].toInt() shl 8) or
                        (bytes[i * 2].toInt() and 0xFF)
                floats[i] = sample / 32768f
            }
            return floats
        }

        /**
         * Converts audio samples from FloatArray back to ByteArray
         * Converts to 16-bit PCM audio
         */
        fun floatToBytes(floats: FloatArray): ByteArray {
            val bytes = ByteArray(floats.size * 2)
            for (i in floats.indices) {
                val sample = (floats[i] * 32768f).toInt().coerceIn(-32768, 32767)
                bytes[i * 2] = sample.toByte()
                bytes[i * 2 + 1] = (sample shr 8).toByte()
            }
            return bytes
        }
    }

    override fun process(input: ByteArray): ByteArray {
        val audioData = bytesToFloat(input)
        val (processedAudio, silentRegions) = removeSilence(audioData)
        val processedBytes = floatToBytes(processedAudio)
        return processedBytes
    }
}