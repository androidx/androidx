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

package androidx.ui.painting

import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.Effect
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.ViewValidator
import androidx.compose.cache

// TODO(haoyuchang): Merge this file with core/TextSpanCompose.kt when IR bug b/129076229 is fixed.
/**
 * This adapter is used by [TextSpanComposer] to build the [TextSpan] tree.
 * @see ApplyAdapter
 */
class TextSpanApplyAdapter : ApplyAdapter<TextSpan> {
    override fun TextSpan.start(instance: TextSpan) {}

    override fun TextSpan.end(instance: TextSpan, parent: TextSpan) {}

    override fun TextSpan.insertAt(index: Int, instance: TextSpan) {
        children.add(index, instance)
    }

    override fun TextSpan.removeAt(index: Int, count: Int) {
        repeat(count) {
            children.removeAt(index)
        }
    }

    override fun TextSpan.move(from: Int, to: Int, count: Int) {
        if (from == to) return

        if (from > to) {
            val moved = mutableListOf<TextSpan>()
            repeat(count) {
                moved.add(children.removeAt(from))
            }
            children.addAll(to, moved)
        } else {
            // Number of elements between to and from is smaller than count, can't move.
            if (count > to - from) return
            repeat(count) {
                val node = children.removeAt(from)
                children.add(to - 1, node)
            }
        }
    }
}

typealias TextSpanUpdater<T> = ComposerUpdater<TextSpan, T>

/**
 * The composer of [TextSpan].
 */
class TextSpanComposer(
    val root: TextSpan,
    recomposer: Recomposer?
) : Composer<TextSpan>(SlotTable(), Applier(root, TextSpanApplyAdapter()), recomposer)

@PublishedApi
internal val invocation = Object()

class TextSpanComposition(val composer: TextSpanComposer) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun <V> Effect<V>.unaryPlus(): V = resolve(this@TextSpanComposition.composer)

    inline fun emit(
        key: Any,
        /*crossinline*/
        ctor: () -> TextSpan,
        update: TextSpanUpdater<TextSpan>.() -> Unit
    ) = with(composer) {
        startNode(key)
        @Suppress("UNCHECKED_CAST") val node = if (inserting) ctor().also { emitNode(it) }
        else useNode()
        TextSpanUpdater(this, node).update()
        endNode()
    }

    inline fun emit(
        key: Any,
        /*crossinline*/
        ctor: () -> TextSpan,
        update: TextSpanUpdater<TextSpan>.() -> Unit,
        children: () -> Unit
    ) = with(composer) {
        startNode(key)
        @Suppress("UNCHECKED_CAST")val node = if (inserting) ctor().also { emitNode(it) }
        else useNode()
        TextSpanUpdater(this, node).update()
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
            startGroup(invocation)
            block()
            endGroup()
        } else {
            skipGroup(invocation)
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
            startGroup(invocation)
            block(f)
            endGroup()
        } else {
            skipGroup(invocation)
        }
        endGroup()
    }
}