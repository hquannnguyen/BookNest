package com.example.quanlysachcanhan.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.quanlysachcanhan.R

class CategoryDonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val percentage: Float, val color: Int)

    private val slices = mutableListOf<Slice>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.surface_container_high)
    }
    private val rect = RectF()

    fun setSlices(newSlices: List<Slice>) {
        slices.clear()
        slices.addAll(newSlices)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke = width * 0.13f
        paint.strokeWidth = stroke
        bgPaint.strokeWidth = stroke

        val padding = stroke / 2f + 4f
        rect.set(padding, padding, width - padding, height - padding)

        // Draw background circle track
        canvas.drawOval(rect, bgPaint)

        if (slices.isEmpty()) return

        var startAngle = -90f
        for (slice in slices) {
            val sweep = slice.percentage * 3.6f
            if (sweep > 0.5f) {
                paint.color = slice.color
                // Slight gap between slices if multiple
                val actualSweep = if (slices.size > 1) (sweep - 2.5f).coerceAtLeast(1f) else sweep
                canvas.drawArc(rect, startAngle, actualSweep, false, paint)
                startAngle += sweep
            }
        }
    }
}
