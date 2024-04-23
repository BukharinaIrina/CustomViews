package ru.netology.statsview.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R.*
import ru.netology.statsview.utils.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(
    context,
    attributeSet,
    defStyleAttr,
    defStyleRes,
) {
    private var radius = 0F
    private var center = PointF(0F, 0F)
    private var oval = RectF(0F, 0F, 0F, 0F)

    private var fontSize = AndroidUtils.dp(context, 20).toFloat()
    private var lineWidth = AndroidUtils.dp(context, 5).toFloat()
    private var colors = emptyList<Int>()

    private var progress = 0F
    private var valueAnimator: ValueAnimator? = null
    private var typeOfFilling: Int = 0
    private var durationAnimation = 1000L

    init {
        context.withStyledAttributes(attributeSet, styleable.StatsView) {
            fontSize = getDimension(styleable.StatsView_fontSize, fontSize)
            lineWidth = getDimension(styleable.StatsView_lineWidth, lineWidth)
            val resId = getResourceId(styleable.StatsView_colors, 0)
            colors = resources.getIntArray(resId).toList()
            typeOfFilling = getInt(styleable.StatsView_typeOfFilling, typeOfFilling)
            durationAnimation =
                getInt(styleable.StatsView_duration, durationAnimation.toInt()).toLong()
        }
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            update()
        }

    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.STROKE
        strokeWidth = lineWidth
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        style = Paint.Style.FILL
        textSize = fontSize
        textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) {
            return
        }

        canvas.drawText(
            "%.2f%%".format(data.sum() * 100.00),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint,
        )

        var startAngle = -90F

        when (typeOfFilling) {
            1 -> {    // rotation
                data.forEachIndexed { index, datum ->
                    val angle = datum * 360F
                    paint.color = colors.getOrElse(index) { randomColor() }
                    canvas.drawArc(
                        oval,
                        startAngle + progress * 360F,
                        angle * progress,
                        false,
                        paint
                    )
                    startAngle += angle
                }
                dot(canvas)
            }

            2 -> {    // sequential
                var finalAngle = 0F
                data.forEachIndexed { index, datum ->
                    val angle = datum * 360F
                    paint.color = colors.getOrElse(index) { randomColor() }
                    canvas.drawArc(oval, startAngle, progress * 360F - finalAngle, false, paint)
                    startAngle += angle
                    finalAngle += angle
                    if (finalAngle > (progress * 360F)) return
                }
                dot(canvas)
            }

            3 -> {    // bidirectional
                data.forEachIndexed { index, datum ->
                    val angle = datum * 360F
                    paint.color = colors.getOrElse(index) { randomColor() }
                    canvas.drawArc(oval, startAngle + 45F, angle / 2 * progress, false, paint)
                    canvas.drawArc(oval, startAngle + 45F, -angle / 2 * progress, false, paint)
                    startAngle += angle
                }
                dot(canvas)
            }

            else -> {    // lectureExample
                data.forEachIndexed { index, datum ->
                    val angle = datum * 360F
                    paint.color = colors.getOrElse(index) { randomColor() }
                    canvas.drawArc(oval, startAngle, angle * progress, false, paint)
                    startAngle += angle
                }
                dot(canvas)
            }
        }
    }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F

        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            duration = durationAnimation
            interpolator = LinearInterpolator()
        }.also {
            it.start()
        }
    }

    private fun randomColor() = Random.nextInt(color.black, color.white)

    private fun dot(canvas: Canvas) {
        if (progress == 1F) {
            paint.color = colors[0]
            canvas.drawPoint(center.x, center.y - radius, paint)
        }
    }
}