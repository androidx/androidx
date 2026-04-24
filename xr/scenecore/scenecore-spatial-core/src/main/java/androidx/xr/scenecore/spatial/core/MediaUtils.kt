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

import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.SpatializerConstants
import com.android.extensions.xr.media.PointSourceParams
import com.android.extensions.xr.media.SoundFieldAttributes
import com.android.extensions.xr.media.SpatializerExtensions

/** Utils for the runtime media class conversions. */
internal object MediaUtils {
    @JvmStatic
    fun convertPointSourceParamsToExtensions(
        params: androidx.xr.scenecore.runtime.PointSourceParams?,
        entity: Entity?,
    ): PointSourceParams {
        val builder = PointSourceParams.Builder()
        if (entity != null) {
            builder.setNode((entity as AndroidXrEntity).getNode())
        }
        return builder.build()
    }

    @JvmStatic
    fun convertSoundFieldAttributesToExtensions(
        attributes: androidx.xr.scenecore.runtime.SoundFieldAttributes
    ): SoundFieldAttributes {
        return SoundFieldAttributes.Builder()
            .setAmbisonicsOrder(convertAmbisonicsOrderToExtensions(attributes.ambisonicsOrder))
            .build()
    }

    @JvmStatic
    fun convertAmbisonicsOrderToExtensions(
        @SpatializerConstants.AmbisonicsOrder ambisonicsOrder: Int
    ): Int {
        return when (ambisonicsOrder) {
            SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER ->
                SpatializerExtensions.AMBISONICS_ORDER_FIRST_ORDER
            SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER ->
                SpatializerExtensions.AMBISONICS_ORDER_SECOND_ORDER
            SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER ->
                SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER
            else ->
                throw IllegalArgumentException(
                    "Invalid Sound Field ambisonics order: $ambisonicsOrder"
                )
        }
    }

    @JvmStatic
    @SpatializerConstants.SourceType
    fun convertExtensionsToSourceType(extensionsSourceType: Int): Int {
        return when (extensionsSourceType) {
            SpatializerExtensions.SOURCE_TYPE_DEFAULT -> SpatializerConstants.SOURCE_TYPE_BYPASS
            SpatializerExtensions.SOURCE_TYPE_POINT_SOURCE ->
                SpatializerConstants.SOURCE_TYPE_POINT_SOURCE
            SpatializerExtensions.SOURCE_TYPE_SOUND_FIELD ->
                SpatializerConstants.SOURCE_TYPE_SOUND_FIELD
            else ->
                throw IllegalArgumentException(
                    "Invalid Sound Spatializer source type: $extensionsSourceType"
                )
        }
    }
}
