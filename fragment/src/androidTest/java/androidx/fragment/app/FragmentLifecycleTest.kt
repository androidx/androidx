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

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@MediumTest
class FragmentLifecycleTest {

    @get:Rule
    val activityRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    fun basicLifecycle() {
        val fm = activityRule.activity.supportFragmentManager
        val strictFragment = StrictFragment()

        // Add fragment; StrictFragment will throw if it detects any violation
        // in standard lifecycle method ordering or expected preconditions.
        fm.beginTransaction().add(strictFragment, "EmptyHeadless").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment is not added").that(strictFragment.isAdded).isTrue()
        assertWithMessage("fragment is detached").that(strictFragment.isDetached).isFalse()
        assertWithMessage("fragment is not resumed").that(strictFragment.isResumed).isTrue()
        val lifecycle = strictFragment.lifecycle
        assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        // Test removal as well; StrictFragment will throw here too.
        fm.beginTransaction().remove(strictFragment).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment is added").that(strictFragment.isAdded).isFalse()
        assertWithMessage("fragment is resumed").that(strictFragment.isResumed).isFalse()
        assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        // Once removed, a new Lifecycle should be created just in case
        // the developer reuses the same Fragment
        assertThat(strictFragment.lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

        // This one is perhaps counterintuitive; "detached" means specifically detached
        // but still managed by a FragmentManager. The .remove call above
        // should not enter this state.
        assertWithMessage("fragment is detached").that(strictFragment.isDetached).isFalse()
    }

    @Test
    fun detachment() {
        val fm = activityRule.activity.supportFragmentManager
        val f1 = StrictFragment()
        val f2 = StrictFragment()

        fm.beginTransaction().add(f1, "1").add(f2, "2").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()
        assertWithMessage("fragment 2 is not added").that(f2.isAdded).isTrue()

        // Test detaching fragments using StrictFragment to throw on errors.
        fm.beginTransaction().detach(f1).detach(f2).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not detached").that(f1.isDetached).isTrue()
        assertWithMessage("fragment 2 is not detached").that(f2.isDetached).isTrue()
        assertWithMessage("fragment 1 is added").that(f1.isAdded).isFalse()
        assertWithMessage("fragment 2 is added").that(f2.isAdded).isFalse()

        // Only reattach f1; leave v2 detached.
        fm.beginTransaction().attach(f1).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()
        assertWithMessage("fragment 1 is detached").that(f1.isDetached).isFalse()
        assertWithMessage("fragment 2 is not detached").that(f2.isDetached).isTrue()

        // Remove both from the FragmentManager.
        fm.beginTransaction().remove(f1).remove(f2).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is added").that(f1.isAdded).isFalse()
        assertWithMessage("fragment 2 is added").that(f2.isAdded).isFalse()
        assertWithMessage("fragment 1 is detached").that(f1.isDetached).isFalse()
        assertWithMessage("fragment 2 is detached").that(f2.isDetached).isFalse()
    }

    @Test
    fun basicBackStack() {
        val fm = activityRule.activity.supportFragmentManager
        val f1 = StrictFragment()
        val f2 = StrictFragment()

        // Add a fragment normally to set up
        fm.beginTransaction().add(f1, "1").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()

        // Remove the first one and add a second. We're not using replace() here since
        // these fragments are headless and as of this test writing, replace() only works
        // for fragments with views and a container view id.
        // Add it to the back stack so we can pop it afterwards.
        fm.beginTransaction().remove(f1).add(f2, "2").addToBackStack("stack1").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is added").that(f1.isAdded).isFalse()
        assertWithMessage("fragment 2 is not added").that(f2.isAdded).isTrue()

        // Test popping the stack
        fm.popBackStack()
        executePendingTransactions(fm)

        assertWithMessage("fragment 2 is added").that(f2.isAdded).isFalse()
        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()
    }

    @Test
    fun attachBackStack() {
        val fm = activityRule.activity.supportFragmentManager
        val f1 = StrictFragment()
        val f2 = StrictFragment()

        // Add a fragment normally to set up
        fm.beginTransaction().add(f1, "1").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()

        fm.beginTransaction().detach(f1).add(f2, "2").addToBackStack("stack1").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not detached").that(f1.isDetached).isTrue()
        assertWithMessage("fragment 2 is detached").that(f2.isDetached).isFalse()
        assertWithMessage("fragment 1 is added").that(f1.isAdded).isFalse()
        assertWithMessage("fragment 2 is not added").that(f2.isAdded).isTrue()
    }

    @Test
    fun viewLifecycle() {
        // Test basic lifecycle when the fragment creates a view

        val fm = activityRule.activity.supportFragmentManager
        val f1 = StrictViewFragment()

        fm.beginTransaction().add(android.R.id.content, f1).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()
        val view = f1.requireView()
        assertWithMessage("fragment 1 returned null from getView").that(view).isNotNull()
        assertWithMessage("fragment 1's view is not attached to a window")
            .that(ViewCompat.isAttachedToWindow(view)).isTrue()

        fm.beginTransaction().remove(f1).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is added").that(f1.isAdded).isFalse()
        assertWithMessage("fragment 1 returned non-null from getView after removal")
            .that(f1.view).isNull()
        assertWithMessage("fragment 1's previous view is still attached to a window")
            .that(ViewCompat.isAttachedToWindow(view)).isFalse()
    }

    @Test
    fun viewReplace() {
        // Replace one view with another, then reverse it with the back stack

        val fm = activityRule.activity.supportFragmentManager
        val f1 = StrictViewFragment()
        val f2 = StrictViewFragment()

        fm.beginTransaction().add(android.R.id.content, f1).commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()

        val origView1 = f1.requireView()
        assertWithMessage("fragment 1 returned null view").that(origView1).isNotNull()
        assertWithMessage("fragment 1's view not attached")
            .that(ViewCompat.isAttachedToWindow(origView1)).isTrue()

        fm.beginTransaction().replace(android.R.id.content, f2).addToBackStack("stack1").commit()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is added").that(f1.isAdded).isFalse()
        assertWithMessage("fragment 2 is added").that(f2.isAdded).isTrue()
        assertWithMessage("fragment 1 returned non-null view").that(f1.view).isNull()
        assertWithMessage("fragment 1's old view still attached")
            .that(ViewCompat.isAttachedToWindow(origView1)).isFalse()
        val origView2 = f2.requireView()
        assertWithMessage("fragment 2 returned null view").that(origView2).isNotNull()
        assertWithMessage("fragment 2's view not attached")
            .that(ViewCompat.isAttachedToWindow(origView2)).isTrue()

        fm.popBackStack()
        executePendingTransactions(fm)

        assertWithMessage("fragment 1 is not added").that(f1.isAdded).isTrue()
        assertWithMessage("fragment 2 is added").that(f2.isAdded).isFalse()
        assertWithMessage("fragment 2 returned non-null view").that(f2.view).isNull()
        assertWithMessage("fragment 2's view still attached")
            .that(ViewCompat.isAttachedToWindow(origView2)).isFalse()
        val newView1 = f1.requireView()
        assertWithMessage("fragment 1 had same view from last attachment")
            .that(newView1).isNotSameAs(origView1)
        assertWithMessage("fragment 1's view not attached")
            .that(ViewCompat.isAttachedToWindow(newView1)).isTrue()
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycle() {
        val viewModelStore = ViewModelStore()
        val fc = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )
        fc.attachHost(null)
        fc.dispatchCreate()

        val fm = fc.supportFragmentManager

        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
            .commitNow()

        fc.dispatchActivityCreated()
        fc.dispatchStart()
        fc.dispatchResume()

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycleForceState() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

        fm.beginTransaction()
            .setMaxLifecycle(fragment, Lifecycle.State.CREATED)
            .commitNow()

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    @UiThreadTest
    fun setMaxLifecyclePop() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val fragment = StrictViewFragment()
        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.CREATED)
            .commitNow()

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)

        fm.beginTransaction()
            .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
            .addToBackStack(null)
            .commit()
        executePendingTransactions(fm)

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)

        fm.popBackStackImmediate()

        assertThat(fragment.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    @Test
    @UiThreadTest
    fun setMaxLifecycleOnDifferentFragments() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)

        val fm = fc.supportFragmentManager

        val fragment1 = StrictViewFragment()
        val fragment2 = StrictViewFragment()
        fm.beginTransaction()
            .add(android.R.id.content, fragment1)
            .add(android.R.id.content, fragment2)
            .setMaxLifecycle(fragment1, Lifecycle.State.STARTED)
            .setMaxLifecycle(fragment2, Lifecycle.State.CREATED)
            .commitNow()

        assertThat(fragment1.lifecycle.currentState).isEqualTo(Lifecycle.State.STARTED)
        assertThat(fragment2.lifecycle.currentState).isEqualTo(Lifecycle.State.CREATED)
    }

    /**
     * This test confirms that as long as a parent fragment has called super.onCreate,
     * any child fragments added, committed and with transactions executed will be brought
     * to at least the CREATED state by the time the parent fragment receives onCreateView.
     * This means the child fragment will have received onAttach/onCreate.
     */
    @Test
    @UiThreadTest
    fun childFragmentManagerAttach() {
        val viewModelStore = ViewModelStore()
        val fc = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )
        fc.attachHost(null)
        fc.dispatchCreate()

        val mockLc = mock(FragmentManager.FragmentLifecycleCallbacks::class.java)
        val mockRecursiveLc = mock(FragmentManager.FragmentLifecycleCallbacks::class.java)

        val fm = fc.supportFragmentManager
        fm.registerFragmentLifecycleCallbacks(mockLc, false)
        fm.registerFragmentLifecycleCallbacks(mockRecursiveLc, true)

        val fragment = ChildFragmentManagerFragment()
        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        verify<FragmentManager.FragmentLifecycleCallbacks>(mockLc, times(1))
            .onFragmentCreated(fm, fragment, null)

        fc.dispatchActivityCreated()

        val childFragment = fragment.childFragment!!

        verify<FragmentLifecycleCallbacks>(mockLc, times(1))
            .onFragmentActivityCreated(fm, fragment, null)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentActivityCreated(fm, fragment, null)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentActivityCreated(fm, childFragment, null)

        fc.dispatchStart()

        verify<FragmentLifecycleCallbacks>(mockLc, times(1)).onFragmentStarted(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentStarted(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentStarted(fm, childFragment)

        fc.dispatchResume()

        verify<FragmentLifecycleCallbacks>(mockLc, times(1)).onFragmentResumed(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentResumed(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentResumed(fm, childFragment)

        // Confirm that the parent fragment received onAttachFragment
        assertWithMessage("parent fragment did not receive onAttachFragment")
            .that(fragment.calledOnAttachFragment).isTrue()

        fc.dispatchStop()

        verify<FragmentLifecycleCallbacks>(mockLc, times(1)).onFragmentStopped(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentStopped(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentStopped(fm, childFragment)

        viewModelStore.clear()
        fc.dispatchDestroy()

        verify<FragmentLifecycleCallbacks>(mockLc, times(1)).onFragmentDestroyed(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentDestroyed(fm, fragment)
        verify<FragmentLifecycleCallbacks>(mockRecursiveLc, times(1))
            .onFragmentDestroyed(fm, childFragment)
    }

    /**
     * This test checks that FragmentLifecycleCallbacks are invoked when expected.
     */
    @Test
    @UiThreadTest
    fun fragmentLifecycleCallbacks() {
        val viewModelStore = ViewModelStore()
        val fc = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore)
        )
        fc.attachHost(null)
        fc.dispatchCreate()

        val fm = fc.supportFragmentManager

        val fragment = ChildFragmentManagerFragment()
        fm.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        fc.dispatchActivityCreated()

        fc.dispatchStart()
        fc.dispatchResume()

        // Confirm that the parent fragment received onAttachFragment
        assertWithMessage("parent fragment did not receive onAttachFragment")
            .that(fragment.calledOnAttachFragment).isTrue()

        fc.shutdown(viewModelStore)
    }

    /**
     * This tests that fragments call onDestroy when the activity finishes.
     */
    @Test
    @UiThreadTest
    fun fragmentDestroyedOnFinish() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)
        val fm = fc.supportFragmentManager

        val fragmentA = StrictViewFragment(R.layout.fragment_a)
        val fragmentB = StrictViewFragment(R.layout.fragment_b)
        fm.beginTransaction()
            .add(android.R.id.content, fragmentA)
            .commit()
        fm.executePendingTransactions()
        fm.beginTransaction()
            .replace(android.R.id.content, fragmentB)
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()
        fc.shutdown(viewModelStore)
        assertThat(fragmentB.calledOnDestroy).isTrue()
        assertThat(fragmentA.calledOnDestroy).isTrue()
    }

    @Test
    @UiThreadTest
    fun testSetArgumentsLifecycle() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)
        val fm = fc.supportFragmentManager

        val f = StrictFragment()
        f.arguments = Bundle()

        fm.beginTransaction().add(f, "1").commitNow()

        f.arguments = Bundle()

        fc.dispatchPause()
        fc.saveAllState()

        try {
            f.arguments = Bundle()
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains("Fragment already added and state has been saved")
        }

        fc.dispatchStop()

        try {
            f.arguments = Bundle()
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat().contains("Fragment already added and state has been saved")
        }

        viewModelStore.clear()
        fc.dispatchDestroy()

        // Fully destroyed, so fragments have been removed.
        f.arguments = Bundle()
    }

    /**
     * Ensure that FragmentManager rejects commit() and commitNow() prior to restoring
     * saved instance state
     */
    @Test
    @UiThreadTest
    fun addRetainedBeforeRestoreSaveState() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        val fm = fc.supportFragmentManager

        val fragment1 = StrictFragment()
        fragment1.retainInstance = true
        fm.beginTransaction()
            .add(fragment1, "1")
            .commitNow()

        fc.shutdown(viewModelStore, false)

        fc = FragmentController.createController(
            ControllerHostCallbacks(activityRule.activity, viewModelStore))

        // Now before we restoreSaveState, add a retained Fragment
        val fragment2 = StrictFragment()
        fragment2.retainInstance = true
        try {
            fc.supportFragmentManager.beginTransaction()
                .add(fragment2, "2")
                .commitNow()
            fail("commitNow() should fail prior to onCreate")
        } catch (expected: IllegalStateException) {
        }
        try {
            fc.supportFragmentManager.beginTransaction()
                .add(fragment2, "2")
                .commit()
            fail("commit() should fail prior to onCreate")
        } catch (expected: IllegalStateException) {
        }
    }

    /**
     * Ensure that FragmentManager
     */
    @Test
    @UiThreadTest
    fun addRetainedAfterSaveState() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        val fragment1 = StrictFragment()
        fragment1.retainInstance = true
        fm.beginTransaction()
            .add(fragment1, "1")
            .commitNow()

        // Now save the state of the FragmentManager
        fc.dispatchPause()
        val savedState = fc.saveAllState()

        val fragment2 = StrictFragment()
        fragment2.retainInstance = true
        fm.beginTransaction()
            .add(fragment2, "2")
            .commitNowAllowingStateLoss()

        fc.dispatchStop()
        fc.dispatchDestroy()

        fc = activityRule.startupFragmentController(viewModelStore, savedState)
        fm = fc.supportFragmentManager

        assertThat(fm.findFragmentByTag("1"))
            .isSameAs(fragment1)
        assertThat(fm.findFragmentByTag("2"))
            .isNull()
    }

    /**
     * When a fragment is saved in non-config, it should be restored to the same index.
     */
    @Test
    @UiThreadTest
    fun restoreNonConfig() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        val fm = fc.supportFragmentManager

        val backStackRetainedFragment = StrictFragment()
        backStackRetainedFragment.retainInstance = true
        val fragment1 = StrictFragment()
        fm.beginTransaction()
            .add(backStackRetainedFragment, "backStack")
            .add(fragment1, "1")
            .setPrimaryNavigationFragment(fragment1)
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()
        val fragment2 = StrictFragment()
        fragment2.retainInstance = true
        fragment2.setTargetFragment(fragment1, 0)
        val fragment3 = StrictFragment()
        fm.beginTransaction()
            .remove(backStackRetainedFragment)
            .remove(fragment1)
            .add(fragment2, "2")
            .add(fragment3, "3")
            .addToBackStack(null)
            .commit()
        fm.executePendingTransactions()

        fc = fc.restart(activityRule, viewModelStore, false)
        var foundFragment2 = false
        for (fragment in fc.supportFragmentManager.fragments) {
            if (fragment === fragment2) {
                foundFragment2 = true
                assertThat(fragment.getTargetFragment()).isNotNull()
                assertThat(fragment.getTargetFragment()!!.tag).isEqualTo("1")
            } else {
                assertThat(fragment.tag).isNotEqualTo("2")
            }
        }
        assertThat(foundFragment2).isTrue()
        fc.supportFragmentManager.popBackStackImmediate()
        val foundBackStackRetainedFragment = fc.supportFragmentManager
            .findFragmentByTag("backStack")
        assertWithMessage("Retained Fragment on the back stack was not retained")
            .that(foundBackStackRetainedFragment).isEqualTo(backStackRetainedFragment)
    }

    /**
     * Check that retained fragments in the backstack correctly restored after two "configChanges"
     */
    @Test
    @UiThreadTest
    fun retainedFragmentInBackstack() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        val fragment1 = StrictFragment()
        fm.beginTransaction().add(fragment1, "1").addToBackStack(null).commit()
        fm.executePendingTransactions()

        val child = StrictFragment()
        child.retainInstance = true
        fragment1.childFragmentManager.beginTransaction().add(child, "child").commit()
        fragment1.childFragmentManager.executePendingTransactions()

        val fragment2 = StrictFragment()
        fm.beginTransaction().remove(fragment1).add(fragment2, "2").addToBackStack(null).commit()
        fm.executePendingTransactions()

        fc = fc.restart(activityRule, viewModelStore, false)
        fc = fc.restart(activityRule, viewModelStore, false)
        fm = fc.supportFragmentManager
        fm.popBackStackImmediate()
        val retainedChild = fm.findFragmentByTag("1")!!
            .childFragmentManager.findFragmentByTag("child")
        assertThat(retainedChild).isEqualTo(child)
    }

    /**
     * When the FragmentManager state changes, the pending transactions should execute.
     */
    @Test
    @UiThreadTest
    fun runTransactionsOnChange() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        val fragment1 = RemoveHelloInOnResume()
        val fragment2 = StrictFragment()
        fm.beginTransaction().add(fragment1, "1").setReorderingAllowed(false).commit()
        fm.beginTransaction().add(fragment2, "Hello").setReorderingAllowed(false).commit()
        fm.executePendingTransactions()

        assertThat(fm.fragments.size).isEqualTo(2)
        assertThat(fm.fragments.contains(fragment1)).isTrue()
        assertThat(fm.fragments.contains(fragment2)).isTrue()

        fc = fc.restart(activityRule, viewModelStore)
        fm = fc.supportFragmentManager

        assertThat(fm.fragments.size).isEqualTo(1)
        for (fragment in fm.fragments) {
            assertThat(fragment is RemoveHelloInOnResume).isTrue()
        }
    }

    @Test
    @UiThreadTest
    fun optionsMenu() {
        val viewModelStore = ViewModelStore()
        val fc = activityRule.startupFragmentController(viewModelStore)
        val fm = fc.supportFragmentManager

        val fragment = InvalidateOptionFragment()
        fm.beginTransaction().add(android.R.id.content, fragment).commit()
        fm.executePendingTransactions()

        val menu = mock(Menu::class.java)
        fc.dispatchPrepareOptionsMenu(menu)
        assertThat(fragment.onPrepareOptionsMenuCalled).isTrue()
        fragment.onPrepareOptionsMenuCalled = false
        fc.shutdown(viewModelStore)
        fc.dispatchPrepareOptionsMenu(menu)
        assertThat(fragment.onPrepareOptionsMenuCalled).isFalse()
    }

    /**
     * When a retained instance fragment is saved while in the back stack, it should go
     * through onCreate() when it is popped back.
     */
    @Test
    @UiThreadTest
    fun retainInstanceWithOnCreate() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        val fragment1 = OnCreateFragment()

        fm.beginTransaction().add(fragment1, "1").commit()
        fm.beginTransaction().remove(fragment1).addToBackStack(null).commit()

        fc = fc.restart(activityRule, viewModelStore)

        // Save again, but keep the state
        fc = fc.restart(activityRule, viewModelStore, false)

        fm = fc.supportFragmentManager

        fm.popBackStackImmediate()
        val fragment2 = fm.findFragmentByTag("1") as OnCreateFragment
        assertThat(fragment2.onCreateCalled).isTrue()
        fm.popBackStackImmediate()
    }

    /**
     * A retained instance fragment should go through onCreate() once, even through save and
     * restore.
     */
    @Test
    @UiThreadTest
    fun retainInstanceOneOnCreate() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        val fragment = OnCreateFragment()

        fm.beginTransaction().add(fragment, "fragment").commit()
        fm.executePendingTransactions()

        fm.beginTransaction().remove(fragment).addToBackStack(null).commit()

        assertThat(fragment.onCreateCalled).isTrue()
        fragment.onCreateCalled = false

        fc = fc.restart(activityRule, viewModelStore, false)
        fm = fc.supportFragmentManager

        fm.popBackStackImmediate()
        assertThat(fragment.onCreateCalled).isFalse()
    }

    /**
     * A retained instance fragment added via XML should go through onCreate() once, but should get
     * onInflate calls for each inflation.
     */
    @Test
    @UiThreadTest
    fun retainInstanceLayoutOnInflate() {
        val viewModelStore = ViewModelStore()
        var fc = activityRule.startupFragmentController(viewModelStore)
        var fm = fc.supportFragmentManager

        var parentFragment = RetainedInflatedParentFragment()

        fm.beginTransaction().add(android.R.id.content, parentFragment).commit()
        fm.executePendingTransactions()

        val childFragment = parentFragment.childFragmentManager
            .findFragmentById(R.id.child_fragment) as RetainedInflatedChildFragment

        fm.beginTransaction().remove(parentFragment).addToBackStack(null).commit()

        fc = fc.restart(activityRule, viewModelStore, false)
        fm = fc.supportFragmentManager

        fm.popBackStackImmediate()

        parentFragment = fm.findFragmentById(android.R.id.content) as RetainedInflatedParentFragment
        val childFragment2 = parentFragment.childFragmentManager
            .findFragmentById(R.id.child_fragment) as RetainedInflatedChildFragment

        assertWithMessage("Child Fragment should be retained")
            .that(childFragment2).isEqualTo(childFragment)
        assertWithMessage("Child Fragment should have onInflate called twice")
            .that(childFragment2.mOnInflateCount).isEqualTo(2)
    }

    private fun executePendingTransactions(fm: FragmentManager) {
        activityRule.runOnUiThread { fm.executePendingTransactions() }
    }

    /**
     * This tests a deliberately odd use of a child fragment, added in onCreateView instead
     * of elsewhere. It simulates creating a UI child fragment added to the view hierarchy
     * created by this fragment.
     */
    class ChildFragmentManagerFragment : StrictFragment() {
        private lateinit var savedChildFragmentManager: FragmentManager
        private lateinit var childFragmentManagerChildFragment: ChildFragmentManagerChildFragment

        val childFragment: Fragment?
            get() = childFragmentManagerChildFragment

        override fun onAttach(context: Context) {
            super.onAttach(context)
            savedChildFragmentManager = childFragmentManager
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = TextView(inflater.context).also {
            assertWithMessage("child FragmentManagers not the same instance")
                .that(childFragmentManager).isSameAs(savedChildFragmentManager)
            var child = savedChildFragmentManager
                .findFragmentByTag("tag") as ChildFragmentManagerChildFragment?
            if (child == null) {
                child = ChildFragmentManagerChildFragment("foo")
                savedChildFragmentManager.beginTransaction().add(child, "tag").commitNow()
                assertWithMessage("argument strings don't match")
                    .that(child.string).isEqualTo("foo")
            }
            childFragmentManagerChildFragment = child
        }
    }

    class ChildFragmentManagerChildFragment : StrictFragment {
        lateinit var string: String
            private set

        constructor()

        constructor(arg: String) {
            val b = Bundle()
            b.putString("string", arg)
            arguments = b
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            string = requireArguments().getString("string", "NO VALUE")
        }
    }

    class RemoveHelloInOnResume : Fragment() {
        override fun onResume() {
            super.onResume()
            val fragment = fragmentManager!!.findFragmentByTag("Hello")
            if (fragment != null) {
                fragmentManager!!.beginTransaction().remove(fragment).commit()
            }
        }
    }

    class InvalidateOptionFragment : Fragment() {
        var onPrepareOptionsMenuCalled: Boolean = false

        init {
            setHasOptionsMenu(true)
        }

        override fun onPrepareOptionsMenu(menu: Menu) {
            onPrepareOptionsMenuCalled = true
            assertThat(context).isNotNull()
            super.onPrepareOptionsMenu(menu)
        }
    }

    class OnCreateFragment : Fragment() {
        var onCreateCalled: Boolean = false

        init {
            retainInstance = true
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            onCreateCalled = true
        }
    }

    class RetainedInflatedParentFragment :
        Fragment(R.layout.nested_retained_inflated_fragment_parent)

    class RetainedInflatedChildFragment : Fragment(R.layout.nested_inflated_fragment_child) {
        internal var mOnInflateCount = 0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }

        override fun onInflate(context: Context, attrs: AttributeSet, savedInstanceState: Bundle?) {
            super.onInflate(context, attrs, savedInstanceState)
            mOnInflateCount++
        }
    }
}
