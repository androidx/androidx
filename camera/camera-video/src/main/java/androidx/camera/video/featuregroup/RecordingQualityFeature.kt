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

package androidx.camera.video.featuregroup

import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.impl.feature.FeatureTypeInternal
import androidx.camera.video.GroupableFeatures
import androidx.camera.video.Quality

/**
 * Denotes the video recording quality in a feature group.
 *
 * This feature should not be instantiated directly, instead use the [GroupableFeatures] objects,
 * e.g. [GroupableFeatures.SD_RECORDING].
 */
internal class RecordingQualityFeature(val quality: Quality) : GroupableFeature() {
    override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.RECORDING_QUALITY

    override fun toString(): String {
        return "RecordingQualityFeature(quality=$quality)"
    }
}
