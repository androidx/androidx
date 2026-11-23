/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import platform.UIKit.UIImage

/**
 * Interface for a callback to allow the caller to provide a particular [UIImage] corresponding to a
 * client-provided texture ID.
 */
@ExperimentalInkCrossPlatformRenderingApi
public fun interface TextureImageStore {
    /**
     * Retrieve a [UIImage] for the given texture ID. This may be called synchronously during
     * drawing, so loading of texture files from disk and decoding them into [UIImage] objects
     * should be done on initialization. The result is expected to be cached by consumers, so this
     * should return a deterministic result for a given input.
     *
     * Since this will be read during the draw, this ideally should return [UIImage] instances
     * wrapping `CGImage`, that is, ones where the underlying bitmap is pre-loaded. Otherwise, the
     * first draw may be slower as texture images are rendered from `CIImage` to `CGImage`.
     *
     * Textures can be disabled by having this function always return `null`. `null` should also be
     * returned when a texture can not be loaded. If `null` is returned, the texture layer in
     * question should be ignored, allowing for graceful fallback. It's recommended that
     * implementations log when a texture can not be loaded.
     *
     * @return The texture image, if any, associated with the given ID.
     */
    public operator fun get(clientTextureId: String): UIImage?

    /**
     * Returns the texture ID to write in place of [clientTextureId] when a `BrushFamily` is encoded
     * with this store using `androidx.ink.storage`. By default, texture IDs are written unchanged.
     * This is not used for rendering.
     *
     * Override this to undo any change made to texture IDs when decoding (in
     * `androidx.ink.storage.OnDecodeTextureUiImage.onDecodeTexture`), for example to remove a
     * prefix that was added to keep texture IDs unique within this store, so that re-encoding a
     * decoded `BrushFamily` writes the texture IDs that it was originally serialized with.
     *
     * This is called whether or not [get] returns an image for [clientTextureId]. The result may be
     * cached by consumers, so this should return a deterministic result for a given input. If two
     * different client texture IDs used in the same `BrushFamily` return the same encoded ID, their
     * layers will share that encoded ID in the serialized form and only the first texture's image
     * will be stored. If `BrushFamily.hasFallbacks` is true for a `BrushFamily` that is encoded on
     * its own, its stored serialized form, which already has the original texture IDs, is written
     * instead, so this has no effect.
     *
     * @param clientTextureId The texture ID used by the `BrushFamily` being encoded.
     * @return The texture ID to write into the serialized `BrushFamily`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public fun getEncodedTextureId(clientTextureId: String): String = clientTextureId
}
