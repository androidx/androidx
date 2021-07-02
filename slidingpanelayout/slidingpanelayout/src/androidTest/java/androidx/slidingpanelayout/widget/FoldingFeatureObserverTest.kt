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

package androidx.slidingpanelayout.widget

import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.FoldingFeature
import androidx.window.WindowLayoutInfo
import androidx.window.testing.FoldingFeature
import androidx.window.testing.WindowLayoutInfoPublisherRule
import androidx.window.windowInfoRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FoldingFeatureObserverTest {
    @get:Rule
    val windowInfoPublisherRule: WindowLayoutInfoPublisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun testNoValuesBeforeSubscribe() {
        val listener = TestListener()
        activityScenarioRule.scenario.onActivity { activity ->
            val observer = FoldingFeatureObserver(activity.windowInfoRepository(), Runnable::run)
            val expected = FoldingFeature(activity = activity)
            val info = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(expected)).build()

            observer.setOnFoldingFeatureChangeListener(listener)
            windowInfoPublisherRule.overrideWindowLayoutInfo(info)

            listener.assertCount(0)
        }
    }

    @Test
    fun testRelaysValuesFromWindowInfoRepo() {
        val listener = TestListener()
        activityScenarioRule.scenario.onActivity { activity ->
            val observer = FoldingFeatureObserver(activity.windowInfoRepository(), Runnable::run)
            val expected = FoldingFeature(activity = activity)
            val info = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(expected)).build()

            observer.setOnFoldingFeatureChangeListener(listener)
            observer.registerLayoutStateChangeCallback()
            windowInfoPublisherRule.overrideWindowLayoutInfo(info)

            listener.assertValue(expected)
        }
    }

    @Test
    fun testRelaysValuesNotRelayedAfterUnsubscribed() {
        val listener = TestListener()
        activityScenarioRule.scenario.onActivity { activity ->
            val observer = FoldingFeatureObserver(activity.windowInfoRepository(), Runnable::run)
            val expected = FoldingFeature(activity = activity)
            val info = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(expected)).build()

            observer.setOnFoldingFeatureChangeListener(listener)
            observer.registerLayoutStateChangeCallback()
            observer.unregisterLayoutStateChangeCallback()
            windowInfoPublisherRule.overrideWindowLayoutInfo(info)

            listener.assertCount(0)
        }
    }

    private class TestListener : FoldingFeatureObserver.OnFoldingFeatureChangeListener {
        private val features = mutableListOf<FoldingFeature>()

        override fun onFoldingFeatureChange(foldingFeature: FoldingFeature) {
            features.add(foldingFeature)
        }

        fun assertCount(count: Int) {
            assertEquals(count, features.size)
        }

        fun assertValue(expected: FoldingFeature) {
            assertCount(1)
            assertEquals(expected, features.first())
        }
    }
}