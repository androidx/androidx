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

@file:OptIn(ExperimentalSessionConfig::class)

package androidx.camera.core.samples

import androidx.annotation.Sampled
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.GroupableFeature.Companion.FPS_60
import androidx.camera.core.featuregroup.GroupableFeature.Companion.HDR_HLG10
import androidx.camera.core.featuregroup.GroupableFeature.Companion.PREVIEW_STABILIZATION
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner

@Sampled
fun configureSessionConfigWithFeatureGroups() {
    // Create a session config where HDR is mandatory while 60 FPS (higher priority) and preview
    // stabilization are optional
    SessionConfig(
        useCases = listOf(Preview.Builder().build()),
        requiredFeatureGroup = setOf(HDR_HLG10),
        preferredFeatureGroup = listOf(FPS_60, PREVIEW_STABILIZATION),
    )

    // Creating the following SessionConfig will throw an exception as there are conflicting
    // information. HDR is mentioned once as required and then again as preferred, it can't be both!
    SessionConfig(
        useCases = listOf(Preview.Builder().build()),
        requiredFeatureGroup = setOf(HDR_HLG10),
        preferredFeatureGroup = listOf(FPS_60, HDR_HLG10),
    )

    // Creating the following SessionConfig will throw an exception as a groupable feature (HDR) is
    // configured with non-grouping API while using grouping API as well. HDR is configured to the
    // with a non-grouping API through the Preview use case, so it can't be grouped together
    // properly with the other groupable features. Instead, it should be configured like the first
    // SessionConfig construction in this code snippet.
    SessionConfig(
        useCases =
            listOf(Preview.Builder().apply { setDynamicRange(DynamicRange.HLG_10_BIT) }.build()),
        preferredFeatureGroup = listOf(FPS_60, PREVIEW_STABILIZATION),
    )
}

@Sampled
fun startCameraWithSomeHighQualityFeatures(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    useCases: List<UseCase>,
) {
    // Bind a session config with some high quality features configured as per app requirements.

    cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        SessionConfig(
                useCases = useCases,
                requiredFeatureGroup = setOf(HDR_HLG10),
                preferredFeatureGroup = listOf(FPS_60, PREVIEW_STABILIZATION),
                // The preferred features will be set according to device capabilities and priority,
                // FPS_60 will be prioritized higher than PREVIEW_STABILIZATION here as FPS_60 is
                // placed earlier in the list.
            )
            .apply {
                setFeatureSelectionListener { features ->
                    // Update app UI based on the features selected if required, e.g. a menu with
                    // the options of HDR and SDR can be updated to show that HDR is the current
                    // selected option
                    updateFeatureMenuUi(selectedFeatures = features)
                }
            },
    )
}

private fun updateFeatureMenuUi(selectedFeatures: Set<GroupableFeature>) {}
