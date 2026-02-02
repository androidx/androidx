/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.appwidget

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.LocalSize
import androidx.glance.layout.fillMaxSize

/**
 * A marker for the translator that indicates that this [EmittableSizeBox] and its children should
 * be translated into a distinct [android.widget.RemoteViews] object.
 *
 * EmittableSizeBox is only functional when it is a direct child of the root [RemoteViewsRoot].
 * Multiple EmittableSizeBox children will each be translated into a distinct RemoteViews, then
 * combined into one multi-sized RemoteViews.
 */
internal class EmittableSizeBox : EmittableWithChildren() {
    override var modifier: GlanceModifier
        get() = children.singleOrNull()?.modifier ?: GlanceModifier.fillMaxSize()
        set(_) {
            throw IllegalAccessError("You cannot set the modifier of an EmittableSizeBox")
        }

    var size: DpSize = DpSize.Unspecified
    var sizeMode: SizeMode = SizeMode.Single

    override fun copy(): Emittable =
        EmittableSizeBox().also {
            it.size = size
            it.sizeMode = sizeMode
            it.children.addAll(children.map { it.copy() })
        }

    override fun toString(): String =
        "EmittableSizeBox(" +
            "size=$size, " +
            "sizeMode=$sizeMode, " +
            "children=[\n${childrenToString()}\n]" +
            ")"
}

/**
 * This composable emits a marker that lets the translator know that this [SizeBox] and its children
 * should be translated into a distinct RemoteViews that is then combined with its siblings to form
 * a multi-sized RemoteViews.
 *
 * This should not be used directly. The correct SizeBoxes can be generated with [ForEachSize].
 */
@Composable
internal fun SizeBox(size: DpSize, sizeMode: SizeMode, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSize provides size) {
        GlanceNode(
            factory = ::EmittableSizeBox,
            update = {
                this.set(size) { this.size = it }
                this.set(sizeMode) { this.sizeMode = it }
            },
            content = content
        )
    }
}

/**
 * For each size indicated by [sizeMode], run [content] with a [SizeBox] set to the corresponding
 * size.
 */
@Composable
internal fun ForEachSize(sizeMode: SizeMode, minSize: DpSize, content: @Composable () -> Unit) {
    val sizes =
        when (sizeMode) {
            is SizeMode.Single -> listOf(minSize)
            is SizeMode.Exact ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    LocalAppWidgetOptions.current.extractAllSizes { minSize }
                } else {
                    LocalAppWidgetOptions.current.extractOrientationSizes().ifEmpty {
                        listOf(minSize)
                    }
                }
            is SizeMode.Responsive ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    sizeMode.sizes
                } else {
                    val smallestSize = sizeMode.sizes.sortedBySize()[0]
                    LocalAppWidgetOptions.current
                        .extractOrientationSizes()
                        .map { findBestSize(it, sizeMode.sizes) ?: smallestSize }
                        .ifEmpty { listOf(smallestSize, smallestSize) }
                }
        }
    sizes.distinct().map { size -> SizeBox(size, sizeMode, content) }
}
