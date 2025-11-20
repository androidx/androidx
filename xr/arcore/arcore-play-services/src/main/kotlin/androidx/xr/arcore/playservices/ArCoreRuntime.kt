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

package androidx.xr.arcore.playservices

import androidx.annotation.RestrictTo
import androidx.xr.arcore.runtime.PerceptionRuntime
import androidx.xr.runtime.Config
import kotlin.time.ComparableTimeMark

/**
 * Implementation of the [androidx.xr.arcore.runtime.PerceptionRuntime] interface using ARCore.
 *
 * @property lifecycleManager that manages the lifecycle of the ARCore session.
 * @property perceptionManager that manages the perception capabilities of a runtime using ARCore.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ArCoreRuntime
internal constructor(
    override val lifecycleManager: ArCoreManager,
    override val perceptionManager: ArCorePerceptionManager,
) : PerceptionRuntime {

    override fun initialize() {
        lifecycleManager.create()
    }

    override fun resume() {
        lifecycleManager.resume()
    }

    override fun pause() {
        lifecycleManager.pause()
    }

    override suspend fun update(): ComparableTimeMark? {
        return lifecycleManager.update()
    }

    override fun configure(config: Config) {
        lifecycleManager.configure(config)
    }

    override fun destroy() {
        lifecycleManager.stop()
    }
}
