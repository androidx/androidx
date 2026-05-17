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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.testing.internal.FakeTexture as InternalFakeTexture
import java.nio.file.Path
import java.nio.file.Paths

/** A data container for inspecting the properties of a [Texture] resource. */
public class TextureTester
internal constructor(
    private val rtTextureResource: InternalFakeTexture,
    internal val texture: Texture,
) {

    internal companion object {
        /**
         * Creates a test data accessor for the given [Texture].
         *
         * This function provides a [TextureTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param texture The texture for which to retrieve the test data accessor.
         * @return A [TextureTester] instance for the given texture.
         */
        internal fun create(texture: Texture): TextureTester {
            return TextureTester((texture.texture as FakeTexture).fakeInternal, texture)
        }
    }

    /**
     * The path used to load this [Texture], relative to the application's `assets/` folder.
     *
     * This matches the [path] parameter provided in [Texture.create].
     *
     * This value is guaranteed to be a relative path string pointing to a texture asset within the
     * `assets/` directory (e.g., "models/texture.png") or a valid URL.
     */
    public val path: Path
        @RequiresApi(Build.VERSION_CODES.O) get() = Paths.get(rtTextureResource.assetName)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextureTester

        if (rtTextureResource != other.rtTextureResource) return false
        if (texture != other.texture) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtTextureResource.hashCode()
        result = 31 * result + texture.hashCode()
        return result
    }
}
