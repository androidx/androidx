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

package androidx.xr.runtime.testing.internal

import androidx.xr.runtime.SpatialApiVersionProvider
import androidx.xr.runtime.SpatialApiVersions
import androidx.xr.runtime.testing.XrDeviceTestRule

/** Internal fake implementation of [SpatialApiVersionProvider]. */
internal class FakeSpatialApiVersionProvider : SpatialApiVersionProvider {

    init {
        instance = this
    }

    companion object {
        internal var instance: FakeSpatialApiVersionProvider? = null
        internal var xrDeviceTestRule: XrDeviceTestRule? = null
    }

    internal fun registerProvider() {
        xrDeviceTestRule?.spatialApiVersionProvider = this
    }

    override var spatialApiVersion: Int = SpatialApiVersions.LATEST_STABLE_API_LEVEL

    override var previewSpatialApiVersion: Int = 0
}
