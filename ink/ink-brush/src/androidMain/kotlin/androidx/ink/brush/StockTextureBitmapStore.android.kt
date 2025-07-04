/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.brush

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.RestrictTo

/**
 * A [TextureBitmapStore] that automatically loads texture bitmaps for [StockBrushes].
 *
 * Pass this to your stroke renderer (e.g. [CanvasStrokeRenderer] and/or [InProgressStrokesView]) to
 * give it access to the textures.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public class StockTextureBitmapStore(private val resources: Resources) : TextureBitmapStore {
    private val idToBitmap = mutableMapOf<String, Bitmap>()

    /**
     * Returns the bitmap for the given client texture ID, or null if there is none.
     *
     * If [clientTextureId] is one of the [StockBrushes] texture IDs, the bitmap will be loaded
     * automatically. Otherwise, the bitmap must be added to the store with [addTexture] before it
     * can be retrieved.
     *
     * Automatic loading of [StockBrushes] textures may cause rendering jank on the first use of
     * each textured brush family, as it needs to load and decode a bitmap from [resources]. To
     * prevent this, call [preloadStockBrushesTextures] in advance.
     */
    public override operator fun get(clientTextureId: String): Bitmap? =
        idToBitmap[clientTextureId]
            ?: when (clientTextureId) {
                StockBrushes.pencilUnstableBackgroundTextureId -> R.drawable.pencil_background_v1
                else -> null
            }?.let { resourceId ->
                // computeIfAbsent is not available until API 24 (Android N).
                checkNotNull(BitmapFactory.decodeResource(resources, resourceId)) {
                        "Failed to decode resource $resourceId for stock brush texture ID $clientTextureId"
                    }
                    .also { idToBitmap.put(clientTextureId, it) }
            }

    /**
     * Preloads the textures for the given [BrushFamily].
     *
     * This does not modify the store if the [BrushFamily] does not use any [StockBrushes] textures,
     * or if the textures are already loaded.
     */
    @OptIn(ExperimentalInkCustomBrushApi::class)
    public fun preloadStockBrushesTextures(brushFamily: BrushFamily) {
        for (coat in brushFamily.coats) {
            for (layer in coat.paint.textureLayers) {
                if (layer.clientTextureId.isNotEmpty()) {
                    @Suppress("CheckReturnValue") get(layer.clientTextureId)
                }
            }
        }
    }

    /** Whether the store contains a texture with the given client . */
    public fun contains(clientTextureId: String): Boolean = idToBitmap[clientTextureId] != null

    /**
     * Adds a texture to the store.
     *
     * This call is only necessary if you want to load a texture for a brush family defined outside
     * of [StockBrushes]. All the [StockBrushes] textures are loaded automatically.
     *
     * Note that this will overwrite any existing texture with the same client texture ID; check
     * with [contains] first if you want to avoid that.
     *
     * @return The bitmap that was previously stored for the given client texture ID, or null if
     *   there was none.
     */
    public fun addTexture(clientTextureId: String, bitmap: Bitmap): Bitmap? =
        idToBitmap.put(clientTextureId, bitmap)
}
