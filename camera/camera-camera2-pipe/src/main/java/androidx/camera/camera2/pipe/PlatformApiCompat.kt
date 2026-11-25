/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.hardware.camera2.MultiResolutionImageReader
import android.hardware.camera2.params.MultiResolutionStreamConfigurationMap
import android.hardware.camera2.params.MultiResolutionStreamInfo
import androidx.annotation.RestrictTo

/**
 * PlatformApiCompat is an interface intended for experimental feature integration where the
 * implementation will be done outside CameraPipe's source tree.
 *
 * All methods must provide a default implementation. This is needed for source compatibility.
 * Additionally, all methods must specify and be backed by a flag which can be used to determine
 * whether said methods are available.
 *
 * Before graduating any of these methods, all usages outside CameraPipe must be properly removed to
 * ensure continuous source compatibility.
 *
 * This interface is integrated and configured as part of [CameraPipe.Config].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PlatformApiCompat {
    /** Whether mixed-format RAW multi-resolution image reader APIs are enabled. */
    public fun isMultiResolutionRawFormatsEnabled(): Boolean = false

    /**
     * Returns supported mixed-format RAW [MultiResolutionStreamInfo] entries from
     * [multiResolutionStreamConfigurationMap], or an empty collection if unsupported.
     *
     * This implementation is guarded by [isMultiResolutionRawFormatsEnabled].
     */
    public fun getMixedRawOutputInfo(
        multiResolutionStreamConfigurationMap: MultiResolutionStreamConfigurationMap
    ): Collection<MultiResolutionStreamInfo> {
        throw UnsupportedOperationException("API not supported on current platform")
    }

    /**
     * Returns whether concurrent readers are supported for mixed-format RAW output streams on
     * [multiResolutionStreamConfigurationMap].
     *
     * This implementation is guarded by [isMultiResolutionRawFormatsEnabled].
     */
    public fun isMixedRawConcurrencySupported(
        multiResolutionStreamConfigurationMap: MultiResolutionStreamConfigurationMap
    ): Boolean {
        throw UnsupportedOperationException("API not supported on current platform")
    }

    /**
     * Creates a [MultiResolutionStreamInfo] with the given dimensions, [physicalCameraId], and
     * [format].
     *
     * This implementation is guarded by [isMultiResolutionRawFormatsEnabled].
     */
    public fun newMultiResolutionStreamInfo(
        streamWidth: Int,
        streamHeight: Int,
        physicalCameraId: String,
        format: Int,
    ): MultiResolutionStreamInfo {
        throw UnsupportedOperationException("API not supported on current platform")
    }

    /**
     * Returns the image format of [multiResolutionStreamInfo].
     *
     * This implementation is guarded by [isMultiResolutionRawFormatsEnabled].
     */
    public fun getFormat(multiResolutionStreamInfo: MultiResolutionStreamInfo): Int {
        throw UnsupportedOperationException("API not supported on current platform")
    }

    /**
     * Builds a [MultiResolutionImageReader] using the per-stream formats in [streams]. If [usage]
     * or [concurrentOutputsEnabled] is specified, it will be set on the builder before building.
     *
     * This implementation is guarded by [isMultiResolutionRawFormatsEnabled].
     */
    public fun buildMultiResolutionImageReader(
        streams: Collection<MultiResolutionStreamInfo>,
        maxImages: Int,
        usage: Long? = null,
        concurrentOutputsEnabled: Boolean? = null,
    ): MultiResolutionImageReader {
        throw UnsupportedOperationException("API not supported on current platform")
    }
}
