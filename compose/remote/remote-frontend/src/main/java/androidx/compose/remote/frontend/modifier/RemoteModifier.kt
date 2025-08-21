/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.modifier

import android.annotation.SuppressLint
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier

/**
 * An ordered, immutable, collection of modifier element for the Remote library.
 *
 * This plays the same role as [androidx.compose.ui.Modifier], but for the Remote composables.
 */
@Stable
interface RemoteModifier {

    fun toRemoteCompose(): RecordingModifier

    @Composable fun Modifier.toComposeUi(): Modifier

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from outside in.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldIn] may be used to accumulate a value starting from the
     * parent or head of the modifier chain to the final wrapped child.
     */
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from inside out.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldOut] may be used to accumulate a value starting from the
     * child or tail of the modifier chain up to the parent or head of the chain.
     */
    fun <R> foldOut(initial: R, operation: (Element, R) -> R): R

    /** Returns `true` if [predicate] returns true for any [Element] in this [RemoteModifier]. */
    fun any(predicate: (Element) -> Boolean): Boolean

    /**
     * Returns `true` if [predicate] returns true for all [Element]s in this [RemoteModifier] or if
     * this [RemoteModifier] contains no [Element]s.
     */
    fun all(predicate: (Element) -> Boolean): Boolean

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [RemoteModifier] representing this modifier followed by [other] in sequence.
     */
    infix fun then(other: RemoteModifier): RemoteModifier =
        if (other === RemoteModifier) this else CombinedRemoteModifier(this, other)

    /** A single element contained within a [RemoteModifier] chain. */
    interface Element : RemoteModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R =
            operation(this, initial)

        override fun any(predicate: (Element) -> Boolean): Boolean = predicate(this)

        override fun all(predicate: (Element) -> Boolean): Boolean = predicate(this)

        override fun toRemoteCompose(): RecordingModifier {
            return RecordingModifier().then(toRemoteComposeElement())
        }

        fun toRemoteComposeElement(): RecordingModifier.Element
    }

    /**
     * The companion object `Modifier` is the empty, default, or starter [RemoteModifier] that
     * contains no [elements][Element]. Use it to create a new [RemoteModifier] using modifier
     * extension factory functions.
     */
    // The companion object implements `Modifier` so that it may be used  as the start of a
    // modifier extension factory expression.
    companion object : RemoteModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial

        override fun <R> foldOut(initial: R, operation: (Element, R) -> R): R = initial

        override fun any(predicate: (Element) -> Boolean): Boolean = false

        override fun all(predicate: (Element) -> Boolean): Boolean = true

        override infix fun then(other: RemoteModifier): RemoteModifier = other

        override fun toString(): String = "Modifier"

        override fun toRemoteCompose(): RecordingModifier = RecordingModifier()

        @Composable override fun Modifier.toComposeUi(): Modifier = this
    }
}

/** Convert to Compose UI Modifier. */
@SuppressLint("ModifierFactoryExtensionFunction")
@Composable
fun RemoteModifier.toComposeUi(): Modifier {
    return Modifier.toComposeUi()
}

/**
 * Filter the Layout relevant [RemoteModifier.Element]s and then convert to Compose UI [Modifier].
 */
@SuppressLint("ModifierFactoryExtensionFunction")
@Composable
fun RemoteModifier.toComposeUiLayout(): Modifier {
    return this.foldIn<RemoteModifier>(RemoteModifier) { r, n ->
            if (n is RemoteLayoutModifier) {
                r.then(n)
            } else {
                r
            }
        }
        .toComposeUi()
}

/**
 * A node in a [RemoteModifier] chain. A CombinedModifier always contains at least two elements; a
 * Modifier [outer] that wraps around the Modifier [inner].
 */
class CombinedRemoteModifier(private val outer: RemoteModifier, private val inner: RemoteModifier) :
    RemoteModifier {

    override fun toRemoteCompose(): RecordingModifier {
        return RecordingModifier().apply {
            if (outer is RemoteModifier.Element) {
                then(outer.toRemoteComposeElement())
            } else {
                then(outer.toRemoteCompose())
            }

            if (inner is RemoteModifier.Element) {
                then(inner.toRemoteComposeElement())
            } else {
                then(inner.toRemoteCompose())
            }
        }
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        var result = this

        result = with(outer) { result.toComposeUi() }
        result = with(inner) { result.toComposeUi() }

        return result
    }

    override fun <R> foldIn(initial: R, operation: (R, RemoteModifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun <R> foldOut(initial: R, operation: (RemoteModifier.Element, R) -> R): R =
        outer.foldOut(inner.foldOut(initial, operation), operation)

    override fun any(predicate: (RemoteModifier.Element) -> Boolean): Boolean =
        outer.any(predicate) || inner.any(predicate)

    override fun all(predicate: (RemoteModifier.Element) -> Boolean): Boolean =
        outer.all(predicate) && inner.all(predicate)

    override fun equals(other: Any?): Boolean =
        other is CombinedRemoteModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = outer.hashCode() + 31 * inner.hashCode()

    override fun toString(): String =
        "[" +
            foldIn("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}

/**
 * Indicates an Element is relevant for further Remote Compose Layout even in Recording, and should
 * be applied.
 */
interface RemoteLayoutModifier : RemoteModifier.Element
