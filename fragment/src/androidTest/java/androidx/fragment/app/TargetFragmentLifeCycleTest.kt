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

package androidx.fragment.app

import android.os.Bundle
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TargetFragmentLifeCycleTest {

    @get:Rule
    val activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    fun targetFragmentNoCycles() {
        val one = Fragment()
        val two = Fragment()
        val three = Fragment()

        try {
            one.setTargetFragment(two, 0)
            two.setTargetFragment(three, 0)
            three.setTargetFragment(one, 0)
            Assert.fail("creating a fragment target cycle did not throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("Setting $one as the target of $three would" +
                    " create a target cycle")
        }
    }

    @Test
    fun targetFragmentSetClear() {
        val one = Fragment()
        val two = Fragment()

        one.setTargetFragment(two, 0)
        one.setTargetFragment(null, 0)
    }

    /**
     * Test that target fragments are in a useful state when we restore them, even if they're
     * on the back stack.
     */
    @Test
    @UiThreadTest
    fun targetFragmentRestoreLifecycleStateBackStack() {
        val viewModelStore = ViewModelStore()
        val fc1 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm1 = fc1.supportFragmentManager

        fc1.attachHost(null)
        fc1.dispatchCreate()

        val target = TargetFragment()
        fm1.beginTransaction().add(target, "target").commitNow()

        val referrer = ReferrerFragment()
        referrer.setTargetFragment(target, 0)

        fm1.beginTransaction()
            .remove(target)
            .add(referrer, "referrer")
            .addToBackStack(null)
            .commit()

        fc1.dispatchActivityCreated()
        fc1.noteStateNotSaved()
        fc1.execPendingActions()
        fc1.dispatchStart()
        fc1.dispatchResume()
        fc1.execPendingActions()

        // Simulate an activity restart
        val fc2 = fc1.restart(activityRule, viewModelStore)

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)
    }

    @Test
    @UiThreadTest
    fun targetFragmentRestoreLifecycleStateManagerOrder() {
        val viewModelStore = ViewModelStore()
        val fc1 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm1 = fc1.supportFragmentManager

        fc1.attachHost(null)
        fc1.dispatchCreate()

        val target1 = TargetFragment()
        val referrer1 = ReferrerFragment()
        referrer1.setTargetFragment(target1, 0)

        fm1.beginTransaction().add(target1, "target1").add(referrer1, "referrer1").commitNow()

        val target2 = TargetFragment()
        val referrer2 = ReferrerFragment()
        referrer2.setTargetFragment(target2, 0)

        // Order shouldn't matter.
        fm1.beginTransaction().add(referrer2, "referrer2").add(target2, "target2").commitNow()

        fc1.dispatchActivityCreated()
        fc1.noteStateNotSaved()
        fc1.execPendingActions()
        fc1.dispatchStart()
        fc1.dispatchResume()
        fc1.execPendingActions()

        // Simulate an activity restart
        val fc2 = fc1.restart(activityRule, viewModelStore)

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)
    }

    @Test
    @UiThreadTest
    fun targetFragmentClearedWhenSetToNull() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val target = TargetFragment()
        val referrer = ReferrerFragment()
        referrer.setTargetFragment(target, 0)

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        referrer.setTargetFragment(null, 0)

        assertWithMessage("Target Fragment should cleared after setTargetFragment with null")
            .that(referrer.targetFragment).isNull()

        fm.beginTransaction().remove(referrer).commitNow()

        assertWithMessage("Target Fragment should still be cleared after being removed")
            .that(referrer.targetFragment).isNull()

        fc.shutdown(viewModelStore)
    }

    @Test
    @UiThreadTest
    fun targetFragment_replacement() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val referrer = ReferrerFragment()
        val target = TargetFragment()
        referrer.setTargetFragment(target, 0)

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(referrer, "referrer").add(target, "target").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        val newTarget = TargetFragment()
        referrer.setTargetFragment(newTarget, 0)

        assertWithMessage("New Target Fragment should returned despite not being added")
            .that(referrer.targetFragment).isSameAs(newTarget)

        referrer.setTargetFragment(target, 0)

        assertWithMessage("Replacement Target Fragment should override previous target")
            .that(referrer.targetFragment).isSameAs(target)

        fc.shutdown(viewModelStore)
    }

    /**
     * Test the availability of getTargetFragment() when the target Fragment is already
     * attached to a FragmentManager, but the referrer Fragment is not attached.
     */
    @Test
    @UiThreadTest
    fun targetFragmentOnlyTargetAdded() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val target = TargetFragment()
        // Add just the target Fragment to the FragmentManager
        fm.beginTransaction().add(target, "target").commitNow()

        val referrer = ReferrerFragment()
        referrer.setTargetFragment(target, 0)

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(referrer, "referrer").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().remove(referrer).commitNow()

        assertWithMessage("Target Fragment should be accessible after being removed")
            .that(referrer.targetFragment).isSameAs(target)

        fc.shutdown(viewModelStore)
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * not retained and the referrer fragment is not retained.
     */
    @Test
    @UiThreadTest
    fun targetFragmentNonRetainedNonRetained() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val target = TargetFragment()
        val referrer = ReferrerFragment()
        referrer.setTargetFragment(target, 0)

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().remove(referrer).commitNow()

        assertWithMessage("Target Fragment should be accessible after being removed")
            .that(referrer.targetFragment).isSameAs(target)

        fc.shutdown(viewModelStore)

        assertWithMessage("Target Fragment should be accessible after destruction")
            .that(referrer.targetFragment).isSameAs(target)
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * retained and the referrer fragment is not retained.
     */
    @Test
    @UiThreadTest
    fun targetFragmentRetainedNonRetained() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val target = TargetFragment()
        target.retainInstance = true
        val referrer = ReferrerFragment()
        referrer.setTargetFragment(target, 0)

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().remove(referrer).commitNow()

        assertWithMessage("Target Fragment should be accessible after being removed")
            .that(referrer.targetFragment).isSameAs(target)

        fc.shutdown(viewModelStore)

        assertWithMessage("Target Fragment should be accessible after destruction")
            .that(referrer.targetFragment).isSameAs(target)
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * not retained and the referrer fragment is retained.
     */
    @Test
    @UiThreadTest
    fun targetFragmentNonRetainedRetained() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val target = TargetFragment()
        val referrer = ReferrerFragment()
        referrer.setTargetFragment(target, 0)
        referrer.retainInstance = true

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        // Save the state
        fc.dispatchPause()
        fc.saveAllState()
        fc.dispatchStop()
        fc.dispatchDestroy()

        assertWithMessage("Target Fragment should be accessible after target Fragment destruction")
            .that(referrer.targetFragment).isSameAs(target)
    }

    /**
     * Test the availability of getTargetFragment() when the target fragment is
     * retained and the referrer fragment is also retained.
     */
    @Test
    @UiThreadTest
    fun targetFragmentRetainedRetained() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val target = TargetFragment()
        target.retainInstance = true
        val referrer = ReferrerFragment()
        referrer.retainInstance = true
        referrer.setTargetFragment(target, 0)

        assertWithMessage("Target Fragment should be accessible before being added")
            .that(referrer.targetFragment).isSameAs(target)

        fm.beginTransaction().add(target, "target").add(referrer, "referrer").commitNow()

        assertWithMessage("Target Fragment should be accessible after being added")
            .that(referrer.targetFragment).isSameAs(target)

        // Save the state
        fc.dispatchPause()
        fc.saveAllState()
        fc.dispatchStop()
        fc.dispatchDestroy()

        assertWithMessage("Target Fragment should be accessible after FragmentManager destruction")
            .that(referrer.targetFragment).isSameAs(target)
    }

    class TargetFragment : Fragment() {
        var calledCreate: Boolean = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            calledCreate = true
        }
    }

    class ReferrerFragment : Fragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val target = targetFragment
            assertWithMessage("target fragment was null during referrer onCreate")
                .that(target).isNotNull()

            if (target !is TargetFragment) {
                throw IllegalStateException("target fragment was not a TargetFragment")
            }

            assertWithMessage("target fragment has not yet been created")
                .that(target.calledCreate).isTrue()
        }
    }
}