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

package androidx.compose.ui.text.platform

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.awt.GraphicsEnvironment
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class AwtFontInteropTest {

    @Before
    fun setUp() {
        assertTrue(
            InternalFontApiChecker.isRunningOnJetBrainsRuntime(),
            "Not running on the JetBrains Runtime",
        )
        assertTrue(
            InternalFontApiChecker.isSunFontApiAccessible(),
            "Missing --add-opens java.desktop/sun.font=ALL-UNNAMED",
        )
    }

    @Test
    fun `should not crash when converting AWT fonts to Compose font families`() {
        val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
        for (font in fonts) {
            font.asFontFamily()
        }
    }

    @Test
    fun `should render different families and styles differently`() {
        val fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().allFonts
        val familyNames = fonts.mapNotNull { it.composeFontFamilyNameOrNull() }
            .toSet()

        var lastFamilyBytes: ByteArray? = null
        var lastFamilyName: String? = null
        for (familyName in familyNames) {
            val bitmap = renderComposeScene(400, 50) {
                BasicText(
                    "the brown fox jumps over the lazy dog",
                    style = TextStyle.Default.copy(
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Normal,
                        fontSize = 12.sp
                    )
                )
            }

            val currentBytes = bitmap.peekPixels()!!.buffer.bytes
            if (lastFamilyBytes != null) {
                assertNotEquals(
                    lastFamilyBytes,
                    currentBytes,
                    "$familyName seems identical to $lastFamilyName when rendered"
                )
            }

            lastFamilyBytes = currentBytes
            lastFamilyName = familyName
        }
    }
}
