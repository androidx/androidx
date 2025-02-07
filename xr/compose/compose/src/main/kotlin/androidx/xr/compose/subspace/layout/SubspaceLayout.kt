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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.xr.compose.platform.LocalCoreEntity
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetName
import androidx.xr.compose.subspace.node.SubspaceNodeApplier
import androidx.xr.compose.subspace.rememberCoreContentlessEntity
import androidx.xr.scenecore.ContentlessEntity

/**
 * [SubspaceLayout] is the main core component for layout for "leaf" nodes. It can be used to
 * measure and position zero children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * @param modifier SubspaceModifier to apply during layout.
 * @param name a name for the ComposeSubspaceNode. This can be useful for debugging.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@SubspaceComposable
@Composable
public fun SubspaceLayout(
    modifier: SubspaceModifier = SubspaceModifier,
    name: String = defaultSubspaceLayoutName(),
    measurePolicy: MeasurePolicy,
) {
    ComposeNode<ComposeSubspaceNode, SubspaceNodeApplier>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(modifier, SetModifier)
            set(name, SetName)
        },
    )
}

/**
 * [SubspaceLayout] is the main core component for layout. It can be used to measure and position
 * zero or more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * @param modifier SubspaceModifier to apply during layout
 * @param content the children composable to be laid out.
 * @param name a name for the ComposeSubspaceNode. This can be useful for debugging.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("ComposableLambdaParameterPosition")
@SubspaceComposable
@Composable
public fun SubspaceLayout(
    content: @Composable @SubspaceComposable () -> Unit,
    modifier: SubspaceModifier = SubspaceModifier,
    name: String = defaultSubspaceLayoutName(),
    measurePolicy: MeasurePolicy,
) {
    val coreEntity = rememberCoreContentlessEntity {
        ContentlessEntity.create(session = this, name = name)
    }
    ComposeNode<ComposeSubspaceNode, SubspaceNodeApplier>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(coreEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
            set(name, SetName)
        },
        content = {
            CompositionLocalProvider(LocalCoreEntity provides coreEntity, content = content)
        },
    )
}

/**
 * [SubspaceLayout] is the main core component for layout for "leaf" nodes. It can be used to
 * measure and position zero children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * @param modifier SubspaceModifier to apply during layout.
 * @param coreEntity SceneCore Entity being placed in this layout. This parameter is generally not
 *   needed for most use cases and should be avoided unless you have specific requirements to manage
 *   entities outside the Compose framework. If provided, it will associate the [SubspaceLayout]
 *   with the given SceneCore Entity.
 * @param name a name for the ComposeSubspaceNode. This can be useful for debugging.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@SubspaceComposable
@Composable
internal fun SubspaceLayout(
    modifier: SubspaceModifier = SubspaceModifier,
    name: String = defaultSubspaceLayoutName(),
    coreEntity: CoreEntity? = null,
    measurePolicy: MeasurePolicy,
) {
    ComposeNode<ComposeSubspaceNode, SubspaceNodeApplier>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(coreEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
            set(name, SetName)
        },
    )
}

/**
 * [SubspaceLayout] is the main core component for layout. It can be used to measure and position
 * zero or more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviours of this layout will be defined by
 * the [measurePolicy] instance. See [MeasurePolicy] for more details.
 *
 * @param modifier SubspaceModifier to apply during layout
 * @param coreEntity SceneCore Entity being placed in this layout. This parameter is generally not
 *   needed for most use cases and should be avoided unless you have specific requirements to manage
 *   entities outside the Compose framework. If provided, it will associate the [SubspaceLayout]
 *   with the given SceneCore Entity.
 * @param content the children composable to be laid out.
 * @param name a name for the ComposeSubspaceNode. This can be useful for debugging.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("ComposableLambdaParameterPosition")
@SubspaceComposable
@Composable
internal fun SubspaceLayout(
    content: @Composable @SubspaceComposable () -> Unit,
    modifier: SubspaceModifier = SubspaceModifier,
    name: String = defaultSubspaceLayoutName(),
    coreEntity: CoreEntity = rememberCoreContentlessEntity {
        ContentlessEntity.create(session = this, name = name)
    },
    measurePolicy: MeasurePolicy,
) {
    ComposeNode<ComposeSubspaceNode, SubspaceNodeApplier>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(measurePolicy, SetMeasurePolicy)
            set(coreEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
            set(name, SetName)
        },
        content = {
            CompositionLocalProvider(LocalCoreEntity provides coreEntity, content = content)
        },
    )
}

private var subspaceLayoutNamePart: Int = 0

public fun defaultSubspaceLayoutName(): String {
    return "SubspaceLayoutNode-${subspaceLayoutNamePart++}"
}
