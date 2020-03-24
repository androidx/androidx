/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.window.sample

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.util.Consumer
import androidx.core.view.doOnLayout
import androidx.window.DeviceState
import androidx.window.DisplayFeature
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Demo activity that shows all display features and current device state on the screen. */
class DisplayFeaturesActivity : BaseSampleActivity() {

    private lateinit var windowManager: WindowManager
    private val stateLog: StringBuilder = StringBuilder()

    private val displayFeatureViews = ArrayList<View>()
    private val deviceStateChangeCallback = DeviceStateChangeCallback()
    private val layoutStateChangeCallback = LayoutStateChangeCallback()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_features)

        windowManager = WindowManager(this, getTestBackend())

        stateLog.clear()
        stateLog.append(getString(R.string.stateUpdateLog)).append("\n")

        windowManager.registerDeviceStateChangeCallback(mainThreadExecutor,
            deviceStateChangeCallback)

        window.decorView.doOnLayout {
            updateCurrentState()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        windowManager.registerLayoutChangeCallback(mainThreadExecutor, layoutStateChangeCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        windowManager.unregisterLayoutChangeCallback(layoutStateChangeCallback)
    }

    /** Updates the device state and display feature positions. */
    internal fun updateCurrentState() {
        // Cleanup previously added feature views
        val rootLayout = findViewById<FrameLayout>(R.id.featureContainerLayout)
        for (featureView in displayFeatureViews) {
            rootLayout.removeView(featureView)
        }
        displayFeatureViews.clear()

        // Update the UI with the current state
        val stateStringBuilder = StringBuilder()
        // Update the current state string
        stateStringBuilder.append(getString(R.string.deviceState))
            .append(": ")
            .append(windowManager.deviceState)
            .append("\n")

        val layoutInfo = windowManager.windowLayoutInfo
        stateStringBuilder.append(getString(R.string.windowLayout))
            .append(": ")
            .append(layoutInfo)

        // Add views that represent display features
        for (displayFeature in layoutInfo.displayFeatures) {
            val lp = getLayoutParamsForFeatureInFrameLayout(displayFeature, rootLayout) ?: continue

            // Make sure that zero-wide and zero-high features are still shown
            if (lp.width == 0) {
                lp.width = 1
            }
            if (lp.height == 0) {
                lp.height = 1
            }

            val featureView = View(this)
            val color = when (displayFeature.type) {
                DisplayFeature.TYPE_FOLD -> getColor(R.color.colorFeatureFold)
                DisplayFeature.TYPE_HINGE -> getColor(R.color.colorFeatureHinge)
                else -> getColor(R.color.colorFeatureUnknown)
            }
            featureView.foreground = ColorDrawable(color)

            rootLayout.addView(featureView, lp)
            featureView.id = View.generateViewId()

            displayFeatureViews.add(featureView)
        }
        findViewById<TextView>(R.id.currentState).text = stateStringBuilder.toString()
    }

    /** Adds the current state to the text log of changes on screen. */
    internal fun updateStateLog(info: Any) {
        stateLog.append(getCurrentTimeString())
            .append(" ")
            .append(info)
            .append("\n")
        findViewById<TextView>(R.id.stateUpdateLog).text = stateLog
    }

    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val currentDate = sdf.format(Date())
        return currentDate.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.unregisterDeviceStateChangeCallback(deviceStateChangeCallback)
    }

    inner class DeviceStateChangeCallback : Consumer<DeviceState> {
        override fun accept(newDeviceState: DeviceState) {
            updateStateLog(newDeviceState)
            updateCurrentState()
        }
    }

    inner class LayoutStateChangeCallback : Consumer<WindowLayoutInfo> {
        override fun accept(newLayoutInfo: WindowLayoutInfo) {
            updateStateLog(newLayoutInfo)
            updateCurrentState()
        }
    }
}
