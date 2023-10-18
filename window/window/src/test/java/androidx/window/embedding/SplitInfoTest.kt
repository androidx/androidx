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
import android.os.IBinder
import androidx.window.embedding.EmbeddingAdapter.Companion.INVALID_ACTIVITY_STACK_TOKEN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

/** Unit tests for [SplitInfo] */
class SplitInfoTest {

    @Test
    fun testSplitInfoContainsActivityFirstStack() {
        val activity = mock<Activity>()
        val firstStack = createTestActivityStack(listOf(activity))
        val secondStack = createTestActivityStack(emptyList())
        val attributes = SplitAttributes()
        val token = Binder()
        val info = SplitInfo(firstStack, secondStack, attributes, token)

        assertTrue(info.contains(activity))
    }

    @Test
    fun testSplitInfoContainsActivitySecondStack() {
        val activity = mock<Activity>()
        val firstStack = createTestActivityStack(emptyList())
        val secondStack = createTestActivityStack(listOf(activity))
        val attributes = SplitAttributes()
        val token = Binder()
        val info = SplitInfo(firstStack, secondStack, attributes, token)

        assertTrue(info.contains(activity))
    }

    @Test
    fun testEqualsImpliesHashCode() {
        val activity = mock<Activity>()
        val firstStack = createTestActivityStack(emptyList())
        val secondStack = createTestActivityStack(listOf(activity))
        val attributes = SplitAttributes()
        val token = Binder()
        val firstInfo = SplitInfo(firstStack, secondStack, attributes, token)
        val secondInfo = SplitInfo(firstStack, secondStack, attributes, token)

        assertEquals(firstInfo, secondInfo)
        assertEquals(firstInfo.hashCode(), secondInfo.hashCode())
    }

    @Test
    fun testSplitInfoProperties() {
        val activity = mock<Activity>()
        val firstStack = createTestActivityStack(emptyList())
        val secondStack = createTestActivityStack(listOf(activity))
        val attributes = SplitAttributes()
        val token = Binder()
        val splitInfo = SplitInfo(firstStack, secondStack, attributes, token)

        assertEquals(firstStack, splitInfo.primaryActivityStack)
        assertEquals(secondStack, splitInfo.secondaryActivityStack)
        assertEquals(attributes, splitInfo.splitAttributes)
    }

    @Test
    fun testToString() {
        val activity = mock<Activity>()
        val firstStack = createTestActivityStack(emptyList())
        val secondStack = createTestActivityStack(listOf(activity))
        val attributes = SplitAttributes()
        val token = Binder()
        val splitInfoString = SplitInfo(firstStack, secondStack, attributes, token).toString()

        assertTrue(splitInfoString.contains(firstStack.toString()))
        assertTrue(splitInfoString.contains(secondStack.toString()))
        assertTrue(splitInfoString.contains(attributes.toString()))
        assertTrue(splitInfoString.contains(token.toString()))
    }

    private fun createTestActivityStack(
        activitiesInProcess: List<Activity>,
        isEmpty: Boolean = false,
        token: IBinder = INVALID_ACTIVITY_STACK_TOKEN,
    ): ActivityStack = ActivityStack(activitiesInProcess, isEmpty, token)
}
