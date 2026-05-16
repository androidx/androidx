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

package androidx.xr.scenecore.testing.internal

import androidx.xr.scenecore.runtime.ExrImageResource

/** Test-only implementation of [androidx.xr.scenecore.runtime.ExrImageResource] */
internal class FakeExrImageResource : ExrImageResource {
    /** The name of the asset to load from the assets' folder. */
    var assetName: String = ""

    /** An asset in the form of a byte array. */
    var assetData: ByteArray = byteArrayOf(0)

    /** The name of the asset to load from the cache. */
    var assetKey: String = ""
}
