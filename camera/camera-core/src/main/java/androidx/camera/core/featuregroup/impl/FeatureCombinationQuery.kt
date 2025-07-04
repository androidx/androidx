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

package androidx.camera.core.featuregroup.impl

import android.util.Size
import android.view.Surface
import androidx.camera.core.DynamicRange
import androidx.camera.core.featuregroup.impl.UseCaseType.Companion.getFeatureGroupUseCaseType
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Queries whether a combination of features is supported by utilizing the
 * [android.hardware.camera2.CameraDevice.CameraDeviceSetup] API.
 */
public interface FeatureCombinationQuery {
    /**
     * Queries whether a combination of features is supported.
     *
     * @param sessionConfig The [SessionConfig] containing all the configuration parameters.
     * @return `true` if the feature combination is supported, `false` otherwise.
     */
    public fun isSupported(sessionConfig: SessionConfig): Boolean

    public companion object {
        @JvmField
        public val NO_OP_FEATURE_COMBINATION_QUERY: FeatureCombinationQuery =
            object : FeatureCombinationQuery {
                override fun isSupported(sessionConfig: SessionConfig): Boolean {
                    return false
                }
            }

        /**
         * Creates a [SessionConfig.Builder] from a [UseCaseConfig] to query feature combinations.
         *
         * This method creates a [SessionConfig.Builder] that contains a placeholder
         * [DeferrableSurface] with the given resolution and dynamic range. This builder can then be
         * used to create a [SessionConfig] for querying the camera capabilities without needing to
         * provide actual output surfaces.
         *
         * It is the responsibility of the caller to close the placeholder [DeferrableSurface]s once
         * the [SessionConfig] is no longer required.
         *
         * @param resolution The resolution of the surface.
         * @param dynamicRange The dynamic range of the surface.
         * @return A [SessionConfig.Builder] configured for feature combination queries.
         */
        @JvmStatic
        public fun UseCaseConfig<*>.createSessionConfigBuilder(
            resolution: Size,
            dynamicRange: DynamicRange,
        ): SessionConfig.Builder {
            val deferrableSurface: DeferrableSurface =
                object : DeferrableSurface(resolution, inputFormat) {
                    override fun provideSurface(): ListenableFuture<Surface?> {
                        return Futures.immediateFuture<Surface?>(null)
                    }
                }

            getFeatureGroupUseCaseType().surfaceClass?.let {
                deferrableSurface.setContainerClass(it)
            }

            return SessionConfig.Builder.createFrom(this, resolution)
                .addSurface(deferrableSurface, dynamicRange)
        }
    }
}
