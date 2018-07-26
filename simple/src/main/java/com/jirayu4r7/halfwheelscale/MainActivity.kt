package com.jirayu4r7.halfwheelscale

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jirayu4r7.half_wheel_scale.HalfWheelScaleView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        half_wheel_scale.setOnHalfWheelValueChangeListener(object : HalfWheelScaleView.OnHalfWheelValueChangeListener{
            override fun onHalfWheelValueChanged(value: Int, maxValue: Int) {
             text_view_value.text = "$value Kg."
            }
        })
    }
}
