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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.Config
import androidx.xr.runtime.DisplayBlendMode
import kotlin.time.ComparableTimeMark

/**
 * Fake implementation of [PerceptionRuntime] for testing purposes.
 *
 * @property lifecycleManager the [FakeLifecycleManager] for this fake runtime
 * @property perceptionManager the [FakePerceptionManager] for this fake runtime
 * @property xrDevicePreferredDisplayBlendMode the value that will be returned by
 *   [androidx.xr.runtime.XrDevice.getPreferredDisplayBlendMode]
 */
@Suppress("DataClassDefinition")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public data class FakePerceptionRuntime(
    override val lifecycleManager: FakeLifecycleManager,
    override val perceptionManager: FakePerceptionManager,
) : PerceptionRuntime {
    public var xrDevicePreferredDisplayBlendMode: DisplayBlendMode = DisplayBlendMode.NO_DISPLAY

    override fun initialize() {
        lifecycleManager.create()
    }

    override fun configure(config: Config) {
        lifecycleManager.configure(config)
    }

    override fun getPreferredDisplayBlendMode(): DisplayBlendMode {
        return xrDevicePreferredDisplayBlendMode
    }

    override fun resume() {
        lifecycleManager.resume()
    }

    override suspend fun update(): ComparableTimeMark? {
        return lifecycleManager.update()
    }

    override fun pause() {
        lifecycleManager.pause()
    }

    override fun destroy() {
        lifecycleManager.stop()
    }
}
