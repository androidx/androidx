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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class SplitInfoTest {

    @Test
    fun testSplitInfoContainsActivityFirstStack() {
        val activity = mock<Activity>()
        val firstStack = ActivityStack(listOf(activity))
        val secondStack = ActivityStack(emptyList())
        val info = SplitInfo(firstStack, secondStack, 0.5f)

        assertTrue(info.contains(activity))
    }

    @Test
    fun testSplitInfoContainsActivitySecondStack() {
        val activity = mock<Activity>()
        val firstStack = ActivityStack(emptyList())
        val secondStack = ActivityStack(listOf(activity))
        val info = SplitInfo(firstStack, secondStack, 0.5f)

        assertTrue(info.contains(activity))
    }

    @Test
    fun testEqualsImpliesHashCode() {
        val activity = mock<Activity>()
        val firstStack = ActivityStack(emptyList())
        val secondStack = ActivityStack(listOf(activity))
        val firstInfo = SplitInfo(firstStack, secondStack, 0.5f)
        val secondInfo = SplitInfo(firstStack, secondStack, 0.5f)

        assertEquals(firstInfo, secondInfo)
        assertEquals(firstInfo.hashCode(), secondInfo.hashCode())
    }
}