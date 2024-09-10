/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android

import android.graphics.Bitmap
import androidx.annotation.RestrictTo
import androidx.ink.brush.ExperimentalInkCustomBrushApi

/**
 * Interface for a callback to allow the caller to provide a particular [Bitmap] for a texture URI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@ExperimentalInkCustomBrushApi
public fun interface TextureBitmapStore {
    /**
     * Retrieve a [Bitmap] for the given texture URI. This may be called synchronously during
     * `onDraw`, so loading of texture files from disk and decoding them into [Bitmap] objects
     * should be done on init. The result may be cached by consumers, so this should return a
     * deterministic result for a given input.
     *
     * Textures can be disabled by having load always return null. null should also be returned when
     * a texture can not be loaded. If null is returned, the texture layer in question should be
     * ignored, allowing for graceful fallback. It's recommended that implementations log when a
     * texture can not be loaded.
     *
     * @return The texture bitmap, if any, associated with the given URI.
     */
    public fun get(textureImageUri: String): Bitmap?
}
