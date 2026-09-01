/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.style

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Interpolatable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp as graphicsLerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as uiLerp

@Suppress("UNCHECKED_CAST")
private fun <T> defaultInterpolate(a: T, b: T, t: Float): T =
    when {
        a is Interpolatable || b is Interpolatable ->
            Interpolatable.lerp(a, b, t) as? T ?: if (t < 0.5) a else b
        a is Dp && b is Dp -> lerp(a, b, t) as T
        a is Float && b is Float -> uiLerp(a, b, t) as T
        a is Int && b is Int -> uiLerp(a, b, t) as T
        a is Long && b is Long -> uiLerp(a, b, t) as T
        a is Color && b is Color -> graphicsLerp(a, b, t) as T
        a is TextUnit && b is TextUnit -> lerp(a, b, t) as T
        else -> if (t < 0.5) a else b
    }

/**
 * Creates a new style property.
 *
 * Defining a style property using [stylePropertyOf] produces a style property that will use a
 * jump-cut halfway through the animation (except if the type implements [Interpolatable] as
 * described below, and a select number of well-known types such as [Dp], [Float], [Int], [Long],
 * [Color] and [TextUnit]). For types that do not support [Interpolatable], it is recommended that a
 * type specific [interpolate] lambda be supplied, even for types like [Dp] that are handled by
 * default, to specify how to interpolate the values of [T] during animation.
 *
 * If the type supports [Interpolatable], such as [Brush], [stylePropertyOf] will use the
 * [Interpolatable] implementation instead of a jump-cut.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f
 * @param defaultValue fallback value provider when the property is not provided
 */
@ExperimentalFoundationStyleApi
public fun <T> stylePropertyOf(
    name: String,
    interpolate: (a: T, b: T, t: Float) -> T = ::defaultInterpolate,
    defaultValue: () -> T,
): ProvidableStyleProperty<T> =
    ProvidableObjectStyleProperty(name, local = false, interpolate, defaultValue = defaultValue)

/**
 * Creates a new local style property.
 *
 * Local style properties inherit their values down the layout tree hierarchy.
 *
 * Defining a local style property using [styleLocalOf] produces a style property that will use a
 * jump-cut halfway through the animation (except if the type implements [Interpolatable] as
 * described below). Use the `interpolate` instead in order to specify the interpolation function to
 * use for an animatable property.
 *
 * If the type supports [Interpolatable], such as [Brush], [styleLocalOf] will use the
 * [Interpolatable] implementation instead of a jump-cut.
 *
 * @param name debug/tooling name for the property
 * @param interpolate interpolate the values `a` and `b` on `t` which is a fraction between 0f and
 *   1f
 * @param defaultValue fallback value provider when the property is not provided
 */
@ExperimentalFoundationStyleApi
public fun <T> styleLocalOf(
    name: String,
    interpolate: (a: T, b: T, t: Float) -> T = ::defaultInterpolate,
    defaultValue: () -> T,
): ProvidableStyleProperty<T> =
    ProvidableObjectStyleProperty(
        name,
        local = true,
        interpolate = interpolate,
        defaultValue = defaultValue,
    )

/** Provides access to resolved style property values. */
@ExperimentalFoundationStyleApi
public interface StylePropertyAccessorScope {
    /** Returns the resolved value of this [StyleProperty], or its default value if not set. */
    public val <T> StyleProperty<T>.value: T

    /** Returns `true` if a value is explicitly set for this [StyleProperty]. */
    public val StyleProperty<*>.isSet: Boolean

    /** Returns the resolved value of [property], or `null` if it is not set. */
    public fun <T> getOrNull(property: StyleProperty<T>): T?

    /** Returns `true` if any property in [properties] is set. */
    public fun anySet(properties: Set<StyleProperty<*>>): Boolean
}

/**
 * Call [block] when [property] has been provided.
 *
 * @param property the property to check.
 * @param block called when [property] has been provided and is called with the resolved value of
 *   [property] as a parameter.
 */
@ExperimentalFoundationStyleApi
public inline fun <T> StylePropertyAccessorScope.ifSet(
    property: StyleProperty<T>,
    crossinline block: (T) -> Unit,
) {
    getOrNull(property)?.let(block)
}

/**
 * Call [block] when [property] has been provided and the value is of type [R].
 *
 * @param R the value of R is required to be.
 * @param T the type of the property inferred from [property]
 * @param property the property to check.
 * @param block called when [property] has been provided and is called with the resolved value of
 *   [property] as a parameter.
 */
@ExperimentalFoundationStyleApi
public inline fun <reified R : T, T> StylePropertyAccessorScope.ifSetAs(
    property: StyleProperty<T>,
    crossinline block: (R) -> Unit,
) {
    (getOrNull(property) as? R)?.let(block)
}

/**
 * Returns the resolved value of [property] if set, or the result of [defaultValue].
 *
 * @param property property to retrieve
 * @param defaultValue fallback calculation when [property] is not set
 */
@ExperimentalFoundationStyleApi
public inline fun <T> StylePropertyAccessorScope.getOrElse(
    property: StyleProperty<T>,
    crossinline defaultValue: () -> T,
): T = if (property.isSet) property.value else defaultValue()

/** Provides scope for declaring or overriding style property values. */
@ExperimentalFoundationStyleApi
public interface StylePropertyProviderScope {
    /** Sets or updates the value of a [ProvidableStyleProperty]. */
    public fun <T> ProvidableStyleProperty<T>.provide(value: T)
}

/**
 * Identifies a style property used in style definitions.
 *
 * @param T type of value held by the property
 * @param name debug/tooling name for the property
 * @param isLocal whether this property inherits down the layout tree
 * @param defaultValue provider for the fallback value when not set
 */
@ExperimentalFoundationStyleApi
public open class StyleProperty<T>(
    public val name: String,
    public val isLocal: Boolean,
    public val defaultValue: () -> T,
) {
    /**
     * Interpolates between value [a] and [b] at fraction [t].
     *
     * @param a starting value at [t] = 0
     * @param b ending value at [t] = 1
     * @param t interpolation fraction, typically between 0 and 1
     * @return interpolated value between [a] and [b]
     */
    public open fun lerp(a: T, b: T, t: Float): T = if (t < 0.5f) a else b

    override fun toString(): String = "$name ${if (isLocal) "local" else "property"}"
}

/**
 * Defines a style property whose value can be provided within a style scope.
 *
 * @param T type of value held by the property
 * @param name optional debug name for the property
 * @param local whether this property inherits down the layout tree
 * @param defaultValue provider for the fallback value when not set
 */
@ExperimentalFoundationStyleApi
public open class ProvidableStyleProperty<T>(name: String, local: Boolean, defaultValue: () -> T) :
    StyleProperty<T>(name, local, defaultValue)

/**
 * Specifies dimension breadths for styling.
 *
 * A [Breadth] is a value that can either be a fraction of the parent or a specific [Dp] value. For
 * example, properties for "width" and "height" are good candidates for [Breadth].
 */
@ExperimentalFoundationStyleApi
@Immutable
public sealed class Breadth : Interpolatable {
    override fun lerp(other: Any?, t: Float): Any? {
        val b = other as? Breadth ?: return null

        // Convert None values into the 0 value of the other's value
        val effectiveA =
            when (this) {
                None ->
                    when (b) {
                        is Distance -> Distance(0.dp)
                        is Fraction -> Fraction(0f)
                        else -> None
                    }
                else -> this
            }
        val effectiveB =
            when (b) {
                None ->
                    when (this) {
                        is Distance -> Distance(0.dp)
                        is Fraction -> Fraction(0f)
                        else -> None
                    }
                else -> b
            }

        return if (effectiveA is Distance && effectiveB is Distance) {
            Distance(uiLerp(effectiveA.value.clamp().value, effectiveB.value.clamp().value, t).dp)
        } else if (effectiveA is Fraction && effectiveB is Fraction) {
            Fraction(uiLerp(effectiveA.value, effectiveB.value, t))
        } else null
    }

    /**
     * Specifies absolute breadth in [Dp].
     *
     * @property value breadth distance
     */
    public class Distance(public val value: Dp) : Breadth() {
        override fun equals(other: Any?): Boolean = other is Distance && other.value == value

        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * Specifies relative breadth as a fraction of available space.
     *
     * @property value breadth fraction, where 1.0 represents 100%
     */
    public class Fraction(public val value: Float) : Breadth() {
        override fun equals(other: Any?): Boolean = other is Fraction && other.value == value

        override fun hashCode(): Int = value.hashCode()
    }

    /** Represents an empty or unspecified breadth. */
    public class None internal constructor() : Breadth() {
        override fun equals(other: Any?): Boolean = other is None

        override fun hashCode(): Int = 1
    }

    public companion object {
        /** Singleton instance representing no breadth. */
        public val None: Breadth = None()
    }
}

/** Create a [Breadth] with a distance value of [value]. */
@ExperimentalFoundationStyleApi public fun Breadth(value: Dp): Breadth = Breadth.Distance(value)

/** Create a [Breadth] with a fraction value of [value]. */
@ExperimentalFoundationStyleApi public fun Breadth(value: Float): Breadth = Breadth.Fraction(value)

private typealias GraphicsColor = Color

private typealias GraphicsBrush = Brush

/**
 * Specifies visual fill for styling.
 *
 * A [Fill] is a value that describes how a region should be filled. "background" or "border color"
 * properties are good candidates for a property of type [Fill].
 */
@ExperimentalFoundationStyleApi
@Immutable
public sealed class Fill : Interpolatable {
    internal abstract val lerpTarget: Any

    /**
     * Return the color of this [Fill] if it is a [Fill.Color] or
     * [Color.Unspecified][androidx.compose.ui.graphics.Color.Unspecified] if [Fill.None] or
     * [Fill.Brush].
     */
    public abstract fun asColor(): GraphicsColor

    /** Return a brush if the [Fill] is a [Fill.Brush], otherwise `null`. */
    public abstract fun asBrush(): GraphicsBrush?

    internal open val isColor: Boolean
        get() = false

    override fun lerp(other: Any?, t: Float): Any? {
        val otherFill = Fill(other) ?: return null
        if (isColor && otherFill.isColor) {
            return Fill(graphicsLerp(asColor(), otherFill.asColor(), t))
        }
        return Fill(Interpolatable.lerp(lerpTarget, otherFill.lerpTarget, t))
    }

    /**
     * Fills with a solid [GraphicsColor].
     *
     * @property color color to fill with
     */
    public class Color(public val color: GraphicsColor) : Fill() {
        override val isColor: Boolean
            get() = true

        override val lerpTarget: Any
            get() = SolidColor(color)

        override fun asColor(): GraphicsColor = color

        override fun asBrush(): GraphicsBrush = SolidColor(color)

        override fun equals(other: Any?): Boolean = other is Fill.Color && other.color == color

        override fun hashCode(): Int = color.hashCode()

        override fun toString(): String = "$color"
    }

    /**
     * Fills with a [GraphicsBrush].
     *
     * @property brush gradient or shader brush to fill with
     */
    public class Brush(public val brush: GraphicsBrush) : Fill() {
        override val lerpTarget: Any
            get() = brush

        override fun asColor(): GraphicsColor =
            when (val brush = brush) {
                is SolidColor -> brush.value
                else -> GraphicsColor.Unspecified
            }

        override fun asBrush(): GraphicsBrush = brush

        override fun equals(other: Any?): Boolean = other is Brush && other.brush == brush

        override fun hashCode(): Int = brush.hashCode()

        override fun toString(): String = "$brush"
    }

    /** Represents an empty fill with no visual representation. */
    @Suppress("CanSealedSubClassBeObject")
    public class None internal constructor() : Fill() {
        override fun asColor(): GraphicsColor = GraphicsColor.Unspecified

        override fun asBrush(): GraphicsBrush? = null

        override val isColor: Boolean
            get() = true

        override val lerpTarget: Any
            get() = SolidColor(GraphicsColor.Transparent)
    }

    @Suppress("unused")
    private class NotExhaustive : Fill() {
        override val lerpTarget: Any
            get() = this

        override fun asBrush(): GraphicsBrush? = null

        override fun asColor(): GraphicsColor = GraphicsColor.Unspecified

        override fun equals(other: Any?): Boolean = this === other

        override fun hashCode(): Int = this::class.hashCode()
    }

    public companion object {
        /** Singleton instance representing no fill. */
        public val None: Fill = None()
    }
}

/**
 * Creates a solid color [Fill].
 *
 * @param color color to fill with
 */
@ExperimentalFoundationStyleApi @Stable public fun Fill(color: Color): Fill = Fill.Color(color)

/**
 * Creates a brush [Fill].
 *
 * @param brush gradient or shader brush to fill with
 */
@ExperimentalFoundationStyleApi @Stable public fun Fill(brush: Brush): Fill = Fill.Brush(brush)

/**
 * Converts [value] to a [Fill] if it is a supported fill type. Returns `null` otherwise.
 *
 * @param value object to convert to a [Fill]
 */
@ExperimentalFoundationStyleApi
@Stable
public fun Fill(value: Any?): Fill? =
    when (value) {
        is Fill -> value
        is Color -> Fill(value)
        is Brush -> Fill(value)
        else -> null
    }

/** Specifies shadow configurations for styling. */
@ExperimentalFoundationStyleApi
@Immutable
public sealed class Shadows : Interpolatable {
    internal open val lerpTarget: Any?
        get() = null

    override fun lerp(other: Any?, t: Float): Any? {
        val otherShadows = other as? Shadows ?: return if (t < 0.5f) this else other
        return Interpolatable.lerp(lerpTarget, otherShadows.lerpTarget, t)?.let { Shadows(it) }
            ?: if (t < 0.5f) this else other
    }

    /**
     * Configures a single [Shadow].
     *
     * @property shadow shadow details
     */
    public class Simple(public val shadow: Shadow) : Shadows() {
        override val lerpTarget = shadow

        override fun hashCode(): Int {
            var result = shadow.hashCode()
            result = 31 * result + lerpTarget.hashCode()
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Simple

            if (shadow != other.shadow) return false
            if (lerpTarget != other.lerpTarget) return false

            return true
        }
    }

    /**
     * Configures multiple stacked [Shadow]s.
     *
     * @property shadows array of shadow details
     */
    public class Compound(public val shadows: Array<out Shadow>) : Shadows() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Compound

            if (!shadows.contentEquals(other.shadows)) return false

            return true
        }

        override fun hashCode(): Int = shadows.contentHashCode()
    }

    /** Represents an empty shadow configuration. */
    internal class None internal constructor() : Shadows() {
        override fun equals(other: Any?): Boolean = other === this

        override fun hashCode(): Int = this::class.hashCode()
    }

    public companion object {
        /** Singleton instance representing no shadow. */
        public val None: Shadows = None()
    }
}

/**
 * Creates a single [Shadows] configuration.
 *
 * @param shadow shadow details
 */
@ExperimentalFoundationStyleApi
@Stable
public fun Shadows(shadow: Shadow): Shadows = Shadows.Simple(shadow)

/**
 * Creates a compound [Shadows] configuration from multiple shadows.
 *
 * @param shadows list of shadow details
 */
@ExperimentalFoundationStyleApi
@Stable
public fun Shadows(vararg shadows: Shadow): Shadows = Shadows.Compound(shadows)

/**
 * Converts [value] to [Shadows] if it is a valid shadow representation.
 *
 * @param value object to convert to [Shadows]
 */
@ExperimentalFoundationStyleApi
@Stable
public fun Shadows(value: Any?): Shadows? =
    @Suppress("UNCHECKED_CAST")
    when (value) {
        is Shadow -> Shadows(value)
        is Array<*> -> Shadows(*(value as Array<out Shadow>))
        null -> Shadows.None
        else -> null
    }

@ExperimentalFoundationStyleApi
internal class ProvidableObjectStyleProperty<T>(
    name: String,
    local: Boolean,
    val interpolate: (a: T, b: T, t: Float) -> T,
    defaultValue: () -> T,
) : ProvidableStyleProperty<T>(name, local, defaultValue) {
    override fun lerp(a: T, b: T, t: Float): T = interpolate(a, b, t)
}

private fun Dp.clamp() = if (isUnspecified || this < 0.dp) 0.dp else this

/** A [lerp] function that allows animating a [Dp] type. */
public fun lerp(a: Dp, b: Dp, t: Float): Dp = uiLerp(a.clamp().value, b.clamp().value, t).dp

/** A [lerp] function that allows animating a [TextUnit] type. */
public fun lerp(a: TextUnit, b: TextUnit, t: Float): TextUnit =
    when {
        a.isSp && b.isSp -> uiLerp(a.value, b.value, t).sp
        a.isEm && b.isEm -> uiLerp(a.value, b.value, t).em
        else -> if (t < 0.5f) a else b
    }
