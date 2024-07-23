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

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.content.Intent.FILL_IN_COMPONENT
import android.widget.RemoteViews
import androidx.glance.appwidget.InsertedViewInfo
import androidx.glance.appwidget.LayoutType
import androidx.glance.appwidget.RemoteCollectionItems
import androidx.glance.appwidget.TopLevelLayoutsCount
import androidx.glance.appwidget.TranslationContext
import androidx.glance.appwidget.applyModifiers
import androidx.glance.appwidget.insertView
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.EmittableLazyList
import androidx.glance.appwidget.lazy.EmittableLazyListItem
import androidx.glance.appwidget.lazy.ReservedItemIdRangeEnd
import androidx.glance.appwidget.setRemoteAdapter
import androidx.glance.appwidget.toSizeString
import androidx.glance.appwidget.translateChild
import androidx.glance.appwidget.translateComposition
import androidx.glance.layout.Alignment

internal fun RemoteViews.translateEmittableLazyColumn(
    translationContext: TranslationContext,
    element: EmittableLazyColumn,
) {
    val viewDef = insertView(translationContext, LayoutType.List, element.modifier)
    translateEmittableLazyList(
        translationContext,
        element,
        viewDef,
    )
}

private fun RemoteViews.translateEmittableLazyList(
    translationContext: TranslationContext,
    element: EmittableLazyList,
    viewDef: InsertedViewInfo,
) {
    check(!translationContext.isLazyCollectionDescendant) {
        "Glance does not support nested list views."
    }
    setPendingIntentTemplate(
        viewDef.mainViewId,
        PendingIntent.getActivity(
            translationContext.context,
            0,
            Intent(),
            FILL_IN_COMPONENT or
                FLAG_MUTABLE or
                FLAG_UPDATE_CURRENT or
                FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT,
            element.activityOptions,
        )
    )
    val items =
        RemoteCollectionItems.Builder()
            .apply {
                val childContext = translationContext.forLazyCollection(viewDef.mainViewId)
                element.children
                    .foldIndexed(false) { position, previous, itemEmittable ->
                        itemEmittable as EmittableLazyListItem
                        val itemId = itemEmittable.itemId
                        addItem(
                            itemId,
                            translateComposition(
                                childContext.forLazyViewItem(position, LazyListItemStartingViewId),
                                listOf(itemEmittable),
                                translationContext.layoutConfiguration?.addLayout(itemEmittable)
                                    ?: -1,
                            )
                        )
                        // If the user specifies any explicit ids, we assume the list to be stable
                        previous || (itemId > ReservedItemIdRangeEnd)
                    }
                    .let { setHasStableIds(it) }
                setViewTypeCount(TopLevelLayoutsCount)
            }
            .build()
    setRemoteAdapter(
        translationContext,
        viewDef.mainViewId,
        translationContext.layoutSize.toSizeString(),
        items
    )
    applyModifiers(translationContext, this, element.modifier, viewDef)
}

/**
 * Translates a list item either to its immediate only child, or a column layout wrapping all its
 * children.
 */
// TODO(b/202382495): Use complex generated layout instead of wrapping in an emittable box to
// support interaction animations in immediate children, e.g. checkboxes,  pre-S
internal fun RemoteViews.translateEmittableLazyListItem(
    translationContext: TranslationContext,
    element: EmittableLazyListItem
) {
    require(element.children.size == 1 && element.alignment == Alignment.CenterStart) {
        "Lazy list items can only have a single child align at the center start of the view. " +
            "The normalization of the composition tree failed."
    }
    translateChild(translationContext, element.children.first())
}

// All the lazy list items should use the same ids, to ensure the layouts can be re-used.
// Using a very high number to avoid collision with the main app widget ids.
private const val LazyListItemStartingViewId: Int = 0x00100000
