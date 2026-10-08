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
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.core.parser.CLArray
import androidx.constraintlayout.core.parser.CLContainer
import androidx.constraintlayout.core.parser.CLNumber
import androidx.constraintlayout.core.parser.CLObject
import androidx.constraintlayout.core.parser.CLString
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * Defines the interpolation parameters between the [ConstraintSet]s to achieve fine-tuned
 * animations.
 *
 * @param from The name of the initial [ConstraintSet]. Should correspond to a named [ConstraintSet]
 *   when added as part of a [MotionScene] with [MotionSceneScope.addTransition].
 * @param to The name of the target [ConstraintSet]. Should correspond to a named [ConstraintSet]
 *   when added as part of a [MotionScene] with [MotionSceneScope.addTransition].
 * @param content Lambda to define the Transition parameters on the given [TransitionScope].
 */
@ExperimentalMotionApi
public fun Transition(
    from: String = "start",
    to: String = "end",
    content: TransitionScope.() -> Unit,
): Transition {
    val transitionScope = TransitionScope(from, to)
    transitionScope.content()
    return TransitionImpl(transitionScope.getObject())
}

/**
 * Scope where [Transition] parameters are defined.
 *
 * Here, you may define multiple KeyFrames for specific [ConstrainedLayoutReference]s, as well was
 * enabling [OnSwipe] handling.
 *
 * @see keyAttributes
 * @see keyPositions
 * @see keyCycles
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class TransitionScope
internal constructor(private val from: String, private val to: String) {
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

    /**
     * The default [Arc] shape for animated layout movement.
     *
     * [Arc.None] by default.
     */
    public var motionArc: Arc = Arc.None

    /**
     * When not null, enables animating through the transition with touch input.
     *
     * Example:
     * ```
     *  MotionLayout(
     *      motionScene = MotionScene {
     *          val textRef = createRefFor("text")
     *          defaultTransition(
     *              from = constraintSet {
     *                  constrain(textRef) {
     *                      top.linkTo(parent.top)
     *                  }
     *              },
     *              to = constraintSet {
     *                  constrain(textRef) {
     *                      bottom.linkTo(parent.bottom)
     *                  }
     *              }
     *          ) {
     *              onSwipe = OnSwipe(
     *                  anchor = textRef,
     *                  side = SwipeSide.Middle,
     *                  direction = SwipeDirection.Down
     *              )
     *          }
     *      },
     *      progress = 0f, // OnSwipe handles the progress, so this should be constant to avoid conflict
     *      modifier = Modifier.fillMaxSize()
     *  ) {
     *      Text("Hello, World!", Modifier.layoutId("text"))
     *  }
     * ```
     *
     * @see OnSwipe
     */
    public var onSwipe: OnSwipe? = null

    /**
     * Defines the maximum delay (in progress value) between a group of staggered widgets.
     *
     * The amount of delay for each widget is decided based on its weight. Where the widget with the
     * lowest weight will receive the full delay. A negative [maxStaggerDelay] value inverts this
     * logic, so that the widget with the highest weight will receive the full delay.
     *
     * By default, the weight of each widget is calculated as the Manhattan Distance from the
     * top-left corner of the layout. You may set custom weights using
     * [MotionSceneScope.staggeredWeight] on a per-widget basis, this essentially allows you to set
     * a custom staggering order. Note that when you set custom weights, widgets without a custom
     * weight will be ignored for this calculation and will animate without delay.
     *
     * The remaining widgets will receive a portion of this delay, based on their weight calculated
     * against each other.
     *
     * This is the formula to calculate the progress delay for a widget **i**, where
     * **Max/MinWeight** is defined by the maximum and minimum calculated (or custom) weight:
     * ```
     * progressDelay[i] = maxStaggerDelay * (1 - ((weight[i] - MinWeight) / (MaxWeight - MinWeight)))
     * ```
     *
     * To simplify, this is the formula normalized against **MinWeight**:
     * ```
     * progressDelay[i] = maxStaggerDelay * (1 - weight[i] / MaxWeight)
     * ```
     *
     * Example:
     *
     * Given three widgets with custom weights `[1, 2, 3]` and [maxStaggerDelay] = 0.7f.
     * - Widget0 will start animating at `progress == 0.7f` for having the lowest weight.
     * - Widget1 will start animating at `progress == 0.35f`
     * - Widget2 will start animating at `progress == 0.0f`
     *
     * This is because the weights are distributed linearly among the widgets.
     */
    @FloatRange(-1.0, 1.0, fromInclusive = false, toInclusive = false)
    public var maxStaggerDelay: Float = 0.0f

    /**
     * Define KeyAttribute KeyFrames for the given [targets].
     *
     * Set multiple KeyFrames with [KeyAttributesScope.frame].
     */
    public fun keyAttributes(
        vararg targets: ConstrainedLayoutReference,
        keyAttributesContent: KeyAttributesScope.() -> Unit,
    ) {
        val scope = KeyAttributesScope(*targets)
        keyAttributesContent(scope)
        addKeyAttributesIfMissing()
        keyAttributesArray.add(scope.keyFramePropsObject)
    }

    /**
     * Define KeyPosition KeyFrames for the given [targets].
     *
     * Set multiple KeyFrames with [KeyPositionsScope.frame].
     */
    public fun keyPositions(
        vararg targets: ConstrainedLayoutReference,
        keyPositionsContent: KeyPositionsScope.() -> Unit,
    ) {
        val scope = KeyPositionsScope(*targets)
        keyPositionsContent(scope)
        addKeyPositionsIfMissing()
        keyPositionsArray.add(scope.keyFramePropsObject)
    }

    /**
     * Define KeyCycle KeyFrames for the given [targets].
     *
     * Set multiple KeyFrames with [KeyCyclesScope.frame].
     */
    public fun keyCycles(
        vararg targets: ConstrainedLayoutReference,
        keyCyclesContent: KeyCyclesScope.() -> Unit,
    ) {
        val scope = KeyCyclesScope(*targets)
        keyCyclesContent(scope)
        addKeyCyclesIfMissing()
        keyCyclesArray.add(scope.keyFramePropsObject)
    }

    /**
     * Creates one [ConstrainedLayoutReference] corresponding to the [ConstraintLayout] element with
     * [id].
     */
    public fun createRefFor(id: Any): ConstrainedLayoutReference = ConstrainedLayoutReference(id)

    internal fun getObject(): CLObject {
        containerObject.putString("pathMotionArc", motionArc.name)
        containerObject.putString("from", from)
        containerObject.putString("to", to)
        // TODO: Uncomment once we decide how to deal with Easing discrepancy from user driven
        //  `progress` value. Eg: `animateFloat(tween(duration, LinearEasing))`
        //        containerObject.putString("interpolator", easing.name)
        //        containerObject.putNumber("duration", durationMs.toFloat())
        containerObject.putNumber("staggered", maxStaggerDelay)
        onSwipe?.let {
            containerObject.put("onSwipe", onSwipeObject)
            onSwipeObject.putString("direction", it.direction.name)
            onSwipeObject.putNumber("scale", it.dragScale)
            it.dragAround?.id?.let { id -> onSwipeObject.putString("around", id.toString()) }
            it.limitBoundsTo?.id?.let { id ->
                onSwipeObject.putString("limitBounds", id.toString())
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

/**
 * The base/common scope for KeyFrames.
 *
 * Each KeyFrame may have multiple frames and multiple properties for each frame. The frame values
 * should be registered on [framesContainer] and the corresponding properties changes on
 * [keyFramePropsObject].
 */
@ExperimentalMotionApi
public sealed class BaseKeyFramesScope(vararg targets: ConstrainedLayoutReference) {
    internal val keyFramePropsObject = CLObject(charArrayOf()).apply { clear() }

    private val targetsContainer = CLArray(charArrayOf())
    internal val framesContainer = CLArray(charArrayOf())

    /** The [Easing] curve to apply for the KeyFrames defined in this scope. */
    public var easing: Easing by addNameOnPropertyChange(Easing.Standard, "transitionEasing")

    init {
        keyFramePropsObject.put("target", targetsContainer)
        keyFramePropsObject.put("frames", framesContainer)
        targets.forEach {
            val targetChars = it.id.toString().toCharArray()
            targetsContainer.add(
                CLString(targetChars).apply {
                    start = 0
                    end = targetChars.size.toLong() - 1
                }
            )
        }
    }

    /**
     * Registers changes of this property to [keyFramePropsObject]. Where the key is the name of the
     * property. Use [nameOverride] to apply a different key.
     */
    internal fun <E : NamedPropertyOrValue?> addNameOnPropertyChange(
        initialValue: E,
        nameOverride: String? = null,
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

/**
 * Fake private implementation of [BaseKeyFramesScope] to prevent exhaustive `when` usages of
 * [BaseKeyFramesScope], while `sealed` prevents undesired inheritance of [BaseKeyFramesScope].
 */
@OptIn(ExperimentalMotionApi::class) private class FakeKeyFramesScope : BaseKeyFramesScope()

/**
 * Scope where multiple attribute KeyFrames may be defined.
 *
 * @see frame
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class KeyAttributesScope internal constructor(vararg targets: ConstrainedLayoutReference) :
    BaseKeyFramesScope(*targets) {

    /**
     * Define KeyAttribute values at a given KeyFrame, where the [frame] is a specific progress
     * value from 0 to 100.
     *
     * All properties set on [KeyAttributeScope] for this [frame] should also be set on other
     * [frame] declarations made within this scope.
     */
    public fun frame(@IntRange(0, 100) frame: Int, keyFrameContent: KeyAttributeScope.() -> Unit) {
        val scope = KeyAttributeScope()
        keyFrameContent(scope)
        framesContainer.add(CLNumber(frame.toFloat()))
        scope.addToContainer(keyFramePropsObject)
    }
}

/**
 * Scope where multiple position KeyFrames may be defined.
 *
 * @see frame
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class KeyPositionsScope internal constructor(vararg targets: ConstrainedLayoutReference) :
    BaseKeyFramesScope(*targets) {
    /**
     * Sets the coordinate space in which KeyPositions are defined.
     *
     * [RelativePosition.Delta] by default.
     */
    public var type: RelativePosition by addNameOnPropertyChange(RelativePosition.Delta)

    /**
     * Define KeyPosition values at a given KeyFrame, where the [frame] is a specific progress value
     * from 0 to 100.
     *
     * All properties set on [KeyPositionScope] for this [frame] should also be set on other [frame]
     * declarations made within this scope.
     */
    public fun frame(@IntRange(0, 100) frame: Int, keyFrameContent: KeyPositionScope.() -> Unit) {
        val scope = KeyPositionScope()
        keyFrameContent(scope)
        framesContainer.add(CLNumber(frame.toFloat()))
        scope.addToContainer(keyFramePropsObject)
    }
}

/**
 * Scope where multiple cycling attribute KeyFrames may be defined.
 *
 * @see frame
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class KeyCyclesScope internal constructor(vararg targets: ConstrainedLayoutReference) :
    BaseKeyFramesScope(*targets) {

    /**
     * Define KeyCycle values at a given KeyFrame, where the [frame] is a specific progress value
     * from 0 to 100.
     *
     * All properties set on [KeyCycleScope] for this [frame] should also be set on other [frame]
     * declarations made within this scope.
     */
    public fun frame(@IntRange(0, 100) frame: Int, keyFrameContent: KeyCycleScope.() -> Unit) {
        val scope = KeyCycleScope()
        keyFrameContent(scope)
        framesContainer.add(CLNumber(frame.toFloat()))
        scope.addToContainer(keyFramePropsObject)
    }
}

/**
 * The base/common scope for individual KeyFrame declarations.
 *
 * Properties should be registered on [keyFramePropertiesValue], however, custom properties must use
 * [customPropertiesValue].
 */
@ExperimentalMotionApi
public sealed class BaseKeyFrameScope {
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
    protected fun <T> addOnPropertyChange(
        initialValue: T,
        nameOverride: String? = null,
    ): ObservableProperty<T> =
        object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
                if (newValue != null) {
                    keyFramePropertiesValue[nameOverride ?: property.name] = newValue
                } else {
                    keyFramePropertiesValue.remove(nameOverride ?: property.name)
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
     * Use when declaring properties that have a named value.
     *
     * E.g.: `var curveFit: CurveFit? by addNameOnPropertyChange(null)`
     */
    protected fun <E : NamedPropertyOrValue?> addNameOnPropertyChange(
        initialValue: E,
        nameOverride: String? = null,
    ): ObservableProperty<E> =
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
        val customPropsObject =
            container.getObjectOrNull("custom")
                ?: run {
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
                    array.add(
                        CLString(stringChars).apply {
                            start = 0
                            end = stringChars.size.toLong() - 1
                        }
                    )
                }
                is Dp -> {
                    array.add(CLNumber(value.value))
                }
                is Number -> {
                    array.add(CLNumber(value.toFloat()))
                }
            }
        }
    }
}

/**
 * Fake private implementation of [BaseKeyFrameScope] to prevent exhaustive `when` usages of
 * [BaseKeyFrameScope], while `sealed` prevents undesired inheritance of [BaseKeyFrameScope].
 */
@OptIn(ExperimentalMotionApi::class) private class FakeKeyFrameScope : BaseKeyFrameScope()

/**
 * Scope to define KeyFrame attributes.
 *
 * Supports transform parameters: alpha, scale, rotation and translation.
 *
 * You may also define custom properties when called within a [MotionSceneScope].
 *
 * @see [MotionSceneScope.customFloat]
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class KeyAttributeScope internal constructor() : BaseKeyFrameScope() {
    public var alpha: Float by addOnPropertyChange(1f, "alpha")
    public var scaleX: Float by addOnPropertyChange(1f, "scaleX")
    public var scaleY: Float by addOnPropertyChange(1f, "scaleY")
    public var rotationX: Float by addOnPropertyChange(0f, "rotationX")
    public var rotationY: Float by addOnPropertyChange(0f, "rotationY")
    public var rotationZ: Float by addOnPropertyChange(0f, "rotationZ")
    public var translationX: Dp by addOnPropertyChange(0.dp, "translationX")
    public var translationY: Dp by addOnPropertyChange(0.dp, "translationY")
    public var translationZ: Dp by addOnPropertyChange(0.dp, "translationZ")
}

/**
 * Scope to define KeyFrame positions.
 *
 * These are modifications on the widget's position and size relative to its final state on the
 * current transition.
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class KeyPositionScope internal constructor() : BaseKeyFrameScope() {
    /**
     * The position as a percentage of the X axis of the current coordinate space.
     *
     * Where 0 is the position at the **start** [ConstraintSet] and 1 is at the **end**
     * [ConstraintSet].
     *
     * The coordinate space is defined by [KeyPositionsScope.type].
     */
    public var percentX: Float by addOnPropertyChange(1f)

    /**
     * The position as a percentage of the Y axis of the current coordinate space.
     *
     * Where 0 is the position at the **start** [ConstraintSet] and 1 is at the **end**
     * [ConstraintSet].
     *
     * The coordinate space is defined by [KeyPositionsScope.type].
     */
    public var percentY: Float by addOnPropertyChange(1f)

    /** The width as a percentage of the width at the end [ConstraintSet]. */
    public var percentWidth: Float by addOnPropertyChange(1f)

    /** The height as a percentage of the height at the end [ConstraintSet]. */
    public var percentHeight: Float by addOnPropertyChange(0f)

    /** Type of fit applied to the curve. [CurveFit.Spline] by default. */
    public var curveFit: CurveFit? by addNameOnPropertyChange(null)
}

/**
 * Scope to define cycling KeyFrames.
 *
 * [KeyCycleScope] allows you to apply wave-based transforms, defined by [period], [offset] and
 * [phase]. A sinusoidal wave is used by default.
 */
@ExperimentalMotionApi
@LayoutScopeMarker
public class KeyCycleScope internal constructor() : BaseKeyFrameScope() {
    public var alpha: Float by addOnPropertyChange(1f)
    public var scaleX: Float by addOnPropertyChange(1f)
    public var scaleY: Float by addOnPropertyChange(1f)
    public var rotationX: Float by addOnPropertyChange(0f)
    public var rotationY: Float by addOnPropertyChange(0f)
    public var rotationZ: Float by addOnPropertyChange(0f)
    public var translationX: Dp by addOnPropertyChange(0.dp)
    public var translationY: Dp by addOnPropertyChange(0.dp)
    public var translationZ: Dp by addOnPropertyChange(0.dp)
    public var period: Float by addOnPropertyChange(0f)
    public var offset: Float by addOnPropertyChange(0f)
    public var phase: Float by addOnPropertyChange(0f)

    // TODO: Add Wave Shape & Custom Wave
}

@ExperimentalMotionApi
public interface NamedPropertyOrValue {
    public val name: String
}

/**
 * Defines the OnSwipe behavior for a [Transition].
 *
 * When swiping, the [MotionLayout] is updated to a progress value so that the given
 * [ConstrainedLayoutReference] is laid out in a position corresponding to the drag.
 *
 * In other words, [OnSwipe] allows you to drive [MotionLayout] by dragging a specific
 * [ConstrainedLayoutReference].
 *
 * @param anchor The [ConstrainedLayoutReference] to track through touch input.
 * @param side Side of the bounds to track, this is to account for when the tracked widget changes
 *   size during the [Transition].
 * @param direction Expected swipe direction to start the animation through touch handling.
 *   Typically, this is the direction the widget takes to the end [ConstraintSet].
 * @param dragScale Scaling factor applied on the dragged distance, meaning that the larger the
 *   scaling value, the shorter distance is required to animate the entire Transition. 1f by
 *   default.
 * @param dragThreshold Distance in pixels required to consider the drag as initiated. 10 by
 *   default.
 * @param dragAround When not-null, causes the [anchor] to be dragged around the center of the given
 *   [ConstrainedLayoutReference] in a circular motion.
 * @param limitBoundsTo When not-null, the touch handling won't be initiated unless it's within the
 *   bounds of the given [ConstrainedLayoutReference]. Useful to deal with touch handling conflicts.
 * @param onTouchUp Defines what behavior MotionLayout should have when the drag event is
 *   interrupted by TouchUp. [SwipeTouchUp.AutoComplete] by default.
 * @param mode Describes how MotionLayout animates during [onTouchUp]. [SwipeMode.velocity] by
 *   default.
 */
@ExperimentalMotionApi
public class OnSwipe(
    public val anchor: ConstrainedLayoutReference,
    public val side: SwipeSide,
    public val direction: SwipeDirection,
    public val dragScale: Float = 1f,
    public val dragThreshold: Float = 10f,
    public val dragAround: ConstrainedLayoutReference? = null,
    public val limitBoundsTo: ConstrainedLayoutReference? = null,
    public val onTouchUp: SwipeTouchUp = SwipeTouchUp.AutoComplete,
    public val mode: SwipeMode = SwipeMode.velocity(),
)

/**
 * Supported Easing curves.
 *
 * You may define your own Cubic-bezier easing curve with [cubic].
 */
@ExperimentalMotionApi
public class Easing internal constructor(override val name: String) : NamedPropertyOrValue {
    public companion object {
        /**
         * Standard [Easing] curve, also known as: Ease in, ease out.
         *
         * Defined as `cubic(0.4f, 0.0f, 0.2f, 1f)`.
         */
        public val Standard: Easing = Easing("standard")

        /**
         * Acceleration [Easing] curve, also known as: Ease in.
         *
         * Defined as `cubic(0.4f, 0.05f, 0.8f, 0.7f)`.
         */
        public val Accelerate: Easing = Easing("accelerate")

        /**
         * Deceleration [Easing] curve, also known as: Ease out.
         *
         * Defined as `cubic(0.0f, 0.0f, 0.2f, 0.95f)`.
         */
        public val Decelerate: Easing = Easing("decelerate")

        /**
         * Linear [Easing] curve.
         *
         * Defined as `cubic(1f, 1f, 0f, 0f)`.
         */
        public val Linear: Easing = Easing("linear")

        /**
         * Anticipate is an [Easing] curve with a small negative overshoot near the start of the
         * motion.
         *
         * Defined as `cubic(0.36f, 0f, 0.66f, -0.56f)`.
         */
        public val Anticipate: Easing = Easing("anticipate")

        /**
         * Overshoot is an [Easing] curve with a small positive overshoot near the end of the
         * motion.
         *
         * Defined as `cubic(0.34f, 1.56f, 0.64f, 1f)`.
         */
        public val Overshoot: Easing = Easing("overshoot")

        /**
         * Defines a Cubic-Bezier curve where the points P1 and P2 are at the given coordinate
         * ratios.
         *
         * P1 and P2 are typically defined within (0f, 0f) and (1f, 1f), but may be assigned beyond
         * these values for overshoot curves.
         *
         * @param x1 X-axis value for P1. Value is typically defined within 0f-1f.
         * @param y1 Y-axis value for P1. Value is typically defined within 0f-1f.
         * @param x2 X-axis value for P2. Value is typically defined within 0f-1f.
         * @param y2 Y-axis value for P2. Value is typically defined within 0f-1f.
         */
        public fun cubic(x1: Float, y1: Float, x2: Float, y2: Float): Easing =
            Easing("cubic($x1, $y1, $x2, $y2)")
    }
}

/** Determines a specific arc direction of the widget's path on a [Transition]. */
@ExperimentalMotionApi
public class Arc internal constructor(public val name: String) {
    public companion object {
        public val None: Arc = Arc("none")
        public val StartVertical: Arc = Arc("startVertical")
        public val StartHorizontal: Arc = Arc("startHorizontal")
        public val Flip: Arc = Arc("flip")
        public val Below: Arc = Arc("below")
        public val Above: Arc = Arc("above")
    }
}

/**
 * Defines the type of motion used when animating during touch-up.
 *
 * @see velocity
 * @see spring
 */
@ExperimentalMotionApi
public class SwipeMode
internal constructor(
    public val name: String,
    internal val springMass: Float = 1f,
    internal val springStiffness: Float = 400f,
    internal val springDamping: Float = 10f,
    internal val springThreshold: Float = 0.01f,
    internal val springBoundary: SpringBoundary = SpringBoundary.Overshoot,
    internal val maxVelocity: Float = 4f,
    internal val maxAcceleration: Float = 1.2f,
) {
    public companion object {
        /**
         * The default Velocity based mode.
         *
         * Defined as `velocity(maxVelocity = 4f, maxAcceleration = 1.2f)`.
         *
         * @see velocity
         */
        public val Velocity: SwipeMode = velocity()

        /**
         * The default Spring based mode.
         *
         * Defined as `spring(mass = 1f, stiffness = 400f, damping = 10f, threshold = 0.01f,
         * boundary = SpringBoundary.Overshoot)`.
         *
         * @see spring
         */
        public val Spring: SwipeMode = spring()

        /**
         * Velocity based behavior during touch up for [OnSwipe].
         *
         * @param maxVelocity Maximum velocity in pixels/milliSecond
         * @param maxAcceleration Maximum acceleration in pixels/milliSecond^2
         */
        public fun velocity(maxVelocity: Float = 4f, maxAcceleration: Float = 1.2f): SwipeMode =
            SwipeMode(
                name = "velocity",
                maxVelocity = maxVelocity,
                maxAcceleration = maxAcceleration,
            )

        /**
         * Defines a spring based behavior during touch up for [OnSwipe].
         *
         * @param mass Mass of the spring, mostly affects the momentum that the spring carries. A
         *   spring with a larger mass will overshoot more and take longer to settle.
         * @param stiffness Stiffness of the spring, mostly affects the acceleration at the start of
         *   the motion. A spring with higher stiffness will move faster when pulled at a constant
         *   distance.
         * @param damping The rate at which the spring settles on its final position. A spring with
         *   larger damping value will settle faster on its final position.
         * @param threshold Distance in meters from the target point at which the bouncing motion of
         *   the spring is to be considered finished. 0.01 (1cm) by default. This value is typically
         *   small since the widget will jump to the final position once the spring motion ends, a
         *   large threshold value might cause the motion to end noticeably far from the target
         *   point.
         * @param boundary Behavior of the spring bouncing motion as it crosses its target position.
         *   [SpringBoundary.Overshoot] by default.
         */
        public fun spring(
            mass: Float = 1f,
            stiffness: Float = 400f,
            damping: Float = 10f,
            threshold: Float = 0.01f,
            boundary: SpringBoundary = SpringBoundary.Overshoot,
        ): SwipeMode =
            SwipeMode(
                name = "spring",
                springMass = mass,
                springStiffness = stiffness,
                springDamping = damping,
                springThreshold = threshold,
                springBoundary = boundary,
            )
    }
}

/**
 * The logic used to decide the target position when the touch input ends.
 *
 * The possible target positions are the positions defined by the **start** and **end**
 * [ConstraintSet]s.
 *
 * To define the type of motion used while animating during touch up, see [SwipeMode] for
 * [OnSwipe.mode].
 */
@ExperimentalMotionApi
public class SwipeTouchUp internal constructor(public val name: String) {
    public companion object {
        /**
         * The widget will be automatically animated towards the [ConstraintSet] closest to where
         * the swipe motion is predicted to end.
         */
        public val AutoComplete: SwipeTouchUp = SwipeTouchUp("autocomplete")

        /**
         * Automatically animates towards the **start** [ConstraintSet] unless it's already exactly
         * at the **end** [ConstraintSet].
         *
         * @see NeverCompleteEnd
         */
        public val ToStart: SwipeTouchUp = SwipeTouchUp("toStart")

        /**
         * Automatically animates towards the **end** [ConstraintSet] unless it's already exactly at
         * the **start** [ConstraintSet].
         *
         * @see NeverCompleteStart
         */
        public val ToEnd: SwipeTouchUp = SwipeTouchUp("toEnd")

        /** Stops right in place, will **not** automatically animate to any [ConstraintSet]. */
        public val Stop: SwipeTouchUp = SwipeTouchUp("stop")

        /**
         * Automatically animates towards the point where the swipe motion is predicted to end.
         *
         * This is guaranteed to stop within the start or end [ConstraintSet]s in the case where
         * it's carrying a lot of speed.
         */
        public val Decelerate: SwipeTouchUp = SwipeTouchUp("decelerate")

        /**
         * Similar to [ToEnd], but it will animate to the **end** [ConstraintSet] even if the widget
         * is exactly at the start [ConstraintSet].
         */
        public val NeverCompleteStart: SwipeTouchUp = SwipeTouchUp("neverCompleteStart")

        /**
         * Similar to [ToStart], but it will animate to the **start** [ConstraintSet] even if the
         * widget is exactly at the end [ConstraintSet].
         */
        public val NeverCompleteEnd: SwipeTouchUp = SwipeTouchUp("neverCompleteEnd")
    }
}

/** Direction of the touch input that will initiate the swipe handling. */
@ExperimentalMotionApi
public class SwipeDirection internal constructor(public val name: String) {
    public companion object {
        public val Up: SwipeDirection = SwipeDirection("up")
        public val Down: SwipeDirection = SwipeDirection("down")
        public val Left: SwipeDirection = SwipeDirection("left")
        public val Right: SwipeDirection = SwipeDirection("right")
        public val Start: SwipeDirection = SwipeDirection("start")
        public val End: SwipeDirection = SwipeDirection("end")
        public val Clockwise: SwipeDirection = SwipeDirection("clockwise")
        public val Counterclockwise: SwipeDirection = SwipeDirection("anticlockwise")
    }
}

/**
 * Side of the bounds to track during touch handling, this is to account for when the widget changes
 * size during the [Transition].
 */
@ExperimentalMotionApi
public class SwipeSide internal constructor(public val name: String) {
    public companion object {
        public val Top: SwipeSide = SwipeSide("top")
        public val Left: SwipeSide = SwipeSide("left")
        public val Right: SwipeSide = SwipeSide("right")
        public val Bottom: SwipeSide = SwipeSide("bottom")
        public val Middle: SwipeSide = SwipeSide("middle")
        public val Start: SwipeSide = SwipeSide("start")
        public val End: SwipeSide = SwipeSide("end")
    }
}

/**
 * Behavior of the spring as it crosses its target position. The target position may be the start or
 * end of the [Transition].
 */
@ExperimentalMotionApi
public class SpringBoundary internal constructor(public val name: String) {
    public companion object {
        /** The default Spring behavior, it will overshoot around the target position. */
        public val Overshoot: SpringBoundary = SpringBoundary("overshoot")

        /**
         * Bouncing motion when the target position is at the start of the [Transition]. Otherwise,
         * it will overshoot.
         */
        public val BounceStart: SpringBoundary = SpringBoundary("bounceStart")

        /**
         * Bouncing motion when the target position is at the end of the [Transition]. Otherwise, it
         * will overshoot.
         */
        public val BounceEnd: SpringBoundary = SpringBoundary("bounceEnd")

        /**
         * Bouncing motion whenever it crosses the target position. This basically guarantees that
         * the spring motion will never overshoot.
         */
        public val BounceBoth: SpringBoundary = SpringBoundary("bounceBoth")
    }
}

/** Type of fit applied between curves. */
@ExperimentalMotionApi
public class CurveFit internal constructor(override val name: String) : NamedPropertyOrValue {
    public companion object {
        public val Spline: CurveFit = CurveFit("spline")
        public val Linear: CurveFit = CurveFit("linear")
    }
}

/** Relative coordinate space in which KeyPositions are applied. */
@ExperimentalMotionApi
public class RelativePosition internal constructor(override val name: String) :
    NamedPropertyOrValue {
    public companion object {
        /**
         * The default coordinate space, defined between the ending and starting point of the
         * motion. Aligned to the layout's X and Y axis.
         */
        public val Delta: RelativePosition = RelativePosition("deltaRelative")

        /**
         * The coordinate space defined between the ending and starting point of the motion. Aligned
         * perpendicularly to the shortest line between the start/end.
         */
        public val Path: RelativePosition = RelativePosition("pathRelative")

        /**
         * The coordinate space defined within the parent layout bounds (the MotionLayout parent).
         */
        public val Parent: RelativePosition = RelativePosition("parentRelative")
    }
}
