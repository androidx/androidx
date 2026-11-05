/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.currentProfileSupportsTouchModifiers
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteLayoutScopeMarker
import androidx.compose.remote.creation.compose.layout.RemotePaddingValues
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.remoteComponent
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.onTouchCancel
import androidx.compose.remote.creation.compose.modifier.onTouchDown
import androidx.compose.remote.creation.compose.modifier.onTouchUp
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.animateRemoteFloatAsState
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Layout component to implement an expressive group of buttons in a row.
 *
 * Each button in a [RemoteButtonGroup] should apply at least one [RemoteButtonGroupScope] modifier
 * ([RemoteButtonGroupScope.weight], [RemoteButtonGroupScope.minWidth], or
 * [RemoteButtonGroupScope.animateWidth]) to participate in width distribution. With
 * [RemoteButtonGroupScope.animateWidth], pressing a button expands its width by [expansionWidth]
 * while its neighboring button(s) shrink accordingly, with the spatial springs of
 * [RemoteMaterialTheme.motionScheme]. The press animation needs a profile that supports touch
 * actions.
 *
 * Example of a [RemoteButtonGroup] with 3 equal-width buttons:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteButtonGroupThreeButtonSample
 *
 * Example of 3 buttons, the middle one bigger, that animate their width on press:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteButtonGroupAnimatedSample
 * @param modifier Modifier to be applied to the button group
 * @param spacing the amount of spacing between buttons
 * @param expansionWidth the additional width (delta) added to a pressed button's resting width,
 *   rather than an absolute target width, while its neighboring button(s) shrink by the same total
 *   amount. Non-positive values result in no width expansion when pressed.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param verticalAlignment the vertical alignment of the button group's children.
 * @param content the content and properties of each button. The UX guidance is to use no more than
 *   3 buttons within a [RemoteButtonGroup].
 */
@Composable
@RemoteComposable
public fun RemoteButtonGroup(
    modifier: RemoteModifier = RemoteModifier,
    spacing: RemoteDp = RemoteButtonGroupDefaults.Spacing,
    expansionWidth: RemoteDp = RemoteButtonGroupDefaults.ExpansionWidth,
    contentPadding: RemotePaddingValues = RemoteButtonGroupDefaults.fullWidthPaddings(),
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.CenterVertically,
    content: @RemoteComposable @Composable (RemoteButtonGroupScope.() -> Unit),
) {
    RemoteButtonGroupImpl(
        modifier = modifier,
        spacing = spacing,
        expansionWidth = expansionWidth,
        contentPadding = contentPadding,
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/**
 * @param initialPressedItem index, among the items that animate their width, of the item that
 *   starts pressed, or -1 for none.
 */
@Composable
@RemoteComposable
internal fun RemoteButtonGroupImpl(
    modifier: RemoteModifier = RemoteModifier,
    spacing: RemoteDp = RemoteButtonGroupDefaults.Spacing,
    expansionWidth: RemoteDp = RemoteButtonGroupDefaults.ExpansionWidth,
    contentPadding: RemotePaddingValues = RemoteButtonGroupDefaults.fullWidthPaddings(),
    verticalAlignment: RemoteAlignment.Vertical = RemoteAlignment.CenterVertically,
    initialPressedItem: Int = -1,
    content: @RemoteComposable @Composable (RemoteButtonGroupScope.() -> Unit),
) {
    // Buttons can't be pressed without touch actions, so the group doesn't animate then, nor when
    // the buttons can't expand.
    val expands = expansionWidth.constantValueOrNull?.let { it > 0f && it.isFinite() } ?: true
    val pressAnimation =
        if (expands && currentProfileSupportsTouchModifiers()) {
            val motionScheme = RemoteMaterialTheme.motionScheme
            PressAnimation(
                pressSpec = motionScheme.buttonGroupPressSpec(),
                releaseSpec = motionScheme.slowSpatialSpec(),
                initialPressedItem = initialPressedItem,
            )
        } else {
            null
        }
    RemoteBox(modifier = modifier, contentAlignment = verticalAlignment.toBoxAlignment()) {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().padding(contentPadding),
            verticalAlignment = verticalAlignment,
            horizontalArrangement =
                RemoteArrangement.spacedBy(spacing, RemoteAlignment.CenterHorizontally),
        ) {
            RemoteButtonGroupScopeImpl(
                    rowWidth = remoteComponent().width,
                    spacing = spacing,
                    expansionWidth = expansionWidth,
                    contentPadding = contentPadding,
                    pressAnimation = pressAnimation,
                )
                .content()
        }
    }
}

/**
 * Where the box of a group places its row: the box takes the modifier of the group, so this keeps
 * `verticalAlignment` working when the group is taller than its buttons.
 */
private fun RemoteAlignment.Vertical.toBoxAlignment(): RemoteAlignment =
    when (this) {
        RemoteAlignment.Top -> RemoteAlignment.TopCenter
        RemoteAlignment.Bottom -> RemoteAlignment.BottomCenter
        else -> RemoteAlignment.Center
    }

/**
 * Scope for the children of a [RemoteButtonGroup].
 *
 * Each child should build its own modifier chain and chain [RemoteButtonGroupScope] modifiers
 * ([weight], [minWidth], [animateWidth]) contiguously before any other modifiers; do not share a
 * scope modifier instance across multiple children.
 */
@RemoteLayoutScopeMarker
@Immutable
public sealed interface RemoteButtonGroupScope {
    /**
     * Size the element's width proportional to its [weight] relative to other weighted sibling
     * elements in the [RemoteButtonGroup].
     *
     * @param weight The proportional width to give to this element, as a multiplier of the total
     *   available width. Must be a positive constant.
     * @return a [RemoteModifier] that sizes the element's width proportional to [weight]
     */
    public fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier

    /**
     * Sets the minimum width of this item, defaults to [RemoteButtonGroupDefaults.MinWidth].
     *
     * @param minWidth The minimum width to use for this item. Must be a positive constant.
     * @return a [RemoteModifier] that sets the minimum width of the item to [minWidth]
     */
    public fun RemoteModifier.minWidth(
        minWidth: RemoteDp = RemoteButtonGroupDefaults.MinWidth
    ): RemoteModifier

    /**
     * Specifies that this item should animate its width when pressed, expanding its resting width
     * by [RemoteButtonGroup]'s `expansionWidth` delta while neighboring items shrink accordingly.
     *
     * The animation needs touch actions, so it's skipped when the creation profile doesn't support
     * them.
     *
     * @return a [RemoteModifier] that animates the item's width when pressed
     */
    public fun RemoteModifier.animateWidth(): RemoteModifier
}

/**
 * How the items of a group animate their width when pressed.
 *
 * @property pressSpec spec of the animation of an item that is pressed.
 * @property releaseSpec spec of the animation of an item that is released.
 * @property initialPressedItem index, among the items that animate their width, of the item that
 *   starts pressed, or -1 for none.
 */
private class PressAnimation(
    val pressSpec: RemoteAnimationSpec,
    val releaseSpec: RemoteAnimationSpec,
    val initialPressedItem: Int,
)

private class ButtonGroupItem(
    var weight: Float = 1f,
    var minWidth: Float =
        checkNotNull(RemoteButtonGroupDefaults.MinWidth.constantValueOrNull) {
            "RemoteButtonGroupDefaults.MinWidth must be a constant"
        },
    /** How much the item is pressed, from 0 to 1, or `null` if it doesn't animate its width. */
    var pressedProgress: RemoteFloat? = null,
)

/** Scope of the items of a [RemoteButtonGroup] that animate with [pressAnimation], if not null. */
private class RemoteButtonGroupScopeImpl(
    private val rowWidth: RemoteFloat,
    private val spacing: RemoteDp,
    private val expansionWidth: RemoteDp,
    private val contentPadding: RemotePaddingValues,
    private val pressAnimation: PressAnimation?,
) : RemoteButtonGroupScope {
    private val items = mutableListOf<ButtonGroupItem>()
    private val itemsByModifier = mutableMapOf<RemoteModifier, ButtonGroupItem>()
    private var animatedItemCount = 0
    private val resolvedWidths: Array<RemoteFloat> by
        lazy(LazyThreadSafetyMode.NONE) { computeWidths() }

    override fun RemoteModifier.weight(weight: RemoteFloat): RemoteModifier {
        val value = requireNotNull(weight.constantValueOrNull) { "weight must be a constant" }
        require(value > 0f && value.isFinite()) {
            "invalid weight $value; must be a finite positive number"
        }
        val (item, modifierWithWidth) = getOrCreateItem(this)
        item.weight = value
        return modifierWithWidth
    }

    override fun RemoteModifier.minWidth(minWidth: RemoteDp): RemoteModifier {
        val value = requireNotNull(minWidth.constantValueOrNull) { "minWidth must be a constant" }
        require(value > 0f && value.isFinite()) {
            "invalid minWidth $value; must be a finite positive number"
        }
        val (item, modifierWithWidth) = getOrCreateItem(this)
        item.minWidth = value
        return modifierWithWidth
    }

    // TODO(b/579903026): Consider moving animateWidth into a Remote Compose foundation modifier.
    override fun RemoteModifier.animateWidth(): RemoteModifier {
        val (item, modifierWithWidth) = getOrCreateItem(this)
        val pressAnimation = pressAnimation
        if (pressAnimation == null || item.pressedProgress != null) {
            return modifierWithWidth
        }
        val pressed =
            MutableRemoteFloat(
                if (animatedItemCount == pressAnimation.initialPressedItem) 1f else 0f
            )
        animatedItemCount++
        // A remote float animates with a single spec, so the progress runs both animations towards
        // the same target and takes the larger: it follows pressSpec while the item is pressed and
        // releaseSpec once it's released, as long as pressSpec is the faster of the two.
        item.pressedProgress =
            max(
                animateRemoteFloatAsState(pressed, pressAnimation.pressSpec),
                animateRemoteFloatAsState(pressed, pressAnimation.releaseSpec),
            )
        val resultModifier =
            modifierWithWidth
                .onTouchDown(valueChangeAction(pressed, 1f.rf))
                .onTouchUp(valueChangeAction(pressed, 0f.rf))
                .onTouchCancel(valueChangeAction(pressed, 0f.rf))
        itemsByModifier[resultModifier] = item
        return resultModifier
    }

    // Each child's modifier is built during `content()` before later siblings are composed, while
    // every item's width depends on the full sibling list (`items.size`, `weights`, `minWidths`,
    // and `pressedProgress`es). Without restricted APIs (`RemoteModifier.foldIn` /
    // `RemoteModifier.Element`), we key on the `RemoteModifier` instance returned by scope
    // modifiers and use `RemoteFloatExpression` to forward-reference `resolvedWidths`, which
    // lazily calls `computeWidths()` once when the row's children are written to the document.
    // TODO: Expose a parent-data / custom layout API in remote-creation-compose so scope modifiers
    // can attach item metadata and the parent layout can resolve child widths directly without
    // `RemoteFloatExpression` or `itemsByModifier`.
    @Suppress("RestrictedApiAndroidX") // RemoteFloatExpression
    private fun getOrCreateItem(modifier: RemoteModifier): Pair<ButtonGroupItem, RemoteModifier> {
        val existing = itemsByModifier[modifier]
        if (existing != null) {
            return existing to modifier
        }
        val index = items.size
        val item = ButtonGroupItem()
        val width = RemoteFloatExpression { resolvedWidths[index] }
        val resultModifier =
            modifier.width(width.asRemoteDp()).let {
                // Players animate size changes of components by default, which would smooth the
                // springs that drive the widths. The widths are already animated, so turn it off.
                if (pressAnimation != null) it.animationSpec(enabled = false) else it
            }
        items.add(item)
        itemsByModifier[resultModifier] = item
        return item to resultModifier
    }

    /** The widths of the items, in dp, in a row that is [rowWidth] pixels wide. */
    private fun computeWidths(): Array<RemoteFloat> {
        if (items.isEmpty()) {
            return emptyArray()
        }
        // The items share the width of the row minus the content padding and the spacing.
        val sharedWidth =
            max(
                ((rowWidth / RemoteDensity.Host.density).asRemoteDp() -
                    contentPadding.leftPadding -
                    contentPadding.rightPadding -
                    spacing * (items.size - 1)) / 1.rdp,
                0f.rf,
            )
        return buttonGroupWidths(
            minWidths = FloatArray(items.size) { items[it].minWidth },
            weights = FloatArray(items.size) { items[it].weight },
            sharedWidth = sharedWidth,
            expansionWidth = max(expansionWidth / 1.rdp, 0f.rf),
            pressedProgresses = Array(items.size) { items[it].pressedProgress },
        )
    }
}

/**
 * Widths, as [RemoteFloat]s in dp, of the items of a [RemoteButtonGroup] that share [sharedWidth]
 * dp (the width of the group inside its content padding, minus the spacing between the items), like
 * Wear Compose Material 3 ButtonGroup.
 *
 * At rest, each item gets `max(minWidth, share * weight)`, where the share fills the shared width
 * (see [buttonGroupShare]), or `share * weight` without room for all the [minWidths]. An item that
 * animates its width grows by [expansionWidth] times its pressed progress (see
 * [pressedProgresses]): an item at an end of the group takes the growth from its only neighbor, an
 * item in the middle from both neighbors equally, and the neighbors may go below their minimum
 * width.
 */
private fun buttonGroupWidths(
    minWidths: FloatArray,
    weights: FloatArray,
    sharedWidth: RemoteFloat,
    expansionWidth: RemoteFloat,
    pressedProgresses: Array<RemoteFloat?>,
): Array<RemoteFloat> {
    val share = buttonGroupShare(minWidths, weights, sharedWidth)
    val fitsMinWidths = sharedWidth.isGreaterThanOrEqualTo(minWidths.sum().rf)
    val widths =
        Array(weights.size) { index ->
            val width = share * weights[index].rf
            fitsMinWidths.select(max(width, minWidths[index].rf), width)
        }
    if (widths.size < 2) {
        return widths
    }
    for ((pressedIndex, progress) in pressedProgresses.withIndex()) {
        if (progress == null) continue
        val neighbors =
            when (pressedIndex) {
                0 -> intArrayOf(1)
                widths.lastIndex -> intArrayOf(pressedIndex - 1)
                else -> intArrayOf(pressedIndex - 1, pressedIndex + 1)
            }
        val growth = expansionWidth * progress * (1f / neighbors.size).rf
        for (neighbor in neighbors) {
            // The neighbor gives the growth, or all its width if it's narrower.
            val shrink = min(growth, max(widths[neighbor], 0f.rf))
            widths[neighbor] = widths[neighbor] - shrink
            widths[pressedIndex] = widths[pressedIndex] + shrink
        }
    }
    return widths
}

/**
 * The width per unit of weight of the items of a group that aren't at their minimum width, when the
 * items share [sharedWidth] dp, see [buttonGroupWidths].
 *
 * As the group gets narrower, the items reach their minimum width one after the other, in
 * decreasing order of `minWidth / weight`. Once the first k of them are at their minimum width, the
 * others share what's left by their weights. Without room for all the minimum widths, all the items
 * share the width by their weights.
 */
private fun buttonGroupShare(
    minWidths: FloatArray,
    weights: FloatArray,
    sharedWidth: RemoteFloat,
): RemoteFloat {
    val count = weights.size
    val order = Array(count) { it }
    order.sortByDescending { minWidths[it] / weights[it] }
    // The minimum widths of the first k items in that order, and the weights of the others.
    val clampedMinWidths = FloatArray(count + 1)
    val otherWeights = FloatArray(count + 1)
    for (k in 0 until count) {
        clampedMinWidths[k + 1] = clampedMinWidths[k] + minWidths[order[k]]
    }
    for (k in count - 1 downTo 0) {
        otherWeights[k] = otherWeights[k + 1] + weights[order[k]]
    }
    var share = sharedWidth * (1f / otherWeights[0]).rf
    for (k in count - 1 downTo 0) {
        // With the first k items at their minimum width, the others share what's left, as long as
        // the next item is above its own minimum width.
        val next = order[k]
        val threshold = clampedMinWidths[k] + minWidths[next] / weights[next] * otherWeights[k]
        val clampedShare = (sharedWidth - clampedMinWidths[k].rf) * (1f / otherWeights[k]).rf
        share = sharedWidth.isGreaterThanOrEqualTo(threshold.rf).select(clampedShare, share)
    }
    return share
}

/** Contains the default values used by [RemoteButtonGroup] */
public object RemoteButtonGroupDefaults {
    /**
     * Return the recommended padding to use as the contentPadding of a [RemoteButtonGroup], when it
     * takes the full width of the screen.
     *
     * @return the recommended [RemotePaddingValues] for a full-width [RemoteButtonGroup]
     */
    public fun fullWidthPaddings(): RemotePaddingValues {
        // TODO(b/466078229): Use expression of the percentage of screen width as padding,
        // currently it's unable to use that since it's too long.
        return RemotePaddingValues(horizontal = 11.rdp, vertical = 0.rdp)
    }

    /** Spacing between buttons. */
    public val Spacing: RemoteDp = 4.rdp

    /**
     * Default additional width (delta) added to a pressed button's resting width in
     * [RemoteButtonGroup], rather than an absolute target width, while neighboring button(s) shrink
     * by the same total amount.
     */
    public val ExpansionWidth: RemoteDp = 24.rdp

    internal val minimumInteractiveComponentSize = 48.rdp

    /** Default for the minimum width of buttons in a [RemoteButtonGroup]. */
    public val MinWidth: RemoteDp = minimumInteractiveComponentSize
}

/**
 * [RemoteMotionScheme.fastSpatialSpec] made twice as fast, like the
 * `fastSpatialSpec().faster(100f)` that Wear Compose Material 3 ButtonGroup animates a press with.
 * A spring is made faster by scaling its stiffness, which only the built-in schemes expose, so a
 * custom scheme's [RemoteMotionScheme.fastSpatialSpec] is used as is.
 */
private fun RemoteMotionScheme.buttonGroupPressSpec(): RemoteAnimationSpec =
    when (this) {
        RemoteMotionScheme.standard() ->
            remoteSpring(
                stiffness = RemoteMotionTokens.StandardFastStiffness * PressSpeedFactorSquared,
                dampingRatio = RemoteMotionTokens.StandardSpatialDampingRatio,
            )
        RemoteMotionScheme.expressive() ->
            remoteSpring(
                stiffness = RemoteMotionTokens.ExpressiveFastStiffness * PressSpeedFactorSquared,
                dampingRatio = RemoteMotionTokens.ExpressiveFastDamping,
            )
        else -> fastSpatialSpec()
    }

/**
 * The square of the speed factor of the press animation: `faster(100f)` doubles the speed of a
 * spring by multiplying its stiffness by 2².
 */
private const val PressSpeedFactorSquared = 4f
