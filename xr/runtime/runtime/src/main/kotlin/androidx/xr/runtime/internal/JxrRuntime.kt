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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Config
import kotlin.time.ComparableTimeMark

/**
 * Describes a runtime that has a lifecycle equivalent of a particular [androidx.xr.runtime.Session]
 * and requires lifecycle and state updates. The Session is responsible for owning these objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("NotCloseable")
public interface JxrRuntime {
    /** The configuration of the runtime. */
    public val config: Config

    /**
     * Executes the [JxrRuntime] initialization logic. It is necessary to call [resume] after
     * calling this method to start the runtime's execution logic.
     */
    public fun initialize() {}

    /**
     * Resumes execution from a paused or init state. It is necessary to have called [initialize]
     * before calling this method.
     */
    public fun resume() {}

    /** Pauses execution while retaining the state in memory. */
    public fun pause() {}

    /**
     * Sets or changes the configuration to use, which will affect the availability of properties or
     * features in other managers. It is necessary to have called [initialize] before calling this
     * method.
     */
    public fun configure(config: Config) {}

    /**
     * Updates the state of the system. The call is blocking and will return once the underlying
     * implementation has been updated or a platform-specific timeout has been reached. This method
     * can only be called when the runtime is resumed.
     *
     * @return the timemark of the latest state. This value is to be used for comparison with other
     *   timemarks and not to be used for absolute time calculations.
     */
    public suspend fun update(): ComparableTimeMark? {
        return null
    }

    /**
     * Stops the execution and releases all resources. It is not valid to call any other method
     * after calling [destroy]. The runtime must not be resumed when this method is called.
     */
    public fun destroy() {}

    /** Gets the pointer to the underlying native session if applicable. */
    public val sessionPointer: Long?
        get() = null
}
