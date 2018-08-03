package com.jirayu4r7.half_wheel_scale

import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import java.text.DecimalFormat
import kotlin.math.PI

class HalfWheelScaleView : View {
    private val CENTER_OFFSET_VERTICAL = Utils().dpToPx(context, 86f)
    private val VELOCITY_THRESHOLD = 0.05f

    private val TOUCH_STATE_RESTING = 0
    private val TOUCH_STATE_CLICK = 1
    private val TOUCH_STATE_SCROLL = 2
    private lateinit var velocityTracker: VelocityTracker

    private var touchState = TOUCH_STATE_RESTING
    private val RADIANS_PER_SECOND = 1

    private var currentTheta: Double = 0.toDouble()
    private var initTheta: Double = 0.toDouble()
    private val bounds = Rect()
    private var minAngleTheta: Double = 0.toDouble()
    private var maxAngleTheta: Double = 0.toDouble()
    private var indicatorGapAngle: Double = 0.toDouble()
    private var currentTime: Long = 0
    private var mMaxValue: Int = 0
    private var mMinValue: Int = 0
    private var indicatorInterval: Int = 0
    private var textSize: Float = 0f
    private var angleToCompare: Int = 0
    private var mLongIndicatorHeight: Float = 18f
    private var mShortIndicatorHeight: Float = 14f
    private var delta: Double = 0.0
    private var indicatorStrokeWidth: Float = 0f
    private var paintInnerCircleColor: Int = 0
    private var paintCircleSmallColor: Int = 0
    private var paintNotchColor: Int = 0
    private var paintTextColor: Int = 0
    private var paintIndicatorColor: Int = 0
    private var paintArcColor: Int = 0

    private val deceleration = 10f

    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var radius: Float = 0f
    private var radiusCircleSmall = 0f
    private var circleWidth = Utils().dpToPx(context, 72f)

    private lateinit var paintInnerCircle: Paint
    private lateinit var paintArc: Paint
    private lateinit var paintIndicator: Paint
    private lateinit var paintText: Paint
    private lateinit var notchPath: Path
    private lateinit var notchPaint: Paint
    private lateinit var paintCircleSmall: Paint

    private var initVelocity = 0.5f

    private var onHalfWheelValueChangeListener: OnHalfWheelValueChangeListener? = null

    fun setOnHalfWheelValueChangeListener(listener: OnHalfWheelValueChangeListener) {
        this.onHalfWheelValueChangeListener = listener
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) :
            super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttrs: Int) :
            super(context, attrs, defStyleAttrs) {
        init(attrs)
    }

    @TargetApi(21)
    constructor(context: Context, attrs: AttributeSet, defStyleAttrs: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttrs, defStyleRes) {
        init(attrs)
    }


    private var dynamicsRunnable: Runnable = object : Runnable {
        override fun run() {
            if (Math.abs(initVelocity) < VELOCITY_THRESHOLD) {
                return
            }
            val newTime = System.nanoTime()
            val deltaNano = newTime - currentTime
            val deltaSecs = deltaNano.toDouble() / 1000000000
            currentTime = newTime
            val finalVelocity = if (initVelocity > 0)
                (initVelocity - deceleration * deltaSecs).toFloat()
            else
                (initVelocity + deceleration * deltaSecs).toFloat()
            if (initVelocity * finalVelocity < 0) {
                return
            }
            rotate(finalVelocity * deltaSecs)
            invalidate()
            this@HalfWheelScaleView.postDelayed(this, (1000 / 60).toLong())
            initVelocity = finalVelocity
        }
    }


    private fun init(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HalfWheelScaleView)
            try {
                indicatorInterval = typedArray.getInt(R.styleable.HalfWheelScaleView_indicatorInterval, 5)
                mMaxValue = typedArray.getInt(R.styleable.HalfWheelScaleView_maxValue, 100)
                mMinValue = typedArray.getInt(R.styleable.HalfWheelScaleView_minValue, 0)
                textSize = typedArray.getDimension(R.styleable.HalfWheelScaleView_textSize, Utils().spToPx(context, 12))
                indicatorStrokeWidth = typedArray.getDimension(R.styleable.HalfWheelScaleView_indicatorStrokeWidth, Utils().dpToPx(context, 1.5f))
                indicatorGapAngle = typedArray.getInt(R.styleable.HalfWheelScaleView_indicatorGapAngle, 3).toDouble()
                paintInnerCircleColor = typedArray.getColor(R.styleable.HalfWheelScaleView_paintInnerCircleColor, Color.DKGRAY)
                paintNotchColor = typedArray.getColor(R.styleable.HalfWheelScaleView_paintNotchColor, Color.parseColor("#f17634"))
                paintIndicatorColor = typedArray.getColor(R.styleable.HalfWheelScaleView_paintIndicatorColor, Color.WHITE)
                paintCircleSmallColor = typedArray.getColor(R.styleable.HalfWheelScaleView_paintCircleColor, Color.WHITE)
                paintTextColor = typedArray.getColor(R.styleable.HalfWheelScaleView_paintTextColor, Color.WHITE)
                paintArcColor = typedArray.getColor(R.styleable.HalfWheelScaleView_paintArcColor, Color.WHITE)
                mLongIndicatorHeight = typedArray.getDimension(R.styleable.HalfWheelScaleView_longIndicatorHeight, Utils().dpToPx(context, 18f))
                mShortIndicatorHeight = typedArray.getDimension(R.styleable.HalfWheelScaleView_shortIndicatorHeight, Utils().dpToPx(context, 14f))
            } finally {
                typedArray.recycle()
            }
        }

        if (mMinValue >= mMaxValue) {
            mMaxValue = mMinValue
            mMinValue = 0
        }

        if (indicatorGapAngle * (mMaxValue - mMinValue) > 350) {
            indicatorGapAngle = 350 / (mMaxValue - mMinValue).toDouble()
        }

        indicatorGapAngle = indicatorGapAngle / 180.toDouble() * PI

        paintInnerCircle = Paint()
        paintCircleSmall = Paint()
        paintArc = Paint()
        paintIndicator = Paint()
        paintText = Paint()

        paintArc.style = Paint.Style.STROKE
        paintIndicator.isAntiAlias = true

        paintText.textAlign = Paint.Align.RIGHT

        paintInnerCircle.color = paintInnerCircleColor
        paintIndicator.color = paintIndicatorColor
        paintText.color = paintTextColor
        paintArc.color = paintArcColor

        currentTheta = PI * 3 / 2
        initTheta = PI * 3 / 2
        angleToCompare = 270

        paintText.textSize = textSize
        paintIndicator.strokeWidth = indicatorStrokeWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        maxAngleTheta = (mMaxValue - mMinValue) * indicatorGapAngle
        minAngleTheta = 0.0

        centerX = measuredWidth / 2f
        centerY = measuredHeight + CENTER_OFFSET_VERTICAL
        radius = measuredWidth / 2f
        radiusCircleSmall = if (circleWidth < radius) radius - circleWidth else radius
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        prepareNotchPaint()
        drawNotchPath()
        canvas?.drawPath(notchPath, notchPaint)
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            makeRadGrad(canvas)
        }
    }

    private fun drawNotchPath() {
        notchPath = Path()
        notchPath.reset()
        notchPath.moveTo((width / 2 - 20).toFloat(), height - radiusCircleSmall + CENTER_OFFSET_VERTICAL)
        notchPath.lineTo((width / 2 - 3).toFloat(), (height - 30 - radiusCircleSmall + CENTER_OFFSET_VERTICAL))
        notchPath.lineTo((width / 2 - 3).toFloat(), height - radiusCircleSmall + 80)
        notchPath.lineTo((width / 2 + 3).toFloat(), height - radiusCircleSmall + 80)
        notchPath.lineTo((width / 2 + 3).toFloat(), (height - 30 - radiusCircleSmall + CENTER_OFFSET_VERTICAL))
        notchPath.lineTo((width / 2 + 20).toFloat(), height - radiusCircleSmall + CENTER_OFFSET_VERTICAL)
    }

    private fun prepareNotchPaint() {
        notchPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        notchPaint.color = paintNotchColor
        notchPaint.strokeWidth = 2f
        notchPaint.style = Paint.Style.FILL_AND_STROKE
    }

    private fun makeRadGrad(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, radius, paintInnerCircle)
        canvas.drawCircle(centerX, centerY, radius, paintArc)
        paintCircleSmall.color = paintCircleSmallColor
        canvas.drawCircle(centerX, centerY, radiusCircleSmall, paintCircleSmall)

        var mIndicatorHeight: Float

        for (i in mMinValue..mMaxValue) {
            val angle = (i - mMinValue) * indicatorGapAngle

            mIndicatorHeight = if (i % indicatorInterval == 0) mLongIndicatorHeight
            else mShortIndicatorHeight

            val newTheta = angle + currentTheta

            val startX = ((radius) * Math.cos(newTheta) + centerX).toFloat()
            val startY = ((radius) * Math.sin(newTheta) + centerY).toFloat()

            val endX = ((radius - mIndicatorHeight) * Math.cos(newTheta) + centerX).toFloat()
            val endY = ((radius - mIndicatorHeight) * Math.sin(newTheta) + centerY).toFloat()

            val textPointX = ((radius - mIndicatorHeight - textSize) * Math.cos(newTheta) + centerX).toFloat()
            val textPointY = ((radius - mIndicatorHeight - textSize) * Math.sin(newTheta) + centerY).toFloat()

            if (i % indicatorInterval == 0) {
                addingTextValuesToDial(canvas, i, textPointX, textPointY)
            }

            canvas.drawLine(startX, startY, endX, endY, paintIndicator)

            val newThetaInDegree = (newTheta / PI * 180).toInt()
            val formatter = DecimalFormat("00")
            val value = formatter.format(i)
            if (newThetaInDegree == angleToCompare) {
                onHalfWheelValueChangeListener?.let {
                    onHalfWheelValueChangeListener?.onHalfWheelValueChanged(value.toInt(), mMaxValue)
                    currentValue = i
                }
            }
        }
    }


    private fun addingTextValuesToDial(canvas: Canvas,
                                       value: Int, startX: Float, startY: Float) {
        paintText.getTextBounds(value.toString(), 0, 1, bounds)
        canvas.drawText(value.toString(), startX + textSize.div(2), startY, paintText)

    }

    private var lastTouchXCircle: Float = 0f
    private var lastTouchYCircle: Float = 0f
    private var xcircle: Float = 0f
    private var ycircle: Float = 0f

    private fun isTouchInCircle(event: MotionEvent): Boolean {
        val dx = Math.pow((event.x - centerX).toDouble(), 2.toDouble())
        val dy = Math.pow((event.y - centerY).toDouble(), 2.toDouble())

        return (dx + dy < Math.pow(radius.toDouble(), 2.0))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (this.parent != null) {
                    this.parent.requestDisallowInterceptTouchEvent(true)
                }
                startTouch(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTouchInCircle(event)) {
                    if (this.parent != null) {
                        this.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    duringTouch(event)
                    return true
                } else {
                    return false
                }
            }

            MotionEvent.ACTION_UP -> {
                if (this.parent != null) {
                    this.parent.requestDisallowInterceptTouchEvent(true)
                }
                processTouch(event)
                return true
            }
            else -> return false
        }
    }

    private fun duringTouch(event: MotionEvent) {
        val eventX = event.x
        val eventY = event.y

        xcircle = eventX - centerX
        ycircle = centerY - eventY

        val originalAngle = Math.atan2(lastTouchYCircle.toDouble(), lastTouchXCircle.toDouble())
        val newAngle = Math.atan2(ycircle.toDouble(), xcircle.toDouble())

        delta = originalAngle - newAngle

        rotate(delta)
        touchState = TOUCH_STATE_SCROLL
        processTouch(event)
    }

    private fun startTouch(event: MotionEvent) {
        // user is touching the list -> no more fling
        removeCallbacks(dynamicsRunnable)

        lastTouchXCircle = event.x - centerX
        lastTouchYCircle = centerY - event.y

        // obtain a velocity tracker and feed it its first event
        velocityTracker = VelocityTracker.obtain()
        velocityTracker.addMovement(event)

        touchState = TOUCH_STATE_CLICK
    }

    private fun rotate(delta: Double) {
        println(delta.toString())
        currentTheta += delta
        if (currentTheta <= minAngleTheta + angleToCompare * PI / 180 && currentTheta >= angleToCompare * PI / 180 - maxAngleTheta) {
            invalidate()
            initTheta += delta
            lastTouchXCircle = xcircle
            lastTouchYCircle = ycircle
        } else {
            if (currentTheta > 3 * PI / 2)
                currentTheta = 3 * PI / 2
            else if (currentTheta < angleToCompare * PI / 180 - maxAngleTheta)
                currentTheta = angleToCompare * PI / 180 - maxAngleTheta
        }
    }

    private fun processTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> velocityTracker.addMovement(event)

            MotionEvent.ACTION_UP -> {
                var velocity = 0f
                if (touchState == TOUCH_STATE_SCROLL) {
                    velocityTracker.computeCurrentVelocity(RADIANS_PER_SECOND)
                    velocity = -1 * velocityTracker.xVelocity
                }
                endTouch(velocity)
            }
        }
        return true
    }

    private fun endTouch(velocity: Float) {
        // recycle the velocity tracker
        velocityTracker.recycle()
        currentTime = System.nanoTime()
        initVelocity = -1 * velocity

        post(dynamicsRunnable)

        // reset touch state
        touchState = TOUCH_STATE_RESTING
    }

    interface OnHalfWheelValueChangeListener {
        fun onHalfWheelValueChanged(value: Int, mMaxValue: Int)
    }

    fun getMinValue() = mMinValue

    fun getMaxValue() = mMaxValue

    fun setValueRange(minValue: Int, maxValue: Int) {
        mMinValue = minValue
        mMaxValue = maxValue
        invalidate()
    }

    fun setLongIndicatorHeight(longIndicatorHeight: Float) {
        mLongIndicatorHeight = Utils().dpToPx(context, longIndicatorHeight)
    }

    fun setShortIndicatorHeight(shortIndicatorHeight: Float) {
        mShortIndicatorHeight = Utils().dpToPx(context, shortIndicatorHeight)
    }

    fun setDonutWidth(donutWidth: Float) {
        circleWidth = Utils().dpToPx(context, donutWidth)
    }

    fun setCircleSmallColor(smallColor: Int) {
        paintCircleSmallColor = smallColor
        paintCircleSmall.color = paintCircleSmallColor
    }

    fun setArcColor(arcColor: Int) {
        paintArcColor = arcColor
        paintArc.color = paintArcColor
    }

    fun setInnerCircleColor(innerCircleColor: Int) {
        paintInnerCircleColor = innerCircleColor
        paintInnerCircle.color = paintInnerCircleColor
    }

    fun setNotchColor(notchColor: Int) {
        paintNotchColor = notchColor
    }

    fun setTextColor(textColor: Int) {
        paintTextColor = textColor
        paintText.color = paintTextColor
    }

    fun setIndicatorColor(indicatorColor: Int) {
        paintIndicatorColor = indicatorColor
        paintIndicator.color = paintIndicatorColor
    }

    fun setTextSize(size: Int) {
        textSize = Utils().spToPx(context, size)
        paintText.textSize = textSize
    }

    fun setIndicatorWidth(indicatorWidth: Float) {
        indicatorStrokeWidth = Utils().dpToPx(context, indicatorWidth)
        paintIndicator.strokeWidth = indicatorStrokeWidth
    }

    fun setIndicatorGapAngle(gap: Double) {
        indicatorGapAngle = gap
    }

    //Todo SaveState

    override fun onSaveInstanceState(): Parcelable {
        val savedState = SavedState(super.onSaveInstanceState())
        savedState.value = currentValue
        return savedState
    }

   private var currentValue = 0

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)
            currentValue = state.value
            println("Current $currentValue")
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    internal class SavedState : View.BaseSavedState {
        var value: Int = 0

        constructor(superState: Parcelable) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            value = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(value)
        }

        companion object {
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}