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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.layout

import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import androidx.collection.emptyIntSet
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.player.compose.embedded.LocalAnimatedVisibilityScope
import androidx.compose.remote.player.compose.embedded.LocalSharedTransitionScope
import androidx.compose.remote.player.compose.embedded.components.LocalStateBranchScope
import androidx.compose.remote.player.compose.embedded.components.RcPlayerComponent
import androidx.compose.remote.player.compose.embedded.components.StateBranchScope
import androidx.compose.remote.player.compose.embedded.components.ownBranchEnterExitSpec
import androidx.compose.remote.player.compose.embedded.doc.mapEasing
import androidx.compose.remote.player.compose.embedded.inspectable.rcComponentContentInspector
import androidx.compose.remote.player.compose.embedded.state.rememberRemoteIntAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun RcPlayerStateLayout(layout: StateLayout, modifier: Modifier) {
    val index by rememberRemoteIntAsState(layout.indexId)
    val children = remember(layout) { ArrayList<Component>().apply { layout.getComponents(this) } }

    if (children.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val targetIndex = index.coerceIn(0, children.size - 1)
    for (i in 0 until children.size) {
        val child = children[i]
        val vis = if (i == targetIndex) Component.Visibility.VISIBLE else Component.Visibility.GONE
        child.mVisibility = vis
    }

    val transitionConfig = rememberLayoutTransitionConfig(layout, children)
    val motionDuration = transitionConfig.duration
    val fadeDuration = transitionConfig.fadeDuration
    val easing = transitionConfig.easing

    if (transitionConfig.transitionDuration <= 0) {
        Box(
            modifier = modifier.rcComponentContentInspector(layout, forStateLayoutContent = true),
            contentAlignment = Alignment.Center,
        ) {
            // Branches switch instantly, so their components have no enter/exit to run.
            CompositionLocalProvider(LocalStateBranchScope provides null) {
                RcPlayerComponent(children[targetIndex])
            }
        }
        return
    }

    @Composable
    fun AnimatedStateContent(
        sharedTransitionScope: SharedTransitionScope?,
        contentModifier: Modifier,
    ) {
        AnimatedContent(
            targetState = targetIndex,
            modifier =
                contentModifier.rcComponentContentInspector(layout, forStateLayoutContent = true),
            contentAlignment = Alignment.Center,
            label = "RcPlayerStateLayout",
            transitionSpec = {
                // Branches whose components fade individually are not cross-faded as a whole, so
                // their slides and rotations are neither faded nor cut short by the branch fade.
                val enter =
                    if (transitionConfig.componentFades(targetState)) {
                        EnterTransition.None
                    } else {
                        fadeIn(
                            animationSpec = tween(durationMillis = fadeDuration, easing = easing)
                        )
                    }
                val exit =
                    if (transitionConfig.componentFades(initialState)) {
                        ExitTransition.None
                    } else {
                        fadeOut(
                            animationSpec = tween(durationMillis = fadeDuration, easing = easing)
                        )
                    }
                (enter togetherWith exit).using(
                    SizeTransform(clip = false) { _, _ ->
                        if (motionDuration <= 0) {
                            snap()
                        } else {
                            tween(durationMillis = motionDuration, easing = easing)
                        }
                    }
                )
            },
        ) { currentIndex ->
            val branchScope =
                remember(this, transitionConfig, currentIndex) {
                    StateBranchScope(
                        visibilityScope = this,
                        pairedSharedIds = transitionConfig.pairedSharedIds,
                        componentFades = transitionConfig.componentFades(currentIndex),
                        fadeRootIds = transitionConfig.fadeRootIds,
                        fadeDuration = fadeDuration,
                        fadeEasing = easing,
                    )
                }
            CompositionLocalProvider(
                LocalSharedTransitionScope provides sharedTransitionScope,
                LocalAnimatedVisibilityScope provides this@AnimatedContent,
                LocalStateBranchScope provides branchScope,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    RcPlayerComponent(children[currentIndex])
                }
            }
        }
    }

    if (transitionConfig.hasSharedElements) {
        SharedTransitionLayout(modifier = modifier) {
            AnimatedStateContent(
                sharedTransitionScope = this@SharedTransitionLayout,
                contentModifier = Modifier,
            )
        }
    } else {
        AnimatedStateContent(sharedTransitionScope = null, contentModifier = modifier)
    }
}

internal class LayoutTransitionConfig(
    /** Duration of the branch size change (and of shared element bounds). */
    val duration: Int,
    val easing: Easing,
    val hasSharedElements: Boolean,
    /** Animation IDs shared by components in more than one of the layout's children. */
    val pairedSharedIds: IntSet = emptyIntSet(),
    /**
     * How long a branch switch animates: the longest of [duration] and the enter/exit of every
     * component spec in the branches, so a spec with only a visibility duration still animates.
     * Zero switches branches instantly.
     */
    val transitionDuration: Int = duration,
    /** Duration of the branch cross-fade, or of the component fades that replace it. */
    val fadeDuration: Int = duration,
    /** Indices of the children whose components fade individually instead of as a whole. */
    private val componentFadeBranches: BooleanArray = BooleanArray(0),
    /**
     * IDs of the components that fade themselves in branches that are not cross-faded as a whole:
     * the outermost components whose subtree has no own non-fade enter/exit.
     */
    val fadeRootIds: IntSet = emptyIntSet(),
) {
    /** Whether the child at [index] fades its components individually. */
    fun componentFades(index: Int): Boolean = componentFadeBranches.getOrElse(index) { false }
}

@Composable
internal fun rememberLayoutTransitionConfig(
    layout: LayoutComponent,
    children: List<Component>,
): LayoutTransitionConfig {
    return remember(layout, children) {
        val layoutSpec =
            layout.componentModifiers?.list?.fastFirstOrNull { it is AnimationSpec }
                as? AnimationSpec ?: layout.animationSpec?.takeIf { it != AnimationSpec.DEFAULT }

        var childSpec: AnimationSpec? = null
        var hasSharedElements = false

        fun search(comp: Component, branchIds: MutableIntSet) {
            if (comp.animationId != -1 && comp.animationId != 0) {
                hasSharedElements = true
                branchIds.add(comp.animationId)
            }
            val s = comp.ownOrInheritedSpec()
            if (
                s != null &&
                    (childSpec == null ||
                        s.effectiveMotionDuration() > childSpec!!.effectiveMotionDuration())
            ) {
                childSpec = s
            }
            if (comp.isBranchContainer()) {
                (comp as LayoutComponent).childrenComponents.fastForEach { search(it, branchIds) }
            }
        }

        val seenIds = MutableIntSet()
        val pairedIds = MutableIntSet()
        children.fastForEach { child ->
            val branchIds = MutableIntSet()
            search(child, branchIds)
            branchIds.forEach { id -> if (!seenIds.add(id)) pairedIds.add(id) }
        }

        // Shared elements paired across branches animate their bounds, so they neither extend the
        // switch nor fade with the branch.
        var maxEnd = layoutSpec?.transitionEndMillis() ?: 0f
        val fadeRootIds = MutableIntSet()

        // Returns whether the subtree of [comp] has a component with its own non-fade enter/exit.
        // When [collect] is set, the outermost subtrees without one are recorded as fade roots.
        fun walk(comp: Component, collect: Boolean): Boolean {
            if (pairedIds.contains(comp.animationId)) return false
            comp.ownOrInheritedSpec()?.let { maxEnd = max(maxEnd, it.transitionEndMillis()) }
            val own = comp.ownBranchEnterExitSpec(pairedIds) != null
            if (!comp.isBranchContainer()) return own
            val kids = (comp as LayoutComponent).childrenComponents
            // A component running its own enter/exit carries its descendants with it.
            val collectKids = collect && !own
            var any = own
            val kidResults = BooleanArray(kids.size)
            kids.fastForEachIndexed { i, kid ->
                kidResults[i] = walk(kid, collectKids)
                if (kidResults[i]) any = true
            }
            if (any && collectKids) {
                kids.fastForEachIndexed { i, kid ->
                    if (!kidResults[i] && !pairedIds.contains(kid.animationId)) {
                        fadeRootIds.add(kid.componentId)
                    }
                }
            }
            return any
        }

        val componentFadeBranches = BooleanArray(children.size) { i -> walk(children[i], true) }

        val spec = layoutSpec ?: childSpec
        val duration = spec?.effectiveMotionDuration()?.toInt() ?: 300
        val transitionDuration = max(duration, maxEnd.toInt())
        val easing = mapEasing(spec?.motionEasingType ?: 0)
        LayoutTransitionConfig(
            duration = duration,
            easing = easing,
            hasSharedElements = transitionDuration > 0 && hasSharedElements,
            pairedSharedIds = pairedIds,
            transitionDuration = transitionDuration,
            fadeDuration = if (duration > 0) duration else transitionDuration,
            componentFadeBranches = componentFadeBranches,
            fadeRootIds = fadeRootIds,
        )
    }
}

/** The component's own animation spec, or the non-default one it inherits from its parent. */
private fun Component.ownOrInheritedSpec(): AnimationSpec? =
    (this as? LayoutComponent)?.componentModifiers?.list?.fastFirstOrNull { it is AnimationSpec }
        as? AnimationSpec ?: animationSpec?.takeIf { it != AnimationSpec.DEFAULT }

/** Whether the branch transition looks into this component's children. */
private fun Component.isBranchContainer(): Boolean =
    this is LayoutComponent && this !is StateLayout && this !is FitBoxLayout

internal fun AnimationSpec.effectiveMotionDuration(): Float =
    if (isAnimationEnabled) motionDuration else 0f

/** When the enter or exit of this spec ends, whichever is later, or 0 when it is disabled. */
private fun AnimationSpec.transitionEndMillis(): Float {
    if (!isAnimationEnabled) return 0f
    fun end(sequence: AnimationSpec.SEQUENCE): Float =
        if (sequence == AnimationSpec.SEQUENCE.CONCURRENT) {
            max(motionDuration, visibilityDuration)
        } else {
            // AFTER and BEFORE run the motion and visibility parts one after the other.
            motionDuration + visibilityDuration
        }
    return max(end(enterSequence), end(exitSequence))
}
