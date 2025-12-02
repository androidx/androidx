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

import androidx.compose.ui.HeadlessTest
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.runHeadlessComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

@OptIn(ExperimentalTestApi::class)
class ClipboardTest {

    var clipboard: Clipboard? = null
    var awtClipboard: java.awt.datatransfer.Clipboard? = null

    @Before
    fun setup() {
        clipboard = null
        awtClipboard = null
    }

    @Test
    fun hasClipboard() = clipboardTest {
        assertNotNull(clipboard)
        assertNotNull(clipboard!!.nativeClipboard)
        assertTrue(clipboard!!.nativeClipboard is java.awt.datatransfer.Clipboard)
        assertNotNull(awtClipboard)
    }

    @Test
    fun makeClipboardEmpty() = clipboardTest {
        clipboard!!.setClipEntry(null)
        assertNull(clipboard!!.getClipEntry())
        assertTrue(awtClipboard!!.availableDataFlavors.isEmpty())
    }

    @Test
    fun setTextToClipboard() = clipboardTest {
        clipboard!!.setClipEntry(null)
        assertNull(clipboard!!.getClipEntry())
        assertTrue(awtClipboard!!.availableDataFlavors.isEmpty())

        clipboard!!.setClipEntry(ClipEntry(StringSelection("test")))
        assertEquals("test", awtClipboard!!.getData(DataFlavor.stringFlavor))

        val ce = clipboard!!.getClipEntry()
        assertNotNull(ce)
        assertEquals("test", ce.asAwtTransferable!!.getTransferData(DataFlavor.stringFlavor))

        clipboard!!.setClipEntry(null)
        assertNull(clipboard!!.getClipEntry())
        assertTrue(awtClipboard!!.availableDataFlavors.isEmpty())
    }

    private fun clipboardTest(block: suspend WindowTestScope.() -> Unit) = runApplicationTest {
        assertNull(clipboard)

        val window = ComposeWindow()
        try {
            window.size = Dimension(300, 400)
            window.setContent {
                clipboard = LocalClipboard.current
                awtClipboard = clipboard!!.nativeClipboard as java.awt.datatransfer.Clipboard
            }
            window.isUndecorated = true
            window.isVisible = true
            window.paint(window.graphics)
            awaitIdle()
            block()
        } finally {
            window.dispose()
        }
    }

    @Test
    @Category(HeadlessTest::class)
    fun nativeClipboardDoesNotCrash() = runHeadlessComposeUiTest {
        lateinit var clipboard: Clipboard
        setContent {
            clipboard = LocalClipboard.current
        }

        clipboard.nativeClipboard  // Just check it doesn't crash
        assertEquals(null, clipboard.awtClipboard)
    }
}