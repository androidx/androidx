/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore

import androidx.annotation.MainThread
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.TextureResource as RtTextureResource
import java.io.File
import java.nio.file.Path

/**
 * Represents a [Texture] in SceneCore.
 *
 * A texture is an image that can be applied to a 3D model to give it color, detail, and realism. It
 * can also be used as an alpha mask for a [SurfaceEntity].
 *
 * It's important to close a [Texture] when it's no longer needed to free up resources. This can be
 * done by calling the [close] method or letting it get garbage collected.
 */
public open class Texture
internal constructor(internal val texture: RtTextureResource, internal val session: Session) :
    AutoCloseable {

    /**
     * Closes the given [Texture].
     *
     * The [Texture] can be explicitly closed at anytime or garbage collected. In both cases, its
     * resources are freed and an exception will be thrown if the [Texture] is used after being
     * closed.
     *
     * @throws IllegalStateException if the resource has already been closed.
     */
    @MainThread
    override public open fun close() {
        session.runtimes.filterIsInstance<RenderingRuntime>().single().destroyTexture(texture)
    }

    public companion object {
        internal suspend fun createAsync(
            renderingRuntime: RenderingRuntime,
            name: String,
            session: Session,
        ): Texture {
            val textureResource = renderingRuntime.loadTexture(name)
            return Texture(textureResource, session)
        }

        /**
         * Public factory for a [Texture], asynchronously loading a preprocessed texture from a
         * [Path] relative to the application's `assets/` folder.
         *
         * Currently, only URLs and relative paths from the `assets/` directory are supported.
         *
         * @param session The [Session] to use for loading the [Texture].
         * @param path The [Path] of the `.png` texture file to be loaded, relative to the
         *   application's `assets/` folder.
         * @return a [Texture] upon completion.
         * @throws IllegalArgumentException if [Path.isAbsolute] is true, as this method requires a
         *   relative path.
         */
        @MainThread
        @JvmStatic
        public suspend fun create(session: Session, path: Path): Texture {
            require(!File(path.toString()).isAbsolute) {
                "Texture.create() expects a path relative to `assets/`, received absolute path $path."
            }
            return createAsync(session.renderingRuntime, path.toString(), session)
        }
    }
}
