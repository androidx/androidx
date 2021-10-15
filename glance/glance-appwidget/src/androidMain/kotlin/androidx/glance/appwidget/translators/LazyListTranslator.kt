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

package androidx.glance.appwidget.translators

import android.widget.RemoteViews
import androidx.core.widget.RemoteViewsCompat
import androidx.glance.appwidget.GeneratedLayoutCount
import androidx.glance.appwidget.LayoutInfo
import androidx.glance.appwidget.LayoutSelector
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.layout.EmittableLazyColumn
import androidx.glance.appwidget.layout.EmittableLazyList
import androidx.glance.appwidget.layout.EmittableLazyListItem
import androidx.glance.appwidget.layout.ReservedItemIdRangeEnd
import androidx.glance.appwidget.remoteViews
import androidx.glance.appwidget.selectLayout
import androidx.glance.appwidget.translateChild
import androidx.glance.layout.Alignment
import androidx.glance.layout.EmittableBox
import androidx.glance.layout.fillMaxWidth

internal fun translateEmittableLazyColumn(
    translationContext: TranslationContext,
    element: EmittableLazyColumn,
): RemoteViews {
    val listLayoutType =
        requireNotNull(listLayouts.getOrNull(translationContext.listCount.getAndIncrement())) {
            """
            Glance widgets only support ${listLayouts.size} lazy lists per widget. If you need more
            lists provide a non-composable [RemoteViews].
            """.trimIndent()
        }
    val listLayout = selectLayout(translationContext, listLayoutType, element.modifier)
    return translateEmittableLazyList(
        translationContext,
        element,
        listLayout,
    )
}

private fun translateEmittableLazyList(
    translationContext: TranslationContext,
    element: EmittableLazyList,
    layoutDef: LayoutInfo,
): RemoteViews =
    remoteViews(translationContext, layoutDef.layoutId)
        .also { rv ->
            check(translationContext.areLazyCollectionsAllowed) {
                "Glance does not support nested list views."
            }
            val items = RemoteViewsCompat.RemoteCollectionItems.Builder().apply {
                val childContext = translationContext.copy(areLazyCollectionsAllowed = false)
                element.children.fold(false) { previous, itemEmittable ->
                    val itemId = (itemEmittable as EmittableLazyListItem).itemId
                    addItem(itemId, translateChild(childContext, itemEmittable))
                    // If the user specifies any explicit ids, we assume the list to be stable
                    previous || (itemId > ReservedItemIdRangeEnd)
                }.let { setHasStableIds(it) }
                setViewTypeCount(GeneratedLayoutCount)
            }.build()
            RemoteViewsCompat.setRemoteAdapter(
                translationContext.context,
                rv,
                translationContext.appWidgetId,
                layoutDef.mainViewId,
                items
            )
            applyModifiers(translationContext, rv, element.modifier, layoutDef)
        }

/**
 * Translates a list item either to its immediate only child, or a column layout wrapping all its
 * children.
 */
// TODO(b/202382495): Use complex generated layout instead of wrapping in an emittable box to
// support interaction animations in immediate children, e.g. checkboxes,  pre-S
internal fun translateEmittableLazyListItem(
    translationContext: TranslationContext,
    element: EmittableLazyListItem
): RemoteViews {
    val child = if (element.children.size == 1 && element.alignment == Alignment.CenterStart) {
        element.children.single()
    } else {
        EmittableBox().apply {
            modifier = modifier.fillMaxWidth()
            contentAlignment = element.alignment
            children.addAll(element.children)
        }
    }
    return translateChild(translationContext, child)
}

private val listLayouts: List<LayoutSelector.Type> = listOf(
    LayoutSelector.Type.List1,
    LayoutSelector.Type.List2,
    LayoutSelector.Type.List3,
)
