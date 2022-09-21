/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Class used to read and manipulate the state of a MotionLayout Composable.
 */
@Immutable
@ExperimentalMotionApi
interface MotionLayoutState {
    // TODO: Add API to listen to finished Transition animation
    // TODO: Add API to know if MotionLayout is on an ongoing animation

    /**
     * Observable value for the animation progress of the current MotionLayout Transition.
     *
     * Where 0.0f is the start of the Transition.
     *
     * And 1.0f is the end of the Transition.
     *
     * Beware that reading a 0 or 1 does not imply that a Transition animation has ended.
     */
    val currentProgress: Float

    /**
     * Observable value to indicate if MotionLayout is in a debugging mode.
     *
     * False by default.
     */
    val isInDebugMode: Boolean

    /**
     * Change the debugging mode.
     *
     * Note that this causes an internal recomposition of the MotionLayout modifiers, cancelling
     * events like swipe handling. Also, debugging may add overhead to measuring and/or drawing.
     *
     * Set [MotionLayoutDebugFlags.NONE] to deactivate any ongoing debugging.
     *
     * @see MotionLayoutDebugFlags
     */
    fun setDebugMode(motionDebugFlag: MotionLayoutDebugFlags)

    /**
     * Set the animation progress to the given [newProgress] without animating. The value change
     * will be instant.
     *
     * Calls to this method will cancel any ongoing animation.
     */
    fun snapTo(@FloatRange(from = 0.0, to = 1.0) newProgress: Float)

    /**
     * Animate the progress to the given [newProgress] using [animationSpec].
     *
     * Repeated calls to this method will cancel previous ongoing animations.
     */
    fun animateTo(
        @FloatRange(from = 0.0, to = 1.0) newProgress: Float,
        animationSpec: AnimationSpec<Float>
    )
}

/**
 * Implementation of [MotionLayoutState] with additional properties used by MotionLayout internals.
 */
@Immutable
@ExperimentalMotionApi
@PublishedApi
internal class MotionLayoutStateImpl(
    initialProgress: Float,
    initialDebugMode: MotionLayoutDebugFlags,
    private val motionCoroutineScope: CoroutineScope
) : MotionLayoutState {
    /**
     * The underlying object that holds the progress value for a [MotionLayout] Composable.
     *
     * Manipulated using the [Animatable] API, exposed internally with
     * [motionProgress]; and externally with [currentProgress], [animateTo] and [snapTo].
     */
    private val animatableProgress = Animatable(initialProgress)

    /**
     * Channel to allow scheduling Animation Commands into [motionCoroutineScope].
     */
    private val channel = Channel<MotionAnimationCommand>(capacity = Channel.UNLIMITED).also {
        motionCoroutineScope.launch {
            while (coroutineContext.isActive) {
                // Wait for the next Command
                val stateCommand = it.receive()

                // Handle the command with `launch` to avoid blocking this scope, when a new Command
                // is received and launched, Animatable will cancel any running animations from
                // previous Commands
                launch {
                    when (stateCommand) {
                        is MotionAnimationCommand.Animate -> {
                            animatableProgress.animateTo(
                                targetValue = stateCommand.newProgress,
                                animationSpec = stateCommand.animationSpec
                            )
                        }
                        is MotionAnimationCommand.Snap -> {
                            animatableProgress.snapTo(targetValue = stateCommand.newProgress)
                        }
                    }
                }
            }
        }
    }

    /**
     * [MutableState] for the debug mode.
     */
    private val debugModeState: MutableState<MotionLayoutDebugFlags> =
        mutableStateOf(initialDebugMode)

    /**
     * Internal observable debug mode.
     *
     * @see MotionLayoutDebugFlags
     */
    @PublishedApi
    internal val debugMode: MotionLayoutDebugFlags
        get() = debugModeState.value

    /**
     * Object used by MotionLayout internals to read and update the progress.
     */
    @PublishedApi
    internal val motionProgress: MotionProgress =
        MotionProgress.fromState(animatableProgress.asState(), ::snapTo)

    override val currentProgress: Float
        get() = animatableProgress.value

    override val isInDebugMode: Boolean
        get() = debugModeState.value == MotionLayoutDebugFlags.SHOW_ALL

    override fun setDebugMode(motionDebugFlag: MotionLayoutDebugFlags) {
        debugModeState.value = motionDebugFlag
    }

    override fun snapTo(newProgress: Float) {
        channel.trySend(MotionAnimationCommand.Snap(newProgress))
    }

    override fun animateTo(newProgress: Float, animationSpec: AnimationSpec<Float>) {
        channel.trySend(MotionAnimationCommand.Animate(newProgress, animationSpec))
    }
}

/**
 * Returns a [MotionLayoutState], when passed to a [MotionLayout] Composable it can be used to
 * observe and animate its internal progress value.
 *
 * - To animate on click:
 * ```
 * @Composable
 * fun MyComposable() {
 *   val motionState = rememberMotionLayoutState()
 *   Column {
 *     MotionLayout(motionLayoutState = motionState, motionScene = MotionScene(<your-json>)) {
 *       <your-composables>
 *     }
 *     Button(
 *       // Animate the associated MotionLayout to end (progress = 1f)
 *       onClick = { motionState.animateTo(1f, spring()) }
 *     ) {
 *       Text(text = "Send")
 *     }
 *   }
 * }
 * ```
 * - Use the current progress value:
 * ```
 * @Composable
 * fun MyComposable() {
 *   val motionState = rememberMotionLayoutState()
 *   Column {
 *     MotionLayout(motionLayoutState = motionState, motionScene = MotionScene(<your-json>)) {
 *       <your-composables>
 *     }
 *     // Text will recompose during MotionLayout animation with the current progress value
 *     Text(text = "Value: ${motionState.currentProgress}")
 *   }
 * }
 * ```
 *
 * Returns the same instance if [key] is equal to the previous composition, otherwise produces and
 * remembers a new instance (with the given initial values).
 */
@ExperimentalMotionApi
@Composable
fun rememberMotionLayoutState(
    key: Any = Unit,
    initialProgress: Float = 0f,
    initialDebugMode: MotionLayoutDebugFlags = MotionLayoutDebugFlags.NONE
): MotionLayoutState {
    val coroutineScope = rememberCoroutineScope()
    return remember(key) {
        MotionLayoutStateImpl(
            initialProgress = initialProgress,
            initialDebugMode = initialDebugMode,
            motionCoroutineScope = coroutineScope
        )
    }
}

/**
 * Convenience interface used for [MotionLayoutStateImpl.channel], to handle calls to [Animatable].
 */
@PublishedApi
internal interface MotionAnimationCommand {

    /**
     * Required parameters used for [Animatable.animateTo].
     */
    class Animate(
        val newProgress: Float,
        val animationSpec: AnimationSpec<Float>
    ) : MotionAnimationCommand

    /**
     * Required parameters used for [Animatable.snapTo].
     */
    class Snap(val newProgress: Float) : MotionAnimationCommand
}

/**
 * Internal representation to read and set values for the progress.
 */
@PublishedApi
internal interface MotionProgress {
    val currentProgress: Float

    fun updateProgress(newProgress: Float)

    companion object {
        fun fromMutableState(mutableProgress: MutableState<Float>): MotionProgress =
            fromState(mutableProgress) { mutableProgress.value = it }

        fun fromState(
            progressState: State<Float>,
            onUpdate: (newProgress: Float) -> Unit
        ): MotionProgress =
            object : MotionProgress {
                override val currentProgress: Float
                    get() = progressState.value

                override fun updateProgress(newProgress: Float) {
                    onUpdate(newProgress)
                }
            }
    }
}