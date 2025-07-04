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

package androidx.camera.viewfinder.impl

import android.os.Build
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.impl.ImplementationModeCompat
import androidx.camera.viewfinder.core.impl.quirk.DeviceQuirks
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument // Needed for Robolectric to correctly instrument classes in the same module
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImplementationModeCompatTest {
    private lateinit var originalManufacturer: String
    private lateinit var originalDevice: String
    private lateinit var originalModel: String

    @Before
    fun setUp() {
        // Store original Build properties to restore them after each test
        originalManufacturer = Build.MANUFACTURER
        originalDevice = Build.DEVICE
        originalModel = Build.MODEL
    }

    @After
    fun tearDown() {
        setBuildPropertiesAndReload(
            manufacturer = originalManufacturer,
            device = originalDevice,
            model = originalModel,
        )
    }

    // --- General API Level Tests (No specific device quirks) ---
    @Test
    // This test will run for the effective minSdk (LOLLIPOP from class) and
    // effective maxSdk (N from this method's config).
    // Both LOLLIPOP (21/22) and N (24) should result in EMBEDDED.
    @Config(maxSdk = Build.VERSION_CODES.N)
    fun chooseCompatibleMode_returnsEmbedded_onApi24AndBelow() {
        assertThat(ImplementationModeCompat.chooseCompatibleMode())
            .isEqualTo(ImplementationMode.EMBEDDED)
    }

    @Test
    // This test will run for the effective minSdk (N_MR1 from this method's config) and up.
    // N_MR1 (25) and higher should result in EXTERNAL.
    @Config(minSdk = Build.VERSION_CODES.N_MR1)
    fun chooseCompatibleMode_returnsExternal_onApi25AndAbove() {
        assertThat(ImplementationModeCompat.chooseCompatibleMode())
            .isEqualTo(ImplementationMode.EXTERNAL)
    }

    // --- Tests for SurfaceViewNotCroppedByParentQuirk (e.g., Redmi Note 10) ---
    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // API 28 (normally EXTERNAL)
    fun chooseMode_returnsEmbedded_onRedmiNote10_onApi28_dueToNotCroppedQuirk() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EMBEDDED,
            manufacturer = "Xiaomi",
            model = "M2101K7AG",
        )
    }

    // --- Tests for SurfaceViewStretchedQuirk (e.g., Galaxy Z Fold2) ---
    @Test
    @Config(
        sdk = [Build.VERSION_CODES.N_MR1]
    ) // API 25 (normally EXTERNAL, but quirk active on this API)
    fun chooseMode_returnsEmbedded_onGalaxyZFold2_onApi25_dueToStretchedQuirk() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EMBEDDED,
            manufacturer = "samsung",
            device = "f2q",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.TIRAMISU]
    ) // API 33 (normally EXTERNAL, quirk NOT active on this API)
    fun chooseMode_returnsExternal_onGalaxyZFold2_onApi26_whenStretchedQuirkInactive() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EXTERNAL,
            manufacturer = "samsung",
            device = "f2q",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.N_MR1]
    ) // API 25 (normally EXTERNAL, but quirk active on this API)
    fun chooseMode_returnsEmbedded_onGalaxyZFold3_onApi25_dueToStretchedQuirk() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EMBEDDED,
            manufacturer = "samsung",
            device = "q2q",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.TIRAMISU]
    ) // API 33 (normally EXTERNAL, quirk NOT active on this API)
    fun chooseMode_returnsExternal_onGalaxyZFold3_onApi26_whenStretchedQuirkInactive() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EXTERNAL,
            manufacturer = "samsung",
            device = "q2q",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.N_MR1]
    ) // API 25 (normally EXTERNAL, but quirk active on this API)
    fun chooseMode_returnsEmbedded_onOppoFindN_onApi25_dueToStretchedQuirk() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EMBEDDED,
            manufacturer = "Oppo",
            device = "OP4E75L1",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.TIRAMISU]
    ) // API 33 (normally EXTERNAL, quirk NOT active on this API)
    fun chooseMode_returnsExternal_onOppoFindN_onApi26_whenStretchedQuirkInactive() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EXTERNAL,
            manufacturer = "Oppo",
            device = "OP4E75L1",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.N_MR1]
    ) // API 25 (normally EXTERNAL, but quirk active on this API)
    fun chooseMode_returnsEmbedded_onLenovoTabP12Pro_onApi25_dueToStretchedQuirk() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EMBEDDED,
            manufacturer = "Lenovo",
            device = "Q706F",
        )
    }

    @Test
    @Config(
        sdk = [Build.VERSION_CODES.TIRAMISU]
    ) // API 33 (normally EXTERNAL, quirk NOT active on this API)
    fun chooseMode_returnsExternal_onLenovoTabP12Pro_onApi26_whenStretchedQuirkInactive() {
        chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
            expectedImplementationMode = ImplementationMode.EXTERNAL,
            manufacturer = "Lenovo",
            device = "Q706F",
        )
    }

    private fun chooseCompatibleMode_returnsExpected_dueToQuirkAvailability(
        expectedImplementationMode: ImplementationMode,
        manufacturer: String,
        device: String? = null,
        model: String? = null,
    ) {
        // This only makes sense to call on API 25+. API 24- will always return EMBEDDED, so we
        // can't tell if it is due to quirk.
        assertWithMessage("assertQuirkChoosesEmbeddedOnDevice should only be called on API 25+")
            .that(Build.VERSION.SDK_INT)
            .isAtLeast(Build.VERSION_CODES.N_MR1)

        setBuildPropertiesAndReload(manufacturer = manufacturer, device = device, model = model)

        assertThat(ImplementationModeCompat.chooseCompatibleMode())
            .isEqualTo(expectedImplementationMode)
    }

    private fun setBuildPropertiesAndReload(
        manufacturer: String,
        device: String? = null,
        model: String? = null,
    ) {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", manufacturer)
        device?.let { ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", device) }
        model?.let { ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model) }
        DeviceQuirks.reload() // Ensure quirks are reloaded with new Build properties
    }
}
