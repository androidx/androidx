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
import android.view.Choreographer
import androidx.animation.InterruptionHandling.UNINTERRUPTIBLE

/**
 * [TransitionAnimation] is responsible for animating from one set of property values (i.e. one
 * [TransitionState]) to another. More specifically, it reads the property values out of the new
 * state that it is going to, as well as the animations defined for the properties, and run these
 * animations until all properties have reached their pre-defined values in the new state. When no
 * animation is specified for a property, a default [Physics] animation will be used.
 *
 * [TransitionAnimation] may be interrupted while the animation is on-going by a request to go
 * to another state. [TransitionAnimation] ensures that all the animating properties preserve their
 * current value and velocity as they createAnimation to the new state.
 *
 * Once a [TransitionDefinition] is instantiated, a [TransitionAnimation] can be created via
 * [TransitionDefinition.createAnimation].
 */
// TODO: refactor out dependency on choreographer
class TransitionAnimation<T> : Choreographer.FrameCallback {

    var onUpdate: (() -> Unit)? = null
    var onStateChangeFinished: ((T) -> Unit)? = null
    private val UNSET = -1L
    private var def: TransitionDefinition<T>
    private var fromState: StateImpl<T>
    private var toState: StateImpl<T>
    private val currentState: AnimationState<T>
    private var startTime: Long = UNSET
    private var lastFrameTime: Long = UNSET
    private var pendingState: StateImpl<T>? = null
    private var currentAnimations: MutableMap<PropKey<Any>, Animation<Any>> = mutableMapOf()
    private var startVelocityMap: MutableMap<PropKey<Any>, Float> = mutableMapOf()
    private var isRunning = false

    // TODO("Create a more efficient code path for default only transition def")

    internal constructor(def: TransitionDefinition<T>) {
        this.def = def
        currentState = AnimationState(def.defaultState, def.defaultState.name)
        // Need to come up with a better plan to avoid the foot gun of accidentally modifying state
        fromState = def.defaultState
        toState = def.defaultState
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
            val startVelocity = startVelocityMap[prop] ?: 0f
            val currentVelocity = currentAnimations[prop]?.getVelocity(
                playTime, fromState[prop], toState[prop], startVelocity,
                prop::interpolate
            ) ?: 0f
            startVelocityMap[prop] = currentVelocity
            currentAnimations[prop] = transitionSpec.getAnimationForProp(prop)
            // TODO: Will need to track a few timelines if we support partially defined list of
            // props in each state.
        }

        startAnimation()

        fromState = AnimationState(currentState, toState.name)
        toState = newState
        if (DEBUG) {
            Log.w("TransAnim", "Animating to new state: ${toState.name}")
        }
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
    operator fun <T> get(propKey: PropKey<T>): T {
        return currentState[propKey]
    }

    // TODO: Make this internal
    override fun doFrame(frameTimeNanos: Long) {
        if (isRunning) {
            doAnimationFrame(frameTimeNanos / 1000000L)
            // TODO: Use refactor out all the dependencies on Choreographer
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // Start animation if not running, otherwise reset start time
    private fun startAnimation() {
        if (!isRunning) {
            isRunning = true
            Choreographer.getInstance().postFrameCallback(this)
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
        for ((prop, animation) in currentAnimations) {
            val velocity: Float = startVelocityMap[prop] ?: 0f
            currentState[prop] =
                animation.getValue(playTime, fromState[prop], toState[prop], velocity,
                    prop::interpolate)
        }

        // Prune the finished animations
        currentAnimations.entries.removeAll {
            val prop = it.key
            val velocity: Float = startVelocityMap[prop] ?: 0f
            it.value.isFinished(playTime, fromState[prop], toState[prop], velocity)
        }

        onUpdate?.invoke()

        // call end animation when all animations end
        if (currentAnimations.isEmpty()) {
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
        Choreographer.getInstance().removeFrameCallback(this)
        startTime = UNSET
        lastFrameTime = UNSET
        isRunning = false
    }
}

/**
 * Private class allows mutation on the prop values.
 */
private class AnimationState<T>(state: StateImpl<T>, name: T) : StateImpl<T>(name) {

    init {
        for ((prop, value) in state.props) {
            // Make a copy of the new values
            val newValue = prop.interpolate(value, value, 0f)
            props[prop] = newValue
        }
    }

    override operator fun <T> set(propKey: PropKey<T>, prop: T) {
        @Suppress("UNCHECKED_CAST")
        propKey as PropKey<Any>
        props[propKey] = prop as Any
    }
}
