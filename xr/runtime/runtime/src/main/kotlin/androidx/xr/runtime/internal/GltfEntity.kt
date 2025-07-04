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

/** Interface for a XR Runtime [GltfEntity]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfEntity : Entity {

    /** Returns the current animation state of the glTF entity. */
    @AnimationStateValue public val animationState: Int

    /**
     * Starts the animation with the given name.
     *
     * @param animationName The name of the animation to start. If null is supplied, will play the
     *   first animation found in the glTF.
     * @param loop Whether the animation should loop.
     */
    public fun startAnimation(loop: Boolean, animationName: String?)

    /** Stops the animation of the glTF entity. */
    public fun stopAnimation()

    /**
     * Sets a material override for a mesh in the glTF model.
     *
     * @param material The material to use for the mesh.
     * @param meshName The name of the mesh to use the material for.
     */
    public fun setMaterialOverride(material: MaterialResource, meshName: String)

    // TODO: b/417750821 - Add an OnAnimationFinished() Listener interface
    //                     Add a getAnimationTimeRemaining() interface

    /** Specifies the current animation state of the [GltfEntity]. */
    public annotation class AnimationStateValue

    /** Specifies the current animation state of the [GltfEntity]. */
    public object AnimationState {
        public const val PLAYING: Int = 0
        public const val STOPPED: Int = 1
    }
}
