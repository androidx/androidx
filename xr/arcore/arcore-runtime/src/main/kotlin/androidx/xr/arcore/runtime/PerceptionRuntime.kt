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

package androidx.xr.arcore.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.time.ComparableTimeMark

/** Set of behaviors that collectively define a runtime. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PerceptionRuntime : JxrRuntime {
    /** Mandatory lifecycle runtime behavior. */
    public val lifecycleManager: LifecycleManager

    /** Mandatory perception runtime behavior. */
    public val perceptionManager: PerceptionManager

    override fun initialize() {
        lifecycleManager.create()
    }

    override fun configure(config: Config) {
        lifecycleManager.configure(config)
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
