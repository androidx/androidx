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
package androidx.xr.arcore.projected

import android.content.Context
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.LifecycleManager
import kotlin.coroutines.CoroutineContext
import kotlin.time.ComparableTimeMark

/**
 * Manages the lifecycle of a Projected session.
 *
 * @property context The [Context] instance
 * @property perceptionManager the [ProjectedPerceptionManager] instance
 * @property timeSource the [ProjectedTimeSource] instance
 * @property coroutineContext the [CoroutineContext] for this manager
 * @property testPerceptionService an optional [IProjectedPerceptionService] for testing
 * @property config the current [Config] of the session
 */
@Suppress("NotCloseable")
internal class ProjectedManager(internal val timeSource: ProjectedTimeSource) : LifecycleManager {
    override var config: Config = Config()

    /**
     * This method implements the [LifecycleManager.create] method.
     *
     * This method must be called before any operations can be performed by the
     * [ProjectedPerceptionManager].
     */
    override fun create() {}

    override fun configure(config: Config) {
        this.config = config
    }

    override fun resume() {}

    override suspend fun update(): ComparableTimeMark {
        return timeSource.markNow()
    }

    override fun pause() {}

    override fun stop() {}
}
