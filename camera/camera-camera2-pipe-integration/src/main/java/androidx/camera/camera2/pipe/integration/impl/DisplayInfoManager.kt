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
import androidx.camera.camera2.pipe.integration.compat.workaround.DisplaySizeCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.MaxPreviewSize
import androidx.camera.core.internal.utils.SizeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("DEPRECATION") // getRealSize
@Singleton
public class DisplayInfoManager @Inject constructor(context: Context) {
    private val maxPreviewSize = MaxPreviewSize()
    private val displaySizeCorrector = DisplaySizeCorrector()

    public companion object {
        private val MAX_PREVIEW_SIZE = Size(1920, 1080)
        /** This is the smallest size from a device which had issue reported to CameraX. */
        private val ABNORMAL_DISPLAY_SIZE_THRESHOLD: Size = Size(320, 240)
        /**
         * The fallback display size for the case that the retrieved display size is abnormally
         * small and no correct display size can be retrieved from DisplaySizeCorrector.
         */
        private val FALLBACK_DISPLAY_SIZE: Size = Size(640, 480)
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

    public val defaultDisplay: Display
        get() = getMaxSizeDisplay()

    private var previewSize: Size? = null

    /** Update the preview size according to current display size. */
    public fun refresh() {
        previewSize = calculatePreviewSize()
    }

    /**
     * PREVIEW refers to the best size match to the device's screen resolution, or to 1080p
     * (1920x1080), whichever is smaller.
     */
    public fun getPreviewSize(): Size {
        // Use cached value to speed up since this would be called multiple times.
        if (previewSize != null) {
            return previewSize as Size
        }
        previewSize = calculatePreviewSize()
        return previewSize as Size
    }

    private fun getMaxSizeDisplay(): Display {
        lazyMaxDisplay?.let {
            return it
        }

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

        return checkNotNull(lazyMaxDisplay) { "No displays found from ${displayManager.displays}!" }
    }

    /** Calculates the device's screen resolution, or MAX_PREVIEW_SIZE, whichever is smaller. */
    private fun calculatePreviewSize(): Size {
        lazyPreviewSize?.let {
            return it
        }

        var displayViewSize = getCorrectedDisplaySize()
        if (SizeUtil.isSmallerByArea(MAX_PREVIEW_SIZE, displayViewSize)) {
            displayViewSize = MAX_PREVIEW_SIZE
        }
        return maxPreviewSize.getMaxPreviewResolution(displayViewSize).also { lazyPreviewSize = it }
    }

    private fun getCorrectedDisplaySize(): Size {
        val displaySize = Point()
        defaultDisplay.getRealSize(displaySize)
        var displayViewSize = Size(displaySize.x, displaySize.y)

        // Checks whether the display size is abnormally small.
        if (SizeUtil.isSmallerByArea(displayViewSize, ABNORMAL_DISPLAY_SIZE_THRESHOLD)) {
            // Gets the display size from DisplaySizeCorrector if the display size retrieved from
            // DisplayManager is abnormally small. Falls back the display size to 640x480 if
            // DisplaySizeCorrector doesn't contain the device's display size info.
            displayViewSize = displaySizeCorrector.displaySize ?: FALLBACK_DISPLAY_SIZE
        }

        // Flips the size to landscape orientation
        if (displayViewSize.height > displayViewSize.width) {
            displayViewSize =
                Size(/* width= */ displayViewSize.height, /* height= */ displayViewSize.width)
        }

        return displayViewSize
    }
}
