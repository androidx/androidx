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

import androidx.xr.compose.unit.IntVolumeSize

/**
 * Interface for nodes that can modify a CoreEntity.
 *
 * Modifications are applied sequentially in the order nodes are applied, with each modification
 * combined according to the property's logic. For example, if two nodes modify scale, the resulting
 * scale will be the product of the individual scales. See the description of each property setter
 * for more details on the particular logic that is applied.
 */
internal interface CoreEntityNode {
    /**
     * Called during the placement of the [CoreEntity] prior to when the size and position is
     * finally set on the node.
     */
    public fun CoreEntityScope.modifyCoreEntity()
}

/**
 * Requests a relayout of the [CoreEntityNode] composition tree.
 *
 * This is used to request a relayout in stateful layout modifiers that are impacted by events that
 * don't trigger a recomposition. *Do not* call this from [CoreEntityNode.modifyCoreEntity] as
 * [modifyCoreEntity] is called during layout and [requestRelayout] will cause a relayout loop.
 */
internal fun CoreEntityNode.requestRelayout() {
    (this as SubspaceModifier.Node).layoutNode?.requestRelayout()
}

/**
 * The scope for modifying a CoreEntity. This gives access to specific changes that may be made to a
 * [CoreEntity] in the [CoreEntityNode.modifyCoreEntity] method.
 */
internal interface CoreEntityScope {
    /**
     * Sets the scale of the [CoreEntity].
     *
     * If multiple [CoreEntityNode] modifiers set the scale, the final scale will be the product of
     * all scales. Only the last call to [setOrAppendScale] from a particular [CoreEntityNode] will
     * be applied.
     *
     * See [CoreEntity.scale] for more details on how scale is applied.
     *
     * @param scale The scale of the CoreEntity.
     */
    public fun setOrAppendScale(scale: Float)

    /**
     * Sets the alpha of the [CoreEntity].
     *
     * If multiple [CoreEntityNode] modifiers set the alpha, the final alpha will be the product of
     * all alphas. Only the last call to [setOrAppendAlpha] from a particular [CoreEntityNode] will
     * be applied.
     *
     * See [CoreEntity.alpha] for more details on how alpha is applied.
     *
     * @param alpha The alpha of the CoreEntity.
     */
    public fun setOrAppendAlpha(alpha: Float)

    /**
     * Sets the rendered size of the [CoreEntity] in pixels.
     *
     * Only the last [CoreEntityNode] in the chain that sets the size of the [CoreEntity] will take
     * effect. This does not change the measured size of the composable content during layout, only
     * the rendered size of the [CoreEntity]. This is similar to how [SubspaceModifier.scale] works.
     *
     * @param size The rendered size of the CoreEntity.
     */
    public fun setRenderedSize(size: IntVolumeSize)
}

private class CoreEntityAccumulator : CoreEntityScope {
    var alpha: Float = 1f
    var scale: Float = 1f
    var size: IntVolumeSize? = null

    override fun setOrAppendScale(scale: Float) {
        this.scale = scale
    }

    override fun setOrAppendAlpha(alpha: Float) {
        this.alpha = alpha
    }

    override fun setRenderedSize(size: IntVolumeSize) {
        this.size = size
    }

    fun merge(next: CoreEntityAccumulator): CoreEntityAccumulator {
        val result = CoreEntityAccumulator()
        result.alpha = alpha * next.alpha
        result.scale = scale * next.scale
        result.size = next.size ?: size
        return result
    }

    fun applyChanges(coreEntity: CoreEntity) {
        coreEntity.scale = scale
        coreEntity.alpha = alpha
        if (coreEntity is ResizableCoreEntity) {
            coreEntity.overrideSize = size
        }
    }
}

/**
 * Applies the [CoreEntityNode]s in the given chain to this [CoreEntity].
 *
 * @param nodes The chain containing the [CoreEntityNode]s to apply.
 */
internal fun CoreEntity.applyCoreEntityNodes(nodes: Sequence<CoreEntityNode>) {
    nodes.getAccumulator().applyChanges(this)
}

private fun Sequence<CoreEntityNode>.getAccumulator(): CoreEntityAccumulator =
    fold(CoreEntityAccumulator()) { accumulator, node ->
        accumulator.merge(with(node) { CoreEntityAccumulator().apply { modifyCoreEntity() } })
    }

/**
 * The CoreEntity that will be modified by the CoreEntityNode.
 *
 * This is only available if the CoreEntityNode is attached to a SubspaceLayoutNode, which happens
 * when the modifier is evaluated. This field cannot be used in the initialization of the
 * [CoreEntityNode].
 */
internal val CoreEntityNode.coreEntity: CoreEntity
    get() =
        checkNotNull((this as? SubspaceModifier.Node)?.layoutNode?.coreEntity) {
            "CoreEntityNode must be attached to a SubspaceLayoutNode to access the CoreEntity."
        }
