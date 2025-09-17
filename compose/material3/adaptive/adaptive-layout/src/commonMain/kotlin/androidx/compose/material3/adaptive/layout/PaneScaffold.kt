/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Transition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Extended scope for the panes of pane scaffolds. All pane scaffolds will implement this interface
 * to provide necessary info for panes to correctly render their content, motion, etc.
 *
 * @param Role The type of roles that denotes panes in the associated pane scaffold.
 * @param ScaffoldValue The type of scaffold values that denotes the [PaneAdaptedValue]s in the
 *   associated pane scaffold.
 * @see ThreePaneScaffoldPaneScope
 * @see PaneScaffoldScope
 * @see PaneScaffoldTransitionScope
 * @see PaneScaffoldPaneScope
 * @see LookaheadScope
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface ExtendedPaneScaffoldPaneScope<
    Role : PaneScaffoldRole,
    ScaffoldValue : PaneScaffoldValue<Role>,
> : ExtendedPaneScaffoldScope<Role, ScaffoldValue>, PaneScaffoldPaneScope<Role>

/**
 * Extended scope for pane scaffolds. All pane scaffolds will implement this interface to provide
 * necessary info for its sub-composables to correctly render their content, motion, etc.
 *
 * @param Role The type of roles that denotes panes in the associated pane scaffold.
 * @param ScaffoldValue The type of scaffold values that denotes the [PaneAdaptedValue]s in the
 *   associated pane scaffold.
 * @see ThreePaneScaffoldScope
 * @see PaneScaffoldScope
 * @see PaneScaffoldTransitionScope
 * @see LookaheadScope
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface ExtendedPaneScaffoldScope<
    Role : PaneScaffoldRole,
    ScaffoldValue : PaneScaffoldValue<Role>,
> : PaneScaffoldScope, PaneScaffoldTransitionScope<Role, ScaffoldValue>, LookaheadScope {
    val focusRequesters: Map<Role, FocusRequester>
}

/**
 * The base scope of pane scaffolds, which provides scoped functions that supported by pane
 * scaffolds.
 */
sealed interface PaneScaffoldScope {
    /**
     * This modifier specifies the preferred width for a pane in [Dp]s, and the pane scaffold
     * implementation will try its best to respect this width when the associated pane is rendered
     * as a fixed pane, i.e., a pane that are not stretching to fill the remaining spaces. In case
     * the modifier is not set or set to [Dp.Unspecified], the default preferred widths provided by
     * [PaneScaffoldDirective] are supposed to be used.
     *
     * Note that the preferred width may not be applied when the associated pane has a higher
     * priority than the rest of panes (for example, primary pane v.s. secondary pane) so it
     * stretches to fill the available width, or when there are hinges to avoid intersecting with
     * the scaffold, so the pane will be shrunk or expanded to respect the hinge areas.
     *
     * Also note that if multiple [PaneScaffoldScope.preferredWidth] modifiers are applied, the last
     * applied one will override all the previous settings.
     *
     * @sample androidx.compose.material3.adaptive.samples.PreferredSizeModifierInDpSample
     * @see PaneScaffoldDirective.defaultPanePreferredWidth
     */
    fun Modifier.preferredWidth(width: Dp): Modifier

    /**
     * This modifier specifies the preferred width for a pane as a proportion of the overall
     * scaffold width. The value is a float ranging from 0.0 to 1.0.
     *
     * The pane scaffold implementation will endeavor to respect this width when the associated pane
     * is rendered as a fixed pane, i.e., a pane that is not stretching to fill the remaining
     * spaces.
     *
     * If this modifier is not set, the default preferred width defined by
     * [PaneScaffoldDirective.defaultPanePreferredWidth] will be used.
     *
     * Note that the preferred width may not be applied when the associated pane has a higher
     * priority than the rest of panes (for example, primary pane v.s. secondary pane) so it
     * stretches to fill the available width, or when there are hinges to avoid intersecting with
     * the scaffold, so the pane will be shrunk or expanded to respect the hinge areas.
     *
     * Also note that if multiple [PaneScaffoldScope.preferredWidth] modifiers are applied, the last
     * applied one will override all the previous settings.
     *
     * @sample androidx.compose.material3.adaptive.samples.PreferredSizeModifierInProportionSample
     * @see PaneScaffoldDirective.defaultPanePreferredWidth
     */
    fun Modifier.preferredWidth(@FloatRange(0.0, 1.0) proportion: Float): Modifier

    /**
     * This modifier specifies the preferred height for a pane in [Dp]s, and the pane scaffold
     * implementation will try its best to respect this height when the associated pane is rendered
     * as a reflowed or a levitated pane. In case the modifier is not set or set to
     * [Dp.Unspecified], the default preferred heights provided by [PaneScaffoldDirective] are
     * supposed to be used.
     *
     * Note that the preferred height may not be applied when the associated pane is an expanded
     * pane so it stretches to fill the available height, or when there are hinges to avoid
     * intersecting with the scaffold, so the pane will be shrunk or expanded to respect the hinge
     * areas.
     *
     * Also note that if multiple [PaneScaffoldScope.preferredHeight] modifiers are applied, the
     * last applied one will override all the previous settings.
     *
     * @sample androidx.compose.material3.adaptive.samples.PreferredSizeModifierInDpSample
     * @see PaneScaffoldDirective.defaultPanePreferredHeight
     */
    fun Modifier.preferredHeight(height: Dp): Modifier

    /**
     * This modifier specifies the preferred height for a pane as a proportion of the overall
     * scaffold height. The value is a float ranging from 0.0 to 1.0.
     *
     * The pane scaffold implementation will endeavor to respect this height when the associated
     * pane is rendered in its [PaneAdaptedValue.Reflowed] or [PaneAdaptedValue.Levitated] state.
     *
     * If this modifier is not set, the default preferred height defined by
     * [PaneScaffoldDirective.defaultPanePreferredHeight] will be used.
     *
     * Note that the preferred height may not be applied when the associated pane is an expanded
     * pane so it stretches to fill the available height, or when there are hinges to avoid
     * intersecting with the scaffold, so the pane will be shrunk or expanded to respect the hinge
     * areas.
     *
     * Also note that if multiple [PaneScaffoldScope.preferredHeight] modifiers are applied, the
     * last applied one will override all the previous settings.
     *
     * @sample androidx.compose.material3.adaptive.samples.PreferredSizeModifierInProportionSample
     * @see PaneScaffoldDirective.defaultPanePreferredHeight
     */
    fun Modifier.preferredHeight(@FloatRange(0.0, 1.0) proportion: Float): Modifier

    /**
     * The modifier that should be applied on a drag handle composable so the drag handle can be
     * dragged and operate on the provided [PaneExpansionState] properly. By default this modifier
     * supports two types of user interactions:
     * 1. Dragging the handle horizontally within the pane scaffold.
     * 2. Accessibility actions provided via [semanticsProperties].
     *
     * Besides that, this modifier also sets up other necessary behaviors of a pane expansion drag
     * handle, like excluding system gestures and ensuring minimum touch target size.
     *
     * See usage samples at:
     *
     * @sample androidx.compose.material3.adaptive.samples.PaneExpansionDragHandleSample
     * @param state the [PaneExpansionState] that controls the pane expansion of the associated pane
     *   scaffold
     * @param minTouchTargetSize the minimum touch target size of the drag handle
     * @param interactionSource the [MutableInteractionSource] to address user interactions
     * @param semanticsProperties the optional semantics setup working with accessibility services;
     *   a default implementation will be used if nothing is provided.
     */
    fun Modifier.paneExpansionDraggable(
        state: PaneExpansionState,
        minTouchTargetSize: Dp,
        interactionSource: MutableInteractionSource,
        semanticsProperties: (SemanticsPropertyReceiver.() -> Unit)? = null,
    ): Modifier

    /**
     * The saveable state holder to save pane states across their visibility life-cycles. The
     * default pane implementations like [AnimatedPane] are supposed to use it to store states.
     */
    val saveableStateHolder: SaveableStateHolder
}

/**
 * The transition scope of pane scaffold implementations, which provides the current transition info
 * of the associated pane scaffold.
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldTransitionScope<
    Role : PaneScaffoldRole,
    ScaffoldValue : PaneScaffoldValue<Role>,
> {
    /** The current scaffold state transition between [PaneScaffoldValue]s. */
    val scaffoldStateTransition: Transition<ScaffoldValue>

    /** The current motion progress. */
    @get:FloatRange(from = 0.0, to = 1.0) val motionProgress: Float

    /**
     * Provides measurement and other data required in motion calculation like the size and offset
     * of each pane before and after the motion.
     *
     * Note that the data provided are supposed to be only read proactively by the motion logic
     * "on-the-fly" when the scaffold motion is happening. Using them elsewhere may cause unexpected
     * behavior.
     */
    val motionDataProvider: PaneScaffoldMotionDataProvider<Role>
}

/**
 * The pane scope of the current pane under the scope, which provides the pane relevant info like
 * its role and [PaneMotion].
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldPaneScope<Role : PaneScaffoldRole> {
    /** The role of the current pane in the scope. */
    val paneRole: Role

    /** The specified pane motion of the current pane in the scope. */
    val paneMotion: PaneMotion

    /**
     * Indicates if the pane should be interactable, i.e. focusable, clickable, etc. A pane can be
     * non-interactable if it's [PaneAdaptedValue.Hidden] or being covered by a scrim casted by a
     * [PaneAdaptedValue.Levitated] pane.
     */
    val isInteractable: Boolean
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal abstract class PaneScaffoldScopeImpl(
    override val saveableStateHolder: SaveableStateHolder
) : PaneScaffoldScope {
    override fun Modifier.preferredWidth(width: Dp): Modifier {
        require(width == Dp.Unspecified || width > 0.dp) { "invalid width" }
        return this.then(PreferredWidthElement(PreferredSize(dp = width)))
    }

    override fun Modifier.preferredWidth(proportion: Float): Modifier {
        require(proportion >= 0f && proportion <= 1f) { "invalid width proportion" }
        return this.then(PreferredWidthElement(PreferredSize(proportion = proportion)))
    }

    override fun Modifier.preferredHeight(height: Dp): Modifier {
        require(height == Dp.Unspecified || height > 0.dp) { "invalid height" }
        return this.then(PreferredHeightElement(PreferredSize(dp = height)))
    }

    override fun Modifier.preferredHeight(proportion: Float): Modifier {
        require(proportion >= 0f && proportion <= 1f) { "invalid width proportion" }
        return this.then(PreferredHeightElement(PreferredSize(proportion = proportion)))
    }
}

internal data class PreferredSize(val dp: Dp = Dp.Unspecified, val proportion: Float = Float.NaN) {
    companion object {
        val Unspecified = PreferredSize()
    }
}

private class PreferredWidthElement(private val width: PreferredSize) :
    ModifierNodeElement<PreferredWidthNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "preferredWidth"
        value = width
    }

    override fun create(): PreferredWidthNode {
        return PreferredWidthNode(width)
    }

    override fun update(node: PreferredWidthNode) {
        node.width = width
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        return width.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? PreferredWidthElement ?: return false
        return width == otherModifier.width
    }
}

private class PreferredWidthNode(var width: PreferredSize) :
    ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentDataImpl) ?: PaneScaffoldParentDataImpl()).also {
            it.preferredWidthInternal = width
        }
}

private class PreferredHeightElement(private val height: PreferredSize) :
    ModifierNodeElement<PreferredHeightNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "preferredHeight"
        value = height
    }

    override fun create(): PreferredHeightNode {
        return PreferredHeightNode(height)
    }

    override fun update(node: PreferredHeightNode) {
        node.height = height
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        return height.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? PreferredHeightElement ?: return false
        return height == otherModifier.height
    }
}

private class PreferredHeightNode(var height: PreferredSize) :
    ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentDataImpl) ?: PaneScaffoldParentDataImpl()).also {
            it.preferredHeightInternal = height
        }
}

internal fun Modifier.animatedPane(): Modifier {
    return this.then(AnimatedPaneElement)
}

private object AnimatedPaneElement : ModifierNodeElement<AnimatedPaneNode>() {
    private val inspectorInfo = debugInspectorInfo {
        name = "isPaneComposable"
        value = true
    }

    override fun create(): AnimatedPaneNode {
        return AnimatedPaneNode()
    }

    override fun update(node: AnimatedPaneNode) {}

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return (other is AnimatedPaneElement)
    }
}

private class AnimatedPaneNode : ParentDataModifierNode, Modifier.Node() {
    override fun Density.modifyParentData(parentData: Any?) =
        ((parentData as? PaneScaffoldParentDataImpl) ?: PaneScaffoldParentDataImpl()).also {
            it.isAnimatedPane = true
        }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val Measurable.minTouchTargetSize: Dp
    get() {
        val size = (parentData as? PaneScaffoldParentData)?.minTouchTargetSize ?: Dp.Unspecified
        return if (size.isSpecified) size else 0.dp
    }

/**
 * The parent data passed to pane scaffolds by their contents like panes and drag handles.
 *
 * @see PaneScaffoldScope.preferredWidth
 */
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldParentData {
    /**
     * The preferred width of the pane, which is supposed to be set via
     * [PaneScaffoldScope.preferredWidth] on a pane composable, like [AnimatedPane]. Note that this
     * won't take effect on drag handle composables with the default scaffold implementations.
     */
    val preferredWidth: Dp

    /**
     * The preferred height of the pane, which is supposed to be set via
     * [PaneScaffoldScope.preferredHeight] on a pane composable, like [AnimatedPane]. Note that this
     * won't take effect on drag handle composables with the default scaffold implementations.
     */
    val preferredHeight: Dp

    /**
     * The preferred width of the pane as a proportion to the overall scaffold width, represented as
     * a float value ranging from 0 to 1. It is supposed to be set via
     * [PaneScaffoldScope.preferredWidth] on a pane composable, like [AnimatedPane]. Note that this
     * won't take effect on drag handle composables with the default scaffold implementations.
     */
    val preferredWidthInProportion: Float

    /**
     * The preferred height of the pane as a proportion to the overall scaffold height, represented
     * as a float value ranging from 0 to 1. It is supposed to be set via
     * [PaneScaffoldScope.preferredHeight] on a pane composable, like [AnimatedPane]. Note that this
     * won't take effect on drag handle composables with the default scaffold implementations.
     */
    val preferredHeightInProportion: Float

    /**
     * `true` to indicate that the pane is an [AnimatedPane]; otherwise `false`. Note that this
     * won't take effect on drag handle composables with the default scaffold implementations.
     */
    val isAnimatedPane: Boolean

    /**
     * The minimum touch target size of the child, which is supposed to be set via
     * [PaneScaffoldScope.paneExpansionDraggable] on a drag handle component. Note that this won't
     * take effect on pane composables with the default scaffold implementations.
     */
    val minTouchTargetSize: Dp
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal data class PaneScaffoldParentDataImpl(
    var preferredWidthInternal: PreferredSize = PreferredSize.Unspecified,
    var preferredHeightInternal: PreferredSize = PreferredSize.Unspecified,
    var paneMargins: PaneMargins = PaneMargins.Unspecified,
    override var isAnimatedPane: Boolean = false,
    override var minTouchTargetSize: Dp = Dp.Unspecified,
) : PaneScaffoldParentData {
    override val preferredWidth: Dp
        get() = preferredWidthInternal.dp

    override val preferredHeight: Dp
        get() = preferredHeightInternal.dp

    override val preferredWidthInProportion: Float
        get() = preferredWidthInternal.proportion

    override val preferredHeightInProportion: Float
        get() = preferredHeightInternal.proportion
}
