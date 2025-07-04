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

package androidx.camera.core.featuregroup.impl.feature

import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.impl.CameraInfoInternal

/**
 * Denotes the dynamic range applied to all [UseCase]s which are not of image capture type in a
 * feature combination, i.e. [Preview] and `VideoCapture` use cases currently.
 *
 * This feature can not be instantiated directly, instead use the [GroupableFeature.HDR_HLG10]
 * object.
 */
@OptIn(ExperimentalSessionConfig::class)
internal class DynamicRangeFeature(val dynamicRange: DynamicRange) : GroupableFeature() {
    override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.DYNAMIC_RANGE

    override fun isSupportedIndividually(
        cameraInfoInternal: CameraInfoInternal,
        sessionConfig: SessionConfig,
    ): Boolean {
        val cameraInfoSupportedDynamicRanges = cameraInfoInternal.supportedDynamicRanges
        Logger.d(
            TAG,
            "isSupportedIndividually:" +
                " cameraInfoSupportedDynamicRanges = $cameraInfoSupportedDynamicRanges" +
                ", this = $this",
        )

        if (!cameraInfoSupportedDynamicRanges.contains(dynamicRange)) {
            return false
        }

        for (useCase in sessionConfig.useCases) {
            val useCaseSupportedDynamicRanges =
                useCase.getSupportedDynamicRanges(cameraInfoInternal)

            Logger.d(
                TAG,
                "isSupportedIndividually:" +
                    " useCaseSupportedDynamicRanges = $useCaseSupportedDynamicRanges" +
                    ", this = $this, useCases = $useCase",
            )

            if (
                useCaseSupportedDynamicRanges != null &&
                    !useCaseSupportedDynamicRanges.contains(dynamicRange)
            ) {
                return false
            }
        }

        return true
    }

    override fun toString(): String {
        return "DynamicRangeFeature(dynamicRange=$dynamicRange)"
    }

    companion object {
        private const val TAG = "DynamicRangeFeature"

        @JvmField val DEFAULT_DYNAMIC_RANGE = DynamicRange.SDR
    }
}
