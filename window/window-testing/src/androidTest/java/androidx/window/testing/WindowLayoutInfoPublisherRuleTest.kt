/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.testing

import android.graphics.Rect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.DisplayFeature
import androidx.window.WindowLayoutInfo
import androidx.window.windowInfoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
public class WindowLayoutInfoPublisherRuleTest {

    private val activityRule = ActivityScenarioRule(TestActivity::class.java)
    private val publisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule
    public val testRule: TestRule

    init {
        testRule = RuleChain.outerRule(publisherRule).around(activityRule)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testWindowLayoutInfo_relayValue(): Unit = runBlockingTest {
        val expected = WindowLayoutInfo.Builder().setDisplayFeatures(emptyList()).build()
        activityRule.scenario.onActivity { activity ->
            val value = async {
                activity.windowInfoRepository().windowLayoutInfo.first()
            }
            publisherRule.overrideWindowLayoutInfo(expected)
            runBlockingTest {
                val actual = value.await()
                assertEquals(expected, actual)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testWindowLayoutInfo_multipleValues(): Unit = runBlockingTest {
        val feature1 = object : DisplayFeature {
            override val bounds: Rect
                get() = Rect()
        }
        val feature2 = object : DisplayFeature {
            override val bounds: Rect
                get() = Rect()
        }
        val expected1 = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature1)).build()
        val expected2 = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature2)).build()
        activityRule.scenario.onActivity { activity ->
            val values = mutableListOf<WindowLayoutInfo>()
            val value = async {
                activity.windowInfoRepository().windowLayoutInfo.take(4).toCollection(values)
            }
            publisherRule.overrideWindowLayoutInfo(expected1)
            publisherRule.overrideWindowLayoutInfo(expected2)
            publisherRule.overrideWindowLayoutInfo(expected1)
            publisherRule.overrideWindowLayoutInfo(expected2)
            runBlockingTest {
                assertEquals(
                    listOf(expected1, expected2, expected1, expected2),
                    value.await().toList()
                )
            }
        }
    }
}