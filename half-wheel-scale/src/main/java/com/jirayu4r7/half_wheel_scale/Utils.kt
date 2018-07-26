package com.jirayu4r7.half_wheel_scale

import android.content.Context
import android.util.TypedValue

open class Utils {

    fun dpToPx(context: Context, dp: Float) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
    )

    fun spToPx(context: Context, sp: Int) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            context.resources.displayMetrics
    )
}