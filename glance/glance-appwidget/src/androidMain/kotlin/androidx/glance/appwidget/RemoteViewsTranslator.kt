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

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.util.SizeF
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.widget.RemoteViewsCompat.setLinearLayoutGravity
import androidx.glance.Emittable
import androidx.glance.EmittableButton
import androidx.glance.EmittableImage
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.appwidget.lazy.EmittableLazyVerticalGrid
import androidx.glance.appwidget.lazy.EmittableLazyVerticalGridListItem
import androidx.glance.appwidget.translators.setText
import androidx.glance.appwidget.translators.translateEmittableCheckBox
import androidx.glance.appwidget.translators.translateEmittableCircularProgressIndicator
import androidx.glance.appwidget.translators.translateEmittableImage
import androidx.glance.appwidget.translators.translateEmittableLazyColumn
import androidx.glance.appwidget.translators.translateEmittableLazyListItem
import androidx.glance.appwidget.translators.translateEmittableLazyVerticalGrid
import androidx.glance.appwidget.translators.translateEmittableLazyVerticalGridListItem
import androidx.glance.appwidget.translators.translateEmittableLinearProgressIndicator
import androidx.glance.appwidget.translators.translateEmittableRadioButton
import androidx.glance.appwidget.translators.translateEmittableSwitch
import androidx.glance.appwidget.translators.translateEmittableText
import androidx.glance.findModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.EmittableColumn
import androidx.glance.layout.EmittableRow
import androidx.glance.layout.EmittableSpacer
import androidx.glance.layout.PaddingModifier
import androidx.glance.layout.padding
import androidx.glance.text.EmittableText
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal fun translateComposition(
    context: Context,
    appWidgetId: Int,
    element: RemoteViewsRoot,
    layoutConfiguration: LayoutConfiguration?,
    rootViewIndex: Int,
    layoutSize: DpSize,
    actionBroadcastReceiver: ComponentName? = null,

) =
    translateComposition(
        TranslationContext(
            context,
            appWidgetId,
            context.isRtl,
            layoutConfiguration,
            itemPosition = -1,
            layoutSize = layoutSize,
            actionBroadcastReceiver = actionBroadcastReceiver,
        ),
        element.children,
        rootViewIndex,
    )

@VisibleForTesting
internal var forceRtl: Boolean? = null

private val Context.isRtl: Boolean
    get() = forceRtl
        ?: (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)

@RequiresApi(Build.VERSION_CODES.S)
private object Api31Impl {
    @DoNotInline
    fun createRemoteViews(sizeMap: Map<SizeF, RemoteViews>): RemoteViews = RemoteViews(sizeMap)
}

internal fun translateComposition(
    translationContext: TranslationContext,
    children: List<Emittable>,
    rootViewIndex: Int
): RemoteViews {
    if (children.all { it is EmittableSizeBox }) {
        // If the children of root are all EmittableSizeBoxes, then we must translate each
        // EmittableSizeBox into a distinct RemoteViews object. Then, we combine them into one
        // multi-sized RemoteViews (a RemoteViews that contains either landscape & portrait RVs or
        // multiple RVs mapped by size).
        val sizeMode = (children.first() as EmittableSizeBox).sizeMode
        val views = children.map { child ->
            val size = (child as EmittableSizeBox).size
            val remoteViewsInfo = createRootView(translationContext, child.modifier, rootViewIndex)
            val rv = remoteViewsInfo.remoteViews.apply {
                translateChild(translationContext.forRoot(root = remoteViewsInfo), child)
            }
            size.toSizeF() to rv
        }
        return when (sizeMode) {
            is SizeMode.Single -> views.single().second
            is SizeMode.Responsive, SizeMode.Exact -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31Impl.createRemoteViews(views.toMap())
                } else {
                    require(views.size == 1 || views.size == 2)
                    combineLandscapeAndPortrait(views.map { it.second })
                }
            }
        }
    } else {
        return children.single().let { child ->
            val remoteViewsInfo = createRootView(translationContext, child.modifier, rootViewIndex)
            remoteViewsInfo.remoteViews.apply {
                translateChild(translationContext.forRoot(root = remoteViewsInfo), child)
            }
        }
    }
}

private fun combineLandscapeAndPortrait(views: List<RemoteViews>): RemoteViews =
    when (views.size) {
        2 -> RemoteViews(views[0], views[1])
        1 -> views[0]
        else -> throw IllegalArgumentException("There must be between 1 and 2 views.")
    }

private const val LastInvalidViewId = 1

internal data class TranslationContext(
    val context: Context,
    val appWidgetId: Int,
    val isRtl: Boolean,
    val layoutConfiguration: LayoutConfiguration?,
    val itemPosition: Int,
    val isLazyCollectionDescendant: Boolean = false,
    val lastViewId: AtomicInteger = AtomicInteger(LastInvalidViewId),
    val parentContext: InsertedViewInfo = InsertedViewInfo(),
    val isBackgroundSpecified: AtomicBoolean = AtomicBoolean(false),
    val layoutSize: DpSize = DpSize.Zero,
    val layoutCollectionViewId: Int = View.NO_ID,
    val layoutCollectionItemId: Int = -1,
    val canUseSelectableGroup: Boolean = false,
    val actionTargetId: Int? = null,
    val actionBroadcastReceiver: ComponentName? = null
) {
    fun nextViewId() = lastViewId.incrementAndGet()

    fun forChild(parent: InsertedViewInfo, pos: Int): TranslationContext =
        copy(itemPosition = pos, parentContext = parent)

    fun forRoot(root: RemoteViewsInfo): TranslationContext =
        forChild(pos = 0, parent = root.view)
            .copy(
                isBackgroundSpecified = AtomicBoolean(false),
                lastViewId = AtomicInteger(LastInvalidViewId),
            )

    fun resetViewId(newViewId: Int = 0) = copy(lastViewId = AtomicInteger(newViewId))

    fun forLazyCollection(viewId: Int) =
        copy(isLazyCollectionDescendant = true, layoutCollectionViewId = viewId)

    fun forLazyViewItem(itemId: Int, newViewId: Int = 0) =
        copy(lastViewId = AtomicInteger(newViewId), layoutCollectionViewId = itemId)

    fun canUseSelectableGroup() = copy(canUseSelectableGroup = true)

    fun forActionTargetId(viewId: Int) = copy(actionTargetId = viewId)
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
        is EmittableLazyVerticalGrid -> {
            translateEmittableLazyVerticalGrid(translationContext, element)
        }
        is EmittableLazyVerticalGridListItem -> {
          translateEmittableLazyVerticalGridListItem(translationContext, element)
        }
        is EmittableRadioButton -> translateEmittableRadioButton(translationContext, element)
        is EmittableSizeBox -> translateEmittableSizeBox(translationContext, element)
        else -> {
            throw IllegalArgumentException(
                "Unknown element type ${element.javaClass.canonicalName}"
            )
        }
    }
}

internal fun RemoteViews.translateEmittableSizeBox(
    translationContext: TranslationContext,
    element: EmittableSizeBox
) {
    require(element.children.size <= 1) {
        "Size boxes can only have at most one child ${element.children.size}. " +
            "The normalization of the composition tree failed."
    }
    element.children.firstOrNull()?.let { translateChild(translationContext, it) }
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
    element.children.forEach {
        it.modifier = it.modifier.then(AlignmentModifier(element.contentAlignment))
    }
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
    val layoutType = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && element.modifier.isSelectableGroup
    ) {
        LayoutType.RadioRow
    } else {
        LayoutType.Row
    }
    val viewDef = insertContainerView(
        translationContext,
        layoutType,
        element.children.size,
        element.modifier,
        horizontalAlignment = null,
        verticalAlignment = element.verticalAlignment,
    )
    setLinearLayoutGravity(
        viewDef.mainViewId,
        Alignment(element.horizontalAlignment, element.verticalAlignment).toGravity()
    )
    applyModifiers(
        translationContext.canUseSelectableGroup(),
        this,
        element.modifier,
        viewDef
    )
    setChildren(
        translationContext,
        viewDef,
        element.children
    )
    if (element.modifier.isSelectableGroup) checkSelectableGroupChildren(element.children)
}

private fun RemoteViews.translateEmittableColumn(
    translationContext: TranslationContext,
    element: EmittableColumn
) {
    val layoutType = if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && element.modifier.isSelectableGroup
    ) {
        LayoutType.RadioColumn
    } else {
        LayoutType.Column
    }
    val viewDef = insertContainerView(
        translationContext,
        layoutType,
        element.children.size,
        element.modifier,
        horizontalAlignment = element.horizontalAlignment,
        verticalAlignment = null,
    )
    setLinearLayoutGravity(
        viewDef.mainViewId,
        Alignment(element.horizontalAlignment, element.verticalAlignment).toGravity()
    )
    applyModifiers(
        translationContext.canUseSelectableGroup(),
        this,
        element.modifier,
        viewDef
    )
    setChildren(
        translationContext,
        viewDef,
        element.children
    )
    if (element.modifier.isSelectableGroup) checkSelectableGroupChildren(element.children)
}

private fun checkSelectableGroupChildren(children: List<Emittable>) {
    check(children.count { it is EmittableRadioButton && it.checked } <= 1) {
        "When using GlanceModifier.selectableGroup(), no more than one RadioButton " +
        "may be checked at a time."
    }
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
    check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        "Buttons in Android R and below are emulated using a EmittableBox containing the text."
    }
    val viewDef = insertView(translationContext, LayoutType.Button, element.modifier)
    setText(
        translationContext,
        viewDef.mainViewId,
        element.text,
        element.style,
        maxLines = element.maxLines,
        verticalTextGravity = Gravity.CENTER_VERTICAL,
    )

    // Adjust appWidget specific modifiers.
    element.modifier = element.modifier
        .enabled(element.enabled)
        .cornerRadius(16.dp)
    if (element.modifier.findModifier<PaddingModifier>() == null) {
        element.modifier = element.modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    }
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
    children: List<Emittable>
) {
    children.take(10).forEachIndexed { index, child ->
        translateChild(
            translationContext.forChild(parent = parentDef, pos = index),
            child,
        )
    }
}

/**
 * Add stable view if on Android S+, otherwise simply add the view.
 */
internal fun RemoteViews.addChildView(viewId: Int, childView: RemoteViews, stableId: Int) {
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
