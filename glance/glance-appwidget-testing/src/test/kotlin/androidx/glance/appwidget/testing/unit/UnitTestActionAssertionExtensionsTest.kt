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

package androidx.glance.appwidget.testing.unit

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.layout.EmittableColumn
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.getGlanceNodeAssertionFor
import androidx.glance.testing.unit.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.ExpectFailure.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Tests appWidget-specific click/action related convenience assertions and the underlying filters
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnitTestActionAssertionExtensionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun assertHasStartActivityClickAction_withIntent() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionStartActivity(
                            testActivityIntent(context, TestActivity::class.java)
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(
            testActivityIntent(context, TestActivity::class.java)
        )
    }

    @Test
    fun assertHasStartActivityClickAction_intentNotMatched_failure() {
        val expectedIntent = testActivityIntent(context, TestActivity::class.java)
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionStartActivity(
                            testActivityIntent(context, AnotherTestActivity::class.java)
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartActivityClickAction(expectedIntent)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "intent: $expectedIntent and parameters: {})"
            )
    }

    @Test
    fun assertHasStartActivityClickAction_withIntentAndParameters() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity(
                            intent = testActivityIntent(context, TestActivity::class.java),
                            parameters = actionParametersOf(
                                TEST_ACTION_PARAM_KEY to -1
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(
            intent = testActivityIntent(context, TestActivity::class.java),
            parameters = actionParametersOf(
                TEST_ACTION_PARAM_KEY to -1
            )
        )
        // no error
    }

    @Test
    fun assertHasStartActivityClickAction_intentParametersNotMatched_failure() {
        val testIntent = testActivityIntent(context, TestActivity::class.java)
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity(
                            intent = testIntent,
                            parameters = actionParametersOf(
                                TEST_ACTION_PARAM_KEY to 100
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartActivityClickAction(
                intent = testIntent,
                parameters = actionParametersOf(TEST_ACTION_PARAM_KEY to 99)
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "intent: $testIntent and parameters: {Test=99})"
            )
    }

    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun assertHasStartActivityClickAction_withIntentParametersAndOptions() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity(
                            intent = testActivityIntent(context, TestActivity::class.java),
                            parameters = actionParametersOf(
                                TEST_ACTION_PARAM_KEY to -1
                            ),
                            activityOptions = TEST_ACTIVITY_OPTIONS_BUNDLE
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(
            intent = testActivityIntent(context, TestActivity::class.java),
            parameters = actionParametersOf(
                TEST_ACTION_PARAM_KEY to -1
            ),
            activityOptions = TEST_ACTIVITY_OPTIONS_BUNDLE
        )
        // no error
    }

    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun assertHasStartActivityClickAction_optionsNotMatched_failure() {
        val testIntent = testActivityIntent(context, TestActivity::class.java)
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity(
                            intent = testIntent,
                            parameters = actionParametersOf(
                                TEST_ACTION_PARAM_KEY to 100
                            ),
                            activityOptions = Bundle.EMPTY
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartActivityClickAction(
                intent = testIntent,
                parameters = actionParametersOf(
                    TEST_ACTION_PARAM_KEY to 99
                ),
                activityOptions = TEST_ACTIVITY_OPTIONS_BUNDLE
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "intent: $testIntent, parameters: {Test=99} and " +
                    "bundle: Bundle[{android:activity.packageName=test.package}])"
            )
    }

    @Test
    fun assertHasStartServiceClickAction() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService<TestService>())
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasStartServiceClickAction(TestService::class.java)
    }

    @Test
    fun assertHasStartServiceClickAction_serviceNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService<AnotherTestService>())
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartServiceClickAction(TestService::class.java)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start service action for " +
                    "non-foreground service: ${TestService::class.java.name})"
            )
    }

    @Test
    fun assertHasStartServiceClickAction_foreground() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionStartService<TestService>(
                            isForegroundService = true
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasStartServiceClickAction(
            serviceClass = TestService::class.java,
            isForegroundService = true
        )
    }

    @Test
    fun assertHasStartServiceClickAction_foregroundNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService<TestService>())
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartServiceClickAction(
                serviceClass = TestService::class.java,
                isForegroundService = true
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start service action for " +
                    "foreground service: ${TestService::class.java.name})"
            )
    }

    @Test
    fun assertHasStartServiceClickAction_component() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService(TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasStartServiceClickAction(TEST_COMPONENT_NAME)
    }

    @Test
    fun assertHasStartServiceClickAction_componentNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService(ANOTHER_TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartServiceClickAction(TEST_COMPONENT_NAME)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start service action for non-foreground " +
                    "service component: ComponentInfo{test.pkg/Test})"
            )
    }

    @Test
    fun assertHasStartServiceClickAction_componentForegroundNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService(ANOTHER_TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartServiceClickAction(
                componentName = TEST_COMPONENT_NAME,
                isForegroundService = true
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start service action for foreground " +
                    "service component: ComponentInfo{test.pkg/Test})"
            )
    }

    @Test
    fun assertHasStartServiceClickAction_intent() {
        val testServiceIntent = testServiceIntent(
            context = context,
            serviceClass = TestService::class.java
        )
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionStartService(testServiceIntent))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasStartServiceClickAction(testServiceIntent)
    }

    @Test
    fun assertHasStartServiceClickAction_intentNotMatched_failure() {
        val expectedServiceIntent = testServiceIntent(
            context = context,
            serviceClass = TestService::class.java
        )
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionStartService(
                            testServiceIntent(
                                context = context,
                                serviceClass = AnotherTestService::class.java
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartServiceClickAction(expectedServiceIntent)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start service action for non-foreground " +
                    "service: $expectedServiceIntent)"
            )
    }

    @Test
    fun assertHasSendBroadcastClickAction_foreground() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast<TestBroadcastReceiver>())
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasSendBroadcastClickAction(TestBroadcastReceiver::class.java)
    }

    @Test
    fun assertHasSendBroadcastClickAction_receiverNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast<AnotherTestBroadcastReceiver>())
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasSendBroadcastClickAction(TestBroadcastReceiver::class.java)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has send broadcast action for receiver class: " +
                    "${TestBroadcastReceiver::class.java.name})"
            )
    }

    @Test
    fun assertHasSendBroadcastClickAction_intentAction() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast(action = "test_action"))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasSendBroadcastClickAction(intentAction = "test_action")
        // no error
    }

    @Test
    fun assertHasSendBroadcastClickAction_intentActionNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast(action = "another_test_action"))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasSendBroadcastClickAction(
                intentAction = "test_action"
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has send broadcast action with intent action: " +
                    "test_action)"
            )
    }

    @Test
    fun assertHasSendBroadcastClickAction_intentActionComponent() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionSendBroadcast(
                            action = "test_action",
                            componentName = TEST_COMPONENT_NAME
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasSendBroadcastClickAction(
            intentAction = "test_action",
            componentName = TEST_COMPONENT_NAME
        )
        // no error
    }

    @Test
    fun assertHasSendBroadcastClickAction_intentActionComponentNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionSendBroadcast(
                            action = "another_test_action",
                            componentName = ANOTHER_TEST_COMPONENT_NAME
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasSendBroadcastClickAction(
                intentAction = "test_action",
                componentName = TEST_COMPONENT_NAME
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has send broadcast action with intent action: " +
                    "test_action and component: ComponentInfo{test.pkg/Test})"
            )
    }

    @Test
    fun assertHasSendBroadcastClickAction_component() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast(TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasSendBroadcastClickAction(TEST_COMPONENT_NAME)
        // no error
    }

    @Test
    fun assertHasSendBroadcastClickAction_componentNotMatched_failure() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast(ANOTHER_TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasSendBroadcastClickAction(TEST_COMPONENT_NAME)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has send broadcast action with " +
                    "component: ComponentInfo{test.pkg/Test})"
            )
    }

    @Test
    fun assertHasSendBroadcastClickAction_intent() {
        val testIntent = testBroadcastIntent(
            context = context,
            receiverClass = TestBroadcastReceiver::class.java
        )
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(actionSendBroadcast(testIntent))
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        nodeAssertion.assertHasSendBroadcastClickAction(testIntent)
        // no error
    }

    @Test
    fun assertHasSendBroadcastClickAction_intentNotMatched_failure() {
        val expectedTestIntent = testBroadcastIntent(
            context = context,
            receiverClass = TestBroadcastReceiver::class.java
        )
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
                    .clickable(
                        actionSendBroadcast(
                            testBroadcastIntent(
                                context = context,
                                receiverClass = AnotherTestBroadcastReceiver::class.java
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasSendBroadcastClickAction(expectedTestIntent)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has send broadcast action with intent: " +
                    "$expectedTestIntent"
            )
    }

    companion object {
        private val TEST_ACTION_PARAM_KEY = ActionParameters.Key<Int>("Test")
        private val TEST_ACTIVITY_OPTIONS_BUNDLE =
            Bundle().apply { putString("android:activity.packageName", "test.package") }

        private val ANOTHER_TEST_COMPONENT_NAME = ComponentName("test.pkg", "AnotherTest")
        private val TEST_COMPONENT_NAME = ComponentName("test.pkg", "Test")

        private class TestActivity : Activity()
        private class AnotherTestActivity : Activity()

        class TestService : Service() {
            override fun onBind(p0: Intent?): IBinder? = null
        }

        class AnotherTestService : Service() {
            override fun onBind(p0: Intent?): IBinder? = null
        }

        class TestBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Nothing
            }
        }

        class AnotherTestBroadcastReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Nothing
            }
        }

        private fun <T : Activity> testActivityIntent(
            context: Context,
            activityClass: Class<T>
        ): Intent {
            return Intent(context, activityClass).setAction("test")
        }

        private fun <T : Service> testServiceIntent(
            context: Context,
            serviceClass: Class<T>
        ): Intent {
            return Intent(context, serviceClass).setAction("test")
        }

        private fun <T : BroadcastReceiver> testBroadcastIntent(
            context: Context,
            receiverClass: Class<T>
        ): Intent {
            return Intent(context, receiverClass).setAction("test")
        }
    }
}
