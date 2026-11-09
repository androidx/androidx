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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
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
    private fun createFakeRule(
        cleanupTimeout: Duration = 10.seconds
    ): Triple<
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
                cleanupTimeout = cleanupTimeout,
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
    fun constructor_rejectsNonPositiveCleanupTimeout() {
        val activity =
            runBlocking(Dispatchers.Main.immediate) {
                Robolectric.buildActivity(CustomTestActivity::class.java).setup().get()
            }
        val trackingRule = TrackingActivityRule(activity)
        assertFailsWith<IllegalArgumentException> {
            XrSessionRule(trackingRule, cleanupTimeout = Duration.ZERO) { it.getActivity() }
        }
        assertFailsWith<IllegalArgumentException> {
            XrSessionRule(trackingRule, cleanupTimeout = (-1).milliseconds) { it.getActivity() }
        }
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
    fun accessorsAndCleanupsOutsideRunningTest_throwIllegalStateException() {
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
            "XrSessionRule.manage is only available while a test is running.",
            assertFailsWith<IllegalStateException> { rule.manage(AutoCloseable {}) }.message,
        )
        assertEquals(
            "XrSessionRule.onCleanup is only available while a test is running.",
            assertFailsWith<IllegalStateException> { rule.onCleanup {} }.message,
        )
        assertEquals(
            "XrSessionRule.onMainThread is only available while a test is running.",
            assertFailsWith<IllegalStateException> { rule.onMainThread {} }.message,
        )

        val resourceClosed = AtomicBoolean(false)
        rule.evaluateRule {
            assertSame(expectedActivity, rule.activity)
            assertSame(expectedSession, rule.session)
            val resource = AutoCloseable { resourceClosed.set(true) }
            assertSame(resource, rule.manage(resource))
        }
        assertTrue(resourceClosed.get())

        // After evaluation finishes, all active-test guards throw again.
        assertFailsWith<IllegalStateException> { rule.activity }
        assertFailsWith<IllegalStateException> { rule.session }
        assertFailsWith<IllegalStateException> { rule.manage(AutoCloseable {}) }
        assertFailsWith<IllegalStateException> { rule.onCleanup {} }
        assertFailsWith<IllegalStateException> { rule.onMainThread {} }
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
        val closedOnMainLooper = AtomicBoolean(false)

        rule.evaluateRule {
            val returned = rule.onMainThread {
                assertSame(Looper.getMainLooper(), Looper.myLooper())
                assertSame(expectedActivity, activity)
                assertSame(expectedSession, session)
                manage(
                    AutoCloseable {
                        closedOnMainLooper.set(Looper.myLooper() == Looper.getMainLooper())
                    }
                )
                "done"
            }
            assertEquals("done", returned)
        }

        assertTrue(closedOnMainLooper.get())
    }

    @Test
    fun cleanups_interleavedManageAndOnCleanupWithKeys_runInStrictLifoOrder() {
        setXrSystemFeature(true)
        val (rule, _, _) = createFakeRule()
        val events = mutableListOf<String>()
        val dedupKey = Any()

        rule.evaluateRule {
            rule.onCleanup(key = dedupKey) {
                assertSame(Looper.getMainLooper(), Looper.myLooper())
                events += "1-keyedCleanup"
            }
            rule.onMainThread {
                manage(
                    AutoCloseable {
                        assertSame(Looper.getMainLooper(), Looper.myLooper())
                        events += "2-manageA"
                    }
                )
                onCleanup { events += "3-cleanupB" }
                // Duplicate key registration must be a no-op and preserve the original LIFO slot.
                onCleanup(key = dedupKey) { events += "duplicate-should-not-run" }
                manage(AutoCloseable { events += "4-manageC" })
            }
            rule.onCleanup { events += "5-cleanupD" }
        }

        assertEquals(
            listOf("5-cleanupD", "4-manageC", "3-cleanupB", "2-manageA", "1-keyedCleanup"),
            events,
        )
    }

    @Test
    fun tearDown_whenTestBodySucceedsAndCleanupsFail_runsAllCleanupsAndThrowsFirstFailure() {
        setXrSystemFeature(true)
        val (rule, _, _) = createFakeRule()
        val events = mutableListOf<String>()

        val thrown =
            assertFailsWith<XrSessionRule.CleanupException> {
                rule.evaluateRule {
                    rule.onCleanup { events += "1-cleanup-ok" }
                    rule.onCleanup {
                        events += "2-cleanup-throws"
                        throw IllegalStateException("cleanup-2 boom")
                    }
                    rule.manage(
                        object : AutoCloseable {
                            override fun close() {
                                events += "3-close-throws"
                                throw IllegalArgumentException("close-3 boom")
                            }

                            override fun toString(): String = "TestResource"
                        }
                    )
                }
            }

        assertEquals(listOf("3-close-throws", "2-cleanup-throws", "1-cleanup-ok"), events)
        assertTrue(thrown.message.orEmpty().contains("close TestResource"))
        assertEquals("close-3 boom", thrown.cause?.message)
        assertEquals(1, thrown.suppressed.size)
        assertTrue(thrown.suppressed[0].message.orEmpty().contains("onCleanup #1"))
        assertEquals("cleanup-2 boom", thrown.suppressed[0].cause?.message)
    }

    @Test
    fun tearDown_whenTestBodyThrowsAndCleanupsFail_attachesSuppressedToPrimaryFailure() {
        setXrSystemFeature(true)
        val (rule, _, _) = createFakeRule()
        val events = mutableListOf<String>()

        val primary =
            assertFailsWith<AssertionError> {
                rule.evaluateRule {
                    rule.onCleanup { events += "1-cleanup-ok" }
                    rule.onCleanup {
                        events += "2-cleanup-throws"
                        throw IllegalStateException("cleanup-2 boom")
                    }
                    rule.manage(
                        object : AutoCloseable {
                            override fun close() {
                                events += "3-close-throws"
                                throw IllegalArgumentException("close-3 boom")
                            }

                            override fun toString(): String = "TestResource"
                        }
                    )
                    throw AssertionError("primary body failure")
                }
            }

        assertEquals("primary body failure", primary.message)
        assertEquals(listOf("3-close-throws", "2-cleanup-throws", "1-cleanup-ok"), events)
        assertEquals(2, primary.suppressed.size)
        assertTrue(primary.suppressed[0].message.orEmpty().contains("close TestResource"))
        assertEquals("close-3 boom", primary.suppressed[0].cause?.message)
        assertTrue(primary.suppressed[1].message.orEmpty().contains("onCleanup #1"))
        assertEquals("cleanup-2 boom", primary.suppressed[1].cause?.message)
    }

    @Test
    fun tearDown_cleanupTimeout_cancelsHungCleanupAndRunsRemainingCleanups() {
        setXrSystemFeature(true)
        val (rule, _, _) = createFakeRule(cleanupTimeout = 50.milliseconds)
        val events = mutableListOf<String>()
        val hungCleanupStarted = CountDownLatch(1)
        val clockAdvancer = thread {
            if (hungCleanupStarted.await(5, TimeUnit.SECONDS)) {
                // Robolectric's paused clock does not advance Handler.postDelayed timers on its
                // own; advance the main looper clock so withTimeout(50.milliseconds) fires.
                Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)
            }
        }

        val thrown =
            try {
                assertFailsWith<XrSessionRule.CleanupException> {
                    rule.evaluateRule {
                        rule.onCleanup { events += "after-timeout" }
                        rule.onCleanup(key = "hungCleanup") {
                            events += "timed-out-item"
                            hungCleanupStarted.countDown()
                            CompletableDeferred<Unit>().await()
                        }
                        rule.manage(AutoCloseable { events += "before-timeout" })
                    }
                }
            } finally {
                hungCleanupStarted.countDown()
                clockAdvancer.join(5000)
            }

        assertEquals(listOf("before-timeout", "timed-out-item", "after-timeout"), events)
        assertTrue(thrown.message.orEmpty().contains("onCleanup (hungCleanup)"))
        assertIs<TimeoutCancellationException>(thrown.cause)
    }
}
