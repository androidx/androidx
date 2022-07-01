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

import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.glance.AndroidResourceImageProvider
import androidx.glance.BackgroundModifier
import androidx.glance.layout.ContentScale
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.EmittableWithChildren
import androidx.glance.ImageProvider
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.extractModifier
import androidx.glance.findModifier
import androidx.glance.action.ActionModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.background
import androidx.glance.text.EmittableText
import androidx.glance.unit.Dimension

internal fun normalizeCompositionTree(root: RemoteViewsRoot) {
    coerceToOneChild(root)
    root.normalizeSizes()
    root.transformTree { view ->
        if (view is EmittableLazyListItem) normalizeLazyListItem(view)
        view.transformBackgroundImageAndActionRipple()
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

/**
 * If this [Emittable] has a background image or a ripple, transform the emittable so that it is
 * wrapped in an [EmittableBox], with the background and ripple added as [ImageView]s in the
 * background and foreground.
 *
 * If this is an [EmittableButton], we additonally set a clip outline on the wrapper box, and
 * convert the target emittable to an [EmittableText]
 */
private fun Emittable.transformBackgroundImageAndActionRipple(): Emittable {
    // EmittableLazyListItem is a wrapper for its immediate only child,
    // and does not get translated to its own element. We will transform
    // the child instead.
    if (this is EmittableLazyListItem) return this

    var target = this

    // We only need to add a background image view if the background is a Bitmap, or a
    // drawable resource with non-default content scale. Otherwise, we can set the background
    // directly on the target element in ApplyModifiers.kt.
    val (bgModifier, notBgModifier) = target.modifier.extractModifier<BackgroundModifier>()
    val addBackground = bgModifier?.imageProvider != null &&
        (bgModifier.imageProvider !is AndroidResourceImageProvider ||
            bgModifier.contentScale != ContentScale.FillBounds)

    // Add a ripple for every element with an action that does not have already have a built in
    // ripple.
    notBgModifier.warnIfMultipleClickableActions()
    val (actionModifier, notBgOrActionModifier) = notBgModifier.extractModifier<ActionModifier>()
    val addRipple = actionModifier != null && !hasBuiltinRipple()
    val isButton = target is EmittableButton
    if (!addBackground && !addRipple && !isButton) return target

    // Hoist the size and action modifiers to the wrapping Box, then set the target element to fill
    // the given space. doNotUnsetAction() prevents the views within the Box from being made
    // clickable.
    val (sizeModifiers, nonSizeModifiers) = notBgOrActionModifier.extractSizeModifiers()
    val boxModifiers = mutableListOf<GlanceModifier?>(sizeModifiers, actionModifier)
    val targetModifiers = mutableListOf<GlanceModifier?>(
        nonSizeModifiers.fillMaxSize().doNotUnsetAction()
    )

    // If we don't need to emulate the background, add the background modifier back to the target.
    if (!addBackground) {
        targetModifiers += bgModifier
    }

    // If this is a button, set the necessary modifiers on the wrapping Box.
    if (target is EmittableButton) {
        boxModifiers += GlanceModifier
            .clipToOutline(true)
            .enabled(target.enabled)
            .background(ImageProvider(R.drawable.glance_button_outline))
        target = target.toEmittableText()
        targetModifiers += GlanceModifier.padding(horizontal = 16.dp, vertical = 8.dp)
    }

    return EmittableBox().apply {
        modifier = boxModifiers.collect()
        if (isButton) contentAlignment = Alignment.Center

        if (addBackground && bgModifier != null) {
            children += EmittableImage().apply {
                modifier = GlanceModifier.fillMaxSize().doNotUnsetAction()
                provider = bgModifier.imageProvider
                contentScale = bgModifier.contentScale
            }
        }
        children += target.apply { modifier = targetModifiers.collect() }
        if (addRipple) {
            children += EmittableImage().apply {
                modifier = GlanceModifier.fillMaxSize().doNotUnsetAction()
                provider = ImageProvider(R.drawable.glance_ripple)
            }
        }
    }
}

private fun Emittable.hasBuiltinRipple() =
    this is EmittableSwitch ||
    this is EmittableRadioButton ||
    this is EmittableCheckBox

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

private fun GlanceModifier.warnIfMultipleClickableActions() {
    val actionCount = foldIn(0) { count, modifier ->
        if (modifier is ActionModifier) count + 1 else count
    }
    if (actionCount > 1) {
        Log.w(
            GlanceAppWidgetTag,
            "More than one clickable defined on the same GlanceModifier, " +
                "only the last one will be used."
        )
    }
}

private fun MutableList<GlanceModifier?>.collect(): GlanceModifier =
    fold(GlanceModifier) { acc: GlanceModifier, mod: GlanceModifier? ->
        mod?.let { acc.then(mod) } ?: acc
    }

private fun EmittableButton.toEmittableText() = EmittableText().also {
    it.text = text
    it.style = style
    it.maxLines = maxLines
}
