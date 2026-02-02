/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.wear.tiles.curved

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.glance.action.Action
import androidx.glance.semantics.SemanticsConfiguration
import androidx.glance.semantics.SemanticsPropertyReceiver

@JvmDefaultWithCompatibility
/**
 * An ordered, immutable, collection of modifier element that works with curved components in the
 * Glance library.
 *
 * This plays the same role as [androidx.compose.ui.Modifier], but for the glance curved composable
 */
@Stable
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public interface GlanceCurvedModifier {
    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from outside in.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldIn] may be used to accumulate a value starting from the
     * parent or head of the modifier chain to the final wrapped child.
     */
    public fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from inside out.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldOut] may be used to accumulate a value starting from the
     * child or tail of the modifier chain up to the parent or head of the chain.
     */
    public fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    /**
     * Returns `true` if [predicate] returns true for any [Element] in this [GlanceCurvedModifier].
     */
    public fun any(predicate: (Element) -> Boolean): Boolean

    /**
     * Returns `true` if [predicate] returns true for all [Element]s in this [GlanceCurvedModifier]
     * or if this [GlanceCurvedModifier] contains no [Element]s.
     */
    public fun all(predicate: (Element) -> Boolean): Boolean

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [GlanceCurvedModifier] representing this modifier followed by [other] in sequence.
     */
    public infix fun then(other: GlanceCurvedModifier): GlanceCurvedModifier =
        if (other === GlanceCurvedModifier) this else CombinedGlanceCurvedModifier(this, other)

    @JvmDefaultWithCompatibility
    /** A single element contained within a [GlanceCurvedModifier] chain. */
    public interface Element : GlanceCurvedModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)

        override fun any(predicate: (Element) -> Boolean): Boolean = predicate(this)

        override fun all(predicate: (Element) -> Boolean): Boolean = predicate(this)
    }

    /**
     * The companion object `Modifier` is the empty, default, or starter [GlanceCurvedModifier] that
     * contains no [elements][Element]. Use it to create a new [GlanceCurvedModifier] using modifier
     * extension factory functions.
     */
    // The companion object implements `Modifier` so that it may be used  as the start of a
    // modifier extension factory expression.
    public companion object : GlanceCurvedModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial

        override fun any(predicate: (Element) -> Boolean): Boolean = false

        override fun all(predicate: (Element) -> Boolean): Boolean = true

        override infix fun then(other: GlanceCurvedModifier): GlanceCurvedModifier = other

        override fun toString(): String = "Modifier"
    }
}

/**
 * A node in a [GlanceCurvedModifier] chain. A CombinedModifier always contains at least two
 * elements; a Modifier [outer] that wraps around the Modifier [inner].
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public class CombinedGlanceCurvedModifier(
    private val outer: GlanceCurvedModifier,
    private val inner: GlanceCurvedModifier,
) : GlanceCurvedModifier {
    override fun <R> foldIn(initial: R, operation: (R, GlanceCurvedModifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(initial: R, operation: (GlanceCurvedModifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(predicate: (GlanceCurvedModifier.Element) -> Boolean): Boolean =
        outer.any(predicate) || inner.any(predicate)

    override fun all(predicate: (GlanceCurvedModifier.Element) -> Boolean): Boolean =
        outer.all(predicate) && inner.all(predicate)

    override fun equals(other: Any?): Boolean =
        other is CombinedGlanceCurvedModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString(): String =
        "[" +
            foldIn("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public inline fun <reified T> GlanceCurvedModifier.findModifier(): T? =
    this.foldIn<T?>(null) { acc, cur ->
        if (cur is T) {
            cur
        } else {
            acc
        }
    }

/**
 * Find the last modifier of the given type, and create a new [GlanceCurvedModifier] which is
 * equivalent with the previous one, but without any modifiers of specified type.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public inline fun <reified T> GlanceCurvedModifier.extractModifier():
    Pair<T?, GlanceCurvedModifier> =
    if (any { it is T }) {
        foldIn<Pair<T?, GlanceCurvedModifier>>(null to GlanceCurvedModifier) { acc, cur ->
            if (cur is T) {
                cur to acc.second
            } else {
                acc.first to acc.second.then(cur)
            }
        }
    } else {
        null to this
    }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class SweepAngleModifier(public val degrees: Float) : GlanceCurvedModifier.Element

/** Sets the sweep angle of the curved element, in degrees */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun GlanceCurvedModifier.sweepAngleDegrees(degrees: Float): GlanceCurvedModifier =
    this.then(SweepAngleModifier(degrees))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class ThicknessModifier(public val thickness: Dp) : GlanceCurvedModifier.Element

/** Sets the thickness of the curved element, in [Dp] */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun GlanceCurvedModifier.thickness(thickness: Dp): GlanceCurvedModifier =
    this.then(ThicknessModifier(thickness))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class ActionCurvedModifier(public val action: Action) : GlanceCurvedModifier.Element

/** Apply an [Action], to be executed in response to a user click */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun GlanceCurvedModifier.clickable(onClick: Action): GlanceCurvedModifier =
    this.then(ActionCurvedModifier(onClick))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class SemanticsCurvedModifier(val configuration: SemanticsConfiguration) :
    GlanceCurvedModifier.Element

/**
 * Associate accessibility semantics with an element. This should generally be used sparingly, amd
 * in mose cases should only be applied to the top-level layout element or clickable elements.
 */
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public fun GlanceCurvedModifier.semantics(
    properties: (SemanticsPropertyReceiver.() -> Unit)
): GlanceCurvedModifier =
    this.then(SemanticsCurvedModifier(SemanticsConfiguration().also { it.properties() }))
