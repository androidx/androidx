/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.TextureResource as RtTextureResource
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.nio.file.Path

/** [Texture] represents a texture that can be used with materials. */
@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class Texture
internal constructor(
    internal val texture: RtTextureResource,
    internal val sampler: TextureSampler = TextureSampler.create(),
    internal val session: Session,
) {

    /**
     * Disposes the given [Texture].
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * Currently, a glTF model (which this texture will be used with) can't be disposed. This means
     * that calling dispose on the texture will lead to a crash if the call is made out of order,
     * that is, if the texture is disposed before the glTF model that uses it.
     */
    // TODO(b/376277201): Provide Session.GltfModel.dispose().
    @MainThread
    public open fun dispose() {
        session.platformAdapter.destroyTexture(texture)
    }

    public companion object {
        @SuppressWarnings("RestrictTo")
        internal fun createAsync(
            platformAdapter: JxrPlatformAdapter,
            name: String,
            sampler: TextureSampler,
            session: Session,
        ): ListenableFuture<Texture> {
            val textureResourceFuture =
                platformAdapter.loadTexture(name, sampler.toRtTextureSampler())
            val textureFuture = ResolvableFuture.create<Texture>()
            textureResourceFuture!!.addListener(
                {
                    try {
                        val texture = textureResourceFuture.get()
                        textureFuture.set(Texture(texture, sampler, session))
                    } catch (e: Exception) {
                        if (e is InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                        textureFuture.setException(e)
                    }
                },
                Runnable::run,
            )
            return textureFuture
        }

        /**
         * Public factory for a Texture, asynchronously loading a preprocessed texture from a [Path]
         * relative to the application's `assets/` folder.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * Currently, only URLs and relative paths from the android_assets/ directory are supported.
         *
         * @param session The [Session] to use for loading the [Texture].
         * @param path The Path of the `.png` texture file to be loaded, relative to the
         *   application's `assets/` folder.
         * @param sampler A [TextureSampler] descriptor which describes how the texture will be
         *   filtered
         * @return a [Texture] upon completion.
         * @throws IllegalArgumentException if [Path.isAbsolute] is true, as this method requires a
         *   relative path, or if the path does not specify a `.zip` file.
         */
        @MainThread
        @JvmStatic
        @Suppress("AsyncSuffixFuture")
        public suspend fun create(session: Session, path: Path, sampler: TextureSampler): Texture {
            require(!File(path.toString()).isAbsolute) {
                "Texture.create() expects a path relative to `assets/`, received absolute path $path."
            }
            return createAsync(session.platformAdapter, path.toString(), sampler, session)
                .awaitSuspending()
        }
    }
}
