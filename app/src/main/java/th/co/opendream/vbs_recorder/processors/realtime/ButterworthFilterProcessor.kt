package th.co.opendream.vbs_recorder.processors.realtime

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.tan

class ButterworthFilterProcessor(
    sampleRate: Int,
    cutoffFrequency: Double,
    order: Int
)  : IChunkProcessor {
    // Pre-computed coefficients
    private val a = DoubleArray(order + 1)
    private val b = DoubleArray(order + 1)
    private val x = DoubleArray(order + 1)
    private val y = DoubleArray(order + 1)

    init {
        // Compute Butterworth filter coefficients
        val omega = 2.0 * PI * cutoffFrequency / sampleRate
        val alpha = tan(omega / 2.0)
        val cosOmega = cos(omega)

        // Second order Butterworth coefficients
        val a0 = 1.0 + 2.0 * alpha + 2.0 * alpha * alpha
        b[0] = 1.0 / a0
        b[1] = -2.0 / a0
        b[2] = 1.0 / a0
        a[1] = (-2.0 + 2.0 * 2.0 * alpha * alpha) / a0
        a[2] = (1.0 - 2.0 * alpha + 2.0 * alpha * alpha) / a0
    }

    override fun processChunk(chunk: ShortArray): ShortArray {
        val size = chunk.size
        val output = ShortArray(size)

        for (i in 0 until size) {
            // Shift x values
            for (j in x.size - 1 downTo 1) {
                x[j] = x[j - 1]
            }
            x[0] = chunk[i].toDouble()

            // Shift y values
            for (j in y.size - 1 downTo 1) {
                y[j] = y[j - 1]
            }

            // Apply filter
            var sum = 0.0
            for (j in 0 until b.size) {
                sum += b[j] * x[j]
            }
            for (j in 1 until a.size) {
                sum -= a[j] * y[j - 1]
            }

            y[0] = sum
            output[i] = sum.toInt().toShort()
        }

        return output
    }
}