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

package androidx.camera.common.testing

import androidx.camera.common.CameraId
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility for tracking and generating fake [CameraId] instances for testing.
 *
 * Generated camera IDs are prefixed with "FakeCamera-" to ensure they are non-numerical. This helps
 * identify code that incorrectly assumes camera IDs are parsable as integers (e.g., assuming camera
 * ID "0" can be parsed to `0`).
 */
public object FakeCameraIds {
    private val fakeCameraIds = AtomicInteger(0)

    /**
     * A default fake [CameraId] instance ("FakeCamera-default").
     *
     * Use this when a test only needs a single camera ID and uniqueness across different components
     * is not required.
     */
    @JvmStatic
    @get:JvmName("getDefault")
    public val default: CameraId = CameraId("FakeCamera-default")

    /**
     * Generates a new, unique fake [CameraId].
     *
     * The generated ID will have the format "FakeCamera-{index}", where index is a monotonically
     * increasing integer starting from 0.
     *
     * @return A unique [CameraId] instance.
     */
    @JvmStatic
    @JvmName("next")
    public fun next(): CameraId = CameraId("FakeCamera-${fakeCameraIds.getAndIncrement()}")
}
