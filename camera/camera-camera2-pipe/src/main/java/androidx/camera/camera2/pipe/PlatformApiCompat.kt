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
import android.view.Surface
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

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
    /** Whether concurrent multi-resolution image reader is enabled. */
    public fun isMultiResolutionConcurrentReadersEnabled(): Boolean = false

    /**
     * Whether concurrency is supported for [format] on the [multiResolutionStreamConfigurationMap].
     *
     * This implementation is guarded by [isMultiResolutionConcurrentReadersEnabled].
     */
    public fun isConcurrentReadersSupported(
        multiResolutionStreamConfigurationMap: MultiResolutionStreamConfigurationMap,
        format: Int,
    ): Boolean {
        throw UnsupportedOperationException("API not supported on current platform")
    }

    /**
     * Builds a MultiResolutionImageReader with the Builder class. If [usage] or
     * [concurrentOutputsEnabled] is specified, it will be set on the builder before building.
     *
     * This implementation is guarded by [isMultiResolutionConcurrentReadersEnabled].
     */
    public fun buildMultiResolutionImageReader(
        streams: Collection<MultiResolutionStreamInfo>,
        format: Int,
        maxImages: Int,
        usage: Long?,
        concurrentOutputsEnabled: Boolean?,
    ): MultiResolutionImageReader {
        throw UnsupportedOperationException("API not supported on current platform")
    }

    /**
     * Sets the active output surfaces listener on the multi-resolution image reader.
     *
     * This implementation is guarded by [isMultiResolutionConcurrentReadersEnabled].
     */
    public fun setOnActiveOutputSurfacesListener(
        multiResolutionImageReader: MultiResolutionImageReader,
        executor: Executor,
        listener: CameraOnActiveOutputSurfacesListener,
    ) {
        throw UnsupportedOperationException("API not supported on current platform")
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface CameraOnActiveOutputSurfacesListener {
    public fun onActiveOutputSurfaces(
        activeOutputSurfaces: List<Surface>,
        timestamp: Long,
        frameNumber: Long,
    )
}
