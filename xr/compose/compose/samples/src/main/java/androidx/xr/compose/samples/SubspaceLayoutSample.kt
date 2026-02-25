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

package androidx.xr.compose.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.runtime.math.Pose

/** A data class representing a 3D size with integer dimensions. */
data class IntVolumeSize(val width: Int, val height: Int, val depth: Int)

/**
 * This sample demonstrates a [SubspaceLayout] with no content, which can be used as a spacer or a
 * placeholder in a 3D layout. It is the equivalent of a `Box` in traditional Compose.
 *
 * @param size The size of the spacer.
 */
@Sampled
@Composable
@SubspaceComposable
fun SubspaceLayoutWithoutContentSample(size: IntVolumeSize) {
    SubspaceLayout(
        modifier = SubspaceModifier.testTag("exactSizeSpacer"),
        measurePolicy =
            SubspaceMeasurePolicy { _, _ -> layout(size.width, size.height, size.depth) {} },
    )
}

/**
 * This sample demonstrates a [SubspaceLayout] with content. The content is measured and placed
 * according to the provided measure policy. In this case, it places all children at the origin.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The composable content to be laid out.
 */
@Sampled
@Composable
@SubspaceComposable
fun SubspaceLayoutWithContentSample(
    modifier: SubspaceModifier = SubspaceModifier,
    content: @SubspaceComposable @Composable () -> Unit,
) {
    SubspaceLayout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight, constraints.maxDepth) {
            placeables.forEach { it.place(Pose.Identity) }
        }
    }
}

/**
 * This sample demonstrates how to provide a `coreEntityName` to a [SubspaceLayout]. This name is
 * used for debugging and identification purposes in the 3D scene graph.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The composable content to be laid out.
 */
@Sampled
@Composable
@SubspaceComposable
fun SubspaceLayoutWithCoreEntityNameSample(
    modifier: SubspaceModifier = SubspaceModifier,
    content: @SubspaceComposable @Composable () -> Unit,
) {
    SubspaceLayout(
        content = content,
        modifier = modifier,
        coreEntityName = "MyCustomLayoutEntity",
        measurePolicy =
            SubspaceMeasurePolicy { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints) }
                layout(constraints.maxWidth, constraints.maxHeight, constraints.maxDepth) {
                    placeables.forEach { it.place(Pose.Identity) }
                }
            },
    )
}
