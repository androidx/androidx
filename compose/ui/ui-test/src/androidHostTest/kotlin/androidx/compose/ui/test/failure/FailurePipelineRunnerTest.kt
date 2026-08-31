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
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.FailureArtifact
import androidx.compose.ui.test.FailureContext
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.TestFailureHandler
import androidx.compose.ui.test.TestFailurePolicy
import androidx.compose.ui.test.TestFailurePolicy.CaptureMode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorage
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
class FailurePipelineRunnerTest {

    private val originalInstrumentation = InstrumentationRegistry.getInstrumentation()
    private val originalArguments = InstrumentationRegistry.getArguments()
    private val originalStorage = PlatformTestStorageRegistry.getInstance()

    @After
    fun tearDown() {
        InstrumentationRegistry.registerInstance(originalInstrumentation, originalArguments)
        PlatformTestStorageRegistry.registerInstance(originalStorage)
    }

    @Test
    fun runPipeline_runsOnlyOnce_subsequentCallsRethrowDirectly() {
        var handlerCallCount = 0
        val fakeScreenshot = FakeScreenshotHandler()
        val fakeHierarchy = FakeUiHierarchyHandler()

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers = listOf(TestFailureHandler { handlerCallCount++ }),
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        val rootError = AssertionError("First failure")

        // First execution runs handlers and capturers
        val firstError =
            assertThrows(AssertionError::class.java) { runner.runPipeline(rootError, emptySet()) }
        assertSame(rootError, firstError)
        assertEquals(1, handlerCallCount)
        assertEquals(1, fakeScreenshot.exportedFiles.size)
        assertEquals(1, fakeHierarchy.exportedFiles.size)

        // Second execution rethrows directly without calling handlers or capturers again
        assertThrows(AssertionError::class.java) {
            runner.runPipeline(AssertionError("Second failure"), emptySet())
        }
        assertEquals(1, handlerCallCount)
        assertEquals(1, fakeScreenshot.exportedFiles.size)
        assertEquals(1, fakeHierarchy.exportedFiles.size)
    }

    @Test
    fun screenshotExportException_isSuppressedAndLogged_pipelineContinues() {
        val screenshotException = IOException("Screenshot storage disk full")
        val fakeScreenshot = FakeScreenshotHandler { throw screenshotException }
        val fakeHierarchy = FakeUiHierarchyHandler()
        var capturedContext: FailureContext? = null

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers =
                            listOf(TestFailureHandler { context -> capturedContext = context }),
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        val rootError = AssertionError("Test assertion failed")
        val thrown =
            assertThrows(AssertionError::class.java) { runner.runPipeline(rootError, emptySet()) }

        assertSame(rootError, thrown)
        assertTrue(thrown.suppressed.contains(screenshotException))
        assertEquals(1, fakeHierarchy.exportedFiles.size)

        // Screenshot failed, so only UI hierarchy should be in artifacts
        requireNotNull(capturedContext)
        assertEquals(1, capturedContext.artifacts.size)
        assertEquals(FailureArtifact.Type.UiHierarchy, capturedContext.artifacts[0].type)
    }

    @Test
    fun uiHierarchyExportException_isSuppressedAndLogged_pipelineContinues() {
        val hierarchyException = RuntimeException("Failed to dump semantics")
        val fakeScreenshot = FakeScreenshotHandler()
        val fakeHierarchy = FakeUiHierarchyHandler { _, _ -> throw hierarchyException }
        var capturedContext: FailureContext? = null

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers =
                            listOf(TestFailureHandler { context -> capturedContext = context }),
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        val rootError = AssertionError("Test assertion failed")
        val thrown =
            assertThrows(AssertionError::class.java) { runner.runPipeline(rootError, emptySet()) }

        assertSame(rootError, thrown)
        assertTrue(thrown.suppressed.contains(hierarchyException))
        assertEquals(1, fakeScreenshot.exportedFiles.size)

        // UI hierarchy failed, so only screenshot should be in artifacts
        requireNotNull(capturedContext)
        assertEquals(1, capturedContext.artifacts.size)
        assertEquals(FailureArtifact.Type.Screenshot, capturedContext.artifacts[0].type)
    }

    @Test
    fun allExceptions_areAccumulatedInSuppressedList() {
        val screenshotException = IOException("Screenshot failed")
        val hierarchyException = RuntimeException("Hierarchy failed")
        val handlerException = IllegalStateException("Handler failed")

        val fakeScreenshot = FakeScreenshotHandler { throw screenshotException }
        val fakeHierarchy = FakeUiHierarchyHandler { _, _ -> throw hierarchyException }

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers = listOf(TestFailureHandler { throw handlerException }),
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        val rootError = AssertionError("Root assertion error")
        val thrown =
            assertThrows(AssertionError::class.java) { runner.runPipeline(rootError, emptySet()) }

        assertSame(rootError, thrown)
        val suppressed = thrown.suppressed.toList()
        assertEquals(3, suppressed.size)
        assertTrue(suppressed.contains(screenshotException))
        assertTrue(suppressed.contains(hierarchyException))
        assertTrue(suppressed.contains(handlerException))
    }

    @Test
    fun captureModes_enabledOverridesGlobalFallback() {
        val newArguments =
            Bundle(originalArguments).apply {
                putString("androidx.compose.ui.test.failure.isScreenshotCaptureEnabled", "false")
                putString("androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled", "false")
            }
        InstrumentationRegistry.registerInstance(originalInstrumentation, newArguments)

        val fakeScreenshot = FakeScreenshotHandler()
        val fakeHierarchy = FakeUiHierarchyHandler()

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        assertThrows(AssertionError::class.java) {
            runner.runPipeline(AssertionError("Error"), emptySet())
        }

        assertEquals(1, fakeScreenshot.exportedFiles.size)
        assertEquals(1, fakeHierarchy.exportedFiles.size)
    }

    @Test
    fun captureModes_disabledOverridesGlobalFallback() {
        val newArguments =
            Bundle(originalArguments).apply {
                putString("androidx.compose.ui.test.failure.isScreenshotCaptureEnabled", "true")
                putString("androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled", "true")
            }
        InstrumentationRegistry.registerInstance(originalInstrumentation, newArguments)

        val fakeScreenshot = FakeScreenshotHandler()
        val fakeHierarchy = FakeUiHierarchyHandler()

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Disabled,
                        uiHierarchyCaptureMode = CaptureMode.Disabled,
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        assertThrows(AssertionError::class.java) {
            runner.runPipeline(AssertionError("Error"), emptySet())
        }

        assertEquals(0, fakeScreenshot.exportedFiles.size)
        assertEquals(0, fakeHierarchy.exportedFiles.size)
    }

    @Test
    fun captureModes_unspecifiedUsesGlobalFallback() {
        val newArguments =
            Bundle(originalArguments).apply {
                putString("androidx.compose.ui.test.failure.isScreenshotCaptureEnabled", "true")
                putString("androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled", "false")
            }
        InstrumentationRegistry.registerInstance(originalInstrumentation, newArguments)

        val fakeScreenshot = FakeScreenshotHandler()
        val fakeHierarchy = FakeUiHierarchyHandler()

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Unspecified,
                        uiHierarchyCaptureMode = CaptureMode.Unspecified,
                    )
            )

        val runner =
            FailurePipelineRunner(
                config = config,
                screenshotHandler = fakeScreenshot,
                uiHierarchyHandler = fakeHierarchy,
            )

        assertThrows(AssertionError::class.java) {
            runner.runPipeline(AssertionError("Error"), emptySet())
        }

        assertEquals(1, fakeScreenshot.exportedFiles.size)
        assertEquals(0, fakeHierarchy.exportedFiles.size)
    }

    @Test
    fun nonTimeoutException_isNotWrapped() {
        val config = ComposeUiTestConfig(testTimeout = 5.seconds)
        val runner = FailurePipelineRunner(config = config)

        val original = IllegalStateException("Some other error")
        val thrown =
            assertThrows(IllegalStateException::class.java) {
                runner.runPipeline(original, emptySet())
            }

        assertSame(original, thrown)
    }

    @Test
    fun customFailureHandlerException_isSuppressedAndLogged_subsequentHandlersStillRun() {
        val firstHandlerException = RuntimeException("Handler 1 failed")
        val executedHandlers = mutableListOf<String>()

        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Disabled,
                        uiHierarchyCaptureMode = CaptureMode.Disabled,
                        failureHandlers =
                            listOf(
                                TestFailureHandler {
                                    executedHandlers.add("first")
                                    throw firstHandlerException
                                },
                                TestFailureHandler { executedHandlers.add("second") },
                            ),
                    )
            )

        val runner = FailurePipelineRunner(config = config)

        val rootError = AssertionError("Test error")
        val thrown =
            assertThrows(AssertionError::class.java) { runner.runPipeline(rootError, emptySet()) }

        assertSame(rootError, thrown)
        assertEquals(listOf("first", "second"), executedHandlers)
        assertTrue(thrown.suppressed.contains(firstHandlerException))
    }

    @Test
    fun uiHierarchy_emptyRoots_printsEmptyMessage() {
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

    @Test
    fun uiHierarchy_containsHeader() {
        val content = dumpHierarchyForTest {
            setContent { Box(Modifier.testTag("box")) }
            onNodeWithTag("non_existent").assertExists()
        }
        assertTrue(content.contains("View and Compose Hierarchy"))
    }

    @Test
    fun uiHierarchy_interleavesComposeUnderView() {
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
    fun uiHierarchy_multipleWindows_dumpsInOrder() {
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
    fun uiHierarchy_androidViewInCompose_dumpsInOrder() {
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
    fun uiHierarchy_composeInAndroidView_dumpsInOrder() {
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

    private class FakeScreenshotHandler(private val onExportCallback: (String) -> Unit = {}) :
        ScreenshotHandler {
        val exportedFiles = mutableListOf<String>()

        override fun export(fileName: String) {
            exportedFiles.add(fileName)
            onExportCallback(fileName)
        }
    }

    private class FakeUiHierarchyHandler(
        private val onExportCallback: (String, Set<ViewRootForTest>) -> Unit = { _, _ -> }
    ) : UiHierarchyHandler {
        val exportedFiles = mutableListOf<String>()
        val exportedRoots = mutableListOf<Set<ViewRootForTest>>()

        override fun export(fileName: String, roots: Set<ViewRootForTest>) {
            exportedFiles.add(fileName)
            exportedRoots.add(roots)
            onExportCallback(fileName, roots)
        }
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
