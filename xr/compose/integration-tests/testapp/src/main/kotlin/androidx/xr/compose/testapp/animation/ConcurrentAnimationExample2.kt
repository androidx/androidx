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

package androidx.xr.compose.testapp.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width

/*
 * Advanced animation example - Concurrent example
 * Panels animate size and position at the same time
 *
 */
@Composable
@SubspaceComposable
fun ConcurrentAnimationExample2(modifier: SubspaceModifier = SubspaceModifier) {
    var isShrink by remember { mutableStateOf(false) }
    val transition = updateTransition(isShrink)
    val animationDuration = 1000

    val parentWidth = 700.dp
    val parentHeight = 500.dp
    val initialBoxWidth = parentWidth / 2
    val initialBoxHeight = parentHeight
    val targetWidth = 200.dp
    val targetHeight = 100.dp

    val initialRightBoxOffsetX = initialBoxWidth / 2
    val initialRightBoxOffsetY = parentHeight - initialBoxHeight
    val targetRightBoxOffsetX = (parentWidth - targetWidth) / 2
    val targetRightBoxOffsetY = (parentHeight - targetHeight) / 2

    val initialLeftBoxOffsetX = -initialBoxWidth / 2
    val initialLeftBoxOffsetY = parentHeight - initialBoxHeight
    val targetLeftBoxOffsetX = -(parentWidth - targetWidth) / 2
    val targetLeftBoxOffsetY = -(parentHeight - targetHeight) / 2

    val leftBoxWidth by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetWidth
                    false -> initialBoxWidth
                }
            },
        )
    val leftBoxHeight by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetHeight
                    false -> initialBoxHeight
                }
            },
        )
    val leftBoxOffsetX by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetLeftBoxOffsetX
                    false -> initialLeftBoxOffsetX
                }
            },
        )
    val leftBoxOffsetY by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetLeftBoxOffsetY
                    false -> initialLeftBoxOffsetY
                }
            },
        )

    val rightBoxWidth by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetWidth
                    false -> initialBoxWidth
                }
            },
        )
    val rightBoxHeight by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetHeight
                    false -> initialBoxHeight
                }
            },
        )
    val rightBoxOffsetX by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetRightBoxOffsetX
                    false -> initialRightBoxOffsetX
                }
            },
        )
    val rightBoxOffsetY by
        transition.animateDp(
            transitionSpec = {
                when {
                    false isTransitioningTo true -> tween(animationDuration)
                    else -> spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)
                }
            },
            targetValueByState = { state ->
                when (state) {
                    true -> targetRightBoxOffsetY
                    false -> initialRightBoxOffsetY
                }
            },
        )

    SpatialColumn(modifier = modifier.padding(5.dp), alignment = SpatialAlignment.Center) {
        SpatialPanel {
            Button(onClick = { isShrink = !isShrink }) {
                Text(if (isShrink) "Expand" else "Shrink")
            }
        }
        SpatialBox(modifier = SubspaceModifier.width(parentWidth).height(parentHeight)) {
            SpatialPanel(
                modifier =
                    SubspaceModifier.offset(x = leftBoxOffsetX, y = leftBoxOffsetY)
                        .width(leftBoxWidth)
                        .height(leftBoxHeight)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Box 1")
                }
            }
            SpatialPanel(
                modifier =
                    SubspaceModifier.offset(x = rightBoxOffsetX, y = rightBoxOffsetY)
                        .width(rightBoxWidth)
                        .height(rightBoxHeight)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Box 2")
                }
            }
        }
    }
}
