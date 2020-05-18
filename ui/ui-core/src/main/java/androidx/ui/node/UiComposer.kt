/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.node

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.ui.core.LayoutNode
import androidx.ui.viewinterop.AndroidViewHolder

// TODO: evaluate if this class is necessary or not
private class Stack<T> {
    private val backing = ArrayList<T>()

    val size: Int get() = backing.size

    fun push(value: T) = backing.add(value)
    fun pop(): T = backing.removeAt(size - 1)
    fun peek(): T = backing[size - 1]
    fun isEmpty() = backing.isEmpty()
    fun isNotEmpty() = !isEmpty()
    fun clear() = backing.clear()
}

private fun invalidNode(node: Any): Nothing =
    error("Unsupported node type ${node.javaClass.simpleName}")

internal class UiApplyAdapter : ApplyAdapter<Any> {
    private data class PendingInsert(val index: Int, val instance: Any)

    private val pendingInserts =
        Stack<PendingInsert>()

    override fun Any.start(instance: Any) {}
    override fun Any.insertAt(index: Int, instance: Any) {
        pendingInserts.push(
            PendingInsert(
                index,
                instance
            )
        )
    }

    override fun Any.removeAt(index: Int, count: Int) {
        when (this) {
            is ViewGroup -> removeViews(index, count)
            is LayoutNode -> removeAt(index, count)
            else -> invalidNode(this)
        }
    }

    override fun Any.move(from: Int, to: Int, count: Int) {
        when (this) {
            is ViewGroup -> {
                if (from > to) {
                    var currentFrom = from
                    var currentTo = to
                    repeat(count) {
                        val view = getChildAt(currentFrom)
                        removeViewAt(currentFrom)
                        addView(view, currentTo)
                        currentFrom++
                        currentTo++
                    }
                } else {
                    repeat(count) {
                        val view = getChildAt(from)
                        removeViewAt(from)
                        addView(view, to - 1)
                    }
                }
            }
            is LayoutNode -> {
                move(from, to, count)
            }
            else -> invalidNode(this)
        }
    }

    override fun Any.end(instance: Any, parent: Any) {
        val adapter = when (instance) {
            is View -> instance.getViewAdapterIfExists()
            else -> null
        }
        if (pendingInserts.isNotEmpty()) {
            val pendingInsert = pendingInserts.peek()
            if (pendingInsert.instance == instance) {
                val index = pendingInsert.index
                pendingInserts.pop()

                when (parent) {
                    is ViewGroup ->
                        when (instance) {
                            is View -> {
                                adapter?.willInsert(instance, parent)
                                parent.addView(instance, index)
                                adapter?.didInsert(instance, parent)
                            }
//                            is LayoutNode -> {
//                                val adaptedView = adapters?.adapt(parent, instance) as? View
//                                    ?: error(
//                                        "Could not convert ${
//                                        instance.javaClass.simpleName
//                                        } to a View"
//                                    )
//                                adapter?.willInsert(adaptedView, parent)
//                                parent.addView(adaptedView, index)
//                                adapter?.didInsert(adaptedView, parent)
//                            }
                            else -> invalidNode(instance)
                        }
                    is LayoutNode ->
                        when (instance) {
                            is View -> {
                                // Wrap the instance in an AndroidViewHolder, unless the instance
                                // itself is already one.
                                val androidViewHolder =
                                    if (instance is AndroidViewHolder) {
                                        instance
                                    } else {
                                        AndroidViewHolder(instance.context).apply {
                                            view = instance
                                        }
                                    }

                                parent.insertAt(index, androidViewHolder.toLayoutNode())
                            }
                            is LayoutNode -> parent.insertAt(index, instance)
                            else -> invalidNode(instance)
                        }
                    else -> invalidNode(parent)
                }
                return
            }
        }
        if (parent is ViewGroup)
            adapter?.didUpdate(instance as View, parent)
    }
}

class UiComposer(
    val context: Context,
    val root: Any,
    slotTable: SlotTable,
    recomposer: Recomposer
) : Composer<Any>(
    slotTable,
    Applier(
        root,
        UiApplyAdapter()
    ),
    recomposer
) {
    init {
        FrameManager.ensureStarted()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : View> emit(
        key: Any,
        /*crossinline*/
        ctor: (context: Context) -> T,
        update: UiUpdater<T>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor(context).also { emitNode(it) }
        else useNode() as T
        UiUpdater(this, node).update()
        endNode()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <T : ViewGroup> emit(
        key: Any,
        /*crossinline*/
        ctor: (context: Context) -> T,
        update: UiUpdater<T>.() -> Unit,
        children: () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor(context).also { emitNode(it) }
        else useNode() as T
        UiUpdater(this, node).update()
        children()
        endNode()
    }

    // There is a compilation error if I change T -> LayoutNode, so I
    // just suppressed the warning
    @Suppress("UNCHECKED_CAST", "FINAL_UPPER_BOUND")
    inline fun <T : LayoutNode> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: UiUpdater<T>.() -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as T
        UiUpdater(this, node).update()
        endNode()
    }

    // There is a compilation error if I change T -> LayoutNode, so I
    // just suppressed the warning
    @Suppress("UNCHECKED_CAST", "FINAL_UPPER_BOUND")
    inline fun <T : LayoutNode> emit(
        key: Any,
        /*crossinline*/
        ctor: () -> T,
        update: UiUpdater<T>.() -> Unit,
        children: () -> Unit
    ) {
        startNode(key)
        val node = if (inserting) ctor().also { emitNode(it) }
        else useNode() as T
        UiUpdater(this, node).update()
        children()
        endNode()
    }
}

typealias UiUpdater<T> = ComposerUpdater<Any, T>