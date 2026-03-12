/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.savedstate.SavedState
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.refEq
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivityNavigatorTest {
    companion object {
        const val TARGET_ID = 1
        const val TARGET_ACTION = "test_action"
        val TARGET_DATA: Uri = Uri.parse("http://www.example.com")
        const val TARGET_ARGUMENT_NAME = "test"
        const val TARGET_DATA_PATTERN = "http://www.example.com/{$TARGET_ARGUMENT_NAME}"
        const val TARGET_ARGUMENT_VALUE = "data_pattern"
        const val TARGET_ARGUMENT_INT_VALUE = 1
        const val TARGET_LABEL = "test_label"
    }

    @Suppress("DEPRECATION")
    @get:Rule
    val activityRule = androidx.test.rule.ActivityTestRule(ActivityNavigatorActivity::class.java)

    private lateinit var activityNavigator: ActivityNavigator

    @Before
    fun setup() {
        activityNavigator = ActivityNavigator(activityRule.activity)
        TargetActivity.instances = spy(ArrayList())
    }

    @After
    fun cleanup() {
        TargetActivity.instances.forEach { activity -> activity.finish() }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun throwOnPutAction() {
        val targetDestination = activityNavigator.createDestination()
        targetDestination.putAction(TARGET_ID, 0)
    }

    @Test
    fun navigate() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        activityNavigator.navigate(targetDestination, null, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should not include FLAG_ACTIVITY_NEW_TASK")
            .that(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            .isEqualTo(0)
    }

    @Test
    fun navigateFromNonActivityContext() {
        // Create using the applicationContext
        val activityNavigator = ActivityNavigator(activityRule.activity.applicationContext)

        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        activityNavigator.navigate(targetDestination, null, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should not include FLAG_ACTIVITY_NEW_TASK")
            .that(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun navigateSingleTop() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        activityNavigator.navigate(
            targetDestination,
            null,
            navOptions { launchSingleTop = true },
            null,
        )

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should include FLAG_ACTIVITY_SINGLE_TOP")
            .that(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .isEqualTo(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    @Test
    fun navigateWithArgs() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }

        val args = Bundle().apply { putString(TARGET_ARGUMENT_NAME, TARGET_ARGUMENT_VALUE) }
        activityNavigator.navigate(targetDestination, args, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should have its arguments in its extras")
            .that(intent.getStringExtra(TARGET_ARGUMENT_NAME))
            .isEqualTo(TARGET_ARGUMENT_VALUE)
    }

    @Test
    fun navigateAction() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setAction(TARGET_ACTION)
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        activityNavigator.navigate(targetDestination, null, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should have action set")
            .that(intent.action)
            .isEqualTo(TARGET_ACTION)
    }

    @Test
    fun navigateData() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setData(TARGET_DATA)
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        activityNavigator.navigate(targetDestination, null, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should have data set").that(intent.data).isEqualTo(TARGET_DATA)
    }

    @Test
    fun navigateDataPattern() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setDataPattern(TARGET_DATA_PATTERN)
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val args = Bundle().apply { putString(TARGET_ARGUMENT_NAME, TARGET_ARGUMENT_VALUE) }
        activityNavigator.navigate(targetDestination, args, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should have data set with argument filled in")
            .that(intent.data?.toString())
            .isEqualTo(
                TARGET_DATA_PATTERN.replace("{$TARGET_ARGUMENT_NAME}", TARGET_ARGUMENT_VALUE)
            )
        assertWithMessage("Intent should have its arguments in its extras")
            .that(intent.getStringExtra(TARGET_ARGUMENT_NAME))
            .isEqualTo(TARGET_ARGUMENT_VALUE)
    }

    @Test
    fun navigateDataPatternInt() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setDataPattern(TARGET_DATA_PATTERN)
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val args = Bundle().apply { putInt(TARGET_ARGUMENT_NAME, TARGET_ARGUMENT_INT_VALUE) }
        activityNavigator.navigate(targetDestination, args, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should have data set with argument filled in")
            .that(intent.data?.toString())
            .isEqualTo(
                TARGET_DATA_PATTERN.replace(
                    "{$TARGET_ARGUMENT_NAME}",
                    TARGET_ARGUMENT_INT_VALUE.toString(),
                )
            )
        assertWithMessage("Intent should have its arguments in its extras")
            .that(intent.getIntExtra(TARGET_ARGUMENT_NAME, -1))
            .isEqualTo(TARGET_ARGUMENT_INT_VALUE)
    }

    @Test
    fun navigateDataPatternIntWithNavType() {
        val navType =
            object : NavType<Int>(false) {
                override fun put(bundle: SavedState, key: String, value: Int) {
                    IntType.put(bundle, key, value)
                }

                override fun get(bundle: SavedState, key: String): Int? = IntType[bundle, key]

                override fun parseValue(value: String): Int =
                    value.replace("Serialized", "").toInt()

                override fun serializeAsValue(value: Int): String = "${value}Serialized"
            }
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setDataPattern(TARGET_DATA_PATTERN)
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
                addArgument(TARGET_ARGUMENT_NAME, NavArgument.Builder().setType(navType).build())
            }
        val args = Bundle().apply { putInt(TARGET_ARGUMENT_NAME, TARGET_ARGUMENT_INT_VALUE) }
        activityNavigator.navigate(targetDestination, args, null, null)

        val targetActivity = waitForActivity()
        val intent = targetActivity.intent
        assertThat(intent).isNotNull()
        assertWithMessage("Intent should have data set with argument filled in")
            .that(intent.data?.toString())
            .isEqualTo(
                TARGET_DATA_PATTERN.replace(
                    "{$TARGET_ARGUMENT_NAME}",
                    "${TARGET_ARGUMENT_INT_VALUE}Serialized",
                )
            )
        assertWithMessage("Intent should have its arguments in its extras")
            .that(intent.getIntExtra(TARGET_ARGUMENT_NAME, -1))
            .isEqualTo(TARGET_ARGUMENT_INT_VALUE)
    }

    @Test
    fun navigateDataPatternMissingArgument() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setDataPattern(TARGET_DATA_PATTERN)
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        try {
            val args = Bundle()
            activityNavigator.navigate(targetDestination, args, null, null)
            fail("navigate() should fail if required arguments are not included")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    @UiThreadTest
    fun navigateWithExtras() {
        val context = mock(Context::class.java)
        val view = mock(View::class.java)
        val activityNavigator = ActivityNavigator(context)
        val activityOptions =
            ActivityOptionsCompat.makeSceneTransitionAnimation(activityRule.activity, view, "test")
        val flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        val extras =
            ActivityNavigator.Extras.Builder()
                .setActivityOptions(activityOptions)
                .addFlags(flags)
                .build()

        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        activityNavigator.navigate(targetDestination, null, null, extras)
        // Just verify that the ActivityOptions got passed through, there's
        // CTS tests to ensure that the ActivityOptions do the right thing
        verify(context)
            .startActivity(
                argThat { intent -> intent.flags and flags != 0 },
                refEq(activityOptions.toBundle()),
            )
    }

    @Test
    fun testEquals() {
        val firstDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val secondDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        assertThat(firstDestination).isEqualTo(secondDestination)
    }

    @Test
    fun testFilterEquals() {
        val firstDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val secondDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
                intent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        assertThat(firstDestination).isEqualTo(secondDestination)
    }

    @Test
    fun testEqualsBothIntentNull() {
        val firstDestination = activityNavigator.createDestination().apply { id = TARGET_ID }
        val secondDestination = activityNavigator.createDestination().apply { id = TARGET_ID }
        assertThat(firstDestination).isEqualTo(secondDestination)
    }

    @Test
    fun testNotEquals() {
        val firstDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val secondDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
                setAction(TARGET_ACTION)
            }
        assertThat(firstDestination).isNotEqualTo(secondDestination)
    }

    @Test
    fun testNotEqualsFirstIntentNull() {
        val firstDestination = activityNavigator.createDestination().apply { id = TARGET_ID }
        val secondDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        assertThat(firstDestination).isNotEqualTo(secondDestination)
    }

    @Test
    fun testNotEqualsSecondIntentNull() {
        val firstDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val secondDestination = activityNavigator.createDestination().apply { id = TARGET_ID }
        assertThat(firstDestination).isNotEqualTo(secondDestination)
    }

    @Test
    fun testToString() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                label = TARGET_LABEL
                setComponentName(ComponentName(activityRule.activity, TargetActivity::class.java))
            }
        val expected =
            "Destination(0x${TARGET_ID.toString(16)}) label=$TARGET_LABEL " +
                "class=${TargetActivity::class.java.name}"
        assertThat(targetDestination.toString()).isEqualTo(expected)
    }

    @Test
    fun testToStringNoClass() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                label = TARGET_LABEL
                setAction(TARGET_ACTION)
            }
        val expected =
            "Destination(0x${TARGET_ID.toString(16)}) label=$TARGET_LABEL " +
                "action=$TARGET_ACTION"
        assertThat(targetDestination.toString()).isEqualTo(expected)
    }

    @Test
    fun testToStringNoClassOrAction() {
        val targetDestination =
            activityNavigator.createDestination().apply {
                id = TARGET_ID
                label = TARGET_LABEL
            }
        val expected = "Destination(0x${TARGET_ID.toString(16)}) label=$TARGET_LABEL"
        assertThat(targetDestination.toString()).isEqualTo(expected)
    }

    private fun waitForActivity(): TargetActivity {
        verify(TargetActivity.instances, timeout(3000)).add(any())
        verifyNoMoreInteractions(TargetActivity.instances)
        val targetActivity: ArrayList<TargetActivity> = ArrayList()
        activityRule.runOnUiThread { targetActivity.addAll(TargetActivity.instances) }
        assertWithMessage("Only expected a single TargetActivity")
            .that(targetActivity.size == 1)
            .isTrue()
        return targetActivity[0]
    }
}

class ActivityNavigatorActivity : Activity()

class TargetActivity : Activity() {
    companion object {
        var instances: ArrayList<TargetActivity> = spy(ArrayList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instances.add(this)
    }
}
