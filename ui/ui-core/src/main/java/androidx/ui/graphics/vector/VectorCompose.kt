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
@file:OptIn(ExperimentalComposeApi::class)
package androidx.ui.graphics.vector

import androidx.compose.Applier
import androidx.compose.ApplyAdapter
import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.ComposerUpdater
import androidx.compose.CompositionReference
import androidx.compose.Composition
import androidx.compose.ExperimentalComposeApi
import androidx.compose.ComposeCompilerApi
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.compositionFor
import androidx.compose.currentComposer
import androidx.ui.graphics.Brush
import androidx.ui.graphics.StrokeCap
import androidx.ui.graphics.StrokeJoin

@Composable
fun VectorScope.Group(
    name: String = DefaultGroupName,
    rotation: Float = DefaultRotation,
    pivotX: Float = DefaultPivotX,
    pivotY: Float = DefaultPivotY,
    scaleX: Float = DefaultScaleX,
    scaleY: Float = DefaultScaleY,
    translationX: Float = DefaultTranslationX,
    translationY: Float = DefaultTranslationY,
    clipPathData: List<PathNode> = EmptyPath,
    children: @Composable VectorScope.() -> Unit
) {
    GroupComponent(
        name = name,
        rotation = rotation,
        pivotX = pivotX,
        pivotY = pivotY,
        scaleX = scaleX,
        scaleY = scaleY,
        translationX = translationX,
        translationY = translationY,
        clipPathData = clipPathData
    ) {
        children()
    }
}

@Composable
fun VectorScope.Path(
    pathData: List<PathNode>,
    name: String = DefaultPathName,
    fill: Brush? = null,
    fillAlpha: Float = DefaultAlpha,
    stroke: Brush? = null,
    strokeAlpha: Float = DefaultAlpha,
    strokeLineWidth: Float = DefaultStrokeLineWidth,
    strokeLineCap: StrokeCap = DefaultStrokeLineCap,
    strokeLineJoin: StrokeJoin = DefaultStrokeLineJoin,
    strokeLineMiter: Float = DefaultStrokeLineMiter
) {
    PathComponent(
        name = name,
        pathData = pathData,
        fill = fill,
        fillAlpha = fillAlpha,
        stroke = stroke,
        strokeAlpha = strokeAlpha,
        strokeLineWidth = strokeLineWidth,
        strokeLineJoin = strokeLineJoin,
        strokeLineCap = strokeLineCap,
        strokeLineMiter = strokeLineMiter
    )
}

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

@OptIn(ComposeCompilerApi::class)
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
