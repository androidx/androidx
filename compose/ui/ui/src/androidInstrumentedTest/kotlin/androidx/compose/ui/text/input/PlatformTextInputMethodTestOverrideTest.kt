/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformTextInputModifierNode
import androidx.compose.ui.platform.PlatformTextInputSession
import androidx.compose.ui.platform.establishTextInputSession
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.PlatformTextInputMethodTestOverride
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.fail
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.runner.RunWith

@Suppress("DEPRECATION")
@OptIn(ExperimentalTestApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class PlatformTextInputMethodTestOverrideTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var testNode: TestNode
    private lateinit var hostView: View

    @Test
    fun overrideHandlesInputMethod() = runTest {
        var lastRequest: PlatformTextInputMethodRequest? = null
        var lastContinuation: CancellableContinuation<Nothing>? = null
        val testRequest = PlatformTextInputMethodRequest { fail("not needed") }
        val coroutineName = "this is a test"

        val testHandler =
            object : PlatformTextInputSession {
                override val view: View
                    get() = fail("not needed")

                override suspend fun startInputMethod(
                    request: PlatformTextInputMethodRequest
                ): Nothing {
                    lastRequest = request
                    suspendCancellableCoroutine<Nothing> { lastContinuation = it }
                }
            }
        setContent(testHandler)

        val testJob =
            rule.runOnIdle {
                launch {
                    testNode.establishTextInputSession {
                        // This context should be propagated to startInputMethod.
                        withContext(CoroutineName(coroutineName)) { startInputMethod(testRequest) }
                    }
                }
            }
        // Let the session start.
        testScheduler.advanceUntilIdle()

        assertThat(lastRequest).isSameInstanceAs(testRequest)
        assertThat(lastContinuation!!.context[CoroutineName]?.name).isEqualTo(coroutineName)
        testJob.cancel()
    }

    @Test
    fun overrideBlocksSystemHandler() = runTest {
        val testRequest = PlatformTextInputMethodRequest { fail("not needed") }

        val testHandler =
            object : PlatformTextInputSession {
                override val view: View
                    get() = fail("not needed")

                override suspend fun startInputMethod(
                    request: PlatformTextInputMethodRequest
                ): Nothing = awaitCancellation()
            }
        setContent(testHandler)

        val testJob =
            rule.runOnIdle {
                launch { testNode.establishTextInputSession { startInputMethod(testRequest) } }
            }
        // Let the session start.
        testScheduler.advanceUntilIdle()

        assertFailsWith<ComposeTimeoutException> {
            // This should never return true.
            rule.waitUntil { hostView.onCheckIsTextEditor() }
        }
        testJob.cancel()
    }

    private fun setContent(testHandler: PlatformTextInputSession) {
        rule.setContent {
            hostView = LocalView.current
            PlatformTextInputMethodTestOverride(sessionHandler = testHandler) {
                Box(Modifier.size(1.dp).then(TestElement { testNode = it }))
            }
        }
    }

    private data class TestElement(val onNode: (TestNode) -> Unit) :
        ModifierNodeElement<TestNode>() {
        override fun create(): TestNode = TestNode(onNode)

        override fun update(node: TestNode) {
            node.onNode = onNode
        }
    }

    private class TestNode(var onNode: (TestNode) -> Unit) :
        Modifier.Node(), PlatformTextInputModifierNode {

        override fun onAttach() {
            onNode(this)
        }
    }
}
