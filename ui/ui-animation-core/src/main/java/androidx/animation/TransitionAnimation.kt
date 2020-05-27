/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

import android.util.Log
import androidx.animation.InterruptionHandling.UNINTERRUPTIBLE
import androidx.annotation.RestrictTo

/**
 * [TransitionAnimation] is the underlying animation used in [androidx.ui.animation.Transition] for
 * animating from one set of property values (i.e. one [TransitionState]) to another. In compose,
 * it is recommended to create such an animation using [androidx.ui.animation.Transition],
 * instead of instantiating [TransitionAnimation] directly.
 *
 * [TransitionAnimation] reads the property values out of the start and end state,  as well as the
 * animations defined for each state pair for each property, and run these animations until all
 * properties have reached their pre-defined values in the new state. When no animation is specified
 * for a property, a default [SpringAnimation] animation will be used.
 *
 * [TransitionAnimation] may be interrupted while the animation is on-going by a request to go
 * to another state. [TransitionAnimation] ensures that all the animating properties preserve their
 * current value and velocity as they createAnimation to the new state.
 *
 * Once a [TransitionDefinition] is instantiated, a [TransitionAnimation] can be created via
 * [TransitionDefinition.createAnimation].
 *
 * @see [androidx.ui.animation.Transition]
 */
class TransitionAnimation<T>(
    internal val def: TransitionDefinition<T>,
    private val clock: AnimationClockObservable,
    initState: T? = null
) : TransitionState {

    var onUpdate: (() -> Unit)? = null
    var onStateChangeFinished: ((T) -> Unit)? = null
    var isRunning = false
        private set

    private val UNSET = -1L
    private var fromState: StateImpl<T>
    private var toState: StateImpl<T>
    private val currentState: AnimationState<T>
    private var startTime: Long = UNSET
    private var lastFrameTime: Long = UNSET
    private var pendingState: StateImpl<T>? = null

    // These animation wrappers contains the start/end value and start velocities for each animation
    // run, to make it convenient to query current values/velocities based on play time. They will
    // be thrown away after each animation run, as we expect start/end value and start
    // velocities to be dynamic. The stateless animation that the wrapper wraps around will be
    // re-used as they are stateless.
    private var currentAnimWrappers: MutableMap<
            PropKey<Any, AnimationVector>,
            AnimationWrapper<Any, AnimationVector>
            > = mutableMapOf()
    private var startVelocityMap: MutableMap<PropKey<Any, AnimationVector>, Any> = mutableMapOf()

    // Named class for animation clock observer to help with tools' reflection.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    inner class TransitionAnimationClockObserver : AnimationClockObserver {
        // This API is intended for tools' use only. Hence the @RestrictTo.
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        val animation: TransitionAnimation<T> = this@TransitionAnimation
        override fun onAnimationFrame(frameTimeMillis: Long) {
            doAnimationFrame(frameTimeMillis)
        }
    }
    private val animationClockObserver: AnimationClockObserver = TransitionAnimationClockObserver()

    // TODO("Create a more efficient code path for default only transition def")

    init {
        // If an initial state is specified in the ctor, use that instead of the default state.
        val defaultState: StateImpl<T>
        if (initState == null) {
            defaultState = def.defaultState
        } else {
            defaultState = def.states[initState]!!
        }
        currentState = AnimationState(defaultState, defaultState.name)
        // Need to come up with a better plan to avoid the foot gun of accidentally modifying state
        fromState = defaultState
        toState = defaultState
    }

    // Interpolate current state and the new state
    private fun setState(newState: StateImpl<T>) {
        if (isRunning) {
            val currentSpec = def.getSpec(fromState.name, toState.name)
            if (currentSpec.interruptionHandling == UNINTERRUPTIBLE) {
                pendingState = newState
                return
            }
        }

        val transitionSpec = def.getSpec(toState.name, newState.name)
        val playTime = getPlayTime()
        // TODO: handle the states that have only partial properties defined
        // For now assume all the properties are defined in all states

        // TODO: Support different interruption types
        // For now assume continuing with the same value,  and for floats the same velocity
        for ((prop, _) in newState.props) {
            val currentVelocity = currentAnimWrappers[prop]?.getVelocity(playTime)
            currentAnimWrappers[prop] = prop.createAnimationWrapper(
                transitionSpec.getAnimationForProp(prop), currentState[prop], currentVelocity,
                newState[prop]
            )

            // TODO: Will need to track a few timelines if we support partially defined list of
            // props in each state.
        }

        fromState = AnimationState(currentState, toState.name)
        toState = newState
        if (DEBUG) {
            Log.w("TransAnim", "Animating to new state: ${toState.name}")
        }

        // Start animation should be called after all the setup has been done
        startAnimation()
    }

    private fun getPlayTime(): Long {
        if (startTime == UNSET) {
            return 0L
        }
        return lastFrameTime - startTime
    }

    /**
     * Starts the animation to go to a new state with the given state name.
     *
     * @param name Name of the [TransitionState] that is defined in the [TransitionDefinition].
     */
    fun toState(name: T) {
        val nextState = def.states[name]
        if (nextState == null) {
            // Throw exception or ignore?
        } else if (pendingState != null && toState.name == name) {
            // just canceling the pending state
            pendingState = null
        } else if ((pendingState ?: toState).name == name) {
            // already targeting this state
        } else {
            setState(nextState)
        }
    }

    /**
     * Gets the value of a property with a given property key.
     *
     * @param propKey Property key (defined in [TransitionDefinition]) for a specific property
     */
    override operator fun <T, V : AnimationVector> get(propKey: PropKey<T, V>): T {
        return currentState[propKey]
    }

    // Start animation if not running, otherwise reset start time
    private fun startAnimation() {
        if (!isRunning) {
            isRunning = true
            clock.subscribe(animationClockObserver)
        } else {
            startTime = lastFrameTime
        }
    }

    private fun doAnimationFrame(frameTimeMillis: Long) {
        // Remove finished animations
        lastFrameTime = frameTimeMillis
        if (startTime == UNSET) {
            startTime = frameTimeMillis
        }

        val playTime = getPlayTime()
        for ((prop, animation) in currentAnimWrappers) {
            currentState[prop] = animation.getValue(playTime)
        }

        // Prune the finished animations
        currentAnimWrappers.entries.removeAll {
            it.value.isFinished(playTime)
        }

        onUpdate?.invoke()

        // call end animation when all animations end
        if (currentAnimWrappers.isEmpty()) {
            // All animations have finished. Snap all values to end value
            for (prop in toState.props.keys) {
                currentState[prop] = toState[prop]
            }
            startVelocityMap.clear()

            endAnimation()
            val currentStateName = toState.name
            val spec = def.getSpec(fromState.name, toState.name)
            val nextState = def.states[spec.nextState]
            fromState = toState

            // Uninterruptible transition to the next state takes a priority over the pending state.
            if (nextState != null && spec.interruptionHandling == UNINTERRUPTIBLE) {
                setState(nextState)
            } else if (pendingState != null) {
                setState(pendingState!!)
                pendingState = null
            } else if (nextState != null) {
                setState(nextState)
            }
            onStateChangeFinished?.invoke(currentStateName)
        }
    }

    private fun endAnimation() {
        clock.unsubscribe(animationClockObserver)
        startTime = UNSET
        lastFrameTime = UNSET
        isRunning = false
    }
}

internal fun <T, V : AnimationVector> PropKey<T, V>.createAnimationWrapper(
    anim: Animation<V>,
    start: T,
    startVelocity: V?,
    end: T
): AnimationWrapper<T, V> {
    val velocity: V = startVelocity ?: typeConverter.convertToVector(start).newInstance()
    return TargetBasedAnimationWrapper(start, velocity, end, anim, typeConverter)
}

/**
 * Private class allows mutation on the prop values.
 */
private class AnimationState<T>(state: StateImpl<T>, name: T) : StateImpl<T>(name) {

    init {
        for ((prop, value) in state.props) {
            // Make a copy of the new values
            props[prop] = value
        }
    }

    override operator fun <T, V : AnimationVector> set(propKey: PropKey<T, V>, prop: T) {
        @Suppress("UNCHECKED_CAST")
        propKey as PropKey<Any, AnimationVector>
        props[propKey] = prop as Any
    }
}
