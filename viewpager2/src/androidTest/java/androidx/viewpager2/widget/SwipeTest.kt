/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget

import androidx.viewpager2.widget.ViewPager2.Orientation
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL
import androidx.viewpager2.widget.swipe.BaseActivity
import androidx.viewpager2.widget.swipe.FragmentAdapterActivity
import androidx.viewpager2.widget.swipe.ViewAdapterActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random
import java.util.concurrent.TimeUnit

// region test

const val randomTesting = false // change to true to enable random tests
const val randomTestsPerConfig = 1 // increase to have more random tests generated

@RunWith(Parameterized::class)
class SwipeTest(private val testConfig: TestConfig) : BaseTest() {
    @Test
    fun test() {
        testConfig.apply {
            setUpTest(totalPages, orientation, activityClass).apply {
                val expectedValues = IntArray(totalPages) { it }
                assertBasicState(0)

                pageSequence.forEachIndexed { currentStep, targetPage ->
                    val currentPage = viewPager.currentItem

                    // value change
                    val modifiedPageValue = stepToNewValue[currentStep]
                    if (modifiedPageValue != null) {
                        expectedValues[currentPage] = modifiedPageValue
                        runOnUiThread { activity.updatePageContent(currentPage, modifiedPageValue) }
                    }

                    // config change
                    if (configChangeSteps.contains(currentStep)) {
                        recreateActivity()
                        assertBasicState(currentPage, expectedValues[currentPage])
                    }

                    // page swipe
                    val latch = viewPager.addWaitForScrolledLatch(targetPage)
                    swiper.swipe(currentPage, targetPage)
                    latch.await(1, TimeUnit.SECONDS)
                    assertBasicState(targetPage, expectedValues[targetPage])
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }
}

// endregion

// region test definitions

data class TestConfig(
    val title: String,
    val activityClass: Class<out BaseActivity>,
    @Orientation val orientation: Int,
    val totalPages: Int,
    val pageSequence: List<Int>,
    val configChangeSteps: Set<Int> = emptySet(),
    val stepToNewValue: Map<Int, Int> = emptyMap()
)

private fun createTestSet(): List<TestConfig> {
    return listOf(
            FragmentAdapterActivity::class.java to HORIZONTAL,
            FragmentAdapterActivity::class.java to VERTICAL,
            ViewAdapterActivity::class.java to HORIZONTAL,
            ViewAdapterActivity::class.java to VERTICAL)
            .flatMap { (activity, orientation) -> createTestSet(activity, orientation) }
}

private fun createTestSet(activity: Class<out BaseActivity>, orientation: Int): List<TestConfig> {
    return listOf(
            TestConfig(
                    title = "basic pass",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 4,
                    pageSequence = listOf(0, 1, 2, 3, 3, 2, 1, 0, 0)
            ),
            TestConfig(
                    title = "full pass",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 8,
                    pageSequence = listOf(1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0)
            ),
            TestConfig(
                    title = "swipe beyond edge pages",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 4,
                    pageSequence = listOf(0, 0, 1, 2, 3, 3, 3, 2, 1, 0, 0, 0)
            ),
            TestConfig(
                    title = "config change",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 7,
                    pageSequence = listOf(1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0),
                    configChangeSteps = setOf(3, 5, 7)
            ),
            TestConfig(
                    title = "regression1",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 10,
                    pageSequence = listOf(1, 2, 3, 2, 1, 2, 3, 4)
            ),
            TestConfig(
                    title = "regression2",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 10,
                    pageSequence = listOf(1, 2, 3, 4, 3, 2, 1, 2, 3, 4, 5)
            ),
            TestConfig(
                    title = "regression3",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 10,
                    pageSequence = listOf(1, 2, 3, 2, 1, 2, 3, 2, 1, 0)
            ))
            .plus(if (activity.supportsMutations) createMutationTests(activity,
                    orientation) else emptyList())
            .plus(if (randomTesting) createRandomTests(activity, orientation) else emptyList())
}

// region mutation testing

// Mutation testing verifies that once a view is modified (e.g. text written in an EditBox), the
// values are persisted during page swipes or screen rotations.

val Class<out BaseActivity>.supportsMutations: Boolean
    get() {
        // Mutation testing only applies to Fragment-based adapters
        return this == FragmentAdapterActivity::class.java
    }

private fun createMutationTests(
    activity: Class<out BaseActivity>,
    @Orientation orientation: Int
): List<TestConfig> {
    return listOf(
            TestConfig(
                    title = "mutations",
                    activityClass = activity,
                    orientation = orientation,
                    totalPages = 7,
                    pageSequence = listOf(1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0),
                    configChangeSteps = setOf(8),
                    stepToNewValue = mapOf(0 to 999, 1 to 100, 3 to 300, 5 to 500)
            ))
}

// endregion

// region random testing

private fun createRandomTests(
    activity: Class<out BaseActivity>,
    @Orientation orientation: Int
): List<TestConfig> {
    return (1..randomTestsPerConfig).flatMap {
        listOf(
                createRandomTest(
                        totalPages = 8,
                        sequenceLength = 50,
                        configChangeProbability = 0.0,
                        mutationProbability = 0.0,
                        advanceProbability = 0.875,
                        activityClass = activity,
                        orientation = orientation
                ),
                createRandomTest(
                        totalPages = 8,
                        sequenceLength = 10,
                        configChangeProbability = 0.5,
                        mutationProbability = 0.0,
                        advanceProbability = 0.875,
                        activityClass = activity,
                        orientation = orientation
                ))
            .plus(if (!activity.supportsMutations) emptyList() else listOf(
                createRandomTest(
                        totalPages = 8,
                        sequenceLength = 50,
                        configChangeProbability = 0.0,
                        mutationProbability = 0.125,
                        advanceProbability = 0.875,
                        activityClass = activity,
                        orientation = orientation
                ),
                createRandomTest(
                        totalPages = 8,
                        sequenceLength = 10,
                        configChangeProbability = 0.5,
                        mutationProbability = 0.125,
                        advanceProbability = 0.875,
                        activityClass = activity,
                        orientation = orientation
                )
        ))
    }
}

private fun createRandomTest(
    totalPages: Int,
    sequenceLength: Int,
    configChangeProbability: Double,
    mutationProbability: Double,
    advanceProbability: Double,
    activityClass: Class<out BaseActivity>,
    @Orientation orientation: Int
): TestConfig {
    val random = Random()
    val seed = random.nextLong()
    random.setSeed(seed)

    val pageSequence = mutableListOf<Int>()
    val configChanges = mutableSetOf<Int>()
    val stepToNewValue = mutableMapOf<Int, Int>()

    var pageIx = 0
    var goRightProbability: Double? = null
    for (currentStep in 0 until sequenceLength) {
        if (random.nextDouble() < configChangeProbability) {
            configChanges.add(currentStep)
        }

        if (random.nextDouble() < mutationProbability) {
            stepToNewValue[currentStep] = random.nextInt(10000)
        }

        val goRight: Boolean
        when (pageIx) {
            0 -> {
                goRight = true
                goRightProbability = advanceProbability
            }
            totalPages - 1 -> { // last page
                goRight = false
                goRightProbability = 1 - advanceProbability
            }
            else -> goRight = random.nextDouble() < goRightProbability!!
        }

        pageSequence.add(if (goRight) ++pageIx else --pageIx)
    }

    return TestConfig(
            "random_$seed",
            activityClass,
            orientation,
            totalPages,
            pageSequence,
            configChanges,
            stepToNewValue
    )
}

// endregion

// endregion
