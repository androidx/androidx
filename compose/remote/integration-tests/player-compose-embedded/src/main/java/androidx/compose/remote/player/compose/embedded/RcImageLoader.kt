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

@file:Suppress("RestrictedApiAndroidX", "PrimitiveInCollection")

package androidx.compose.remote.player.compose.embedded

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.remote.core.RemoteContext
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
 * Implementations must return a **stable** [State] for a given [bitmapId] (cache per id) so the
 * composable and the canvas observe the same value, and so an async completion updates both.
 */
@Stable
public fun interface RcImageLoader {
    /** A reactive holder for the [Drawable] of [bitmapId]; `null` until/unless one is available. */
    public fun loadImage(bitmapId: Int): State<Drawable?>
}

/**
 * Default [RcImageLoader]: resolves the document's embedded `BitmapData` for an id to a
 * [BitmapDrawable]. Decoding is lazy (on first request) and the result tracks the snapshot-backed
 * bitmap store, so a host swap of the underlying bitmap updates the returned [State]. States are
 * cached per id so repeated requests (composable + canvas) share one instance.
 */
internal class EmbeddedRcImageLoader(private val context: RemoteContext) : RcImageLoader {
    private val cache = HashMap<Int, State<Drawable?>>()

    override fun loadImage(bitmapId: Int): State<Drawable?> =
        cache.getOrPut(bitmapId) {
            // Trigger the lazy decode (a snapshot write, done outside any derived read), then
            // expose
            // the store-backed bitmap reactively as a Drawable.
            resolveBitmap(context, bitmapId)
            var lastBitmap: Bitmap? = null
            var lastDrawable: Drawable? = null
            derivedStateOf {
                val bitmap = context.mRemoteComposeState.getFromId(bitmapId) as? Bitmap
                if (bitmap !== lastBitmap) {
                    lastBitmap = bitmap
                    lastDrawable = bitmap?.let { BitmapDrawable(Resources.getSystem(), it) }
                }
                lastDrawable
            }
        }
}

/**
 * The active [RcImageLoader], provided by [RcPlayer] (default [EmbeddedRcImageLoader]). A host can
 * override it — via [RcPlayer]'s `imageLoader` parameter — to plug in its own image library.
 */
public val LocalRcImageLoader: ProvidableCompositionLocal<RcImageLoader> =
    staticCompositionLocalOf {
        error("No RcImageLoader provided")
    }
