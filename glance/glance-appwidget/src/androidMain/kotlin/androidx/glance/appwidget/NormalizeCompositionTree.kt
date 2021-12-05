/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BackgroundModifier
import androidx.glance.layout.ContentScale
import androidx.glance.Emittable
import androidx.glance.EmittableImage
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.extractModifier
import androidx.glance.findModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.unit.Dimension

internal fun normalizeCompositionTree(root: RemoteViewsRoot) {
    coerceToOneChild(root)
    root.normalizeSizes()
    root.transformTree { view ->
        if (view is EmittableLazyListItem) normalizeLazyListItem(view)
        view.transformBackgroundImage()
    }
}

private fun coerceToOneChild(container: EmittableWithChildren) {
    if (container.children.size == 1) return
    val box = EmittableBox()
    box.children += container.children
    container.children.clear()
    container.children += box
}

/**
 * Resolve mixing wrapToContent and fillMaxSize on containers.
 *
 * Make sure that if a node with wrapToContent has a child with fillMaxSize, then it becomes
 * fillMaxSize. Otherwise, the behavior depends on the version of Android.
 */
private fun EmittableWithChildren.normalizeSizes() {
    children.forEach { child ->
        if (child is EmittableWithChildren) {
            child.normalizeSizes()
        }
    }
    if ((modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap) is Dimension.Wrap &&
        children.any { child ->
            child.modifier.findModifier<HeightModifier>()?.height is Dimension.Fill
        }
    ) {
        modifier = modifier.fillMaxHeight()
    }
    if ((modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap) is Dimension.Wrap &&
        children.any { child ->
            child.modifier.findModifier<WidthModifier>()?.width is Dimension.Fill
        }
    ) {
        modifier = modifier.fillMaxWidth()
    }
}

/** Transform each node in the tree. */
private fun EmittableWithChildren.transformTree(block: (Emittable) -> Emittable) {
    children.forEachIndexed { index, child ->
        val newChild = block(child)
        children[index] = newChild
        if (newChild is EmittableWithChildren) newChild.transformTree(block)
    }
}

private fun normalizeLazyListItem(view: EmittableLazyListItem) {
    if (view.children.size == 1 && view.alignment == Alignment.CenterStart) return
    val box = EmittableBox()
    box.children += view.children
    box.contentAlignment = view.alignment
    box.modifier = view.modifier
    view.children.clear()
    view.children += box
    view.alignment = Alignment.CenterStart
}

private fun Emittable.transformBackgroundImage(): Emittable {
    val (bgModifier, modifier) = modifier.extractModifier<BackgroundModifier>()
    if (bgModifier?.imageProvider == null ||
        (bgModifier.imageProvider is AndroidResourceImageProvider &&
            bgModifier.contentScale == ContentScale.FillBounds)
    ) {
        return this
    }
    val split = modifier.extractSizeModifiers()
    this.modifier = split.nonSizeModifiers.fillMaxSize()
    return EmittableBox().also { box ->
        box.modifier = split.sizeModifiers
        box.children +=
            EmittableImage().also { image ->
                image.modifier = GlanceModifier.fillMaxSize()
                image.provider = bgModifier.imageProvider
                image.contentScale = bgModifier.contentScale
            }
        box.children += this
    }
}

private data class ExtractedSizeModifiers(
    val sizeModifiers: GlanceModifier = GlanceModifier,
    val nonSizeModifiers: GlanceModifier = GlanceModifier,
)

/**
 * Split the [GlanceModifier] into one that contains the [WidthModifier]s and [HeightModifier]s and
 * one that contains the rest.
 */
private fun GlanceModifier.extractSizeModifiers() =
    if (any { it is WidthModifier || it is HeightModifier }) {
        foldIn(ExtractedSizeModifiers()) { acc, modifier ->
            if (modifier is WidthModifier || modifier is HeightModifier) {
                acc.copy(sizeModifiers = acc.sizeModifiers.then(modifier))
            } else {
                acc.copy(nonSizeModifiers = acc.nonSizeModifiers.then(modifier))
            }
        }
    } else {
        ExtractedSizeModifiers(nonSizeModifiers = this)
    }
