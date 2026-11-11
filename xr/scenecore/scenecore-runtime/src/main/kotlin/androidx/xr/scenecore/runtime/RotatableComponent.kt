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

package androidx.xr.scenecore.runtime

import androidx.annotation.RestrictTo

/**
 * Component that enables the user to rotate the attached geometry entity by grabbing and dragging
 * it.
 *
 * The rotation gesture is interpreted by the rendering runtime and the resulting rotation is
 * applied to the entity by the component. Supported on [GltfEntity] and [MeshEntity].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RotatableComponent : Component {
    /**
     * Restores the orientation the entity had when this component was attached.
     *
     * Orientation changes made after attaching, including ones made by the application, are
     * discarded. The entity's position is adjusted so that the center of the geometry stays where
     * it currently is. The pose is updated asynchronously, so [Entity.getPose] can still return the
     * previous pose right after this call. Has no effect if the component is not attached to an
     * entity.
     */
    public fun reset()
}
