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

package androidx.compose.ui.platform

import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.test.runUIKitInstrumentedTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.jetbrains.skiko.SystemTheme
import platform.UIKit.UIUserInterfaceStyle

class SystemThemeTest {

    @Test
    fun testInitialOverrideUserInterfaceStyleLight() = runUIKitInstrumentedTest {
        appDelegate.window?.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
        var systemTheme: SystemTheme? = null
        setContent {
            systemTheme = LocalSystemTheme.current
        }

        assertEquals(SystemTheme.LIGHT, systemTheme)
    }

    @Test
    fun testInitialOverrideUserInterfaceStyleDark() = runUIKitInstrumentedTest {
        appDelegate.window?.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
        var systemTheme: SystemTheme? = null
        setContent {
            systemTheme = LocalSystemTheme.current
        }

        assertEquals(SystemTheme.DARK, systemTheme)
    }

    @Test
    fun testOverrideUserInterfaceStyle() = runUIKitInstrumentedTest {
        var systemTheme: SystemTheme? = null
        setContent {
            systemTheme = LocalSystemTheme.current
        }

        assertNotNull(systemTheme)

        appDelegate.window?.overrideUserInterfaceStyle =
            UIUserInterfaceStyle.UIUserInterfaceStyleLight
        waitUntil("System theme should eventually be Light") { systemTheme == SystemTheme.LIGHT }

        appDelegate.window?.overrideUserInterfaceStyle =
            UIUserInterfaceStyle.UIUserInterfaceStyleDark
        waitUntil("System theme should eventually be Dark") { systemTheme == SystemTheme.DARK }

        appDelegate.window?.overrideUserInterfaceStyle =
            UIUserInterfaceStyle.UIUserInterfaceStyleLight
        waitUntil("System theme should eventually be Light") { systemTheme == SystemTheme.LIGHT }
    }
}
