/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.demo.coresdk

import android.content.Context
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.util.AttributeSet
import android.util.Log
import android.view.Display.DEFAULT_DISPLAY
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.window.demo.R
import androidx.window.demo.databinding.WindowStateViewBinding
import androidx.window.layout.WindowMetricsCalculator
import java.text.SimpleDateFormat
import java.util.Date

/** View to show the display configuration from the latest update. */
class WindowStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var title: String = "N/A"

    /**
     * [DisplayManager]s and [WindowManager]s from `Activity` and `Application` are updated from
     * different resource configurations, so we show config from them separately.
     */
    private val applicationDisplayManager: DisplayManager
    private val activityDisplayManager: DisplayManager
    private val windowMetricsCalculator: WindowMetricsCalculator

    private val timestampView: WindowStateConfigView
    private val applicationDisplayRotationView: WindowStateConfigView
    private val activityDisplayRotationView: WindowStateConfigView
    private val applicationDisplayBoundsView: WindowStateConfigView
    private val activityDisplayBoundsView: WindowStateConfigView
    private val prevApplicationDisplayRotationView: WindowStateConfigView
    private val prevActivityDisplayRotationView: WindowStateConfigView
    private val prevApplicationDisplayBoundsView: WindowStateConfigView
    private val prevActivityDisplayBoundsView: WindowStateConfigView

    private val shouldHidePrevConfig: Boolean
    private var lastApplicationDisplayRotation = -1
    private var lastActivityDisplayRotation = -1
    private val lastApplicationDisplayBounds = Rect()
    private val lastActivityDisplayBounds = Rect()

    init {
        applicationDisplayManager =
            context.applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        activityDisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        windowMetricsCalculator = WindowMetricsCalculator.getOrCreate()

        val viewBinding = WindowStateViewBinding.inflate(LayoutInflater.from(context), this, true)
        timestampView = viewBinding.timestampView
        applicationDisplayRotationView = viewBinding.applicationDisplayRotationView
        activityDisplayRotationView = viewBinding.activityDisplayRotationView
        applicationDisplayBoundsView = viewBinding.applicationDisplayBoundsView
        activityDisplayBoundsView = viewBinding.activityDisplayBoundsView
        prevApplicationDisplayRotationView = viewBinding.prevApplicationDisplayRotationView
        prevActivityDisplayRotationView = viewBinding.prevActivityDisplayRotationView
        prevApplicationDisplayBoundsView = viewBinding.prevApplicationDisplayBoundsView
        prevActivityDisplayBoundsView = viewBinding.prevActivityDisplayBoundsView

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WindowStateView,
            0, 0).apply {
            try {
                getString(R.styleable.WindowStateView_title)?.let {
                    viewBinding.callbackTitle.text = it
                    title = it
                }
                shouldHidePrevConfig = getBoolean(
                    R.styleable.WindowStateView_hidePrevConfig,
                    false)
                if (shouldHidePrevConfig) {
                    timestampView.visibility = GONE
                    applicationDisplayRotationView.shouldHighlightChange = false
                    activityDisplayRotationView.shouldHighlightChange = false
                    applicationDisplayBoundsView.shouldHighlightChange = false
                    activityDisplayBoundsView.shouldHighlightChange = false
                    prevApplicationDisplayRotationView.visibility = GONE
                    prevActivityDisplayRotationView.visibility = GONE
                    prevApplicationDisplayBoundsView.visibility = GONE
                    prevActivityDisplayBoundsView.visibility = GONE
                } else {
                    applicationDisplayRotationView.shouldHighlightChange = true
                    activityDisplayRotationView.shouldHighlightChange = true
                    applicationDisplayBoundsView.shouldHighlightChange = true
                    activityDisplayBoundsView.shouldHighlightChange = true
                }
            } finally {
                recycle()
            }
        }
    }

    /** Called when the corresponding system callback is invoked. */
    fun onWindowStateCallbackInvoked() {
        val applicationDisplayRotation =
            applicationDisplayManager.getDisplay(DEFAULT_DISPLAY).rotation
        val activityDisplayRotation =
            activityDisplayManager.getDisplay(DEFAULT_DISPLAY).rotation
        val applicationDisplayBounds = windowMetricsCalculator
            .computeMaximumWindowMetrics(context.applicationContext)
            .bounds
        val activityDisplayBounds = windowMetricsCalculator
            .computeMaximumWindowMetrics(context)
            .bounds

        if (shouldHidePrevConfig &&
            applicationDisplayRotation == lastApplicationDisplayRotation &&
            activityDisplayRotation == lastActivityDisplayRotation &&
            applicationDisplayBounds == lastApplicationDisplayBounds &&
            activityDisplayBounds == lastActivityDisplayBounds) {
            // Skip if the state is unchanged.
            return
        }

        if (!shouldHidePrevConfig) {
            // Debug log for the change title.
            Log.d(WindowStateCallbackActivity.TAG, title)
        }

        timestampView.updateValue(TIME_FORMAT.format(Date()))
        applicationDisplayRotationView.updateValue(applicationDisplayRotation.toString())
        activityDisplayRotationView.updateValue(activityDisplayRotation.toString())
        applicationDisplayBoundsView.updateValue(applicationDisplayBounds.toString())
        activityDisplayBoundsView.updateValue(activityDisplayBounds.toString())

        if (!shouldHidePrevConfig && lastApplicationDisplayRotation != -1) {
            // Skip if there is no previous value.
            prevApplicationDisplayRotationView.updateValue(
                lastApplicationDisplayRotation.toString())
            prevActivityDisplayRotationView.updateValue(lastActivityDisplayRotation.toString())
            prevApplicationDisplayBoundsView.updateValue(lastApplicationDisplayBounds.toString())
            prevActivityDisplayBoundsView.updateValue(lastActivityDisplayBounds.toString())
        }

        lastApplicationDisplayRotation = applicationDisplayRotation
        lastActivityDisplayRotation = activityDisplayRotation
        lastApplicationDisplayBounds.set(applicationDisplayBounds)
        lastActivityDisplayBounds.set(activityDisplayBounds)
    }

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
