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
import androidx.ui.lerp

/**
 * This is the base class for [AnimatedValue]. It contains all the functionality of AnimatedValue.
 * It is intended to be used as a base class for the other classes (such as [AnimatedFloat] to build
 * on top of.
 *
 * @param valueHolder A value holder whose value gets updated by [BaseAnimatedValue] on every
 *                    animation frame.
 */
sealed class BaseAnimatedValue<T>(private val valueHolder: ValueHolder<T>)
    : DynamicTargetAnimation<T> {

    /**
     * Creates a [BaseAnimatedValue] instance that starts at the given value, and uses the given
     * value interpolator
     *
     * @param initVal Initial value of the [BaseAnimatedValue]
     * @param valueInterpolator The value interpolator used to interpolate two values of type [T]
     */
    constructor(
        initVal: T,
        valueInterpolator: (T, T, Float) -> T
    ): this(ValueHolderImpl<T>(initVal, valueInterpolator))

    override var value: T
        internal set(newVal: T) {
            valueHolder.value = newVal
        }
        get() = valueHolder.value

    override var isRunning: Boolean = false
        internal set

    override var targetValue: T = valueHolder.value
        internal set

    internal var internalVelocity: Float = 0f
    internal var onFinished: ((Boolean) -> Unit)? = null
    private lateinit var anim: AnimationWrapper<T>
    private var startTime: Long = -1

    private var frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // TODO: Refactor out all the dependencies on Choreographer
            doAnimationFrame(frameTimeNanos / 1000000L)
        }
    }

    override fun animateTo(targetValue: T) {
        toValueInternal(targetValue, null, PhysicsBuilder())
    }
    override fun animateTo(targetValue: T, onFinished: (Boolean) -> Unit) {
        toValueInternal(targetValue, onFinished, PhysicsBuilder())
    }
    override fun animateTo(
        targetValue: T,
        anim: AnimationBuilder<T>,
        onFinished: (Boolean) -> Unit
    ) {
        toValueInternal(targetValue, onFinished, anim)
    }
    override fun animateTo(targetValue: T, anim: AnimationBuilder<T>) {
        toValueInternal(targetValue, null, anim)
    }

    private fun toValueInternal(
        targetValue: T,
        onFinished: ((Boolean) -> Unit)?,
        anim: AnimationBuilder<T>
    ) {
        if (isRunning) {
            notifyFinished(true)
        }

        this.targetValue = targetValue
        val animationWrapper = TargetBasedAnimationWrapper(value, internalVelocity, targetValue,
            valueHolder.interpolator, anim.build())

        if (DEBUG) {
            Log.w(
                "AnimValue", "To value called: start value: $value," +
                        "end value: $targetValue, velocity: $internalVelocity"
            )
        }
        this.onFinished = onFinished
        startAnimation(animationWrapper)
    }

    override fun snapTo(targetValue: T) {
        stop()
        value = targetValue
        this.targetValue = targetValue
    }

    override fun stop() {
        if (isRunning) {
            endAnimation(true)
        }
    }

    internal fun notifyFinished(canceled: Boolean) {
        val onFinished = this.onFinished
        this.onFinished = null
        onFinished?.invoke(canceled)
    }

    internal open fun doAnimationFrame(time: Long) {
        var playtime: Long
        if (startTime == -1L) {
            startTime = time
            playtime = 0
        } else {
            playtime = time - startTime
        }
        value = anim.getValue(playtime)
        internalVelocity = anim.getVelocity(playtime)
        val animationFinished = anim.isFinished(playtime)
        if (!animationFinished) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
            if (DEBUG) {
                Log.w(
                    "AnimValue",
                    "value = $value, playtime = $playtime, velocity: $internalVelocity"
                )
            }
        } else {
            if (DEBUG) {
                Log.w("AnimValue", "value = $value, playtime = $playtime, animation finished")
            }
            endAnimation()
        }
    }

    internal fun startAnimation(anim: AnimationWrapper<T>) {
        this.anim = anim
        startTime = -1
        if (!isRunning) {
            isRunning = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
        if (DEBUG) {
            Log.w("AnimValue", "start animation")
        }
    }

    internal fun endAnimation(canceled: Boolean = false) {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        isRunning = false
        startTime = -1
        if (DEBUG) {
            Log.w("AnimValue", "end animation")
        }
        notifyFinished(canceled)
    }
}

/**
 * AnimatedValue is an animatable value holder. It can hold any type of value, and automatically
 * animate the value change when the value is changed via [animateTo]. AnimatedValue supports value
 * change during an ongoing value change animation. When that happens, a new animation will
 * transition AnimatedValue from its current value (i.e. value at the point of interruption) to the
 * new target. This ensures that the value change is always continuous.
 *
 * @param valueHolder A value holder whose value field will be updated during animations
 */
class AnimatedValue<T>(valueHolder: ValueHolder<T>) : BaseAnimatedValue<T>(valueHolder)

/**
 * This class inherits most of the functionality from BaseAnimatedValue. In addition, it tracks
 * velocity and supports the definition of bounds. Once bounds are defined using [setBounds], the
 * animation will consider itself finished when it reaches the upper or lower bound, even when the
 * velocity is non-zero.
 *
 * @param valueHolder A value holder of Float type whose value field will be updated during
 *                    animations
 */
class AnimatedFloat(valueHolder: ValueHolder<Float>) : BaseAnimatedValue<Float>(valueHolder) {

    /**
     * Velocity of the current animation.
     */
    var velocity: Float = 0f
        get() = internalVelocity

    private var min: Float = Float.NEGATIVE_INFINITY
    private var max: Float = Float.POSITIVE_INFINITY

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

    override fun doAnimationFrame(time: Long) {
        super.doAnimationFrame(time)
        if (value < min) {
            value = min
            endAnimation()
        } else if (value > max) {
            value = max
            stop()
            endAnimation()
        }
    }
}

/**
 * Starts a fling animation with the specified starting velocity.
 *
 * @param startVelocity Starting velocity of the fling animation
 * @param decay The decay animation used for slowing down the animation from the starting
 *              velocity
 * @param onFinished An optional callback that will be invoked when this fling animation is
 *                   finished.
 */
// TODO: Figure out an API for customizing the type of decay & the friction
fun AnimatedFloat.fling(
    startVelocity: Float,
    decay: DecayAnimation = ExponentialDecay(),
    onFinished: ((Boolean) -> Unit)? = null
) {
    if (isRunning) {
        notifyFinished(true)
    }

    this.onFinished = onFinished

    // start from current value with the given internalVelocity
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
 * @param onFinished An optional callback that will be invoked when this fling animation is
 *                   finished.
 */
fun AnimatedFloat.fling(
    startVelocity: Float,
    decay: DecayAnimation = ExponentialDecay(),
    adjustTarget: (Float) -> TargetAnimation?,
    onFinished: ((Boolean) -> Unit)? = null
) {
    if (isRunning) {
        notifyFinished(true)
    }

    this.onFinished = onFinished

    // start from current value with the given internalVelocity
    if (DEBUG) {
        Log.w("AnimFloat", "Calculating target. Value: $value, velocity: $startVelocity")
    }
    targetValue = decay.getTarget(value, startVelocity)
    val targetAnimation = adjustTarget(targetValue)
    if (DEBUG) {
        Log.w("AnimFloat", "original targetValue: $targetValue, new target:" +
                " ${targetAnimation?.target}")
    }
    if (targetAnimation == null) {
        val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
        startAnimation(animWrapper)
    } else {
        targetValue = targetAnimation.target
        val animWrapper = targetAnimation.animation
            .createWrapper(value, startVelocity, targetAnimation.target, ::lerp)
        startAnimation(animWrapper)
    }
}
