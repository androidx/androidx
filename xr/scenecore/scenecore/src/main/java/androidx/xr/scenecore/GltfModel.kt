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

import android.annotation.SuppressLint
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.GltfModelResource as RtGltfModel
import androidx.xr.runtime.internal.JxrPlatformAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Path

/**
 * GltfModel represents a [glTF](https://www.khronos.org/Gltf) resource in SceneCore. These can be
 * used as part of the [SpatialEnvironment] or to display 3D models with [GltfModelEntity].
 */
// TODO: b/319269278 - Make this and ExrImage derive from a common Resource base class which has
//                     async helpers.
// TODO: b/417750821 - Add an interface which returns an integer animation IDX given a string
//                     animation name for a loaded glTF.
//                   - an interface for selecting the
//                     playback animation from the integer index.
//                   - an interface which returns a list of available animation names

public class GltfModel internal constructor(internal val model: RtGltfModel) {

    public companion object {

        private suspend fun create(platformAdapter: JxrPlatformAdapter, name: String): GltfModel {
            return createModel(platformAdapter.loadGltfByAssetName(name))
        }

        private suspend fun create(
            platformAdapter: JxrPlatformAdapter,
            assetData: ByteArray,
            assetKey: String,
        ): GltfModel {
            return createModel(platformAdapter.loadGltfByByteArray(assetData, assetKey))
        }

        /**
         * Public factory for a GltfModel, where the glTF is asynchronously loaded from a [Path]
         * relative to the application's `assets/` folder.
         *
         * Currently, only binary glTF (.glb) files are supported.
         *
         * @param session The [Session] to use for loading the model.
         * @param path The Path of the binary glTF (.glb) model to be loaded, relative to the
         *   application's `assets/` folder.
         * @return a [GltfModel] upon completion.
         * @throws IllegalArgumentException if [path.isAbsolute] is true, as this method requires a
         *   relative path.
         */
        @MainThread
        @JvmStatic
        @SuppressLint("NewApi")
        public suspend fun create(session: Session, path: Path): GltfModel {
            require(!path.isAbsolute) {
                "GltfModel.create() expects a path relative to `assets/`, received absolute path $path."
            }
            return create(session.platformAdapter, path.toString())
        }

        /**
         * Public factory for a GltfModel, where the glTF is asynchronously loaded from a [Uri].
         *
         * Currently, only binary glTF (.glb) files are supported.
         *
         * @param session The [Session] to use for loading the model.
         * @param uri The Uri for a binary glTF (.glb) model to be loaded.
         * @return a [GltfModel] upon completion.
         */
        @MainThread
        @JvmStatic
        public suspend fun create(session: Session, uri: Uri): GltfModel =
            create(session.platformAdapter, uri.toString())

        /**
         * Public factory for a GltfModel, where the glTF is asynchronously loaded.
         *
         * Currently, only binary glTF files are supported.
         *
         * @param session The [Session] to use for loading the model.
         * @param assetData The byte array data of a binary glTF (`.glb`) model to be loaded.
         * @param assetKey The key to use for the model. This is used to identify the model in the
         *   SceneCore cache.
         * @return a [GltfModel] upon completion.
         */
        @MainThread
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public suspend fun create(
            session: Session,
            assetData: ByteArray,
            assetKey: String,
        ): GltfModel {
            return GltfModel.create(session.platformAdapter, assetData, assetKey)
        }

        private suspend fun createModel(
            gltfResourceFuture: ListenableFuture<RtGltfModel>
        ): GltfModel {
            return GltfModel(gltfResourceFuture.awaitSuspending())
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
