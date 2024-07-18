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

import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders

internal const val PERMANENT_RESOURCES_VERSION = "0"
private val defaultResources = Resources.Builder()
    .setVersion(PERMANENT_RESOURCES_VERSION)
    .build()

/**
 * Container class storing callbacks required to render previews for methods annotated with
 * [TilePreview].
 *
 * @param onTileResourceRequest callback that provides a [Resources]. It will be called before
 * rendering the preview of the [TileBuilders.Tile]. By default, this callback will return a
 * [Resources] with the version "0".
 * @param onTileRequest callback that provides the [TileBuilders.Tile] to be previewed. It will be
 * called before rendering the preview.
 *
 * @see [TilePreviewHelper.singleTimelineEntryTileBuilder]
 */
class TilePreviewData
@JvmOverloads constructor(
    val onTileResourceRequest: (ResourcesRequest) -> Resources = { defaultResources },
    val onTileRequest: (TileRequest) -> TileBuilders.Tile,
) {
    override fun toString(): String {
        return "TilePreviewData(onTileResourceRequest=$onTileResourceRequest," +
            " onTileRequest=$onTileRequest)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TilePreviewData

        if (onTileResourceRequest != other.onTileResourceRequest) return false
        if (onTileRequest != other.onTileRequest) return false

        return true
    }

    override fun hashCode(): Int {
        var result = onTileResourceRequest.hashCode()
        result = 31 * result + onTileRequest.hashCode()
        return result
    }
}
