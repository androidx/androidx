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
package androidx.xr.compose.subspace.node

import androidx.collection.mutableObjectIntMapOf
import androidx.xr.compose.subspace.layout.CoreEntityNode
import androidx.xr.compose.subspace.layout.ParentLayoutParamsModifier
import androidx.xr.compose.subspace.layout.SubspaceModifier
import kotlin.collections.getOrPut
import kotlin.reflect.KClass

@JvmInline
internal value class SubspaceNodeKind<T>(val mask: Int) {
    @Suppress("NOTHING_TO_INLINE")
    inline infix fun or(other: SubspaceNodeKind<*>): Int = mask or other.mask

    @Suppress("NOTHING_TO_INLINE") inline infix fun or(other: Int): Int = mask or other
}

internal object SubspaceNodes {
    inline val Any // All nodes
        get() = SubspaceNodeKind<SubspaceModifier.Node>(0b1 shl 0)

    inline val Layout
        get() = SubspaceNodeKind<SubspaceLayoutModifierNode>(0b1 shl 1)

    inline val CoreEntity
        get() = SubspaceNodeKind<CoreEntityNode>(0b1 shl 2)

    inline val ParentData
        get() = SubspaceNodeKind<ParentLayoutParamsModifier>(0b1 shl 3)

    inline val LayoutAware
        get() = SubspaceNodeKind<SubspaceLayoutAwareModifierNode>(0b1 shl 4)

    inline val Locals
        get() = SubspaceNodeKind<CompositionLocalConsumerSubspaceModifierNode>(0b1 shl 5)

    inline val Semantics
        get() = SubspaceNodeKind<SubspaceSemanticsModifierNode>(0b1 shl 6)
}

private val classToSubspaceKindSetMap = mutableObjectIntMapOf<KClass<out SubspaceModifier.Node>>()

internal fun calculateSubspaceNodeKindSetFrom(node: SubspaceModifier.Node): Int {
    if (node.kindSet != 0) return node.kindSet
    val nodeClass = node::class
    @Suppress("UNCHECKED_CAST")
    val calculatedMask =
        classToSubspaceKindSetMap.getOrPut(key = nodeClass as KClass<SubspaceModifier.Node>) {
            var mask = SubspaceNodes.Any.mask
            if (node is SubspaceLayoutModifierNode) {
                mask = mask or SubspaceNodes.Layout
            }
            if (node is CoreEntityNode) {
                mask = mask or SubspaceNodes.CoreEntity
            }
            if (node is ParentLayoutParamsModifier) {
                mask = mask or SubspaceNodes.ParentData
            }
            if (node is SubspaceLayoutAwareModifierNode) {
                mask = mask or SubspaceNodes.LayoutAware
            }
            if (node is CompositionLocalConsumerSubspaceModifierNode) {
                mask = mask or SubspaceNodes.Locals
            }
            if (node is SubspaceSemanticsModifierNode) {
                mask = mask or SubspaceNodes.Semantics
            }
            mask
        }
    node.kindSet = calculatedMask
    return calculatedMask
}

@Suppress("NOTHING_TO_INLINE")
internal inline infix fun Int.or(other: SubspaceNodeKind<*>): Int = this or other.mask

@Suppress("NOTHING_TO_INLINE")
internal inline operator fun Int.contains(value: SubspaceNodeKind<*>): Boolean =
    this and value.mask != 0
