# Android Half Wheel Scale

#### Android custom view that uses half wheel for picking the number from given range.

## How to use this library?
- ### Gradle dependency: 
    - Add below dependency into your build.gradle file.
        ```groovy
        implementation 'com.jirayu4r7.half_wheel_scale:half-wheel-scale:0.2'
        ```
- Add `HalfWheelScaleView` inside your XML layout.
```xml
 <com.jirayu4r7.half_wheel_scale.HalfWheelScaleView
        android:id="@+id/half_wheel_scale"
        android:layout_width="match_parent"
        android:layout_height="160dp"
        app:maxValue="120"
        app:minValue="20"
        app:indicatorInterval="5"
        app:paintIndicatorColor="@android:color/white"
        app:longIndicatorHeight="18dp"
        app:shortIndicatorHeight="12dp"
        app:indicatorStrokeWidth="2dp"
        app:indicatorGapAngle="3"
        app:paintNotchColor="#FA8100"
        app:textSize="14sp"
        app:paintTextColor="@android:color/white"
        app:paintInnerCircleColor="#98A1AE"
        app:paintArcColor="#071A36"
        app:paintCircleColor="#D7D7D7"
        />
```
