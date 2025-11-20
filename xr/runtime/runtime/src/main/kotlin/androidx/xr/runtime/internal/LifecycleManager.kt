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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import kotlin.time.ComparableTimeMark

/** Describes the lifecycle a runtime implementation. */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface LifecycleManager {
    /**
     * Executes the runtime initialization logic. It is necessary to call [resume] after calling
     * this method to start the runtime's execution logic.
     */
    public fun create()

    /** The current state of the runtime configuration. */
    public val config: Config

    /**
     * Sets or changes the configuration to use, which will affect the availability of properties or
     * features in other managers. It is necessary to have called [create] before calling this
     * method.
     */
    public fun configure(config: Config)

    /**
     * Resumes execution from a paused or init state. It is necessary to have called [create] before
     * calling this method.
     */
    public fun resume()

    /**
     * Updates the state of the system. The call is blocking and will return once the underlying
     * implementation has been updated or a platform-specific timeout has been reached. This method
     * can only be called when the runtime is resumed.
     *
     * @return the timemark of the latest state. This value is to be used for comparison with other
     *   timemarks and not to be used for absolute time calculations.
     */
    public suspend fun update(): ComparableTimeMark

    /** Pauses execution while retaining the state in memory. */
    public fun pause()

    /**
     * Stops the execution and releases all resources. It is not valid to call any other method
     * after calling [stop]. The runtime must not be resumed when this method is called.
     */
    public fun stop()
}
