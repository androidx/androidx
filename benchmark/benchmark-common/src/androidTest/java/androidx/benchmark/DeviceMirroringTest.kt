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

package androidx.benchmark

import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Enclosed::class)
class DeviceMirroringTest {

    @RunWith(Parameterized::class)
    class DeviceMirroringActiveDumpsysTest(val dumpsysOutputFile: String) {
        @Test
        fun hasDisplayForStudioDeviceMirroringInDumpsys_deviceMirroringActive_returnsTrue() {
            val testDumpsysOutput = readTextAsset(dumpsysOutputFile)
            val hasMirroringDisplay =
                DeviceMirroring.hasDisplayForStudioDeviceMirroringInSurfaceFlingerDump(
                    testDumpsysOutput
                )
            assertWithMessage("Test case: $dumpsysOutputFile").that(hasMirroringDisplay).isTrue()
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun parameters(): Array<String> =
                arrayOf(
                    "htcU11Life_api26_surfaceflingerDump_deviceMirroring_active.txt",
                    "blackberryKey2_api27_surfaceflingerDump_deviceMirroring_active.txt",
                    "sonyG8441_api28_surfaceflingerDump_deviceMirroring_active.txt",
                    "motoGPower_api30_surfaceflingerDump_deviceMirroring_active.txt",
                    "lenovoTab_api30_surfaceflingerDump_deviceMirroring_active.txt",
                    "wembley_api31_surfaceflingerDump_deviceMirroring_active.txt",
                    "wembley_api32_surfaceflingerDump_deviceMirroring_active.txt",
                    "s23plus_api34_surfaceflingerDump_deviceMirroring_active.txt",
                    "mokey_api35_surfaceflingerDump_deviceMirroring_active.txt",
                    "pixelwatch3_api35_surfaceflingerDump_deviceMirroring_active.txt",
                    "pixel9a_api36_surfaceflingerDump_deviceMirroring_active.txt",
                )
        }
    }

    @RunWith(Parameterized::class)
    class DeviceMirroringInactiveDumpsysTest(val dumpsysOutputFile: String) {
        @Test
        fun hasDisplayForStudioDeviceMirroringInDumpsys_deviceMirroringInactive_returnsFalse() {
            val testDumpsysOutput = readTextAsset(dumpsysOutputFile)
            val hasMirroringDisplay =
                DeviceMirroring.hasDisplayForStudioDeviceMirroringInSurfaceFlingerDump(
                    testDumpsysOutput
                )
            assertWithMessage("Test case: $dumpsysOutputFile").that(hasMirroringDisplay).isFalse()
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun parameters(): Array<String> =
                arrayOf(
                    "htcU11Life_api26_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "blackberryKey2_api27_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "sonyG8441_api28_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "motoGPower_api30_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "lenovoTab_api30_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "pixel5_api31_surfaceflingerDump_deviceMirroring_inactive_scrcpyActive.txt",
                    "wembley_api31_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "wembley_api32_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "s23plus_api34_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "mokey_api35_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "pixelwatch3_api35_surfaceflingerDump_deviceMirroring_inactive.txt",
                    "pixel9a_api36_surfaceflingerDump_deviceMirroring_inactive.txt",
                )
        }
    }
}

private fun readTextAsset(filename: String): String {
    return InstrumentationRegistry.getInstrumentation().context.assets.open(filename).reader().use {
        it.readText()
    }
}
