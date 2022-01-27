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
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.DpSize
import androidx.core.widget.RemoteViewsCompat.setLinearLayoutGravity
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.appwidget.translators.setText
import androidx.glance.appwidget.translators.translateEmittableCheckBox
import androidx.glance.appwidget.translators.translateEmittableImage
import androidx.glance.appwidget.translators.translateEmittableLazyColumn
import androidx.glance.appwidget.translators.translateEmittableLazyListItem
import androidx.glance.appwidget.translators.translateEmittableSwitch
import androidx.glance.appwidget.translators.translateEmittableText
import androidx.glance.appwidget.translators.translateEmittableLinearProgressIndicator
import androidx.glance.appwidget.translators.translateEmittableCircularProgressIndicator
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.text.EmittableText
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal fun translateComposition(
    context: Context,
    appWidgetId: Int,
    element: RemoteViewsRoot,
    layoutConfiguration: LayoutConfiguration,
    rootViewIndex: Int,
    layoutSize: DpSize,
) =
    translateComposition(
        TranslationContext(
            context,
            appWidgetId,
            context.isRtl,
            layoutConfiguration,
            itemPosition = -1,
            layoutSize = layoutSize,
        ),
        element.children,
        rootViewIndex,
    )

@VisibleForTesting
internal var forceRtl: Boolean? = null

private val Context.isRtl: Boolean
    get() = forceRtl
        ?: (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)

internal fun translateComposition(
    translationContext: TranslationContext,
    children: List<Emittable>,
    rootViewIndex: Int
): RemoteViews {
    require(children.size == 1) {
        "The root of the tree must have exactly one child. " +
            "The normalization of the composition tree failed."
    }
    val child = children.first()
    val remoteViewsInfo = createRootView(translationContext, child.modifier, rootViewIndex)
    val rv = remoteViewsInfo.remoteViews
    rv.translateChild(translationContext.forRoot(root = remoteViewsInfo), child)
    return rv
}

internal data class TranslationContext(
    val context: Context,
    val appWidgetId: Int,
    val isRtl: Boolean,
    val layoutConfiguration: LayoutConfiguration,
    val itemPosition: Int,
    val isLazyCollectionDescendant: Boolean = false,
    val lastViewId: AtomicInteger = AtomicInteger(0),
    val parentContext: InsertedViewInfo = InsertedViewInfo(),
    val isBackgroundSpecified: AtomicBoolean = AtomicBoolean(false),
    val layoutSize: DpSize = DpSize.Zero,
    val layoutCollectionViewId: Int = View.NO_ID,
    val layoutCollectionItemId: Int = -1,
) {
    fun nextViewId() = lastViewId.incrementAndGet()

    fun forChild(parent: InsertedViewInfo, pos: Int): TranslationContext =
        copy(itemPosition = pos, parentContext = parent)

    fun forRoot(root: RemoteViewsInfo): TranslationContext =
        forChild(pos = 0, parent = root.view)

    fun resetViewId(newViewId: Int = 0) = copy(lastViewId = AtomicInteger(newViewId))

    fun forLazyCollection(viewId: Int) =
        copy(isLazyCollectionDescendant = true, layoutCollectionViewId = viewId)

    fun forLazyViewItem(itemId: Int, newViewId: Int = 0) =
        copy(lastViewId = AtomicInteger(newViewId), layoutCollectionViewId = itemId)
}

internal fun RemoteViews.translateChild(
    translationContext: TranslationContext,
    element: Emittable
) {
    when (element) {
        is EmittableBox -> translateEmittableBox(translationContext, element)
        is EmittableButton -> translateEmittableButton(translationContext, element)
        is EmittableRow -> translateEmittableRow(translationContext, element)
        is EmittableColumn -> translateEmittableColumn(translationContext, element)
        is EmittableText -> translateEmittableText(translationContext, element)
        is EmittableLazyListItem -> translateEmittableLazyListItem(translationContext, element)
        is EmittableLazyColumn -> translateEmittableLazyColumn(translationContext, element)
        is EmittableAndroidRemoteViews -> {
            translateEmittableAndroidRemoteViews(translationContext, element)
        }
        is EmittableCheckBox -> translateEmittableCheckBox(translationContext, element)
        is EmittableSpacer -> translateEmittableSpacer(translationContext, element)
        is EmittableSwitch -> translateEmittableSwitch(translationContext, element)
        is EmittableImage -> translateEmittableImage(translationContext, element)
        is EmittableLinearProgressIndicator -> {
            translateEmittableLinearProgressIndicator(translationContext, element)
        }
        is EmittableCircularProgressIndicator -> {
            translateEmittableCircularProgressIndicator(translationContext, element)
        }
        else -> {
            throw IllegalArgumentException(
                "Unknown element type ${element.javaClass.canonicalName}"
            )
        }
    }
}

internal fun remoteViews(translationContext: TranslationContext, @LayoutRes layoutId: Int) =
    RemoteViews(translationContext.context.packageName, layoutId)

internal fun Alignment.Horizontal.toGravity(): Int =
    when (this) {
        Alignment.Horizontal.Start -> Gravity.START
        Alignment.Horizontal.End -> Gravity.END
        Alignment.Horizontal.CenterHorizontally -> Gravity.CENTER_HORIZONTAL
        else -> {
            Log.w(GlanceAppWidgetTag, "Unknown horizontal alignment: $this")
            Gravity.START
        }
    }

internal fun Alignment.Vertical.toGravity(): Int =
    when (this) {
        Alignment.Vertical.Top -> Gravity.TOP
        Alignment.Vertical.Bottom -> Gravity.BOTTOM
        Alignment.Vertical.CenterVertically -> Gravity.CENTER_VERTICAL
        else -> {
            Log.w(GlanceAppWidgetTag, "Unknown vertical alignment: $this")
            Gravity.TOP
        }
    }

internal fun Alignment.toGravity() = horizontal.toGravity() or vertical.toGravity()

private fun RemoteViews.translateEmittableBox(
    translationContext: TranslationContext,
    element: EmittableBox
) {
    val viewDef = insertContainerView(
        translationContext,
        LayoutType.Box,
        element.children.size,
        element.modifier,
        element.contentAlignment.horizontal,
        element.contentAlignment.vertical,
    )
    applyModifiers(
        translationContext,
        this,
        element.modifier,
        viewDef
    )
    setChildren(
        translationContext,
        viewDef,
        element.children
    )
}

private fun RemoteViews.translateEmittableRow(
    translationContext: TranslationContext,
    element: EmittableRow
) {
    val viewDef = insertContainerView(
        translationContext,
        LayoutType.Row,
        element.children.size,
        element.modifier,
        horizontalAlignment = null,
        verticalAlignment = element.verticalAlignment,
    )
    setLinearLayoutGravity(
        viewDef.mainViewId,
        element.horizontalAlignment.toGravity()
    )
    applyModifiers(
        translationContext,
        this,
        element.modifier,
        viewDef
    )
    setChildren(
        translationContext,
        viewDef,
        element.children
    )
}

private fun RemoteViews.translateEmittableColumn(
    translationContext: TranslationContext,
    element: EmittableColumn
) {
    val viewDef = insertContainerView(
        translationContext,
        LayoutType.Column,
        element.children.size,
        element.modifier,
        horizontalAlignment = element.horizontalAlignment,
        verticalAlignment = null,
    )
    setLinearLayoutGravity(
        viewDef.mainViewId,
        element.verticalAlignment.toGravity()
    )
    applyModifiers(
        translationContext,
        this,
        element.modifier,
        viewDef
    )
    setChildren(
        translationContext,
        viewDef,
        element.children
    )
}

private fun RemoteViews.translateEmittableAndroidRemoteViews(
    translationContext: TranslationContext,
    element: EmittableAndroidRemoteViews
) {
    val rv = if (element.children.isEmpty()) {
        element.remoteViews
    } else {
        check(element.containerViewId != View.NO_ID) {
            "To add children to an `AndroidRemoteViews`, its `containerViewId` must be set."
        }
        element.remoteViews.copy().apply {
            removeAllViews(element.containerViewId)
            element.children.forEachIndexed { index, child ->
                val rvInfo = createRootView(translationContext, child.modifier, index)
                val rv = rvInfo.remoteViews
                rv.translateChild(translationContext.forRoot(rvInfo), child)
                addChildView(element.containerViewId, rv, index)
            }
        }
    }
    val viewDef = insertView(translationContext, LayoutType.Frame, element.modifier)
    applyModifiers(translationContext, this, element.modifier, viewDef)
    removeAllViews(viewDef.mainViewId)
    addChildView(viewDef.mainViewId, rv, stableId = 0)
}

private fun RemoteViews.translateEmittableButton(
    translationContext: TranslationContext,
    element: EmittableButton
) {
    val viewDef = insertView(translationContext, LayoutType.Button, element.modifier)
    setText(
        translationContext,
        viewDef.mainViewId,
        element.text,
        element.style,
        maxLines = element.maxLines,
        verticalTextGravity = Gravity.CENTER_VERTICAL,
    )
    setBoolean(viewDef.mainViewId, "setEnabled", element.enabled)
    applyModifiers(translationContext, this, element.modifier, viewDef)
}

private fun RemoteViews.translateEmittableSpacer(
    translationContext: TranslationContext,
    element: EmittableSpacer
) {
    val viewDef = insertView(translationContext, LayoutType.Frame, element.modifier)
    applyModifiers(translationContext, this, element.modifier, viewDef)
}

// Sets the emittables as children to the view. This first remove any previously added view, the
// add a view per child, with a stable id if of Android S+. Currently the stable id is the index
// of the child in the iterable.
internal fun RemoteViews.setChildren(
    translationContext: TranslationContext,
    parentDef: InsertedViewInfo,
    children: Iterable<Emittable>
) {
    children.forEachIndexed { index, child ->
        translateChild(
            translationContext.forChild(parent = parentDef, pos = index),
            child,
        )
    }
}

/**
 * Add stable view if on Android S+, otherwise simply add the view.
 */
private fun RemoteViews.addChildView(viewId: Int, childView: RemoteViews, stableId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RemoteViewsTranslatorApi31Impl.addChildView(this, viewId, childView, stableId)
        return
    }
    addView(viewId, childView)
}

/**
 * Copy a RemoteViews (the exact method depends on the version of Android)
 */
@Suppress("DEPRECATION") // RemoteViews.clone must be used before Android P.
private fun RemoteViews.copy(): RemoteViews =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        RemoteViewsTranslatorApi28Impl.copyRemoteViews(this)
    } else {
        clone()
    }

@RequiresApi(Build.VERSION_CODES.P)
private object RemoteViewsTranslatorApi28Impl {
    @DoNotInline
    fun copyRemoteViews(rv: RemoteViews) = RemoteViews(rv)
}

@RequiresApi(Build.VERSION_CODES.S)
private object RemoteViewsTranslatorApi31Impl {
    @DoNotInline
    fun addChildView(rv: RemoteViews, viewId: Int, childView: RemoteViews, stableId: Int) {
        rv.addStableView(viewId, childView, stableId)
    }
}
