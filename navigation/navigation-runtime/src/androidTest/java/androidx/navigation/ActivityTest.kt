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
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@LargeTest
class ActivityTest {
    @Suppress("DEPRECATION")
    @get:Rule val activityRule = androidx.test.rule.ActivityTestRule<TestArgsActivity>(
        TestArgsActivity::class.java
    )
    private val view get() = activityRule.activity.findViewById<View>(VIEW_ID)

    @Test fun findNavController() {
        val navController = NavController(activityRule.activity)
        Navigation.setViewNavController(view, navController)

        val foundNavController = activityRule.activity.findNavController(VIEW_ID)
        assertWithMessage("View should have NavController set")
            .that(foundNavController).isSameInstanceAs(navController)
    }

    @Test fun findNavControllerNull() {
        try {
            activityRule.activity.findNavController(VIEW_ID)
            fail(
                "findNavController should throw IllegalStateException if a NavController" +
                    " was not set"
            )
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test fun findNavControllerInvalidViewId() {
        try {
            activityRule.activity.findNavController(INVALID_VIEW_ID)
            fail(
                "findNavController should throw IllegalArgumentException if the view" +
                    " does not exist"
            )
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test fun navArgsLazy() {
        // Normally, this would be set by using an <activity> destination to
        // start the Activity, but we'll fake it here in the test
        activityRule.activity.intent = Intent(
            activityRule.activity, TestArgsActivity::class.java
        ).apply {
            putExtra("test", "test")
        }
        assertThat(activityRule.activity.args)
            .isNotNull()
        assertThat(activityRule.activity.args.bundle["test"])
            .isEqualTo("test")
    }

    @Test fun navArgsLazyNoExtras() {
        // Normally, this would be set by using an <activity> destination to
        // start the Activity, but we'll fake it here in the test
        activityRule.activity.intent = Intent(
            activityRule.activity, TestArgsActivity::class.java
        )
        try {
            activityRule.activity.args
            fail("by navArgs() should throw an IllegalStateException when there are no extras")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().isEqualTo(
                "Activity ${activityRule.activity} has null extras in " +
                    activityRule.activity.intent
            )
        }
    }
}

private const val VIEW_ID = 1
private const val INVALID_VIEW_ID = 2

/**
 * It is a lot harder to test generated NavArgs classes, so
 * we'll just fake one that has the same fromBundle method
 * that NavArgsLazy expects
 */
data class FakeTestArgs(val bundle: Bundle) : NavArgs {
    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle) = FakeTestArgs(bundle)
    }
}
class TestArgsActivity : Activity() {
    val args: FakeTestArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            View(this).apply {
                id = VIEW_ID
            }
        )
    }
}
