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
import androidx.compose.Component
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.CompositionContext
import androidx.compose.CompositionReference
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.ViewValidator
import androidx.compose.cache
import java.util.WeakHashMap

private val VectorTreeRoots = WeakHashMap<VectorComponent, VectorTree>()

class VectorScope(val composer: VectorComposition)

private fun obtainVectorTree(container: VectorComponent): VectorTree {
    var vectorTree = VectorTreeRoots[container]
    if (vectorTree == null) {
        vectorTree = VectorTree()
        VectorTreeRoots[container] = vectorTree
    }
    return vectorTree
}

fun composeVector(
    container: VectorComponent,
    parent: CompositionReference? = null,
    composable: @Composable() VectorScope.(viewportWidth: Float, viewportHeight: Float) -> Unit
) {
    var root = VectorTreeRoots[container]
    if (root == null) {
        lateinit var composer: VectorComposer
        root = obtainVectorTree(container)
        root.context = CompositionContext.prepare(root, parent) {
            VectorComposer(container.root, this).also { composer = it }
        }
        root.viewportWidth = container.viewportWidth
        root.viewportHeight = container.viewportHeight
        root.scope = VectorScope(VectorComposition(composer))
    }
    root.composable = composable
    root.context.compose()
}

class VectorComposer(
    val root: VNode,
    recomposer: Recomposer
) : Composer<VNode>(SlotTable(), Applier(root, VectorApplyAdapter()), recomposer)

fun disposeVector(container: VectorComponent, parent: CompositionReference? = null) {
    composeVector(container, parent) { _, _ -> }
    VectorTreeRoots.remove(container)
}

private class VectorTree : Component() {

    lateinit var scope: VectorScope
    lateinit var composable: @Composable() VectorScope.(Float, Float) -> Unit
    lateinit var context: CompositionContext

    var viewportWidth: Float = 0.0f
    var viewportHeight: Float = 0.0f

    override fun compose() {
        with(context.composer) {
            startGroup(0) // TODO (njawad) what key should be used here?
            scope.composable(viewportWidth, viewportHeight)
            endGroup()
        }
    }
}

@PublishedApi
internal val VectorGroupKey = Object()

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

class VectorComposition(val composer: VectorComposer) {
    inline fun <T : VNode> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: VectorUpdater<VNode>.() -> Unit
    ) = with(composer) {
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
    ) = with(composer) {
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun joinKey(left: Any, right: Any?): Any = composer.joinKey(left, right)

    inline fun call(
        key: Any,
        /*crossinline*/
        invalid: ViewValidator.() -> Boolean,
        block: () -> Unit
    ) = with(composer) {
        startGroup(key)
        if (ViewValidator(composer).invalid() || inserting) {
            startGroup(0)
            block()
            endGroup()
        } else {
            skipCurrentGroup()
        }
        endGroup()
    }

    inline fun <T> call(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        /*crossinline*/
        invalid: ViewValidator.(f: T) -> Boolean,
        block: (f: T) -> Unit
    ) = with(composer) {
        startGroup(key)
        val f = cache(true, ctor)
        if (ViewValidator(this).invalid(f) || inserting) {
            startGroup(0)
            block(f)
            endGroup()
        } else {
            skipCurrentGroup()
        }
        endGroup()
    }

    @Suppress("PLUGIN_WARNING")
    inline fun <T> expr(
        key: Any,
        block: () -> T
    ): T = with(composer) {
        startGroup(key)
        val result = block()
        endGroup()
        result
    }
}