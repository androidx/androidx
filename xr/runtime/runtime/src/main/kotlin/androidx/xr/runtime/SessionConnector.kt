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

package androidx.xr.runtime

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.JxrRuntime

/**
 * Describes classes that have a lifecycle equivalent of a particular [Session] (i.e. singletons)
 * and provide Session-level functionality via extension functions. The Session is responsible for
 * owning these objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("NotCloseable")
public interface SessionConnector {
    /** Initializes the [SessionConnector]. */
    public fun initialize(runtimes: List<JxrRuntime>)

    /** Closes the [SessionConnector]. Called when the [Session] is destroyed. */
    public fun close()
}
