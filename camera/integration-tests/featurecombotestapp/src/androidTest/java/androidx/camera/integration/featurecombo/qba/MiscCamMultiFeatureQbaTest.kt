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

package androidx.camera.integration.featurecombo.qba

import android.util.Range
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.integration.featurecombo.AppUseCase
import androidx.camera.testing.impl.CameraUtil
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests [FeatureGroupQueryBindAlignmentTestBase] for cameras other than front or back, with
 * multiple features.
 */
@LargeTest
@RunWith(Parameterized::class)
class MiscCamMultiFeatureQbaTest(
    testName: String,
    cameraSelector: CameraSelector,
    implName: String,
    cameraXConfig: CameraXConfig,
    featureGroup: Set<GroupableFeature>,
    useCasesToTest: List<AppUseCase>,
) :
    FeatureGroupQueryBindAlignmentTestBase(
        testName,
        cameraSelector,
        implName,
        cameraXConfig,
        featureGroup,
        useCasesToTest,
    ) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors()
                    .filter {
                        it.lensFacing != CameraSelector.LENS_FACING_BACK &&
                            it.lensFacing != CameraSelector.LENS_FACING_FRONT
                    }
                    .forEach { selector ->
                        val lens = selector.lensFacing

                        for (featureGroup in
                        // Generates all non-empty subsets of the features to test all combinations
                        allHighQualityFeatures.toPowerSet().filter {
                                // Do not test more than 3 features at once to save time
                                Range(2, 3).contains(it.size) && !it.containsSameTypeFeatures()
                            }) {
                            useCaseCombinationsToTest.forEach { useCases ->
                                add(
                                    arrayOf(
                                        "config=${Camera2Config::class.simpleName} lensFacing={$lens}" +
                                            " featureGroup={$featureGroup} useCases = {$useCases}",
                                        selector,
                                        Camera2Config::class.simpleName,
                                        Camera2Config.defaultConfig(),
                                        featureGroup,
                                        useCases,
                                    )
                                )
                            }
                        }
                    }
            }
    }
}
