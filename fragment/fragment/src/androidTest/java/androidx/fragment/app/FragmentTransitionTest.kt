/*
 * Copyright 2018 The Android Open Source Project
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

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.transition.TransitionSet
import android.view.View
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.waitForExecution
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

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
class FragmentTransitionTest(private val reorderingAllowed: Boolean) {

    @get:Rule
    val activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var fragmentManager: FragmentManager
    private var onBackStackChangedTimes: Int = 0
    private val onBackStackChangedListener =
        FragmentManager.OnBackStackChangedListener { onBackStackChangedTimes++ }

    @Before
    fun setup() {
        activityRule.setContentView(R.layout.simple_container)
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
        val blue = findBlue()
        val green = findBlue()

        // exit transition
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .remove(fragment)
            .addToBackStack(null)
            .commit()

        fragment.waitForTransition()
        verifyAndClearTransition(fragment.exitTransition, null, green, blue)
        verifyNoOtherTransitions(fragment)
        assertThat(onBackStackChangedTimes).isEqualTo(2)

        // reenter transition
        activityRule.popBackStackImmediate()
        fragment.waitForTransition()
        val green2 = findGreen()
        val blue2 = findBlue()
        verifyAndClearTransition(fragment.reenterTransition, null, green2, blue2)
        verifyNoOtherTransitions(fragment)
        assertThat(onBackStackChangedTimes).isEqualTo(3)

        // return transition
        activityRule.popBackStackImmediate()
        fragment.waitForTransition()
        verifyAndClearTransition(fragment.returnTransition, null, green2, blue2)
        verifyNoOtherTransitions(fragment)
        assertThat(onBackStackChangedTimes).isEqualTo(4)
    }

    // Test that shared elements transition from one fragment to the next
    // and back during pop.
    @Test
    fun sharedElement() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.scene2)

        verifyTransition(fragment1, fragment2, "blueSquare")

        // Now pop the back stack
        verifyPopTransition(1, fragment2, fragment1)
    }

    // Test that shared element transitions through multiple fragments work together
    @Test
    fun intermediateFragment() {
        val fragment1 = setupInitialFragment()

        val fragment2 = TransitionFragment(R.layout.scene3)

        verifyTransition(fragment1, fragment2, "shared")

        val fragment3 = TransitionFragment(R.layout.scene2)

        verifyTransition(fragment2, fragment3, "blueSquare")

        // Should transfer backwards when popping multiple:
        verifyPopTransition(2, fragment3, fragment1, fragment2)
    }

    // Adding/removing the same fragment multiple times shouldn't mess anything up
    @Test
    fun removeAdded() {
        val fragment1 = setupInitialFragment()

        val startBlue = findBlue()
        val startGreen = findGreen()

        val fragment2 = TransitionFragment(R.layout.scene2)

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
        val endBlue = findBlue()
        val endGreen = findGreen()
        verifyAndClearTransition(fragment1.exitTransition, null, startBlue, startGreen)
        verifyAndClearTransition(fragment2.enterTransition, null, endBlue, endGreen)
        verifyNoOtherTransitions(fragment1)
        verifyNoOtherTransitions(fragment2)

        // Pop should also do the same thing
        activityRule.popBackStackImmediate()
        assertThat(onBackStackChangedTimes).isEqualTo(3)

        fragment1.waitForTransition()
        val popBlue = findBlue()
        val popGreen = findGreen()
        verifyAndClearTransition(fragment1.reenterTransition, null, popBlue, popGreen)
        verifyAndClearTransition(fragment2.returnTransition, null, endBlue, endGreen)
        verifyNoOtherTransitions(fragment1)
        verifyNoOtherTransitions(fragment2)
    }

    // Make sure that shared elements on two different fragment containers don't interact
    @Test
    fun crossContainer() {
        activityRule.setContentView(R.layout.double_container)
        val fragment1 = TransitionFragment(R.layout.scene1)
        val fragment2 = TransitionFragment(R.layout.scene1)
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
        verifyAndClearTransition(fragment1.enterTransition, null, greenSquare1, blueSquare1)
        verifyNoOtherTransitions(fragment1)
        fragment2.waitForTransition()
        val greenSquare2 = findViewById(fragment2, R.id.greenSquare)
        val blueSquare2 = findViewById(fragment2, R.id.blueSquare)
        verifyAndClearTransition(fragment2.enterTransition, null, greenSquare2, blueSquare2)
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
        val fragment2 = TransitionFragment(R.layout.scene2)

        val enterCallback = mock(SharedElementCallback::class.java)
        fragment2.setEnterSharedElementCallback(enterCallback)

        val startBlue = findBlue()

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

        val endBlue = findBlue()

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

        val reenterBlue = findBlue()

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
        val fragment2 = TransitionFragment(R.layout.scene2)

        val startBlue = findBlue()
        val startGreen = findGreen()

        val startGreenBounds = getBoundsOnScreen(startGreen)

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

        val endBlue = findBlue()
        val endBlueBounds = getBoundsOnScreen(endBlue)

        verifyAndClearTransition(
            fragment2.sharedElementEnter, startGreenBounds, startGreen,
            endBlue
        )

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

        val reenterGreen = findGreen()
        verifyAndClearTransition(
            fragment2.sharedElementReturn, endBlueBounds, endBlue,
            reenterGreen
        )
    }

    // Make sure that onMapSharedElement works to change the shared element target
    @Test
    fun onMapSharedElementIn() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.scene2)

        val startBlue = findBlue()
        val startBlueBounds = getBoundsOnScreen(startBlue)

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

        val endGreen = findGreen()
        val endBlue = findBlue()
        val endGreenBounds = getBoundsOnScreen(endGreen)

        verifyAndClearTransition(
            fragment2.sharedElementEnter, startBlueBounds, startBlue,
            endGreen
        )

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

        val reenterBlue = findBlue()
        verifyAndClearTransition(
            fragment2.sharedElementReturn, endGreenBounds, endGreen,
            reenterBlue
        )
    }

    // Ensure that shared element transitions that have targets properly target the views
    @Test
    fun complexSharedElementTransition() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = ComplexTransitionFragment()

        val startBlue = findBlue()
        val startGreen = findGreen()
        val startBlueBounds = getBoundsOnScreen(startBlue)

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

        val endBlue = findBlue()
        val endGreen = findGreen()
        val endBlueBounds = getBoundsOnScreen(endBlue)

        verifyAndClearTransition(
            fragment2.sharedElementEnterTransition1, startBlueBounds,
            startBlue, endBlue
        )
        verifyAndClearTransition(
            fragment2.sharedElementEnterTransition2, startBlueBounds,
            startGreen, endGreen
        )

        // Now see if it works when popped
        activityRule.popBackStackImmediate()
        assertThat(onBackStackChangedTimes).isEqualTo(3)

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val reenterBlue = findBlue()
        val reenterGreen = findGreen()

        verifyAndClearTransition(
            fragment2.sharedElementReturnTransition1, endBlueBounds,
            endBlue, reenterBlue
        )
        verifyAndClearTransition(
            fragment2.sharedElementReturnTransition2, endBlueBounds,
            endGreen, reenterGreen
        )
    }

    // Ensure that after transitions have executed that they don't have any targets or other
    // unfortunate modifications.
    @Test
    fun transitionsEndUnchanged() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.scene2)

        verifyTransition(fragment1, fragment2, "blueSquare")
        assertThat(fragment1.exitTransition.getTargets().size).isEqualTo(0)
        assertThat(fragment2.sharedElementEnter.getTargets().size).isEqualTo(0)
        assertThat(fragment2.enterTransition.getTargets().size).isEqualTo(0)
        assertThat(fragment1.exitTransition.epicenterCallback).isNull()
        assertThat(fragment2.enterTransition.epicenterCallback).isNull()
        assertThat(fragment2.sharedElementEnter.epicenterCallback).isNull()

        // Now pop the back stack
        verifyPopTransition(1, fragment2, fragment1)

        assertThat(fragment2.returnTransition.getTargets().size).isEqualTo(0)
        assertThat(fragment2.sharedElementReturn.getTargets().size).isEqualTo(0)
        assertThat(fragment1.reenterTransition.getTargets().size).isEqualTo(0)
        assertThat(fragment2.returnTransition.epicenterCallback).isNull()
        assertThat(fragment2.sharedElementReturn.epicenterCallback).isNull()
        assertThat(fragment2.reenterTransition.epicenterCallback).isNull()
    }

    // Ensure that transitions are done when a fragment is shown and hidden
    @Test
    fun showHideTransition() {
        val fragment1 = setupInitialFragment()
        val fragment2 = TransitionFragment(R.layout.scene2)

        val startBlue = findBlue()
        val startGreen = findGreen()

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment2)
            .hide(fragment1)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()
        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val endGreen = findViewById(fragment2, R.id.greenSquare)
        val endBlue = findViewById(fragment2, R.id.blueSquare)

        assertThat(fragment1.requireView().visibility).isEqualTo(View.GONE)
        assertThat(startGreen.visibility).isEqualTo(View.VISIBLE)
        assertThat(startBlue.visibility).isEqualTo(View.VISIBLE)

        verifyAndClearTransition(fragment1.exitTransition, null, startGreen, startBlue)
        verifyNoOtherTransitions(fragment1)

        verifyAndClearTransition(fragment2.enterTransition, null, endGreen, endBlue)
        verifyNoOtherTransitions(fragment2)

        activityRule.popBackStackImmediate()

        activityRule.waitForExecution()
        fragment1.waitForTransition()
        fragment2.waitForTransition()

        verifyAndClearTransition(fragment1.reenterTransition, null, startGreen, startBlue)
        verifyNoOtherTransitions(fragment1)

        assertThat(fragment1.requireView().visibility).isEqualTo(View.VISIBLE)
        assertThat(startGreen.visibility).isEqualTo(View.VISIBLE)
        assertThat(startBlue.visibility).isEqualTo(View.VISIBLE)

        verifyAndClearTransition(fragment2.returnTransition, null, endGreen, endBlue)
        verifyNoOtherTransitions(fragment2)
    }

    // Ensure that transitions are done when a fragment is attached and detached
    @Test
    fun attachDetachTransition() {
        val fragment1 = setupInitialFragment()
        val fragment2 = TransitionFragment(R.layout.scene2)

        val startBlue = findBlue()
        val startGreen = findGreen()

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment2)
            .detach(fragment1)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()

        val endGreen = findViewById(fragment2, R.id.greenSquare)
        val endBlue = findViewById(fragment2, R.id.blueSquare)

        verifyAndClearTransition(fragment1.exitTransition, null, startGreen, startBlue)
        verifyNoOtherTransitions(fragment1)

        verifyAndClearTransition(fragment2.enterTransition, null, endGreen, endBlue)
        verifyNoOtherTransitions(fragment2)

        activityRule.popBackStackImmediate()

        activityRule.waitForExecution()

        val reenterBlue = findBlue()
        val reenterGreen = findGreen()

        verifyAndClearTransition(fragment1.reenterTransition, null, reenterGreen, reenterBlue)
        verifyNoOtherTransitions(fragment1)

        verifyAndClearTransition(fragment2.returnTransition, null, endGreen, endBlue)
        verifyNoOtherTransitions(fragment2)
    }

    // Ensure that shared element without matching transition name doesn't error out
    @Test
    fun sharedElementMismatch() {
        val fragment1 = setupInitialFragment()

        // Now do a transition to scene2
        val fragment2 = TransitionFragment(R.layout.scene2)

        val startBlue = findBlue()
        val startGreen = findGreen()
        val startBlueBounds = getBoundsOnScreen(startBlue)

        fragmentManager.beginTransaction()
            .addSharedElement(startBlue, "fooSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .setReorderingAllowed(reorderingAllowed)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        fragment1.waitForTransition()
        fragment2.waitForTransition()

        val endBlue = findBlue()
        val endGreen = findGreen()

        if (reorderingAllowed) {
            verifyAndClearTransition(fragment1.exitTransition, null, startGreen, startBlue)
        } else {
            verifyAndClearTransition(fragment1.exitTransition, startBlueBounds, startGreen)
            verifyAndClearTransition(fragment2.sharedElementEnter, startBlueBounds, startBlue)
        }
        verifyNoOtherTransitions(fragment1)

        verifyAndClearTransition(fragment2.enterTransition, null, endGreen, endBlue)
        verifyNoOtherTransitions(fragment2)
    }

    // Ensure that using the same source or target shared element results in an exception.
    @Test
    fun sharedDuplicateTargetNames() {
        setupInitialFragment()

        val startBlue = findBlue()
        val startGreen = findGreen()

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

        val startBlue = findBlue()
        val startGreen = findGreen()
        val startBlueBounds = getBoundsOnScreen(startBlue)

        val fragment2 = TransitionFragment(R.layout.scene2)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()

        fragment1.waitForTransition()
        fragment2.waitForTransition()
        val midGreen = findGreen()
        val midBlue = findBlue()
        val midBlueBounds = getBoundsOnScreen(midBlue)
        verifyAndClearTransition(fragment1.exitTransition, startBlueBounds, startGreen)
        verifyAndClearTransition(fragment2.sharedElementEnter, startBlueBounds, startBlue, midBlue)
        verifyAndClearTransition(fragment2.enterTransition, midBlueBounds, midGreen)
        verifyNoOtherTransitions(fragment1)
        verifyNoOtherTransitions(fragment2)

        val fragment3 = TransitionFragment(R.layout.scene3)

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
            verifyAndClearTransition(fragment2.returnTransition, null, midGreen, midBlue)
            val endGreen = findGreen()
            val endBlue = findBlue()
            val endRed = findRed()
            verifyAndClearTransition(fragment3.enterTransition, null, endGreen, endBlue, endRed!!)
            verifyNoOtherTransitions(fragment2)
            verifyNoOtherTransitions(fragment3)
        } else {
            // fragment3 doesn't get a transition since it conflicts with the pop transition
            verifyNoOtherTransitions(fragment3)
            // Everything else is just doing its best. Ordered transactions can't handle
            // multiple transitions acting together except for popping multiple together.
        }
    }

    // When there is no matching shared element, the transition name should not be changed
    @Test
    fun noMatchingSharedElementRetainName() {
        val fragment1 = setupInitialFragment()

        val startBlue = findBlue()
        val startGreen = findGreen()
        val startGreenBounds = getBoundsOnScreen(startGreen)

        val fragment2 = TransitionFragment(R.layout.scene3)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .addSharedElement(startGreen, "greenSquare")
            .addSharedElement(startBlue, "blueSquare")
            .replace(R.id.fragmentContainer, fragment2)
            .addToBackStack(null)
            .commit()

        fragment2.waitForTransition()
        val midGreen = findGreen()
        val midBlue = findBlue()
        val midRed = findRed()
        val midGreenBounds = getBoundsOnScreen(midGreen)
        if (reorderingAllowed) {
            verifyAndClearTransition(
                fragment2.sharedElementEnter, startGreenBounds, startGreen,
                midGreen
            )
        } else {
            verifyAndClearTransition(
                fragment2.sharedElementEnter, startGreenBounds, startGreen,
                midGreen, startBlue
            )
        }
        verifyAndClearTransition(fragment2.enterTransition, midGreenBounds, midBlue, midRed!!)
        verifyNoOtherTransitions(fragment2)

        activityRule.popBackStackImmediate()
        fragment2.waitForTransition()
        fragment1.waitForTransition()

        val endBlue = findBlue()
        val endGreen = findGreen()

        assertThat(endBlue.transitionName).isEqualTo("blueSquare")
        assertThat(endGreen.transitionName).isEqualTo("greenSquare")
    }

    private fun setupInitialFragment(): TransitionFragment {
        val fragment1 = TransitionFragment(R.layout.scene1)
        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .add(R.id.fragmentContainer, fragment1)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(1)
        fragment1.waitForTransition()
        val blueSquare1 = findBlue()
        val greenSquare1 = findGreen()
        verifyAndClearTransition(fragment1.enterTransition, null, blueSquare1, greenSquare1)
        verifyNoOtherTransitions(fragment1)
        return fragment1
    }

    private fun findViewById(fragment: Fragment, id: Int): View {
        return fragment.requireView().findViewById(id)
    }

    private fun findGreen(): View {
        return activityRule.activity.findViewById(R.id.greenSquare)
    }

    private fun findBlue(): View {
        return activityRule.activity.findViewById(R.id.blueSquare)
    }

    private fun findRed(): View? {
        return activityRule.activity.findViewById(R.id.redSquare)
    }

    private fun verifyAndClearTransition(
        transition: TargetTracking,
        epicenter: Rect?,
        vararg expected: View
    ) {
        if (epicenter == null) {
            assertThat(transition.capturedEpicenter).isNull()
        } else {
            assertThat(transition.capturedEpicenter).isEqualTo(epicenter)
        }
        val targets = transition.trackedTargets
        val sb = StringBuilder()
        sb.append("Expected: [")
            .append(expected.size)
            .append("] {")
        var isFirst = true
        for (view in expected) {
            if (isFirst) {
                isFirst = false
            } else {
                sb.append(", ")
            }
            sb.append(view)
        }
        sb.append("}, but got: [")
            .append(targets.size)
            .append("] {")
        isFirst = true
        for (view in targets) {
            if (isFirst) {
                isFirst = false
            } else {
                sb.append(", ")
            }
            sb.append(view)
        }
        sb.append("}")
        val errorMessage = sb.toString()

        assertWithMessage(errorMessage).that(targets.size).isEqualTo(expected.size)
        for (view in expected) {
            assertWithMessage(errorMessage).that(targets.contains(view)).isTrue()
        }
        transition.clearTargets()
    }

    private fun verifyNoOtherTransitions(fragment: TransitionFragment) {
        assertThat(fragment.enterTransition.targets.size).isEqualTo(0)
        assertThat(fragment.exitTransition.targets.size).isEqualTo(0)
        assertThat(fragment.reenterTransition.targets.size).isEqualTo(0)
        assertThat(fragment.returnTransition.targets.size).isEqualTo(0)
        assertThat(fragment.sharedElementEnter.targets.size).isEqualTo(0)
        assertThat(fragment.sharedElementReturn.targets.size).isEqualTo(0)
    }

    private fun verifyTransition(
        from: TransitionFragment,
        to: TransitionFragment,
        sharedElementName: String
    ) {
        val startOnBackStackChanged = onBackStackChangedTimes
        val startBlue = findBlue()
        val startGreen = findGreen()
        val startRed = findRed()

        val startBlueRect = getBoundsOnScreen(startBlue)

        fragmentManager.beginTransaction()
            .setReorderingAllowed(reorderingAllowed)
            .addSharedElement(startBlue, sharedElementName)
            .replace(R.id.fragmentContainer, to)
            .addToBackStack(null)
            .commit()

        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo(startOnBackStackChanged + 1)

        to.waitForTransition()
        val endGreen = findGreen()
        val endBlue = findBlue()
        val endRed = findRed()
        val endBlueRect = getBoundsOnScreen(endBlue)

        if (startRed != null) {
            verifyAndClearTransition(from.exitTransition, startBlueRect, startGreen, startRed)
        } else {
            verifyAndClearTransition(from.exitTransition, startBlueRect, startGreen)
        }
        verifyNoOtherTransitions(from)

        if (endRed != null) {
            verifyAndClearTransition(to.enterTransition, endBlueRect, endGreen, endRed)
        } else {
            verifyAndClearTransition(to.enterTransition, endBlueRect, endGreen)
        }
        verifyAndClearTransition(to.sharedElementEnter, startBlueRect, startBlue, endBlue)
        verifyNoOtherTransitions(to)
    }

    private fun verifyCrossTransition(
        swapSource: Boolean,
        from1: TransitionFragment,
        from2: TransitionFragment
    ) {
        val startNumOnBackStackChanged = onBackStackChangedTimes
        val changesPerOperation = if (reorderingAllowed) 1 else 2

        val to1 = TransitionFragment(R.layout.scene2)
        val to2 = TransitionFragment(R.layout.scene2)

        val fromExit1 = findViewById(from1, R.id.greenSquare)
        val fromShared1 = findViewById(from1, R.id.blueSquare)
        val fromSharedRect1 = getBoundsOnScreen(fromShared1)

        val fromExitId2 = if (swapSource) R.id.blueSquare else R.id.greenSquare
        val fromSharedId2 = if (swapSource) R.id.greenSquare else R.id.blueSquare
        val fromExit2 = findViewById(from2, fromExitId2)
        val fromShared2 = findViewById(from2, fromSharedId2)
        val fromSharedRect2 = getBoundsOnScreen(fromShared2)

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
        val toSharedRect1 = getBoundsOnScreen(toShared1)

        val toEnter2 = findViewById(to2, fromSharedId2)
        val toShared2 = findViewById(to2, fromExitId2)
        val toSharedRect2 = getBoundsOnScreen(toShared2)

        verifyAndClearTransition(from1.exitTransition, fromSharedRect1, fromExit1)
        verifyAndClearTransition(from2.exitTransition, fromSharedRect2, fromExit2)
        verifyNoOtherTransitions(from1)
        verifyNoOtherTransitions(from2)

        verifyAndClearTransition(to1.enterTransition, toSharedRect1, toEnter1)
        verifyAndClearTransition(to2.enterTransition, toSharedRect2, toEnter2)
        verifyAndClearTransition(to1.sharedElementEnter, fromSharedRect1, fromShared1, toShared1)
        verifyAndClearTransition(to2.sharedElementEnter, fromSharedRect2, fromShared2, toShared2)
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

        verifyAndClearTransition(to1.returnTransition, toSharedRect1, toEnter1)
        verifyAndClearTransition(to2.returnTransition, toSharedRect2, toEnter2)
        verifyAndClearTransition(to1.sharedElementReturn, toSharedRect1, toShared1, returnShared1)
        verifyAndClearTransition(to2.sharedElementReturn, toSharedRect2, toShared2, returnShared2)
        verifyNoOtherTransitions(to1)
        verifyNoOtherTransitions(to2)

        verifyAndClearTransition(from1.reenterTransition, fromSharedRect1, returnEnter1)
        verifyAndClearTransition(from2.reenterTransition, fromSharedRect2, returnEnter2)
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
        val startBlue = findBlue()
        val startGreen = findGreen()
        val startRed = findRed()
        val startSharedRect = getBoundsOnScreen(startBlue)

        instrumentation.runOnMainSync {
            for (i in 0 until numPops) {
                fragmentManager.popBackStack()
            }
        }
        activityRule.waitForExecution()
        assertThat(onBackStackChangedTimes).isEqualTo((startOnBackStackChanged + 1))

        to.waitForTransition()
        val endGreen = findGreen()
        val endBlue = findBlue()
        val endRed = findRed()
        val endSharedRect = getBoundsOnScreen(endBlue)

        if (startRed != null) {
            verifyAndClearTransition(from.returnTransition, startSharedRect, startGreen, startRed)
        } else {
            verifyAndClearTransition(from.returnTransition, startSharedRect, startGreen)
        }
        verifyAndClearTransition(from.sharedElementReturn, startSharedRect, startBlue, endBlue)
        verifyNoOtherTransitions(from)

        if (endRed != null) {
            verifyAndClearTransition(to.reenterTransition, endSharedRect, endGreen, endRed)
        } else {
            verifyAndClearTransition(to.reenterTransition, endSharedRect, endGreen)
        }
        verifyNoOtherTransitions(to)

        for (fragment in others) {
            verifyNoOtherTransitions(fragment)
        }
    }

    class ComplexTransitionFragment : TransitionFragment(R.layout.scene2) {
        val sharedElementEnterTransition1 = TrackingTransition()
        val sharedElementEnterTransition2 = TrackingTransition()
        val sharedElementReturnTransition1 = TrackingTransition()
        val sharedElementReturnTransition2 = TrackingTransition()

        val sharedElementEnterTransition: TransitionSet = TransitionSet()
            .addTransition(sharedElementEnterTransition1)
            .addTransition(sharedElementEnterTransition2)
        val sharedElementReturnTransition: TransitionSet = TransitionSet()
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

    class InvisibleFragment : TransitionFragment(R.layout.scene1) {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            view.visibility = View.INVISIBLE
            super.onViewCreated(view, savedInstanceState)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Array<Boolean> {
            return arrayOf(false, true)
        }

        private fun getBoundsOnScreen(view: View): Rect {
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
        }
    }
}
