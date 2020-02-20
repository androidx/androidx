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
import androidx.animation.AnimationEndReason.BoundReached
import androidx.animation.AnimationEndReason.Interrupted
import androidx.animation.AnimationEndReason.TargetReached

/**
 * This is the base class for [AnimatedValue]. It contains all the functionality of AnimatedValue.
 * It is intended to be used as a base class for the other classes (such as [AnimatedFloat] to build
 * on top of.
 *
 * Animations in this class allow and anticipate the animation target to change frequently. When
 * the target changes as the animation is in-flight, the animation is expected to make a continuous
 * transition to the new target.
 *
 * @param typeConverter A two way type converter that converts from value to [AnimationVector1D],
 *                      [AnimationVector2D], [AnimationVector3D], or [AnimationVector4D], and vice
 *                      versa.
 * @param clock An animation clock observable controlling the progression of the animated value
 */
sealed class BaseAnimatedValue<T, V : AnimationVector>(
    internal val typeConverter: TwoWayConverter<T, V>,
    private val clock: AnimationClockObservable
) {

    /**
     * Current value of the animation.
     */
    abstract var value: T
        protected set

    /**
     * Indicates whether the animation is running.
     */
    var isRunning: Boolean = false
        internal set

    /**
     * The target of the current animation. This target will not be the same as the value of the
     * animation, until the animation finishes un-interrupted.
     */
    var targetValue: T
        get() {
            if (_targetBackingField != null) {
                return _targetBackingField!!
            } else {
                return value
            }
        }
        internal set(newTarget) {
            _targetBackingField = newTarget
        }

    // TODO: remove the backing field when b/148422703 is fixed
    private var _targetBackingField: T? = null
        get() {
            if (field == null) {
                field = value
            }
            return field
        }

    /**
     * Velocity of the animation. The velocity will be of [AnimationVector1D], [AnimationVector2D],
     * [AnimationVector3D], or [AnimationVector4D] type.
     */
    internal var velocityVector: V
        get() = _velocityBackField!!
        set(value) {
            _velocityBackField = value
        }

    // TODO: remove the backing field when b/148422703 is fixed
    private var _velocityBackField: V? = null
        get() {
            if (field == null) {
                field = typeConverter.convertToVector(value).newInstance()
            }
            return field
        }

    internal var onEnd: ((AnimationEndReason, T) -> Unit)? = null
    private lateinit var anim: AnimationWrapper<T, V>
    private var startTime: Long = Unset
    // last frame time only gets updated during the animation pulse. It will be reset at the
    // end of the animation.
    private var lastFrameTime: Long = Unset

    private var animationClockObserver = object : AnimationClockObserver {
        override fun onAnimationFrame(frameTimeMillis: Long) {
            doAnimationFrame(frameTimeMillis)
        }
    }

    // TODO: Need a test for animateTo(...) being called with the same target value
    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, and start a new animation from the current value to the new target
     * value.
     *
     * @param targetValue The new value to animate to
     */
    fun animateTo(targetValue: T) {
        toValueInternal(targetValue, null, PhysicsBuilder())
    }

    // TODO: merge the 4 animateTo() methods into one with default values when issue around
    //  erroneous code gen on default value of generic type is fixed:
    //  https://youtrack.jetbrains.com/issue/KT-28228

    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, invoke [onEnd] that is associated with that animation, and start
     * a new animation from the current value to the new target value.
     *
     * @param targetValue The new value to animate to
     * @param onEnd A callback that will be invoked when the animation finished by any reason.
     */
    fun animateTo(targetValue: T, onEnd: (AnimationEndReason, T) -> Unit) {
        toValueInternal(targetValue, onEnd, PhysicsBuilder())
    }

    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, invoke [onEnd] that is associated with that animation, and start
     * a new animation from the current value to the new target value.
     *
     * @param targetValue The new value to animate to
     * @param anim The animation that will be used to animate from the current value to the new
     *             target value
     * @param onEnd A callback that will be invoked when the animation finished by any reason.
     */
    fun animateTo(
        targetValue: T,
        anim: AnimationBuilder<T>,
        onEnd: (AnimationEndReason, T) -> Unit
    ) {
        toValueInternal(targetValue, onEnd, anim)
    }

    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, invoke [onEnd] that is associated with that animation, and start
     * a new animation from the current value to the new target value.
     *
     * @param targetValue The new value to animate to
     * @param anim The animation that will be used to animate from the current value to the new
     *             target value
     */
    fun animateTo(targetValue: T, anim: AnimationBuilder<T>) {
        toValueInternal(targetValue, null, anim)
    }

    private fun toValueInternal(
        targetValue: T,
        onEnd: ((AnimationEndReason, T) -> Unit)?,
        anim: AnimationBuilder<T>
    ) {
        if (isRunning) {
            notifyEnded(Interrupted, value)
        }

        this.targetValue = targetValue
        val animationWrapper = TargetBasedAnimationWrapper(
            value, velocityVector, targetValue, anim.build(typeConverter), typeConverter
        )

        if (DEBUG) {
            Log.w(
                "AnimValue", "To value called: start value: $value," +
                        "end value: $targetValue, velocity: $velocityVector"
            )
        }
        this.onEnd = onEnd
        startAnimation(animationWrapper)
    }

    /**
     * Sets the current value to the target value immediately, without any animation.
     *
     * @param targetValue The new target value to set [value] to.
     */
    open fun snapTo(targetValue: T) {
        stop()
        value = targetValue
        this.targetValue = targetValue
    }

    /**
     * Stops any on-going animation. No op if no animation is running. Note that this method does
     * not skip the animation value to its target value. Rather the animation will be stopped in its
     * track.
     */
    fun stop() {
        if (isRunning) {
            endAnimation(Interrupted)
        }
    }

    internal fun notifyEnded(endReason: AnimationEndReason, endValue: T) {
        val onEnd = this.onEnd
        this.onEnd = null
        onEnd?.invoke(endReason, endValue)
    }

    private fun doAnimationFrame(timeMillis: Long) {
        val playtime: Long
        if (startTime == Unset) {
            startTime = timeMillis
            playtime = 0
        } else {
            playtime = timeMillis - startTime
        }

        lastFrameTime = timeMillis
        value = anim.getValue(playtime)
        velocityVector = anim.getVelocity(playtime)

        checkFinished(playtime)
    }

    protected open fun checkFinished(playtime: Long) {
        val animationFinished = anim.isFinished(playtime)
        if (DEBUG) {
            val debugLogMessage = if (animationFinished)
                "value = $value, playtime = $playtime, animation finished"
            else
                "value = $value, playtime = $playtime, velocity: $velocityVector"
            Log.w("AnimValue", debugLogMessage)
        }
        if (animationFinished) endAnimation()
    }

    internal fun startAnimation(anim: AnimationWrapper<T, V>) {
        this.anim = anim
        // Quick sanity check before officially starting
        if (anim.isFinished(0)) {
            // If the animation value & velocity is already meeting the finished condition before
            // the animation even starts, end it now.
            endAnimation()
            return
        }

        if (isRunning) {
            startTime = lastFrameTime
        } else {
            startTime = Unset
            isRunning = true
            clock.subscribe(animationClockObserver)
        }
        if (DEBUG) {
            Log.w("AnimValue", "start animation")
        }
    }

    internal fun endAnimation(endReason: AnimationEndReason = TargetReached) {
        clock.unsubscribe(animationClockObserver)
        isRunning = false
        startTime = Unset
        lastFrameTime = Unset
        if (DEBUG) {
            Log.w("AnimValue", "end animation with reason $endReason")
        }
        notifyEnded(endReason, value)
        // reset velocity after notifyFinish as we might need to return it in onFinished callback
        // depending on whether or not velocity was involved in the animation
        velocityVector.reset()
    }
}

/**
 * AnimatedValue is an animatable value holder. It can hold any type of value, and automatically
 * animate the value change when the value is changed via [animateTo]. AnimatedValue supports value
 * change during an ongoing value change animation. When that happens, a new animation will
 * transition AnimatedValue from its current value (i.e. value at the point of interruption) to the
 * new target. This ensures that the value change is always continuous.
 *
 */
abstract class AnimatedValue<T, V : AnimationVector>(
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable
) : BaseAnimatedValue<T, V>(typeConverter, clock) {
    val velocity: V
        get() = velocityVector
}

/**
 * This class inherits most of the functionality from BaseAnimatedValue. In addition, it tracks
 * velocity and supports the definition of bounds. Once bounds are defined using [setBounds], the
 * animation will consider itself finished when it reaches the upper or lower bound, even when the
 * velocity is non-zero.
 *
 * @param clock An animation clock observable controlling the progression of the animated value
 */
abstract class AnimatedFloat(
    clock: AnimationClockObservable
) : BaseAnimatedValue<Float, AnimationVector1D>(FloatToVectorConverter, clock) {

    /**
     * Lower bound of the animation value. When animations reach this lower bound, it will
     * automatically stop with [AnimationEndReason] being [AnimationEndReason.BoundReached].
     * This bound is [Float.NEGATIVE_INFINITY] by default. It can be adjusted via [setBounds].
     */
    var min: Float = Float.NEGATIVE_INFINITY
        private set
    /**
     * Upper bound of the animation value. When animations reach this upper bound, it will
     * automatically stop with [AnimationEndReason] being [AnimationEndReason.BoundReached].
     * This bound is [Float.POSITIVE_INFINITY] by default. It can be adjusted via [setBounds].
     */
    var max: Float = Float.POSITIVE_INFINITY
        private set

    val velocity: Float
        get() = velocityVector.value

    /**
     * Sets up the bounds that the animation should be constrained to. Note that when the animation
     * reaches the bounds it will stop right away, even when there is remaining velocity.
     *
     * @param min Lower bound of the animation value. Defaults to [Float.NEGATIVE_INFINITY]
     * @param max Upper bound of the animation value. Defaults to [Float.POSITIVE_INFINITY]
     */
    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY) {
        if (max < min) {
            // throw exception?
        }
        this.min = min
        this.max = max
    }

    override fun snapTo(targetValue: Float) {
        super.snapTo(targetValue.coerceIn(min, max))
    }

    override fun checkFinished(playtime: Long) {
        if (value < min) {
            value = min
            endAnimation(BoundReached)
        } else if (value > max) {
            value = max
            endAnimation(BoundReached)
        } else {
            super.checkFinished(playtime)
        }
    }
}

/**
 * Typealias for lambda that will be invoked when fling animation ends.
 * Unlike [AnimatedValue.animateTo] onEnd, this lambda includes 3rd param remainingVelocity,
 * that represents velocity that wasn't consumed after fling finishes.
 */
// TODO: Consolidate onAnimationEnd and onEnd
typealias OnAnimationEnd =
            (endReason: AnimationEndReason, endValue: Float, remainingVelocity: Float) -> Unit

/**
 * Starts a fling animation with the specified starting velocity.
 *
 * @param startVelocity Starting velocity of the fling animation
 * @param decay The decay animation used for slowing down the animation from the starting
 *              velocity
 * @param onEnd An optional callback that will be invoked when this fling animation is
 *                   finished.
 */
// TODO: Figure out an API for customizing the type of decay & the friction
fun AnimatedFloat.fling(
    startVelocity: Float,
    decay: DecayAnimation = ExponentialDecay(),
    onEnd: OnAnimationEnd? = null
) {
    if (isRunning) {
        notifyEnded(Interrupted, value)
    }

    this.onEnd = { endReason, endValue ->
        onEnd?.invoke(endReason, endValue, velocity)
    }

    // start from current value with the given velocity
    targetValue = decay.getTarget(value, startVelocity)
    val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
    startAnimation(animWrapper)
}

// TODO: Devs may want to change the target animation based on how close the target is to the
//       snapping position.
/**
 * Starts a fling animation with the specified starting velocity.
 *
 * @param startVelocity Starting velocity of the fling animation
 * @param adjustTarget A lambda that takes in the projected destination based on the decay
 *                     animation, and returns a nullable TargetAnimation object that contains a
 *                     new destination and an animation to animate to the new destination. This
 *                     lambda should return null when the original target is respected.
 * @param decay The decay animation used for slowing down the animation from the starting
 *              velocity
 * @param onEnd An optional callback that will be invoked when the animation
 *              finished by any reason.
 */
fun AnimatedFloat.fling(
    startVelocity: Float,
    decay: DecayAnimation = ExponentialDecay(),
    adjustTarget: (Float) -> TargetAnimation?,
    onEnd: OnAnimationEnd? = null
) {
    if (isRunning) {
        notifyEnded(Interrupted, value)
    }

    this.onEnd = { endReason, endValue ->
        onEnd?.invoke(endReason, endValue, velocity)
    }

    // start from current value with the given velocity
    if (DEBUG) {
        Log.w("AnimFloat", "Calculating target. Value: $value, velocity: $startVelocity")
    }
    targetValue = decay.getTarget(value, startVelocity)
    val targetAnimation = adjustTarget(targetValue)
    if (DEBUG) {
        Log.w(
            "AnimFloat", "original targetValue: $targetValue, new target:" +
                    " ${targetAnimation?.target}"
        )
    }
    if (targetAnimation == null) {
        val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
        startAnimation(animWrapper)
    } else {
        targetValue = targetAnimation.target
        val animWrapper = TargetBasedAnimationWrapper(
            value,
            AnimationVector1D(startVelocity),
            targetAnimation.target,
            targetAnimation.animation.build(typeConverter),
            typeConverter
        )
        startAnimation(animWrapper)
    }
}

private const val Unset: Long = -1

/**
 * Factory method for creating an [AnimatedValue] object, and initialize the value field to
 * [initVal].
 *
 * @param initVal Initial value to initialize the animation to.
 * @param typeConverter Converter for converting value type [T] to [AnimationVector], and vice versa
 * @param clock The animation clock used to drive the animation.
 */
fun <T, V : AnimationVector> AnimatedValue(
    initVal: T,
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable
): AnimatedValue<T, V> =
    AnimatedValueImpl(initVal, typeConverter, clock)

/**
 * Factory method for creating an [AnimatedVector] object, and initialize the value field to
 * [initVal].
 *
 * @param initVal Initial value to initialize the animation to.
 * @param clock The animation clock used to drive the animation.
 */
fun <V : AnimationVector> AnimatedVector(
    initVal: V,
    clock: AnimationClockObservable
): AnimatedValue<V, V> =
    AnimatedValueImpl(initVal, TwoWayConverter({ it }, { it }), clock)

/**
 * Factory method for creating an [AnimatedFloat] object, and initialize the value field to
 * [initVal].
 *
 * @param initVal Initial value to initialize the animation to.
 * @param clock The animation clock used to drive the animation.
 */
fun AnimatedFloat(initVal: Float, clock: AnimationClockObservable): AnimatedFloat =
    AnimatedFloatImpl(initVal, clock)

// Private impl for AnimatedValue
private class AnimatedValueImpl<T, V : AnimationVector>(
    initVal: T,
    typeConverter: TwoWayConverter<T, V>,
    clock: AnimationClockObservable
) : AnimatedValue<T, V>(typeConverter, clock) {
    override var value: T = initVal
}

// Private impl for AnimatedFloat
private class AnimatedFloatImpl(
    initVal: Float,
    clock: AnimationClockObservable
) : AnimatedFloat(clock) {
    override var value: Float = initVal
}