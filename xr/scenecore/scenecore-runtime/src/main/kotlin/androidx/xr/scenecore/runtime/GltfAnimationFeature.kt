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

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Provide the rendering implementation for [GltfAnimationFeature] */
// TODO(b/481429599): Audit usage of LIBRARY_GROUP_PREFIX in SceneCore and migrate it over to
// LIBRARY_GROUP.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface GltfAnimationFeature {

    /** Returns the current animation state of the glTF model. */
    public val animationState: Int

    /** The index of the animation in the glTF model file. */
    public val animationIndex: Int

    /** The optional name of the animation. */
    public val animationName: String?

    /** The duration of the animation in seconds. */
    public val animationDuration: Float

    /**
     * Starts the animation with specific settings for the animation.
     *
     * @param loop Whether the animation should loop.
     * @param speed The animation playback rate multiplier where 1.0 is the normal speed and
     *   negative values will play the animation in reverse.
     * @param seekStartTimeSeconds The animation playback start time in seconds.
     */
    @MainThread
    public fun startAnimation(loop: Boolean, speed: Float?, seekStartTimeSeconds: Float?)

    /** Stops the animation of the glTF model. */
    @MainThread public fun stopAnimation()

    /* Pause the animation of the glTF model. */
    @MainThread public fun pauseAnimation()

    /* Resume the animation of the glTF model. */
    @MainThread public fun resumeAnimation()

    /**
     * Sets the playback time of the animation.
     *
     * @param startTime The animation playback start time in seconds.
     */
    @MainThread public fun seekAnimation(startTime: Float)

    /**
     * Sets the playback speed of the animation.
     *
     * @param speed The animation playback rate multiplier where 1.0 is the normal speed and
     *   negative values will play the animation in reverse.
     */
    @MainThread public fun setAnimationSpeed(speed: Float)

    /**
     * Adds a listener that will be called whenever the animation state of the glTF is updated.
     *
     * @param executor The executor to run the listener on.
     * @param listener The listener that will be called when the animation state changes.
     */
    @MainThread public fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>)

    /** Removes an animation state updated listener. */
    @MainThread public fun removeAnimationStateListener(listener: Consumer<Int>)
}
