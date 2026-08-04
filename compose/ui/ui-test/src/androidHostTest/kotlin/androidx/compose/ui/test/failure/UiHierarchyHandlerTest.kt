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

package androidx.compose.ui.test.failure

import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.TestFailurePolicy
import androidx.compose.ui.test.TestFailurePolicy.CaptureMode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.io.PlatformTestStorage
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
@OptIn(ExperimentalTestApi::class)
class UiHierarchyHandlerTest {
    private val originalStorage = PlatformTestStorageRegistry.getInstance()

    @After
    fun tearDown() {
        PlatformTestStorageRegistry.registerInstance(originalStorage)
    }

    @Test
    fun containsHeader() {
        val content = dumpHierarchyForTest {
            setContent { Box(Modifier.testTag("box")) }
            onNodeWithTag("non_existent").assertExists()
        }
        assertTrue(content.contains("View and Compose Hierarchy"))
    }

    @Test
    fun interleavesComposeUnderView() {
        val content = dumpHierarchyForTest {
            setContent { Box(Modifier.testTag("compose_root_box")) }
            onNodeWithTag("non_existent").assertExists()
        }
        val viewIndex = content.indexOf("AndroidComposeView")
        val composeIndex = content.indexOf("Tag: 'compose_root_box'")

        assertTrue("Expected AndroidComposeView in dump", viewIndex != -1)
        assertTrue("Expected Compose node after AndroidComposeView", composeIndex > viewIndex)
    }

    @Test
    fun multipleWindows_dumpsInOrder() {
        val content = dumpHierarchyForTest {
            setContent {
                Box(Modifier.testTag("main_window_box")) {
                    Popup(alignment = Alignment.Center) {
                        Box(Modifier.testTag("popup_window_box"))
                    }
                }
            }
            onNodeWithTag("non_existent").assertExists()
        }

        val window0 = content.indexOf("Window (index = 0)")
        val mainBox = content.indexOf("Tag: 'main_window_box'")
        val window1 = content.indexOf("Window (index = 1)")
        val popupBox = content.indexOf("Tag: 'popup_window_box'")

        assertTrue("Expected main window content under Window 0", mainBox in window0 until window1)
        assertTrue("Expected popup content after Window 1", popupBox > window1)
    }

    @Test
    fun androidViewInCompose_dumpsInOrder() {
        val content = dumpHierarchyForTest {
            setContent {
                Box(Modifier.testTag("parent_compose_box")) {
                    AndroidView(factory = { FrameLayout(it) })
                }
            }
            onNodeWithTag("non_existent").assertExists()
        }

        val composeBox = content.indexOf("Tag: 'parent_compose_box'")
        val frameLayout = content.lastIndexOf("FrameLayout")

        assertTrue("Expected parent Compose box in dump", composeBox != -1)
        assertTrue(
            "Expected embedded FrameLayout after parent Compose node",
            frameLayout > composeBox,
        )
    }

    @Test
    fun composeInAndroidView_dumpsInOrder() {
        val content = dumpHierarchyForTest {
            setContent {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            addView(
                                ComposeView(ctx).apply {
                                    setContent { Box(Modifier.testTag("nested_compose_box")) }
                                }
                            )
                        }
                    }
                )
            }
            onNodeWithTag("non_existent").assertExists()
        }

        val frameLayout = content.lastIndexOf("FrameLayout")
        val composeView = content.lastIndexOf("ComposeView")
        val nestedBox = content.indexOf("Tag: 'nested_compose_box'")

        assertTrue("Expected FrameLayout before nested ComposeView", composeView > frameLayout)
        assertTrue("Expected nested Compose box after ComposeView", nestedBox > composeView)
    }

    @Test
    fun emptyRoots_printsEmptyMessage() {
        val memoryStorage = MemoryTestStorage()
        PlatformTestStorageRegistry.registerInstance(memoryStorage)

        val handler = AndroidUiHierarchyHandler()
        val fileName = "empty_roots_ui.txt"
        handler.export(fileName, emptySet())

        val uiBytes = memoryStorage.outputFiles[fileName]
        requireNotNull(uiBytes) { "UI Hierarchy file was never written to storage" }
        val uiString = uiBytes.toString("UTF-8").replace("\r\n", "\n")

        val expected =
            "====================================================\n" +
                "--- No UI hierarchy found ---\n" +
                "====================================================\n" +
                "\n"
        assertEquals(expected, uiString)
    }

    private fun dumpHierarchyForTest(testBody: ComposeUiTest.() -> Unit): String {
        val memoryStorage = MemoryTestStorage()
        PlatformTestStorageRegistry.registerInstance(memoryStorage)

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Disabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                    )
            )

        assertThrows(AssertionError::class.java) { runComposeUiTest(config, testBody) }

        val uiFile = memoryStorage.outputFiles.entries.first { it.key.endsWith("_ui.txt") }
        return uiFile.value.toString("UTF-8")
    }

    class MemoryTestStorage : PlatformTestStorage {
        val outputFiles = mutableMapOf<String, ByteArrayOutputStream>()

        override fun openOutputFile(pathname: String): OutputStream {
            val stream = ByteArrayOutputStream()
            outputFiles[pathname] = stream
            return stream
        }

        override fun openOutputFile(pathname: String?, append: Boolean): OutputStream? = null

        override fun addOutputProperties(properties: Map<String?, Serializable?>?) {}

        override fun getOutputProperties(): Map<String?, Serializable?>? = null

        override fun getInputFileUri(pathname: String): Uri? = null

        override fun getOutputFileUri(pathname: String): Uri? = null

        override fun isTestStorageFilePath(pathname: String): Boolean = false

        override fun openInputFile(pathname: String): InputStream {
            throw UnsupportedOperationException("Not needed for this test")
        }

        override fun getInputArg(argName: String): String? = null

        override fun getInputArgs(): Map<String?, String?>? = null
    }
}
