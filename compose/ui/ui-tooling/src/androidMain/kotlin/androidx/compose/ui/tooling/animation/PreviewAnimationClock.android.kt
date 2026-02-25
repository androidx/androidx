/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.tooling.animation

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.tooling.ComposeAnimatedProperty
import androidx.compose.animation.tooling.ComposeAnimation
import androidx.compose.animation.tooling.TransitionInfo
import androidx.compose.ui.tooling.animation.clock.AnimatedVisibilityClock
import androidx.compose.ui.tooling.animation.clock.ComposeAnimationClock
import androidx.compose.ui.tooling.animation.clock.millisToNanos
import androidx.compose.ui.tooling.animation.search.SearchInfo
import androidx.compose.ui.tooling.animation.search.UnsupportedSearchInfo
import androidx.compose.ui.tooling.animation.states.AnimatedVisibilityState
import androidx.compose.ui.tooling.animation.states.TargetState
import kotlin.collections.set

/**
 * A class that keeps track of and controls animations in Compose Previews.
 *
 * This class is controlled by the Animation Preview in Android Studio. Most of its methods are
 * called via reflection, either directly from Android Studio or through `ComposeViewAdapter`.
 *
 * Methods intercepted in Android Studio:
 * - [notifySubscribe]
 * - [notifyUnsubscribe]
 *
 * Methods called from Android Studio:
 * - [updateFromAndToStates]
 * - [updateAnimatedVisibilityState]
 * - [getAnimatedVisibilityState]
 * - [getMaxDuration]
 * - [getMaxDurationPerIteration]
 * - [getAnimatedProperties]
 * - [getTransitions]
 * - [setClockTime]
 * - [setClockTimes]
 */
internal open class PreviewAnimationClock(
    private val requestLayout: () -> Unit,
    private val applySnapshot: () -> Unit,
) {

    private val TAG = "PreviewAnimationClock"

    private val DEBUG = false

    /** Map of subscribed [ComposeAnimation]s and corresponding [ComposeAnimationClock]s. */
    @VisibleForTesting
    internal val animationClocks = mutableMapOf<ComposeAnimation, ComposeAnimationClock<*, *>>()

    private val clockInfo =
        object : ClockInfo {
            override fun getMaxDurationPerIterationMillis(): Long {
                return this@PreviewAnimationClock.getMaxDurationPerIteration()
            }

            override fun requestLayout() {
                this@PreviewAnimationClock.requestLayout()
            }
        }

    /**
     * Track [ComposeAnimation] and it's [ComposeAnimationClock] created for target [SearchInfo]. If
     * [ComposeAnimation] is not supported or there is an issue with parsing (for example API is not
     * available), it still will be tracked as [UnsupportedComposeAnimation] which doesn't have any
     * associated [ComposeAnimationClock].
     */
    fun <AnimationType : ComposeAnimation> trackComposeAnimation(
        searchInfo: SearchInfo<AnimationType, *>
    ) {
        trackAnimation(searchInfo.animationObject) {
            searchInfo
                .takeIf { it !is UnsupportedSearchInfo }
                ?.createAnimation()
                ?.let {
                    animationClocks[it] =
                        searchInfo.createClock(it, clockInfo).apply {
                            // Reset time for newly created clock.
                            this.setClockTime(0L)
                        }
                    notifySubscribe(it)
                    return@trackAnimation
                }

            // If animation is not supported or for some reason animation couldn't be parsed, track
            // it as unsupported.
            UnsupportedComposeAnimation.create(searchInfo.label)?.let {
                trackedUnsupportedAnimations.add(it)
                notifySubscribe(it)
            }
        }
    }

    @VisibleForTesting val trackedUnsupportedAnimations = linkedSetOf<UnsupportedComposeAnimation>()

    /** Tracked animations. */
    private val trackedAnimations = linkedSetOf<Any>()
    private val lock = Any()

    private fun trackAnimation(animation: Any, createClockAndSubscribe: (Any) -> Unit): Boolean {
        synchronized(lock) {
            if (trackedAnimations.contains(animation)) {
                if (DEBUG) {
                    Log.d(TAG, "Animation $animation is already being tracked")
                }
                return false
            }
            trackedAnimations.add(animation)
        }

        createClockAndSubscribe(animation)

        if (DEBUG) {
            Log.d(TAG, "Animation $animation is now tracked")
        }

        return true
    }

    @VisibleForTesting
    protected open fun notifySubscribe(animation: ComposeAnimation) {
        // This method is expected to be no-op. It is intercepted in Android Studio using bytecode
        // manipulation, in order for the tools to be aware that the animation is now tracked.
    }

    @VisibleForTesting
    protected open fun notifyUnsubscribe(animation: ComposeAnimation) {
        // This method is expected to be no-op. It is intercepted in Android Studio using bytecode
        // manipulation, in order for the tools to be aware that the animation is no longer
        // tracked.
    }

    /**
     * Updates the [TargetState] corresponding to the given [ComposeAnimation].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun updateFromAndToStates(composeAnimation: ComposeAnimation, fromState: Any, toState: Any) {
        animationClocks[composeAnimation]?.setStateParameters(fromState, toState)
    }

    /**
     * Updates the given [AnimatedVisibilityClock]'s with the given state.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun updateAnimatedVisibilityState(composeAnimation: ComposeAnimation, state: Any) {
        animationClocks[composeAnimation]?.setStateParameters(state)
    }

    /**
     * Returns the [AnimatedVisibilityState] corresponding to the given [AnimatedVisibilityClock]
     * object. Falls back to [AnimatedVisibilityState.Enter].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getAnimatedVisibilityState(composeAnimation: ComposeAnimation): AnimatedVisibilityState {
        return animationClocks[composeAnimation]?.state as? AnimatedVisibilityState
            ?: AnimatedVisibilityState.Enter
    }

    /**
     * Returns the duration (ms) of the longest animation being tracked.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getMaxDuration(): Long {
        return animationClocks.values.maxOfOrNull { it.getMaxDuration() } ?: 0
    }

    /**
     * Returns the longest duration (ms) per iteration among the animations being tracked. This can
     * be different from [getMaxDuration], for instance, when there is one or more repeatable
     * animations with multiple iterations.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getMaxDurationPerIteration(): Long {
        return animationClocks.values.maxOfOrNull { it.getMaxDurationPerIteration() } ?: 0
    }

    /**
     * Returns a list of the given [ComposeAnimation]'s animated properties. The properties are
     * wrapped into a [ComposeAnimatedProperty] object containing the property label and the
     * corresponding value at the current time.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getAnimatedProperties(animation: ComposeAnimation): List<ComposeAnimatedProperty> {
        return animationClocks[animation]?.getAnimatedProperties() ?: emptyList()
    }

    /**
     * Returns a list of the given [ComposeAnimation]'s animated properties. The properties are
     * wrapped into a [TransitionInfo] object containing the property label, start and time of
     * animation and values of the animation.
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun getTransitions(animation: ComposeAnimation, stepMillis: Long): List<TransitionInfo> {
        return animationClocks[animation]?.getTransitions(stepMillis) ?: emptyList()
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMillis].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun setClockTime(animationTimeMillis: Long) {
        val timeNanos = millisToNanos(animationTimeMillis)
        animationClocks.values.forEach { it.setClockTime(timeNanos) }
        applySnapshot()
    }

    /**
     * Seeks each animation being tracked to the given [animationTimeMillis].
     *
     * Expected to be called via reflection from Android Studio.
     */
    fun setClockTimes(animationTimeMillis: Map<ComposeAnimation, Long>) {
        animationTimeMillis.forEach { (composeAnimation, millis) ->
            animationClocks[composeAnimation]?.setClockTime(millisToNanos(millis))
        }
        applySnapshot()
    }

    /** Unsubscribes the currently tracked animations and clears all the caches. */
    fun dispose() {
        animationClocks.forEach { notifyUnsubscribe(it.key) }
        trackedUnsupportedAnimations.forEach { notifyUnsubscribe(it) }
        trackedUnsupportedAnimations.clear()
        animationClocks.clear()
        trackedAnimations.clear()
    }
}
