/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.openxr

import android.app.Activity
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark

/**
 * Manages the lifecycle of an OpenXR session.
 *
 * @property activity the [Activity] instance
 * @property perceptionManager the [OpenXrPerceptionManager] instance
 * @property timeSource the [OpenXrTimeSource] instance
 * @property nativePointer a pointer to the native `OpenXrManager`
 * @property sessionPointer a pointer to the native
 *   [XrSession](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrSession)
 * @property instancePointer a pointer to the native
 *   [XrInstance](https://registry.khronos.org/OpenXR/specs/1.0/html/xrspec.html#XrInstance)
 * @property config the current [Config] of the session
 */
@Suppress("NotCloseable")
internal class OpenXrManager internal constructor(internal val timeSource: OpenXrTimeSource) :
    LifecycleManager {

    private companion object {
        private const val KEY_API_KEY = "com.google.android.ar.API_KEY"
        private val contextList = mutableListOf<Context>()
    }

    internal var nativePointer: Long = 0L

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) override var sessionPointer: Long = 0L

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) var instancePointer: Long = 0L

    internal var instanceProcAddr: Long = 0L

    override fun create() {}

    override var config: Config =
        Config(
            PlaneTrackingMode.DISABLED,
            HandTrackingMode.DISABLED,
            DeviceTrackingMode.DISABLED,
            DepthEstimationMode.DISABLED,
            AnchorPersistenceMode.LOCAL,
            augmentedImageDatabase = null,
        )
        private set

    @OptIn(PreviewSpatialApi::class)
    override fun configure(config: Config) {
        this.config = config
    }

    override fun resume() {}

    override suspend fun update(): ComparableTimeMark {
        val now = timeSource.markNow()
        return now
    }

    override fun pause() {}

    override fun stop() {}
}
