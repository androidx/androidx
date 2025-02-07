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
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource as RtGltfModel
import com.google.common.util.concurrent.ListenableFuture

/** Represents a 3D model in SceneCore. */
public interface Model

/**
 * [GltfModel] represents a glTF resource in SceneCore. These can be used as part of the
 * [Environment] or to display 3D models with [GltfModelEntity]. These are created by the [Session].
 */
// TODO: b/319269278 - Make this and ExrImage derive from a common Resource base class which has
//                     async helpers.
// TODO: b/362368652 - Add an interface which returns an integer animation IDX given a string
//                     animation name for a loaded glTF, as well as an interface for selecting the
//                     playback animation from the integer index.
// TODO: b/362368652 - Add an interface which returns a list of available animation names
public class GltfModel internal constructor(public val model: RtGltfModel) : Model {

    public companion object {
        @Deprecated(
            message = "This function is deprecated, use createAsync() instead",
            replaceWith = ReplaceWith("createAsync()"),
        )
        internal fun create(platformAdapter: JxrPlatformAdapter, name: String): GltfModel {
            val gltfResourceFuture = platformAdapter.loadGltfByAssetName(name)
            // TODO: b/320858652 - Implement async loading of GltfModel.
            return GltfModel(gltfResourceFuture!!.get())
        }

        // ResolvableFuture is marked as RestrictTo(LIBRARY_GROUP_PREFIX), which is intended for
        // classes
        // within AndroidX. We're in the process of migrating to AndroidX. Without suppressing this
        // warning, however, we get a build error - go/bugpattern/RestrictTo.
        @SuppressWarnings("RestrictTo")
        internal fun createAsync(
            platformAdapter: JxrPlatformAdapter,
            name: String,
        ): ListenableFuture<GltfModel> {
            val gltfResourceFuture = platformAdapter.loadGltfByAssetNameSplitEngine(name)
            val modelFuture = ResolvableFuture.create<GltfModel>()

            // TODO: b/375070346 - remove this `!!` when we're sure the future is non-null.
            gltfResourceFuture!!.addListener(
                {
                    try {
                        val gltfResource = gltfResourceFuture.get()
                        modelFuture.set(GltfModel(gltfResource))
                    } catch (e: Exception) {
                        if (e is InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                        modelFuture.setException(e)
                    }
                },
                Runnable::run,
            )
            return modelFuture
        }

        /**
         * Public factory function for a [GltfModel], where the glTF is asynchronously loaded.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * Currently, only URLs and relative paths from the android_assets/ directory are supported.
         * Currently, only binary glTF (.glb) files are supported.
         *
         * @param session The [Session] to use for loading the model.
         * @param name The URL or asset-relative path of a binary glTF (.glb) model to be loaded
         * @return a ListenableFuture<GltfModel>. Listeners will be called on the main thread if
         *   Runnable::run is supplied.
         */
        @MainThread
        @JvmStatic
        @Suppress("AsyncSuffixFuture")
        public fun create(session: Session, name: String): ListenableFuture<GltfModel> {
            return GltfModel.createAsync(session.platformAdapter, name)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GltfModel

        // Perform a structural equality check on the underlying model.
        if (model != other.model) return false

        return true
    }

    override fun hashCode(): Int {
        return model.hashCode()
    }
}
