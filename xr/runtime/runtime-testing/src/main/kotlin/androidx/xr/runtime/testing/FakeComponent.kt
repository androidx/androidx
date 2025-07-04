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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Component
import androidx.xr.runtime.internal.Entity

/** Test-only implementation of [Component] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeComponent : Component {
    /**
     * For test purposes only.
     *
     * This variable is used to simulate the condition about whether this component can be attached
     * to an entity or not. In tests, you can change this variable's value and verify that your code
     * responds correctly to [onAttach].
     */
    public var canBeAttached: Boolean = true

    /**
     * Lifecycle event, called when component is attached to an Entity.
     *
     * @param entity Entity the component is attached to.
     * @return True if the component can attach to the given entity.
     */
    override fun onAttach(entity: Entity): Boolean {
        return canBeAttached
    }

    /**
     * Lifecycle event, called when component is detached from an Entity.
     *
     * @param entity Entity the component detached from.
     */
    override fun onDetach(entity: Entity) {
        canBeAttached = true
    }
}
