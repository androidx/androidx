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

package androidx.fragment.app

import androidx.core.view.ViewCompat
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentSharedElementTransitionTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testNestedSharedElementView() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragment = TransitionFragment(R.layout.nested_transition_groups)
            withActivity {
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment).commit()
            }

            val squareContainer = withActivity { findViewById(R.id.squareContainer) }
            var blueSquare = withActivity { findViewById(R.id.blueSquare) }

            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .addSharedElement(squareContainer, "squareContainer")
                    .addSharedElement(blueSquare, "blueSquare")
                    .replace(R.id.content, TransitionFragment(R.layout.nested_transition_groups))
                    .commit()
            }

            blueSquare = withActivity { findViewById(R.id.blueSquare) }

            assertThat(ViewCompat.getTransitionName(blueSquare)).isEqualTo("blueSquare")
        }
    }

    @Test
    fun testNestedSharedElementViewsMoreOutViews() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragment = TransitionFragment(R.layout.scene5)
            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content, fragment)
                    .commit()
            }

            val containerBlueSquare = withActivity { findViewById(R.id.containerBlueSquare) }
            val greenSquare = withActivity { findViewById(R.id.greenSquare) }
            val redSquare = withActivity { findViewById(R.id.redSquare) }
            val startBlueBounds = containerBlueSquare.boundsOnScreen

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(greenSquare, redSquare)
            }
            verifyNoOtherTransitions(fragment)

            val fragment2 = TransitionFragment(R.layout.scene4)

            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(containerBlueSquare, "blueSquare")
                    .replace(R.id.content, fragment2)
                    .commit()
            }

            val blueSquare = withActivity { findViewById(R.id.blueSquare) }

            fragment.exitTransition.verifyAndClearTransition {
                exitingViews += listOf(greenSquare, redSquare)
                epicenter = startBlueBounds
            }
            fragment2.sharedElementEnter.verifyAndClearTransition {
                enteringViews += listOf(blueSquare)
                exitingViews += listOf(containerBlueSquare)
                epicenter = startBlueBounds
            }
            verifyNoOtherTransitions(fragment)
            verifyNoOtherTransitions(fragment2)
        }
    }

    @Test
    fun testNestedSharedElementViewsMoreInViews() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragment = TransitionFragment(R.layout.scene4)
            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content, fragment)
                    .commit()
            }

            val blueSquare = withActivity { findViewById(R.id.blueSquare) }
            val startBlueBounds = blueSquare.boundsOnScreen

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(blueSquare)
            }
            verifyNoOtherTransitions(fragment)

            val fragment2 = TransitionFragment(R.layout.scene6)

            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .addSharedElement(blueSquare, "blueSquare")
                    .replace(R.id.content, fragment2)
                    .commit()
            }

            val containerBlueSquare = withActivity { findViewById(R.id.containerBlueSquare) }

            fragment2.sharedElementEnter.verifyAndClearTransition {
                exitingViews += listOf(blueSquare)
                enteringViews += listOf(containerBlueSquare)
                epicenter = startBlueBounds
            }
            verifyNoOtherTransitions(fragment)
            verifyNoOtherTransitions(fragment2)
        }
    }

    @Test
    fun testNestedTransitionGroupTrue() {
        withUse(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragment = TransitionFragment(R.layout.scene7)
            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content, fragment)
                    .commit()
            }

            val containerBlueSquare = withActivity { findViewById(R.id.containerBlueSquare) }

            fragment.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(containerBlueSquare)
            }
            verifyNoOtherTransitions(fragment)

            val fragment2 = TransitionFragment(R.layout.scene4)

            withActivity {
                supportFragmentManager
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content, fragment2)
                    .commit()
            }

            val blueSquare = withActivity { findViewById(R.id.blueSquare) }

            fragment.exitTransition.verifyAndClearTransition {
                exitingViews += listOf(containerBlueSquare)
            }
            fragment2.enterTransition.verifyAndClearTransition {
                enteringViews += listOf(blueSquare)
            }
            verifyNoOtherTransitions(fragment)
            verifyNoOtherTransitions(fragment2)
        }
    }
}
