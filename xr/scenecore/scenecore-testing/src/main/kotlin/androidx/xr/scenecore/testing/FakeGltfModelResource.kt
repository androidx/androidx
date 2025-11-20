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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.GltfModelResource

/** Test-only implementation of [androidx.xr.scenecore.runtime.GltfModelResource] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeGltfModelResource(public val mToken: Long) : GltfModelResource {
    /**
     * The asset name that was used to "load" this fake resource.
     *
     * This property is intended for testing purposes. It is populated by the
     * [FakeRenderingRuntime.loadGltfByAssetNameAsync] method and can be inspected by tests to
     * verify that the correct asset path was used during the model loading process.
     */
    public var assetName: String = ""
        internal set

    /**
     * The asset data that was used to "load" this fake resource.
     *
     * This property is intended for testing purposes. It is populated by the
     * [FakeRenderingRuntime.loadGltfByByteArrayAsync] method and can be inspected by tests to
     * verify that the correct asset data was used during the model loading process.
     */
    public var assetData: ByteArray = ByteArray(0)
        internal set

    /**
     * The asset key that was used to "load" this fake resource.
     *
     * This property is intended for testing purposes. It is populated by the
     * [FakeRenderingRuntime.loadGltfByByteArrayAsync] method and can be inspected by tests to
     * verify that the correct asset key was used during the model loading process.
     */
    public var assetKey: String = ""
        internal set
}
