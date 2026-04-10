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

package androidx.xr.arcore.testing

import androidx.xr.arcore.testing.internal.FakeLifecycleManager
import androidx.xr.arcore.testing.internal.FakeRuntimeDepthMap
import androidx.xr.runtime.DepthEstimationMode
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Represents a camera view perspective on the device.
 *
 * @property width the width of the [androidx.xr.arcore.DepthMap]
 * @property height the height of the [androidx.xr.arcore.DepthMap]
 * @property rawDepthMap the [FloatBuffer] containing raw depth data
 * @property rawConfidenceMap the [ByteBuffer] containing raw depth confidence
 * @property smoothDepthMap the [FloatBuffer] containing smooth depth data
 * @property smoothConfidenceMap the [ByteBuffer] containing smooth depth confidence
 */
public class TestDepth
internal constructor(
    private val arCoreTestRule: ArCoreTestRule,
    private val fakeRuntimeDepthMap: FakeRuntimeDepthMap,
) {
    public var width: Int = 0
        set(value) {
            field = value
            if (arCoreTestRule.runtime.config.depthEstimation != DepthEstimationMode.DISABLED) {
                fakeRuntimeDepthMap.width = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var height: Int = 0
        set(value) {
            field = value
            if (arCoreTestRule.runtime.config?.depthEstimation != DepthEstimationMode.DISABLED) {
                fakeRuntimeDepthMap.height = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var rawDepthMap: FloatBuffer? = null
        set(value) {
            field = value
            if (isRawDepthConfigured()) {
                fakeRuntimeDepthMap.rawDepthMap = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var rawConfidenceMap: ByteBuffer? = null
        set(value) {
            field = value
            if (isRawDepthConfigured()) {
                fakeRuntimeDepthMap.rawConfidenceMap = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var smoothDepthMap: FloatBuffer? = null
        set(value) {
            field = value
            if (isSmoothDepthConfigured()) {
                fakeRuntimeDepthMap.smoothDepthMap = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    public var smoothConfidenceMap: ByteBuffer? = null
        set(value) {
            field = value
            if (isSmoothDepthConfigured()) {
                fakeRuntimeDepthMap.smoothConfidenceMap = value
            }
            FakeLifecycleManager.allowOneMoreCallToUpdate()
        }

    private fun isRawDepthConfigured(): Boolean =
        arCoreTestRule.runtime.config.depthEstimation == DepthEstimationMode.RAW_ONLY ||
            arCoreTestRule.runtime.config.depthEstimation == DepthEstimationMode.SMOOTH_AND_RAW

    private fun isSmoothDepthConfigured(): Boolean =
        arCoreTestRule.runtime.config.depthEstimation == DepthEstimationMode.SMOOTH_ONLY ||
            arCoreTestRule.runtime.config.depthEstimation == DepthEstimationMode.SMOOTH_AND_RAW
}
