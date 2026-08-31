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

package androidx.compose.ui.test

import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestFailurePolicy.CaptureMode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorage
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FailurePipelineTest {

    private val originalInstrumentation = InstrumentationRegistry.getInstrumentation()
    private val originalArguments = InstrumentationRegistry.getArguments()
    private val originalStorage = PlatformTestStorageRegistry.getInstance()
    private val memoryStorage = MemoryTestStorage()

    @Before
    fun setUp() {
        PlatformTestStorageRegistry.registerInstance(memoryStorage)
        val newArguments =
            Bundle(originalArguments).apply {
                putString("androidx.compose.ui.test.failure.isScreenshotCaptureEnabled", "true")
                putString("androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled", "true")
            }
        InstrumentationRegistry.registerInstance(originalInstrumentation, newArguments)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.registerInstance(originalInstrumentation, originalArguments)
        PlatformTestStorageRegistry.registerInstance(originalStorage)
    }

    @Test
    fun customHandler_isCalled_withArtifacts_onAssertionFailure() {
        var capturedArtifacts: List<FailureArtifact> = emptyList()
        val config = createCapturingConfig { capturedArtifacts = it }

        val error =
            assertThrows(AssertionError::class.java) {
                runComposeUiTest(config) {
                    setContent { Box(Modifier.testTag("my_box")) }
                    onNodeWithTag("non_existent").assertExists()
                }
            }

        assertTrue("Expected no suppressed exceptions", error.suppressed.isEmpty())
        assertEquals("Expected exactly 2 artifacts", 2, capturedArtifacts.size)
        assertTrue(
            capturedArtifacts.any {
                it.type == FailureArtifact.Type.UiHierarchy && it.fileName.endsWith("_ui.txt")
            }
        )
        assertTrue(
            capturedArtifacts.any {
                it.type == FailureArtifact.Type.Screenshot &&
                    it.fileName.endsWith("_screenshot.png")
            }
        )

        assertArtifactsCaptured(expectedTag = "my_box")
    }

    @Test
    fun uncompletedCoroutinesError_isWrappedIntoTimeoutException() {
        val config = ComposeUiTestConfig(testTimeout = 10.milliseconds)
        val error =
            assertThrows(AndroidComposeUiTestTimeoutException::class.java) {
                runComposeUiTest(config) {
                    withContext(Dispatchers.Default) { delay(1000.milliseconds) }
                }
            }

        assertTrue(
            "Expected error message to mention testTimeout",
            error.message?.contains("testTimeout") == true,
        )
        assertEquals(
            "Expected original UncompletedCoroutinesError as cause",
            "kotlinx.coroutines.test.UncompletedCoroutinesError",
            error.cause?.javaClass?.name,
        )
        assertTrue("Expected suppressed exceptions list to be empty", error.suppressed.isEmpty())
    }

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun capturesArtifactsOnFailureWithComponentActivity() {
        assertThrows(AssertionError::class.java) {
            runAndroidComposeUiTest(ComponentActivity::class.java) {
                setContent { Box(Modifier.testTag("component_activity_box")) }
                onNodeWithTag("non_existent").assertExists()
            }
        }

        assertArtifactsCaptured(expectedTag = "component_activity_box")
    }

    @Test
    fun capturesArtifactsAndHierarchyOnFailureWithCustomActivity() {
        val config = createCapturingConfig()

        assertThrows(AssertionError::class.java) {
            androidx.compose.ui.test.v2.runAndroidComposeUiTest(
                activityClass = CustomComposeHostActivity::class.java,
                config = config,
            ) {
                val hostActivity = requireNotNull(activity)
                runOnUiThread {
                    hostActivity.setContent { Box(Modifier.testTag("custom_activity_box")) }
                }
                waitForIdle()
                onNodeWithTag("non_existent").assertExists()
            }
        }

        assertArtifactsCaptured(expectedTag = "custom_activity_box", expectedLayout = "FrameLayout")
    }

    @Test
    fun capturesArtifactsOnUncaughtCoroutineExceptionWithCustomActivity() {
        val config = createCapturingConfig()

        assertThrows(IllegalStateException::class.java) {
            androidx.compose.ui.test.v2.runAndroidComposeUiTest(
                activityClass = CustomComposeHostActivity::class.java,
                config = config,
            ) {
                val hostActivity = requireNotNull(activity)
                runOnUiThread {
                    hostActivity.setContent { Box(Modifier.testTag("coroutine_error_box")) }
                }
                waitForIdle()
                CoroutineScope(Dispatchers.Main).launch {
                    throw IllegalStateException("Uncaught exception in coroutine")
                }
            }
        }

        assertArtifactsCaptured(expectedTag = "coroutine_error_box")
    }

    @Test
    fun capturesArtifactsOnRecompositionExceptionWithCustomActivity() {
        val config = createCapturingConfig()

        assertThrows(IllegalStateException::class.java) {
            androidx.compose.ui.test.v2.runAndroidComposeUiTest(
                activityClass = CustomComposeHostActivity::class.java,
                config = config,
            ) {
                val hostActivity = requireNotNull(activity)
                var state by mutableIntStateOf(0)
                runOnUiThread {
                    hostActivity.setContent {
                        if (state == 1) {
                            throw IllegalStateException("Recomposition failure")
                        }
                        Button(
                            onClick = { state = 1 },
                            modifier = Modifier.testTag("throw_button"),
                        ) {
                            Text("Click to fail")
                        }
                    }
                }
                waitForIdle()
                onNodeWithTag("throw_button").performClick()
                waitForIdle()
            }
        }

        assertArtifactsCaptured(expectedTag = "throw_button")
    }

    @Test
    fun capturesArtifactsWithMultipleWindows_dialogAndPopup() {
        val config = createCapturingConfig()

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent {
                    Box(Modifier.testTag("main_window_box")) {
                        Dialog(onDismissRequest = {}) {
                            Box(Modifier.testTag("dialog_window_box")) {
                                Popup { Box(Modifier.testTag("popup_window_box")) }
                            }
                        }
                    }
                }
                waitForIdle()
                onNodeWithTag("non_existent").assertExists()
            }
        }

        assertArtifactsCaptured(expectedTag = "main_window_box")
        val uiDump = getUiDump()
        assertTrue(
            "Expected UI dump to contain 'dialog_window_box'",
            uiDump.contains("dialog_window_box"),
        )
        assertTrue(
            "Expected UI dump to contain 'popup_window_box'",
            uiDump.contains("popup_window_box"),
        )
    }

    @Test
    fun capturesArtifactsWithInteropHierarchy() {
        val config = createCapturingConfig()

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent {
                    Box(Modifier.testTag("parent_compose_box")) {
                        AndroidView(
                            factory = { context ->
                                FrameLayout(context).apply {
                                    addView(TextView(context).apply { text = "Interop TextView" })
                                    addView(
                                        ComposeView(context).apply {
                                            setContent {
                                                Box(Modifier.testTag("nested_compose_box"))
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                waitForIdle()
                onNodeWithTag("non_existent").assertExists()
            }
        }

        assertArtifactsCaptured(expectedTag = "parent_compose_box", expectedLayout = "FrameLayout")
        val uiDump = getUiDump()
        assertTrue("Expected UI dump to contain 'TextView'", uiDump.contains("TextView"))
        assertTrue("Expected UI dump to contain 'ComposeView'", uiDump.contains("ComposeView"))
        assertTrue(
            "Expected UI dump to contain 'nested_compose_box'",
            uiDump.contains("nested_compose_box"),
        )
    }

    private fun createCapturingConfig(
        onArtifacts: (List<FailureArtifact>) -> Unit = {}
    ): ComposeUiTestConfig =
        ComposeUiTestConfig(
            failurePolicy =
                TestFailurePolicy(
                    screenshotCaptureMode = CaptureMode.Enabled,
                    uiHierarchyCaptureMode = CaptureMode.Enabled,
                    failureHandlers = listOf(TestFailureHandler { onArtifacts(it.artifacts) }),
                )
        )

    private fun getUiDump(): String {
        val uiFile = memoryStorage.outputFiles.entries.find { it.key.endsWith("_ui.txt") }
        requireNotNull(uiFile) { "UI Hierarchy file was not written to storage" }
        return uiFile.value.toString(Charsets.UTF_8.name())
    }

    private fun assertArtifactsCaptured(
        expectedTag: String? = null,
        expectedLayout: String? = null,
    ) {
        val screenshotFile =
            memoryStorage.outputFiles.entries.find { it.key.endsWith("_screenshot.png") }

        requireNotNull(screenshotFile) { "Screenshot file was not written to storage" }
        assertTrue(
            "Expected screenshot byte array to not be empty",
            screenshotFile.value.toByteArray().isNotEmpty(),
        )

        val uiString = getUiDump()
        assertTrue(
            "Expected UI dump to contain 'View and Compose Hierarchy'",
            uiString.contains("View and Compose Hierarchy"),
        )
        if (expectedTag != null) {
            assertTrue(
                "Expected UI dump to contain tag '$expectedTag'",
                uiString.contains(expectedTag),
            )
        }
        if (expectedLayout != null) {
            assertTrue(
                "Expected UI dump to contain layout '$expectedLayout'",
                uiString.contains(expectedLayout),
            )
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
