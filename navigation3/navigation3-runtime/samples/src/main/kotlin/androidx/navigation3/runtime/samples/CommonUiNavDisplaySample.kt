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

package androidx.navigation3.runtime.samples

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Icon
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.samples.CommonUiNavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND
import androidx.navigation3.runtime.samples.CommonUiNavDisplay.NAV_UI_LAYOUT_POLICY
import androidx.navigation3.runtime.samples.CommonUiNavDisplay.POP_TRANSITION_SPEC
import androidx.navigation3.runtime.samples.CommonUiNavDisplay.TRANSITION_SPEC

/** Object that indicates the features that can be handled by the [CommonUiNavDisplay] */
object CommonUiNavDisplay {
    internal const val TRANSITION_SPEC = "transitionSpec"
    internal const val POP_TRANSITION_SPEC = "popTransitionSpec"
    internal const val NAV_UI_LAYOUT_POLICY = "navUiLayoutPolicy"
    const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 700

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [CommonUiNavDisplay] that the
     * content should be animated using the provided [ContentTransform].
     */
    public fun transitionSpec(
        transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(TRANSITION_SPEC to transitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [CommonUiNavDisplay] that,
     * when popping from backstack, the content should be animated using the provided
     * [ContentTransform].
     */
    public fun popTransitionSpec(
        popTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(POP_TRANSITION_SPEC to popTransitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [CommonUiNavDisplay] that
     * updates the [NavUiLayoutPolicy].
     */
    fun layoutPolicy(value: NavUiLayoutPolicy = NavUiLayoutPolicy.Default): Map<String, Any> =
        mapOf(NAV_UI_LAYOUT_POLICY to value)
}

@Composable
fun <T : Any> CommonUiNavDisplay(
    backstack: List<T>,
    topLevelRoutes: List<TopLevelRoute>,
    onItemClick: (TopLevelRoute) -> Unit,
    modifier: Modifier = Modifier,
    entryDecorators: List<NavEntryDecorator<*>> = emptyList(),
    contentAlignment: Alignment = Alignment.TopStart,
    sizeTransform: SizeTransform? = null,
    transitionSpec: ContentTransform =
        ContentTransform(
            fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
            fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
            sizeTransform = null,
        ),
    popTransitionSpec: ContentTransform = transitionSpec,
    onBack: () -> Unit = { if (backstack is MutableList) backstack.removeAt(backstack.size - 1) },
    entryProvider: (key: T) -> NavEntry<out T>,
) {
    BackHandler(backstack.size > 1, onBack)
    val entries = rememberDecoratedNavEntries(backstack, entryDecorators, entryProvider)
    // Make a copy shallow copy so that transition.currentState and transition.targetState are
    // different backstack instances. This ensures currentState reflects the old backstack when
    // the backstack (targetState) is updated.
    val newStack = entries.map { it.contentKey }
    val entry = entries.last()
    val transition = updateTransition(targetState = newStack, label = newStack.toString())
    val isPop = isPop(transition.currentState, newStack)
    // Incoming entry defines transitions, otherwise it uses default transitions from
    // NavDisplay
    val contentTransform =
        if (isPop) {
            entry.metadata[POP_TRANSITION_SPEC] as? ContentTransform ?: popTransitionSpec
        } else {
            entry.metadata[TRANSITION_SPEC] as? ContentTransform ?: transitionSpec
        }
    transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = contentTransform.targetContentEnter,
                initialContentExit = contentTransform.initialContentExit,
                sizeTransform = contentTransform.sizeTransform,
            )
        },
        contentAlignment = contentAlignment,
        contentKey = { it.last() },
    ) { innerStack ->
        val lastKey = innerStack.last()
        val layoutPolicy =
            entry.metadata[NAV_UI_LAYOUT_POLICY] as? NavUiLayoutPolicy ?: NavUiLayoutPolicy.Default
        NavigationSuiteScaffold(
            layoutType = layoutPolicy.toLayoutType(),
            navigationSuiteItems = {
                topLevelRoutes.forEach { topLevelRoute ->
                    item(
                        selected = topLevelRoute.route == innerStack.last(),
                        onClick = { onItemClick(topLevelRoute) },
                        icon = { Icon(imageVector = topLevelRoute.icon, contentDescription = null) },
                    )
                }
            },
        ) {
            entries.findLast { entry -> entry.contentKey == lastKey }?.Content()
        }
    }
}

data class TopLevelRoute(val route: Any, val icon: ImageVector)

enum class NavUiLayoutPolicy {
    Default, // Default layout behavior
    None, // Never show the common UI
    NoNavBar; // Never show the nav bar

    @Composable
    fun toLayoutType(): NavigationSuiteType {
        val defaultLayout =
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
        return when (this) {
            Default -> defaultLayout
            None -> {
                NavigationSuiteType.None
            }
            NoNavBar -> {
                if (defaultLayout == NavigationSuiteType.NavigationBar) {
                    NavigationSuiteType.None
                } else {
                    defaultLayout
                }
            }
        }
    }
}

private fun <T : Any> isPop(oldBackStack: List<T>, newBackStack: List<T>): Boolean {
    // entire stack replaced
    if (oldBackStack.first() != newBackStack.first()) return false
    // navigated
    if (newBackStack.size > oldBackStack.size) return false
    val divergingIndex =
        newBackStack.indices.firstOrNull { index -> newBackStack[index] != oldBackStack[index] }
    // if newBackStack never diverged from oldBackStack, then it is a clean subset of the oldStack
    // and is a pop
    return divergingIndex == null
}
