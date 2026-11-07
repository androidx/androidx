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

package androidx.camera.camera2.compat.quirk

import android.os.Build
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.impl.CameraCallbackMap
import androidx.camera.camera2.impl.CameraGraphConfigProvider
import androidx.camera.camera2.impl.ComboRequestListener
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.common.CameraId
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class DisableAbortCapturesOnStopQuirkTest(
    private val brand: String,
    private val model: String,
    private val device: String,
    private val expectedEnabled: Boolean,
) {
    @Before
    fun setUp() {
        ShadowBuild.reset()
    }

    @After
    fun tearDown() {
        ShadowBuild.reset()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Brand: {0}, Model: {1}, Device: {2}, Enabled: {3}"
        )
        fun data() =
            listOf(
                // Tecno devices
                arrayOf<Any>("TECNO", "TECNO BF6", "TECNO-BF6", true),
                arrayOf<Any>("TECNO-MOBILE", "tecno mobile bf6", "TECNO-BF6", true),
                // Samsung Galaxy Note 10+ (b/431912245)
                arrayOf<Any>("Samsung", "SM-N975U1", "d2q", true),
                // Samsung Galaxy A54 5G variants (b/583056763)
                arrayOf<Any>("Samsung", "SM-A546E", "a54x", true),
                arrayOf<Any>("Samsung", "SM-A546U1", "a54x", true),
                arrayOf<Any>("Samsung", "SC-53D", "a54x", true),
                arrayOf<Any>("Samsung", "sm-a546b", "a54xnsxx", true),
                // Poco X3 Pro (b/356792947)
                arrayOf<Any>("POCO", "M2102J20SG", "vayu", true),
                // Unaffected Samsung and non-Samsung devices
                arrayOf<Any>("Samsung", "SM-A536U", "a53x", false),
                arrayOf<Any>("Samsung", "SM-A556E", "a55x", false),
                arrayOf<Any>("Samsung", "SM-S921B", "e1s", false),
                arrayOf<Any>("Oppo", "CPH2239", "a54x", false),
                arrayOf<Any>("Google", "Pixel 8", "shiba", false),
            )
    }

    @Test
    fun canEnableQuirkAndConfigureAbortCapturesOnStop() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(brand)
        ShadowBuild.setModel(model)
        ShadowBuild.setDevice(device)

        assertThat(DisableAbortCapturesOnStopQuirk.isEnabled()).isEqualTo(expectedEnabled)
        assertThat(DeviceQuirks[DisableAbortCapturesOnStopQuirk::class.java] != null)
            .isEqualTo(expectedEnabled)

        val cameraMetadata = FakeCameraMetadata(cameraId = CameraId("0"))
        val streamMap =
            StreamConfigurationMapCompat(
                StreamConfigurationMapBuilder.newBuilder().build(),
                cameraMetadata,
            )
        val configProvider =
            CameraGraphConfigProvider(
                callbackMap = CameraCallbackMap(),
                requestListener = ComboRequestListener(),
                cameraConfig = CameraConfig(CameraId("0")),
                cameraQuirks = CameraQuirks(cameraMetadata, streamMap),
                zslControl = ZslControlNoOpImpl(),
                templateParamsOverride = NoOpTemplateParamsOverride,
                cameraMetadata = cameraMetadata,
            )
        val bundle =
            configProvider.create(
                operatingMode = OperatingMode.NORMAL,
                sessionConfig = null,
                setOutputType = false,
            )

        val expectedAbortCapturesOnStop =
            if (expectedEnabled) {
                false
            } else {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            }
        assertThat(bundle.graphConfig.flags.abortCapturesOnStop)
            .isEqualTo(expectedAbortCapturesOnStop)
    }
}
