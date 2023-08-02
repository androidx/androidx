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
package androidx.window.integration.layout

import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.graphics.Rect
import androidx.lifecycle.lifecycleScope
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.integration.TestActivity
import androidx.window.integration.TestConsumer
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.WindowInfoTracker
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** A collection of end to end tests for [WindowInfoTracker] to ensure correct behavior. */
@ExperimentalCoroutinesApi
class WindowInfoTrackerEndToEndTest {

    // Specify the scope for handling coroutines.
    private val testScope = TestScope(StandardTestDispatcher())

    // Specify the rules to launch/close an activity before/after each test.
    @get:Rule
    val openActivityRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    /** Checks that the [DisplayFeature]'s transform appropriately upon screen rotation. */
    @Test
    fun verifyDisplayFeatures_ScreenRotation() = testScope.runTest {
        // Initialize a collector that stores the DisplayFeatures from an Activity.
        // The DisplayFeatures will later be compared across different screen orientations.
        val expectedLayoutsCollected = 1
        val featureCollectorPortrait = TestConsumer<List<DisplayFeature>>(
            count = expectedLayoutsCollected)
        val featureCollectorLandscape = TestConsumer<List<DisplayFeature>>(
            count = expectedLayoutsCollected)

        // Set the screen orientation to portrait and collect its DisplayFeatures.
        setScreenOrientation(SCREEN_ORIENTATION_PORTRAIT)
        getFirstDisplayFeature(featureCollectorPortrait)
        // Assertion: there is only one value in the collector (from portrait mode).
        featureCollectorPortrait.waitForValueCount()
        // Get the DisplayFeatures from the portrait layout.
        val displayFeaturesPortrait = featureCollectorPortrait.get(valueIndex = 0)

        // Change the screen orientation to landscape and collect its DisplayFeatures.
        setScreenOrientation(SCREEN_ORIENTATION_LANDSCAPE)
        getFirstDisplayFeature(featureCollectorLandscape)
        // Assertion: there are two values in the collector (from portrait and landscape mode).
        featureCollectorLandscape.waitForValueCount()
        // Get the DisplayFeatures from the landscape layout.
        val displayFeaturesLandscape = featureCollectorLandscape.get(valueIndex = 0)

        // Assertion: the number of features in both layouts are the same.
        assertEquals(displayFeaturesPortrait.size, displayFeaturesLandscape.size)
        // Assertion: the number of FoldingFeatures in both layouts are the same
        assertEquals(displayFeaturesPortrait.filterIsInstance<FoldingFeature>().size,
            displayFeaturesLandscape.filterIsInstance<FoldingFeature>().size)
        // Check that the properties of each DisplayFeature is valid.
        val featureStateCounterPortrait = validateDisplayFeatures(displayFeaturesPortrait)
        val featureStateCounterLandscape = validateDisplayFeatures(displayFeaturesLandscape)
        // Verify that the expected counts of FoldingFeature properties is consistent.
        assertEquals(featureStateCounterPortrait, featureStateCounterLandscape)
    }

    /** Changes the screen orientation and waits for the rotation to occur. */
    private fun setScreenOrientation(screenOrientation: Int) {
        // Start the activity using the ActivityScenarioRule and set the orientation.
        openActivityRule.scenario.onActivity { activity: TestActivity ->
            // Initiate a timer and wait for the screen to rotate.
            activity.resetLayoutCounter()
            activity.requestedOrientation = screenOrientation
            activity.waitForLayout()

            // Assertion: the screen has properly rotated (the internal value is set).
            assertEquals(
                "Expected the Screen to Rotate to state $screenOrientation;",
                activity.requestedOrientation, screenOrientation
            )
        }
    }

    /**
     * Extracts a list of [DisplayFeature]s from a [TestActivity] using the first window layout.
     * Stores the [DisplayFeature]s into a collector for later analysis after the scope ends.
     */
    private fun getFirstDisplayFeature(featureCollector: TestConsumer<List<DisplayFeature>>) {
        // Start the activity using the ActivityScenarioRule and store its DisplayFeatures.
        openActivityRule.scenario.onActivity { activity: TestActivity ->
            activity.lifecycleScope.launch {
                // Take the first WindowLayoutInfo from the Flow stored in WindowInfoTracker.
                val layoutInfo = WindowInfoTracker.getOrCreate(activity)
                    .windowLayoutInfo(activity).first()

                // Store the DisplayFeatures for further analysis after the coroutine is closed.
                featureCollector.accept(layoutInfo.displayFeatures)
            }
        }
    }

    /**
     * Checks that the [DisplayFeature]'s bounds have non-negative area
     * and at least one positive dimension.
     */
    private fun validateDisplayFeatureBounds(displayFeatureBounds: Rect) {
        // Assert that the DisplayFeature has positive dimensions.
        assertFalse("Error: a display feature was found with negative dimensions.",
            displayFeatureBounds.width() < 0 || displayFeatureBounds.height() < 0)
        // Assert that the DisplayFeature has at least one dimension.
        assertFalse("Error: a display feature was found with zero area.",
            displayFeatureBounds.width() == 0 && displayFeatureBounds.height() == 0)
    }

    /** A class to keep track of the number of each [FoldingFeature]'s state. */
    data class FoldingFeatureStateCounter(var flatCount: Int, var halfOpenedCount: Int)

    /**
     * Checks each [DisplayFeature]'s properties to make sure it is a valid [DisplayFeature].
     */
    private fun validateDisplayFeatures(
        displayFeatures: List<DisplayFeature>
    ): FoldingFeatureStateCounter {
        // Create a counter to count the states for the features in displayFeatures.
        val foldingFeatureStateCounter = FoldingFeatureStateCounter(
            flatCount = 0,
            halfOpenedCount = 0
        )

        // Loop through each DisplayFeature and verify its properties.
        for (displayFeature in displayFeatures) {
            // DisplayFeature bounds must be 1-dimensional, nonnull, and positive.
            validateDisplayFeatureBounds(displayFeature.bounds)

            // If the DisplayFeature is a FoldingFeature, validate the FoldingFeature properties.
            if (displayFeature is FoldingFeature) {
                // Keep a running counter of each state to compare them between orientations.
                when (displayFeature.state) {
                    FLAT -> {
                        foldingFeatureStateCounter.flatCount++
                    }
                    HALF_OPENED -> {
                        foldingFeatureStateCounter.halfOpenedCount++
                    }
                    else -> {
                        fail("The FoldingFeature state ${displayFeature.state} has not been " +
                            "added to the list of checked states. Please add the state and retry.")
                    }
                }
            }
        }
        return foldingFeatureStateCounter
    }
}