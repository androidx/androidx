/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.tiles.tooling.preview

import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.TileBuilders

/** Helper object that provides methods to create Tiles for Preview purposes. */
object TilePreviewHelper {
    /**
     * Helper method that creates a [TileBuilders.Tile.Builder] with a timeline consisting of a
     * single timeline entry. The provided [LayoutElementBuilders.Layout] is used as the layout
     * of the timeline entry.
     *
     * @param layout The layout that will be used to create the single timeline entry in the
     * [TileBuilders.Tile.Builder].
     */
    @JvmStatic
    fun singleTimelineEntryTileBuilder(
        layout: LayoutElementBuilders.Layout,
    ): TileBuilders.Tile.Builder = TileBuilders.Tile.Builder()
        .setResourcesVersion(PERMANENT_RESOURCES_VERSION)
        .setTileTimeline(
            TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder()
                        .setLayout(layout)
                        .build()
                )
                .build()
        )

    /**
     * Helper method that creates a [TileBuilders.Tile.Builder] with a timeline consisting of a
     * single timeline entry. The provided [LayoutElementBuilders.LayoutElement] is wrapped in a
     * [LayoutElementBuilders.Box] and used as the timeline entry's layout.
     *
     * @param layoutElement The layout element that will be used to create a single entry timeline
     * [TileBuilders.Tile.Builder]. This layout element will be added to a
     * [LayoutElementBuilders.Box] which will then be used as the layout root of the
     * [TileBuilders.Tile.Builder]'s timeline entry. The layout element will be aligned in the
     * center of the [LayoutElementBuilders.Box].
     */
    @JvmStatic
    fun singleTimelineEntryTileBuilder(
        layoutElement: LayoutElementBuilders.LayoutElement,
    ): TileBuilders.Tile.Builder = singleTimelineEntryTileBuilder(
        layout = LayoutElementBuilders.Layout.Builder()
            .setRoot(
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                    .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                    .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                    .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
                    .addContent(layoutElement)
                    .build()
            )
            .build()
    )
}
