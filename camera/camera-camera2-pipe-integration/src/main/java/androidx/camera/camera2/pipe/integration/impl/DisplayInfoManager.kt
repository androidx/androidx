/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Display
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION") // getRealSize
@Singleton
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class DisplayInfoManager @Inject constructor(context: Context) {
    private val MAX_PREVIEW_SIZE = Size(1920, 1080)

    companion object {
        private var lazyMaxDisplay: Display? = null
        private var lazyPreviewSize: Size? = null

        internal fun invalidateLazyFields() {
            lazyMaxDisplay = null
            lazyPreviewSize = null
        }

        internal val displayListener by lazy {
            object : DisplayListener {
                override fun onDisplayAdded(displayId: Int) {
                    invalidateLazyFields()
                }

                override fun onDisplayRemoved(displayId: Int) {
                    invalidateLazyFields()
                }

                override fun onDisplayChanged(displayId: Int) {
                    invalidateLazyFields()
                }
            }
        }
    }

    private val displayManager: DisplayManager by lazy {
        (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).also {
            it.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        }
    }

    val defaultDisplay: Display
        get() = getMaxSizeDisplay()

    val previewSize: Size
        get() = calculatePreviewSize()

    private fun getMaxSizeDisplay(): Display {
        lazyMaxDisplay?.let { return it }

        val displays = displayManager.displays

        var maxDisplayWhenStateNotOff: Display? = null
        var maxDisplaySizeWhenStateNotOff = -1

        var maxDisplay: Display? = null
        var maxDisplaySize = -1

        for (display: Display in displays) {
            val displaySize = Point()
            // TODO(b/230400472): Use WindowManager#getCurrentWindowMetrics(). Display#getRealSize()
            //  is deprecated since API level 31.
            display.getRealSize(displaySize)

            if (displaySize.x * displaySize.y > maxDisplaySize) {
                maxDisplaySize = displaySize.x * displaySize.y
                maxDisplay = display
            }
            if (display.state != Display.STATE_OFF) {
                if (displaySize.x * displaySize.y > maxDisplaySizeWhenStateNotOff) {
                    maxDisplaySizeWhenStateNotOff = displaySize.x * displaySize.y
                    maxDisplayWhenStateNotOff = display
                }
            }
        }

        lazyMaxDisplay = maxDisplayWhenStateNotOff ?: maxDisplay

        return checkNotNull(lazyMaxDisplay) {
            "No displays found from ${displayManager.displays}!"
        }
    }

    /**
     * Calculates the device's screen resolution, or MAX_PREVIEW_SIZE, whichever is smaller.
     */
    private fun calculatePreviewSize(): Size {
        lazyPreviewSize?.let { return it }

        val displaySize = Point()
        val display: Display = defaultDisplay
        // TODO(b/230400472): Use WindowManager#getCurrentWindowMetrics(). Display#getRealSize()
        //  is deprecated since API level 31.
        display.getRealSize(displaySize)
        var displayViewSize: Size
        displayViewSize = if (displaySize.x > displaySize.y) {
            Size(displaySize.x, displaySize.y)
        } else {
            Size(displaySize.y, displaySize.x)
        }
        if (displayViewSize.width * displayViewSize.height
            > MAX_PREVIEW_SIZE.width * MAX_PREVIEW_SIZE.height
        ) {
            displayViewSize = MAX_PREVIEW_SIZE
        }
        // TODO(b/230402463): Migrate extra cropping quirk from CameraX.

        return displayViewSize.also { lazyPreviewSize = displayViewSize }
    }
}
