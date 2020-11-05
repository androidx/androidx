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
    // Store most recent values for the device state and window layout
    private val stateContainer = StateContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_features)

        windowManager = getTestBackend()?.let { backend -> WindowManager(this, backend) }
            ?: WindowManager(this)

        stateLog.clear()
        stateLog.append(getString(R.string.stateUpdateLog)).append("\n")
    }

    override fun onStart() {
        super.onStart()
        windowManager.registerDeviceStateChangeCallback(
            mainThreadExecutor,
            stateContainer.stateConsumer
        )
        windowManager.registerLayoutChangeCallback(
            mainThreadExecutor,
            stateContainer.layoutConsumer
        )
    }

    override fun onStop() {
        super.onStop()
        windowManager.unregisterDeviceStateChangeCallback(stateContainer.stateConsumer)
        windowManager.unregisterLayoutChangeCallback(stateContainer.layoutConsumer)
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
        stateContainer.lastState?.let { deviceState ->
            stateStringBuilder.append(getString(R.string.deviceState))
                .append(": ")
                .append(deviceState)
                .append("\n")
        }

        stateContainer.lastLayoutInfo?.let { windowLayoutInfo ->
            stateStringBuilder.append(getString(R.string.windowLayout))
                .append(": ")
                .append(windowLayoutInfo)

            // Add views that represent display features
            for (displayFeature in windowLayoutInfo.displayFeatures) {
                val lp = getLayoutParamsForFeatureInFrameLayout(displayFeature, rootLayout)
                    ?: continue

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

    inner class StateContainer {
        var lastState: DeviceState? = null
        var lastLayoutInfo: WindowLayoutInfo? = null

        val stateConsumer: Consumer<DeviceState>
        val layoutConsumer: Consumer<WindowLayoutInfo>

        init {
            stateConsumer = Consumer { state: DeviceState -> update(state) }
            layoutConsumer = Consumer { layout: WindowLayoutInfo -> update(layout) }
        }

        fun update(newDeviceState: DeviceState) {
            updateStateLog(newDeviceState)
            lastState = newDeviceState
            updateCurrentState()
        }

        fun update(newLayoutInfo: WindowLayoutInfo) {
            updateStateLog(newLayoutInfo)
            lastLayoutInfo = newLayoutInfo
            updateCurrentState()
        }
    }
}
