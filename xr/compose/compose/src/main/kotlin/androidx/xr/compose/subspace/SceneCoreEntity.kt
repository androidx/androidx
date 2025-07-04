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

package androidx.xr.compose.subspace

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.ui.util.fastForEachIndexed
import androidx.xr.compose.subspace.layout.AdaptableCoreEntity
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCompositionLocalMap
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Entity
import kotlin.math.max
import kotlin.math.min

/**
 * A composable that attaches to a SceneCore entity and allow compose to size, position, reparent,
 * add children, and apply modifiers to the entity.
 *
 * Usage of this API requires the SceneCore dependency to be added. See
 * https://developer.android.com/jetpack/androidx/releases/xr-scenecore
 *
 * @param factory the factory method for creating the SceneCore [Entity].
 * @param modifier the [SubspaceModifier] that will be applied to this node.
 * @param update a callback to be invoked on recomposition to apply any state changes to the Entity.
 *   This will track snapshot state reads and call [update] when they change.
 * @param sizeAdapter an adapter that allows compose to integrate its layout size changes with the
 *   rendered entity size. This adapter implementation will likely be different for every entity and
 *   some SceneCore entities may not require sizing at all (this may be null).
 * @param content the children of this [Entity].
 * @see SceneCoreEntitySizeAdapter for more information on how compose sizes SceneCore entities.
 */
@Composable
@SubspaceComposable
public fun <T : Entity> SceneCoreEntity(
    factory: () -> T,
    modifier: SubspaceModifier = SubspaceModifier,
    update: (T) -> Unit = {},
    sizeAdapter: SceneCoreEntitySizeAdapter<T>? = null,
    content: @Composable @SubspaceComposable () -> Unit = {},
) {
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    val entity = remember(factory)

    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = {
            ComposeSubspaceNode.Constructor().apply {
                SetCoreEntity(AdaptableCoreEntity(entity, sizeAdapter))
                SetMeasurePolicy(
                    SceneCoreEntityMeasurePolicy(sizeAdapter?.intrinsicSize?.invoke(entity))
                )
            }
        },
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(modifier, SetModifier)
            update(sizeAdapter) {
                getAdaptableCoreEntity<T>()?.sceneCoreEntitySizeAdapter = sizeAdapter
            }
            update(entity)
        },
        content = content,
    )
}

/**
 * The sizing strategy used by [SceneCoreEntity] to control and read the size of an entity.
 *
 * The developer should use [onLayoutSizeChanged] to apply compose layout size changes to the
 * entity. Compose will not inherently affect the size of the [Entity].
 *
 * If the developer uses [onLayoutSizeChanged] to change the size of the entity, but [intrinsicSize]
 * is not provided, then the intrinsic size of the entity will be ignored and the layout size as
 * determined solely by compose will be used to size the entity. If the [SceneCoreEntity] has no
 * children or size modifiers then compose doesn't know how to size this node and it will be size 0,
 * causing it not to render at all. In such a case, please do one of the following: (1) provide
 * [intrinsicSize] so compose can infer the size from the entity, (2) add a sizing modifier to
 * control the size of the entity, or (3) remove the adapter from the [SceneCoreEntity] as without
 * an adapter compose will not try to control the size of this entity.
 *
 * Note that many SceneCore entities accept sizes in meter units instead of pixels. The [Meter] type
 * may be used to convert from pixels to meters.
 *
 * ```kotlin
 * Meter.fromPixel(px, density).toM()
 * ```
 *
 * @param onLayoutSizeChanged a callback that is invoked with the final layout size of the
 *   composable in pixels.
 * @param intrinsicSize a getter method that returns the current [IntVolumeSize] in pixels of the
 *   entity. This isn't as critical for compose as [onLayoutSizeChanged]; however, this can help to
 *   inform compose of the intrinsic size of the entity.
 */
public class SceneCoreEntitySizeAdapter<T : Entity>(
    public val onLayoutSizeChanged: T.(IntVolumeSize) -> Unit,
    public val intrinsicSize: (T.() -> IntVolumeSize)? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SceneCoreEntitySizeAdapter<*>

        if (onLayoutSizeChanged !== other.onLayoutSizeChanged) return false
        if (intrinsicSize !== other.intrinsicSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = onLayoutSizeChanged.hashCode()
        result = 31 * result + (intrinsicSize?.hashCode() ?: 0)
        return result
    }
}

private class SceneCoreEntityMeasurePolicy(private val originalSize: IntVolumeSize?) :
    SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        if (measurables.isEmpty()) {
            return if (originalSize == null) {
                layout(constraints.minWidth, constraints.minHeight, constraints.minDepth) {}
            } else {
                layout(
                    max(originalSize.width, constraints.minWidth),
                    max(originalSize.height, constraints.minHeight),
                    max(originalSize.depth, constraints.minDepth),
                ) {}
            }
        }

        val placeables = arrayOfNulls<SubspacePlaceable>(measurables.size)
        var width = max(constraints.minWidth, min(originalSize?.width ?: 0, constraints.maxWidth))
        var height =
            max(constraints.minHeight, min(originalSize?.height ?: 0, constraints.maxHeight))
        var depth = max(constraints.minDepth, min(originalSize?.depth ?: 0, constraints.maxDepth))
        measurables.fastForEachIndexed { index, measurable ->
            val placeable = measurable.measure(constraints)
            placeables[index] = placeable
            width = max(width, placeable.measuredWidth)
            height = max(height, placeable.measuredHeight)
            depth = max(depth, placeable.measuredDepth)
        }

        return layout(width, height, depth) {
            placeables.forEachIndexed { index, placeable ->
                placeable as SubspacePlaceable
                placeable.place(Pose.Identity)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SceneCoreEntityMeasurePolicy

        return originalSize == other.originalSize
    }

    override fun hashCode(): Int {
        return originalSize?.hashCode() ?: 0
    }
}

private fun <T : Entity> ComposeSubspaceNode.getAdaptableCoreEntity(): AdaptableCoreEntity<T>? =
    coreEntity.castTo<AdaptableCoreEntity<T>>()

private inline fun <reified T> Any?.castTo() = this as? T
