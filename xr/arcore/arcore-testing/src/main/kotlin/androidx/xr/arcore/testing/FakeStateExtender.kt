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

package androidx.xr.arcore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.StateExtender
import androidx.xr.runtime.internal.JxrRuntime

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeStateExtender() : StateExtender {

    /** Whether the [StateExtender] has been initialized or not. */
    public var isInitialized: Boolean = false

    /** List of [androidx.xr.runtime.CoreState] instances that have been extended. */
    public val extended: MutableList<CoreState> = mutableListOf<CoreState>()

    override fun initialize(runtimes: List<JxrRuntime>) {
        isInitialized = true
    }

    override suspend fun extend(coreState: CoreState) {
        extended.add(coreState)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnotherFakeStateExtender() : StateExtender {

    /** Whether the [StateExtender] has been initialized or not. */
    public var isInitialized: Boolean = false

    /** List of [CoreState] instances that have been extended. */
    public val extended: MutableList<CoreState> = mutableListOf<CoreState>()

    override fun initialize(runtimes: List<JxrRuntime>) {
        isInitialized = true
    }

    override suspend fun extend(coreState: CoreState) {
        extended.add(coreState)
    }
}
