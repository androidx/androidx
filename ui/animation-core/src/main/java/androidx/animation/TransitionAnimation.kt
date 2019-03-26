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
class TransitionAnimation<T : Any> : Choreographer.FrameCallback {

    var onUpdate: (() -> Unit)? = null
    var onStateChangeFinished: ((T) -> Unit)? = null
    private val UNSET = -1L
    private var def: TransitionDefinition<T>
    private var fromState: StateImpl
    private var toState: StateImpl
    private var currentState: AnimationState
    private var startTime: Long = UNSET
    private var lastFrameTime: Long = UNSET
    private var pendingState: StateImpl? = null
    private var currentAnimations: MutableMap<PropKey<Any>, Animation<Any>> = mutableMapOf()
    private var startVelocityMap: MutableMap<PropKey<Any>, Float> = mutableMapOf()

    // TODO("Create a more efficient code path for default only transition def")

    internal constructor(def: TransitionDefinition<T>) {
        this.def = def
        currentState = AnimationState(def.defaultState)
        // Need to come up with a better plan to avoid the foot gun of accidentally modifying state
        fromState = def.defaultState
        toState = def.defaultState
    }

    // Interpolate current state and the new state
    private fun setState(newState: StateImpl) {
        if (isRunning()) {
            val currentSpec = def.getSpec(fromState.name, toState.name)
            if (currentSpec.interruptionHandling == InterruptionHandling.UNINTERRUPTIBLE) {
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
            prop as PropKey<Any>
            val propVal = currentState.get(prop)
            // TODO: support velocity in more default types than Floats
            if (propVal is Float) {
                // map velocity to new target range space
                val startVal = fromState.get(prop) as Float
                val endVal = toState.get(prop) as Float
                val startVelocity = startVelocityMap[prop] ?: 0f

                val velocity = currentAnimations[prop]?.getVelocity(
                    playTime, startVal, endVal, startVelocity
                ) ?: 0f
                startVelocityMap[prop] = velocity
            }
            currentAnimations[prop] = transitionSpec.getAnimationForProp(prop)
            // TODO: Will need to track a few timelines if we support partially defined list of
            // props in each state.
        }

        startAnimation()

        fromState = AnimationState(currentState, toState.name)
        toState = newState
        if (DEBUG) {
            Log.w("LTD", "Animating to new state: ${toState.name}")
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
        if (def.states[name] == null) {
            // Throw exception or ignore?
        } else if (toState.name == name) {
            // no op
            return
        } else {
            val nextState = def.states[name]!!
            setState(nextState)
        }
    }

    /**
     * Gets the value of a property with a given property key.
     *
     * @param propKey Property key (defined in [TransitionDefinition]) for a specific property
     */
    operator fun <T : Any> get(propKey: PropKey<T>): T {
        return currentState.get(propKey)
    }

    // TODO: Make this internal
    override fun doFrame(frameTimeNanos: Long) {
        doAnimationFrame(frameTimeNanos / 1000000L)
        if (isRunning()) {
            // TODO: Use refactor out all the dependencies on Choreographer
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // Start animation if not running, otherwise reset start time
    private fun startAnimation() {
        if (!isRunning()) {
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

            fromState = toState
            onStateChangeFinished?.invoke(toState.name as T)
            if (pendingState == null) {
                endAnimation()
            } else {
                setState(pendingState!!)
                pendingState = null
            }
        }
    }

    private fun endAnimation() {
        Choreographer.getInstance().removeFrameCallback(this)
        startTime = UNSET
    }
    private fun isRunning() = (startTime != UNSET)
}

/**
 * Private class allows mutation on the prop values.
 */
private class AnimationState(state: StateImpl, name: Any = "") : StateImpl(name) {

    init {
        for ((prop, value) in state.props) {
            // Make a copy of the new values
            val newValue = (prop as PropKey<Any>).interpolate(value, value, 0f)
            props[prop] = newValue
        }
    }

    override operator fun set(name: PropKey<out Any>, prop: Any) {
        props[name] = prop
    }
}