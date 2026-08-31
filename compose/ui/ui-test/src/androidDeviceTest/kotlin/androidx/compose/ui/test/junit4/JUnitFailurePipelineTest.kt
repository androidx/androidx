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

package androidx.compose.ui.test.junit4

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.CustomComposeHostActivity
import androidx.compose.ui.test.TestFailurePolicy
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.io.PlatformTestStorage
import androidx.test.platform.io.PlatformTestStorageRegistry
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement

@RunWith(Parameterized::class)
class JUnitFailurePipelineTest(activityClass: Class<out ComponentActivity>) {

    private val memoryStorage = MemoryTestStorage()

    private val config =
        ComposeUiTestConfig(
            failurePolicy =
                TestFailurePolicy(
                    screenshotCaptureMode = TestFailurePolicy.CaptureMode.Enabled,
                    uiHierarchyCaptureMode = TestFailurePolicy.CaptureMode.Enabled,
                )
        )

    private val composeTestRule = createAndroidComposeRule(activityClass, config)

    private val artifactAssertionRule =
        ArtifactAssertionRule(memoryStorage) { error ->
            val uiFile = memoryStorage.outputFiles.entries.find { it.key.endsWith("_ui.txt") }
            val screenshotFile =
                memoryStorage.outputFiles.entries.find { it.key.endsWith("_screenshot.png") }

            requireNotNull(uiFile) {
                "UI Hierarchy file was not written to storage before ActivityScenario closed. " +
                    "Root error: $error"
            }
            requireNotNull(screenshotFile) {
                "Screenshot was not captured before ActivityScenario closed. Root error: $error"
            }

            val uiString = uiFile.value.toString(Charsets.UTF_8.name())
            assertTrue(
                "Expected UI dump to contain 'View and Compose Hierarchy'",
                uiString.contains("View and Compose Hierarchy"),
            )
            assertTrue("Expected UI dump to contain test tag", uiString.contains("rule_test_box"))

            assertTrue(
                "Expected screenshot bytes to be non-empty",
                screenshotFile.value.toByteArray().isNotEmpty(),
            )
        }

    @get:Rule
    val testRuleChain: TestRule =
        RuleChain.emptyRuleChain().around(artifactAssertionRule).around(composeTestRule)

    @Test
    fun assertionFailure_capturesArtifactsBeforeActivityCloses() {
        setContent { Box(Modifier.testTag("rule_test_box")) }
        composeTestRule.onNodeWithTag("non_existent").assertExists()
    }

    @Test
    fun uncaughtCoroutineException_capturesArtifactsBeforeActivityCloses() {
        setContent {
            Box(Modifier.testTag("rule_test_box"))
            LaunchedEffect(Unit) {
                throw IllegalStateException("Uncaught exception in LaunchedEffect")
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun recompositionException_capturesArtifactsBeforeActivityCloses() {
        var state by mutableIntStateOf(0)
        setContent {
            Box(Modifier.testTag("rule_test_box")) {
                if (state == 1) {
                    throw IllegalStateException("Recomposition failure")
                }
                Button(onClick = { state = 1 }, modifier = Modifier.testTag("throw_button")) {
                    Text("Click to fail")
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("throw_button").performClick()
        composeTestRule.waitForIdle()
    }

    private fun setContent(content: @Composable () -> Unit) {
        when (val activity = composeTestRule.activity) {
            is CustomComposeHostActivity ->
                composeTestRule.runOnUiThread { activity.setContent(content) }
            else -> composeTestRule.setContent(content)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet() =
            listOf(ComponentActivity::class.java, CustomComposeHostActivity::class.java)
    }

    private class ArtifactAssertionRule(
        private val memoryStorage: MemoryTestStorage,
        private val onTestFailed: (Throwable) -> Unit,
    ) : TestRule {
        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                override fun evaluate() {
                    val originalStorage = PlatformTestStorageRegistry.getInstance()
                    PlatformTestStorageRegistry.registerInstance(memoryStorage)
                    try {
                        base.evaluate()
                    } catch (t: Throwable) {
                        onTestFailed(t)
                        return
                    } finally {
                        PlatformTestStorageRegistry.registerInstance(originalStorage)
                    }
                    fail("Expected test to fail, but no exception was thrown")
                }
            }
        }
    }

    private class MemoryTestStorage : PlatformTestStorage {
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
