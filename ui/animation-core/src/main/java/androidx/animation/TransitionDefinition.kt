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

import androidx.ui.graphics.Color
import kotlin.experimental.ExperimentalTypeInference

/**
 * Static specification for the transition from one state to another.
 *
 * Each property involved in the states that the transition is from and to can have an animation
 * associated with it. When such an animation is defined, the animation system will be using it
 * instead of the default [Physics] animation to createAnimation the value change for that property.
 *
 * The following sample code will create a [TransitionSpec] that defines animations for 3
 * properties: `radius`, `alpha`, and `background`.
 * ```
 *  transition(fromState = ButtonState.Released, toState = ButtonState.Pressed) {
 *      radius using tween {
 *          easing = LinearEasing
 *          duration = 75
 *      }
 *      alpha using keyframes {
 *          duration = 375
 *          0f at 0 // ms  // Optional
 *          0.4f at 75 // ms
 *          0.4f at 225 // ms
 *          0f at 375 // ms  // Optional
 *      }
 *      background using physics {
 *          dampingRatio = 1.0f
 *      }
 *      interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
 *  }
 * ```
 **/
class TransitionSpec<S> internal constructor() {
    /**
     * The state that the transition is going from. It defaults to null, meaning any state.
     */
    internal var fromState: S? = null

    /**
     * The state that the transition is going to. It defaults to null, meaning any state.
     */
    internal var toState: S? = null

    /**
     * Optional state where should we start switching after this transition finishing.
     */
    var nextState: S? = null

    /**
     * The interruption handling mechanism. The default interruption handling is
     * [InterruptionHandling.PHYSICS]. Meaning both value and velocity of the property will be
     * preserved as the target state (and therefore target animation value) changes.
     * [InterruptionHandling.TWEEN], which only ensures the continuity of current animation value.
     * [InterruptionHandling.UNINTERRUPTIBLE] defines a scenario where an animation is so important
     * that it cannot be interrupted, so the new state request has to be queued.
     * [InterruptionHandling.SNAP_TO_END] can be used for cases where higher priority events (such
     * as user gesture) come in and the on-going animation needs to finish immediately to give way
     * to the user events.
     */
    var interruptionHandling: InterruptionHandling = InterruptionHandling.PHYSICS

    /**
     * The default animation to use when it wasn't explicitly provided for a property
     */
    internal var defaultAnimation: () -> Animation<Any> = { Physics() }

    private val propAnimation: MutableMap<PropKey<*>, Animation<*>> = mutableMapOf()
    internal fun <T> getAnimationForProp(prop: PropKey<T>): Animation<T> {
        @Suppress("UNCHECKED_CAST")
        return (propAnimation.getOrPut(prop, defaultAnimation)) as Animation<T>
    }

    /**
     * Associates a property with a [AnimationBuilder]
     *
     * @param builder: [AnimationBuilder] for animating [this] property value changes
     */
    infix fun <T> PropKey<T>.using(builder: AnimationBuilder<T>) {
        propAnimation[this] = builder.build()
    }

    /**
     * Creates a [Tween] animation, initialized with [init]
     *
     * @param init Initialization function for the [Tween] animation
     */
    fun <T> tween(init: TweenBuilder<T>.() -> Unit): DurationBasedAnimationBuilder<T> =
        TweenBuilder<T>().apply(init)

    /**
     * Creates a [Physics] animation, initialized with [init]
     *
     * @param init Initialization function for the [Physics] animation
     */
    fun <T> physics(init: PhysicsBuilder<T>.() -> Unit): AnimationBuilder<T> =
        PhysicsBuilder<T>().apply(init)

    /**
     * Creates a [Keyframes] animation, initialized with [init]
     *
     * @param init Initialization function for the [Keyframes] animation
     */
    fun <T> keyframes(init: KeyframesBuilder<T>.() -> Unit): DurationBasedAnimationBuilder<T> =
        KeyframesBuilder<T>().apply(init)

    /**
     * Creates a [Repeatable] animation, initialized with [init]
     *
     * @param init Initialization function for the [Repeatable] animation
     */
    fun <T> repeatable(init: RepeatableBuilder<T>.() -> Unit): AnimationBuilder<T> =
        RepeatableBuilder<T>().apply(init)

    /**
     * Creates a Snap animation for immediately switching the animating value to the end value.
     */
    fun <T> snap(): AnimationBuilder<T> = SnapBuilder()
}

/**
 * Static definitions of states and transitions.
 *
 */
class TransitionDefinition<T> {
    internal val states: MutableMap<T, StateImpl<T>> = mutableMapOf()
    internal lateinit var defaultState: StateImpl<T>
    private val transitionSpecs: MutableList<TransitionSpec<T>> = mutableListOf()

    // TODO: Consider also having the initial defined at call site for cases where many components
    // share the same transition def
    private val defaultTransitionSpec = TransitionSpec<T>()

    /**
     * Defines all the properties and their values associated with the state with the name: [name]
     * When a state is specified as default (via [default] = true), transition animation will be
     * using this default state's property values as its initial values to createAnimation from.
     *
     * Note that the first [MutableTransitionState] created with [state] in a [TransitionDefinition]
     * will be used as the initial state.
     *
     * @param name The name of the state, which can be used to createAnimation from or to this state
     * @param init Lambda to initialize a state
     */
    fun state(name: T, init: MutableTransitionState.() -> Unit) {
        val newState = StateImpl(name).apply(init)
        states[name] = newState
        if (!::defaultState.isInitialized) {
            defaultState = newState
        }
    }

    /**
     * Defines a transition from state [fromState] to [toState]. When animating from one state to
     * another, [TransitionAnimation] will find the most specific matching transition, and use the
     * animations defined in it for the state transition. Both [fromState] and [toState] are
     * optional. When undefined, it means a wildcard transition going from/to any state.
     *
     * @param fromState The state that the transition will be animated from
     * @param toState The state that the transition will be animated to
     * @param init Lambda to initialize the transition
     */
    fun transition(fromState: T? = null, toState: T? = null, init: TransitionSpec<T>.() -> Unit) {
        val newSpec = TransitionSpec<T>().apply(init)
        newSpec.fromState = fromState
        newSpec.toState = toState
        transitionSpecs.add(newSpec)
    }

    /**
     * Defines a transition from state first value to the second value of the [fromToPair].
     * When animating from one state to another, [TransitionAnimation] will find the most specific
     * matching transition, and use the animations defined in it for the state transition. Both
     * values in the pair can be null. When they are null, it means a wildcard transition going
     * from/to any state.
     *
     * Sample of usage with [Pair]s infix extension [to]:
     *     transition(State.Released to State.Pressed) {
     *         ...
     *     }
     *
     * @param fromToPair The pair of states for this transition
     * @param init Lambda to initialize the transition
     */
    // TODO: support a list of (from, to) pairs
    fun transition(fromToPair: Pair<T?, T?>, init: TransitionSpec<T>.() -> Unit) =
        transition(fromToPair.first, fromToPair.second, init)

    /**
     * With this transition definition we are saying that every time we reach the
     * state 'from' we should immediately snap to 'to' state instead.
     *
     * Sample of usage with [Pair]s infix extension [to]:
     *     snapTransition(State.Released to State.Pressed)
     *
     * @param fromToPair The pair of states for this transition
     * @param nextState Optional state where should we start switching after snap
     */
    fun snapTransition(fromToPair: Pair<T?, T?>, nextState: T? = null) =
        transition(fromToPair) {
            this.nextState = nextState
            defaultAnimation = { Snap() }
        }

    internal fun getSpec(fromState: T, toState: T): TransitionSpec<T> {
        return transitionSpecs.firstOrNull { it.fromState == fromState && it.toState == toState }
            ?: transitionSpecs.firstOrNull { it.fromState == fromState && it.toState == null }
            ?: transitionSpecs.firstOrNull { it.fromState == null && it.toState == toState }
            ?: transitionSpecs.firstOrNull { it.fromState == null && it.toState == null }
            ?: defaultTransitionSpec
    }

    /**
     * Creates a transition animation using the transition definition.
     */
    fun createAnimation() = TransitionAnimation(this)

    /**
     * Returns a state holder for the specific state [name]. Useful for the cases
     * where we don't need actual animation to be happening like in tests.
     */
    fun getStateFor(name: T): TransitionState = states.getValue(name)
}

/**
 * Creates a [TransitionDefinition] using the [init] function to initialize it.
 *
 * @param init Initialization function for the [TransitionDefinition]
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T> transitionDefinition(@BuilderInference init: TransitionDefinition<T>.() -> Unit) =
    TransitionDefinition<T>().apply(init)

enum class InterruptionHandling {
    PHYSICS,
    SNAP_TO_END, // Not yet supported
    TWEEN, // Not yet supported
    UNINTERRUPTIBLE
}

/********************* The rest of this file is an example ***********************/
private enum class ButtonState {
    Pressed,
    Released
}

private val alpha = FloatPropKey()
private val radius = FloatPropKey()
private val background = ColorPropKey()

// TODO: Support states with only part of the props defined

/**
 * TransitionState defines how the UI should look in terms of values of a certain set of properties
 * that are critical to the look and feel for the UI. Each state can be considered as a snapshot of
 * the UI. Once states are defined, if no animations are specified, states will be able to
 * createAnimation from one to another when state changes, using the system's default physics based
 * animation.
 *
 * Transition defines how to createAnimation from one state to another with a specific animation for
 * each property defined in the states. Currently transition supports:
 * 1) tween (simple interpolation between start and end value), physics (i.e. spring only, decay
 * coming). Keyframes support coming soon.
 * 2) When no transition is specified, the default physics based animation will be used. Same for
 * the properties that have no animation associated with them in a transition
 * 3) For each transition, both the from and the to state can be omitted. Omitting in this case is
 * equivalent to a wildcard that means any starting state or ending state. When both are omitted at
 * the same time, it means this transition applies to all the state transitions unless a more
 * specific transition have been defined.
 *
 */
private val example = transitionDefinition {
    state(ButtonState.Pressed) {
        this[alpha] = 0f
        this[radius] = 200f
        this[background] = Color(alpha = 255, red = 255, green = 0, blue = 0)
    }
    state(ButtonState.Released) {
        this[alpha] = 0f
        this[radius] = 60f
        this[background] = Color(alpha = 255, red = 0, green = 255, blue = 0)
    }

    transition(fromState = ButtonState.Released, toState = ButtonState.Pressed) {
        background using tween {
            easing = LinearEasing
            duration = 75 // TODO: support time unit
        }
        alpha using keyframes {
            duration = 375
            0f at 0 // ms  // Optional
            0.4f at 75 // ms
            0.4f at 225 // ms
            0f at 375 // ms  // Optional
        }
        radius using physics {
            dampingRatio = 1.0f
        }
        interruptionHandling = InterruptionHandling.UNINTERRUPTIBLE
    }

    transition(ButtonState.Released to ButtonState.Pressed) {
        //
//        // TODO: how do we define sequential tween, alpha then radius snap
//        alpha using tween {
//            easing = LinearEasing
//            duration = 150
//            onAnimationEnd {
//                // TODO: Does this look better as its own entry?
//                radius to 60f
//            }
//            // TODO: Default behavior for transition: when the transition finishes
//            // normally, all values should be snapped to the pre-defined values.
//        }
//
    }

    // TODO: Support wild card transition
}.createAnimation().apply {
    // Usage
    toState(ButtonState.Released)
}
