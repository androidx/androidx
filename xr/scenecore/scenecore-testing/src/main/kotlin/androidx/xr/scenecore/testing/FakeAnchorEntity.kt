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

    /**
     * The underlying [androidx.xr.arcore.runtime.Anchor] instance that this fake entity represents,
     * set when [setAnchor] is called.
     *
     * @see androidx.xr.arcore.runtime.Anchor
     * @see androidx.xr.arcore.testing.FakeRuntimeAnchor
     */
    internal var anchor: Anchor? = null

    private var onStateChangedListener: OnStateChangedListener? =
        OnStateChangedListener { newState ->
            _state = newState
        }

    private var _state: @AnchorEntity.State Int = AnchorEntity.State.UNANCHORED

    /** The current state of the anchor. */
    override val state: @AnchorEntity.State Int
        get() = _state

    /** Registers a listener to be called when the state of the anchor changes. */
    @Suppress("ExecutorRegistration")
    override fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener?) {
        this.onStateChangedListener = onStateChangedListener
        onStateChangedListener?.onStateChanged(_state)
    }

    override fun setAnchor(anchor: Anchor): Boolean {
        // detach current
        this.anchor?.detach()
        this.anchor = anchor
        onStateChangedListener?.onStateChanged(AnchorEntity.State.ANCHORED)
        return true
    }

    @Suppress("RestrictedApiAndroidX")
    override fun getPose(relativeTo: Int): Pose {
        return anchor?.runtimeAnchor?.pose ?: Pose.Identity
    }

    @Suppress("RestrictedApiAndroidX")
    override fun dispose() {
        anchor?.runtimeAnchor?.detach()
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
        onStateChangedListener?.onStateChanged(newState)
    }
}
