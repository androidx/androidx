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
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlin.collections.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/**
 * [GltfAnimation] represents an animation in a [GltfModelEntity].
 *
 * Multiple animations can play simultaneously. If multiple animations affect the same node, the
 * animation with the **highest index** in the glTF file takes precedence. Specifically, only the
 * state of the highest-index animation is reflected in the scene for a given frame. Updates from
 * other lower-index animations (such as node transforms) for the same target will be overwritten.
 *
 * A [GltfAnimation] cannot be used once its associated [GltfModelEntity] has been destroyed.
 *
 * @property index The index of this animation in the source glTF model.
 * @property name The name of this animation, or `null` if the animation is unnamed.
 */
@Suppress("NotCloseable")
@ExperimentalGltfAnimationApi
public class GltfAnimation
internal constructor(
    private val rtGltfEntity: RtGltfEntity,
    private val rtGltfAnimation: RtGltfAnimation,
    public val index: Int,
    public val name: String?,
    private val durationSeconds: Float,
) {
    /**
     * Whether this animation should loop when playing.
     *
     * The default value is `false` (playback does not loop). When looping is disabled (`false`) and
     * playback reaches the end of the animation, the animation state transitions to
     * [AnimationState.STOPPED], while remaining clamped at the final frame pose.
     *
     * Changes to the loop configuration only take effect during [start].
     */
    @get:MainThread
    @set:MainThread
    @get:Suppress("GetterSetterNames")
    public var loop: Boolean = false

    /**
     * The playback speed multiplier for this animation.
     *
     * This can be changed while the animation is playing or paused.
     *
     * The default playback speed is `1.0f`. The speed multiplier determines the playback rate:
     * * **1.0:** Normal speed.
     * * **> 1.0:** Faster playback.
     * * **> 0.0 and < 1.0:** Slower playback (e.g., 0.5 is half speed).
     * * **0.0:** Freezes the animation at the current frame while keeping it active (unlike
     *   pausing).
     * * **< 0.0:** Plays the animation in reverse.
     *
     * **Note on Reverse Playback:** When playing in reverse without looping ([loop] set to
     * `false`), starting the animation from the beginning causes it to reach the end of playback
     * immediately. Enable looping via [loop] (setting `loop` to `true`) to play the animation in
     * reverse continuously.
     */
    @get:MainThread
    @set:MainThread
    public var speed: Float = 1.0f
        set(value) {
            field = value
            rtGltfAnimation.setAnimationSpeed(value)
        }

    /** The duration of this animation. */
    @get:RequiresApi(Build.VERSION_CODES.O)
    public val duration: Duration
        get() =
            java.time.Duration.ofMillis((durationSeconds * TimeUnit.SECONDS.toMillis(1)).toLong())

    private val mAnimationStateListeners: MutableMap<Consumer<AnimationState>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    /** Specifies the current animation state of this [GltfAnimation]. */
    public class AnimationState private constructor(private val value: Int) {
        public companion object {
            /** The animation is currently playing. */
            @JvmField public val PLAYING: AnimationState = AnimationState(1)
            /**
             * The animation is currently stopped. When in this state, the animation playback time
             * is reset to the beginning. The animation must be [start]ed to play again.
             */
            @JvmField public val STOPPED: AnimationState = AnimationState(2)
            /**
             * The animation is currently paused. When in this state, the animation is frozen at the
             * current frame. The animation can be [resume]d to continue playback from the paused
             * point.
             */
            @JvmField public val PAUSED: AnimationState = AnimationState(3)
        }

        override fun toString(): String =
            when (this) {
                PLAYING -> "PLAYING"
                STOPPED -> "STOPPED"
                PAUSED -> "PAUSED"
                else -> "UNKNOWN ($value)"
            }
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
     * This transitions the animation state to [AnimationState.PLAYING]. By default, the animation
     * plays once at normal speed (`1.0f`) without looping unless configured otherwise via [loop]
     * and [speed]. Calling [start] while the animation is currently playing or paused will restart
     * playback from the beginning.
     */
    @MainThread
    public fun start() {
        rtGltfAnimation.startAnimation(loop, speed, /* startTimeSeconds= */ 0.0f)
    }

    /**
     * Starts playing this animation with the specified [options].
     *
     * @param options Configuration options for starting the animation.
     */
    @Deprecated(
        "Use GltfAnimation.loop and GltfAnimation.speed properties with parameterless start() instead.",
        ReplaceWith("apply { loop = options.shouldLoop; speed = options.speed }.start()"),
    )
    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("DEPRECATION")
    @MainThread
    public fun start(options: GltfAnimationStartOptions) {
        this.loop = options.shouldLoop
        this.speed = options.speed
        start()
    }

    /**
     * Seeks this animation to the specified [time] offset.
     *
     * @param time The offset from the beginning of the animation.
     * @throws IllegalArgumentException if [time] is negative.
     */
    @Deprecated("Seeking animation is no longer supported.")
    @RequiresApi(Build.VERSION_CODES.O)
    @MainThread
    public fun seekTo(time: Duration) {
        require(!time.isNegative) { "time must be non-negative." }

        rtGltfAnimation.seekAnimation(time.toMillis() / 1000.0f)
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
     * to the start of the animation, and transitions the state to [AnimationState.PLAYING].
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
