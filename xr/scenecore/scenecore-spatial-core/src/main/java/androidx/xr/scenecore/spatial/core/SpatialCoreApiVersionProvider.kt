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

package androidx.xr.scenecore.spatial.core

import android.extensions.xr.XrExtensions
import androidx.xr.runtime.SpatialApiVersionProvider

/**
 * Implementation of [SpatialApiVersionProvider] for the spatial core module.
 *
 * This class provides the version of XR APIs by checking for the presence of different versions of
 * the `XrExtensions` class at runtime.
 */
internal class SpatialCoreApiVersionProvider : SpatialApiVersionProvider {

    /**
     * The version of the stable Spatial APIs available on the device.
     *
     * If the `XrExtensions` class or its methods are not found, this will default to `1`. This
     * ensures backward compatibility, as version `1` represents the baseline stable API.
     */
    override val spatialApiVersion: Int
        get() =
            try {
                XrExtensions.getSpatialApiVersion()
            } catch (e: NoClassDefFoundError) {
                1
            } catch (e: NoSuchMethodError) {
                1
            }

    /**
     * The version of the preview Spatial APIs available on the device.
     *
     * If the `XrExtensions` class or its methods are not found, this will default to `0`. A value
     * of `0` indicates that no preview APIs are available on the device.
     */
    override val previewSpatialApiVersion: Int
        get() =
            try {
                XrExtensions.getPreviewApiVersion()
            } catch (e: NoClassDefFoundError) {
                0
            } catch (e: NoSuchMethodError) {
                0
            }
}
