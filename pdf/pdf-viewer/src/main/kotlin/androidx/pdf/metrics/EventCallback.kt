/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.metrics

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.pdf.util.TileBoard.TileInfo

@RestrictTo(RestrictTo.Scope.LIBRARY)
@MainThread
public interface EventCallback {

    /** To be called as soon as viewer (page 0) is visible. */
    public fun onViewerVisible() {}

    /** To be called when a page [pageNum] is loaded in view. */
    public fun onPageVisible(pageNum: Int) {}

    /** To be called when a page [pageNum] is zoomed to [stableZoom] level. */
    public fun onPageZoomed(pageNum: Int, stableZoom: Float) {}

    /** To be called when Bitmap is requested for a page [pageNum]. */
    public fun onPageBitmapOnlyRequested(pageNum: Int) {}

    /** To be called when Tiles [tiles] are requested for a page [pageNum]. */
    public fun onPageTilesRequested(pageNum: Int, tiles: Iterable<TileInfo>) {}

    /** To be called when Bitmap is delivered for a page [pageNum]. */
    public fun onPageBitmapDelivered(pageNum: Int) {}

    /** To be called when a Tile [tile] is delivered for a page [pageNum]. */
    public fun onTileBitmapDelivered(pageNum: Int, tile: TileInfo) {}

    /** To be called when a page [pageNum] view is cleared. */
    public fun onPageCleared(pageNum: Int) {}

    /** To be called when a tiled page [pageNum] view is cleared. */
    public fun onPageTilesCleared(pageNum: Int) {}

    /** To be called when the pdf-viewer is reset. */
    public fun onViewerReset() {}
}
