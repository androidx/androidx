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

package androidx.privacysandbox.ui.tests.endtoend

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.privacysandbox.ui.client.SharedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SharedUiContainer
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.tests.util.TestSessionManager
import androidx.privacysandbox.ui.tests.util.TestSharedUiAdapter
import androidx.privacysandbox.ui.tests.util.TestSharedUiSessionClient
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// TODO(b/263460954): Once event change listener is implemented for SharedUiContainer,
// onSessionError behavior should be tested.
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
@RunWith(Parameterized::class)
@MediumTest
class SharedSessionIntegrationTests(private val invokeBackwardsCompatFlow: Boolean) {
    companion object {
        const val TIMEOUT = 1000L
        const val CONTAINER_WIDTH = 100
        const val CONTAINER_HEIGHT = 100

        @JvmStatic
        @Parameterized.Parameters(name = "invokeBackwardsCompatFlow={0}")
        fun data(): Array<Any> = arrayOf(arrayOf(true), arrayOf(false))
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var linearLayout: LinearLayout
    private lateinit var sharedUiContainer: SharedUiContainer
    private lateinit var sessionManager: TestSessionManager

    @get:Rule var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        if (!invokeBackwardsCompatFlow) {
            // Device needs to support remote provider to invoke non-backward-compat flow.
            assumeTrue(BackwardCompatUtil.canProviderBeRemote())
        }
        sessionManager = TestSessionManager(context, invokeBackwardsCompatFlow)

        activityScenarioRule.withActivity {
            linearLayout =
                LinearLayout(this).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                        )
                }
            setContentView(linearLayout)

            sharedUiContainer =
                SharedUiContainer(this).apply {
                    layoutParams = ViewGroup.LayoutParams(CONTAINER_WIDTH, CONTAINER_HEIGHT)
                }
            linearLayout.addView(sharedUiContainer)
        }
    }

    @Test
    fun testBinderAdapter_notReWrapped() {
        val adapter = TestSharedUiAdapter()
        val binderAdapter = sessionManager.getCoreLibInfoFromSharedUiAdapter(adapter)
        val adapterFromCoreLibInfo = SharedUiAdapterFactory.createFromCoreLibInfo(binderAdapter)
        // send this back to the SDK and see if the same binder is sent back to the app.
        val binderAdapter2 =
            sessionManager.getCoreLibInfoFromSharedUiAdapter(adapterFromCoreLibInfo)
        assertThat(binderAdapter).isEqualTo(binderAdapter2)
    }

    @Test
    fun testOpenSession_fromAdapter() {
        val client = TestSharedUiSessionClient()

        val adapter =
            sessionManager.createSharedUiAdapterAndEstablishSession(
                testSharedSessionClient = client
            )

        assertThat(adapter.session).isNotNull()
        assertThat(client.isSessionOpened).isTrue()
    }

    @Test
    fun testOpenSession_onSetAdapter() {
        val adapter =
            sessionManager.createSharedUiAdapterAndEstablishSession(
                sharedUiContainer = sharedUiContainer
            )

        assertThat(adapter.session).isNotNull()
    }

    @Test
    fun testSessionError() {
        val client = TestSharedUiSessionClient()

        sessionManager.createSharedUiAdapterAndEstablishSession(
            testSharedSessionClient = client,
            isFailingSession = true,
        )

        assertThat(client.isSessionErrorCalled).isTrue()
    }

    @Test
    fun testCloseSession() {
        val client = TestSharedUiSessionClient()
        val adapter =
            sessionManager.createSharedUiAdapterAndEstablishSession(
                testSharedSessionClient = client
            )

        client.closeClient()

        assertThat(client.isClientClosed).isTrue()
        assertWithMessage("close is called on Session").that(adapter.isCloseSessionCalled).isTrue()
    }

    @Test
    fun testSessionClientProxy_methodsOnObjectClass() {
        // Only makes sense when a dynamic proxy is involved in the flow
        assumeTrue(invokeBackwardsCompatFlow)
        val testSharedUiSessionClient = TestSharedUiSessionClient()

        val sdkAdapter =
            sessionManager.createSharedUiAdapterAndEstablishSession(
                testSharedSessionClient = testSharedUiSessionClient
            )

        // Verify toString, hashCode and equals have been implemented for dynamic proxy
        val testSession = sdkAdapter.session as TestSharedUiAdapter.TestSession
        val client = testSession.sessionClient

        assertThat(client.toString()).contains(testSharedUiSessionClient.toString())
        assertThat(client.equals(client)).isTrue()
        assertThat(client).isNotEqualTo(testSharedUiSessionClient)
        assertThat(client.hashCode()).isEqualTo(client.hashCode())
    }
}
