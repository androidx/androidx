/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ink.brush

import android.graphics.Bitmap

/**
 * Interface for a callback to allow the caller to provide a particular [Bitmap] corresponding to a
 * client-provided texture ID.
 */
public fun interface TextureBitmapStore {
    /**
     * Retrieve a [Bitmap] for the given texture id. This may be called synchronously during
     * `onDraw`, so loading of texture files from disk and decoding them into [Bitmap] objects
     * should be done on init. The result may be cached by consumers, so this should return a
     * deterministic result for a given input.
     *
     * Textures can be disabled by having load always return null. null should also be returned when
     * a texture can not be loaded. If null is returned, the texture layer in question should be
     * ignored, allowing for graceful fallback. It's recommended that implementations log when a
     * texture can not be loaded.
     *
     * @return The texture bitmap, if any, associated with the given id.
     */
    public operator fun get(clientTextureId: String): Bitmap?

    /**
     * Returns the texture ID to write in place of [clientTextureId] when a `BrushFamily` is encoded
     * with this store using `androidx.ink.storage`. By default, texture IDs are written unchanged.
     * This is not used for rendering.
     *
     * Override this to undo any change made to texture IDs when decoding (in
     * `androidx.ink.storage.BrushFamilyDecodeCallback.onDecodeTexture`), for example to remove a
     * prefix that was added to keep texture IDs unique within this store, so that re-encoding a
     * decoded `BrushFamily` writes the texture IDs that it was originally serialized with.
     *
     * This is called whether or not [get] returns a bitmap for [clientTextureId]. The result may be
     * cached by consumers, so this should return a deterministic result for a given input. If two
     * different client texture IDs used in the same `BrushFamily` return the same encoded ID, their
     * layers will share that encoded ID in the serialized form and only the first texture's bitmap
     * will be stored. If `BrushFamily.hasFallbacks` is true for a `BrushFamily` that is encoded on
     * its own, its stored serialized form, which already has the original texture IDs, is written
     * instead, so this has no effect.
     *
     * @param clientTextureId The texture ID used by the `BrushFamily` being encoded.
     * @return The texture ID to write into the serialized `BrushFamily`.
     */
    @ExperimentalInkBrushCompatibilityApi
    public fun getEncodedTextureId(clientTextureId: String): String = clientTextureId
}
