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

package androidx.compose.ui.tooling

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewDeviceSpecTest {
    @Test
    fun testSupportedParameters() {
        val supportedParameters =
            Class.forName("androidx.compose.ui.tooling.PreviewDeviceSpec")
                .getDeclaredField("SUPPORTED_PARAMETERS")
                .apply { isAccessible = true }
                .get(null) as String
        assertEquals(
            setOf(
                "windowPosture",
                "pointerPrecision",
                "keyboardKind",
                "hasCamera",
                "hasMicrophone",
                "viewingDistance",
                "expand",
                "maxHeight",
                "maxWidth",
            ),
            supportedParameters.split(',').toSet(),
        )
    }
}
