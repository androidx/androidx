/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core.impl

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/** Camera extension capabilities. */
@RequiresApi(31)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraExtensionCapabilities {
    /** Gets the extension characteristic value for the given key. */
    public fun <T> get(key: Any): T?

    /** Returns whether postview is supported or not. */
    public fun isPostviewSupported(): Boolean

    /** Returns whether capture process progress is supported or not. */
    public fun isCaptureProcessProgressSupported(): Boolean

    /** Returns whether extension strength is supported or not. */
    public fun isExtensionStrengthSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return getAvailableCaptureRequestKeys().contains(CaptureRequest.EXTENSION_STRENGTH)
        }
        return false
    }

    /** Returns whether current extension mode is supported or not. */
    public fun isCurrentExtensionModeSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return getAvailableCaptureResultKeys().contains(CaptureResult.EXTENSION_CURRENT_TYPE)
        }
        return false
    }

    /** Returns whether night mode indicator is supported or not. */
    public fun isNightModeIndicatorSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= 36) {
            return getAvailableCaptureResultKeys()
                .contains(CaptureResult.EXTENSION_NIGHT_MODE_INDICATOR)
        }
        return false
    }

    /** Returns the supported output sizes for the given format. */
    public fun getOutputSizes(format: Int): Set<Size>

    /** Returns the supported output sizes for the given class. */
    public fun getOutputSizes(klass: Class<*>): Set<Size>

    /** Returns the supported postview sizes for the given capture size and format. */
    public fun getPostviewSizes(captureSize: Size, format: Int): Set<Size>

    /**
     * Returns the estimated capture latency range for the given capture size and format in
     * milliseconds.
     */
    public fun getEstimatedCaptureLatencyRangeMillis(captureSize: Size, format: Int): Range<Long>?

    /** Returns the available capture request keys. */
    public fun getAvailableCaptureRequestKeys(): Set<Any>

    /** Returns the available capture result keys. */
    public fun getAvailableCaptureResultKeys(): Set<Any>

    /** Returns the available characteristics key values. */
    public fun getAvailableCharacteristicsKeyValues(): List<Pair<Any, Any>> = emptyList()
}
