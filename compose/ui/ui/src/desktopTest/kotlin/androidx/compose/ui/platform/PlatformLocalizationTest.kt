/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.runApplicationTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class PlatformLocalizationTest {
    /**
     * Verify that [LocalLocalization] exists in [ApplicationScope].
     */
    @Test
    fun platformLocalizationExistsByDefaultInApp() = runApplicationTest {
        var localization: PlatformLocalization? = null
        launchTestApplication {
            localization = LocalLocalization.current
        }.join()

        assertNotNull(localization)
    }

    /**
     * Verify that [LocalLocalization] is available to the composable passed to
     * [ComposeUiTest.setContent].
     */
    @Test
    fun platformLocalizationExistsByDefaultInTest() = runComposeUiTest {
        var localization: PlatformLocalization? = null
        setContent {
            localization = LocalLocalization.current
        }

        assertNotNull(localization)
    }

    /**
     * Verify that providing a [PlatformLocalization] in [LocalLocalization] correctly overrides the
     * default one.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun canOverridePlatformLocalizationInContextMenuArea() = runComposeUiTest {
        val customLocalization = object : PlatformLocalization {
            override val copy: String
                get() = "My Copy"
            override val cut: String
                get() = "My Cut"
            override val paste: String
                get() = "My Paste"
            override val selectAll: String
                get() = "My Select All"
        }

        val textManager = object: TextContextMenu.TextManager {
            override val selectedText: AnnotatedString
                get() = AnnotatedString("")
            override val cut = { }
            override val copy = { }
            override val paste = { }
            override val selectAll = { }
            override fun selectWordAtPositionIfNotAlreadySelected(offset: Offset) { }
        }

        setContent {
            CompositionLocalProvider(LocalLocalization provides customLocalization) {
                val state = ContextMenuState().also {
                    it.status = ContextMenuState.Status.Open(Rect(0f, 0f, 100f, 100f))
                }
                LocalTextContextMenu.current.Area(textManager, state) {
                    Box(Modifier.size(200.dp))
                }
            }
        }

        onNodeWithText("My Copy").assertExists()
        onNodeWithText("My Cut").assertExists()
        onNodeWithText("My Paste").assertExists()
        onNodeWithText("My Select All").assertExists()

        onNodeWithText("Copy").assertDoesNotExist()
        onNodeWithText("Cut").assertDoesNotExist()
        onNodeWithText("Paste").assertDoesNotExist()
        onNodeWithText("Select All").assertDoesNotExist()
    }

    /**
     * Verify that providing a [PlatformLocalization] in [LocalLocalization] in the application
     * scope correctly overrides it in the window scope.
     */
    @Test
    fun canOverridePlatformLocalizationFromAppToWindowScope() = runApplicationTest {
        val customLocalization = object : PlatformLocalization {
            override val copy: String
                get() = "My Copy"
            override val cut: String
                get() = "My Cut"
            override val paste: String
                get() = "My Paste"
            override val selectAll: String
                get() = "My Select All"
        }

        lateinit var actualLocalization: PlatformLocalization
        launchTestApplication {
            CompositionLocalProvider(LocalLocalization provides customLocalization) {
                Window(onCloseRequest = { }) {
                    actualLocalization = LocalLocalization.current
                }
            }
        }
        awaitIdle()

        assertEquals(customLocalization, actualLocalization)
    }

    /**
     * Verify that providing a [PlatformLocalization] in [LocalLocalization] in the window scope
     * correctly overrides it in a [Popup].
     */
    @Test
    fun canOverridePlatformLocalizationFromWindowToPopupScope() = runComposeUiTest {
        val customLocalization = object : PlatformLocalization {
            override val copy: String
                get() = "My Copy"
            override val cut: String
                get() = "My Cut"
            override val paste: String
                get() = "My Paste"
            override val selectAll: String
                get() = "My Select All"
        }

        lateinit var actualLocalization: PlatformLocalization
        setContent {
            CompositionLocalProvider(LocalLocalization provides customLocalization) {
                Popup {
                    actualLocalization = LocalLocalization.current
                }
            }
        }

        assertEquals(customLocalization, actualLocalization)
    }
}