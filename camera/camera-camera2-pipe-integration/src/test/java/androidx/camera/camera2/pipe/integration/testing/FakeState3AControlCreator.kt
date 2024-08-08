/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.State3AControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import org.robolectric.shadows.StreamConfigurationMapBuilder

object FakeState3AControlCreator {
    fun createState3AControl(
        properties: CameraProperties = FakeCameraProperties(),
        requestControl: UseCaseCameraRequestControl = FakeUseCaseCameraRequestControl(),
    ) =
        State3AControl(
                properties,
                NoOpAutoFlashAEModeDisabler,
                AeFpsRange(
                    CameraQuirks(
                        properties.metadata,
                        StreamConfigurationMapCompat(
                            StreamConfigurationMapBuilder.newBuilder().build(),
                            OutputSizesCorrector(
                                properties.metadata,
                                StreamConfigurationMapBuilder.newBuilder().build()
                            )
                        )
                    )
                ),
            )
            .apply { this.requestControl = requestControl }
}
