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

package androidx.xr.scenecore.spatial.core;

import androidx.xr.scenecore.runtime.PointSourceParams;
import androidx.xr.scenecore.runtime.SoundFieldAttributes;
import androidx.xr.scenecore.runtime.SpatializerConstants;

import com.android.extensions.xr.media.SpatializerExtensions;
import com.android.extensions.xr.node.Node;

/** Utils for the runtime media class conversions. */
class MediaUtils {
    private MediaUtils() {}

    static com.android.extensions.xr.media.PointSourceParams convertPointSourceParamsToExtensions(
            PointSourceParams params) {

        Node node = ((AndroidXrEntity) params.getEntity()).getNode();

        return new com.android.extensions.xr.media.PointSourceParams.Builder()
                .setNode(node)
                .build();
    }

    static com.android.extensions.xr.media.SoundFieldAttributes
            convertSoundFieldAttributesToExtensions(SoundFieldAttributes attributes) {

        return new com.android.extensions.xr.media.SoundFieldAttributes.Builder()
                .setAmbisonicsOrder(
                        convertAmbisonicsOrderToExtensions(attributes.getAmbisonicsOrder()))
                .build();
    }

    static int convertAmbisonicsOrderToExtensions(
            @SpatializerConstants.AmbisonicsOrder int ambisonicsOrder) {
        switch (ambisonicsOrder) {
            case SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER:
                return SpatializerExtensions.AMBISONICS_ORDER_FIRST_ORDER;
            case SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER:
                return SpatializerExtensions.AMBISONICS_ORDER_SECOND_ORDER;
            case SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER:
                return SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER;
            default:
                throw new IllegalArgumentException(
                        "Invalid Sound Field ambisonics order: " + ambisonicsOrder);
        }
    }

    @SpatializerConstants.SourceType
    static int convertExtensionsToSourceType(int extensionsSourceType) {
        switch (extensionsSourceType) {
            case SpatializerExtensions.SOURCE_TYPE_DEFAULT:
                return SpatializerConstants.SOURCE_TYPE_BYPASS;
            case SpatializerExtensions.SOURCE_TYPE_POINT_SOURCE:
                return SpatializerConstants.SOURCE_TYPE_POINT_SOURCE;
            case SpatializerExtensions.SOURCE_TYPE_SOUND_FIELD:
                return SpatializerConstants.SOURCE_TYPE_SOUND_FIELD;
            default:
                throw new IllegalArgumentException(
                        "Invalid Sound Spatializer source type: " + extensionsSourceType);
        }
    }
}
