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

package androidx.camera.core.featurecombination.impl.feature

import android.util.Range
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.featurecombination.Feature

/**
 * Denotes the FPS range that is applied to the camera.
 *
 * This feature can not be instantiated directly for usage, instead use the [Feature.FPS_60] object.
 */
@OptIn(ExperimentalSessionConfig::class)
public class FpsRangeFeature(public val minFps: Int, public val maxFps: Int) : Feature() {
    override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.FPS_RANGE

    override fun toString(): String {
        return "FpsRangeFeature(minFps=$minFps, maxFps=$maxFps)"
    }

    public companion object {
        @JvmField public val DEFAULT_FPS_RANGE: Range<Int> = Range(30, 30)

        // TODO: b/402372530  - Add a function to create a FpsRangeFeature instance where min and
        //  max FPS values are different.
    }
}
