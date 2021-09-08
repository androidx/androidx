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

package androidx.wear.compose.material

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// TODO(jnichol): Add pagination indication to the scaffold
/**
 * Scaffold implements the basic Wear Material Design visual layout structure.
 *
 * This component provides API to put together several material components to construct your
 * screen, by ensuring proper layout strategy for them and collecting necessary data so these
 * components will work together correctly.
 *
 * The Scaffold provides the main application structure in a Wear Material application. It provides
 * slots for the different parts of the application and sensible defaults were appropriate.
 *
 * The layout of the Wear Scaffold is typically z-layered with decorations such as
 * [PositionIndicator] and [Vignette] applied in the order laid out in the Wear Material Design
 * guidance.
 *
 * Simple example of a Scaffold with a [ScalingLazyColumn] as the main application content and a
 * scroll indicator to show the position of the items in the ScalingLazyColumn as.
 *
 * @sample androidx.wear.compose.material.samples.SimpleScaffoldWithScrollIndicator
 *
 * @param modifier optional Modifier for the root of the [Scaffold]
 * @param vignette a full screen slot for applying a vignette over the contents of the scaffold. The
 * vignette is used to blur the screen edges when the main content is scrollable content that
 * extends beyond the screen edge.
 * @param positionIndicator slot for optional position indicator used to display information about
 * the position of the Scaffolds contents. Usually a [PositionIndicator]. Common use cases for the
 * position indicator are scroll indication for a list or rsb/bezel indication such as volume.
 * @param timeText time and potential application status message to display at the top middle of the
 * screen. Expected to be a TimeText component.
 */
@Composable
public fun Scaffold(
    modifier: Modifier = Modifier,
    vignette: @Composable (() -> Unit)? = null,
    positionIndicator: @Composable (() -> Unit)? = null,
    timeText: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {

    Box(modifier = modifier) {
        content()

        if (positionIndicator != null) {
            Box(
                modifier = Modifier.align(
                    alignment = if (LocalLayoutDirection.current == LayoutDirection.Ltr)
                        Alignment.CenterEnd else Alignment.CenterStart
                )
            ) {
                positionIndicator()
            }
        }

        if (vignette != null) {
            vignette()
        }

        if (timeText != null) {
            Box(
                modifier = Modifier.align(alignment = Alignment.TopCenter)
            ) {
                timeText()
            }
        }
    }
}
