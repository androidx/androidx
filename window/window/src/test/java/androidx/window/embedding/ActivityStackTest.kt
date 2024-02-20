/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.embedding

import android.app.Activity
import android.os.Binder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

/** Unit tests for [ActivityStack] */
class ActivityStackTest {

    @Test
    fun testContainsActivity() {
        val activity = mock<Activity>()
        val stack = ActivityStack(listOf(activity), isEmpty = false, Binder())

        assertTrue(activity in stack)
    }

    @Test
    fun testEqualsImpliesHashCode() {
        val activity = mock<Activity>()
        val token = Binder()
        val first = ActivityStack(listOf(activity), isEmpty = false, token)
        val second = ActivityStack(listOf(activity), isEmpty = false, token)

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())

        val anotherToken = Binder()
        val third = ActivityStack(emptyList(), isEmpty = true, anotherToken)

        assertNotEquals(first, third)
        assertNotEquals(first.hashCode(), third.hashCode())
    }

    @Test
    fun testIsEmpty() {
        var stack = ActivityStack(emptyList(), isEmpty = true, Binder())

        assertTrue(stack.isEmpty)

        stack = ActivityStack(emptyList(), isEmpty = false, Binder())

        assertFalse(stack.isEmpty)
    }

    @Test
    fun testToString() {
        val activitiesInProcess = mock<List<Activity>>()
        val isEmpty = false
        val token = Binder()

        val stackString = ActivityStack(activitiesInProcess, isEmpty, token).toString()

        assertTrue(stackString.contains(activitiesInProcess.toString()))
        assertTrue(stackString.contains(isEmpty.toString()))
        assertTrue(stackString.contains(token.toString()))
    }
}
