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

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.HeightModifier
import androidx.glance.layout.WidthModifier
import androidx.glance.unit.Dimension

/**
 * Information about a generated layout, including the layout id, ids of elements within, and other
 * details about the layout contents.
 */
internal data class LayoutInfo(@LayoutRes val layoutId: Int)

/**
 * Information about a [RemoteViews] created from generated layouts, including the layout id, ids
 * of elements within, and other details about the layout contents.
 */
internal data class RemoteViewsInfo(
    val remoteViews: RemoteViews,
    val view: InsertedViewInfo,
)

internal data class InsertedViewInfo(
    val mainViewId: Int = View.NO_ID,
    val complexViewId: Int = View.NO_ID,
    val children: Map<Int, Map<SizeSelector, Int>> = emptyMap(),
)

internal val InsertedViewInfo.isSimple: Boolean
    get() = complexViewId == View.NO_ID

/**
 * Container selector.
 *
 * This class is used to select a particular container layout.
 */
internal data class ContainerSelector(
    val type: LayoutType,
    val numChildren: Int,
    val horizontalAlignment: Alignment.Horizontal? = null,
    val verticalAlignment: Alignment.Vertical? = null,
)

internal data class ContainerInfo(@LayoutRes val layoutId: Int)

/** Type of size needed for a layout. */
internal enum class LayoutSize {
    Wrap,
    Fixed,
    Expand,
    MatchParent,
}

/** Type of a layout. */
internal enum class LayoutType {
    Row,
    Column,
    Box,
    Text,
    List,
    CheckBox,
    CheckBoxBackport,
    Button,
    Frame,
    LinearProgressIndicator,
    CircularProgressIndicator,

    // Note: Java keywords, such as 'switch', can't be used for layout ids.
    Swtch,
    SwtchBackport,
    ImageCrop,
    ImageFit,
    ImageFillBounds,
}

/** Mapping from layout type to fixed layout (if any). */
private val LayoutMap = mapOf(
    LayoutType.Text to R.layout.text,
    LayoutType.List to R.layout.list,
    LayoutType.CheckBox to R.layout.check_box,
    LayoutType.CheckBoxBackport to R.layout.check_box_backport,
    LayoutType.Button to R.layout.button,
    LayoutType.Swtch to R.layout.swtch,
    LayoutType.SwtchBackport to R.layout.swtch_backport,
    LayoutType.Frame to R.layout.frame,
    LayoutType.ImageCrop to R.layout.image_crop,
    LayoutType.ImageFit to R.layout.image_fit,
    LayoutType.ImageFillBounds to R.layout.image_fill_bounds,
    LayoutType.LinearProgressIndicator to R.layout.linear_progress_indicator,
    LayoutType.CircularProgressIndicator to R.layout.circular_progress_indicator,
)

internal data class SizeSelector(
    val width: LayoutSize,
    val height: LayoutSize,
)

/** Make the selector for a view sub, that is transforming "Fixed" into "Wrap". */
private fun LayoutSize.toViewStubSize() =
    if (this == LayoutSize.Fixed) LayoutSize.Wrap else this

private fun makeViewStubSelector(width: LayoutSize, height: LayoutSize) =
    SizeSelector(width = width.toViewStubSize(), height = height.toViewStubSize())

private val RootAliasTypeCount = generatedRootLayoutShifts.size

internal val TopLevelLayoutsCount: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    RootAliasCount
} else {
    RootAliasCount / RootAliasTypeCount
}

/**
 * Create the [RemoteViews] that can be used to create the child.
 *
 * @param translationContext Context for the translation for that node
 * @param modifier Modifier attached to the view that will be added to the root
 * @param aliasIndex Alias to use to create this root view
 * @return The [RemoteViews] created and the descriptor needed to be able to add the first view.
 */
internal fun createRootView(
    translationContext: TranslationContext,
    modifier: GlanceModifier,
    aliasIndex: Int
): RemoteViewsInfo {
    val context = translationContext.context
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        require(aliasIndex < RootAliasCount) {
            "Index of the root view cannot be more than $RootAliasCount, " +
                "currently $aliasIndex"
        }
        val sizeSelector = SizeSelector(LayoutSize.Wrap, LayoutSize.Wrap)
        val layoutId = FirstRootAlias + aliasIndex
        return RemoteViewsInfo(
            remoteViews = remoteViews(
                translationContext,
                layoutId
            ).apply {
                modifier.findModifier<WidthModifier>()?.let {
                    applySimpleWidthModifier(context, this, it, R.id.rootView)
                }
                modifier.findModifier<HeightModifier>()?.let {
                    applySimpleHeightModifier(context, this, it, R.id.rootView)
                }
            },
            view = InsertedViewInfo(children = mapOf(0 to mapOf(sizeSelector to R.id.rootStubId)))
        )
    }
    require(RootAliasTypeCount * aliasIndex < RootAliasCount) {
        "Index of the root view cannot be more than ${RootAliasCount / 4}, " +
            "currently $aliasIndex"
    }
    val widthMod =
        modifier.findModifier<WidthModifier>()?.width?.resolveDimension(context) ?: Dimension.Wrap
    val heightMod =
        modifier.findModifier<HeightModifier>()?.height?.resolveDimension(context) ?: Dimension.Wrap
    val width = if (widthMod == Dimension.Fill) LayoutSize.MatchParent else LayoutSize.Wrap
    val height = if (heightMod == Dimension.Fill) LayoutSize.MatchParent else LayoutSize.Wrap
    val sizeSelector = makeViewStubSelector(width, height)
    val layoutIdShift = generatedRootLayoutShifts[sizeSelector]
        ?: throw IllegalStateException("Cannot find root element for size [$width, $height]")
    val layoutId = FirstRootAlias + RootAliasTypeCount * aliasIndex + layoutIdShift
    return RemoteViewsInfo(
        remoteViews = remoteViews(translationContext, layoutId),
        view = InsertedViewInfo(children = mapOf(0 to mapOf(sizeSelector to R.id.rootStubId))),
    )
}

internal fun RemoteViews.insertView(
    translationContext: TranslationContext,
    type: LayoutType,
    modifier: GlanceModifier
): InsertedViewInfo {
    val childLayout = LayoutMap[type]
        ?: throw IllegalArgumentException("Cannot use `insertView` with a container like $type")
    return insertViewInternal(translationContext, childLayout, modifier)
}

private fun RemoteViews.insertViewInternal(
    translationContext: TranslationContext,
    @LayoutRes childLayout: Int,
    modifier: GlanceModifier
): InsertedViewInfo {
    val pos = translationContext.itemPosition
    val widthMod = modifier.findModifier<WidthModifier>()?.width ?: Dimension.Wrap
    val heightMod = modifier.findModifier<HeightModifier>()?.height ?: Dimension.Wrap
    // Null unless the view Id is specified by some attributes.
    val specifiedViewId = if (modifier.all { it !is AppWidgetBackgroundModifier }) {
        null
    } else {
        check(!translationContext.isBackgroundSpecified.getAndSet(true)) {
            "At most one view can be set as AppWidgetBackground."
        }
        android.R.id.background
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val width = if (widthMod == Dimension.Expand) LayoutSize.Expand else LayoutSize.Wrap
        val height = if (heightMod == Dimension.Expand) LayoutSize.Expand else LayoutSize.Wrap
        val stubId = selectChild(translationContext, pos, width, height)
        val resId = inflateViewStub(translationContext, stubId, childLayout, specifiedViewId)
        return InsertedViewInfo(mainViewId = resId)
    }
    val context = translationContext.context
    val width = widthMod.resolveDimension(context).toSpecSize()
    val height = heightMod.resolveDimension(context).toSpecSize()
    val stubId = selectChild(translationContext, pos, width, height)
    val needsResize = width == LayoutSize.Fixed || height == LayoutSize.Fixed
    return if (needsResize) {
        val complexLayout = generatedComplexLayouts[SizeSelector(width, height)]
            ?: throw IllegalArgumentException(
                "Could not find complex layout for width=$width, height=$height"
            )
        val complexId = inflateViewStub(translationContext, stubId, complexLayout.layoutId)
        val childId =
            inflateViewStub(translationContext, R.id.glanceViewStub, childLayout, specifiedViewId)
        InsertedViewInfo(mainViewId = childId, complexViewId = complexId)
    } else {
        val resId = inflateViewStub(translationContext, stubId, childLayout, specifiedViewId)
        InsertedViewInfo(mainViewId = resId)
    }
}

@IdRes
private fun RemoteViews.selectChild(
    translationContext: TranslationContext,
    pos: Int,
    width: LayoutSize,
    height: LayoutSize
): Int {
    val child = makeViewStubSelector(width, height)
    val children = translationContext.parentContext.children[pos]
        ?: throw IllegalStateException("Parent doesn't have child position $pos")
    val stubId = children[child]
        ?: throw IllegalStateException("No child for position $pos and size $width x $height")
    children.values
        .filter { it != stubId }
        .forEach {
            inflateViewStub(translationContext, it, R.layout.deleted_view, R.id.deletedViewId)
        }
    return stubId
}

internal fun RemoteViews.insertContainerView(
    translationContext: TranslationContext,
    type: LayoutType,
    numChildren: Int,
    modifier: GlanceModifier,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
): InsertedViewInfo {
    val childLayout = generatedContainers[ContainerSelector(
        type,
        numChildren,
        horizontalAlignment,
        verticalAlignment
    )]
        ?: throw IllegalArgumentException("Cannot find container $type with $numChildren children")
    val childrenMapping = generatedChildren[type]
        ?: throw IllegalArgumentException("Cannot find generated children for $type")
    return insertViewInternal(translationContext, childLayout.layoutId, modifier)
        .copy(children = childrenMapping)
}

private fun Dimension.toSpecSize(): LayoutSize =
    when (this) {
        is Dimension.Wrap -> LayoutSize.Wrap
        is Dimension.Expand -> LayoutSize.Expand
        is Dimension.Fill -> LayoutSize.MatchParent
        is Dimension.Dp, is Dimension.Resource -> LayoutSize.Fixed
    }

internal fun Dimension.resolveDimension(context: Context): Dimension {
    if (this !is Dimension.Resource) return this
    val sizePx = context.resources.getDimension(res)
    return when (sizePx.toInt()) {
        ViewGroup.LayoutParams.MATCH_PARENT -> Dimension.Fill
        ViewGroup.LayoutParams.WRAP_CONTENT -> Dimension.Wrap
        else -> Dimension.Dp((sizePx / context.resources.displayMetrics.density).dp)
    }
}
