package th.co.opendream.vbs_recorder.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FrequencySpectrumView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    var frequencies: DoubleArray = doubleArrayOf()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (frequencies.isEmpty()) return

        val barWidth = width / frequencies.size.toFloat()
        for (i in frequencies.indices) {
            val barHeight = frequencies[i] * height
            canvas.drawRect(
                i * barWidth,
                (height - barHeight).toFloat(),
                (i + 1) * barWidth,
                height.toFloat(),
                paint
            )
        }
    }
}