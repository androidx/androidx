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

package androidx.navigation.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackEventCompat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.internal.DefaultNavTransitions

@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun NavHost(
    navController: NavHostController,
    graph: NavGraph,
    modifier: Modifier,
    contentAlignment: Alignment,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition),
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition),
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition),
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition),
    sizeTransform: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> SizeTransform?)?
) {
    val isDefaultTransition = enterTransition == DefaultNavTransitions.enterTransition &&
        exitTransition == DefaultNavTransitions.exitTransition &&
        popEnterTransition == DefaultNavTransitions.enterTransition &&
        popExitTransition == DefaultNavTransitions.exitTransition &&
        sizeTransform == DefaultNavTransitions.sizeTransform

    if (isDefaultTransition) {
        val iosBlackout = @Composable fun BoxScope.(isBackAnimation: Boolean, progress: Float) {
            val blackoutFraction = if (isBackAnimation) 1 - progress else progress
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRect(Color.Black, alpha = 0.106f * blackoutFraction)
                    }
            )
        }

        val backEventEdge = when (LocalLayoutDirection.current) {
            LayoutDirection.Ltr -> BackEventCompat.EDGE_LEFT
            LayoutDirection.Rtl -> BackEventCompat.EDGE_RIGHT
        }
        NavHost(
            navController,
            graph,
            modifier,
            contentAlignment,
            enterTransition,
            exitTransition,
            DefaultNavTransitions.popEnterTransition,
            DefaultNavTransitions.popExitTransition,
            sizeTransform,
            drawOnBottomEntryDuringAnimation = iosBlackout,
            limitBackGestureSwipeEdge = backEventEdge
        )
    } else {
        NavHost(
            navController,
            graph,
            modifier,
            contentAlignment,
            enterTransition,
            exitTransition,
            popEnterTransition,
            popExitTransition,
            sizeTransform,
            null,
            null
        )
    }
}
