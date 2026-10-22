/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import androidx.compose.remote.player.compose.embedded.resources.EmbeddedRcImageLoader
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Pluggable image loader so the embedded player doesn't depend on any specific image-loading
 * library.
 *
 * The player resolves a document image reference (a `BitmapData` id) to a [Drawable] through this
 * interface. The default ([EmbeddedRcImageLoader]) wraps the document's embedded, already-decoded
 * bitmaps; a host can supply its own loader — backed by Coil, Glide, a disk cache, etc. — to
 * resolve ids the document doesn't carry pixels for (e.g. a host-substituted or remotely-fetched
 * image).
 *
 * [loadImage] returns a [State] rather than a plain value so the result works in both worlds:
 * - in a `@Composable` (the `Image` recomposes when an asynchronously-loaded drawable arrives), and
 * - on the canvas draw path, read imperatively via `state.value`.
 *
 * Implementations must return a **stable** [State] for a given `bitmapId` (cache per id) so the
 * composable and the canvas observe the same value, and so an async completion updates both.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun interface RcImageLoader {
    /** A reactive holder for the [Drawable] of `bitmapId`; `null` until/unless one is available. */
    public fun loadImage(bitmapId: Int): State<Drawable?>
}

/**
 * The active [RcImageLoader], provided by [RcPlayer] (default [EmbeddedRcImageLoader]). A host can
 * override it — via [RcPlayer]'s `imageLoader` parameter — to plug in its own image library.
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val LocalRcImageLoader: ProvidableCompositionLocal<RcImageLoader> =
    staticCompositionLocalOf {
        error("No RcImageLoader provided")
    }
