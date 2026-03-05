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

package androidx.compose.ui.test

import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalFontLoader
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CompositionLocalsInTestsTest {

    @Test
    fun lifecycleInComposeTest() = runComposeUiTest {
        setContent {
            val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
            assertEquals(Lifecycle.State.RESUMED, lifecycleState)
        }
    }

    @Test
    fun commonCompositionLocalsInComposeTest() = runComposeUiTest {
        setContent {
            assertNotNull(LocalAccessibilityManager.current)
            @Suppress("DEPRECATION")
            LocalAutofill.current
            LocalAutofillManager.current
            @Suppress("DEPRECATION")
            assertNotNull(LocalAutofillTree.current)
            @Suppress("DEPRECATION")
            assertNotNull(LocalClipboardManager.current)
            assertNotNull(LocalClipboard.current)
            assertTrue(LocalDensity.current.density > 0f)
            assertNotNull(LocalFocusManager.current)
            @Suppress("DEPRECATION")
            assertNotNull(LocalFontLoader.current)
            assertNotNull(LocalFontFamilyResolver.current)
            assertNotNull(LocalHapticFeedback.current)
            assertNotNull(LocalInputModeManager.current)
            LocalLayoutDirection.current
            @Suppress("DEPRECATION")
            assertNotNull(LocalTextInputService.current)
            assertNotNull(LocalSoftwareKeyboardController.current)
            assertNotNull(LocalTextToolbar.current)
            assertNotNull(LocalUriHandler.current)
            assertNotNull(LocalViewConfiguration.current)
            assertNotNull(LocalWindowInfo.current)
            assertNotNull(LocalGraphicsContext.current)
            assertFalse(LocalLocaleList.current.isEmpty())
            assertNotNull(LocalRetainedValuesStore.current)
        }
    }
}
