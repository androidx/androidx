/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.testutils.expectError
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.AndroidComposeUiTestEnvironment
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class SynchronizationMethodsTest {

    private val environment =
        AndroidComposeUiTestEnvironment<ComponentActivity> {
            throw NotImplementedError("This test shouldn't use the Activity")
        }
    private val composeRootRegistry = environment.composeRootRegistry
    private val test = environment.test

    @get:Rule val testName = TestName()

    @get:Rule
    val registryRule: TestRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                composeRootRegistry.withRegistry { base.evaluate() }
            }
        }
    }

    @Before
    fun addResumedComposeRootMock() {
        composeRootRegistry.registerComposeRoot(mockResumedComposeRoot())
    }

    @Test
    fun runOnUiThread() {
        val result = test.runOnUiThread { "Hello" }
        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun runOnUiThread_void() {
        var called = false
        test.runOnUiThread { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun runOnUiThread_nullable() {
        val result: String? = test.runOnUiThread { null }
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun runOnUiThread_exception_rethrownWithCallersStackTrace() {
        var errorThrown = false
        val expectedError = CustomException("test")
        try {
            test.runOnUiThread<Unit> { throw expectedError }
        } catch (t: Throwable) {
            errorThrown = true
            assertWithMessage("caught error should be the original error")
                .that(t)
                .isSameInstanceAs(expectedError)
            assertWithMessage("test thread's stacktrace should be added as a suppressed exception")
                .that(
                    t.suppressedExceptions.first().stackTrace.any {
                        testName.methodName in it.methodName
                    }
                )
                .isTrue()
        }
        assertWithMessage("error on the UI thread was not propagated by runOnUiThread")
            .that(errorThrown)
            .isEqualTo(true)
    }

    @Test
    fun runOnIdle() {
        val result = test.runOnIdle { "Hello" }
        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun runOnIdle_void() {
        var called = false
        test.runOnIdle { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun runOnIdle_nullable() {
        val result: String? = test.runOnIdle { null }
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun runOnIdle_assert_fails() {
        test.runOnIdle {
            expectError<IllegalStateException> {
                test.onNode(hasTestTag("placeholder")).assertExists()
            }
        }
    }

    @Test
    fun runOnIdle_waitForIdle_fails() {
        test.runOnIdle { expectError<IllegalStateException> { test.waitForIdle() } }
    }

    @Test
    fun runOnIdle_runOnIdle_fails() {
        test.runOnIdle { expectError<IllegalStateException> { test.runOnIdle {} } }
    }

    private fun mockResumedComposeRoot(): ViewRootForTest {
        val composeRoot = mock<ViewRootForTest>()
        doReturn(true).whenever(composeRoot).isLifecycleInResumedState
        doReturn(mockRootView()).whenever(composeRoot).view
        return composeRoot
    }

    private fun mockRootView(): View {
        val rootView = mock<View>()
        doReturn(false).whenever(rootView).isAttachedToWindow
        doReturn(rootView).whenever(rootView).rootView
        return rootView
    }
}

private class CustomException(message: String?) : Exception(message)
