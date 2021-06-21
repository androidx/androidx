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

import android.widget.EditText
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SaveStateFragmentTest {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    @UiThreadTest
    @Suppress("DEPRECATION")
    fun setInitialSavedState() {
        val fm = activityRule.activity.supportFragmentManager

        // Add a StateSaveFragment
        var fragment = StateSaveFragment("Saved", "")
        fm.beginTransaction().add(fragment, "tag").commit()
        executePendingTransactions(fm)

        // Change the user visible hint before we save state
        fragment.userVisibleHint = false

        // Save its state and remove it
        val state = fm.saveFragmentInstanceState(fragment)
        fm.beginTransaction().remove(fragment).commit()
        executePendingTransactions(fm)

        // Create a new instance, calling setInitialSavedState
        fragment = StateSaveFragment("", "")
        fragment.setInitialSavedState(state)

        // Add the new instance
        fm.beginTransaction().add(fragment, "tag").commit()
        executePendingTransactions(fm)

        assertWithMessage("setInitialSavedState did not restore saved state")
            .that(fragment.savedState).isEqualTo("Saved")
        assertWithMessage("setInitialSavedState did not restore user visible hint")
            .that(fragment.userVisibleHint).isEqualTo(false)
    }

    @Test
    @UiThreadTest
    @Suppress("DEPRECATION")
    fun setInitialSavedStateWithSetUserVisibleHint() {
        val fm = activityRule.activity.supportFragmentManager

        // Add a StateSaveFragment
        var fragment = StateSaveFragment("Saved", "")
        fm.beginTransaction().add(fragment, "tag").commit()
        executePendingTransactions(fm)

        // Save its state and remove it
        val state = fm.saveFragmentInstanceState(fragment)
        fm.beginTransaction().remove(fragment).commit()
        executePendingTransactions(fm)

        // Create a new instance, calling setInitialSavedState
        fragment = StateSaveFragment("", "")
        fragment.setInitialSavedState(state)

        // Change the user visible hint after we call setInitialSavedState
        fragment.userVisibleHint = false

        // Add the new instance
        fm.beginTransaction().add(fragment, "tag").commit()
        executePendingTransactions(fm)

        assertWithMessage("setInitialSavedState did not restore saved state")
            .that(fragment.savedState).isEqualTo("Saved")
        assertWithMessage("setUserVisibleHint should override setInitialSavedState")
            .that(fragment.userVisibleHint).isEqualTo(false)
    }

    @Test
    @UiThreadTest
    fun testFragmentViewStateSaved() {
        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)
        val fm1 = fc1.supportFragmentManager

        val fragment = SaveViewStateFragment()

        fm1.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val editText = fragment.requireView().findViewById<EditText>(R.id.editText)

        editText.setText("saved")

        fc1.dispatchPause()
        @Suppress("DEPRECATION")
        val savedState = fc1.saveAllState()
        fc1.dispatchStop()
        fc1.dispatchDestroy()

        val fc2 = activityRule.startupFragmentController(viewModelStore, savedState)
        val fm2 = fc2.supportFragmentManager

        val restoredFragment = fm2.findFragmentById(android.R.id.content) as SaveViewStateFragment
        assertWithMessage("Fragment was not restored")
            .that(restoredFragment).isNotNull()

        val restoredEditText = restoredFragment.requireView().findViewById<EditText>(R.id.editText)

        assertWithMessage("Fragment view was not properly restored")
            .that(restoredEditText.text.toString())
            .isEqualTo("saved")

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThreadTest
    fun restoreRetainedInstanceFragments() {
        // Create a new FragmentManager in isolation, nest some assorted fragments
        // and then restore them to a second new FragmentManager.
        val viewModelStore = ViewModelStore()
        val fc1 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm1 = fc1.supportFragmentManager

        fc1.attachHost(null)
        fc1.dispatchCreate()

        // Configure fragments.

        // This retained fragment will be added, then removed. After being removed, it
        // should no longer be retained by the FragmentManager
        val removedFragment = StateSaveFragment("Removed", "UnsavedRemoved", true)
        fm1.beginTransaction().add(removedFragment, "tag:removed").commitNow()
        fm1.beginTransaction().remove(removedFragment).commitNow()

        // This retained fragment will be added, then detached. After being detached, it
        // should continue to be retained by the FragmentManager
        val detachedFragment = StateSaveFragment("Detached", "UnsavedDetached", true)
        fm1.beginTransaction().add(detachedFragment, "tag:detached").commitNow()
        fm1.beginTransaction().detach(detachedFragment).commitNow()

        // Grandparent fragment will not retain instance
        val grandparentFragment = StateSaveFragment("Grandparent", "UnsavedGrandparent")
        assertWithMessage("grandparent fragment saved state not initialized")
            .that(grandparentFragment.savedState).isNotNull()
        assertWithMessage("grandparent fragment unsaved state not initialized")
            .that(grandparentFragment.unsavedState).isNotNull()
        fm1.beginTransaction().add(grandparentFragment, "tag:grandparent").commitNow()

        // Parent fragment will retain instance
        val parentFragment = StateSaveFragment("Parent", "UnsavedParent", true)
        assertWithMessage("parent fragment saved state not initialized")
            .that(parentFragment.savedState).isNotNull()
        assertWithMessage("parent fragment unsaved state not initialized")
            .that(parentFragment.unsavedState).isNotNull()
        grandparentFragment.childFragmentManager.beginTransaction()
            .add(parentFragment, "tag:parent").commitNow()
        assertWithMessage("parent fragment is not a child of grandparent")
            .that(parentFragment.parentFragment).isSameInstanceAs(grandparentFragment)

        // Child fragment will not retain instance
        val childFragment = StateSaveFragment("Child", "UnsavedChild")
        assertWithMessage("child fragment saved state not initialized")
            .that(childFragment.savedState).isNotNull()
        assertWithMessage("child fragment unsaved state not initialized")
            .that(childFragment.unsavedState).isNotNull()
        parentFragment.childFragmentManager.beginTransaction()
            .add(childFragment, "tag:child").commitNow()
        assertWithMessage("child fragment is not a child of grandparent")
            .that(childFragment.parentFragment).isSameInstanceAs(parentFragment)

        // Saved for comparison later
        val parentChildFragmentManager = parentFragment.childFragmentManager

        fc1.dispatchActivityCreated()
        fc1.noteStateNotSaved()
        fc1.execPendingActions()
        fc1.dispatchStart()
        fc1.dispatchResume()
        fc1.execPendingActions()

        // Bring the state back down to destroyed, simulating an activity restart
        fc1.dispatchPause()
        val savedState = fc1.saveAllState()
        fc1.dispatchStop()
        fc1.dispatchDestroy()

        // Create the new controller and restore state
        val fc2 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm2 = fc2.supportFragmentManager

        fc2.attachHost(null)
        fc2.restoreSaveState(savedState)
        fc2.dispatchCreate()

        // Confirm that the restored fragments are available and in the expected states
        val restoredRemovedFragment = fm2.findFragmentByTag("tag:removed") as StateSaveFragment?
        assertThat(restoredRemovedFragment).isNull()
        assertWithMessage("Removed Fragment should be destroyed")
            .that(removedFragment.calledOnDestroy).isTrue()

        val restoredDetachedFragment = fm2.findFragmentByTag("tag:detached") as StateSaveFragment
        assertThat(restoredDetachedFragment).isNotNull()

        val restoredGrandparent = fm2.findFragmentByTag("tag:grandparent") as StateSaveFragment
        assertWithMessage("grandparent fragment not restored").that(restoredGrandparent).isNotNull()

        assertWithMessage("grandparent fragment instance was saved")
            .that(restoredGrandparent).isNotSameInstanceAs(grandparentFragment)
        assertWithMessage("grandparent fragment saved state was not equal")
            .that(restoredGrandparent.savedState).isEqualTo(grandparentFragment.savedState)
        assertWithMessage("grandparent fragment unsaved state was unexpectedly preserved")
            .that(restoredGrandparent.unsavedState).isNotEqualTo(grandparentFragment.unsavedState)

        val restoredParent = restoredGrandparent
            .childFragmentManager.findFragmentByTag("tag:parent") as StateSaveFragment
        assertWithMessage("parent fragment not restored").that(restoredParent).isNotNull()

        assertWithMessage("parent fragment instance was not saved")
            .that(restoredParent).isSameInstanceAs(parentFragment)
        assertWithMessage("parent fragment saved state was not equal")
            .that(restoredParent.savedState).isEqualTo(parentFragment.savedState)
        assertWithMessage("parent fragment unsaved state was not equal")
            .that(restoredParent.unsavedState).isEqualTo(parentFragment.unsavedState)
        assertWithMessage("parent fragment has the same child FragmentManager")
            .that(restoredParent.childFragmentManager)
            .isNotSameInstanceAs(parentChildFragmentManager)

        val restoredChild = restoredParent
            .childFragmentManager.findFragmentByTag("tag:child") as StateSaveFragment
        assertWithMessage("child fragment not restored").that(restoredChild).isNotNull()

        assertWithMessage("child fragment instance state was saved")
            .that(restoredChild).isNotSameInstanceAs(childFragment)
        assertWithMessage("child fragment saved state was not equal")
            .that(restoredChild.savedState).isEqualTo(childFragment.savedState)
        assertWithMessage("child fragment saved state was unexpectedly equal")
            .that(restoredChild.unsavedState).isNotEqualTo(childFragment.unsavedState)

        fc2.dispatchActivityCreated()
        fc2.noteStateNotSaved()
        fc2.execPendingActions()
        fc2.dispatchStart()
        fc2.dispatchResume()
        fc2.execPendingActions()

        // Test that the fragments are in the configuration we expect

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)

        assertWithMessage("grandparent not destroyed")
            .that(restoredGrandparent.calledOnDestroy).isTrue()
        assertWithMessage("parent not destroyed").that(restoredParent.calledOnDestroy).isTrue()
        assertWithMessage("child not destroyed").that(restoredChild.calledOnDestroy).isTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThreadTest
    fun restoreRetainedInstanceFragmentWithTransparentActivityConfigChange() {
        // Create a new FragmentManager in isolation, add a retained instance Fragment,
        // then mimic the following scenario:
        // 1. Activity A adds retained Fragment F
        // 2. Activity A starts translucent Activity B
        // 3. Activity B start opaque Activity C
        // 4. Rotate phone
        // 5. Finish Activity C
        // 6. Finish Activity B

        val viewModelStore = ViewModelStore()
        val fc1 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm1 = fc1.supportFragmentManager

        fc1.attachHost(null)
        fc1.dispatchCreate()

        // Add the retained Fragment
        val retainedFragment = StateSaveFragment("Retained", "UnsavedRetained", true)
        fm1.beginTransaction().add(retainedFragment, "tag:retained").commitNow()

        // Move the activity to resumed
        fc1.dispatchActivityCreated()
        fc1.noteStateNotSaved()
        fc1.execPendingActions()
        fc1.dispatchStart()
        fc1.dispatchResume()
        fc1.execPendingActions()

        // Launch the transparent activity on top
        fc1.dispatchPause()

        // Launch the opaque activity on top
        val savedState = fc1.saveAllState()
        fc1.dispatchStop()

        // Finish the opaque activity, making our Activity visible i.e., started
        fc1.noteStateNotSaved()
        fc1.execPendingActions()
        fc1.dispatchStart()

        // Add another Fragment while we're started
        val retainedOnStartFragment = StateSaveFragment("onStart", "onStart", true)
        fm1.beginTransaction().add(retainedOnStartFragment, "tag:onStart").commitNow()

        // Finish the transparent activity, causing a config change
        fc1.dispatchStop()
        fc1.dispatchDestroy()

        // Create the new controller and restore state
        val fc2 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm2 = fc2.supportFragmentManager

        fc2.attachHost(null)
        fc2.restoreSaveState(savedState)
        fc2.dispatchCreate()

        val restoredFragment = fm2.findFragmentByTag("tag:retained") as StateSaveFragment
        assertWithMessage("retained fragment not restored").that(restoredFragment).isNotNull()
        assertWithMessage("The retained Fragment shouldn't be recreated")
            .that(restoredFragment).isEqualTo(retainedFragment)
        val restoredOnStartFragment = fm2.findFragmentByTag("tag:onStart") as StateSaveFragment?
        assertWithMessage("Retained Fragment added after saved state shouldn't be restored")
            .that(restoredOnStartFragment)
            .isNull()
        assertWithMessage("Retained Fragment added after saved state should be destroyed")
            .that(retainedOnStartFragment.calledOnDestroy)
            .isTrue()
        assertWithMessage("Retained Fragment should be removed from non config")
            .that(fm2.fragmentStore.nonConfig.retainedFragments)
            .containsExactly(retainedFragment)

        fc2.dispatchActivityCreated()
        fc2.noteStateNotSaved()
        fc2.execPendingActions()
        fc2.dispatchStart()
        fc2.dispatchResume()
        fc2.execPendingActions()

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThreadTest
    fun testSavedInstanceStateAfterRestore() {

        val viewModelStore = ViewModelStore()
        val fc1 = activityRule.startupFragmentController(viewModelStore)
        val fm1 = fc1.supportFragmentManager

        // Add the initial state
        val parentFragment = StrictFragment()
        parentFragment.retainInstance = true
        val childFragment = StrictFragment()
        fm1.beginTransaction().add(parentFragment, "parent").commitNow()
        val childFragmentManager = parentFragment.childFragmentManager
        childFragmentManager.beginTransaction().add(childFragment, "child").commitNow()

        // Confirm the initial state
        assertWithMessage("Initial parent saved instance state should be null")
            .that(parentFragment.lastSavedInstanceState).isNull()
        assertWithMessage("Initial child saved instance state should be null")
            .that(childFragment.lastSavedInstanceState).isNull()

        // Bring the state back down to destroyed, simulating an activity restart
        fc1.dispatchPause()
        val savedState = fc1.saveAllState()
        fc1.dispatchStop()
        fc1.dispatchDestroy()

        // Create the new controller and restore state
        val fc2 = activityRule.startupFragmentController(viewModelStore, savedState)
        val fm2 = fc2.supportFragmentManager

        val restoredParentFragment = fm2.findFragmentByTag("parent") as StrictFragment
        assertWithMessage("Parent fragment was not restored")
            .that(restoredParentFragment).isNotNull()
        val restoredChildFragment = restoredParentFragment
            .childFragmentManager.findFragmentByTag("child") as StrictFragment
        assertWithMessage("Child fragment was not restored").that(restoredChildFragment).isNotNull()

        assertWithMessage(
            "Parent fragment saved instance state should still be null since it is " +
                "a retained Fragment"
        ).that(restoredParentFragment.lastSavedInstanceState).isNull()
        assertWithMessage("Child fragment saved instance state should be non-null")
            .that(restoredChildFragment.lastSavedInstanceState).isNotNull()

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)
    }

    @Suppress("DEPRECATION")
    @Test
    @UiThreadTest
    fun restoreNestedFragmentsOnBackStack() {
        val viewModelStore = ViewModelStore()
        val fc1 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm1 = fc1.supportFragmentManager

        fc1.attachHost(null)
        fc1.dispatchCreate()

        // Add the initial state
        val parentFragment = StrictFragment()
        val childFragment = StrictFragment()
        fm1.beginTransaction().add(parentFragment, "parent").commitNow()
        val childFragmentManager = parentFragment.childFragmentManager
        childFragmentManager.beginTransaction().add(childFragment, "child").commitNow()

        // Now add a Fragment to the back stack
        val replacementChildFragment = StrictFragment()
        childFragmentManager.beginTransaction()
            .remove(childFragment)
            .add(replacementChildFragment, "child")
            .addToBackStack("back_stack").commit()
        childFragmentManager.executePendingTransactions()

        // Move the activity to resumed
        fc1.dispatchActivityCreated()
        fc1.noteStateNotSaved()
        fc1.execPendingActions()
        fc1.dispatchStart()
        fc1.dispatchResume()
        fc1.execPendingActions()

        // Now bring the state back down
        fc1.dispatchPause()
        val savedState = fc1.saveAllState()
        fc1.dispatchStop()
        fc1.dispatchDestroy()

        // Create the new controller and restore state
        val fc2 = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )

        val fm2 = fc2.supportFragmentManager

        fc2.attachHost(null)
        fc2.restoreSaveState(savedState)
        fc2.dispatchCreate()

        val restoredParentFragment = fm2.findFragmentByTag("parent") as StrictFragment
        assertWithMessage("Parent fragment was not restored")
            .that(restoredParentFragment).isNotNull()
        val restoredChildFragment = restoredParentFragment
            .childFragmentManager.findFragmentByTag("child") as StrictFragment
        assertWithMessage("Child fragment was not restored").that(restoredChildFragment).isNotNull()

        fc2.dispatchActivityCreated()
        fc2.noteStateNotSaved()
        fc2.execPendingActions()
        fc2.dispatchStart()
        fc2.dispatchResume()
        fc2.execPendingActions()

        // Bring the state back down to destroyed before we finish the test
        fc2.shutdown(viewModelStore)
    }

    /**
     * When a fragment has been optimized out, it state should still be saved during
     * save and restore instance state.
     */
    @Test
    @UiThreadTest
    fun saveRemovedFragment() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        var fragment1 = StateSaveFragment("1")
        fm.beginTransaction()
            .add(android.R.id.content, fragment1, "1")
            .addToBackStack(null)
            .commit()
        var fragment2 = StateSaveFragment("2")
        fm.beginTransaction()
            .replace(android.R.id.content, fragment2, "2")
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()

        // fragment on backstack should be in CREATED state and removed
        assertThat(fragment1.currentState).isEqualTo(StrictFragment.State.CREATED)
        assertThat(fragment1.isRemoving).isTrue()

        fc = fc.restart(activityRule, viewModelStore)
        fm = fc.supportFragmentManager

        fragment2 = fm.findFragmentByTag("2") as StateSaveFragment
        assertThat(fragment2).isNotNull()
        assertThat(fragment2.savedState).isEqualTo("2")

        fragment1 = fm.findFragmentByTag("1") as StateSaveFragment
        // make sure fragment is same as when it was saved
        assertThat(fragment1.currentState).isEqualTo(StrictFragment.State.CREATED)
        assertThat(fragment1.isRemoving).isTrue()

        fm.popBackStackImmediate()
        assertThat(fragment1).isNotNull()
        assertThat(fragment1.savedState).isEqualTo("1")
        assertThat(fragment1.isRemoving).isFalse()
    }

    /**
     * Test to ensure that the maxLifecycleState is of a fragment is saved and restored properly
     * when using CREATED.
     */
    @Test
    @UiThreadTest
    fun saveFragmentMaxLifecycleCreated() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        var fragment1 = StateSaveFragment("1")
        fm.beginTransaction()
            .add(android.R.id.content, fragment1, "1")
            .addToBackStack(null)
            .setMaxLifecycle(fragment1, Lifecycle.State.CREATED)
            .commit()
        fm.executePendingTransactions()

        fc = fc.restart(activityRule, viewModelStore)
        fm = fc.supportFragmentManager
        fragment1 = fm.findFragmentByTag("1") as StateSaveFragment
        assertThat(fragment1).isNotNull()
        assertThat(fragment1.savedState).isEqualTo("1")
        assertThat(fragment1.mMaxState).isEqualTo(Lifecycle.State.CREATED)
    }

    /**
     * Test to ensure that the maxLifecycleState is of a fragment is saved and restored properly
     * when using STARTED
     */
    @Test
    @UiThreadTest
    fun saveFragmentMaxLifecycleStarted() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        var fragment1 = StateSaveFragment("1")
        fm.beginTransaction()
            .add(android.R.id.content, fragment1, "1")
            .addToBackStack(null)
            .setMaxLifecycle(fragment1, Lifecycle.State.STARTED)
            .commit()
        fm.executePendingTransactions()

        fc = fc.restart(activityRule, viewModelStore)
        fm = fc.supportFragmentManager
        fragment1 = fm.findFragmentByTag("1") as StateSaveFragment
        assertThat(fragment1).isNotNull()
        assertThat(fragment1.savedState).isEqualTo("1")
        assertThat(fragment1.mMaxState).isEqualTo(Lifecycle.State.STARTED)
    }

    /**
     * Test to ensure that when dispatch* is called that the fragment manager
     * doesn't cause the contained fragment states to change even if no state changes.
     */
    @Test
    @UiThreadTest
    fun noPrematureStateChange() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        fm.beginTransaction().add(StrictFragment(), "1").commitNow()

        fc = fc.restart(activityRule, viewModelStore)

        fm = fc.supportFragmentManager

        val fragment1 = fm.findFragmentByTag("1") as StrictFragment
        assertWithMessage("Fragment should be resumed after restart")
            .that(fragment1.calledOnResume).isTrue()
        fragment1.calledOnResume = false
        fc.dispatchResume()

        assertWithMessage("Fragment should not get onResume() after second dispatchResume()")
            .that(fragment1.calledOnResume).isFalse()
    }

    @Test
    @UiThreadTest
    fun testIsStateSaved() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)
        val fm = fc.supportFragmentManager

        val f = StrictFragment()
        fm.beginTransaction().add(f, "1").commitNow()

        assertWithMessage("fragment reported state saved while resumed")
            .that(f.isStateSaved).isFalse()

        fc.dispatchPause()
        @Suppress("DEPRECATION")
        fc.saveAllState()

        assertWithMessage("fragment reported state not saved after saveAllState")
            .that(f.isStateSaved).isTrue()

        fc.dispatchStop()

        assertWithMessage("fragment reported state not saved after stop")
            .that(f.isStateSaved).isTrue()

        viewModelStore.clear()
        fc.dispatchDestroy()

        assertWithMessage("fragment reported state saved after destroy")
            .that(f.isStateSaved).isFalse()
    }

    @Test
    @UiThreadTest
    fun saveAnimationState() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        fm.beginTransaction()
            .setCustomAnimations(0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
            .add(android.R.id.content, StrictViewFragment(R.layout.fragment_a))
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)

        // Causes save and restore of fragments and back stack
        fc = fc.restart(activityRule, viewModelStore)
        fm = fc.supportFragmentManager

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)

        fm.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, 0, 0)
            .replace(android.R.id.content, StrictViewFragment(R.layout.fragment_b))
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()

        assertAnimationsMatch(fm, R.anim.fade_in, R.anim.fade_out, 0, 0)

        // Causes save and restore of fragments and back stack
        fc = fc.restart(activityRule, viewModelStore)
        fm = fc.supportFragmentManager

        assertAnimationsMatch(fm, R.anim.fade_in, R.anim.fade_out, 0, 0)

        fm.popBackStackImmediate()

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)

        fc.shutdown(viewModelStore)
    }

    private fun executePendingTransactions(fm: FragmentManager) {
        activityRule.runOnUiThread { fm.executePendingTransactions() }
    }

    private fun assertAnimationsMatch(
        fm: FragmentManager,
        enter: Int,
        exit: Int,
        popEnter: Int,
        popExit: Int
    ) {
        val record = fm.mBackStack[fm.mBackStack.size - 1]

        assertThat(record.mEnterAnim).isEqualTo(enter)
        assertThat(record.mExitAnim).isEqualTo(exit)
        assertThat(record.mPopEnterAnim).isEqualTo(popEnter)
        assertThat(record.mPopExitAnim).isEqualTo(popExit)
    }

    class SaveViewStateFragment : StrictViewFragment(R.layout.with_edit_text)
}