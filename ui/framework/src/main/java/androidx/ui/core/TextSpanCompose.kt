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

package androidx.ui.core

import androidx.compose.Recomposer
import androidx.compose.Children
import androidx.compose.Component
import androidx.compose.Composable
import androidx.compose.CompositionContext
import androidx.compose.CompositionReference
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextSpanComposer
import androidx.ui.painting.TextSpanComposition
import androidx.ui.painting.TextStyle
import java.util.WeakHashMap

/**
 * As the name indicates, [Root] object is associated with a [TextSpan] tree root. It contains
 * necessary information used to compose and recompose [TextSpan] tree. It's created and stored
 * when the [TextSpan] container is composed for the first time.
 */
private class Root : Component() {
    @Suppress("DEPRECATION")
    fun update() = recomposeSync()

    lateinit var scope: TextSpanScope
    lateinit var composable: @Composable() TextSpanScope.() -> Unit
    @Suppress("PLUGIN_ERROR")
    override fun compose() {
        with(scope.composer.composer) {
            startGroup(0)
            scope.composable()
            endGroup()
        }
    }
}

/**
 *  The map used store the [Root] object for [TextSpan] trees.
 */
private val TEXTSPAN_ROOT_COMPONENTS = WeakHashMap<TextSpan, Root>()

/**
 * Get the [Root] object of the given [TextSpan] root node.
 */
private fun getRootComponent(node: TextSpan): Root? {
    return TEXTSPAN_ROOT_COMPONENTS[node]
}

/**
 * Store the [Root] object of [node].
 */
private fun setRoot(node: TextSpan, component: Root) {
    TEXTSPAN_ROOT_COMPONENTS[node] = component
}

/**
 * Compose a [TextSpan] tree.
 * @param container The root of [TextSpan] tree where the children TextSpans will be attached to.
 * @param parent The parent composition reference, if applicable. Default is null.
 * @param composable The composable function to compose the children of [container].
 * @see CompositionReference
 */
@Suppress("PLUGIN_ERROR")
fun compose(
    container: TextSpan,
    parent: CompositionReference? = null,
    composable: @Composable() TextSpanScope.() -> Unit
) {
    var root = getRootComponent(container)
    if (root == null) {
        lateinit var composer: TextSpanComposer
        root = Root()
        setRoot(container, root)

        val cc = CompositionContext.prepare(root, parent) {
            TextSpanComposer(container, this).also { composer = it }
        }
        val scope = TextSpanScope(TextSpanComposition(composer))

        root.scope = scope
        root.composable = composable

        cc.compose()
    } else {
        root.composable = composable
        root.update()
    }
}

/**
 * Cleanup when the [TextSpan] is no longer used.
 *
 * @param container The root of the [TextSpan] to be disposed.
 * @param parent The [CompositionReference] used together with [container] when [composer] is
 * called.
 */
fun disposeComposition(
    container: TextSpan,
    parent: CompositionReference? = null
) {
    // temporary easy way to call correct lifecycles on everything
    compose(container, parent) {}
    TEXTSPAN_ROOT_COMPONENTS.remove(container)
}

/**
 * The receiver class of the children of Text and TextSpan. Such that [Span] can only be used
 * within [Text] and [TextSapn].
 */
class TextSpanScope(val composer: TextSpanComposition)

@Composable
fun TextSpanScope.Span(
    text: String? = null,
    style: TextStyle? = null,
    @Children child: @Composable TextSpanScope.() -> Unit
) {
    TextSpan(text = text, style = style) {
        child()
    }
}

@Composable
fun TextSpanScope.Span(
    text: String? = null,
    style: TextStyle? = null
) {
    TextSpan(text = text, style = style)
}