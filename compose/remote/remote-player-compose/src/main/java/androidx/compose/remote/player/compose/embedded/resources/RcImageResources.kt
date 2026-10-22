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

package androidx.compose.remote.player.compose.embedded.resources

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.BitmapData
import androidx.compose.remote.player.compose.embedded.RcImageLoader
import androidx.compose.remote.player.compose.embedded.doc.GraphContext
import androidx.compose.remote.player.compose.embedded.doc.writeContext
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf

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

/** Sentinel cached in `RemoteComposeState` when decoding a [BitmapData] fails. */
private object FailedBitmapDecode

/**
 * Returns the decoded [Bitmap] for [id], decoding it on first use (lazy bitmap loading).
 *
 * The embedded player registers each [BitmapData]'s metadata at setup (via `putObject`, so declared
 * width/height stay available to `ImageAttribute` without a decode) but defers the costly pixel
 * decode until the bitmap is actually needed — when it is drawn, or when its Image component first
 * composes. The decoded bitmap is cached in the state's data map (`getFromId`), so later lookups
 * are cheap. Returns null if there is no bitmap or metadata for the id.
 *
 * [remoteContext] may be a read-only [GraphContext]; the decode is written through its
 * [writeContext].
 */
internal fun resolveBitmap(remoteContext: RemoteContext, id: Int): Bitmap? {
    val cached = remoteContext.mRemoteComposeState.getFromId(id)
    if (cached is Bitmap) return cached
    if (cached === FailedBitmapDecode) return null
    // Not decoded yet: find the registered BitmapData and decode it now (apply = putObject +
    // loadBitmap, which caches the decoded Bitmap under the id).
    val data = remoteContext.mRemoteComposeState.getObject(id) as? BitmapData ?: return null
    val decoded =
        try {
            data.apply(remoteContext.writeContext)
            remoteContext.mRemoteComposeState.getFromId(id) as? Bitmap
        } catch (_: Exception) {
            null
        }
    if (decoded == null) {
        remoteContext.mRemoteComposeState.cacheData(id, FailedBitmapDecode)
    }
    return decoded
}

/**
 * Resolves a document image draw to a [Bitmap] through the pluggable [RcImageLoader] (on [graph]),
 * falling back to the embedded decode ([resolveBitmap]). Reading the loader's `State` here
 * registers the draw as an observer, so a host's asynchronously-loaded image re-runs the draw when
 * it arrives.
 *
 * The canvas blit ops need a [Bitmap] (for src/dst sub-rect blitting), so only a [BitmapDrawable]
 * from the loader is used directly; any other host [Drawable] falls back to the embedded bitmap.
 * (The composable Image layout, by contrast, can render any [Drawable].)
 */
internal fun resolveCanvasBitmap(
    graph: GraphContext?,
    remoteContext: RemoteContext,
    id: Int,
): Bitmap? {
    val loaded = graph?.imageLoader?.loadImage(id)?.value
    if (loaded is BitmapDrawable) return loaded.bitmap
    return resolveBitmap(remoteContext, id)
}
