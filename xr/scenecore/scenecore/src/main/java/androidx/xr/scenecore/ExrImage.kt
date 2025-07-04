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
import androidx.xr.runtime.internal.ExrImageResource as RtExrImage
import androidx.xr.runtime.internal.JxrPlatformAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Path

/**
 * Represents an [EXR image](https://openexr.com/) in SceneCore.
 *
 * EXR images are used by the [SpatialEnvironment] for drawing skyboxes.
 */
// TODO(b/319269278): Make this and GltfModel derive from a common Resource base class which has
//                    async helpers.
public class ExrImage
internal constructor(internal val image: RtExrImage, internal val session: Session? = null) {

    /**
     * Returns the reflection texture from a preprocessed EXR image.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @return a CubeMapTexture.
     * @throws IllegalStateException if the reflection texture couldn't be retrieved or if the EXR
     *   image was not preprocessed.
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun getReflectionTexture(): CubeMapTexture {
        if (session == null) {
            throw IllegalStateException(
                "Can only retrieve reflection texture from preprocessed EXR images."
            )
        }
        val reflectionTexture = session.platformAdapter.getReflectionTextureFromIbl(image)
        if (reflectionTexture == null) {
            throw IllegalStateException(
                "Failed to retrieve reflection texture from the preprocessed EXR image."
            )
        }
        return CubeMapTexture(reflectionTexture, session)
    }

    public companion object {
        internal suspend fun createFromZip(
            platformAdapter: JxrPlatformAdapter,
            name: String,
            session: Session,
        ): ExrImage {
            require(name.endsWith(".zip", ignoreCase = true)) {
                "Only preprocessed skybox files with the .zip extension are supported."
            }

            return createExrImage(platformAdapter.loadExrImageByAssetName(name), session)
        }

        @SuppressWarnings("RestrictTo")
        internal suspend fun createFromZip(
            platformAdapter: JxrPlatformAdapter,
            byteArray: ByteArray,
            assetKey: String,
            session: Session,
        ): ExrImage {
            return createExrImage(
                platformAdapter.loadExrImageByByteArray(byteArray, assetKey),
                session,
            )
        }

        /**
         * Public factory for an ExrImage, asynchronously loading a preprocessed skybox from a
         * [Path] relative to the application's `assets/` folder.
         *
         * The input `.zip` file should contain the preprocessed image-based lighting (IBL) data,
         * typically generated from an `.exr` or `.hdr` environment map using a tool like Filament's
         * `cmgen`. See: https://github.com/google/filament/tree/main/tools/cmgen
         *
         * @param session The [Session] to use for loading the asset.
         * @param path The Path of the preprocessed `.zip` skybox file to be loaded, relative to the
         *   application's `assets/` folder.
         * @return a [ExrImage] upon completion.
         *     @throws IllegalArgumentException if [Path.isAbsolute] is true, as this method
         *       requires a relative path, or if the path does not specify a `.zip` file.
         */
        @MainThread
        @JvmStatic
        // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
        @SuppressLint("NewApi")
        public suspend fun createFromZip(session: Session, path: Path): ExrImage {
            require(!path.isAbsolute) {
                "ExrImage.createFromZip() expects a path relative to `assets/`, received absolute path $path."
            }
            return createFromZip(session.platformAdapter, path.toString(), session)
        }

        /**
         * Public factory for an ExrImage, asynchronously loading a preprocessed skybox from a
         * [Uri].
         *
         * The input `.zip` file should contain the preprocessed image-based lighting (IBL) data,
         * typically generated from an `.exr` or `.hdr` environment map using a tool like Filament's
         * `cmgen`. See: https://github.com/google/filament/tree/main/tools/cmgen
         *
         * @param session The [Session] to use for loading the asset.
         * @param uri The Uri of the preprocessed `.zip` skybox file to be loaded.
         * @return a [ExrImage] upon completion.
         *     @throws IllegalArgumentException if the Uri does not specify a `.zip` file.
         */
        @MainThread
        @JvmStatic
        public suspend fun createFromZip(session: Session, uri: Uri): ExrImage =
            createFromZip(session.platformAdapter, uri.toString(), session)

        /**
         * Public factory function for a preprocessed EXRImage, where the preprocessed EXRImage is
         * asynchronously loaded.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session The [Session] to use for loading the asset.
         * @param assetData The byte array of the preprocessed EXR image to be loaded.
         * @param assetKey The key of the preprocessed EXR image to be loaded. This is used to
         *   identify the asset in the SceneCore cache.
         * @return a [ExrImage] upon completion.
         */
        @MainThread
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public suspend fun createFromZip(
            session: Session,
            assetData: ByteArray,
            assetKey: String,
        ): ExrImage {
            return createFromZip(session.platformAdapter, assetData, assetKey, session)
        }

        private suspend fun createExrImage(
            exrImageResourceFuture: ListenableFuture<RtExrImage>,
            session: Session,
        ): ExrImage {
            val image = exrImageResourceFuture.awaitSuspending()
            return ExrImage(image, session)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExrImage

        // Perform a structural equality check on the underlying image.
        if (image != other.image) return false

        return true
    }

    override fun hashCode(): Int {
        return image.hashCode()
    }
}
