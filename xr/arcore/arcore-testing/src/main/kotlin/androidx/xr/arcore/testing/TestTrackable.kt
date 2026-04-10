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

/**
 * Represents a real-world object in the user's environment that is not part of the user and which
 * can be collected by ARCore, such as a `Plane` or `AugmentedObject`.
 *
 * @property isVisible indicates whether the trackable object is currently in view of the runtime
 */
public abstract class TestTrackable {
    public abstract var isVisible: Boolean

    internal val isAddedToTestRule: Boolean
        get() = ::arCoreTestRule.isInitialized

    internal lateinit var arCoreTestRule: ArCoreTestRule
}
