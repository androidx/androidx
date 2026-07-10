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
import androidx.wear.protolayout.expression.PlatformDataKey
import androidx.wear.protolayout.expression.PlatformDataValues
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders
import java.util.Objects

internal const val PERMANENT_RESOURCES_VERSION = "0"
private val defaultResources = Resources.Builder().setVersion(PERMANENT_RESOURCES_VERSION).build()

/**
 * Container class storing data required to render previews for methods annotated with [Preview].
 *
 * @param onTileResourceRequest an optional callback that provides a [Resources]. If the layout
 *   provided in [onTileRequest] uses automatic resource registration (either from
 *   [androidx.wear.protolayout.material3.materialScopeWithResources] or other methods from
 *   [androidx.wear.protolayout.ProtoLayoutScope]), this callback will not be needed and, if
 *   provided, will be ignored. In other cases, it will be called before rendering the preview of
 *   the [TileBuilders.Tile]. By default, this callback will return resources automatically
 *   collected from the rendered tile via [androidx.wear.protolayout.ProtoLayoutScope] if they
 *   exist, or [Resources] with the version "0" otherwise.
 * @param platformDataValues allows overriding platform data values for any [PlatformDataKey].
 *   Default platform data values will be set for all platform health sources that have not been
 *   overridden.
 * @param onTileRequest callback that provides the [TileBuilders.Tile] to be previewed. It will be
 *   called before rendering the preview.
 * @see [TilePreviewHelper.singleTimelineEntryTileBuilder]
 */
public class TilePreviewData
@JvmOverloads
constructor(
    public val onTileResourceRequest: (ResourcesRequest) -> Resources = { defaultResources },
    public val platformDataValues: PlatformDataValues? = null,
    public val onTileRequest: (TileRequest) -> TileBuilders.Tile,
) {
    override fun toString(): String {
        return "TilePreviewData(onTileResourceRequest=$onTileResourceRequest, " +
            "platformDataValues=$platformDataValues,  onTileRequest=$onTileRequest)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TilePreviewData

        if (onTileResourceRequest != other.onTileResourceRequest) return false
        if (platformDataValues != other.platformDataValues) return false
        if (onTileRequest != other.onTileRequest) return false

        return true
    }

    override fun hashCode(): Int =
        Objects.hash(onTileResourceRequest, platformDataValues, onTileRequest)
}
