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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.LocalComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.SkiaBasedOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.requireCurrent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.min

/**
 * Adding [content] as root layout to separate [androidx.compose.ui.node.Owner].
 */
@Composable
internal fun RootLayout(
    modifier: Modifier,
    focusable: Boolean,
    onOutsidePointerEvent: ((PointerInputEvent) -> Unit)? = null,
    content: @Composable (SkiaBasedOwner) -> Unit
) {
    val scene = LocalComposeScene.requireCurrent()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val parentComposition = rememberCompositionContext()
    val (owner, composition) = remember {
        val owner = SkiaBasedOwner(
            scene = scene,
            platform = scene.platform,
            coroutineContext = parentComposition.effectCoroutineContext,
            initDensity = density,
            initLayoutDirection = layoutDirection,
            focusable = focusable,
            onOutsidePointerEvent = onOutsidePointerEvent,
            onPointerUpdate = scene::onPointerUpdate,
            modifier = modifier
        )
        scene.attach(owner)
        owner to owner.setContent(parent = parentComposition) {
            content(owner)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            scene.detach(owner)
            composition.dispose()
            owner.dispose()
        }
    }
    SideEffect {
        owner.density = density
        owner.layoutDirection = layoutDirection
    }
}

@Composable
internal fun EmptyLayout(
    modifier: Modifier = Modifier
) = Layout(
    content = {},
    modifier = modifier,
    measurePolicy = { _, _ ->
        layout(0, 0) {}
    }
)

internal fun RootMeasurePolicy(
    platformInsets: PlatformInsets,
    usePlatformDefaultWidth: Boolean,
    calculatePosition: MeasureScope.(windowSize: IntSize, contentSize: IntSize) -> IntOffset,
) = MeasurePolicy {measurables, constraints ->
    val platformConstraints = applyPlatformConstrains(
        constraints, platformInsets, usePlatformDefaultWidth
    )
    val placeables = measurables.fastMap { it.measure(platformConstraints) }
    val windowSize = IntSize(constraints.maxWidth, constraints.maxHeight)
    val contentSize = IntSize(
        width = placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth,
        height = placeables.fastMaxBy { it.height }?.height ?: constraints.minHeight
    )
    val position = calculatePosition(windowSize, contentSize)
    layout(windowSize.width, windowSize.height) {
        placeables.fastForEach {
            it.place(position.x, position.y)
        }
    }
}

private fun Density.applyPlatformConstrains(
    constraints: Constraints,
    platformInsets: PlatformInsets,
    usePlatformDefaultWidth: Boolean
): Constraints {
    val horizontal = platformInsets.left.roundToPx() + platformInsets.right.roundToPx()
    val vertical = platformInsets.top.roundToPx() + platformInsets.bottom.roundToPx()
    val platformConstraints = constraints.offset(-horizontal, -vertical)
    return if (usePlatformDefaultWidth) {
        platformConstraints.constrain(
            platformDefaultConstrains(constraints)
        )
    } else {
        platformConstraints
    }
}

internal fun MeasureScope.positionWithInsets(
    insets: PlatformInsets,
    size: IntSize,
    calculatePosition: (size: IntSize) -> IntOffset,
): IntOffset {
    val horizontal = insets.left.roundToPx() + insets.right.roundToPx()
    val vertical = insets.top.roundToPx() + insets.bottom.roundToPx()
    val sizeWithPadding = IntSize(
        width = size.width - horizontal,
        height = size.height - vertical
    )
    val position = calculatePosition(sizeWithPadding)
    val offset = IntOffset(
        x = insets.left.roundToPx(),
        y = insets.top.roundToPx()
    )
    return position + offset
}

private fun Density.platformDefaultConstrains(
    constraints: Constraints
): Constraints = constraints.copy(
    maxWidth = min(preferredDialogWidth(constraints), constraints.maxWidth)
)

// Ported from Android. See https://cs.android.com/search?q=abc_config_prefDialogWidth
private fun Density.preferredDialogWidth(constraints: Constraints): Int {
    val smallestWidth = min(constraints.maxWidth, constraints.maxHeight).toDp()
    return when {
        smallestWidth >= 600.dp -> 580.dp
        smallestWidth >= 480.dp -> 440.dp
        else -> 320.dp
    }.roundToPx()
}
