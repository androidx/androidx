/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.openxr

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.DepthMap
import androidx.xr.runtime.math.IntSize2d
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Wraps the native [XrDepthSwapchainImageANDROID] with the [DepthMap] interface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrDepthMap internal constructor(private val viewIndex: Int) : DepthMap {

    override val width: Int
        get() = resolution.width

    override val height: Int
        get() = resolution.height

    override var rawDepthMap: FloatBuffer? = null
        private set

    override var rawConfidenceMap: ByteBuffer? = null
        private set

    override var smoothDepthMap: FloatBuffer? = null
        private set

    override var smoothConfidenceMap: ByteBuffer? = null
        private set

    private var resolution: IntSize2d = IntSize2d()

    internal var depthEstimationMode: Config.DepthEstimationMode =
        Config.DepthEstimationMode.DISABLED

    internal fun updateDepthEstimationMode(depthEstimationMode: Config.DepthEstimationMode) {
        if (depthEstimationMode == Config.DepthEstimationMode.DISABLED) {
            resolution = IntSize2d()
            rawDepthMap = null
            rawConfidenceMap = null
            smoothDepthMap = null
            smoothConfidenceMap = null
        } else {
            resolution = nativeGetDepthImageWidthAndHeight()
        }
        this.depthEstimationMode = depthEstimationMode
    }

    internal fun update(depthMapBuffers: Array<ByteBuffer>) {
        when (depthEstimationMode) {
            Config.DepthEstimationMode.RAW_ONLY -> {
                check(depthMapBuffers.size == EXPECTED_RAW_BUFFER_COUNT) {
                    "Unexpected number of depth map buffers for ${depthEstimationMode.toString()} config: expected=${EXPECTED_RAW_BUFFER_COUNT}, actual=${depthMapBuffers.size}"
                }
                updateRawBuffers(depthMapBuffers)
                smoothDepthMap = null
                smoothConfidenceMap = null
            }
            Config.DepthEstimationMode.SMOOTH_ONLY -> {
                check(depthMapBuffers.size == EXPECTED_SMOOTH_BUFFER_COUNT) {
                    "Unexpected number of depth map buffers for ${depthEstimationMode.toString()} config: expected=${EXPECTED_SMOOTH_BUFFER_COUNT}, actual=${depthMapBuffers.size}"
                }
                updateSmoothBuffers(depthMapBuffers)
                rawDepthMap = null
                rawConfidenceMap = null
            }
            Config.DepthEstimationMode.SMOOTH_AND_RAW -> {
                check(
                    depthMapBuffers.size == EXPECTED_RAW_BUFFER_COUNT + EXPECTED_SMOOTH_BUFFER_COUNT
                ) {
                    "Unexpected number of depth map buffers for ${depthEstimationMode.toString()} config: expected=${EXPECTED_RAW_BUFFER_COUNT + EXPECTED_SMOOTH_BUFFER_COUNT}, actual=${depthMapBuffers.size}"
                }
                updateRawBuffers(depthMapBuffers, /* startIndex= */ 0)
                updateSmoothBuffers(depthMapBuffers, /* startIndex= */ EXPECTED_RAW_BUFFER_COUNT)
            }
        }
    }

    private fun updateRawBuffers(depthMapBuffers: Array<ByteBuffer>, startIndex: Int = 0) {
        rawDepthMap =
            depthMapBuffers[startIndex + DEPTH_INDEX + viewIndex]
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .asReadOnlyBuffer()
        rawConfidenceMap =
            depthMapBuffers[startIndex + CONFIDENCE_INDEX + viewIndex].asReadOnlyBuffer()
    }

    private fun updateSmoothBuffers(depthMapBuffers: Array<ByteBuffer>, startIndex: Int = 0) {
        smoothDepthMap =
            depthMapBuffers[startIndex + DEPTH_INDEX + viewIndex]
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .asReadOnlyBuffer()
        smoothConfidenceMap =
            depthMapBuffers[startIndex + CONFIDENCE_INDEX + viewIndex].asReadOnlyBuffer()
    }

    private companion object {
        const val DEPTH_INDEX = 0
        const val CONFIDENCE_INDEX = 2
        const val EXPECTED_RAW_BUFFER_COUNT = 4
        const val EXPECTED_SMOOTH_BUFFER_COUNT = 4
    }

    private external fun nativeGetDepthImageWidthAndHeight(): IntSize2d
}
