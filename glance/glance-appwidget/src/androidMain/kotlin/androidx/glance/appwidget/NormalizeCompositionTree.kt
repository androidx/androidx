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

import androidx.glance.BackgroundModifier
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.layout.EmittableLazyListItem
import androidx.glance.extractModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.AndroidResourceImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableImage
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.fillMaxSize

internal fun normalizeCompositionTree(root: RemoteViewsRoot) {
    coerceToOneChild(root)
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

/** Transform each node in the tree. */
private fun EmittableWithChildren.transformTree(block: (Emittable) -> Emittable) {
    children.mapIndexed { index, child ->
        children[index] = block(child)
        if (child is EmittableWithChildren) child.transformTree(block)
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
