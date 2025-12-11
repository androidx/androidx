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

package androidx.compose.ui.platform

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class UiKitPlatformClipboardTest {

    // TODO: consider writing instrumented tests for Clipboard
    // The unit tests can't use (copy/paste) the native UIPasteboard:
    // "Cannot connect to pasteboard server" - an error at runtime

    @Test
    fun canGetLocalClipboard() = runComposeUiTest {
        var clipboard: Clipboard? = null
        var nativeClipboard: NativeClipboard? = null

        setContent {
            clipboard = LocalClipboard.current
            nativeClipboard = clipboard?.nativeClipboard
        }

        waitForIdle()

        assertNotNull(clipboard)
        assertNotNull(nativeClipboard)
    }

    @Test
    fun smokeTestUseClipboardMethods() = runComposeUiTest {
        var clipboard: Clipboard? = null
        var nativeClipboard: NativeClipboard? = null

        setContent {
            clipboard = LocalClipboard.current
            nativeClipboard = clipboard?.nativeClipboard
        }

        waitForIdle()

        assertNotNull(clipboard)
        assertNotNull(nativeClipboard)

        runBlocking {
            // Can't do much more asserts in a unit test. For more checks use an instrumented test
            clipboard!!.setClipEntry(null)
            clipboard!!.setClipEntry(ClipEntry.withPlainText("test"))
            clipboard!!.getClipEntry()
        }
    }
}