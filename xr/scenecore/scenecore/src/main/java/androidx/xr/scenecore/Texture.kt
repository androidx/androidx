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
         * Public factory function for a [Texture], where the texture is asynchronously loaded.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * Currently, only URLs and relative paths from the android_assets/ directory are supported.
         *
         * @param session The [Session] to use for loading the [Texture].
         * @param name The URL or asset-relative path of a [Texture] to be loaded
         * @param sampler A [TextureSampler] descriptor which describes how the texture will be
         *   filtered
         * @return a ListenableFuture<Texture>. Listeners will be called on the main thread if
         *   Runnable::run is supplied.
         */
        @MainThread
        @JvmStatic
        @Suppress("AsyncSuffixFuture")
        public fun create(
            session: Session,
            name: String,
            sampler: TextureSampler,
        ): ListenableFuture<Texture> {
            return createAsync(session.platformAdapter, name, sampler, session)
        }
    }
}
