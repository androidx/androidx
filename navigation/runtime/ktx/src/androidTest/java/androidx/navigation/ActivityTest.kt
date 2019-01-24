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
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@LargeTest
class ActivityTest {
    @get:Rule val activityRule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private val view get() = activityRule.activity.findViewById<View>(VIEW_ID)

    @Test fun findNavController() {
        val navController = NavController(activityRule.activity)
        Navigation.setViewNavController(view, navController)

        val foundNavController = activityRule.activity.findNavController(VIEW_ID)
        assertTrue("View should have NavController set",
                foundNavController == navController)
    }

    @Test fun findNavControllerNull() {
        try {
            activityRule.activity.findNavController(VIEW_ID)
            fail("findNavController should throw IllegalStateException if a NavController" +
                    " was not set")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test fun findNavControllerInvalidViewId() {
        try {
            activityRule.activity.findNavController(INVALID_VIEW_ID)
            fail("findNavController should throw IllegalArgumentException if the view" +
                    " does not exist")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test fun navArgsLazy() {
        // Normally, this would be set by using an <activity> destination to
        // start the Activity, but we'll fake it here in the test
        activityRule.activity.intent = Intent(
            activityRule.activity, TestActivity::class.java
        ).apply {
            putExtra("test", "test")
        }
        assertThat(activityRule.activity.args)
            .isNotNull()
        assertThat(activityRule.activity.args.bundle["test"])
            .isEqualTo("test")
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
class TestActivity : Activity() {
    val args: FakeTestArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(View(this).apply {
            id = VIEW_ID
        })
    }
}
