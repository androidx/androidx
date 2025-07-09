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

package androidx.xr.scenecore

/**
 * Defines the clipping configuration for all panels within the [Scene].
 *
 * This setting cannot be applied to an individual panel within the Scene, it must apply to all
 * panels.
 *
 * @property isDepthTestEnabled When `true`, enables depth testing for all panels in the Scene,
 *   allowing them to be drawn in an intuitive, distance-based way with respect to other objects in
 *   the scene, including other panels and the environment. When `false`, all panels are rendered on
 *   top of any other non-depth-tested 3D content that were drawn **before** them, regardless of
 *   their actual distance (i.e., depth) from the camera. The `false` setting can be used to ensure
 *   panels are drawn on top of the virtual environment, i.e., they do not clip into the
 *   environment.
 */
public class PanelClippingConfig
@JvmOverloads
constructor(public val isDepthTestEnabled: Boolean = true) {

    /**
     * Returns a copy of this configuration with the specified values updated.
     *
     * @param isDepthTestEnabled The new depth test enabled state.
     * @return A new [PanelClippingConfig] instance with the updated values.
     */
    @JvmOverloads
    public fun copy(isDepthTestEnabled: Boolean = this.isDepthTestEnabled): PanelClippingConfig {
        return PanelClippingConfig(isDepthTestEnabled = isDepthTestEnabled)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PanelClippingConfig) return false
        if (isDepthTestEnabled != other.isDepthTestEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        return isDepthTestEnabled.hashCode()
    }

    override fun toString(): String {
        return "PanelClippingConfig(isDepthTestEnabled=$isDepthTestEnabled)"
    }
}
