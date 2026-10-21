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

package androidx.work.impl

import androidx.annotation.RestrictTo
import androidx.work.Configuration

/**
 * Read-only Java access to the Kotlin-`internal` process-isolation flag on [Configuration] for
 * callers within `work-runtime` (such as [WorkManagerImpl]).
 *
 * This is deliberately different from other internal knobs (which are exposed directly on
 * [Configuration] with `@RestrictTo(LIBRARY_GROUP)`):
 * - Keeping `Configuration.isProcessIsolationEnabled` and
 *   `Configuration.Builder.setProcessIsolationEnabled` Kotlin `internal` (+ `@JvmSynthetic`) keeps
 *   them out of the tracked API surface and adds no non-synthetic accessor to [Configuration]. Like
 *   `@RestrictTo`, this is a visibility measure, not an access-control boundary.
 * - It keeps [Configuration] free of `LIBRARY_GROUP` bytecode methods so the eventual public API
 *   shape remains flexible without requiring a deprecation cycle on [Configuration].
 * - Exposing only the read-only [isProcessIsolationEnabled] accessor here lets internal Java
 *   callers (such as [WorkManagerImpl]) inspect the flag without exposing a setter on this bridge.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object IsolationInternalBridge {
    /** Returns whether process isolation is enabled in the given [configuration]. */
    @JvmStatic
    public fun isProcessIsolationEnabled(configuration: Configuration): Boolean =
        configuration.isProcessIsolationEnabled
}
