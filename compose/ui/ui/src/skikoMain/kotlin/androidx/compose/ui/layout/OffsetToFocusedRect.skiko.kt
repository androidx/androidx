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

package androidx.compose.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.animation.withAnimationProgress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrDefault
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration

@Composable
internal fun OffsetToFocusedRect(
    insets: PlatformInsets,
    getFocusedRect: () -> Rect?,
    size: IntSize?,
    animationDuration: Duration,
    animationCompletion: () -> Unit,
    content: @Composable () -> Unit,
) {
    var currentOffset by remember { mutableStateOf(IntOffset.Zero) }
    var startOffset by remember { mutableStateOf(IntOffset.Zero) }
    var offsetProgress by remember { mutableStateOf(1f) }
    val density = LocalDensity.current

    fun translatedFocusedRect(): Rect? {
        // The current implementation of OffsetToFocusedRect assumes that the focus rectangle is
        // either static or only changes during the animation.
        // The [Snapshot.withoutReadObservation] function is used here to avoid side effects caused
        // by the internal implementation of the [getFocusedRect] function.
        val rect = Snapshot.withoutReadObservation { getFocusedRect() }
        return rect?.translate(-currentOffset.toOffset())
    }

    LaunchedEffect(insets, animationDuration) {
        startOffset = currentOffset
        if (animationDuration.isPositive()) {
            if (startOffset == density.adjustedToFocusedRectOffset(
                    insets = insets,
                    focusedRect = translatedFocusedRect(),
                    size = size
                )
            ) {
                offsetProgress = 1f
            } else {
                withAnimationProgress(animationDuration) {
                    offsetProgress = it
                }
            }
            animationCompletion()
        } else {
            offsetProgress = 1f
        }
    }

    Layout(
        content = content,
        measurePolicy = { measurables, constraints ->
            val endOffset = density.adjustedToFocusedRectOffset(
                insets = insets,
                focusedRect = translatedFocusedRect(),
                size = size
            )

            // Intentionally update state within composition to trigger second measure and
            // layout because focus rect may be miscalculated due to simultaneous offset and
            // window insets changes.
            //
            // FIXME: this "second measure" only settles in-frame because BaseComposeScene.draw()
            //  currently calls Snapshot.sendApplyNotifications() between the measure and draw phases -
            //  a temporary, un-Android workaround kept solely for this code path.
            currentOffset = startOffset + (endOffset - startOffset) * offsetProgress

            val placeables = measurables.fastMap { it.measure(constraints) }
            layout(
                placeables.fastMaxOfOrDefault(constraints.minWidth) { it.width },
                placeables.fastMaxOfOrDefault(constraints.minHeight) { it.height }
            ) {
                placeables.fastForEach {
                    it.place(currentOffset)
                }
            }
        }
    )
}

internal fun Density.adjustedToFocusedRectOffset(
    insets: PlatformInsets,
    focusedRect: Rect?,
    size: IntSize?
): IntOffset {
    focusedRect ?: return IntOffset.Zero
    size ?: return IntOffset.Zero
    if (insets == PlatformInsets.Zero) {
        return IntOffset.Zero
    }

    val visibleAfterOffset = Rect(offset = Offset.Zero, size = size.toSize())
        .intersect(focusedRect)
        .isEmpty
        .not()

    return if (visibleAfterOffset) {
        IntOffset(
            x = directionalFocusOffset(
                contentSize = size.width.toFloat(),
                contentInsetStart = insets.left.toFloat(),
                contentInsetEnd = insets.right.toFloat(),
                focusStart = focusedRect.left,
                focusEnd = focusedRect.right
            ),
            y = directionalFocusOffset(
                contentSize = size.height.toFloat(),
                contentInsetStart = insets.top.toFloat(),
                contentInsetEnd = insets.bottom.toFloat(),
                focusStart = focusedRect.top,
                focusEnd = focusedRect.bottom,
            )
        )
    } else {
        IntOffset.Zero
    }
}

private fun directionalFocusOffset(
    contentSize: Float,
    contentInsetStart: Float,
    contentInsetEnd: Float,
    focusStart: Float,
    focusEnd: Float
): Int {
    val hiddenFromPart = contentInsetStart - max(focusStart, 0f)
    val hiddenToPart = contentInsetEnd - contentSize + min(focusEnd, contentSize)

    return if (hiddenFromPart >= 0 && hiddenToPart >= 0) {
        0
    } else if (hiddenToPart < 0) {
        max(0f, min(hiddenFromPart, -hiddenToPart)).roundToInt()
    } else {
        min(0f, max(hiddenFromPart, -hiddenToPart)).roundToInt()
    }
}
