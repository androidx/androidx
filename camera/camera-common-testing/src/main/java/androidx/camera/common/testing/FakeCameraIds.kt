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
 * Utility class for tracking and creating Fake [CameraId] instances for use in testing.
 *
 * These id's are intentionally non-numerical to help prevent code that may assume that camera2
 * camera ids are parsable.
 */
public object FakeCameraIds {
    private val fakeCameraIds = AtomicInteger(0)

    @JvmStatic
    @get:JvmName("getDefault")
    public val default: CameraId = CameraId("FakeCamera-default")

    @JvmStatic
    @JvmName("next")
    public fun next(): CameraId = CameraId("FakeCamera-${fakeCameraIds.getAndIncrement()}")
}
