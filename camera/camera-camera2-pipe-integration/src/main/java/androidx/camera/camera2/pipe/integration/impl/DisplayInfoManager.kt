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
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.integration.compat.workaround.DisplaySizeCorrector
import androidx.camera.camera2.pipe.integration.compat.workaround.MaxPreviewSize
import androidx.camera.core.impl.utils.ContextUtil
import androidx.camera.core.internal.utils.SizeUtil

/**
 * A singleton class to retrieve display related information.
 *
 * This class uses a caching strategy to reduce calls to the system's [DisplayManager].
 *
 * The cached information is lazy-loaded. It is only fetched from the [DisplayManager] when first
 * needed. A [DisplayManager.DisplayListener] invalidates the cache (by setting it to null) whenever
 * the display configuration changes. The next call to [getDisplays] or [getPreviewSize] will then
 * fetch the fresh data.
 */
@Suppress("DEPRECATION") // getRealSize
public class DisplayInfoManager private constructor(context: Context) {
    private val maxPreviewSize = MaxPreviewSize()
    private val displaySizeCorrector = DisplaySizeCorrector()

    /** A lock to ensure thread-safe access to the cached display information. */
    private val lock = Any()

    /**
     * A cache for the array of [Display] objects. It is invalidated (set to null) by the
     * [displayListener] and re-populated on the next call to [getDisplays].
     */
    @Volatile private var displays: Array<Display>? = null

    /**
     * A listener to detect display changes.
     *
     * This listener invalidates the cached display information (by setting it to null) whenever the
     * display configuration changes. The next call to [getDisplays] or [getPreviewSize] will then
     * fetch the fresh data.
     */
    private val displayListener: DisplayListener =
        object : DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                synchronized(lock) {
                    displays = null
                    previewSize = null
                }
            }

            override fun onDisplayRemoved(displayId: Int) {
                synchronized(lock) {
                    displays = null
                    previewSize = null
                }
            }

            override fun onDisplayChanged(displayId: Int) {
                synchronized(lock) {
                    displays = null
                    previewSize = null
                }
            }
        }

    public companion object {
        private val MAX_PREVIEW_SIZE = Size(1920, 1080)
        /** This is the smallest size from a device which had issue reported to CameraX. */
        private val ABNORMAL_DISPLAY_SIZE_THRESHOLD: Size = Size(320, 240)
        /**
         * The fallback display size for the case that the retrieved display size is abnormally
         * small and no correct display size can be retrieved from DisplaySizeCorrector.
         */
        private val FALLBACK_DISPLAY_SIZE: Size = Size(640, 480)

        @Volatile private var instance: DisplayInfoManager? = null

        public fun getInstance(context: Context): DisplayInfoManager {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: DisplayInfoManager(ContextUtil.getPersistentApplicationContext(context))
                            .also { instance = it }
                }
        }

        /**
         * Test purpose only. To release the instance so that the test can create a new instance.
         */
        @VisibleForTesting
        public fun releaseInstance() {
            instance?.let {
                synchronized(this) {
                    it.displayManager.unregisterDisplayListener(it.displayListener)
                    instance = null
                }
            }
        }
    }

    private val displayManager: DisplayManager =
        (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).also {
            it.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        }

    @Volatile private var previewSize: Size? = null

    /** Refreshes the preview size. */
    public fun refreshPreviewSize() {
        synchronized(lock) { previewSize = calculatePreviewSize() }
    }

    /**
     * PREVIEW refers to the best size match to the device's screen resolution, or to 1080p
     * (1920x1080), whichever is smaller.
     */
    public fun getPreviewSize(): Size {
        // Use cached value to speed up since this would be called multiple times.
        synchronized(lock) {
            if (previewSize != null) {
                return previewSize as Size
            }

            previewSize = calculatePreviewSize()

            return previewSize!!
        }
    }

    /**
     * Gets the array of displays, using a cache to avoid unnecessary calls to the system. The cache
     * is lazily populated.
     */
    private fun getDisplays(): Array<Display> {
        synchronized(lock) {
            val cachedDisplays = displays
            if (cachedDisplays != null) {
                return cachedDisplays
            }
            val newDisplays = displayManager.displays
            this.displays = newDisplays
            return newDisplays
        }
    }

    public fun getMaxSizeDisplay(skipStateOffDisplay: Boolean = true): Display {
        val displays = getDisplays()

        if (displays.size == 1) {
            return displays[0]
        }

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

        val result =
            if (skipStateOffDisplay) {
                maxDisplayWhenStateNotOff ?: maxDisplay
            } else {
                maxDisplay
            }

        return checkNotNull(result) { "No displays found from ${displays.contentToString()}!" }
    }

    /** Calculates the device's screen resolution, or MAX_PREVIEW_SIZE, whichever is smaller. */
    private fun calculatePreviewSize(): Size {
        var displayViewSize = getCorrectedDisplaySize()
        if (SizeUtil.isSmallerByArea(MAX_PREVIEW_SIZE, displayViewSize)) {
            displayViewSize = MAX_PREVIEW_SIZE
        }
        return maxPreviewSize.getMaxPreviewResolution(displayViewSize)
    }

    private fun getCorrectedDisplaySize(): Size {
        val displaySize = Point()
        getMaxSizeDisplay(false).getRealSize(displaySize)
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
