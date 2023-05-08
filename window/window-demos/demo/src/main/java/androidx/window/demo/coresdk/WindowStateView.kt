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
import android.view.Display.DEFAULT_DISPLAY
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.window.demo.R
import androidx.window.demo.databinding.WindowStateViewBinding
import java.text.SimpleDateFormat
import java.util.Date

/** View to show the display configuration from the latest update. */
class WindowStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val displayManager: DisplayManager
    private val windowManager: WindowManager

    private val timestampView: WindowStateConfigView
    private val displayRotationView: WindowStateConfigView
    private val displayBoundsView: WindowStateConfigView
    private val prevDisplayRotationView: WindowStateConfigView
    private val prevDisplayBoundsView: WindowStateConfigView

    private val shouldHidePrevConfig: Boolean
    private var lastDisplayRotation = -1
    private val lastDisplayBounds = Rect()

    init {
        displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val viewBinding = WindowStateViewBinding.inflate(LayoutInflater.from(context), this, true)
        timestampView = viewBinding.timestampView
        displayRotationView = viewBinding.displayRotationView
        displayBoundsView = viewBinding.displayBoundsView
        prevDisplayRotationView = viewBinding.prevDisplayRotationView
        prevDisplayBoundsView = viewBinding.prevDisplayBoundsView

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WindowStateView,
            0, 0).apply {
            try {
                getString(R.styleable.WindowStateView_title)?.let {
                    viewBinding.callbackTitle.text = it
                }
                shouldHidePrevConfig = getBoolean(
                    R.styleable.WindowStateView_hidePrevConfig,
                    false)
                if (shouldHidePrevConfig) {
                    timestampView.visibility = GONE
                    displayRotationView.shouldHighlightChange = false
                    displayBoundsView.shouldHighlightChange = false
                    prevDisplayRotationView.visibility = GONE
                    prevDisplayBoundsView.visibility = GONE
                } else {
                    displayRotationView.shouldHighlightChange = true
                    displayBoundsView.shouldHighlightChange = true
                }
            } finally {
                recycle()
            }
        }
    }

    /** Called when the corresponding system callback is invoked. */
    fun onWindowStateCallbackInvoked() {
        val displayRotation = displayManager.getDisplay(DEFAULT_DISPLAY).rotation
        val displayBounds = windowManager.maximumWindowMetrics.bounds

        if (shouldHidePrevConfig &&
            displayRotation == lastDisplayRotation &&
            displayBounds == lastDisplayBounds) {
            // Skip if the state is unchanged.
            return
        }

        timestampView.updateValue(TIME_FORMAT.format(Date()))
        displayRotationView.updateValue(displayRotation.toString())
        displayBoundsView.updateValue(displayBounds.toString())

        if (!shouldHidePrevConfig && lastDisplayRotation != -1) {
            // Skip if there is no previous value.
            prevDisplayRotationView.updateValue(lastDisplayRotation.toString())
            prevDisplayBoundsView.updateValue(lastDisplayBounds.toString())
        }

        lastDisplayRotation = displayRotation
        lastDisplayBounds.set(displayBounds)
    }

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    }
}