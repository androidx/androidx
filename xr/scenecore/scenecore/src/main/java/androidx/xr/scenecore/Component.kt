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

package androidx.xr.scenecore

import androidx.annotation.RestrictTo

/** A Component adds functionality or behaviors to an [Entity]. */
public interface Component {

    /**
     * Called by an [Entity] when it attempts to add this Component to itself.
     *
     * This method is restricted because it is only called from [Entity.addComponent].
     *
     * @param entity Entity to which this Component was attached.
     * @return True if the Component was attached to the given Entity. False if the Entity did not
     *   support having this Component attached.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public fun onAttach(entity: Entity): Boolean

    /**
     * Called by an [Entity] when it attempts to detach this Component from itself.
     *
     * This method is restricted because it is only called from [Entity.removeComponent].
     *
     * @param entity Entity from which this Component was detached.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public fun onDetach(entity: Entity)
}
