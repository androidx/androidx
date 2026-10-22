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
import androidx.compose.remote.core.operations.ImageAttribute
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
 * Returns true if this [BitmapData] uses placeholder dimensions (`0x0`, or `1x1` on
 * [BitmapData.ENCODING_URL]/[BitmapData.ENCODING_FILE]) that should be populated from the loaded
 * image.
 */
internal fun BitmapData.hasIntrinsicDimensions(): Boolean =
    isIntrinsicPlaceholder(width, height, encoding)

/**
 * Returns true if [width]x[height] are placeholder dimensions for a bitmap with [encoding] (`0x0`,
 * or `1x1` on [BitmapData.ENCODING_URL]/[BitmapData.ENCODING_FILE]), meaning the real size comes
 * from the loaded image.
 */
internal fun isIntrinsicPlaceholder(width: Int, height: Int, encoding: Short): Boolean =
    (width == 0 && height == 0) ||
        (width == 1 &&
            height == 1 &&
            (encoding == BitmapData.ENCODING_URL || encoding == BitmapData.ENCODING_FILE))

/** Sets this [BitmapData]'s dimensions to those of [bitmap], e.g. when a host overrides it. */
internal fun BitmapData.setDimensions(bitmap: Bitmap) {
    width = bitmap.width
    height = bitmap.height
}

/**
 * Populates this [BitmapData]'s dimensions with [width]x[height] if it still has placeholder
 * dimensions (see [hasIntrinsicDimensions]) and the new size is non-empty.
 */
internal fun BitmapData.fillIntrinsicDimensions(width: Int, height: Int) {
    if (width > 0 && height > 0 && hasIntrinsicDimensions()) {
        this.width = width
        this.height = height
    }
}

/**
 * Populates this [BitmapData]'s placeholder dimensions from [drawable], preferring the backing
 * [Bitmap] of a [BitmapDrawable] over the drawable's intrinsic size.
 */
internal fun BitmapData.fillIntrinsicDimensions(drawable: Drawable) {
    val bitmap = (drawable as? BitmapDrawable)?.bitmap
    if (bitmap != null) {
        fillIntrinsicDimensions(bitmap.width, bitmap.height)
    } else {
        fillIntrinsicDimensions(drawable.intrinsicWidth, drawable.intrinsicHeight)
    }
}

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
    if (cached is Bitmap) {
        (remoteContext.mRemoteComposeState.getObject(id) as? BitmapData)?.fillIntrinsicDimensions(
            cached.width,
            cached.height,
        )
        return cached
    }
    if (cached === FailedBitmapDecode) return null
    // Not decoded yet: find the registered BitmapData and decode it now (apply = putObject +
    // loadBitmap, which caches the decoded Bitmap under the id).
    val data = remoteContext.mRemoteComposeState.getObject(id) as? BitmapData ?: return null
    val useIntrinsic = data.hasIntrinsicDimensions()
    val prevWidth = data.width
    val prevHeight = data.height
    if (useIntrinsic) {
        data.width = 0
        data.height = 0
    }
    val decoded =
        try {
            data.apply(remoteContext.writeContext)
            remoteContext.mRemoteComposeState.getFromId(id) as? Bitmap
        } catch (_: Exception) {
            null
        }
    if (decoded == null) {
        if (useIntrinsic) {
            data.width = prevWidth
            data.height = prevHeight
        }
        remoteContext.mRemoteComposeState.cacheData(id, FailedBitmapDecode)
    } else {
        data.fillIntrinsicDimensions(decoded.width, decoded.height)
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
    val bitmap =
        if (loaded is BitmapDrawable) {
            loaded.bitmap
        } else {
            resolveBitmap(remoteContext, id)
        }
    val data = remoteContext.mRemoteComposeState.getObject(id) as? BitmapData
    if (bitmap != null) {
        data?.fillIntrinsicDimensions(bitmap.width, bitmap.height)
    } else if (loaded != null) {
        data?.fillIntrinsicDimensions(loaded)
    }
    return bitmap
}

/**
 * Ensures [BitmapData] dimensions for [imageId] reflect any loaded or overridden image before
 * [ImageAttribute] reads them. When [BitmapData] has intrinsic placeholder dimensions (`0x0` or
 * `1x1` on URL/file bitmaps), triggers image resolution so the loaded image's dimensions populate
 * [BitmapData].
 */
internal fun resolveImageAttributeDimensions(
    graph: GraphContext?,
    remoteContext: RemoteContext,
    imageId: Int,
) {
    val bitmapData = remoteContext.mRemoteComposeState.getObject(imageId) as? BitmapData ?: return
    val loaded = graph?.imageLoader?.loadImage(imageId)?.value
    val bitmap =
        (loaded as? BitmapDrawable)?.bitmap
            ?: (remoteContext.mRemoteComposeState.getFromId(imageId) as? Bitmap)
    if (bitmap != null) {
        bitmapData.setDimensions(bitmap)
    } else if (loaded != null && loaded.intrinsicWidth > 0 && loaded.intrinsicHeight > 0) {
        bitmapData.width = loaded.intrinsicWidth
        bitmapData.height = loaded.intrinsicHeight
    } else if (bitmapData.hasIntrinsicDimensions()) {
        resolveCanvasBitmap(graph, remoteContext, imageId)
    }
}
