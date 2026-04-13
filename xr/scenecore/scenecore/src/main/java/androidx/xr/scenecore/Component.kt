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

/** A Component adds functionality or behaviors to an [Entity]. */
public abstract class Component internal constructor() {
    /**
     * Handles the component attachment logic. For internal framework use only.
     *
     * This method serves as the exclusive entry point for the [Entity] system to initiate the
     * attachment process. Its default implementation calls the protected [onAttach] method.
     *
     * @param entity The [Entity] to which this component is being attached.
     * @return The result of the underlying [onAttach] call.
     */
    internal fun handleAttachInternal(entity: Entity): Boolean {
        return onAttach(entity)
    }

    /**
     * Handles the component detachment logic; for internal framework use only.
     *
     * This method serves as the exclusive entry point for the [Entity] system to initiate the
     * detachment process. Its default implementation calls the protected [onDetach] method.
     *
     * @param entity The [Entity] from which this component is being detached.
     */
    internal fun handleDetachInternal(entity: Entity) {
        onDetach(entity)
    }

    /**
     * Called by the framework when this component is being added to an [Entity].
     *
     * This method is triggered when [Entity.addComponent] is invoked. Implementations should
     * override this method to perform setup logic or to validate if the component is compatible
     * with the provided [entity].
     *
     * @param entity The [Entity] to which this component is being attached.
     * @return `true` if the component was successfully attached; `false` if the [entity] does not
     *   support this component or if attachment failed.
     */
    protected abstract fun onAttach(entity: Entity): Boolean

    /**
     * Called by the framework when this component is being removed from an [Entity].
     *
     * This method is triggered when [Entity.removeComponent] is invoked. Implementations should
     * override this method to release resources or undo any changes made during [onAttach].
     *
     * @param entity The [Entity] from which this component is being detached.
     */
    protected abstract fun onDetach(entity: Entity)
}
