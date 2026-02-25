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

package androidx.xr.scenecore

import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.xr.scenecore.runtime.GltfAnimationFeature as RtGltfAnimation
import androidx.xr.scenecore.runtime.GltfEntity as RtGltfEntity
import java.time.Duration
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.collections.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * [GltfAnimation] represents an animation in a [GltfModelEntity].
 *
 * Multiple animations can play simultaneously. If multiple animations affect the same node, the
 * animation **processed last** takes precedence. Specifically, only the state of the last processed
 * animation is reflected in the scene for a given frame. Updates from other animations (such as
 * node transforms) for the same target will be overwritten.
 *
 * A [GltfAnimation] cannot be used once its associated [GltfModelEntity] has been destroyed.
 *
 * @property index The index of this animation in the source glTF model.
 * @property name The name of this animation, or `null` if the animation is unnamed.
 * @property duration The duration of this animation.
 */
@Suppress("NotCloseable")
@RequiresApi(Build.VERSION_CODES.O)
public class GltfAnimation
internal constructor(
    private val rtGltfEntity: RtGltfEntity,
    private val rtGltfAnimation: RtGltfAnimation,
    public val index: Int,
    public val name: String?,
    public val duration: Duration,
) {
    private val mAnimationStateListeners: MutableMap<Consumer<AnimationState>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    /** Specifies the current animation state of this [GltfAnimation]. */
    public class AnimationState private constructor(private val name: String) {
        public companion object {
            /** The animation is currently playing. */
            @JvmField public val PLAYING: AnimationState = AnimationState("PLAYING")
            /**
             * The animation is currently stopped. When in this state, the animation playback time
             * is reset to the beginning. The animation must be [started] to play again.
             */
            @JvmField public val STOPPED: AnimationState = AnimationState("STOPPED")
            /**
             * The animation is currently paused. When in this state, the animation is frozen at the
             * current frame. The animation can be [resumed] to continue playback from the paused
             * point.
             */
            @JvmField public val PAUSED: AnimationState = AnimationState("PAUSED")
        }

        public override fun toString(): String = name
    }

    /**
     * The current state of this animation.
     *
     * @see AnimationState
     */
    @get:MainThread
    public val animationState: AnimationState
        get() = mapInternalState(rtGltfAnimation.animationState)

    /**
     * Starts playing this animation.
     *
     * This transitions the animation state to [AnimationState.PLAYING].
     *
     * @param options The options that describe how the glTF model will be animated. Using default
     *   values if not specified.
     */
    @JvmOverloads
    @MainThread
    public fun start(options: GltfAnimationStartOptions = GltfAnimationStartOptions()) {
        rtGltfAnimation.startAnimation(
            options.shouldLoop,
            options.speed,
            options.seekStartTime.toMillis() / 1000.0f,
        )
    }

    /**
     * Stops this animation.
     *
     * This resets the playback time to 0 and transitions the animation state to
     * [AnimationState.STOPPED]. If this animation is not currently playing or pausing, this method
     * has no effect.
     */
    @MainThread
    public fun stop() {
        rtGltfAnimation.stopAnimation()
    }

    /**
     * Pauses this animation.
     *
     * This freezes the animation at the current frame and transitions the animation state to
     * [AnimationState.PAUSED]. Use [resume] to continue playback.
     *
     * Note: Calling [start] while in the [AnimationState.PAUSED] state will reset the playback time
     * to [animationOptions.seekStartTime] and transition the state to [AnimationState.PLAYING].
     */
    @MainThread
    public fun pause() {
        rtGltfAnimation.pauseAnimation()
    }

    /**
     * Resumes this animation.
     *
     * This continues the animation from the point where it was paused and transitions the animation
     * state to [AnimationState.PLAYING].
     *
     * Note: Calling [resume] while in the [AnimationState.PLAYING] and [AnimationState.STOPPED]
     * state will have no effect.
     */
    @MainThread
    public fun resume() {
        rtGltfAnimation.resumeAnimation()
    }

    /**
     * Seeks the animation to a specific time position.
     *
     * Note: This call is only valid during the [AnimationState.PLAYING] and [AnimationState.PAUSED]
     * states. Calling this method while in the [AnimationState.STOPPED] state has no effect.
     *
     * The behavior depends on whether the animation is looping:
     * * **Looping Enabled:** The time is treated as cyclical. Values exceeding the duration will
     *   wrap around (modulo arithmetic).
     * * **Looping Disabled:** The time is clamped to the valid playback range. Values less than
     *   [GltfAnimationStartOptions.seekStartTime] clamp to the start. Values exceeding the duration
     *   (or end time) clamp to the end.
     *
     * Interaction with [GltfAnimationStartOptions.seekStartTime]:
     * * **Positive Speed:** If the seek time is less than
     *   [GltfAnimationStartOptions.seekStartTime], the animation clamps to
     *   [GltfAnimationStartOptions.seekStartTime] (the start of the valid window).
     * * **Negative Speed:** If the seek time is less than
     *   [GltfAnimationStartOptions.seekStartTime], the animation clamps to
     *   [GltfAnimationStartOptions.seekStartTime]. Since the animation plays in reverse, it
     *   effectively stops there if looping is disabled.
     *
     * @param time The offset from the beginning of the animation.
     * @throws IllegalArgumentException if [time] is negative.
     */
    @MainThread
    public fun seekTo(time: Duration) {
        require(!time.isNegative) { "time must be non-negative." }

        rtGltfAnimation.seekAnimation(time.toMillis() / 1000.0f)
    }

    /**
     * Sets the playback speed for this animation.
     *
     * The speed multiplier determines the playback rate:
     * * **1.0:** Normal speed.
     * * **> 1.0:** Faster playback.
     * * **> 0.0 and < 1.0:** Slower playback (e.g., 0.5 is half speed).
     * * **0.0:** Freezes the animation at the current frame while keeping it active (unlike
     *   pausing).
     * * **< 0.0:** Plays the animation in reverse.
     *
     * Note: This call is only valid during the [AnimationState.PLAYING] and [AnimationState.PAUSED]
     * states. Calling this method while in the [AnimationState.STOPPED] state will have no effect.
     *
     * @param speed The playback rate multiplier.
     */
    @MainThread
    public fun setSpeed(speed: Float) {
        rtGltfAnimation.setAnimationSpeed(speed)
    }

    /**
     * Registers a listener to be invoked when the animation state of this [GltfAnimation] changes.
     *
     * @param executor The executor on which the listener will be invoked.
     * @param listener The listener to invoke when the state changes. It receives the new
     *   [AnimationState].
     */
    public fun addAnimationStateListener(executor: Executor, listener: Consumer<AnimationState>) {
        if (mAnimationStateListeners.isEmpty()) {
            rtGltfAnimation.addAnimationStateListener(
                executor = Dispatchers.Main.asExecutor(),
                listener = this::onAnimationStateUpdated,
            )
        }
        mAnimationStateListeners[listener] = executor
    }

    /**
     * Registers a listener to be invoked on the main thread when the animation state of the
     * [GltfAnimation] changes.
     *
     * @param listener The listener to invoke when the state changes. It receives the new
     *   [AnimationState].
     */
    public fun addAnimationStateListener(listener: Consumer<AnimationState>) {
        addAnimationStateListener(executor = Dispatchers.Main.asExecutor(), listener = listener)
    }

    /**
     * Unregisters a previously registered animation state update listener.
     *
     * @param listener The listener to remove.
     */
    public fun removeAnimationStateListener(listener: Consumer<AnimationState>) {
        mAnimationStateListeners.remove(listener)
        if (mAnimationStateListeners.isEmpty()) {
            rtGltfAnimation.removeAnimationStateListener(this::onAnimationStateUpdated)
        }
    }

    private fun onAnimationStateUpdated(@RtGltfEntity.AnimationStateValue animationState: Int) {
        val result = mapInternalState(animationState)
        for ((listener, executor) in mAnimationStateListeners.entries) {
            executor.execute { listener.accept(result) }
        }
    }

    private fun mapInternalState(rtState: Int): AnimationState {
        return when (rtState) {
            RtGltfEntity.AnimationState.PLAYING -> AnimationState.PLAYING
            RtGltfEntity.AnimationState.STOPPED -> AnimationState.STOPPED
            RtGltfEntity.AnimationState.PAUSED -> AnimationState.PAUSED
            else -> AnimationState.STOPPED
        }
    }
}
