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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestFailurePolicy.CaptureMode
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorage
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FailurePipelineTest {

    private val originalInstrumentation = InstrumentationRegistry.getInstrumentation()
    private val originalArguments = InstrumentationRegistry.getArguments()
    private val originalStorage = PlatformTestStorageRegistry.getInstance()

    @After
    fun tearDown() {
        InstrumentationRegistry.registerInstance(originalInstrumentation, originalArguments)
        PlatformTestStorageRegistry.registerInstance(originalStorage)
    }

    @Test
    fun customHandler_isCalled_withArtifacts_onAssertionFailure() {
        var capturedArtifacts: List<FailureArtifact> = emptyList()
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers =
                            listOf(
                                TestFailureHandler { context ->
                                    capturedArtifacts = context.artifacts
                                }
                            ),
                    )
            )

        val error =
            assertThrows(AssertionError::class.java) {
                runComposeUiTest(config) {
                    setContent { Box(Modifier.testTag("box")) }
                    onNodeWithTag("non-existent").assertExists()
                }
            }

        assertEquals("Expected exactly 2 artifacts", 2, capturedArtifacts.size)
        assertTrue(capturedArtifacts.any { it.type == FailureArtifact.Type.Screenshot })
        assertTrue(capturedArtifacts.any { it.type == FailureArtifact.Type.UiHierarchy })

        assertTrue("Expected no file writing exceptions", error.suppressed.isEmpty())
    }

    @Test
    fun failureHandlersWriteBytesToStorage() {
        val memoryStorage = MemoryTestStorage()
        PlatformTestStorageRegistry.registerInstance(memoryStorage)

        var capturedArtifacts: List<FailureArtifact> = emptyList()
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers =
                            listOf(
                                TestFailureHandler { context ->
                                    capturedArtifacts = context.artifacts
                                }
                            ),
                    )
            )

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent { Box(Modifier.testTag("my_box")) }
                onNodeWithTag("non-existent").assertExists()
            }
        }

        val uiArtifact = capturedArtifacts.find { it.type == FailureArtifact.Type.UiHierarchy }
        val screenshotArtifact =
            capturedArtifacts.find { it.type == FailureArtifact.Type.Screenshot }

        requireNotNull(uiArtifact) { "UI Hierarchy artifact was not registered" }
        requireNotNull(screenshotArtifact) { "Screenshot artifact was not registered" }

        val uiBytes = memoryStorage.outputFiles[uiArtifact.fileName]
        requireNotNull(uiBytes) { "UI Hierarchy file was never written to storage" }
        val uiString = uiBytes.toString(Charsets.UTF_8.name())
        assertTrue(
            "Expected UI dump to contain 'View and Compose Hierarchy'",
            uiString.contains("View and Compose Hierarchy"),
        )
        assertTrue("Expected UI dump to contain the node tag", uiString.contains("my_box"))

        val screenshotBytes = memoryStorage.outputFiles[screenshotArtifact.fileName]
        requireNotNull(screenshotBytes) { "Screenshot file was never written to storage" }
        assertTrue(
            "Expected screenshot byte array to not be empty",
            screenshotBytes.toByteArray().isNotEmpty(),
        )
    }

    @Test
    fun readsGlobalArgumentsWhenUnspecified() {
        val newArguments =
            Bundle(originalArguments).apply {
                putString("androidx.compose.ui.test.failure.isScreenshotCaptureEnabled", "true")
                putString("androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled", "true")
            }
        InstrumentationRegistry.registerInstance(originalInstrumentation, newArguments)

        var capturedArtifacts: List<FailureArtifact> = emptyList()
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        failureHandlers =
                            listOf(
                                TestFailureHandler { context ->
                                    capturedArtifacts = context.artifacts
                                }
                            )
                    )
            )

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent { Box(Modifier) }
                onNodeWithTag("non-existent").assertExists()
            }
        }

        assertEquals(
            "Fallback arguments should have triggered both captures",
            2,
            capturedArtifacts.size,
        )
    }

    @Test
    fun captureDisabled_overridesSuiteLevelArguments() {
        val newArguments =
            Bundle(originalArguments).apply {
                putString("androidx.compose.ui.test.failure.isScreenshotCaptureEnabled", "true")
                putString("androidx.compose.ui.test.failure.isUiHierarchyCaptureEnabled", "true")
            }
        InstrumentationRegistry.registerInstance(originalInstrumentation, newArguments)

        var capturedArtifacts: List<FailureArtifact>? = null
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Disabled,
                        uiHierarchyCaptureMode = CaptureMode.Disabled,
                        failureHandlers =
                            listOf(
                                TestFailureHandler { context ->
                                    capturedArtifacts = context.artifacts
                                }
                            ),
                    )
            )

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent { Box(Modifier) }
                onNodeWithTag("non-existent").assertExists()
            }
        }

        requireNotNull(capturedArtifacts)
        assertTrue(
            "Expected 0 artifacts because captures were explicitly disabled",
            capturedArtifacts.isEmpty(),
        )
    }

    @Test
    fun captureDisabled_failureHandlerReceivesNoArtifacts() {
        var capturedArtifacts: List<FailureArtifact>? = null
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Disabled,
                        uiHierarchyCaptureMode = CaptureMode.Disabled,
                        failureHandlers =
                            listOf(
                                TestFailureHandler { context ->
                                    capturedArtifacts = context.artifacts
                                }
                            ),
                    )
            )

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent { Box(Modifier.testTag("box")) }
                onNodeWithTag("non-existent").assertExists()
            }
        }

        requireNotNull(capturedArtifacts)
        assertTrue(
            "Expected failure handler to receive empty artifacts list when captures are disabled",
            capturedArtifacts.isEmpty(),
        )
    }

    @Test
    fun customHandlerException_isSuppressed() {
        val handlerException = RuntimeException("Handler failed!")
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        failureHandlers = listOf(TestFailureHandler { throw handlerException })
                    )
            )

        val error =
            assertThrows(AssertionError::class.java) {
                runComposeUiTest(config) {
                    setContent { Box(Modifier) }
                    onNodeWithTag("non-existent").assertExists()
                }
            }

        val suppressed = error.suppressed
        assertTrue("Handler exception was not suppressed", suppressed.contains(handlerException))
    }

    @Test
    fun multipleHandlers_areCalledInOrder() {
        val calls = mutableListOf<String>()
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        failureHandlers =
                            listOf(
                                TestFailureHandler { calls.add("first") },
                                TestFailureHandler { calls.add("second") },
                                TestFailureHandler { calls.add("third") },
                            )
                    )
            )

        assertThrows(AssertionError::class.java) {
            runComposeUiTest(config) {
                setContent { Box(Modifier) }
                onNodeWithTag("non-existent").assertExists()
            }
        }

        assertEquals(listOf("first", "second", "third"), calls)
    }

    @Test
    fun artifacts_arePopulatedInContext() {
        var capturedArtifacts: List<FailureArtifact> = emptyList()
        val config =
            ComposeUiTestConfig(
                failurePolicy =
                    TestFailurePolicy(
                        screenshotCaptureMode = CaptureMode.Enabled,
                        uiHierarchyCaptureMode = CaptureMode.Enabled,
                        failureHandlers =
                            listOf(
                                TestFailureHandler { context ->
                                    capturedArtifacts = context.artifacts
                                }
                            ),
                    )
            )

        val error =
            assertThrows(AssertionError::class.java) {
                runComposeUiTest(config) {
                    setContent { Box(Modifier) }
                    onNodeWithTag("non-existent").assertExists()
                }
            }

        assertEquals(2, capturedArtifacts.size)

        val screenshotArtifact =
            capturedArtifacts.find { it.type == FailureArtifact.Type.Screenshot }
        assertTrue(screenshotArtifact != null)
        assertTrue(screenshotArtifact!!.fileName.endsWith("_screenshot.png"))

        val uiArtifact = capturedArtifacts.find { it.type == FailureArtifact.Type.UiHierarchy }
        assertTrue(uiArtifact != null)
        assertTrue(uiArtifact!!.fileName.endsWith("_ui.txt"))

        assertTrue("Expected no file writing exceptions", error.suppressed.isEmpty())
    }

    @Test
    fun noPolicyAndNoInstrumentationArgs_propagatesOriginalErrorWithEmptySuppressed() {
        val emptyArguments = Bundle()
        InstrumentationRegistry.registerInstance(originalInstrumentation, emptyArguments)

        val originalError = AssertionError("Original test failure")
        val error =
            assertThrows(AssertionError::class.java) { runComposeUiTest { throw originalError } }

        assertSame("Expected the exact original error instance", originalError, error)
        assertTrue("Expected empty suppressed list", error.suppressed.isEmpty())
    }

    @Test
    fun uncompletedCoroutinesError_isWrappedIntoAndroidComposeUiTestTimeoutException() {
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

    @Test
    fun failureContext_publicConstructor() {
        val testError = AssertionError("Unit test root error")
        val testArtifacts =
            listOf(FailureArtifact(FailureArtifact.Type.Screenshot, "test_screenshot.png"))

        val contextWithArtifacts = FailureContext(error = testError, artifacts = testArtifacts)
        assertSame(testError, contextWithArtifacts.error)
        assertEquals(testArtifacts, contextWithArtifacts.artifacts)

        val contextDefault = FailureContext(error = testError)
        assertSame(testError, contextDefault.error)
        assertTrue(contextDefault.artifacts.isEmpty())
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
