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

import android.os.Build
import android.util.Log
import androidx.annotation.DimenRes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Backend
import androidx.glance.BackgroundModifier
import androidx.glance.ColorFilter
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.EmittableLazyItemWithChildren
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.ActionModifier
import androidx.glance.action.LambdaAction
import androidx.glance.action.NoRippleOverride
import androidx.glance.addChild
import androidx.glance.addChildIfNotNull
import androidx.glance.appwidget.action.CompoundButtonAction
import androidx.glance.appwidget.components.EmittableM3IconButton
import androidx.glance.appwidget.components.EmittableM3TextButton
import androidx.glance.background
import androidx.glance.extractModifier
import androidx.glance.findModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.ContentScale
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.removeModifiersOfType
import androidx.glance.text.EmittableText
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.toEmittableText
import androidx.glance.unit.Dimension

/** @return what backend to emit: RemoteViews vs RemoteCompose */
internal fun normalizeCompositionTree(
    root: RemoteViewsRoot,
    backendOverrideRequest: Backend?,
    isPreviewComposition: Boolean = false,
): Backend {
    val isRemoteCompose: Boolean =
        (root.requiresRemoteCompose() || backendOverrideRequest == Backend.RemoteCompose) &&
            backendOverrideRequest != Backend.RemoteView
    coerceToOneChild(root)
    root.normalizeSizes()

    // M3 Button replacement pass
    if (!isRemoteCompose) {
        root.transformTree { view: Emittable ->
            when (view) {
                is EmittableM3TextButton -> view.normalizeForRemoteViews()
                is EmittableM3IconButton -> view.normalizeForRemoteViews()
                else -> view
            }
        }
    }

    // Full normalize pass
    root.transformTree { view: Emittable ->
        if (isPreviewComposition) {
            view.removeActionModifiers()
        }
        if (view is EmittableLazyItemWithChildren && !isRemoteCompose) normalizeLazyListItem(view)

        view.transformBackgroundImageAndActionRipple(isRemoteCompose)
    }

    return if (isRemoteCompose) Backend.RemoteCompose else Backend.RemoteView
}

/** Remove any action modifiers within the tree. */
private fun Emittable.removeActionModifiers() {
    if (this !is EmittableSizeBox && this.modifier.any { it is ActionModifier }) {
        this.modifier = this.modifier.removeModifiersOfType<ActionModifier>()
    }
}

/**
 * Ensure that [container] has only one direct child.
 *
 * If [container] has multiple children, wrap them in an [EmittableBox] and make that the only child
 * of container. If [container] contains only children of type [EmittableSizeBox], then we will make
 * sure each of the [EmittableSizeBox]es has one child by wrapping their children in an
 * [EmittableBox].
 */
private fun coerceToOneChild(container: EmittableWithChildren) {
    if (container.children.isNotEmpty() && container.children.all { it is EmittableSizeBox }) {
        for (item in container.children) {
            item as EmittableSizeBox
            if (item.children.size == 1) continue
            val box = EmittableBox()
            box.children += item.children
            item.children.clear()
            item.children += box
        }
        return
    } else if (container.children.size == 1) {
        return
    }
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
    if (
        (modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap) is Dimension.Wrap &&
            children.any { child ->
                child.modifier.findModifier<HeightModifier>()?.height is Dimension.Fill
            }
    ) {
        modifier = modifier.fillMaxHeight()
    }
    if (
        (modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap) is Dimension.Wrap &&
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

/**
 * Walks through the Emittable tree and updates the key for all LambdaActions.
 *
 * This function updates the key such that the final key is equal to the original key plus a string
 * indicating its index among its siblings. This is because sibling Composables will often have the
 * same key due to how [androidx.compose.runtime.currentCompositeKeyHash] works. Adding the index
 * makes sure that all of these keys are unique.
 *
 * Note that, because we run the same composition multiple times for different sizes in certain
 * modes (see [ForEachSize]), action keys in one SizeBox should mirror the action keys in other
 * SizeBoxes, so that if an action is triggered on the widget being displayed in one size, the state
 * will be updated for the composition in all sizes. This is why there can be multiple LambdaActions
 * for each key, even after de-duping.
 */
internal fun EmittableWithChildren.updateLambdaActionKeys(): Map<String, List<LambdaAction>> =
    children.foldIndexed(mutableMapOf<String, MutableList<LambdaAction>>()) { index, actions, child
        ->
        val (action: LambdaAction?, modifiers: GlanceModifier) =
            child.modifier.extractLambdaAction()
        if (
            action != null && child !is EmittableSizeBox && child !is EmittableLazyItemWithChildren
        ) {
            val newKey = action.key + "+$index"
            val newAction = LambdaAction(newKey, action.block)
            actions.getOrPut(newKey) { mutableListOf() }.add(newAction)
            child.modifier = modifiers.then(ActionModifier(newAction))
        }
        if (child is EmittableWithChildren) {
            child.updateLambdaActionKeys().forEach { (key, childActions) ->
                actions.getOrPut(key) { mutableListOf() }.addAll(childActions)
            }
        }
        actions
    }

private fun GlanceModifier.extractLambdaAction(): Pair<LambdaAction?, GlanceModifier> =
    extractModifier<ActionModifier>().let { (actionModifier, modifiers) ->
        val action = actionModifier?.action
        when {
            action is LambdaAction -> action to modifiers
            action is CompoundButtonAction && action.innerAction is LambdaAction ->
                action.innerAction to modifiers
            else -> null to modifiers
        }
    }

private fun normalizeLazyListItem(view: EmittableLazyItemWithChildren) {
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
 * If this is an [EmittableButton], we additionally set a clip outline on the wrapper box, and
 * convert the target emittable to an [EmittableText]
 */
private fun Emittable.transformBackgroundImageAndActionRipple(isRemoteCompose: Boolean): Emittable {
    // EmittableLazyItemWithChildren and EmittableSizeBox are wrappers for their immediate
    // only child, and do not get translated to their own element. We will transform their child
    // instead.
    if (this is EmittableLazyItemWithChildren || this is EmittableSizeBox) return this

    var target = this
    val isButton = target is EmittableButton

    // Button ignores background modifiers.
    if (isButton && Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
        // Buttons cannot have a background image modifier. Remove BackgroundModifier.Image from
        // the button if it exists.
        val (maybeBgImageModifier, modifiersMinusBgImage) =
            target.modifier.extractModifier<BackgroundModifier.Image>()
        if (maybeBgImageModifier != null) {
            Log.w(
                GlanceAppWidgetTag,
                "Glance Buttons should not have a background image modifier. " +
                    "Consider an image with a clickable modifier.",
            )
            target.modifier = modifiersMinusBgImage
        }

        // Buttons ignore background color modifier. Remove it.
        val (maybeBgColorModifier, modifiersMinusBgColor) =
            target.modifier.extractModifier<BackgroundModifier.Image>()
        if (maybeBgColorModifier != null) {
            Log.w(
                GlanceAppWidgetTag,
                "Glance Buttons should not have a background color modifier. " +
                    "Consider a tinted image with a clickable modifier",
            )
            target.modifier = modifiersMinusBgColor
        }
    }

    val shouldWrapTargetInABox =
        target.modifier.any {
            // Background images (i.e. BitMap or drawable resources) are emulated by placing the
            // image
            // before the target in the wrapper box. This allows us to support content scale as well
            // as
            // can help support additional processing on background images. Note: Button's don't
            // support
            // bg image modifier.
            (it is BackgroundModifier.Image) ||
                // R- buttons are implemented using box, images and text.
                (isButton && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) ||
                // Ripples are implemented by placing a drawable after the target in the wrapper
                // box.
                (it is ActionModifier && !hasBuiltinRipple(isRemoteCompose))
        }
    if (!shouldWrapTargetInABox) return target

    // Hoisted modifiers are subtracted from the target one by one and added to the box and the
    // remaining modifiers are applied to the target.
    val boxModifiers: MutableList<GlanceModifier?> = mutableListOf<GlanceModifier?>()
    val targetModifiers: MutableList<GlanceModifier?> = mutableListOf<GlanceModifier?>()
    var rippleImage: EmittableImage? = null

    val (bgModifier: BackgroundModifier?, targetModifiersMinusBg) =
        target.modifier.extractModifier<BackgroundModifier>()

    val backgroundEmittableImage: EmittableImage? =
        handleBgImage(
            bgModifier = bgModifier,
            targetModifiers = targetModifiers,
            isRemoteCompose = isRemoteCompose,
            isButton = isButton,
        )

    // Action modifiers are hoisted on the wrapping box and a ripple image is added to the
    // foreground if the target doesn't have it built-in.
    targetModifiersMinusBg.warnIfMultipleClickableActions()
    val (actionModifier, targetModifiersMinusAction) =
        targetModifiersMinusBg.extractModifier<ActionModifier>()
    boxModifiers += actionModifier
    if (actionModifier != null && !hasBuiltinRipple(isRemoteCompose)) {
        val maybeRippleOverride = actionModifier.rippleOverride
        val rippleImageProvider =
            if (maybeRippleOverride != NoRippleOverride) {
                ImageProvider(maybeRippleOverride)
            } else if (isButton) {
                ImageProvider(R.drawable.glance_button_ripple)
            } else {
                ImageProvider(R.drawable.glance_ripple)
            }
        rippleImage =
            EmittableImage().apply {
                modifier = GlanceModifier.fillMaxSize()
                provider = rippleImageProvider
            }
    }

    // Hoist the size and corner radius modifiers to the wrapping Box, then set the target element
    // to fill the given space.
    val (sizeAndCornerModifiers, targetModifiersMinusSizeAndCornerRadius) =
        targetModifiersMinusAction.extractSizeAndCornerRadiusModifiers()
    boxModifiers += sizeAndCornerModifiers
    targetModifiers += targetModifiersMinusSizeAndCornerRadius.fillMaxSize()

    if (!isRemoteCompose && target is EmittableButton) {
        boxModifiers += GlanceModifier.enabled(target.enabled)
        target = target.toEmittableText()
        if (target.modifier.findModifier<PaddingModifier>() == null) {
            targetModifiers += GlanceModifier.padding(horizontal = 16.dp, vertical = 8.dp)
        }
    }

    return EmittableBox().apply {
        modifier = boxModifiers.collect()
        target.modifier = targetModifiers.collect()

        if (isButton) contentAlignment = Alignment.Center

        addChildIfNotNull(backgroundEmittableImage)
        addChild(target)
        addChildIfNotNull(rippleImage)
    }
}

private fun Emittable.hasBuiltinRipple(isRemoteCompose: Boolean): Boolean {
    return if (isRemoteCompose) {
        true // do not add a ripple for remote compose
    } else {
        this is EmittableSwitch ||
            this is EmittableRadioButton ||
            this is EmittableCheckBox ||
            // S+ versions use a native button with fixed rounded corners and matching ripple set in
            // layout xml. In R- versions, buttons are implemented using a background drawable with
            // rounded corners and an EmittableText in R- versions.
            (this is EmittableButton && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }
}

private data class ExtractedSizeAndCornerModifiers(
    val sizeAndCornerModifiers: GlanceModifier = GlanceModifier,
    val nonSizeOrCornerModifiers: GlanceModifier = GlanceModifier,
)

/**
 * Split the [GlanceModifier] into one that contains the [WidthModifier]s, [HeightModifier]s,
 * [CornerRadiusModifier]s, [AppWidgetBackgroundModifier] and one that contains the rest.
 *
 * The [AppWidgetBackgroundModifier] is relevant to corner radius.
 */
private fun GlanceModifier.extractSizeAndCornerRadiusModifiers() =
    if (
        any {
            it is WidthModifier ||
                it is HeightModifier ||
                it is CornerRadiusModifier ||
                it is AppWidgetBackgroundModifier
        }
    ) {
        foldIn(ExtractedSizeAndCornerModifiers()) { acc, modifier ->
            if (
                modifier is WidthModifier ||
                    modifier is HeightModifier ||
                    modifier is CornerRadiusModifier ||
                    modifier is AppWidgetBackgroundModifier
            ) {
                acc.copy(sizeAndCornerModifiers = acc.sizeAndCornerModifiers.then(modifier))
            } else {
                acc.copy(nonSizeOrCornerModifiers = acc.nonSizeOrCornerModifiers.then(modifier))
            }
        }
    } else {
        ExtractedSizeAndCornerModifiers(nonSizeOrCornerModifiers = this)
    }

private fun GlanceModifier.warnIfMultipleClickableActions() {
    val actionCount =
        foldIn(0) { count, modifier -> if (modifier is ActionModifier) count + 1 else count }
    if (actionCount > 1) {
        Log.w(
            GlanceAppWidgetTag,
            "More than one clickable defined on the same GlanceModifier, " +
                "only the last one will be used.",
        )
    }
}

private fun MutableList<GlanceModifier?>.collect(): GlanceModifier =
    fold(GlanceModifier) { acc: GlanceModifier, mod: GlanceModifier? ->
        mod?.let { acc.then(mod) } ?: acc
    }

/**
 * Build up the ui tree for an M3 button. The material buttons are not xml views, so need special
 * handling. They are manually built out of lower level emittables.
 *
 * @return a tree of emittables that will render as an M3 button.
 */
private fun EmittableM3TextButton.normalizeForRemoteViews(): Emittable {
    fun maybeRoundCorners(@DimenRes radius: Int) =
        if (isAtLeastApi31) GlanceModifier.cornerRadius(radius) else GlanceModifier

    val iconSize = 18.dp
    val totalHorizontalPadding = if (icon != null) 24.dp else 16.dp

    val mainBox =
        EmittableBox().also { mainBox ->
            mainBox.modifier =
                this.modifier
                    .padding(
                        start = 16.dp,
                        end = totalHorizontalPadding,
                        top = 10.dp,
                        bottom = 10.dp,
                    )
                    .background(
                        imageProvider = ImageProvider(backgroundResource),
                        colorFilter = ColorFilter.tint(backgroundTint),
                    )
                    .enabled(enabled)
                    .then(maybeRoundCorners(R.dimen.glance_component_button_corners))
            mainBox.contentAlignment = Alignment.Center
        }

    val emittableText =
        EmittableText().also {
            it.text = this.text
            it.style = TextStyle(color = this.contentColor, fontSize = 14.sp, FontWeight.Medium)
            it.maxLines = this.maxLines
        }

    if (icon == null) {

        mainBox.children.add(
            // for accessibility only: force button to be the same min height as the icon
            // version.
            // TODO: remove once b/290677181 is addressed
            EmittableBox().also { sizeFixBox ->
                sizeFixBox.modifier = GlanceModifier.size(iconSize)
            }
        )
        mainBox.children.add(emittableText)
    } else {
        // TODO b/479573471 if no text is provided, the icon will be off center
        val row = EmittableRow()
        row.verticalAlignment = Alignment.Vertical.CenterVertically
        row.children.add(
            EmittableImage().also { emittableImage ->
                emittableImage.provider = icon
                emittableImage.colorFilterParams = ColorFilter.tint(contentColor).colorFilterParams
                emittableImage.modifier = GlanceModifier.size(iconSize)
            }
        )

        row.children.add(
            EmittableSpacer().also { emittableSpacer ->
                emittableSpacer.modifier = GlanceModifier.width(8.dp)
            }
        )

        row.children.add(emittableText)
        mainBox.addChild(row)
    }

    return mainBox
}

/**
 * Build up the ui tree for an M3 button. The material buttons are not xml views, so need special
 * handling. They are manually built out of lower level emittables.
 *
 * @return a tree of emittables that will render as an M3 Icon Button.
 */
private fun EmittableM3IconButton.normalizeForRemoteViews(): Emittable {
    val contentColor = contentColor!! // mandatory
    val backgroundColor = backgroundColor

    val backgroundModifier =
        if (backgroundColor == null) GlanceModifier
        else
            GlanceModifier.background(
                ImageProvider(shape.shape),
                colorFilter = ColorFilter.tint(backgroundColor),
            )

    val outerBox =
        EmittableBox().also { outerBox ->
            outerBox.contentAlignment = Alignment.Center
            outerBox.modifier =
                GlanceModifier.size(
                        shape.defaultSize
                    ) // acts as a default if not overridden by [modifier]
                    .then(this.modifier)
                    .then(backgroundModifier)
                    .enabled(enabled)
                    .then(maybeRoundCorners(shape.cornerRadius))

            outerBox.addChild(
                EmittableImage().also { emittableImage ->
                    emittableImage.provider = imageProvider
                    emittableImage.colorFilterParams =
                        ColorFilter.tint(contentColor).colorFilterParams
                    emittableImage.modifier = GlanceModifier.size(24.dp)
                }
            )
        }

    return outerBox
}

/** If [bgModifier] is not null, will handle it. Buttons and other views are handled differently. */
private fun handleBgImage(
    bgModifier: BackgroundModifier?,
    targetModifiers: MutableList<GlanceModifier?>,
    isRemoteCompose: Boolean,
    isButton: Boolean,
): EmittableImage? {
    if (bgModifier == null) {
        return null
    }

    val noBackgroundImage: EmittableImage? = null
    val backgroundEmittableImage: EmittableImage? =
        if (!isRemoteCompose && isButton) {
            // Emulate rounded corners (fixed radius) using a drawable and apply background colors
            // to it. Note: Currently, button doesn't support bg image modifier, but only button
            // colors.
            EmittableImage().apply {
                modifier = GlanceModifier.fillMaxSize()
                provider = ImageProvider(R.drawable.glance_button_outline)
                // Without setting alpha, if this drawable's base was transparent, solid color
                // won't
                // be applied as the default blending mode uses alpha from base. And if this
                // drawable's base was white/none, applying transparent tint will lead to black
                // color. This shouldn't be issue for icon type drawables, but in this case we
                // are
                // emulating colored outline. So, we apply tint as well as alpha.
                (bgModifier as? BackgroundModifier.Color)?.colorProvider?.let {
                    colorFilterParams = TintAndAlphaColorFilterParams(it)
                }
                contentScale = ContentScale.FillBounds
            }
        } else {
            // bgModifier.imageProvider is converted to an actual image but bgModifier.colorProvider
            // is applied back to the target. Note: We could have hoisted the bg color to box
            // instead of adding it back to the target, but for buttons, we also add an outline
            // background to the box.
            when (bgModifier) {
                is BackgroundModifier.Image -> {
                    EmittableImage().apply {
                        modifier = GlanceModifier.fillMaxSize()
                        provider = bgModifier.imageProvider
                        contentScale = bgModifier.contentScale
                        colorFilterParams = bgModifier.colorFilter?.colorFilterParams
                        alpha = bgModifier.alpha
                    }
                }
                is BackgroundModifier.Color -> {
                    targetModifiers += bgModifier
                    noBackgroundImage
                }
            }
        }

    return backgroundEmittableImage
}

private fun maybeRoundCorners(@DimenRes radius: Int) =
    if (isAtLeastApi31) GlanceModifier.cornerRadius(radius) else GlanceModifier

internal val isAtLeastApi31
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
