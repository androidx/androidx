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

package androidx.window.testing.layout

import android.graphics.Rect
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.layout.DisplayFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import androidx.window.testing.TestActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@LargeTest
@RunWith(AndroidJUnit4::class)
public class WindowLayoutInfoPublisherRuleTest {

    private val activityRule = ActivityScenarioRule(TestActivity::class.java)
    private val publisherRule = WindowLayoutInfoPublisherRule()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val testScope = TestCoroutineScope()

    @get:Rule
    public val testRule: TestRule

    init {
        testRule = RuleChain.outerRule(publisherRule).around(activityRule)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testWindowLayoutInfo_relayValue(): Unit = testScope.runBlockingTest {
        val expected = WindowLayoutInfo(emptyList())
        activityRule.scenario.onActivity { activity ->
            val value = testScope.async {
                WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).first()
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
    public fun testException_resetsFactoryMethod() {
        ActivityScenario.launch(TestActivity::class.java).onActivity { activity ->
            WindowInfoTracker.reset()
            try {
                WindowLayoutInfoPublisherRule().apply(
                    object : Statement() {
                        override fun evaluate() {
                            throw TestException
                        }
                    },
                    Description.EMPTY
                ).evaluate()
            } catch (e: TestException) {
                // Throw unexpected exceptions.
            }
            assertFalse(WindowInfoTracker.getOrCreate(activity) is PublishLayoutInfoTracker)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testWindowLayoutInfo_multipleValues(): Unit = testScope.runBlockingTest {
        val feature1 = object : DisplayFeature {
            override val bounds: Rect
                get() = Rect()
        }
        val feature2 = object : DisplayFeature {
            override val bounds: Rect
                get() = Rect()
        }
        val expected1 = WindowLayoutInfo(listOf(feature1))
        val expected2 = WindowLayoutInfo(listOf(feature2))
        activityRule.scenario.onActivity { activity ->
            val values = mutableListOf<WindowLayoutInfo>()
            val value = testScope.async {
                WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).take(4)
                    .toCollection(values)
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

    private object TestException : Exception("TEST EXCEPTION")
}