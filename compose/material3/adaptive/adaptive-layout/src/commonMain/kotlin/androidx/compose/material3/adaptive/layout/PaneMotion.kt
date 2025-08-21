/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastRoundToInt
import kotlin.jvm.JvmInline
import kotlin.math.max
import kotlin.math.min

/**
 * Scope for performing pane motions within a pane scaffold. It provides the spec and necessary info
 * to decide a pane's [EnterTransition] and [ExitTransition], as well as how bounds morphing will be
 * performed.
 *
 * @param Role the pane scaffold role class used to specify panes in the associated pane scaffold;
 *   see [ThreePaneScaffoldRole], [ListDetailPaneScaffoldRole], and [SupportingPaneScaffoldRole].
 */
@Suppress("PrimitiveInCollection") // No way to get underlying Long of IntSize or IntOffset
@ExperimentalMaterial3AdaptiveApi
sealed interface PaneScaffoldMotionDataProvider<Role : PaneScaffoldRole> {
    /**
     * The scaffold's current size. Note that the value of the field will only be updated during
     * measurement of the scaffold and before the first measurement the value will be
     * [IntSize.Zero].
     *
     * Note that this field is not backed by snapshot states so it's supposed to be only read
     * proactively by the motion logic "on-the-fly" when the scaffold motion is happening.
     */
    val scaffoldSize: IntSize

    /** The number of [PaneMotionData] stored in the provider. */
    val count: Int

    /**
     * Returns the role of the pane at the given index.
     *
     * @param index the index of the associated pane
     * @throws IndexOutOfBoundsException if [index] is larger than or equals to [count]
     */
    fun getRoleAt(index: Int): Role

    /**
     * Returns [PaneMotionData] associated with the given pane scaffold [role].
     *
     * The size and the offset info provided by motion data will only be update during the
     * measurement stage of the scaffold. Before the first measurement, their values will be
     * [IntSize.Zero] and [IntOffset.Zero].
     *
     * Note that the aforementioned variable fields are **NOT** backed by snapshot states and they
     * are supposed to be only read proactively by the motion logic "on-the-fly" when the scaffold
     * motion is happening. Using them elsewhere may cause unexpected behavior.
     *
     * @param role the role of the associated pane
     */
    operator fun get(role: Role): PaneMotionData

    /**
     * Returns [PaneMotionData] associated with the given index, in the left-to-right order of the
     * panes in the scaffold.
     *
     * The size and the offset info provided by motion data will only be update during the
     * measurement stage of the scaffold. Before the first measurement, their values will be
     * [IntSize.Zero] and [IntOffset.Zero].
     *
     * Note that the aforementioned variable fields are **NOT** backed by snapshot states and they
     * are supposed to be only read proactively by the motion logic "on-the-fly" when the scaffold
     * motion is happening. Using them elsewhere may cause unexpected behavior.
     *
     * @param index the index of the associated pane
     * @throws IndexOutOfBoundsException if [index] is larger than or equals to [count]
     */
    operator fun get(index: Int): PaneMotionData
}

/**
 * Perform actions on each [PaneMotionData], in the left-to-right order of the panes in the
 * scaffold.
 *
 * @param action action to perform on each [PaneMotionData].
 */
@ExperimentalMaterial3AdaptiveApi
inline fun <Role : PaneScaffoldRole> PaneScaffoldMotionDataProvider<Role>.forEach(
    action: (Role, PaneMotionData) -> Unit
) {
    for (i in 0 until count) {
        action(getRoleAt(i), get(i))
    }
}

/**
 * Perform actions on each [PaneMotionData], in the right-to-left order of the panes in the
 * scaffold.
 *
 * @param action action to perform on each [PaneMotionData].
 */
@ExperimentalMaterial3AdaptiveApi
inline fun <Role : PaneScaffoldRole> PaneScaffoldMotionDataProvider<Role>.forEachReversed(
    action: (Role, PaneMotionData) -> Unit
) {
    for (i in count - 1 downTo 0) {
        action(getRoleAt(i), get(i))
    }
}

/** The default settings of pane motions. */
@ExperimentalMaterial3AdaptiveApi
object PaneMotionDefaults {
    private val IntRectVisibilityThreshold = IntRect(1, 1, 1, 1)

    /**
     * The default [FiniteAnimationSpec] used to animate panes. Note that the animation spec is
     * based on bounds animation - in a situation to animate offset or size independently,
     * developers can use the derived [OffsetAnimationSpec] and [SizeAnimationSpec].
     */
    val AnimationSpec: FiniteAnimationSpec<IntRect> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 380f,
            visibilityThreshold = IntRectVisibilityThreshold,
        )

    /**
     * The default [FiniteAnimationSpec] used to animate panes with a delay. Note that the animation
     * spec is based on bounds animation - in a situation to animate offset or size independently,
     * developers can use the derived [DelayedOffsetAnimationSpec] and [DelayedSizeAnimationSpec].
     */
    val DelayedAnimationSpec: FiniteAnimationSpec<IntRect> =
        DelayedSpringSpec(
            dampingRatio = 0.8f,
            stiffness = 380f,
            delayedRatio = 0.1f,
            visibilityThreshold = IntRectVisibilityThreshold,
        )

    /** The default [FiniteAnimationSpec] used to animate panes' visibility. */
    val VisibilityAnimationSpec: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /**
     * The derived [FiniteAnimationSpec] that can be used to animate panes' positions when the
     * specified pane motion is sliding in or out without size change. The spec will be derived from
     * the provided [AnimationSpec] the using the corresponding top-left coordinates.
     */
    val OffsetAnimationSpec: FiniteAnimationSpec<IntOffset> =
        DerivedOffsetAnimationSpec(AnimationSpec)

    /**
     * The derived [FiniteAnimationSpec] that can be used to animate panes' sizes when the specified
     * pane motion is expanding or shrinking without position change. The spec will be derived from
     * the provided [AnimationSpec] by using the corresponding sizes.
     */
    val SizeAnimationSpec: FiniteAnimationSpec<IntSize> = DerivedSizeAnimationSpec(AnimationSpec)

    /**
     * The derived [FiniteAnimationSpec] that can be used to animate panes' positions when the
     * specified pane motion is sliding in or out with a delay without size change. The spec will be
     * derived from the provided [DelayedAnimationSpec] the using the corresponding top-left
     * coordinates.
     */
    val DelayedOffsetAnimationSpec: FiniteAnimationSpec<IntOffset> =
        DerivedOffsetAnimationSpec(DelayedAnimationSpec)

    /**
     * The derived [FiniteAnimationSpec] that can be used to animate panes' sizes when the specified
     * pane motion is expanding or shrinking with a delay without position change. The spec will be
     * derived from the provided [DelayedAnimationSpec] by using the corresponding sizes.
     */
    val DelayedSizeAnimationSpec: FiniteAnimationSpec<IntSize> =
        DerivedSizeAnimationSpec(DelayedAnimationSpec)
}

/**
 * A class to collect motion-relevant data of a specific pane.
 *
 * @property motion The specified [PaneMotion] of the pane.
 * @property originSize The origin measured size of the pane that it should animate from.
 * @property originPosition The origin placement of the pane that it should animate from, with the
 *   offset relative to the associated pane scaffold's local coordinates.
 * @property targetSize The target measured size of the pane that it should animate to.
 * @property targetPosition The target placement of the pane that it should animate to, with the
 *   offset relative to the associated pane scaffold's local coordinates.
 */
@ExperimentalMaterial3AdaptiveApi
class PaneMotionData internal constructor() {
    var motion: PaneMotion by mutableStateOf(PaneMotion.NoMotion)
        internal set

    var originSize: IntSize = IntSize.Zero
        internal set

    var originPosition: IntOffset = IntOffset.Zero
        internal set

    var targetSize: IntSize = IntSize.Zero
        internal set

    var targetPosition: IntOffset = IntOffset.Zero
        internal set

    internal var zIndex: Float = 0f

    internal var isOriginSizeAndPositionSet = false

    internal var isTargetSizeAndPositionSet = false
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneMotionData.targetLeft
    get() = targetPosition.x

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneMotionData.targetRight
    get() = targetPosition.x + targetSize.width

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneMotionData.targetTop
    get() = targetPosition.y

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val PaneMotionData.targetBottom
    get() = targetPosition.y + targetSize.height

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneMotionData.currentLeft
    get() = originPosition.x

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneMotionData.currentRight
    get() = originPosition.x + originSize.width

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionDataProvider<*>.slideInFromLeftOffset: Int
    get() {
        // The sliding in distance from left will either be:
        // 1. The target offset of the left edge of the pane after all panes that are sliding in
        //    from left, so to account for the spacer size between the sliding panes and other
        //    panes.
        // 2. If no such panes exist, use the right edge of the last pane that is sliding in from
        //    left, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        forEachReversed { _, it ->
            if (
                it.motion == PaneMotion.EnterFromLeft ||
                    it.motion == PaneMotion.EnterFromLeftDelayed
            ) {
                return -(previousPane?.targetLeft ?: it.targetRight)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown ||
                    it.motion.type == PaneMotion.Type.Entering
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionDataProvider<*>.slideInFromRightOffset: Int
    get() {
        // The sliding in distance from right will either be:
        // 1. The target offset of the right edge of the pane before all panes that are sliding in
        //    from right, so to account for the spacer size between the sliding panes and other
        //    panes.
        // 2. If no such panes exist, use the left edge of the first pane that is sliding in from
        //    right, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        forEach { _, it ->
            if (
                it.motion == PaneMotion.EnterFromRight ||
                    it.motion == PaneMotion.EnterFromRightDelayed
            ) {
                return scaffoldSize.width - (previousPane?.targetRight ?: it.targetLeft)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown ||
                    it.motion.type == PaneMotion.Type.Entering
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionDataProvider<*>.slideOutToLeftOffset: Int
    get() {
        // The sliding out distance to left will either be:
        // 1. The current offset of the left edge of the pane after all panes that are sliding out
        //    to left, so to account for the spacer size between the sliding panes and other panes.
        // 2. If no such panes exist, use the right edge of the last pane that is sliding out to
        //    left, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        forEachReversed { _, it ->
            if (it.motion == PaneMotion.ExitToLeft) {
                return -(previousPane?.currentLeft ?: it.currentRight)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown || it.motion.type == PaneMotion.Type.Exiting
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal val PaneScaffoldMotionDataProvider<*>.slideOutToRightOffset: Int
    get() {
        // The sliding out distance to right will either be:
        // 1. The current offset of the right edge of the pane before all panes that are sliding out
        //    to right, so to account for the spacer size between the sliding panes and other panes.
        // 2. If no such panes exist, use the left edge of the first pane that is sliding out to
        //    right, as in this case we don't need to account for the spacer size.
        var previousPane: PaneMotionData? = null
        forEach { _, it ->
            if (it.motion == PaneMotion.ExitToRight) {
                return scaffoldSize.width - (previousPane?.currentRight ?: it.currentLeft)
            }
            if (
                it.motion.type == PaneMotion.Type.Shown || it.motion.type == PaneMotion.Type.Exiting
            ) {
                previousPane = it
            }
        }
        return 0
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal fun <Role : PaneScaffoldRole> PaneScaffoldMotionDataProvider<Role>
    .getHiddenPaneCurrentLeft(role: Role): Int {
    var currentLeft = 0
    forEach { paneRole, data ->
        // Find the right edge of the shown pane next to the left.
        if (paneRole == role) {
            return currentLeft
        }
        if (
            data.motion.type == PaneMotion.Type.Shown || data.motion.type == PaneMotion.Type.Exiting
        ) {
            currentLeft = data.currentRight
        }
    }
    return currentLeft
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@VisibleForTesting
internal fun <Role : PaneScaffoldRole> PaneScaffoldMotionDataProvider<Role>.getHidingPaneTargetLeft(
    role: Role
): Int {
    var targetLeft = 0
    forEach { paneRole, data ->
        // Find the right edge of the shown pane next to the left.
        if (paneRole == role) {
            return targetLeft
        }
        if (
            data.motion.type == PaneMotion.Type.Shown ||
                data.motion.type == PaneMotion.Type.Entering
        ) {
            targetLeft = data.targetRight
        }
    }
    return targetLeft
}

/**
 * Calculates the default [EnterTransition] of the pane associated to the given role when it's
 * showing. The [PaneMotion] and pane measurement data provided by [PaneScaffoldMotionDataProvider]
 * will be used to decide the transition type and relevant values like sliding offsets.
 *
 * @param role the role of the pane that is supposed to perform the [EnterTransition] when showing.
 */
@ExperimentalMaterial3AdaptiveApi
fun <Role : PaneScaffoldRole> PaneScaffoldMotionDataProvider<Role>.calculateDefaultEnterTransition(
    role: Role
) =
    when (this[role].motion) {
        PaneMotion.EnterFromLeft ->
            slideInHorizontally(PaneMotionDefaults.OffsetAnimationSpec) { slideInFromLeftOffset }
        PaneMotion.EnterFromLeftDelayed ->
            slideInHorizontally(PaneMotionDefaults.DelayedOffsetAnimationSpec) {
                slideInFromLeftOffset
            }
        PaneMotion.EnterFromRight ->
            slideInHorizontally(PaneMotionDefaults.OffsetAnimationSpec) { slideInFromRightOffset }
        PaneMotion.EnterFromRightDelayed ->
            slideInHorizontally(PaneMotionDefaults.DelayedOffsetAnimationSpec) {
                slideInFromRightOffset
            }
        PaneMotion.EnterWithExpand -> {
            expandHorizontally(PaneMotionDefaults.SizeAnimationSpec, Alignment.CenterHorizontally) +
                slideInHorizontally(PaneMotionDefaults.OffsetAnimationSpec) {
                    getHiddenPaneCurrentLeft(role) - this[role].targetLeft
                }
        }
        PaneMotion.EnterAsModal -> {
            fadeIn(animationSpec = PaneMotionDefaults.VisibilityAnimationSpec)
        }
        else -> EnterTransition.None
    }

/**
 * Calculates the default [ExitTransition] of the pane associated to the given role when it's
 * hiding. The [PaneMotion] and pane measurement data provided by [PaneScaffoldMotionDataProvider]
 * will be used to decide the transition type and relevant values like sliding offsets.
 *
 * @param role the role of the pane that is supposed to perform the [ExitTransition] when hiding.
 */
@ExperimentalMaterial3AdaptiveApi
fun <Role : PaneScaffoldRole> PaneScaffoldMotionDataProvider<Role>.calculateDefaultExitTransition(
    role: Role
) =
    when (this[role].motion) {
        PaneMotion.ExitToLeft ->
            slideOutHorizontally(PaneMotionDefaults.OffsetAnimationSpec) { slideOutToLeftOffset }
        PaneMotion.ExitToRight ->
            slideOutHorizontally(PaneMotionDefaults.OffsetAnimationSpec) { slideOutToRightOffset }
        PaneMotion.ExitWithShrink -> {
            shrinkHorizontally(PaneMotionDefaults.SizeAnimationSpec, Alignment.CenterHorizontally) +
                slideOutHorizontally(PaneMotionDefaults.OffsetAnimationSpec) {
                    getHidingPaneTargetLeft(role) - this[role].currentLeft
                }
        }
        PaneMotion.ExitAsModal -> {
            fadeOut(animationSpec = PaneMotionDefaults.VisibilityAnimationSpec)
        }
        else -> ExitTransition.None
    }

/** Interface to specify a custom pane enter/exit motion when a pane's visibility changes. */
@ExperimentalMaterial3AdaptiveApi
@Stable
sealed interface PaneMotion {
    /** The type of the motion, like exiting, entering, etc. See [Type]. */
    val type: Type

    /**
     * Indicates the current type of pane motion, like if the pane is entering or exiting, or is
     * kept showing or hidden.
     */
    @ExperimentalMaterial3AdaptiveApi
    @JvmInline
    value class Type private constructor(val value: Int) {
        override fun toString(): String {
            return "PaneMotion.Type[${
                when(this) {
                    Hidden -> "Hidden"
                    Exiting -> "Exiting"
                    Entering -> "Entering"
                    Shown -> "Shown"
                    ExitingModal -> "ExitingModal"
                    EnteringModal -> "EnteringModal"
                    else -> "Unknown value=$value"
                }
            }]"
        }

        companion object {
            /** Indicates the pane is kept hidden during the current motion. */
            val Hidden = Type(0)

            /** Indicates the pane is exiting or hiding during the current motion. */
            val Exiting = Type(1)

            /** Indicates the pane is entering or showing during the current motion. */
            val Entering = Type(2)

            /** Indicates the pane is keeping being shown during the current motion. */
            val Shown = Type(3)

            /**
             * Indicates the pane is exiting or hiding as a modal, i.e., a levitated pane, during
             * the current motion.
             */
            val ExitingModal = Type(5)

            /**
             * Indicates the pane is entering or showing as a modal, i.e., a levitated pane, during
             * the current motion.
             */
            val EnteringModal = Type(6)

            internal fun calculate(
                previousValue: PaneAdaptedValue,
                currentValue: PaneAdaptedValue,
            ): Type {
                val wasShown = previousValue != PaneAdaptedValue.Hidden
                val isShown = currentValue != PaneAdaptedValue.Hidden
                val levitatedPane =
                    previousValue is PaneAdaptedValue.Levitated ||
                        currentValue is PaneAdaptedValue.Levitated
                return when {
                    wasShown && isShown -> Shown
                    !wasShown && !isShown -> Hidden
                    wasShown && !isShown -> if (levitatedPane) ExitingModal else Exiting
                    !wasShown && isShown -> if (levitatedPane) EnteringModal else Entering
                    else -> Hidden // Not possible
                }
            }
        }
    }

    @Immutable
    private class DefaultImpl(val name: String, override val type: Type) : PaneMotion {
        override fun toString() = name
    }

    companion object {
        /** The default pane motion that no animation will be performed. */
        val NoMotion: PaneMotion = DefaultImpl("NoMotion", Type.Hidden)

        /**
         * The default pane motion that will animate panes bounds with the given animation specs
         * during motion. Note that this should only be used when the associated pane is keeping
         * showing during the motion.
         */
        val AnimateBounds: PaneMotion = DefaultImpl("AnimateBounds", Type.Shown)

        /**
         * The default pane motion that will slide panes in from left. Note that this should only be
         * used when the associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromLeft: PaneMotion = DefaultImpl("EnterFromLeft", Type.Entering)

        /**
         * The default pane motion that will slide panes in from right. Note that this should only
         * be used when the associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromRight: PaneMotion = DefaultImpl("EnterFromRight", Type.Entering)

        /**
         * The default pane motion that will slide panes in from left with a delay, usually to avoid
         * the interference of other exiting panes. Note that this should only be used when the
         * associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromLeftDelayed: PaneMotion = DefaultImpl("EnterFromLeftDelayed", Type.Entering)

        /**
         * The default pane motion that will slide panes in from right with a delay, usually to
         * avoid the interference of other exiting panes. Note that this should only be used when
         * the associated pane is entering - i.e. becoming visible from a hidden state.
         */
        val EnterFromRightDelayed: PaneMotion = DefaultImpl("EnterFromRightDelayed", Type.Entering)

        /**
         * The default pane motion that will slide panes out to left. Note that this should only be
         * used when the associated pane is exiting - i.e. becoming hidden from a visible state.
         */
        val ExitToLeft: PaneMotion = DefaultImpl("ExitToLeft", Type.Exiting)

        /**
         * The default pane motion that will slide panes out to right. Note that this should only be
         * used when the associated pane is exiting - i.e. becoming hidden from a visible state.
         */
        val ExitToRight: PaneMotion = DefaultImpl("ExitToRight", Type.Exiting)

        /**
         * The default pane motion that will expand panes from a zero size. Note that this should
         * only be used when the associated pane is entering - i.e. becoming visible from a hidden
         * state.
         */
        val EnterWithExpand: PaneMotion = DefaultImpl("EnterWithExpand", Type.Entering)

        /**
         * The default pane motion that will shrink panes until it's gone. Note that this should
         * only be used when the associated pane is exiting - i.e. becoming hidden from a visible
         * state.
         */
        val ExitWithShrink: PaneMotion = DefaultImpl("ExitWithShrink", Type.Exiting)

        /**
         * The default pane motion that will show the pane as a modal. Note that this should only be
         * used when the associated pane is entering into a levitated state from a hidden state.
         */
        val EnterAsModal: PaneMotion = DefaultImpl("EnterAsModal", Type.EnteringModal)

        /**
         * The default pane motion that will hide the pane as a modal. Note that this should only be
         * used when the associated pane is exiting from a leviated state to a hidden state.
         */
        val ExitAsModal: PaneMotion = DefaultImpl("ExitAsModal", Type.ExitingModal)
    }
}

@ExperimentalMaterial3AdaptiveApi
internal fun <Role : PaneScaffoldRole> calculatePaneMotion(
    previousScaffoldValue: PaneScaffoldValue<Role>,
    currentScaffoldValue: PaneScaffoldValue<Role>,
    paneOrder: PaneScaffoldHorizontalOrder<Role>,
): List<PaneMotion> {
    val numOfPanes = paneOrder.size
    val paneMotionTypes = Array(numOfPanes) { PaneMotion.Type.Hidden }
    val paneMotions = MutableList(numOfPanes) { PaneMotion.NoMotion }
    var firstShownPaneIndex = numOfPanes
    var firstEnteringPaneIndex = numOfPanes
    var lastShownPaneIndex = -1
    var lastEnteringPaneIndex = -1
    // First pass, to decide the entering/exiting status of each pane, and collect info for
    // deciding, given a certain pane, if there's a pane on its left or on its right that is
    // entering or keep showing during the transition.
    // Also set up the motions of all panes that keep showing to AnimateBounds.
    paneOrder.forEachIndexed { i, role ->
        paneMotionTypes[i] =
            PaneMotion.Type.calculate(previousScaffoldValue[role], currentScaffoldValue[role])
        when (paneMotionTypes[i]) {
            PaneMotion.Type.Shown -> {
                firstShownPaneIndex = min(firstShownPaneIndex, i)
                lastShownPaneIndex = max(lastShownPaneIndex, i)
                paneMotions[i] = PaneMotion.AnimateBounds
            }
            PaneMotion.Type.Entering -> {
                firstEnteringPaneIndex = min(firstEnteringPaneIndex, i)
                lastEnteringPaneIndex = max(lastEnteringPaneIndex, i)
            }
        }
    }
    // Second pass, to decide the exiting motions of all exiting panes.
    // Also collects info for the next pass to decide the entering motions of entering panes.
    var hasPanesExitToRight = false
    var hasPanesExitToLeft = false
    var firstPaneExitToRightIndex = numOfPanes
    var lastPaneExitToLeftIndex = -1
    paneOrder.forEachIndexed { i, _ ->
        val hasShownPanesOnLeft = firstShownPaneIndex < i
        val hasEnteringPanesOnLeft = firstEnteringPaneIndex < i
        val hasShownPanesOnRight = lastShownPaneIndex > i
        val hasEnteringPanesOnRight = lastEnteringPaneIndex > i
        if (paneMotionTypes[i] == PaneMotion.Type.Exiting) {
            paneMotions[i] =
                if (!hasShownPanesOnRight && !hasEnteringPanesOnRight) {
                    // No panes will interfere the motion on the right, exit to right.
                    hasPanesExitToRight = true
                    firstPaneExitToRightIndex = min(firstPaneExitToRightIndex, i)
                    PaneMotion.ExitToRight
                } else if (!hasShownPanesOnLeft && !hasEnteringPanesOnLeft) {
                    // No panes will interfere the motion on the left, exit to left.
                    hasPanesExitToLeft = true
                    lastPaneExitToLeftIndex = max(lastPaneExitToLeftIndex, i)
                    PaneMotion.ExitToLeft
                } else if (!hasShownPanesOnRight) {
                    // Only showing panes can interfere the motion on the right, exit to right.
                    hasPanesExitToRight = true
                    firstPaneExitToRightIndex = min(firstPaneExitToRightIndex, i)
                    PaneMotion.ExitToRight
                } else if (!hasShownPanesOnLeft) { // Only showing panes on left
                    // Only showing panes can interfere the motion on the left, exit to left.
                    hasPanesExitToLeft = true
                    lastPaneExitToLeftIndex = max(lastPaneExitToLeftIndex, i)
                    PaneMotion.ExitToLeft
                } else {
                    // Both sides has panes that keep being visible during transition, shrink to
                    // exit
                    PaneMotion.ExitWithShrink
                }
        }
    }
    // Third pass, to decide the entering motions of all entering panes.
    paneOrder.forEachIndexed { i, _ ->
        val hasShownPanesOnLeft = firstShownPaneIndex < i
        val hasShownPanesOnRight = lastShownPaneIndex > i
        val hasLeftPaneExitToRight = firstPaneExitToRightIndex < i
        val hasRightPaneExitToLeft = lastPaneExitToLeftIndex > i
        // For a given pane, if there's another pane that keeps showing on its right, or there's
        // a pane on its right that's exiting to its left, the pane cannot enter from right since
        // doing so will either interfere with the showing pane, or cause incorrect order of the
        // pane position during the transition. In other words, this case is considered "blocking".
        // Same on the other side.
        val noBlockingPanesOnRight = !hasShownPanesOnRight && !hasRightPaneExitToLeft
        val noBlockingPanesOnLeft = !hasShownPanesOnLeft && !hasLeftPaneExitToRight
        if (paneMotionTypes[i] == PaneMotion.Type.Entering) {
            paneMotions[i] =
                if (noBlockingPanesOnRight && !hasPanesExitToRight) {
                    // No panes will block the motion on the right, enter from right.
                    PaneMotion.EnterFromRight
                } else if (noBlockingPanesOnLeft && !hasPanesExitToLeft) {
                    // No panes will block the motion on the left, enter from left.
                    PaneMotion.EnterFromLeft
                } else if (noBlockingPanesOnRight) {
                    // Only hiding panes can interfere the motion on the right, enter from right.
                    PaneMotion.EnterFromRightDelayed
                } else if (noBlockingPanesOnLeft) {
                    // Only hiding panes can interfere the motion on the left, enter from left.
                    PaneMotion.EnterFromLeftDelayed
                } else {
                    // Both sides has panes that keep being visible during transition, expand to
                    // enter
                    PaneMotion.EnterWithExpand
                }
        }
    }
    // Fourth pass, to decide the motions of all levitated panes.
    paneOrder.forEachIndexed { i, _ ->
        if (paneMotionTypes[i] == PaneMotion.Type.EnteringModal) {
            paneMotions[i] = PaneMotion.EnterAsModal
        } else if (paneMotionTypes[i] == PaneMotion.Type.ExitingModal) {
            paneMotions[i] = PaneMotion.ExitAsModal
        }
    }
    return paneMotions
}

internal val IntRectToVector: TwoWayConverter<IntRect, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = {
            AnimationVector4D(
                it.left.toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(),
            )
        },
        convertFromVector = {
            IntRect(
                it.v1.fastRoundToInt(),
                it.v2.fastRoundToInt(),
                it.v3.fastRoundToInt(),
                it.v4.fastRoundToInt(),
            )
        },
    )

internal class DerivedSizeAnimationSpec(private val boundsSpec: FiniteAnimationSpec<IntRect>) :
    FiniteAnimationSpec<IntSize> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<IntSize, V>
    ): VectorizedFiniteAnimationSpec<V> =
        boundsSpec.vectorize(
            object : TwoWayConverter<IntRect, V> {
                override val convertFromVector: (V) -> IntRect = { vector ->
                    with(converter.convertFromVector(vector)) { IntRect(0, 0, width, height) }
                }
                override val convertToVector: (IntRect) -> V = { bounds ->
                    converter.convertToVector(bounds.size)
                }
            }
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedSizeAnimationSpec) return false
        return boundsSpec == other.boundsSpec
    }

    override fun hashCode(): Int = boundsSpec.hashCode()
}

internal class DerivedOffsetAnimationSpec(private val boundsSpec: FiniteAnimationSpec<IntRect>) :
    FiniteAnimationSpec<IntOffset> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<IntOffset, V>
    ): VectorizedFiniteAnimationSpec<V> =
        boundsSpec.vectorize(
            object : TwoWayConverter<IntRect, V> {
                override val convertFromVector: (V) -> IntRect = { vector ->
                    with(converter.convertFromVector(vector)) { IntRect(x, y, x, y) }
                }
                override val convertToVector: (IntRect) -> V = { bounds ->
                    converter.convertToVector(bounds.topLeft)
                }
            }
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedOffsetAnimationSpec) return false
        return boundsSpec == other.boundsSpec
    }

    override fun hashCode(): Int = boundsSpec.hashCode()
}

internal class DelayedSpringSpec<T>(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    private val delayedRatio: Float,
    visibilityThreshold: T? = null,
) : FiniteAnimationSpec<T> {
    private val originalSpringSpec = spring(dampingRatio, stiffness, visibilityThreshold)

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> =
        DelayedVectorizedSpringSpec(originalSpringSpec.vectorize(converter), delayedRatio)
}

private class DelayedVectorizedSpringSpec<V : AnimationVector>(
    val originalVectorizedSpringSpec: VectorizedFiniteAnimationSpec<V>,
    val delayedRatio: Float,
) : VectorizedFiniteAnimationSpec<V> {
    var delayedTimeNanos: Long = 0
    var cachedInitialValue: V? = null
    var cachedTargetValue: V? = null
    var cachedInitialVelocity: V? = null
    var cachedOriginalDurationNanos: Long = 0

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return if (playTimeNanos <= delayedTimeNanos) {
            initialValue
        } else {
            originalVectorizedSpringSpec.getValueFromNanos(
                playTimeNanos - delayedTimeNanos,
                initialValue,
                targetValue,
                initialVelocity,
            )
        }
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return if (playTimeNanos <= delayedTimeNanos) {
            initialVelocity
        } else {
            originalVectorizedSpringSpec.getVelocityFromNanos(
                playTimeNanos - delayedTimeNanos,
                initialValue,
                targetValue,
                initialVelocity,
            )
        }
    }

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long {
        updateDelayedTimeNanosIfNeeded(initialValue, targetValue, initialVelocity)
        return cachedOriginalDurationNanos + delayedTimeNanos
    }

    private fun updateDelayedTimeNanosIfNeeded(
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ) {
        if (
            initialValue != cachedInitialValue ||
                targetValue != cachedTargetValue ||
                initialVelocity != cachedInitialVelocity
        ) {
            cachedOriginalDurationNanos =
                originalVectorizedSpringSpec.getDurationNanos(
                    initialValue,
                    targetValue,
                    initialVelocity,
                )
            delayedTimeNanos = (cachedOriginalDurationNanos * delayedRatio).toLong()
        }
    }
}
