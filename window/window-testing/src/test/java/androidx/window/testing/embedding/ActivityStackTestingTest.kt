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

package androidx.window.testing.embedding

import android.app.Activity
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.ActivityStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

/** Test class to verify [TestActivityStack] */
@OptIn(ExperimentalWindowApi::class)
class ActivityStackTestingTest {

    /** Verifies the default value of [TestActivityStack] */
    @Test
    fun testActivityStackDefaultValue() {
        val activityStack = TestActivityStack()

        assertEquals(
            ActivityStack(emptyList(), isEmpty = false, TEST_ACTIVITY_STACK_TOKEN),
            activityStack
        )
    }

    /** Verifies [TestActivityStack] */
    @Test
    fun testActivityStackWithNonEmptyActivityList() {
        val activity = mock<Activity>()
        val activityStack = TestActivityStack(listOf(activity), isEmpty = false)

        assertTrue(activity in activityStack)
        assertFalse(activityStack.isEmpty)
    }
}
