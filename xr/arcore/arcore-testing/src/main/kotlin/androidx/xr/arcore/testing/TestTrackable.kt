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

package androidx.xr.arcore.testing

import androidx.xr.arcore.runtime.Trackable
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.internal.FakePerceptionManager

/**
 * Represents a real-world object in the user's environment that is not part of the user and which
 * can be collected by ARCore, such as a `Plane` or `AugmentedObject`.
 *
 * @property isVisible indicates whether the trackable object is currently in view of the runtime
 */
public abstract class TestTrackable internal constructor() {
    public abstract var isVisible: Boolean

    internal abstract val fakeRuntimeTrackable: Trackable

    internal val isAddedToTestRule: Boolean
        get() = ::arCoreTestRule.isInitialized

    /**
     * Returns `true` if this Trackable's [TrackingState] has not yet been set to
     * [TrackingState.STOPPED] by the [FakePerceptionManager]. A value of `false` means property
     * changes on the `TestTrackable` will not be reflected in the corresponding ArCore API.
     */
    internal val canBeTracked: Boolean
        get() = fakeRuntimeTrackable.trackingState != TrackingState.STOPPED

    /**
     * The [ArCoreTestRule] currently in use. This reference is initialized when the `TestTrackable`
     * is added via [ArCoreTestRule.addTrackables].
     */
    internal lateinit var arCoreTestRule: ArCoreTestRule

    /**
     * Returns `true` if the runtime's configuration enables this Trackable type. If no runtime
     * reference is available yet, the result will be `false`.
     */
    internal abstract fun isTrackableConfigured(): Boolean
}
