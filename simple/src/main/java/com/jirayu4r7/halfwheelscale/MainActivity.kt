package com.jirayu4r7.halfwheelscale

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import com.jirayu4r7.half_wheel_scale.HalfWheelScaleView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val halfWheelScaleView = findViewById<HalfWheelScaleView>(R.id.half_wheel_scale)

        halfWheelScaleView.setOnHalfWheelValueChangeListener(object : HalfWheelScaleView.OnHalfWheelValueChangeListener {
            override fun onHalfWheelValueChanged(value: Int, maxValue: Int) {
                text_view_value.text = "$value"
                image_view.layoutParams.width = dpToPx(3 * value).toInt()
            }
        })
    }

    fun dpToPx(dp: Int) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
    )
}
