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
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.xr.compose.platform.LocalOpaqueEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCompositionLocalMap
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.runtime.Session
import androidx.xr.scenecore.Entity

/**
 * [SubspaceLayout] is the main component for laying out leaf nodes with zero children.
 *
 * The measurement, layout and intrinsic measurement behaviors of this layout will be defined by the
 * [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
 *
 * @sample androidx.xr.compose.samples.SubspaceLayoutWithoutContentSample
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
 * The measurement, layout and intrinsic measurement behaviors of this layout will be defined by the
 * [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
 *
 * @sample androidx.xr.compose.samples.SubspaceLayoutWithContentSample
 * @sample androidx.xr.compose.samples.SubspaceLayoutWithCoreEntityNameSample
 * @param modifier SubspaceModifier to apply during layout
 * @param content the child composables to be laid out.
 * @param coreEntityName A name for the underlying [androidx.xr.scenecore.Entity] that is created to
 *   host the content of this layout. This name is used for debugging and identification purposes;
 *   it will appear in scene graph inspectors, making it easier to correlate this composable with
 *   its corresponding node in the 3D scene.
 * @param measurePolicy a policy defining the measurement and positioning of the layout.
 */
@Suppress("ComposableLambdaParameterPosition", "NOTHING_TO_INLINE")
@SubspaceComposable
@Composable
public inline fun SubspaceLayout(
    crossinline content: @Composable @SubspaceComposable () -> Unit,
    modifier: SubspaceModifier = SubspaceModifier,
    coreEntityName: String = "Entity",
    measurePolicy: SubspaceMeasurePolicy,
) {
    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }

    val coreEntity = rememberOpaqueEntity { Entity.create(session = this, name = coreEntityName) }
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    CompositionLocalProvider(LocalOpaqueEntity provides coreEntity) {
        ComposeNode<ComposeSubspaceNode, Applier<Any>>(
            factory = ComposeSubspaceNode.Constructor,
            update = {
                set(compositionLocalMap, SetCompositionLocalMap)
                set(measurePolicy, SetMeasurePolicy)
                set(coreEntity, SetCoreEntity)
                set(modifier, SetModifier)
            },
            content = content,
        )
    }
}

/** Creates a [CoreGroupEntity] that is automatically disposed of when it leaves the composition. */
@Composable
@PublishedApi
internal fun rememberOpaqueEntity(
    entityFactory: @DisallowComposableCalls Session.() -> Entity
): OpaqueEntity {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    return remember { CoreGroupEntity(session.entityFactory()) }
}

/**
 * [SubspaceLayout] is the main core component for layout for "leaf" nodes. It can be used to
 * measure and position zero children.
 *
 * The measurement, layout and intrinsic measurement behaviors of this layout will be defined by the
 * [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
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
    coreEntity: CoreEntity,
    measurePolicy: SubspaceMeasurePolicy,
) {
    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    CompositionLocalProvider(LocalOpaqueEntity provides coreEntity) {
        ComposeNode<ComposeSubspaceNode, Applier<Any>>(
            factory = ComposeSubspaceNode.Constructor,
            update = {
                set(compositionLocalMap, SetCompositionLocalMap)
                set(measurePolicy, SetMeasurePolicy)
                set(coreEntity, SetCoreEntity)
                set(modifier, SetModifier)
            },
        )
    }
}

/**
 * [SubspaceLayout] is the main core component for layout. It can be used to measure and position
 * zero or more layout children.
 *
 * The measurement, layout and intrinsic measurement behaviors of this layout will be defined by the
 * [SubspaceMeasurePolicy] instance. See [SubspaceMeasurePolicy] for more details.
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
    coreEntity: CoreEntity,
    measurePolicy: SubspaceMeasurePolicy,
) {

    check(currentComposer.applier.current is ComposeSubspaceNode) {
        "SubspaceComposable functions are expected to be used within the context of a " +
            "Subspace composition. Please ensure that this component is in a Subspace or " +
            " is a child of another SubspaceComposable."
    }
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    CompositionLocalProvider(LocalOpaqueEntity provides coreEntity) {
        ComposeNode<ComposeSubspaceNode, Applier<Any>>(
            factory = ComposeSubspaceNode.Constructor,
            update = {
                set(compositionLocalMap, SetCompositionLocalMap)
                set(measurePolicy, SetMeasurePolicy)
                set(coreEntity, SetCoreEntity)
                set(modifier, SetModifier)
            },
            content = content,
        )
    }
}
