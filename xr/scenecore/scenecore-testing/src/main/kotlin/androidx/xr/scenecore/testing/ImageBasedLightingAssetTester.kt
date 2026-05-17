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

package androidx.xr.scenecore.testing

import android.net.Uri
import androidx.xr.scenecore.ImageBasedLightingAsset
import androidx.xr.scenecore.testing.internal.FakeExrImageResource as InternalImageBasedLightingAsset
import java.nio.file.Path

/** A data container for inspecting the properties of an [ImageBasedLightingAsset] resource. */
public class ImageBasedLightingAssetTester
internal constructor(
    private val rtAsset: InternalImageBasedLightingAsset,
    internal val asset: ImageBasedLightingAsset,
) {

    internal companion object {
        /**
         * Retrieves a test data accessor for the given [ImageBasedLightingAsset].
         *
         * This function provides a [ImageBasedLightingAssetTester] instance, which can be used to
         * inspect and manipulate its underlying data in the test environment.
         *
         * @param asset The resource for which to retrieve the test data accessor.
         * @return A [ImageBasedLightingAssetTester] instance for the given resource.
         */
        internal fun create(asset: ImageBasedLightingAsset): ImageBasedLightingAssetTester {
            return ImageBasedLightingAssetTester(
                @Suppress("DEPRECATION") (asset.image as FakeExrImageResource).fakeInternal,
                asset,
            )
        }
    }

    /**
     * A string representation of the source location for the preprocessed `.zip`
     * [ImageBasedLightingAsset]. The exact contents and format of this string depend on which
     * factory method was used to create this instance.
     * - If created using [ImageBasedLightingAsset.createFromZip] (with [Path]): This value is the
     *   result of `path.toString()`. The input [Path] must be relative to the application's
     *   `assets/` folder, so this string will represent a relative path, typically using `/` as a
     *   separator. Examples:
     *     - `Paths.get("test.zip")` results in `assetPath` being `"test.zip"`.
     *     - `Paths.get("data", "test.zip")` results in `assetPath` being `"data/test.zip"`.
     * - If created using [ImageBasedLightingAsset.createFromZip] (with [Uri]): This value is the
     *   result of `uri.toString()`. This can be any valid [android.net.Uri] string representation
     *   supported for loading assets, including relative paths or URIs with schemes. Examples:
     *     - `"data/test.zip".toUri()` (using AndroidX extension) results in `assetPath` being
     *       `"data/test.zip"`.
     */
    public val assetPath: String
        get() = rtAsset.assetName

    /** The raw byte data of the asset. */
    public val assetData: ByteArray
        get() = rtAsset.assetData

    /** A unique key identifying the asset in the fake rendering runtime. */
    public val assetKey: String
        get() = rtAsset.assetKey

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageBasedLightingAssetTester

        if (rtAsset != other.rtAsset) return false
        if (asset != other.asset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rtAsset.hashCode()
        result = 31 * result + asset.hashCode()
        return result
    }
}
