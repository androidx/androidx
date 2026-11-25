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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.RotatableComponent
import androidx.xr.scenecore.testing.internal.FakeRotatableComponent as InternalFakeRotatableComponent

/** Test-only implementation of [androidx.xr.scenecore.runtime.RotatableComponent] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeRotatableComponent
internal constructor(internal val fakeInternal: InternalFakeRotatableComponent) :
    FakeComponent(), RotatableComponent {

    public constructor() : this(InternalFakeRotatableComponent())

    /** The number of times [reset] has been called. */
    public val resetCount: Int
        get() = fakeInternal.resetCount

    override fun reset() {
        fakeInternal.reset()
    }
}
