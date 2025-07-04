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

package androidx.xr.compose.subspace.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.xr.compose.platform.LocalOpaqueEntity
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.entityName
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCompositionLocalMap
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.compose.subspace.rememberCoreGroupEntity
import androidx.xr.scenecore.GroupEntity

/**
 * [SubspaceLayout] is the main component for laying out leaf nodes with zero children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
 *
 * Example:
 * ```kotlin
 * fun ExactSizeSpacer(size: IntVolumeSize) {
 *   SubspaceLayout(SubspaceModifier.testTag("exactSizeSpacer")) {
 *     _, _ -> layout(size.width, size.height, size.depth) {}
 *   }
 * }
 * ```
 *
 * @param modifier SubspaceModifier to apply during layout.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("NOTHING_TO_INLINE")
@SubspaceComposable
@Composable
public inline fun SubspaceLayout(
    modifier: SubspaceModifier = SubspaceModifier,
    measurePolicy: SubspaceMeasurePolicy,
) {
    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(modifier, SetModifier)
        },
    )
}

/**
 * [SubspaceLayout] is the main core component for layout. It can be used to measure and position
 * zero or more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
 *
 * Example:
 * ```kotlin
 * fun MyLayout(
 *     modifier: SubspaceModifier = SubspaceModifier,
 *     content: @SubspaceComposable @Composable () -> Unit) {
 *   SubspaceLayout(content = content, modifier = modifier) {
 *     measurables, constraints ->
 *     val placeables = measurables.map { it.measure(constraints) }
 *     layout(constraints.maxWidth, constraints.maxHeight, constraints.maxDepth) {
 *       placeables.forEach { it.place(Pose.Identity) }
 *     }
 *   }
 * }
 * ```
 *
 * @param modifier SubspaceModifier to apply during layout
 * @param content the child composables to be laid out.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("ComposableLambdaParameterPosition", "NOTHING_TO_INLINE")
@SubspaceComposable
@Composable
public inline fun SubspaceLayout(
    crossinline content: @Composable @SubspaceComposable () -> Unit,
    modifier: SubspaceModifier = SubspaceModifier,
    measurePolicy: SubspaceMeasurePolicy,
) {
    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }
    val coreEntity = rememberCoreGroupEntity {
        GroupEntity.create(session = this, name = entityName("Entity"))
    }
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(coreEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
        },
        content = { CompositionLocalProvider(LocalOpaqueEntity provides coreEntity) { content() } },
    )
}

/**
 * [SubspaceLayout] is the main core component for layout for "leaf" nodes. It can be used to
 * measure and position zero children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
 *
 * @param modifier SubspaceModifier to apply during layout.
 * @param coreEntity SceneCore Entity being placed in this layout. This parameter is generally not
 *   needed for most use cases and should be avoided unless you have specific requirements to manage
 *   entities outside the Compose framework. If provided, it will associate the [SubspaceLayout]
 *   with the given SceneCore Entity.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("NOTHING_TO_INLINE")
@SubspaceComposable
@Composable
internal inline fun SubspaceLayout(
    modifier: SubspaceModifier = SubspaceModifier,
    coreEntity: CoreEntity? = null,
    measurePolicy: SubspaceMeasurePolicy,
) {
    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(coreEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
        },
    )
}

/**
 * [SubspaceLayout] is the main core component for layout. It can be used to measure and position
 * zero or more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
 *
 * @param modifier SubspaceModifier to apply during layout
 * @param coreEntity SceneCore Entity being placed in this layout. This parameter is generally not
 *   needed for most use cases and should be avoided unless you have specific requirements to manage
 *   entities outside the Compose framework. If provided, it will associate the [SubspaceLayout]
 *   with the given SceneCore Entity.
 * @param content the children composable to be laid out.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("ComposableLambdaParameterPosition", "NOTHING_TO_INLINE")
@SubspaceComposable
@Composable
internal inline fun SubspaceLayout(
    crossinline content: @Composable @SubspaceComposable () -> Unit,
    modifier: SubspaceModifier = SubspaceModifier,
    coreEntity: CoreEntity = rememberCoreGroupEntity {
        GroupEntity.create(session = this, name = entityName("Entity"))
    },
    measurePolicy: SubspaceMeasurePolicy,
) {
    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }

    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(coreEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
        },
        content = { CompositionLocalProvider(LocalOpaqueEntity provides coreEntity) { content() } },
    )
}
