/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.graphics.vector

import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.currentComposerIntrinsic
import java.util.WeakHashMap

private val VectorCompositions = WeakHashMap<VectorComponent, VectorComposition>()

class VectorScope(val composer: VectorComposer)

fun composeVector(
    container: VectorComponent,
    parent: CompositionReference? = null,
    composable: @Composable() VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
): Composition {
    val composition = VectorCompositions[container]
        ?: VectorComposition(container, parent).also { VectorCompositions[container] = it }
    composition.compose(composable)
    return composition
}

class VectorComposition(
    private val container: VectorComponent,
    parent: CompositionReference? = null
) : Composition(
    { slots, recomposer -> VectorComposer(container.root, slots, recomposer) },
    parent
) {
    fun compose(
        content: @Composable VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
    ) {
        super.compose {
            val composer = currentComposerIntrinsic as VectorComposer
            val scope = VectorScope(composer)
            scope.content(container.viewportWidth, container.viewportHeight)
        }
    }
}

class VectorComposer(
    val root: VNode,
    slotTable: SlotTable,
    recomposer: Recomposer
) : Composer<VNode>(slotTable, Applier(root, VectorApplyAdapter()), recomposer) {
    inline fun <T : VNode> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: VectorUpdater<VNode>.() -> Unit
    ) {
        startNode(key)

        @Suppress("UNCHECKED_CAST")
        val node = if (inserting) {
            ctor().also {
                emitNode(it)
            }
        } else {
            useNode()
        }

        VectorUpdater(this, node).update()
        endNode()
    }

    inline fun emit(
        key: Any,
        /*crossinline*/
        ctor: () -> GroupComponent,
        update: VectorUpdater<GroupComponent>.() -> Unit,
        children: () -> Unit
    ) {
        startNode(key)

        @Suppress("UNCHECKED_CAST")
        val node = if (inserting) {
            ctor().also {
                emitNode(it)
            }
        } else {
            useNode() as GroupComponent
        }

        VectorUpdater(this, node).update()
        children()
        endNode()
    }
}

fun disposeVector(container: VectorComponent, parent: CompositionReference? = null) {
    composeVector(container, parent) { _, _ -> }
    VectorCompositions.remove(container)
}

internal class VectorApplyAdapter : ApplyAdapter<VNode> {
    override fun VNode.start(instance: VNode) {
        // NO-OP
    }

    override fun VNode.insertAt(index: Int, instance: VNode) {
        obtainGroup().insertAt(index, instance)
    }

    override fun VNode.removeAt(index: Int, count: Int) {
        obtainGroup().remove(index, count)
    }

    override fun VNode.move(from: Int, to: Int, count: Int) {
        obtainGroup().move(from, to, count)
    }

    override fun VNode.end(instance: VNode, parent: VNode) {
        // NO-OP
    }

    fun VNode.obtainGroup(): GroupComponent {
        return when (this) {
            is GroupComponent -> this
            else -> throw IllegalArgumentException("Cannot only insert VNode into Group")
        }
    }
}

typealias VectorUpdater<T> = ComposerUpdater<VNode, T>
