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
import androidx.compose.CompositionReference
import androidx.compose.Composition
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.compositionFor
import androidx.compose.currentComposer

class VectorScope(val composer: VectorComposer)

@Suppress("NAME_SHADOWING")
fun composeVector(
    container: VectorComponent,
    recomposer: Recomposer,
    parent: CompositionReference? = null,
    composable: @Composable VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
): Composition = compositionFor(
    container = container,
    recomposer = recomposer,
    parent = parent,
    composerFactory = { slots, recomposer -> VectorComposer(container.root, slots, recomposer) }
).apply {
    setContent {
        val composer = currentComposer as VectorComposer
        val scope = VectorScope(composer)
        scope.composable(container.viewportWidth, container.viewportHeight)
    }
}

@Deprecated(
    "Specify the Recomposer explicitly",
    ReplaceWith(
        "composeVector(container, Recomposer.current(), parent, composable)",
        "androidx.compose.Recomposer"
    )
)
fun composeVector(
    container: VectorComponent,
    parent: CompositionReference? = null,
    composable: @Composable VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
): Composition = composeVector(container, Recomposer.current(), parent, composable)

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
