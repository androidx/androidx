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

package androidx.wear.tiles.tooling

import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.TilePreview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tiles.tooling.preview.TilePreviewHelper.singleTimelineEntryTileBuilder

private const val RESOURCES_VERSION = "1"
private val resources = ResourceBuilders.Resources.Builder()
    .setVersion(RESOURCES_VERSION)
    .build()

private fun layoutElement() = LayoutElementBuilders.Text.Builder()
    .setText("Hello world!")
    .setFontStyle(
        LayoutElementBuilders.FontStyle.Builder()
            .setColor(argb(0xFF000000.toInt()))
            .build()
    ).build()

private fun layout() = LayoutElementBuilders.Layout.Builder().setRoot(layoutElement()).build()

private fun tile() = TileBuilders.Tile.Builder()
    .setResourcesVersion(RESOURCES_VERSION)
    .setTileTimeline(
        TimelineBuilders.Timeline.Builder().addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder().setLayout(
                layout()
            ).build()
        ).build()
    ).build()

@TilePreview
fun tilePreview() = TilePreviewData(
    onTileResourceRequest = { resources },
    onTileRequest = { tile() },
)

@TilePreview
fun tileLayoutPreview() = TilePreviewData {
    singleTimelineEntryTileBuilder(layout()).build()
}

@TilePreview
fun tileLayoutElementPreview() = TilePreviewData {
    singleTimelineEntryTileBuilder(layoutElement()).build()
}

@TilePreview
private fun tilePreviewWithPrivateVisibility() = TilePreviewData { tile() }

fun duplicateFunctionName(x: Int) = x

@TilePreview
fun duplicateFunctionName() = TilePreviewData { tile() }

@TilePreview
fun tilePreviewWithContextParameter(@Suppress("UNUSED_PARAMETER") context: Context) =
    TilePreviewData { tile() }

@TilePreview
fun tilePreviewWithWrongReturnType() = Unit

@TilePreview
fun tilePreviewWithNonContextParameter(@Suppress("UNUSED_PARAMETER") i: Int) =
    TilePreviewData { tile() }

class SomeClass {
    @TilePreview
    fun nonStaticMethod() = TilePreviewData { tile() }
}
