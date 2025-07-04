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

import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter

/**
 * [Component] that modifies the pointer icon that is rendered on the component's [Entity]. If this
 * Component is not present on an Entity the default, system-determined icon is used. Removing this
 * Component will set the Entity's pointer back to the default icon.
 */
public class SpatialPointerComponent
private constructor(private val platformAdapter: JxrPlatformAdapter) : Component {

    private val rtComponent by lazy { platformAdapter.createSpatialPointerComponent() }

    private var entity: Entity? = null

    /**
     * The [SpatialPointerIcon] that will be rendered on the component's [Entity] when the pointer
     * is located on the entity. A [SpatialPointerIcon.DEFAULT] value indicates the default pointer
     * icon should be used.
     */
    public var spatialPointerIcon: SpatialPointerIcon
        get() = rtComponent.getSpatialPointerIcon().toSpatialPointerIcon()
        set(value) = rtComponent.setSpatialPointerIcon(value.toRtSpatialPointerIcon())

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }
        if ((entity as BaseEntity<*>).rtEntity.addComponent(rtComponent)) {
            this.entity = entity
            spatialPointerIcon = SpatialPointerIcon.DEFAULT
            return true
        } else {
            return false
        }
    }

    override fun onDetach(entity: Entity) {
        spatialPointerIcon = SpatialPointerIcon.DEFAULT
        (entity as BaseEntity<*>).rtEntity.removeComponent(rtComponent)
        this.entity = null
    }

    public companion object {

        /**
         * Creates a new [SpatialPointerComponent].
         *
         * @param session The [Session] to use for creating the component.
         * @return A new [SpatialPointerComponent].
         */
        @JvmStatic
        public fun create(session: Session): SpatialPointerComponent {
            return SpatialPointerComponent(session.platformAdapter)
        }
    }
}
