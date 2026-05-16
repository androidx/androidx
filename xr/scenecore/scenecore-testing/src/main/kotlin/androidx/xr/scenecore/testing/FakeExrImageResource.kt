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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.ExrImageResource
import androidx.xr.scenecore.testing.internal.FakeExrImageResource as InternalFakeExrImageResource

/** Test-only implementation of [androidx.xr.scenecore.runtime.ExrImageResource] */
@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeExrImageResource
internal constructor(
    public val mToken: Long,
    internal val fakeInternal: InternalFakeExrImageResource,
) : ExrImageResource {

    public constructor(mToken: Long) : this(mToken, InternalFakeExrImageResource())

    /**
     * The asset name that was used to "load" this fake resource.
     *
     * This property is intended for testing purposes. It is populated by the
     * [FakeRenderingRuntime.loadExrImageByAssetName] method and can be inspected by tests to verify
     * that the correct asset path was used during the model loading process.
     */
    public var assetName: String
        get() = fakeInternal.assetName
        internal set(value) {
            fakeInternal.assetName = value
        }

    /** An asset in the form of a byte array. */
    public var assetData: ByteArray
        get() = fakeInternal.assetData
        set(value) {
            fakeInternal.assetData = value
        }

    /** The name of the asset to load from the cache. */
    public var assetKey: String
        get() = fakeInternal.assetKey
        set(value) {
            fakeInternal.assetKey = value
        }
}
