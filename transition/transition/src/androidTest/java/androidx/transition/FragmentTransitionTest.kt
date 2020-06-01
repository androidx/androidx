/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.transition

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.waitForExecution
import androidx.transition.test.R
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class FragmentTransitionTest(private val reorderingAllowed: Boolean) {

    @get:Rule
    val activityRule = ActivityTestRule(FragmentTransitionTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var fragmentManager: FragmentManager
    private var onBackStackChangedTimes: Int = 0
    private val onBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { onBackStackChangedTimes++ }

    @Before
    fun setup() {
        onBackStackChangedTimes = 0
        fragmentManager = activityRule.activity.supportFragmentManager
        fragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)
    }

    @After
    fun teardown() {
        fragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
    }

    // Test that normal view transitions (enter, exit, reenter, return) run with
    // a single fragment.
    @Test
    fun enterExitTransitions() {
        // enter transition
        val fragment = setupInitialFragment()
        val blue = activityRule.findBlue()
        val green = activityRule.findGreen()

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .remove(fragment)
            .addToBackStack(null)
            .commit()

        fragment.waitForTransition()
        fragment.exitTransition.verifyAndClearTransition {
            exitingViews += listOf(green, blue)
        }
        verifyNoOtherTransitions(fragment)
        assertThat(onBackStackChangedTimes).isEqualTo(2)

        // reenter transition
        activityRule.popBackStackImmediate()
        fragment.waitForTransition()
        val green2 = activityRule.findGreen()
        val blue2 = activityRule.findBlue()
        fragment.reenterTransition.verifyAndClearTransition {
            enteringViews += listOf(green2, blue2)
        }
        verifyNoOtherTransitions(fragment)
        assertThat(onBackStackChangedTimes).isEqualTo(3)

        // return transition
        activityRule.popBackStackImmediate()
        fragment.waitForTransition()
        fragment.returnTransition.verifyAndClearTransition {
            exitingViews += listOf(green2, blue2)
        }
        verifyNoOtherTransitions(fragment)
        assertThat(onBackStackChangedTimes).isEqualTo(4)
    }

    // Test removing a Fragment with a Transition and adding it back before the Transition
    // finishes is handled correctly.
    @Test
    fun removeThenAddBeforeTransitionFinishes() {
        // enter transition
        val fragment = setupInitialFragment()
        val blue = activityRule.findBlue()
        val green = activityRule.findGreen()

        val view1 = fragment.view

        activityRule.runOnUiThread {
            // exit transition
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .remove(fragment)
                .addToBackStack(null)
                .commit()

            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        activityRule.waitForExecution()

        // If reordering is allowed, the remove is ignored and the transaction is just added to the
        // back stack
        if (reorderingAllowed) {
            assertThat(onBackStackChangedTimes).isEqualTo(2)
            assertThat(fragment.requireView()).isEqualTo(view1)
        } else {
            // If reorder is not allowed we will get the exit Transition and the fragment will be
            // added with a different view.
            fragment.waitForTransition()
            fragment.exitTransition.verifyAndClearTransition {
                exitingViews += listOf(green, blue)
            }
            assertThat(onBackStackChangedTimes).isEqualTo(3)
            assertThat(fragment.requireView()).isNotEqualTo(view1)
        }
        verifyNoOtherTransitions(fragment)
    }

    @Test
    fun ensureTransitionsFinishBeforeViewDestroyed() {
        // enter transition
        val fragment = TransitionFinishFirstFragment()
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        fragment.waitForTransition()
        val blueSquare1 = activityRule.findBlue()
        val greenSquare1 = activityRule.findGreen()
        fragment.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(blueSquare1, greenSquare1)
        }
        verifyNoOtherTransitions(fragment)

        // Ensure that our countdown latch has been reset for the Fragment
        assertThat(fragment.endTransitionCountDownLatch.count).isEqualTo(1)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .replace(R.id.fragmentContainer, TransitionFragment())
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        assertWithMessage("Timed out waiting for onDestroyView")
            .that(fragment.onDestroyViewCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        // Ensure all transitions have been executed before onDestroyView was called
        assertThat(fragment.transitionCountInOnDestroyView).isEqualTo(0)
    }

    // Test that shared elements transition from one fragment to the next
    // and back during pop.
    @Test
    fun sharedElement() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        verifyTransition(fragment1, fragment2, "blueSquare")

        // Now pop the back stack
        verifyPopTransition(1, fragment2, fragment1)
    }

    // Test that shared elements transition from one fragment to the next
    // and back during pop.
    @Suppress("DEPRECATION")
    @Test
    fun sharedElementWithTargetFragment() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)
        fragment2.setTargetFragment(fragment1, 13)

        verifyTransition(fragment1, fragment2, "blueSquare")

        // Now pop the back stack
        verifyPopTransition(1, fragment2, fragment1)
    }

    // Test that shared element transitions through multiple fragments work together
    @Test
    fun intermediateFragment() {
        val fragment1 = setupInitialFragment()

        val fragment2 = TransitionFragment(R.layout.fragment_scene3)

        verifyTransition(fragment1, fragment2, "shared")

        val fragment3 = TransitionFragment(R.layout.fragment_scene2)

        verifyTransition(fragment2, fragment3, "blueSquare")

        // Should transfer backwards when popping multiple:
        verifyPopTransition(2, fragment3, fragment1, fragment2)
    }

    // Adding/removing the same fragment multiple times shouldn't mess anything up
    @Test
    fun removeAdded() {
        val fragment1 = setupInitialFragment()

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()

        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        instrumentation.runOnMainSync {
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .replace(R.id.fragmentContainer, fragment2)
                .replace(R.id.fragmentContainer, fragment1)
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
        }
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(2)

        // should be a normal transition from fragment1 to fragment2
        fragment2.waitForTransition()
        val endBlue = activityRule.findBlue()
        val endGreen = activityRule.findGreen()
        fragment1.exitTransition.verifyAndClearTransition {
            exitingViews += listOf(startBlue, startGreen)
        }
        fragment2.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(endBlue, endGreen)
        }
        verifyNoOtherTransitions(fragment1)
        verifyNoOtherTransitions(fragment2)

        // Pop should also do the same thing
        activityRule.popBackStackImmediate()
        assertThat(onBackStackChangedTimes).isEqualTo(3)

        fragment1.waitForTransition()
        fragment2.waitForTransition()
        val popBlue = activityRule.findBlue()
        val popGreen = activityRule.findGreen()
        fragment1.reenterTransition.verifyAndClearTransition {
            enteringViews += listOf(popBlue, popGreen)
        }
        fragment2.returnTransition.verifyAndClearTransition {
            exitingViews += listOf(endBlue, endGreen)
        }
        verifyNoOtherTransitions(fragment1)
        verifyNoOtherTransitions(fragment2)
    }

    // Make sure that shared elements on two different fragment containers don't interact
    @Test
    fun crossContainer() {
        activityRule.setContentView(R.layout.double_container)
        val fragment1 = TransitionFragment(R.layout.fragment_scene1)
        val fragment2 = TransitionFragment(R.layout.fragment_scene1)
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer1, fragment1)
            .add(R.id.fragmentContainer2, fragment2)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)

        fragment1.waitForTransition()
        val greenSquare1 = findViewById(fragment1, R.id.greenSquare)
        val blueSquare1 = findViewById(fragment1, R.id.blueSquare)
        fragment1.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(greenSquare1, blueSquare1)
        }
        verifyNoOtherTransitions(fragment1)
        fragment2.waitForTransition()
        val greenSquare2 = findViewById(fragment2, R.id.greenSquare)
        val blueSquare2 = findViewById(fragment2, R.id.blueSquare)
        fragment2.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(greenSquare2, blueSquare2)
        }
        verifyNoOtherTransitions(fragment2)

        // Make sure the correct transitions are run when the target names
        // are different in both shared elements. We may fool the system.
        verifyCrossTransition(false, fragment1, fragment2)

        // Make sure the correct transitions are run when the source names
        // are different in both shared elements. We may fool the system.
        verifyCrossTransition(true, fragment1, fragment2)
    }

    // Make sure that onSharedElementStart and onSharedElementEnd are called
    @Suppress("UNCHECKED_CAST")
    @Test
    fun callStartEndWithSharedElements() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        val enterCallback = mock(SharedElementCallback::class.java)
        fragment2.setEnterSharedElementCallback(enterCallback)

        val startBlue = activityRule.findBlue()

        verifyTransition(fragment1, fragment2, "blueSquare")

        val names = ArgumentCaptor.forClass(List::class.java as Class<List<String>>)
        val views = ArgumentCaptor.forClass(List::class.java as Class<List<View>>)
        val snapshots = ArgumentCaptor.forClass(List::class.java as Class<List<View>>)
        verify(enterCallback).onSharedElementStart(
            names.capture(), views.capture(),
            snapshots.capture()
        )
        assertThat(names.value.size).isEqualTo(1)
        assertThat(views.value.size).isEqualTo(1)
        assertThat(snapshots.value).isNull()
        assertThat(names.value[0]).isEqualTo("blueSquare")
        assertThat(views.value[0]).isEqualTo(startBlue)

        val endBlue = activityRule.findBlue()

        verify(enterCallback).onSharedElementEnd(
            names.capture(), views.capture(),
            snapshots.capture()
        )
        assertThat(names.value.size).isEqualTo(1)
        assertThat(views.value.size).isEqualTo(1)
        assertThat(snapshots.value).isNull()
        assertThat(names.value[0]).isEqualTo("blueSquare")
        assertThat(views.value[0]).isEqualTo(endBlue)

        // Now pop the back stack
        reset(enterCallback)
        verifyPopTransition(1, fragment2, fragment1)

        verify(enterCallback).onSharedElementStart(
            names.capture(), views.capture(),
            snapshots.capture()
        )
        assertThat(names.value.size).isEqualTo(1)
        assertThat(views.value.size).isEqualTo(1)
        assertThat(snapshots.value).isNull()
        assertThat(names.value[0]).isEqualTo("blueSquare")
        assertThat(views.value[0]).isEqualTo(endBlue)

        val reenterBlue = activityRule.findBlue()

        verify(enterCallback).onSharedElementEnd(
            names.capture(), views.capture(),
            snapshots.capture()
        )
        assertThat(names.value.size).isEqualTo(1)
        assertThat(views.value.size).isEqualTo(1)
        assertThat(snapshots.value).isNull()
        assertThat(names.value[0]).isEqualTo("blueSquare")
        assertThat(views.value[0]).isEqualTo(reenterBlue)
    }

    // Make sure that onMapSharedElement works to change the shared element going out
    @Test
    fun onMapSharedElementOut() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()

        val startGreenBounds = startGreen.boundsOnScreen

        val mapOut = object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>,
                sharedElements: MutableMap<String, View>
            ) {
                assertThat(names.size).isEqualTo(1)
                assertThat(names[0]).isEqualTo("blueSquare")
                assertThat(sharedElements.size).isEqualTo(1)
                assertThat(sharedElements["blueSquare"]).isEqualTo(startBlue)
                sharedElements["blueSquare"] = startGreen
            }
        }
        fragment1.setExitSharedElementCallback(mapOut)

        fragmentManager.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .setReorderingAllowed(reorderingAllowed)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val endBlue = activityRule.findBlue()
        val endBlueBounds = endBlue.boundsOnScreen

        fragment2.sharedElementEnter.verifyAndClearTransition {
            epicenter = startGreenBounds
            exitingViews += startGreen
            enteringViews += endBlue
        }

        val mapBack = object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>,
                sharedElements: MutableMap<String, View>
            ) {
                assertThat(names.size).isEqualTo(1)
                assertThat(names[0]).isEqualTo("blueSquare")
                assertThat(sharedElements.size).isEqualTo(1)
                val expectedBlue = findViewById(fragment1, R.id.blueSquare)
                assertThat(sharedElements["blueSquare"]).isEqualTo(expectedBlue)
                val greenSquare = findViewById(fragment1, R.id.greenSquare)
                sharedElements["blueSquare"] = greenSquare
            }
        }
        fragment1.setExitSharedElementCallback(mapBack)

        activityRule.popBackStackImmediate()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val reenterGreen = activityRule.findGreen()
        fragment2.sharedElementReturn.verifyAndClearTransition {
            epicenter = endBlueBounds
            exitingViews += endBlue
            enteringViews += reenterGreen
        }
    }

    // Make sure that onMapSharedElement works to change the shared element target
    @Test
    fun onMapSharedElementIn() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        val startBlue = activityRule.findBlue()
        val startBlueBounds = startBlue.boundsOnScreen

        val mapIn = object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>,
                sharedElements: MutableMap<String, View>
            ) {
                assertThat(names.size).isEqualTo(1)
                assertThat(names[0]).isEqualTo("blueSquare")
                assertThat(sharedElements.size).isEqualTo(1)
                val blueSquare = findViewById(fragment2, R.id.blueSquare)
                assertThat(sharedElements["blueSquare"]).isEqualTo(blueSquare)
                val greenSquare = findViewById(fragment2, R.id.greenSquare)
                sharedElements["blueSquare"] = greenSquare
            }
        }
        fragment2.setEnterSharedElementCallback(mapIn)

        fragmentManager.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .setReorderingAllowed(reorderingAllowed)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val endGreen = activityRule.findGreen()
        val endBlue = activityRule.findBlue()
        val endGreenBounds = endGreen.boundsOnScreen

        fragment2.sharedElementEnter.verifyAndClearTransition {
            epicenter = startBlueBounds
            exitingViews += startBlue
            enteringViews += endGreen
        }

        val mapBack = object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>,
                sharedElements: MutableMap<String, View>
            ) {
                assertThat(names.size).isEqualTo(1)
                assertThat(names[0]).isEqualTo("blueSquare")
                assertThat(sharedElements.size).isEqualTo(1)
                assertThat(sharedElements["blueSquare"]).isEqualTo(endBlue)
                sharedElements["blueSquare"] = endGreen
            }
        }
        fragment2.setEnterSharedElementCallback(mapBack)

        activityRule.popBackStackImmediate()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val reenterBlue = activityRule.findBlue()
        fragment2.sharedElementReturn.verifyAndClearTransition {
            epicenter = endGreenBounds
            exitingViews += endGreen
            enteringViews += reenterBlue
        }
    }

    // Ensure that shared element transitions that have targets properly target the views
    @Test
    fun complexSharedElementTransition() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = ComplexTransitionFragment()

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

        fragmentManager.beginTransaction()
            .addSharedElement(startBlue, "blueSquare")
            .addSharedElement(startGreen, "greenSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .setReorderingAllowed(true)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(2)

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val endBlue = activityRule.findBlue()
        val endGreen = activityRule.findGreen()
        val endBlueBounds = endBlue.boundsOnScreen

        fragment2.sharedElementEnterTransition1.verifyAndClearTransition {
            epicenter = startBlueBounds
            exitingViews += startBlue
            enteringViews += endBlue
        }
        fragment2.sharedElementEnterTransition2.verifyAndClearTransition {
            epicenter = startBlueBounds
            exitingViews += startGreen
            enteringViews += endGreen
        }

        // Now see if it works when popped
        activityRule.popBackStackImmediate()
        assertThat(onBackStackChangedTimes).isEqualTo(3)

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val reenterBlue = activityRule.findBlue()
        val reenterGreen = activityRule.findGreen()

        fragment2.sharedElementReturnTransition1.verifyAndClearTransition {
            epicenter = endBlueBounds
            exitingViews += endBlue
            enteringViews += reenterBlue
        }
        fragment2.sharedElementReturnTransition2.verifyAndClearTransition {
            epicenter = endBlueBounds
            exitingViews += endGreen
            enteringViews += reenterGreen
        }
    }

    // Ensure that after transitions have executed that they don't have any targets or other
    // unfortunate modifications.
    @Test
    fun transitionsEndUnchanged() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        verifyTransition(fragment1, fragment2, "blueSquare")
        assertThat(fragment1.exitTransition.targets.size).isEqualTo(0)
        assertThat(fragment2.sharedElementEnter.targets.size).isEqualTo(0)
        assertThat(fragment2.enterTransition.targets.size).isEqualTo(0)
        assertThat(fragment1.exitTransition.epicenterCallback).isNull()
        assertThat(fragment2.enterTransition.epicenterCallback).isNull()
        assertThat(fragment2.sharedElementEnter.epicenterCallback).isNull()

        // Now pop the back stack
        verifyPopTransition(1, fragment2, fragment1)

        assertThat(fragment2.returnTransition.targets.size).isEqualTo(0)
        assertThat(fragment2.sharedElementReturn.targets.size).isEqualTo(0)
        assertThat(fragment1.reenterTransition.targets.size).isEqualTo(0)
        assertThat(fragment2.returnTransition.epicenterCallback).isNull()
        assertThat(fragment2.sharedElementReturn.epicenterCallback).isNull()
        assertThat(fragment2.reenterTransition.epicenterCallback).isNull()
    }

    // Ensure that transitions are done when a fragment is shown and hidden
    @Test
    fun showHideTransition() {
        val fragment1 = setupInitialFragment()
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        val listener = TestShowHideTransitionListener(fragment1)

        fragment1.exitTransition = TrackingVisibility().apply {
            setRealTransition(true)
            addListener(listener)
        }

        fragment1.setExitTransition(fragment1.exitTransition)

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment2)
            .hide(fragment1)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()
        fragment1.waitForTransition()
        fragment2.waitForTransition()
        fragment1.exitTransition.endAnimatorCountDownLatch.await(1000, TimeUnit.MILLISECONDS)

        val endGreen = findViewById(fragment2, R.id.greenSquare)
        val endBlue = findViewById(fragment2, R.id.blueSquare)

        assertThat(fragment1.requireView().visibility).isEqualTo(View.GONE)
        assertThat(startGreen.visibility).isEqualTo(View.VISIBLE)
        assertThat(startBlue.visibility).isEqualTo(View.VISIBLE)

        fragment1.exitTransition.verifyAndClearTransition {
            exitingViews += listOf(startGreen, startBlue)
        }
        verifyNoOtherTransitions(fragment1)

        fragment2.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(endGreen, endBlue)
        }
        verifyNoOtherTransitions(fragment2)

        activityRule.popBackStackImmediate()

        activityRule.waitForExecution()
        fragment1.waitForTransition()
        fragment2.waitForTransition()

        fragment1.reenterTransition.verifyAndClearTransition {
            enteringViews += listOf(startGreen, startBlue)
        }
        verifyNoOtherTransitions(fragment1)

        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(startGreen.visibility).isEqualTo(View.VISIBLE)
        assertThat(startBlue.visibility).isEqualTo(View.VISIBLE)

        fragment2.returnTransition.verifyAndClearTransition {
            exitingViews += listOf(endGreen, endBlue)
        }
        verifyNoOtherTransitions(fragment2)
    }

    // Ensure that transitions are done when a fragment is attached and detached
    @Test
    fun attachDetachTransition() {
        val fragment1 = setupInitialFragment()
        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment2)
            .detach(fragment1)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()

        val endGreen = findViewById(fragment2, R.id.greenSquare)
        val endBlue = findViewById(fragment2, R.id.blueSquare)

        fragment1.exitTransition.verifyAndClearTransition {
            exitingViews += listOf(startGreen, startBlue)
        }
        verifyNoOtherTransitions(fragment1)

        fragment2.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(endGreen, endBlue)
        }
        verifyNoOtherTransitions(fragment2)

        activityRule.popBackStackImmediate()

        activityRule.waitForExecution()

        val reenterBlue = activityRule.findBlue()
        val reenterGreen = activityRule.findGreen()

        fragment1.reenterTransition.verifyAndClearTransition {
            enteringViews += listOf(reenterGreen, reenterBlue)
        }
        verifyNoOtherTransitions(fragment1)

        fragment2.returnTransition.verifyAndClearTransition {
            exitingViews += listOf(endGreen, endBlue)
        }
        verifyNoOtherTransitions(fragment2)
    }

    // Ensure that shared element without matching transition name doesn't error out
    @Test
    fun sharedElementMismatch() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.scene2)

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

        fragmentManager.beginTransaction()
            .addSharedElement(startBlue, "fooSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .setReorderingAllowed(reorderingAllowed)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val endBlue = activityRule.findBlue()
        val endGreen = activityRule.findGreen()

        if (reorderingAllowed) {
            fragment1.exitTransition.verifyAndClearTransition {
                exitingViews += listOf(startGreen, startBlue)
            }
        } else {
            fragment1.exitTransition.verifyAndClearTransition {
                epicenter = startBlueBounds
                exitingViews += startGreen
            }
            fragment2.sharedElementEnter.verifyAndClearTransition {
                epicenter = startBlueBounds
                exitingViews += startBlue
            }
        }
        verifyNoOtherTransitions(fragment1)

        fragment2.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(endGreen, endBlue)
        }
        verifyNoOtherTransitions(fragment2)
    }

    // Ensure that using the same source or target shared element results in an exception.
    @Test
    fun sharedDuplicateTargetNames() {
        setupInitialFragment()

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()

        val ft = fragmentManager.beginTransaction()
        ft.addSharedElement(startBlue, "blueSquare")
        try {
            ft.addSharedElement(startGreen, "blueSquare")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains("A shared element with the target name 'blueSquare' " +
                        "has already been added to the transaction.")
        }

        try {
            ft.addSharedElement(startBlue, "greenSquare")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains("A shared element with the source name 'blueSquare' " +
                        "has already been added to the transaction.")
        }
    }

    // Test that invisible fragment views don't participate in transitions
    @Test
    fun invisibleNoTransitions() {
        if (!reorderingAllowed) {
            return // only reordered transitions can avoid interaction
        }
        // enter transition
        val fragment = InvisibleFragment()
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        fragment.waitForNoTransition()
        verifyNoOtherTransitions(fragment)

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .remove(fragment)
            .addToBackStack(null)
            .commit()

        fragment.waitForNoTransition()
        verifyNoOtherTransitions(fragment)

        // reenter transition
        activityRule.popBackStackImmediate()
        fragment.waitForNoTransition()
        verifyNoOtherTransitions(fragment)

        // return transition
        activityRule.popBackStackImmediate()
        fragment.waitForNoTransition()
        verifyNoOtherTransitions(fragment)
    }

    // No crash when transitioning a shared element and there is no shared element transition.
    @Test
    fun noSharedElementTransition() {
        val fragment1 = setupInitialFragment()

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startBlueBounds = startBlue.boundsOnScreen

        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()

        fragment1.waitForTransition()
        fragment2.waitForTransition()
        val midGreen = activityRule.findGreen()
        val midBlue = activityRule.findBlue()
        val midBlueBounds = midBlue.boundsOnScreen
        fragment1.exitTransition.verifyAndClearTransition {
            epicenter = startBlueBounds
            exitingViews += startGreen
        }
        fragment2.sharedElementEnter.verifyAndClearTransition {
            epicenter = startBlueBounds
            exitingViews += startBlue
            enteringViews += midBlue
        }
        fragment2.enterTransition.verifyAndClearTransition {
            epicenter = midBlueBounds
            enteringViews += midGreen
        }
        verifyNoOtherTransitions(fragment1)
        verifyNoOtherTransitions(fragment2)

        val fragment3 = TransitionFragment(R.layout.fragment_scene3)

        activityRule.runOnUiThread {
            val fm = activityRule.activity.supportFragmentManager
            fm.popBackStack()
            fm.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack(null)
                .commit()
        }

        // This shouldn't give an error.
        activityRule.executePendingTransactions()

        fragment2.waitForTransition()
        // It does not transition properly for ordered transactions, though.
        if (reorderingAllowed) {
            // reordering allowed fragment3 to get a transition so we should wait for it to finish
            fragment3.waitForTransition()
            fragment2.returnTransition.verifyAndClearTransition {
                exitingViews += listOf(midGreen, midBlue)
            }
            val endGreen = activityRule.findGreen()
            val endBlue = activityRule.findBlue()
            val endRed = activityRule.findRed()
            fragment3.enterTransition.verifyAndClearTransition {
                enteringViews += listOfNotNull(endGreen, endBlue, endRed)
            }
            verifyNoOtherTransitions(fragment2)
            verifyNoOtherTransitions(fragment3)
        } else {
            // The pop transition will be executed so we should wait until fragment 1 finishes
            fragment1.waitForTransition()
            // fragment3 doesn't get a transition since it conflicts with the pop transition
            verifyNoOtherTransitions(fragment3)
            // Everything else is just doing its best. Ordered transactions can't handle
            // multiple transitions acting together except for popping multiple together.
        }
    }

    // No crash when there is no shared element transition and transitioning a shared element after
    // a pop
    @Test
    fun noSharedElementTransitionOnPop() {
        val fragment1 = setupInitialFragment()

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startGreenBounds = startGreen.boundsOnScreen

        val fragment2 = TransitionFragment(R.layout.fragment_scene2)

        activityRule.runOnUiThread {
            fragmentManager.popBackStack()
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit()
        }

        fragment1.waitForTransition()

        // This shouldn't give an error.
        activityRule.executePendingTransactions()

        // It does not transition properly for ordered transactions, though.
        if (reorderingAllowed) {
            fragment1.returnTransition.verifyAndClearTransition {
                exitingViews += startGreen
            }
            val endGreen = activityRule.findGreen()
            val endBlue = activityRule.findBlue()
            fragment2.enterTransition.verifyAndClearTransition {
                epicenter = startGreenBounds
                enteringViews += endGreen
            }
            fragment2.sharedElementEnter.verifyAndClearTransition {
                // In this case, we can't find an epicenter
                epicenter = Rect()
                exitingViews += startBlue
                enteringViews += endBlue
            }
            verifyNoOtherTransitions(fragment1)
            verifyNoOtherTransitions(fragment2)
        } else {
            // fragment2 doesn't get a transition since it conflicts with the pop transition
            verifyNoOtherTransitions(fragment2)
            // Everything else is just doing its best. Ordered transactions can't handle
            // multiple transitions acting together except for popping multiple together.
        }
    }

    // When there is no matching shared element, the transition name should not be changed
    @Test
    fun noMatchingSharedElementRetainName() {
        val fragment1 = setupInitialFragment()

        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startGreenBounds = startGreen.boundsOnScreen

        val fragment2 = TransitionFragment(R.layout.fragment_scene3)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .addSharedElement(startGreen, "greenSquare")
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()

        fragment2.waitForTransition()
        val midGreen = activityRule.findGreen()
        val midBlue = activityRule.findBlue()
        val midRed = activityRule.findRed()
        val midGreenBounds = midGreen.boundsOnScreen
        if (reorderingAllowed) {
            fragment2.sharedElementEnter.verifyAndClearTransition {
                epicenter = startGreenBounds
                exitingViews += startGreen
                enteringViews += midGreen
            }
        } else {
            fragment2.sharedElementEnter.verifyAndClearTransition {
                epicenter = startGreenBounds
                exitingViews += listOf(startGreen, startBlue)
                enteringViews += midGreen
            }
        }
        fragment2.enterTransition.verifyAndClearTransition {
            epicenter = midGreenBounds
            enteringViews += listOfNotNull(midBlue, midRed)
        }
        verifyNoOtherTransitions(fragment2)

        activityRule.popBackStackImmediate()
        fragment2.waitForTransition()
        fragment1.waitForTransition()

        val endBlue = activityRule.findBlue()
        val endGreen = activityRule.findGreen()

        assertThat(endBlue.transitionName).isEqualTo("blueSquare")
        assertThat(endGreen.transitionName).isEqualTo("greenSquare")
    }

    private fun setupInitialFragment(): TransitionFragment {
        val fragment1 = TransitionFragment(R.layout.fragment_scene1)
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        fragment1.waitForTransition()
        val blueSquare1 = activityRule.findBlue()
        val greenSquare1 = activityRule.findGreen()
        fragment1.enterTransition.verifyAndClearTransition {
            enteringViews += listOf(blueSquare1, greenSquare1)
        }
        verifyNoOtherTransitions(fragment1)
        return fragment1
    }

    private fun findViewById(fragment: Fragment, id: Int): View {
        return fragment.requireView().findViewById(id)
    }

    private fun verifyTransition(
        from: TransitionFragment,
        to: TransitionFragment,
        sharedElementName: String
    ) {
        val startOnBackStackChanged = onBackStackChangedTimes
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startRed = activityRule.findRed()

        val startBlueRect = startBlue.boundsOnScreen

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .addSharedElement(startBlue, sharedElementName)
            .replace(R.id.fragmentContainer, to)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(startOnBackStackChanged + 1)

        to.waitForTransition()
        val endGreen = activityRule.findGreen()
        val endBlue = activityRule.findBlue()
        val endRed = activityRule.findRed()
        val endBlueRect = endBlue.boundsOnScreen

        from.exitTransition.verifyAndClearTransition {
            epicenter = startBlueRect
            exitingViews += listOfNotNull(startGreen, startRed)
        }
        verifyNoOtherTransitions(from)

        to.enterTransition.verifyAndClearTransition {
            epicenter = endBlueRect
            enteringViews += listOfNotNull(endGreen, endRed)
        }

        to.sharedElementEnter.verifyAndClearTransition {
            epicenter = startBlueRect
            exitingViews += startBlue
            enteringViews += endBlue
        }
        verifyNoOtherTransitions(to)
    }

    private fun verifyCrossTransition(
        swapSource: Boolean,
        from1: TransitionFragment,
        from2: TransitionFragment
    ) {
        val startNumOnBackStackChanged = onBackStackChangedTimes
        val changesPerOperation = if (reorderingAllowed) 1 else 2

        val to1 = TransitionFragment(R.layout.fragment_scene2)
        val to2 = TransitionFragment(R.layout.fragment_scene2)

        val fromExit1 = findViewById(from1, R.id.greenSquare)
        val fromShared1 = findViewById(from1, R.id.blueSquare)
        val fromSharedRect1 = fromShared1.boundsOnScreen

        val fromExitId2 = if (swapSource) R.id.blueSquare else R.id.greenSquare
        val fromSharedId2 = if (swapSource) R.id.greenSquare else R.id.blueSquare
        val fromExit2 = findViewById(from2, fromExitId2)
        val fromShared2 = findViewById(from2, fromSharedId2)
        val fromSharedRect2 = fromShared2.boundsOnScreen

        val sharedElementName = if (swapSource) "blueSquare" else "greenSquare"

        activityRule.runOnUiThread {
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .addSharedElement(fromShared1, "blueSquare")
                .replace(R.id.fragmentContainer1, to1)
                .addToBackStack(null)
                .commit()
            fragmentManager.beginTransaction()
                .setReorderingAllowed(reorderingAllowed)
                .addSharedElement(fromShared2, sharedElementName)
                .replace(R.id.fragmentContainer2, to2)
                .addToBackStack(null)
                .commit()
        }
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes)
            .isEqualTo(startNumOnBackStackChanged + changesPerOperation)

        from1.waitForTransition()
        from2.waitForTransition()
        to1.waitForTransition()
        to2.waitForTransition()

        val toEnter1 = findViewById(to1, R.id.greenSquare)
        val toShared1 = findViewById(to1, R.id.blueSquare)
        val toSharedRect1 = toShared1.boundsOnScreen

        val toEnter2 = findViewById(to2, fromSharedId2)
        val toShared2 = findViewById(to2, fromExitId2)
        val toSharedRect2 = toShared2.boundsOnScreen

        from1.exitTransition.verifyAndClearTransition {
            epicenter = fromSharedRect1
            exitingViews += fromExit1
        }
        from2.exitTransition.verifyAndClearTransition {
            epicenter = fromSharedRect2
            exitingViews += fromExit2
        }
        verifyNoOtherTransitions(from1)
        verifyNoOtherTransitions(from2)

        to1.enterTransition.verifyAndClearTransition {
            epicenter = toSharedRect1
            enteringViews += toEnter1
        }
        to2.enterTransition.verifyAndClearTransition {
            epicenter = toSharedRect2
            enteringViews += toEnter2
        }
        to1.sharedElementEnter.verifyAndClearTransition {
            epicenter = fromSharedRect1
            exitingViews += fromShared1
            enteringViews += toShared1
        }
        to2.sharedElementEnter.verifyAndClearTransition {
            epicenter = fromSharedRect2
            exitingViews += fromShared2
            enteringViews += toShared2
        }
        verifyNoOtherTransitions(to1)
        verifyNoOtherTransitions(to2)

        // Now pop it back
        activityRule.runOnUiThread {
            fragmentManager.popBackStack()
            fragmentManager.popBackStack()
        }
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes)
            .isEqualTo(startNumOnBackStackChanged + changesPerOperation + 1)

        from1.waitForTransition()
        from2.waitForTransition()
        to1.waitForTransition()
        to2.waitForTransition()

        val returnEnter1 = findViewById(from1, R.id.greenSquare)
        val returnShared1 = findViewById(from1, R.id.blueSquare)

        val returnEnter2 = findViewById(from2, fromExitId2)
        val returnShared2 = findViewById(from2, fromSharedId2)

        to1.returnTransition.verifyAndClearTransition {
            epicenter = toSharedRect1
            exitingViews += toEnter1
        }
        to2.returnTransition.verifyAndClearTransition {
            epicenter = toSharedRect2
            exitingViews += toEnter2
        }
        to1.sharedElementReturn.verifyAndClearTransition {
            epicenter = toSharedRect1
            exitingViews += toShared1
            enteringViews += returnShared1
        }
        to2.sharedElementReturn.verifyAndClearTransition {
            epicenter = toSharedRect2
            exitingViews += toShared2
            enteringViews += returnShared2
        }
        verifyNoOtherTransitions(to1)
        verifyNoOtherTransitions(to2)

        from1.reenterTransition.verifyAndClearTransition {
            epicenter = fromSharedRect1
            enteringViews += returnEnter1
        }
        from2.reenterTransition.verifyAndClearTransition {
            epicenter = fromSharedRect2
            enteringViews += returnEnter2
        }
        verifyNoOtherTransitions(from1)
        verifyNoOtherTransitions(from2)
    }

    private fun verifyPopTransition(
        numPops: Int,
        from: TransitionFragment,
        to: TransitionFragment,
        vararg others: TransitionFragment
    ) {
        val startOnBackStackChanged = onBackStackChangedTimes
        val startBlue = activityRule.findBlue()
        val startGreen = activityRule.findGreen()
        val startRed = activityRule.findRed()
        val startSharedRect = startBlue.boundsOnScreen

        instrumentation.runOnMainSync {
            for (i in 0 until numPops) {
                fragmentManager.popBackStack()
            }
        }
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo((startOnBackStackChanged + 1))

        to.waitForTransition()
        val endGreen = activityRule.findGreen()
        val endBlue = activityRule.findBlue()
        val endRed = activityRule.findRed()
        val endSharedRect = endBlue.boundsOnScreen

        from.returnTransition.verifyAndClearTransition {
            epicenter = startSharedRect
            exitingViews += listOfNotNull(startGreen, startRed)
        }
        from.sharedElementReturn.verifyAndClearTransition {
            epicenter = startSharedRect
            exitingViews += startBlue
            enteringViews += endBlue
        }
        verifyNoOtherTransitions(from)

        to.reenterTransition.verifyAndClearTransition {
            epicenter = endSharedRect
            enteringViews += listOfNotNull(endGreen, endRed)
        }
        verifyNoOtherTransitions(to)

        for (fragment in others) {
            verifyNoOtherTransitions(fragment)
        }
    }

    class ComplexTransitionFragment : TransitionFragment(R.layout.fragment_scene2) {
        val sharedElementEnterTransition1 = TrackingTransition()
        val sharedElementEnterTransition2 = TrackingTransition()
        val sharedElementReturnTransition1 = TrackingTransition()
        val sharedElementReturnTransition2 = TrackingTransition()

        private val sharedElementEnterTransition: TransitionSet = TransitionSet()
            .addTransition(sharedElementEnterTransition1)
            .addTransition(sharedElementEnterTransition2)
        private val sharedElementReturnTransition: TransitionSet = TransitionSet()
            .addTransition(sharedElementReturnTransition1)
            .addTransition(sharedElementReturnTransition2)

        init {
            sharedElementEnterTransition1.addTarget(R.id.blueSquare)
            sharedElementEnterTransition2.addTarget(R.id.greenSquare)
            sharedElementReturnTransition1.addTarget(R.id.blueSquare)
            sharedElementReturnTransition2.addTarget(R.id.greenSquare)
            setSharedElementEnterTransition(sharedElementEnterTransition)
            setSharedElementReturnTransition(sharedElementReturnTransition)
        }
    }

    class InvisibleFragment : TransitionFragment(R.layout.fragment_scene1) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.visibility = View.INVISIBLE
            super.onViewCreated(view, savedInstanceState)
        }
    }

    class TransitionFinishFirstFragment : TransitionFragment(R.layout.fragment_scene1) {
        var onDestroyViewCountDownLatch = CountDownLatch(1)
        var transitionCountInOnDestroyView = 0L

        override fun onDestroyView() {
            transitionCountInOnDestroyView = endTransitionCountDownLatch.count
            onDestroyViewCountDownLatch.countDown()
            super.onDestroyView()
        }
    }

    class TestShowHideTransitionListener(
        fragment: TransitionFragment
    ) : TestTransitionFragmentListener(
        fragment
    ) {
        override fun onTransitionEnd(transition: Transition) {
            fragment.endTransitionCountDownLatch.countDown()
            fragment.startTransitionCountDownLatch = CountDownLatch(1)
        }

        override fun onTransitionStart(transition: Transition) {
            fragment.startTransitionCountDownLatch.countDown()
            transition.removeListener(this)
            transition.addListener(this)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Boolean> {
            return arrayOf(false, true)
        }
    }
}

class FragmentTransitionTestActivity : FragmentActivity(R.layout.simple_container)
