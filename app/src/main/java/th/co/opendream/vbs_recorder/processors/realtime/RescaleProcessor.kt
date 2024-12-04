package th.co.opendream.vbs_recorder.processors.realtime

class RescaleProcessor : IChunkProcessor {
    private val targetMin: Short = -32768
    private val targetMax: Short = 32767
    override fun processChunk(chunk: ShortArray): ShortArray {
        if (chunk.isEmpty()) return chunk

        // Find current min/max
        var currentMin = chunk[0].toInt()
        var currentMax = chunk[0].toInt()

        for (sample in chunk) {
            if (sample < currentMin) currentMin = sample.toInt()
            if (sample > currentMax) currentMax = sample.toInt()
        }

        // If signal is flat, return original
        if (currentMax == currentMin) return chunk

        // Calculate scaling factors
        val currentRange = (currentMax - currentMin).toDouble()
        val targetRange = (targetMax - targetMin).toDouble()
        val scaleFactor = targetRange / currentRange

        // Create new array for rescaled values
        val rescaled = ShortArray(chunk.size)

        // Apply rescaling
        for (i in chunk.indices) {
            val normalized = (chunk[i] - currentMin) * scaleFactor
            val scaled = (normalized + targetMin).toInt()
            // Clamp to short bounds
            rescaled[i] = scaled.coerceIn(targetMin.toInt(), targetMax.toInt()).toShort()
        }

        return rescaled
    }
}