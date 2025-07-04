/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.tests.endtoend

import android.content.Context
import android.widget.LinearLayout
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.DelegatingSandboxedUiAdapter
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.integration.testingutils.TestEventListener
import androidx.privacysandbox.ui.tests.util.TestSessionManager
import androidx.privacysandbox.ui.tests.util.TestSessionManager.TestDelegatingAdapterWithDelegate
import androidx.privacysandbox.ui.tests.util.TestSessionManager.TestSandboxedUiAdapter
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@MediumTest
@OptIn(ExperimentalFeatures.DelegatingAdapterApi::class)
class DelegatingAdapterIntegrationTests(invokeBackwardsCompatFlow: Boolean) {

    @get:Rule val rule = IntegrationTestSetupRule(invokeBackwardsCompatFlow)

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "invokeBackwardsCompatFlow={0}")
        fun data(): Array<Any> = arrayOf(arrayOf(true), arrayOf(false))
    }

    private lateinit var context: Context
    private lateinit var view: SandboxedSdkView
    private lateinit var eventListener: TestEventListener
    private lateinit var linearLayout: LinearLayout
    private lateinit var sessionManager: TestSessionManager
    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setup() {
        context = rule.context
        view = rule.view
        eventListener = rule.eventListener
        linearLayout = rule.linearLayout
        sessionManager = rule.sessionManager
        activityScenario = rule.activityScenario
    }

    @Test
    fun testRefreshDelegate() {
        val testDelegatingAdapterWrapper =
            sessionManager.createDelegatingAdapterAndEstablishSession(viewForSession = view)
        val delegate = testDelegatingAdapterWrapper.delegate
        assertThat(delegate.session).isNotNull()
        runBlocking {
            val adapterWithData =
                createAndUpdateDelegate(testDelegatingAdapterWrapper.delegatingAdapter)
            assertThat(adapterWithData.delegate.session).isNotNull()
            assertThat(delegate.session).isNull()
        }
    }

    @Test
    fun testConsecutiveRefreshWithFailedDelegate() {
        val testDelegatingAdapterWrapper =
            sessionManager.createDelegatingAdapterAndEstablishSession(viewForSession = view)
        val delegate = testDelegatingAdapterWrapper.delegate
        assertThat(delegate.session).isNotNull()
        var adapterWithData: TestDelegatingAdapterWithDelegate?
        var hasThrown = false
        runBlocking {
            try {
                adapterWithData =
                    createAndUpdateDelegate(
                        testDelegatingAdapterWrapper.delegatingAdapter,
                        failSessionCreation = true,
                    )
            } catch (e: IllegalStateException) {
                hasThrown = true
                // check that the old session is still open
                assertThat(delegate.session).isNotNull()

                adapterWithData =
                    createAndUpdateDelegate(
                        testDelegatingAdapterWrapper.delegatingAdapter,
                        failSessionCreation = false,
                    )
                // a new session with the new delegate is established
                assertThat(adapterWithData!!.delegate.session).isNotNull()
                // assert that the old session is closed
                assertThat(delegate.session).isNull()
            }
            assertTrue(hasThrown)
        }
    }

    private suspend fun createAndUpdateDelegate(
        delegatingAdapter: DelegatingSandboxedUiAdapter,
        failSessionCreation: Boolean = false,
        placeViewInsideFrameLayout: Boolean = false,
    ): TestDelegatingAdapterWithDelegate {
        lateinit var delegate: TestSandboxedUiAdapter
        activityScenario.onActivity {
            delegate =
                TestSandboxedUiAdapter(false, placeViewInsideFrameLayout, failSessionCreation)
        }
        val delegateBundle = sessionManager.getCoreLibInfoFromSharedUiAdapter(delegate)
        delegatingAdapter.updateDelegate(delegateBundle)
        return TestDelegatingAdapterWithDelegate(delegatingAdapter, delegate)
    }
}
