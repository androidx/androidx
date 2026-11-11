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

package androidx.xr.testutils

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AssumptionViolatedException
import org.junit.Before
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class XrSessionRuleTest {

    private class CustomTestActivity : ComponentActivity()

    private class TrackingActivityRule(private val activity: CustomTestActivity) : TestRule {
        var applied: Boolean = false

        override fun apply(base: Statement, description: Description): Statement =
            object : Statement() {
                override fun evaluate() {
                    applied = true
                    base.evaluate()
                }
            }

        fun getActivity(): CustomTestActivity = activity
    }

    @Before
    fun setUp() {
        setXrSystemFeature(false)
    }

    @After
    fun tearDown() {
        setXrSystemFeature(false)
    }

    private fun setXrSystemFeature(enabled: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager).setSystemFeature(FEATURE_XR_API_SPATIAL, enabled)
    }

    @Suppress("DEPRECATION")
    private fun createFakeRule():
        Triple<
            XrSessionRule<TrackingActivityRule, CustomTestActivity>,
            CustomTestActivity,
            Session,
        > {
        val (activity, session) =
            runBlocking(Dispatchers.Main.immediate) {
                val builtActivity =
                    Robolectric.buildActivity(CustomTestActivity::class.java).setup().get()
                val builtSession =
                    Session(
                        activity = builtActivity,
                        stateExtenders = emptyList(),
                        sessionConnectors = emptyList(),
                        runtimes = emptyList(),
                    )
                builtActivity to builtSession
            }
        val trackingRule = TrackingActivityRule(activity)
        val rule =
            XrSessionRule(
                activityRule = trackingRule,
                sessionFactory = { SessionCreateSuccess(session) },
                activityProvider = { it.getActivity() },
            )
        return Triple(rule, activity, session)
    }

    private fun XrSessionRule<*, *>.evaluateRule(
        testName: String = "testBody",
        body: () -> Unit,
    ) {
        val description = Description.createTestDescription(XrSessionRuleTest::class.java, testName)
        apply(
                object : Statement() {
                    override fun evaluate() = body()
                },
                description,
            )
            .evaluate()
    }

    @Test
    fun factories_constructConfiguredRule() {
        val defaultRule: XrSessionRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
            createXrSessionRule()
        assertNotNull(defaultRule.activityRule)

        val reifiedRule:
            XrSessionRule<ActivityScenarioRule<CustomTestActivity>, CustomTestActivity> =
            createAndroidXrSessionRule<CustomTestActivity>()
        assertNotNull(reifiedRule.activityRule)

        val classRule: XrSessionRule<ActivityScenarioRule<CustomTestActivity>, CustomTestActivity> =
            createAndroidXrSessionRule(CustomTestActivity::class.java)
        assertNotNull(classRule.activityRule)

        val scenarioRule = ActivityScenarioRule(CustomTestActivity::class.java)
        val wrappedRule = createAndroidXrSessionRule(scenarioRule)
        assertSame(scenarioRule, wrappedRule.activityRule)
    }

    @Test
    fun apply_skipsWhenXrFeatureMissingBeforeApplyingActivityRule() {
        setXrSystemFeature(false)
        val activity =
            runBlocking(Dispatchers.Main.immediate) {
                Robolectric.buildActivity(CustomTestActivity::class.java).setup().get()
            }
        val trackingRule = TrackingActivityRule(activity)
        val rule = XrSessionRule(trackingRule) { it.getActivity() }
        val bodyExecuted = AtomicBoolean(false)

        assertFailsWith<AssumptionViolatedException> {
            rule.evaluateRule("skipNonXrTest") { bodyExecuted.set(true) }
        }

        assertFalse(trackingRule.applied)
        assertFalse(bodyExecuted.get())
    }

    @Test
    fun accessorsOutsideRunningTest_throwIllegalStateException() {
        setXrSystemFeature(true)
        val (rule, expectedActivity, expectedSession) = createFakeRule()

        assertEquals(
            "XrSessionRule.activity is only available while a test is running.",
            assertFailsWith<IllegalStateException> { rule.activity }.message,
        )
        assertEquals(
            "XrSessionRule.session is only available while a test is running.",
            assertFailsWith<IllegalStateException> { rule.session }.message,
        )
        assertEquals(
            "XrSessionRule.onMainThread is only available while a test is running.",
            assertFailsWith<IllegalStateException> { rule.onMainThread {} }.message,
        )
        assertFailsWith<IllegalStateException> { rule.captureToBitmap() }

        rule.evaluateRule {
            assertSame(expectedActivity, rule.activity)
            assertSame(expectedSession, rule.session)
        }

        // After evaluation finishes, all active-test guards throw again.
        assertFailsWith<IllegalStateException> { rule.activity }
        assertFailsWith<IllegalStateException> { rule.session }
        assertFailsWith<IllegalStateException> { rule.onMainThread {} }
        assertFailsWith<IllegalStateException> { rule.captureToBitmap() }
    }

    @Test
    fun mainThreadCalls_throwIllegalStateException() {
        val rule = createXrSessionRule()
        val description =
            Description.createTestDescription(XrSessionRuleTest::class.java, "mainThreadTest")

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertFailsWith<IllegalStateException> {
                rule
                    .apply(
                        object : Statement() {
                            override fun evaluate() {}
                        },
                        description,
                    )
                    .evaluate()
            }
            assertFailsWith<IllegalStateException> { rule.onMainThread {} }
            assertFailsWith<IllegalStateException> { rule.captureToBitmap() }
            assertFailsWith<IllegalArgumentException> { rule.captureToBitmap(padding = -1) }
        }
    }

    @Test
    fun captureToBitmap_rejectsNegativePaddingOffMainThread() {
        val rule = createXrSessionRule()
        assertFailsWith<IllegalArgumentException> { rule.captureToBitmap(padding = -1) }
    }

    @Test
    fun onMainThread_providesScopeReceiverOnMainLooper() {
        setXrSystemFeature(true)
        val (rule, expectedActivity, expectedSession) = createFakeRule()

        rule.evaluateRule {
            val returned = rule.onMainThread {
                assertSame(Looper.getMainLooper(), Looper.myLooper())
                assertSame(expectedActivity, activity)
                assertSame(expectedSession, session)
                "done"
            }
            assertEquals("done", returned)
        }
    }

    @Test
    fun apply_whenTestBodyThrows_rethrowsAndClearsActiveState() {
        setXrSystemFeature(true)
        val (rule, _, _) = createFakeRule()

        val thrown =
            assertFailsWith<AssertionError> {
                rule.evaluateRule { throw AssertionError("primary body failure") }
            }

        assertEquals("primary body failure", thrown.message)
        assertFailsWith<IllegalStateException> { rule.activity }
        assertFailsWith<IllegalStateException> { rule.session }
        assertFailsWith<IllegalStateException> { rule.onMainThread {} }
    }
}
