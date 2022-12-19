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
import androidx.annotation.IntRange
import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLContainer
import androidx.constraintlayout.core.parser.CLNumber
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLString
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

@ExperimentalMotionApi
fun Transition(
    from: String = "start",
    to: String = "end",
    transitionContent: TransitionScope.() -> Unit
): Transition {
    val transitionScope = TransitionScope(from, to)
    transitionScope.transitionContent()
    return TransitionImpl(transitionScope.getObject())
}

@ExperimentalMotionApi
class TransitionScope internal constructor(
    private val from: String,
    private val to: String
) {
    private val containerObject = CLObject(charArrayOf())

    private val keyFramesObject = CLObject(charArrayOf())
    private val keyAttributesArray = CLArray(charArrayOf())
    private val keyPositionsArray = CLArray(charArrayOf())
    private val keyCyclesArray = CLArray(charArrayOf())

    private val onSwipeObject = CLObject(charArrayOf())

    internal fun reset() {
        containerObject.clear()
        keyFramesObject.clear()
        keyAttributesArray.clear()
        onSwipeObject.clear()
    }

    private fun addKeyAttributesIfMissing() {
        containerObject.put("KeyFrames", keyFramesObject)
        keyFramesObject.put("KeyAttributes", keyAttributesArray)
    }

    private fun addKeyPositionsIfMissing() {
        containerObject.put("KeyFrames", keyFramesObject)
        keyFramesObject.put("KeyPositions", keyPositionsArray)
    }

    private fun addKeyCyclesIfMissing() {
        containerObject.put("KeyFrames", keyFramesObject)
        keyFramesObject.put("KeyCycles", keyCyclesArray)
    }

    var motionArc: Arc = Arc.None

    var onSwipe: OnSwipe? = null

    fun keyAttributes(
        vararg targets: ConstrainedLayoutReference,
        keyAttributesContent: KeyAttributesScope.() -> Unit
    ) {
        val scope = KeyAttributesScope(*targets)
        keyAttributesContent(scope)
        addKeyAttributesIfMissing()
        keyAttributesArray.add(scope.keyFramePropsObject)
    }

    fun keyPositions(
        vararg targets: ConstrainedLayoutReference,
        keyPositionsContent: KeyPositionsScope.() -> Unit
    ) {
        val scope = KeyPositionsScope(*targets)
        keyPositionsContent(scope)
        addKeyPositionsIfMissing()
        keyPositionsArray.add(scope.keyFramePropsObject)
    }

    fun keyCycles(
        vararg targets: ConstrainedLayoutReference,
        keyCyclesContent: KeyCyclesScope.() -> Unit
    ) {
        val scope = KeyCyclesScope(*targets)
        keyCyclesContent(scope)
        addKeyCyclesIfMissing()
        keyCyclesArray.add(scope.keyFramePropsObject)
    }

    /**
     * Creates one [ConstrainedLayoutReference] corresponding to the [ConstraintLayout] element
     * with [id].
     */
    fun createRefFor(id: Any): ConstrainedLayoutReference = ConstrainedLayoutReference(id)

    internal fun getObject(): CLObject {
        containerObject.putString("pathMotionArc", motionArc.name)
        containerObject.putString("from", from)
        containerObject.putString("to", to)
        // TODO: Uncomment once we decide how to deal with Easing discrepancy from user driven
        //  `progress` value. Eg: `animateFloat(tween(duration, LinearEasing))`
//        containerObject.putString("interpolator", easing.name)
//        containerObject.putNumber("duration", durationMs.toFloat())
        onSwipe?.let {
            containerObject.put("onSwipe", onSwipeObject)
            onSwipeObject.putString("direction", it.direction.name)
            onSwipeObject.putNumber("dragScale", it.dragScale)
            it.dragAround?.id?.let { id ->
                onSwipeObject.putString("around", id.toString())
            }
            onSwipeObject.putNumber("threshold", it.dragThreshold)
            onSwipeObject.putString("anchor", it.anchor.id.toString())
            onSwipeObject.putString("side", it.side.name)
            onSwipeObject.putString("touchUp", it.onTouchUp.name)
            onSwipeObject.putString("mode", it.mode.name)
            onSwipeObject.putNumber("maxVelocity", it.mode.maxVelocity)
            onSwipeObject.putNumber("maxAccel", it.mode.maxAcceleration)
            onSwipeObject.putNumber("springMass", it.mode.springMass)
            onSwipeObject.putNumber("springStiffness", it.mode.springStiffness)
            onSwipeObject.putNumber("springDamping", it.mode.springDamping)
            onSwipeObject.putNumber("stopThreshold", it.mode.springThreshold)
            onSwipeObject.putString("springBoundary", it.mode.springBoundary.name)
        }
        return containerObject
    }
}

@ExperimentalMotionApi
open class BaseKeyFramesScope internal constructor(vararg targets: ConstrainedLayoutReference) {
    internal val keyFramePropsObject = CLObject(charArrayOf()).apply {
        clear()
    }

    private val targetsContainer = CLArray(charArrayOf())
    protected val framesContainer = CLArray(charArrayOf())

    var easing: Easing by addNameOnPropertyChange(Easing.Standard, "transitionEasing")

    init {
        keyFramePropsObject.put("target", targetsContainer)
        keyFramePropsObject.put("frames", framesContainer)
        targets.forEach {
            val targetChars = it.id.toString().toCharArray()
            targetsContainer.add(CLString(targetChars).apply {
                start = 0
                end = targetChars.size.toLong() - 1
            })
        }
    }

    protected fun <E : NamedPropertyOrValue?> addNameOnPropertyChange(
        initialValue: E,
        nameOverride: String? = null
    ) =
        object : ObservableProperty<E>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: E, newValue: E) {
                val name = nameOverride ?: property.name
                if (newValue != null) {
                    keyFramePropsObject.putString(name, newValue.name)
                }
            }
        }
}

@ExperimentalMotionApi
class KeyAttributesScope internal constructor(vararg targets: ConstrainedLayoutReference) :
    BaseKeyFramesScope(*targets) {
    fun frame(@IntRange(0, 100) frame: Int, keyFrameContent: KeyAttributeScope.() -> Unit) {
        val scope = KeyAttributeScope()
        keyFrameContent(scope)
        framesContainer.add(CLNumber(frame.toFloat()))
        scope.addToContainer(keyFramePropsObject)
    }
}

@ExperimentalMotionApi
class KeyPositionsScope internal constructor(vararg targets: ConstrainedLayoutReference) :
    BaseKeyFramesScope(*targets) {
    var type by addNameOnPropertyChange(RelativePosition.Parent)

    fun frame(@IntRange(0, 100) frame: Int, keyFrameContent: KeyPositionScope.() -> Unit) {
        val scope = KeyPositionScope()
        keyFrameContent(scope)
        framesContainer.add(CLNumber(frame.toFloat()))
        scope.addToContainer(keyFramePropsObject)
    }
}

@ExperimentalMotionApi
class KeyCyclesScope internal constructor(vararg targets: ConstrainedLayoutReference) :
    BaseKeyFramesScope(*targets) {
    fun frame(@IntRange(0, 100) frame: Int, keyFrameContent: KeyCycleScope.() -> Unit) {
        val scope = KeyCycleScope()
        keyFrameContent(scope)
        framesContainer.add(CLNumber(frame.toFloat()))
        scope.addToContainer(keyFramePropsObject)
    }
}

@ExperimentalMotionApi
abstract class BaseKeyFrameScope internal constructor() {
    /**
     * PropertyName-Value map for the properties of each type of key frame.
     *
     * The values are for a singular unspecified frame.
     */
    private val keyFramePropertiesValue = mutableMapOf<String, Any>()

    /**
     * PropertyName-Value map for user-defined values.
     *
     * Typically used on KeyAttributes only.
     */
    internal val customPropertiesValue = mutableMapOf<String, Any>()

    /**
     * When changed, updates the value of type [T] on the [keyFramePropertiesValue] map.
     *
     * Where the Key is the property's name unless [nameOverride] is not null.
     */
    protected fun <T> addOnPropertyChange(initialValue: T, nameOverride: String? = null) =
        object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
                val name = nameOverride ?: property.name
                if (newValue != null) {
                    keyFramePropertiesValue[name] = newValue
                }
            }
        }

    /**
     * Property delegate that updates the [keyFramePropertiesValue] map on value changes.
     *
     * Where the Key is the property's name unless [nameOverride] is not null.
     *
     * The value is the String given by [NamedPropertyOrValue.name].
     *
     * &nbsp;
     *
     * Use when declaring properties that have a named value.
     *
     * E.g.: `var curveFit: CurveFit? by addNameOnPropertyChange(null)`
     */
    protected fun <E : NamedPropertyOrValue?> addNameOnPropertyChange(
        initialValue: E,
        nameOverride: String? = null
    ) =
        object : ObservableProperty<E>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: E, newValue: E) {
                val name = nameOverride ?: property.name
                if (newValue != null) {
                    keyFramePropertiesValue[name] = newValue.name
                }
            }
        }

    /**
     * Adds the property maps to the given container.
     *
     * Where every value is treated as part of array.
     */
    internal fun addToContainer(container: CLContainer) {
        container.putValuesAsArrayElements(keyFramePropertiesValue)
        val customPropsObject = container.getObjectOrNull("custom") ?: run {
            val custom = CLObject(charArrayOf())
            container.put("custom", custom)
            custom
        }
        customPropsObject.putValuesAsArrayElements(customPropertiesValue)
    }

    /**
     * Adds the values from [propertiesSource] to the [CLContainer].
     *
     * Each value will be added as a new element of their corresponding array (given by the Key,
     * which is the name of the affected property).
     */
    private fun CLContainer.putValuesAsArrayElements(propertiesSource: Map<String, Any>) {
        propertiesSource.forEach { (name, value) ->
            val array = this.getArrayOrCreate(name)
            when (value) {
                is String -> {
                    val stringChars = value.toCharArray()
                    array.add(CLString(stringChars).apply {
                        start = 0
                        end = stringChars.size.toLong() - 1
                    })
                }
                is Number -> {
                    array.add(CLNumber(value.toFloat()))
                }
            }
        }
    }
}

@ExperimentalMotionApi
class KeyAttributeScope internal constructor() : BaseKeyFrameScope() {
    var alpha by addOnPropertyChange(1f, "alpha")
    var scaleX by addOnPropertyChange(1f, "scaleX")
    var scaleY by addOnPropertyChange(1f, "scaleY")
    var rotationX by addOnPropertyChange(0f, "rotationX")
    var rotationY by addOnPropertyChange(0f, "rotationY")
    var rotationZ by addOnPropertyChange(0f, "rotationZ")
    var translationX by addOnPropertyChange(0f, "translationX")
    var translationY by addOnPropertyChange(0f, "translationY")
    var translationZ by addOnPropertyChange(0f, "translationZ")
}

@ExperimentalMotionApi
class KeyPositionScope internal constructor() : BaseKeyFrameScope() {
    var percentX by addOnPropertyChange(1f)
    var percentY by addOnPropertyChange(1f)
    var percentWidth by addOnPropertyChange(1f)
    var percentHeight by addOnPropertyChange(0f)
    var curveFit: CurveFit? by addNameOnPropertyChange(null)
}

@ExperimentalMotionApi
class KeyCycleScope internal constructor() : BaseKeyFrameScope() {
    var alpha by addOnPropertyChange(1f)
    var scaleX by addOnPropertyChange(1f)
    var scaleY by addOnPropertyChange(1f)
    var rotationX by addOnPropertyChange(0f)
    var rotationY by addOnPropertyChange(0f)
    var rotationZ by addOnPropertyChange(0f)
    var translationX by addOnPropertyChange(0f)
    var translationY by addOnPropertyChange(0f)
    var translationZ by addOnPropertyChange(0f)
    var period by addOnPropertyChange(0f)
    var offset by addOnPropertyChange(0f)
    var phase by addOnPropertyChange(0f)

    // TODO: Add Wave Shape & Custom Wave
}

internal interface NamedPropertyOrValue {
    val name: String
}

@ExperimentalMotionApi
data class OnSwipe(
    val anchor: ConstrainedLayoutReference,
    val side: SwipeSide,
    val direction: SwipeDirection,
    val dragScale: Float = 1f,
    val dragThreshold: Float = 10f,
    val dragAround: ConstrainedLayoutReference? = null,
    val limitBoundsTo: ConstrainedLayoutReference? = null,
    val onTouchUp: SwipeTouchUp = SwipeTouchUp.AutoComplete,
    val mode: SwipeMode = SwipeMode.Velocity(),
)

@ExperimentalMotionApi
class Easing internal constructor(override val name: String) : NamedPropertyOrValue {
    companion object {
        val Standard = Easing("standard")
        val Accelerate = Easing("accelerate")
        val Decelerate = Easing("decelerate")
        val Linear = Easing("linear")
        val Anticipate = Easing("anticipate")
        val Overshoot = Easing("overshoot")

        /**
         * Defines a Cubic-Bezier curve where the points P1 and P2 are at the given coordinate
         * ratios.
         */
        fun Cubic(
            @FloatRange(from = 0.0, to = 1.0) x1: Float,
            @FloatRange(from = 0.0, to = 1.0) y1: Float,
            @FloatRange(from = 0.0, to = 1.0) x2: Float,
            @FloatRange(from = 0.0, to = 1.0) y2: Float
        ) = Easing("cubic($x1, $y1, $x2, $y2)")
    }
}

@ExperimentalMotionApi
class Arc internal constructor(val name: String) {
    companion object {
        val None = Arc("none")
        val StartVertical = Arc("startVertical")
        val StartHorizontal = Arc("startHorizontal")
        val Flip = Arc("flip")
    }
}

@ExperimentalMotionApi
class SwipeMode internal constructor(
    val name: String,
    internal val springMass: Float = 1f,
    internal val springStiffness: Float = 400f,
    internal val springDamping: Float = 10f,
    internal val springThreshold: Float = 0.01f,
    internal val springBoundary: SpringBoundary = SpringBoundary.Overshoot,
    internal val maxVelocity: Float = 4f,
    internal val maxAcceleration: Float = 1.2f
) {
    companion object {
        val Velocity = Velocity()

        val Spring = Spring()

        fun Velocity(maxVelocity: Float = 4f, maxAcceleration: Float = 1.2f): SwipeMode =
            SwipeMode(
                name = "velocity",
                maxVelocity = maxVelocity,
                maxAcceleration = maxAcceleration
            )

        fun Spring(
            mass: Float = 1f,
            stiffness: Float = 400f,
            damping: Float = 10f,
            threshold: Float = 0.01f,
            boundary: SpringBoundary = SpringBoundary.Overshoot
        ): SwipeMode =
            SwipeMode(
                name = "spring",
                springMass = mass,
                springStiffness = stiffness,
                springDamping = damping,
                springThreshold = threshold,
                springBoundary = boundary
            )
    }
}

@ExperimentalMotionApi
class SwipeTouchUp internal constructor(val name: String) {
    companion object {
        val AutoComplete: SwipeTouchUp = SwipeTouchUp("autocomplete")
        val ToStart: SwipeTouchUp = SwipeTouchUp("toStart")
        val ToEnd: SwipeTouchUp = SwipeTouchUp("toEnd")
        val Stop: SwipeTouchUp = SwipeTouchUp("stop")
        val Decelerate: SwipeTouchUp = SwipeTouchUp("decelerate")
        val NeverCompleteStart: SwipeTouchUp = SwipeTouchUp("neverCompleteStart")
        val NeverCompleteEnd: SwipeTouchUp = SwipeTouchUp("neverCompleteEnd")
    }
}

@ExperimentalMotionApi
class SwipeDirection internal constructor(val name: String) {
    companion object {
        val Up: SwipeDirection = SwipeDirection("up")
        val Down: SwipeDirection = SwipeDirection("down")
        val Left: SwipeDirection = SwipeDirection("left")
        val Right: SwipeDirection = SwipeDirection("right")
        val Start: SwipeDirection = SwipeDirection("start")
        val End: SwipeDirection = SwipeDirection("end")
        val ClockWise: SwipeDirection = SwipeDirection("clockwise")
        val AntiClockWise: SwipeDirection = SwipeDirection("anticlockwise")
    }
}

@ExperimentalMotionApi
class SwipeSide internal constructor(val name: String) {
    companion object {
        val Top: SwipeSide = SwipeSide("top")
        val Left: SwipeSide = SwipeSide("left")
        val Right: SwipeSide = SwipeSide("right")
        val Bottom: SwipeSide = SwipeSide("bottom")
        val Middle: SwipeSide = SwipeSide("middle")
        val Start: SwipeSide = SwipeSide("start")
        val End: SwipeSide = SwipeSide("end")
    }
}

@ExperimentalMotionApi
class SpringBoundary internal constructor(val name: String) {
    companion object {
        val Overshoot = SpringBoundary("overshoot")
        val BounceStart = SpringBoundary("bounceStart")
        val BounceEnd = SpringBoundary("bounceEnd")
        val BounceBoth = SpringBoundary("bounceBoth")
    }
}

@ExperimentalMotionApi
class CurveFit internal constructor(override val name: String) : NamedPropertyOrValue {
    companion object {
        val Spline: CurveFit = CurveFit("spline")
        val Linear: CurveFit = CurveFit("linear")
    }
}

@ExperimentalMotionApi
class RelativePosition internal constructor(override val name: String) : NamedPropertyOrValue {
    companion object {
        val Delta: RelativePosition = RelativePosition("deltaRelative")
        val Path: RelativePosition = RelativePosition("pathRelative")
        val Parent: RelativePosition = RelativePosition("parentRelative")
    }
}