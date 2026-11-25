package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.sin

/**
 * Status orb: a plain disc that breathes when ready, sends out rings while listening and has a
 * thin spinning arc while working.
 */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    enum class Mode { OFF, IDLE, LISTENING, WORKING, SPEAKING }

    var mode: Mode = Mode.OFF
        set(value) { field = value; invalidate() }

    /** Disable for screenshots/tests so a single static frame is drawn. */
    var animate = true

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val arc = RectF()
    private val startTime = System.currentTimeMillis()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = if (animate) (System.currentTimeMillis() - startTime) / 1000f else 0.4f
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f
        val core = r * 0.5f

        val color = when (mode) {
            Mode.OFF -> Color.rgb(51, 65, 85)          // muted slate
            Mode.IDLE -> Color.rgb(56, 189, 248)       // sky blue (#38BDF8)
            Mode.LISTENING -> Color.rgb(34, 211, 238)  // crisp cyan (#22D3EE)
            Mode.WORKING -> Color.rgb(14, 165, 233)    // deep sky blue (#0EA5E9)
            Mode.SPEAKING -> Color.rgb(103, 232, 249)  // bright ice cyan (#67E8F9)
        }

        when (mode) {
            Mode.LISTENING -> {
                // Two cyan rings expanding outwards and fading.
                ring.strokeWidth = r * 0.025f
                for (i in 0 until 2) {
                    val p = ((t * 0.8f + i * 0.5f) % 1f)
                    ring.color = withAlpha(color, (130 * (1 - p)).toInt())
                    canvas.drawCircle(cx, cy, core + (r * 0.95f - core) * p, ring)
                }
            }
            Mode.WORKING -> {
                val arcColor = Color.rgb(103, 232, 249)
                ring.strokeWidth = r * 0.035f
                ring.color = withAlpha(arcColor, 48)
                val ar = core + r * 0.18f
                canvas.drawCircle(cx, cy, ar, ring)
                ring.color = arcColor
                arc.set(cx - ar, cy - ar, cx + ar, cy + ar)
                canvas.drawArc(arc, t * 260f, 95f, false, ring)
            }
            Mode.SPEAKING -> {
                // Soft sky-cyan halo pulsing around the core while speaking.
                ring.strokeWidth = r * 0.03f
                val pulse = 0.5f + 0.5f * sin(t * 6.5f)
                ring.color = withAlpha(color, (55 + 75 * pulse).toInt())
                canvas.drawCircle(cx, cy, core + r * (0.14f + 0.12f * pulse), ring)
            }
            else -> {}
        }

        val breathe = when (mode) {
            Mode.IDLE -> 1f + 0.035f * sin(t * 1.6f)
            Mode.SPEAKING -> 1f + 0.05f * sin(t * 6.5f)
            else -> 1f
        }
        fill.color = color
        canvas.drawCircle(cx, cy, core * breathe, fill)

        if (animate && mode != Mode.OFF && isAttachedToWindow) postInvalidateOnAnimation()
    }

    private fun withAlpha(c: Int, a: Int) = Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
}
