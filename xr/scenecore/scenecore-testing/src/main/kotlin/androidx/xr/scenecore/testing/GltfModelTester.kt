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

import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.testing.internal.FakeGltfModelResource as InternalFakeGltfModelResource

/** A data container for inspecting the properties of a [GltfModel] resource. */
public class GltfModelTester
internal constructor(
    private val model: InternalFakeGltfModelResource,
    internal val gltfModel: GltfModel,
) {
    internal companion object {
        /**
         * Retrieves a test data accessor for the given [GltfModel].
         *
         * This function provides a [GltfModelTester] instance, which can be used to inspect and
         * manipulate its underlying data in the test environment.
         *
         * @param gltfModel The resource for which to retrieve the test data accessor.
         * @return A [GltfModelTester] instance for the given resource.
         */
        internal fun create(gltfModel: GltfModel): GltfModelTester {
            return GltfModelTester(
                @Suppress("DEPRECATION") (gltfModel.model as FakeGltfModelResource).fakeInternal,
                gltfModel,
            )
        }
    }

    /**
     * The path or URI string of the glTF asset file used to create the [GltfModel].
     *
     * This value represents:
     * - The `path` parameter from [GltfModel.create] when created via a [java.nio.file.Path]
     *   (relative to the application's `assets/` folder).
     * - The `uri` parameter from [GltfModel.create] when created via an [android.net.Uri].
     *
     * The default value is `""` (an empty string). This typically indicates that the model was not
     * loaded from a file path or URI (for example, if it was created using a byte array via
     * `GltfModel.create(session, assetData, assetKey)`).
     */
    public val assetPath: String
        get() = model.assetName

    /** The raw byte data of the glTF asset. */
    public val assetData: ByteArray
        get() = model.assetData

    /** A unique key identifying the asset in the fake rendering runtime. */
    public val assetKey: String
        get() = model.assetKey

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GltfModelTester

        if (model != other.model) return false
        if (gltfModel != other.gltfModel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = model.hashCode()
        result = 31 * result + gltfModel.hashCode()
        return result
    }
}
