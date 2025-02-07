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

/**
 * Base interface for all components.
 *
 * Components are attached to entities, to add functionality to those entities.
 */
public interface Component {
    /**
     * Called when this component is attached to the entity.
     *
     * @param entity Entity this component is being attached to.
     * @return True if the component can attach to given Entity.
     */
    public fun onAttach(entity: Entity): Boolean

    /**
     * Called when this component is detached from the entity.
     *
     * @param entity Entity this component is being detached from.
     */
    public fun onDetach(entity: Entity)
}
