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
import androidx.core.widget.RemoteViewsCompat
import androidx.glance.appwidget.layout.EmittableLazyColumn
import androidx.glance.appwidget.layout.EmittableLazyList
import androidx.glance.appwidget.layout.EmittableLazyListItem
import androidx.glance.appwidget.layout.ReservedItemIdRangeEnd
import androidx.glance.layout.EmittableBox

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
    val listLayout =
        selectLayout(listLayoutType, element.modifier)
    return translateEmittableLazyList(
        translationContext,
        element,
        listLayout,
    )
}

private fun translateEmittableLazyList(
    translationContext: TranslationContext,
    element: EmittableLazyList,
    layoutDef: LayoutIds,
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
                // TODO(b/198618359): assign an explicit view type count
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

private val listLayouts: List<LayoutSelector.Type> = listOf(
    LayoutSelector.Type.List1,
    LayoutSelector.Type.List2,
    LayoutSelector.Type.List3,
)
