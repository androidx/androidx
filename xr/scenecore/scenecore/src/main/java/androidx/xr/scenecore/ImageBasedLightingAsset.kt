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

package androidx.xr.scenecore

import android.annotation.SuppressLint
import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.scenecore.runtime.ExrImageResource as RtImageBasedLightingAsset
import androidx.xr.scenecore.runtime.RenderingRuntime
import java.nio.file.Path

/**
 * Represents an image based lighting asset which contains lighting information for the scene.
 *
 * EXR and HDR images are supported by the [SpatialEnvironment].
 */
// TODO(b/461909954): Add AutoCloseable interface when it is approved.
public class ImageBasedLightingAsset
internal constructor(
    internal val session: Session?,
    internal val image: RtImageBasedLightingAsset,
) {

    /**
     * Closes the given [ImageBasedLightingAsset].
     *
     * The [ImageBasedLightingAsset] can be explicitly closed at any time or garbage collected. When
     * either happens, its resources are freed. If close() is not explicitly invoked by the client,
     * the [ImageBasedLightingAsset] will be automatically closed when the [ImageBasedLightingAsset]
     * is garbage collected.
     *
     * @throws IllegalStateException if the resource has already been closed.
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun close() {
        session?.renderingRuntime?.destroyExrImage(image)
    }

    /**
     * Returns the reflection texture from a preprocessed image based lighting asset.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @return a CubeMapTexture.
     * @throws IllegalStateException if the reflection texture couldn't be retrieved or if the image
     *   based lighting asset was not preprocessed.
     */
    @MainThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getReflectionTexture(): CubeMapTexture {
        if (session == null) {
            throw IllegalStateException(
                "Can only retrieve a reflection texture from preprocessed image based lighting assets."
            )
        }
        val reflectionTexture = session.renderingRuntime.getReflectionTextureFromIbl(image)
        if (reflectionTexture == null) {
            throw IllegalStateException(
                "Failed to retrieve a reflection texture from preprocessed image based lighting assets."
            )
        }
        return CubeMapTexture(reflectionTexture, session)
    }

    public companion object {
        internal suspend fun createFromZip(
            session: Session,
            renderingRuntime: RenderingRuntime,
            name: String,
        ): ImageBasedLightingAsset {
            require(name.endsWith(".zip", ignoreCase = true)) {
                "Only preprocessed skybox files with the .zip extension are supported."
            }

            return createImageBasedLightingAsset(
                session,
                renderingRuntime.loadExrImageByAssetName(name),
            )
        }

        @SuppressWarnings("RestrictTo")
        internal suspend fun createFromZip(
            session: Session,
            renderingRuntime: RenderingRuntime,
            byteArray: ByteArray,
            assetKey: String,
        ): ImageBasedLightingAsset {
            return createImageBasedLightingAsset(
                session,
                renderingRuntime.loadExrImageByByteArray(byteArray, assetKey),
            )
        }

        /**
         * Public factory for an [ImageBasedLightingAsset], asynchronously loading a preprocessed
         * image based lighting asset from a [Path] relative to the application's `assets/` folder.
         *
         * The input `.zip` file should contain the preprocessed image-based lighting (IBL) data,
         * typically generated from an `.exr` or `.hdr` environment map using a tool like Filament's
         * `cmgen`. See: https://github.com/google/filament/tree/main/tools/cmgen
         *
         * @param session The [Session] to use for loading the asset.
         * @param path The Path of the preprocessed `.zip` image based lighting asset file to be
         *   loaded, relative to the application's `assets/` folder.
         * @return a [ImageBasedLightingAsset] upon completion.
         * @throws IllegalArgumentException if [Path.isAbsolute] is true, as this method requires a
         *   relative path, or if the path does not specify a `.zip` file.
         */
        @MainThread
        @JvmStatic
        // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
        @SuppressLint("NewApi")
        public suspend fun createFromZip(session: Session, path: Path): ImageBasedLightingAsset {
            require(!path.isAbsolute) {
                "ImageBasedLightingAsset.createFromZip() expects a path relative to `assets/`, received absolute path $path."
            }
            return createFromZip(session, session.renderingRuntime, path.toString())
        }

        /**
         * Public factory for an [ImageBasedLightingAsset], asynchronously loading a preprocessed
         * image based lighting asset from a [Uri].
         *
         * The input `.zip` file should contain the preprocessed image-based lighting (IBL) data,
         * typically generated from an `.exr` or `.hdr` environment map using a tool like Filament's
         * `cmgen`. See: https://github.com/google/filament/tree/main/tools/cmgen
         *
         * @param session The [Session] to use for loading the asset.
         * @param uri The Uri of the preprocessed `.zip` image based lighting asset file to be
         *   loaded.
         * @return a [ImageBasedLightingAsset] upon completion.
         * @throws IllegalArgumentException if the Uri does not specify a `.zip` file.
         */
        @MainThread
        @JvmStatic
        public suspend fun createFromZip(session: Session, uri: Uri): ImageBasedLightingAsset =
            createFromZip(session, session.renderingRuntime, uri.toString())

        /**
         * Public factory function for a preprocessed [ImageBasedLightingAsset], where the
         * preprocessed [ImageBasedLightingAsset] is asynchronously loaded.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session The [Session] to use for loading the asset.
         * @param assetData The byte array of the preprocessed image based lighting asset to be
         *   loaded.
         * @param assetKey The key of the preprocessed image based lighting asset to be loaded. This
         *   is used to identify the asset in the SceneCore cache.
         * @return a [ImageBasedLightingAsset] upon completion.
         */
        @MainThread
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public suspend fun createFromZip(
            session: Session,
            assetData: ByteArray,
            assetKey: String,
        ): ImageBasedLightingAsset {
            return createFromZip(session, session.renderingRuntime, assetData, assetKey)
        }

        private fun createImageBasedLightingAsset(
            session: Session,
            imageBasedLightingResource: RtImageBasedLightingAsset,
        ): ImageBasedLightingAsset = ImageBasedLightingAsset(session, imageBasedLightingResource)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageBasedLightingAsset

        // Perform a structural equality check on the underlying image.
        if (image != other.image) return false

        return true
    }

    override fun hashCode(): Int {
        return image.hashCode()
    }
}
