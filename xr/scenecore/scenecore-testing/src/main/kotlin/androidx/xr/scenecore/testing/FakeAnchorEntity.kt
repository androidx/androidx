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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.arcore.Anchor
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.AnchorEntity.OnStateChangedListener
import androidx.xr.scenecore.testing.internal.FakeAnchorEntity as InternalFakeAnchorEntity

/** Test-only implementation of [androidx.xr.scenecore.runtime.AnchorEntity] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeAnchorEntity internal constructor(fakeInternal: InternalFakeAnchorEntity) :
    FakeSystemSpaceEntity(fakeInternal), AnchorEntity {

    public constructor() : this(InternalFakeAnchorEntity())

    private val internalAnchorEntity: InternalFakeAnchorEntity
        get() = fakeInternal as InternalFakeAnchorEntity

    /** The current state of the anchor. */
    override val state: @AnchorEntity.State Int
        get() = internalAnchorEntity.state

    /** Registers a listener to be called when the state of the anchor changes. */
    @Suppress("ExecutorRegistration")
    override fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener?) {
        internalAnchorEntity.setOnStateChangedListener(onStateChangedListener)
    }

    override fun setAnchor(anchor: Anchor): Boolean {
        return internalAnchorEntity.setAnchor(anchor)
    }

    @Suppress("RestrictedApiAndroidX")
    override fun getPose(relativeTo: Int): Pose {
        return internalAnchorEntity.getPose(relativeTo)
    }

    @Suppress("RestrictedApiAndroidX")
    override fun dispose() {
        internalAnchorEntity.dispose()
    }

    /**
     * Test function to invoke the onStateChanged listener callback.
     *
     * This function is used to simulate the update of the underlying
     * [androidx.xr.scenecore.runtime.AnchorEntity.State], triggering the registered listener. In
     * tests, you can call this function to manually trigger the listener and verify that your code
     * responds correctly to state updates.
     */
    public fun onStateChanged(newState: @AnchorEntity.State Int) {
        internalAnchorEntity.onStateChanged(newState)
    }
}
