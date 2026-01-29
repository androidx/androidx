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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.SpatialApiVersionProvider

/**
 * A fake implementation of [SpatialApiVersionProvider] for testing.
 *
 * This class is loaded by its class name and provides values that can be configured for testing via
 * the companion object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSpatialApiVersionProvider : SpatialApiVersionProvider {
    public companion object {
        /**
         * The value to be returned by [spatialApiVersion].
         *
         * If null, accessing [spatialApiVersion] will throw an [IllegalStateException].
         */
        public var testSpatialApiVersion: Int? = null

        /**
         * The value to be returned by [previewSpatialApiVersion].
         *
         * If null, accessing [previewSpatialApiVersion] will throw an [IllegalStateException].
         */
        public var testPreviewSpatialApiVersion: Int? = null
    }

    override val spatialApiVersion: Int
        get() =
            testSpatialApiVersion
                ?: throw IllegalStateException(
                    "spatialApiVersion is not set for testing. Provide a value via " +
                        "FakeSpatialApiVersionProvider.testSpatialApiVersion"
                )

    override val previewSpatialApiVersion: Int
        get() =
            testPreviewSpatialApiVersion
                ?: throw IllegalStateException(
                    "previewSpatialApiVersion is not set for testing. Provide a value via " +
                        "FakeSpatialApiVersionProvider.testPreviewSpatialApiVersion"
                )
}
