/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Velocity

/**
 * An OverscrollEffect represents a visual effect that displays when the edges of a scrolling
 * container have been reached with a scroll or fling. To create an instance of the default /
 * currently provided [OverscrollFactory], use [rememberOverscrollEffect].
 *
 * To implement, make sure to override [node] - this has a default implementation for compatibility
 * reasons, but is required for an OverscrollEffect to render.
 *
 * OverscrollEffect conceptually 'decorates' scroll / fling events: consuming some of the delta or
 * velocity before and/or after the event is consumed by the scrolling container. [applyToScroll]
 * applies overscroll to a scroll event, and [applyToFling] applies overscroll to a fling.
 *
 * Higher level components such as [androidx.compose.foundation.lazy.LazyColumn] will automatically
 * configure an OverscrollEffect for you. To use a custom OverscrollEffect you first need to provide
 * it with scroll and/or fling events - usually by providing it to a
 * [androidx.compose.foundation.gestures.scrollable]. Then you can draw the effect on top of the
 * scrolling content using [Modifier.overscroll].
 *
 * @sample androidx.compose.foundation.samples.OverscrollSample
 */
@Stable
interface OverscrollEffect {
    /**
     * Applies overscroll to [performScroll]. [performScroll] should represent a drag / scroll, and
     * returns the amount of delta consumed, so in simple cases the amount of overscroll to show
     * should be equal to `delta - performScroll(delta)`. The OverscrollEffect can optionally
     * consume some delta before calling [performScroll], such as to release any existing tension.
     * The implementation *must* call [performScroll] exactly once. This function should return the
     * sum of all the delta that was consumed during this operation - both by the overscroll and
     * [performScroll].
     *
     * For example, assume we want to apply overscroll to a custom component that isn't using
     * [androidx.compose.foundation.gestures.scrollable]. Here is a simple example of a component
     * using [androidx.compose.foundation.gestures.draggable] instead:
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_Before
     *
     * To apply overscroll, we need to decorate the existing logic with applyToScroll, and return
     * the amount of delta we have consumed when updating the drag position. Note that we also need
     * to call applyToFling - this is used as an end signal for overscroll so that effects can
     * correctly reset after any animations, when the gesture has stopped.
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_After
     * @param delta total scroll delta available
     * @param source the source of the delta
     * @param performScroll the scroll action that the overscroll is applied to. The [Offset]
     *   parameter represents how much delta is available, and the return value is how much delta
     *   was consumed. Any delta that was not consumed should be used to show the overscroll effect.
     * @return the delta consumed from [delta] by the operation of this function - including that
     *   consumed by [performScroll].
     */
    fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset

    /**
     * Applies overscroll to [performFling]. [performFling] should represent a fling (the release of
     * a drag or scroll), and returns the amount of [Velocity] consumed, so in simple cases the
     * amount of overscroll to show should be equal to `velocity - performFling(velocity)`. The
     * OverscrollEffect can optionally consume some [Velocity] before calling [performFling], such
     * as to release any existing tension. The implementation *must* call [performFling] exactly
     * once.
     *
     * For example, assume we want to apply overscroll to a custom component that isn't using
     * [androidx.compose.foundation.gestures.scrollable]. Here is a simple example of a component
     * using [androidx.compose.foundation.gestures.draggable] instead:
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_Before
     *
     * To apply overscroll, we decorate the existing logic with applyToScroll, and return the amount
     * of delta we have consumed when updating the drag position. We then call applyToFling using
     * the velocity provided by onDragStopped.
     *
     * @sample androidx.compose.foundation.samples.OverscrollWithDraggable_After
     * @param velocity total [Velocity] available
     * @param performFling the [Velocity] consuming lambda that the overscroll is applied to. The
     *   [Velocity] parameter represents how much [Velocity] is available, and the return value is
     *   how much [Velocity] was consumed. Any [Velocity] that was not consumed should be used to
     *   show the overscroll effect.
     */
    suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity)

    /**
     * Whether this OverscrollEffect is currently displaying overscroll.
     *
     * @return true if this OverscrollEffect is currently displaying overscroll
     */
    val isInProgress: Boolean

    /**
     * A [Modifier] that will draw this OverscrollEffect
     *
     * This API is deprecated- implementers should instead override [node]. Callers should use
     * [Modifier.overscroll].
     */
    @Deprecated(
        "This has been replaced with `node`. If you are calling this property to render overscroll, use Modifier.overscroll() instead. If you are implementing OverscrollEffect, override `node` instead to render your overscroll.",
        level = DeprecationLevel.ERROR,
        replaceWith =
            ReplaceWith("Modifier.overscroll(this)", "androidx.compose.foundation.overscroll"),
    )
    val effectModifier: Modifier
        get() = Modifier

    /**
     * The [DelegatableNode] that will render this OverscrollEffect and provide any required size or
     * other information to this effect.
     *
     * In most cases you should use [Modifier.overscroll] to render this OverscrollEffect, which
     * will internally attach this node to the hierarchy. The node should be attached before
     * [applyToScroll] or [applyToFling] is called to ensure correctness.
     *
     * This property should return a single instance, and can only be attached once, as with other
     * [DelegatableNode]s.
     */
    val node: DelegatableNode
        get() = object : Modifier.Node() {}
}

/**
 * Returns a wrapped version of [this] [OverscrollEffect] with an empty [OverscrollEffect.node].
 * This prevents the overscroll effect from applying any visual effect, but it will still handle
 * events.
 *
 * This can be used along with [withoutEventHandling] in cases where you wish to change where
 * overscroll is rendered for a given component. Pass this wrapped instance that doesn't render to
 * the component that handles events (such as [androidx.compose.foundation.lazy.LazyColumn]) to
 * prevent it from drawing the overscroll effect. Then to separately render the original overscroll
 * effect, you can directly pass it to [Modifier.overscroll] (since that modifier only renders, and
 * does not handle events). If instead you want to draw the overscroll in another component that
 * handles events, such as a different lazy list, you need to first wrap the original overscroll
 * effect with [withoutEventHandling] to prevent it from also dispatching events.
 *
 * @sample androidx.compose.foundation.samples.OverscrollRenderedOnTopOfLazyListDecorations
 * @see withoutEventHandling
 */
@Stable
fun OverscrollEffect.withoutVisualEffect(): OverscrollEffect =
    WrappedOverscrollEffect(
        attachNode = false,
        eventHandlingEnabled = true,
        innerOverscrollEffect = this,
    )

/**
 * Returns a wrapped version of [this] [OverscrollEffect] that will not handle any incoming events.
 * This means that calls to [OverscrollEffect.applyToScroll] / [OverscrollEffect.applyToFling] will
 * directly execute the provided performScroll / performFling lambdas, without the
 * [OverscrollEffect] ever seeing the incoming values. [OverscrollEffect.node] will still be
 * attached, so that overscroll can render.
 *
 * This can be useful if you want to render an [OverscrollEffect] in a different component that
 * normally provides events to overscroll, such as a [androidx.compose.foundation.lazy.LazyColumn].
 * Use this along with [withoutVisualEffect] to create two wrapped instances: one that does not
 * handle events, and one that does not draw, so you can ensure that the overscroll effect is only
 * rendered once, and only receives events from one source.
 *
 * @see withoutVisualEffect
 */
@Stable
fun OverscrollEffect.withoutEventHandling(): OverscrollEffect =
    WrappedOverscrollEffect(
        attachNode = true,
        eventHandlingEnabled = false,
        innerOverscrollEffect = this,
    )

@Immutable
private class WrappedOverscrollEffect(
    private val attachNode: Boolean,
    private val eventHandlingEnabled: Boolean,
    private val innerOverscrollEffect: OverscrollEffect,
) : OverscrollEffect {
    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        return if (eventHandlingEnabled) {
            innerOverscrollEffect.applyToScroll(delta, source, performScroll)
        } else {
            performScroll(delta)
        }
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        if (eventHandlingEnabled) {
            innerOverscrollEffect.applyToFling(velocity, performFling)
        } else {
            performFling(velocity)
        }
    }

    override val isInProgress: Boolean
        get() = innerOverscrollEffect.isInProgress

    override val node: DelegatableNode =
        if (attachNode) innerOverscrollEffect.node else object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WrappedOverscrollEffect) return false

        if (attachNode != other.attachNode) return false
        if (eventHandlingEnabled != other.eventHandlingEnabled) return false
        if (innerOverscrollEffect != other.innerOverscrollEffect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = attachNode.hashCode()
        result = 31 * result + eventHandlingEnabled.hashCode()
        result = 31 * result + innerOverscrollEffect.hashCode()
        return result
    }
}

/**
 * Renders overscroll from the provided [overscrollEffect].
 *
 * This modifier attaches the provided [overscrollEffect]'s [OverscrollEffect.node] to the
 * hierarchy, which renders the actual effect. Note that this modifier is only responsible for the
 * visual part of overscroll - on its own it will not handle input events. In addition to using this
 * modifier you also need to propagate events to the [overscrollEffect], most commonly by using a
 * [androidx.compose.foundation.gestures.scrollable].
 *
 * Alternatively, you can use a higher level API such as [verticalScroll] or
 * [androidx.compose.foundation.lazy.LazyColumn] and provide a custom [OverscrollEffect] - these
 * components will both render and provide events to the [OverscrollEffect], so you do not need to
 * manually render the effect with this modifier.
 *
 * @sample androidx.compose.foundation.samples.OverscrollSample
 * @param overscrollEffect the [OverscrollEffect] to render
 */
@Suppress("DEPRECATION_ERROR")
fun Modifier.overscroll(overscrollEffect: OverscrollEffect?): Modifier {
    val effectModifier = overscrollEffect?.effectModifier ?: Modifier
    val modifier =
        if (effectModifier !== Modifier) effectModifier
        else OverscrollModifierElement(overscrollEffect)
    return this.then(modifier)
}

private class OverscrollModifierElement(private val overscrollEffect: OverscrollEffect?) :
    ModifierNodeElement<OverscrollModifierNode>() {
    override fun create(): OverscrollModifierNode {
        return OverscrollModifierNode(overscrollEffect?.node)
    }

    override fun update(node: OverscrollModifierNode) {
        node.update(overscrollEffect?.node)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverscrollModifierElement) return false

        if (overscrollEffect != other.overscrollEffect) return false
        return true
    }

    override fun hashCode(): Int {
        return overscrollEffect.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "overscroll"
        properties["overscrollEffect"] = overscrollEffect
    }
}

private class OverscrollModifierNode(private var overscrollNode: DelegatableNode?) :
    DelegatingNode() {
    override fun onAttach() {
        attachIfNeeded()
    }

    override fun onDetach() {
        overscrollNode?.let { undelegate(it) }
    }

    fun update(overscrollNode: DelegatableNode?) {
        this.overscrollNode?.let { undelegate(it) }
        this.overscrollNode = overscrollNode
        attachIfNeeded()
    }

    private fun attachIfNeeded() {
        overscrollNode =
            if (overscrollNode?.node?.isAttached == false) {
                delegate(overscrollNode!!)
            } else {
                null
            }
    }
}

/**
 * Returns a remembered [OverscrollEffect] created from the current value of
 * [LocalOverscrollFactory]. If [LocalOverscrollFactory] changes, a new [OverscrollEffect] will be
 * returned. Returns `null` if `null` is provided to [LocalOverscrollFactory].
 */
@Composable
fun rememberOverscrollEffect(): OverscrollEffect? {
    val overscrollFactory = LocalOverscrollFactory.current ?: return null
    return remember(overscrollFactory) { overscrollFactory.createOverscrollEffect() }
}

/**
 * Needed for behavioral backwards compatibility for
 * [androidx.compose.foundation.gestures.ScrollableDefaults.overscrollEffect]. New code should use
 * [rememberOverscrollEffect] instead, which takes into account theme provided overscroll, rather
 * than always using the platform default, without any customizations.
 */
@Composable internal expect fun rememberPlatformOverscrollEffect(): OverscrollEffect?

/**
 * A factory for creating [OverscrollEffect]s. You can provide a factory instance to
 * [LocalOverscrollFactory] to globally change the factory, and hence effect, used by components
 * within the hierarchy.
 *
 * See [rememberOverscrollEffect] to remember an [OverscrollEffect] from the current factory
 * provided to [LocalOverscrollFactory].
 */
interface OverscrollFactory {
    /** Returns a new [OverscrollEffect] instance. */
    fun createOverscrollEffect(): OverscrollEffect

    /**
     * Require hashCode() to be implemented. Using a data class is sufficient. Singletons and
     * instances with no properties may implement this function by returning an arbitrary constant.
     */
    override fun hashCode(): Int

    /**
     * Require equals() to be implemented. Using a data class is sufficient. Singletons may
     * implement this function with referential equality (`this === other`). Instances with no
     * properties may implement this function by checking the type of the other object.
     */
    override fun equals(other: Any?): Boolean
}

/**
 * CompositionLocal that provides an [OverscrollFactory] through the hierarchy. This will be used by
 * default by scrolling components, so you can provide an [OverscrollFactory] here to override the
 * overscroll used by components within a hierarchy.
 *
 * See [rememberOverscrollEffect] to remember an [OverscrollEffect] from the current provided value.
 */
val LocalOverscrollFactory: ProvidableCompositionLocal<OverscrollFactory?> =
    compositionLocalWithComputedDefaultOf {
        defaultOverscrollFactory()
    }

internal expect fun CompositionLocalAccessorScope.defaultOverscrollFactory(): OverscrollFactory?
