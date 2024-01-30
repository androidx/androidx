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

package androidx.glance.testing.unit

import android.app.Activity
import android.content.ComponentName
import android.os.Bundle
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.LambdaAction
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.EmittableColumn
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import com.google.common.truth.ExpectFailure.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Tests click/action related convenience assertions and the underlying filters / matchers that are
// common to surfaces and relevant to unit tests
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnitTestActionAssertionExtensionsTest {
    @Test
    fun assertHasClickAction_lambda() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(onClick = LambdaAction("test-key") {})
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasClickAction()
        // no error
    }

    @Test
    fun assertHasClickAction_nonLambdaAction() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(onClick = actionStartActivity<TestActivity>())
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasClickAction()
        // no error
    }

    @Test
    fun assertHasClickAction_noClick_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasClickAction()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (has click action)")
    }

    @Test
    fun assertHasNoClickAction() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasNoClickAction()
        // no error
    }

    @Test
    fun assertHasNoClickAction_hasClick_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(actionStartActivity<TestActivity>())
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasNoClickAction()
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains("Failed to assert condition: (has no click action)")
    }

    @Test
    fun assertHasStartActivityClickAction_withActivityClass() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(actionStartActivity<TestActivity>())
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(TestActivity::class.java)
        // no error
    }

    @Test
    fun assertHasStartActivityClickAction_activityClassNotMatched_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(actionStartActivity<AnotherTestActivity>())
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartActivityClickAction(TestActivity::class.java)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "activity: ${TestActivity::class.java.name} and parameters: {}"
            )
    }

    @Test
    fun assertHasStartActivityClickAction_withActivityClassParameters() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity<TestActivity>(
                            actionParametersOf(
                                TEST_ACTION_PARAM_KEY to -1
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(
            TestActivity::class.java, actionParametersOf(
                TEST_ACTION_PARAM_KEY to -1
            )
        )
        // no error
    }

    @Test
    fun assertHasStartActivityClickAction_parametersNotMatched_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity<TestActivity>(
                            actionParametersOf(
                                TEST_ACTION_PARAM_KEY to 100
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartActivityClickAction(
                activityClass = TestActivity::class.java,
                parameters = actionParametersOf(
                    TEST_ACTION_PARAM_KEY to 99
                )
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "activity: ${TestActivity::class.java.name} and parameters: {Test=99}"
            )
    }

    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun assertHasStartActivityClickAction_withActivityClassParametersAndOptions() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity<TestActivity>(
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
            activityClass = TestActivity::class.java,
            parameters = actionParametersOf(
                TEST_ACTION_PARAM_KEY to -1
            ),
            activityOptions = TEST_ACTIVITY_OPTIONS_BUNDLE
        )
        // no error
    }

    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun assertHasStartActivityClickAction_optionsNotMatched_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity<TestActivity>(
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
                activityClass = TestActivity::class.java,
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
                    "activity: ${TestActivity::class.java.name}, parameters: {Test=99} and " +
                    "bundle: Bundle[{android:activity.packageName=test.package}])"
            )
    }

    @Test
    fun assertHasStartActivityClickAction_withComponentName() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(actionStartActivity(TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(TEST_COMPONENT_NAME)
        // no error
    }

    @Test
    fun assertHasStartActivityClickAction_componentNotMatched_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(actionStartActivity(TEST_COMPONENT_NAME))
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        val assertionError = Assert.assertThrows(AssertionError::class.java) {
            nodeAssertion.assertHasStartActivityClickAction(ANOTHER_TEST_COMPONENT_NAME)
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "componentName: ComponentInfo{test.pkg/AnotherTestActivity} and parameters: {})"
            )
    }

    @Test
    fun assertHasStartActivityClickAction_withComponentNameAndParameters() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity(
                            componentName = TEST_COMPONENT_NAME,
                            parameters = actionParametersOf(
                                TEST_ACTION_PARAM_KEY to -1
                            )
                        )
                    )
            },
            onNodeMatcher = hasTestTag("existing-test-tag")
        )

        nodeAssertion.assertHasStartActivityClickAction(
            componentName = TEST_COMPONENT_NAME,
            parameters = actionParametersOf(
                TEST_ACTION_PARAM_KEY to -1
            ),
        )
        // no error
    }

    @Test
    fun assertHasStartActivityClickAction_componentParametersNotMatched_assertionError() {
        val nodeAssertion = getGlanceNodeAssertionFor(
            emittable = EmittableColumn().apply {
                modifier = GlanceModifier.semantics { testTag = "existing-test-tag" }
                    .clickable(
                        actionStartActivity(
                            componentName = TEST_COMPONENT_NAME,
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
                componentName = TEST_COMPONENT_NAME,
                parameters = actionParametersOf(TEST_ACTION_PARAM_KEY to 99)
            )
        }

        assertThat(assertionError)
            .hasMessageThat()
            .contains(
                "Failed to assert condition: (has start activity click action with " +
                    "componentName: ComponentInfo{test.pkg/TestActivity} and parameters: {Test=99})"
            )
    }

    companion object {
        private val TEST_ACTION_PARAM_KEY = ActionParameters.Key<Int>("Test")
        private val TEST_ACTIVITY_OPTIONS_BUNDLE =
            Bundle().apply { putString("android:activity.packageName", "test.package") }
        private val TEST_COMPONENT_NAME = ComponentName("test.pkg", "TestActivity")
        private val ANOTHER_TEST_COMPONENT_NAME = ComponentName("test.pkg", "AnotherTestActivity")

        private class TestActivity : Activity()
        private class AnotherTestActivity : Activity()
    }
}
