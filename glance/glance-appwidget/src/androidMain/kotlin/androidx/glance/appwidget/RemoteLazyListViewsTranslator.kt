@file:OptIn(GlanceInternalApi::class)
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

import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.widget.RemoteViewsCompat
import androidx.glance.GlanceInternalApi
import androidx.glance.appwidget.layout.EmittableLazyColumn
import androidx.glance.appwidget.layout.EmittableLazyList
import androidx.glance.appwidget.layout.EmittableLazyListItem
import androidx.glance.appwidget.layout.ReservedItemIdRangeEnd
import androidx.glance.layout.EmittableBox

@OptIn(GlanceInternalApi::class)
internal fun translateEmittableLazyColumn(
    translationContext: TranslationContext,
    element: EmittableLazyColumn,
): RemoteViews {
    val listLayout = requireNotNull(listLayouts.getOrNull(translationContext.listCount)) {
        """
            Glance widgets only support ${listLayouts.size} lazy lists per widget. If you need more
            lists provide a non-composable [RemoteViews].
        """.trimIndent()
    }
    translationContext.listCount++
    return translateEmittableLazyList(
        translationContext,
        element,
        listLayout.viewId,
        listLayout.layoutId
    )
}

@OptIn(GlanceInternalApi::class)
private fun translateEmittableLazyList(
    translationContext: TranslationContext,
    element: EmittableLazyList,
    @IdRes viewId: Int,
    @LayoutRes layoutId: Int
): RemoteViews = remoteViews(translationContext, layoutId)
    .also { rv ->
        val items = RemoteViewsCompat.RemoteCollectionItems.Builder().apply {
            element.children.fold(false) { previous, itemEmittable ->
                val itemId = (itemEmittable as EmittableLazyListItem).itemId
                addItem(itemId, translateChild(translationContext, itemEmittable))
                // If the user specifies any explicit ids, we assume the list to be stable
                previous || (itemId > ReservedItemIdRangeEnd)
            }.let { setHasStableIds(it) }
            // TODO(b/198618359): assign an explicit view type count
        }.build()
        RemoteViewsCompat.setRemoteAdapter(
            translationContext.context,
            rv,
            translationContext.appWidgetId,
            viewId,
            items
        )
        applyModifiers(translationContext.context, rv, element.modifier, viewId)
    }

/**
 * Translates a list item either to its immediate only child, or a column layout wrapping all its
 * children.
 */
internal fun translateEmittableLazyListItem(
    translationContext: TranslationContext,
    element: EmittableLazyListItem
): RemoteViews =
    if (element.children.size == 1) {
        translateChild(translationContext, element.children.single())
    } else {
        translateChild(
            translationContext,
            EmittableBox().also { it.children.addAll(element.children) }
        )
    }

private data class LazyListLayout(@IdRes val viewId: Int, @LayoutRes val layoutId: Int)

private val listLayouts: List<LazyListLayout> = listOf(
    LazyListLayout(R.id.glanceListView1, R.layout.list_layout_1),
    LazyListLayout(R.id.glanceListView2, R.layout.list_layout_2),
    LazyListLayout(R.id.glanceListView2, R.layout.list_layout_3),
)
