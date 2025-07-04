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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/** Base interface for all components. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Component {
    /**
     * Lifecycle event, called when component is attached to an Entity.
     *
     * @param entity Entity the component is attached to.
     * @return True if the component can attach to the given entity.
     */
    public fun onAttach(entity: Entity): Boolean

    /**
     * Lifecycle event, called when component is detached from an Entity.
     *
     * @param entity Entity the component detached from.
     */
    public fun onDetach(entity: Entity)
}
