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


package androidx.fragment.app;

import static androidx.fragment.app.FragmentTestUtil.HostCallbacks;
import static androidx.fragment.app.FragmentTestUtil.shutdownFragmentController;
import static androidx.fragment.app.FragmentTestUtil.startupFragmentController;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ContentView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.test.EmptyFragmentTestActivity;
import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelStore;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class FragmentLifecycleTest {

    @Rule
    public ActivityTestRule<EmptyFragmentTestActivity> mActivityRule =
            new ActivityTestRule<EmptyFragmentTestActivity>(EmptyFragmentTestActivity.class);

    @Test
    public void basicLifecycle() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment strictFragment = new StrictFragment();

        // Add fragment; StrictFragment will throw if it detects any violation
        // in standard lifecycle method ordering or expected preconditions.
        fm.beginTransaction().add(strictFragment, "EmptyHeadless").commit();
        executePendingTransactions(fm);

        assertTrue("fragment is not added", strictFragment.isAdded());
        assertFalse("fragment is detached", strictFragment.isDetached());
        assertTrue("fragment is not resumed", strictFragment.isResumed());
        Lifecycle lifecycle = strictFragment.getLifecycle();
        assertThat(lifecycle.getCurrentState())
                .isEqualTo(Lifecycle.State.RESUMED);

        // Test removal as well; StrictFragment will throw here too.
        fm.beginTransaction().remove(strictFragment).commit();
        executePendingTransactions(fm);

        assertFalse("fragment is added", strictFragment.isAdded());
        assertFalse("fragment is resumed", strictFragment.isResumed());
        assertThat(lifecycle.getCurrentState())
                .isEqualTo(Lifecycle.State.DESTROYED);
        // Once removed, a new Lifecycle should be created just in case
        // the developer reuses the same Fragment
        assertThat(strictFragment.getLifecycle().getCurrentState())
                .isEqualTo(Lifecycle.State.INITIALIZED);

        // This one is perhaps counterintuitive; "detached" means specifically detached
        // but still managed by a FragmentManager. The .remove call above
        // should not enter this state.
        assertFalse("fragment is detached", strictFragment.isDetached());
    }

    @Test
    public void detachment() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment f1 = new StrictFragment();
        final StrictFragment f2 = new StrictFragment();

        fm.beginTransaction().add(f1, "1").add(f2, "2").commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());
        assertTrue("fragment 2 is not added", f2.isAdded());

        // Test detaching fragments using StrictFragment to throw on errors.
        fm.beginTransaction().detach(f1).detach(f2).commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not detached", f1.isDetached());
        assertTrue("fragment 2 is not detached", f2.isDetached());
        assertFalse("fragment 1 is added", f1.isAdded());
        assertFalse("fragment 2 is added", f2.isAdded());

        // Only reattach f1; leave v2 detached.
        fm.beginTransaction().attach(f1).commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());
        assertFalse("fragment 1 is detached", f1.isDetached());
        assertTrue("fragment 2 is not detached", f2.isDetached());

        // Remove both from the FragmentManager.
        fm.beginTransaction().remove(f1).remove(f2).commit();
        executePendingTransactions(fm);

        assertFalse("fragment 1 is added", f1.isAdded());
        assertFalse("fragment 2 is added", f2.isAdded());
        assertFalse("fragment 1 is detached", f1.isDetached());
        assertFalse("fragment 2 is detached", f2.isDetached());
    }

    @Test
    public void basicBackStack() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment f1 = new StrictFragment();
        final StrictFragment f2 = new StrictFragment();

        // Add a fragment normally to set up
        fm.beginTransaction().add(f1, "1").commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());

        // Remove the first one and add a second. We're not using replace() here since
        // these fragments are headless and as of this test writing, replace() only works
        // for fragments with views and a container view id.
        // Add it to the back stack so we can pop it afterwards.
        fm.beginTransaction().remove(f1).add(f2, "2").addToBackStack("stack1").commit();
        executePendingTransactions(fm);

        assertFalse("fragment 1 is added", f1.isAdded());
        assertTrue("fragment 2 is not added", f2.isAdded());

        // Test popping the stack
        fm.popBackStack();
        executePendingTransactions(fm);

        assertFalse("fragment 2 is added", f2.isAdded());
        assertTrue("fragment 1 is not added", f1.isAdded());
    }

    @Test
    public void attachBackStack() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment f1 = new StrictFragment();
        final StrictFragment f2 = new StrictFragment();

        // Add a fragment normally to set up
        fm.beginTransaction().add(f1, "1").commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());

        fm.beginTransaction().detach(f1).add(f2, "2").addToBackStack("stack1").commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not detached", f1.isDetached());
        assertFalse("fragment 2 is detached", f2.isDetached());
        assertFalse("fragment 1 is added", f1.isAdded());
        assertTrue("fragment 2 is not added", f2.isAdded());
    }

    @Test
    public void viewLifecycle() throws Throwable {
        // Test basic lifecycle when the fragment creates a view

        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment f1 = new StrictViewFragment();

        fm.beginTransaction().add(android.R.id.content, f1).commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());
        final View view = f1.getView();
        assertNotNull("fragment 1 returned null from getView", view);
        assertTrue("fragment 1's view is not attached to a window",
                ViewCompat.isAttachedToWindow(view));

        fm.beginTransaction().remove(f1).commit();
        executePendingTransactions(fm);

        assertFalse("fragment 1 is added", f1.isAdded());
        assertNull("fragment 1 returned non-null from getView after removal", f1.getView());
        assertFalse("fragment 1's previous view is still attached to a window",
                ViewCompat.isAttachedToWindow(view));
    }

    @Test
    public void viewReplace() throws Throwable {
        // Replace one view with another, then reverse it with the back stack

        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment f1 = new StrictViewFragment();
        final StrictViewFragment f2 = new StrictViewFragment();

        fm.beginTransaction().add(android.R.id.content, f1).commit();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());

        View origView1 = f1.getView();
        assertNotNull("fragment 1 returned null view", origView1);
        assertTrue("fragment 1's view not attached", ViewCompat.isAttachedToWindow(origView1));

        fm.beginTransaction().replace(android.R.id.content, f2).addToBackStack("stack1").commit();
        executePendingTransactions(fm);

        assertFalse("fragment 1 is added", f1.isAdded());
        assertTrue("fragment 2 is added", f2.isAdded());
        assertNull("fragment 1 returned non-null view", f1.getView());
        assertFalse("fragment 1's old view still attached",
                ViewCompat.isAttachedToWindow(origView1));
        View origView2 = f2.getView();
        assertNotNull("fragment 2 returned null view", origView2);
        assertTrue("fragment 2's view not attached", ViewCompat.isAttachedToWindow(origView2));

        fm.popBackStack();
        executePendingTransactions(fm);

        assertTrue("fragment 1 is not added", f1.isAdded());
        assertFalse("fragment 2 is added", f2.isAdded());
        assertNull("fragment 2 returned non-null view", f2.getView());
        assertFalse("fragment 2's view still attached", ViewCompat.isAttachedToWindow(origView2));
        View newView1 = f1.getView();
        assertNotSame("fragment 1 had same view from last attachment", origView1, newView1);
        assertTrue("fragment 1's view not attached", ViewCompat.isAttachedToWindow(newView1));
    }

    /**
     * This test confirms that as long as a parent fragment has called super.onCreate,
     * any child fragments added, committed and with transactions executed will be brought
     * to at least the CREATED state by the time the parent fragment receives onCreateView.
     * This means the child fragment will have received onAttach/onCreate.
     */
    @Test
    @UiThreadTest
    public void childFragmentManagerAttach() throws Throwable {
        final ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));
        fc.attachHost(null);
        fc.dispatchCreate();

        FragmentManager.FragmentLifecycleCallbacks
                mockLc = mock(FragmentManager.FragmentLifecycleCallbacks.class);
        FragmentManager.FragmentLifecycleCallbacks
                mockRecursiveLc = mock(FragmentManager.FragmentLifecycleCallbacks.class);

        FragmentManager fm = fc.getSupportFragmentManager();
        fm.registerFragmentLifecycleCallbacks(mockLc, false);
        fm.registerFragmentLifecycleCallbacks(mockRecursiveLc, true);

        ChildFragmentManagerFragment fragment = new ChildFragmentManagerFragment();
        fm.beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        verify(mockLc, times(1)).onFragmentCreated(fm, fragment, null);

        fc.dispatchActivityCreated();

        Fragment childFragment = fragment.getChildFragment();

        verify(mockLc, times(1)).onFragmentActivityCreated(fm, fragment, null);
        verify(mockRecursiveLc, times(1)).onFragmentActivityCreated(fm, fragment, null);
        verify(mockRecursiveLc, times(1)).onFragmentActivityCreated(fm, childFragment, null);

        fc.dispatchStart();

        verify(mockLc, times(1)).onFragmentStarted(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentStarted(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentStarted(fm, childFragment);

        fc.dispatchResume();

        verify(mockLc, times(1)).onFragmentResumed(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentResumed(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentResumed(fm, childFragment);

        // Confirm that the parent fragment received onAttachFragment
        assertTrue("parent fragment did not receive onAttachFragment",
                fragment.mCalledOnAttachFragment);

        fc.dispatchStop();

        verify(mockLc, times(1)).onFragmentStopped(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentStopped(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentStopped(fm, childFragment);

        viewModelStore.clear();
        fc.dispatchDestroy();

        verify(mockLc, times(1)).onFragmentDestroyed(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentDestroyed(fm, fragment);
        verify(mockRecursiveLc, times(1)).onFragmentDestroyed(fm, childFragment);
    }

    /**
     * This test checks that FragmentLifecycleCallbacks are invoked when expected.
     */
    @Test
    @UiThreadTest
    public void fragmentLifecycleCallbacks() throws Throwable {
        final ViewModelStore viewModelStore = new ViewModelStore();
        final FragmentController fc = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity(), viewModelStore));
        fc.attachHost(null);
        fc.dispatchCreate();

        FragmentManager fm = fc.getSupportFragmentManager();

        ChildFragmentManagerFragment fragment = new ChildFragmentManagerFragment();
        fm.beginTransaction()
                .add(android.R.id.content, fragment)
                .commitNow();

        fc.dispatchActivityCreated();

        fc.dispatchStart();
        fc.dispatchResume();

        // Confirm that the parent fragment received onAttachFragment
        assertTrue("parent fragment did not receive onAttachFragment",
                fragment.mCalledOnAttachFragment);

        shutdownFragmentController(fc, viewModelStore);
    }

    /**
     * This tests that fragments call onDestroy when the activity finishes.
     */
    @Test
    @UiThreadTest
    public void fragmentDestroyedOnFinish() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        FragmentController fc = startupFragmentController(mActivityRule.getActivity(), null,
                viewModelStore);
        FragmentManager fm = fc.getSupportFragmentManager();

        StrictViewFragment fragmentA = StrictViewFragment.create(R.layout.fragment_a);
        StrictViewFragment fragmentB = StrictViewFragment.create(R.layout.fragment_b);
        fm.beginTransaction()
                .add(android.R.id.content, fragmentA)
                .commit();
        fm.executePendingTransactions();
        fm.beginTransaction()
                .replace(android.R.id.content, fragmentB)
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();
        shutdownFragmentController(fc, viewModelStore);
        assertTrue(fragmentB.mCalledOnDestroy);
        assertTrue(fragmentA.mCalledOnDestroy);
    }

    // Make sure that executing transactions during activity lifecycle events
    // is properly prevented.
    @Test
    public void preventReentrantCalls() throws Throwable {
        testLifecycleTransitionFailure(StrictFragment.ATTACHED, StrictFragment.CREATED);
        testLifecycleTransitionFailure(StrictFragment.CREATED, StrictFragment.ACTIVITY_CREATED);
        testLifecycleTransitionFailure(StrictFragment.ACTIVITY_CREATED, StrictFragment.STARTED);
        testLifecycleTransitionFailure(StrictFragment.STARTED, StrictFragment.RESUMED);

        testLifecycleTransitionFailure(StrictFragment.RESUMED, StrictFragment.STARTED);
        testLifecycleTransitionFailure(StrictFragment.STARTED, StrictFragment.CREATED);
        testLifecycleTransitionFailure(StrictFragment.CREATED, StrictFragment.ATTACHED);
        testLifecycleTransitionFailure(StrictFragment.ATTACHED, StrictFragment.DETACHED);
    }

    private void testLifecycleTransitionFailure(final int fromState,
            final int toState) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ViewModelStore viewModelStore = new ViewModelStore();
                final FragmentController fc1 = startupFragmentController(
                        mActivityRule.getActivity(), null, viewModelStore);

                final FragmentManager fm1 = fc1.getSupportFragmentManager();

                final Fragment reentrantFragment = ReentrantFragment.create(fromState, toState);

                fm1.beginTransaction()
                        .add(reentrantFragment, "reentrant")
                        .commit();
                try {
                    fm1.executePendingTransactions();
                } catch (IllegalStateException e) {
                    fail("An exception shouldn't happen when initially adding the fragment");
                }

                // Now shut down the fragment controller. When fromState > toState, this should
                // result in an exception
                Parcelable savedState;
                try {
                    fc1.dispatchPause();
                    savedState = fc1.saveAllState();
                    fc1.dispatchStop();
                    fc1.dispatchDestroy();
                    if (fromState > toState) {
                        fail("Expected IllegalStateException when moving from "
                                + StrictFragment.stateToString(fromState) + " to "
                                + StrictFragment.stateToString(toState));
                    }
                } catch (IllegalStateException e) {
                    if (fromState < toState) {
                        fail("Unexpected IllegalStateException when moving from "
                                + StrictFragment.stateToString(fromState) + " to "
                                + StrictFragment.stateToString(toState));
                    }
                    return; // test passed!
                }

                // now restore from saved state. This will be reached when
                // fromState < toState. We want to catch the fragment while it
                // is being restored as the fragment controller state is being brought up.

                try {
                    startupFragmentController(mActivityRule.getActivity(), savedState,
                            viewModelStore);

                    fail("Expected IllegalStateException when moving from "
                            + StrictFragment.stateToString(fromState) + " to "
                            + StrictFragment.stateToString(toState));
                } catch (IllegalStateException e) {
                    // expected, so the test passed!
                }
            }
        });
    }

    @Test
    @UiThreadTest
    public void testSetArgumentsLifecycle() throws Throwable {
        ViewModelStore viewModelStore = new ViewModelStore();
        FragmentController fc = startupFragmentController(mActivityRule.getActivity(), null,
                viewModelStore);
        FragmentManager fm = fc.getSupportFragmentManager();

        Fragment f = new StrictFragment();
        f.setArguments(new Bundle());

        fm.beginTransaction()
                .add(f, "1")
                .commitNow();

        f.setArguments(new Bundle());

        fc.dispatchPause();
        fc.saveAllState();

        boolean threw = false;
        try {
            f.setArguments(new Bundle());
        } catch (IllegalStateException ise) {
            threw = true;
        }
        assertTrue("fragment allowed setArguments after state save", threw);

        fc.dispatchStop();

        threw = false;
        try {
            f.setArguments(new Bundle());
        } catch (IllegalStateException ise) {
            threw = true;
        }
        assertTrue("fragment allowed setArguments after stop", threw);

        viewModelStore.clear();
        fc.dispatchDestroy();

        // Fully destroyed, so fragments have been removed.
        f.setArguments(new Bundle());
    }

    /**
     * FragmentActivity should not raise the state of a Fragment while it is being destroyed.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void fragmentActivityFinishEarly() throws Throwable {
        Intent intent = new Intent(mActivityRule.getActivity(), FragmentTestActivity.class);
        intent.putExtra("finishEarly", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        FragmentTestActivity activity = (FragmentTestActivity)
                InstrumentationRegistry.getInstrumentation().startActivitySync(intent);

        assertTrue(activity.onDestroyLatch.await(1000, TimeUnit.MILLISECONDS));
    }

    /**
     * When a fragment is saved in non-config, it should be restored to the same index.
     */
    @Test
    @UiThreadTest
    public void restoreNonConfig() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        Fragment backStackRetainedFragment = new StrictFragment();
        backStackRetainedFragment.setRetainInstance(true);
        Fragment fragment1 = new StrictFragment();
        fm.beginTransaction()
                .add(backStackRetainedFragment, "backStack")
                .add(fragment1, "1")
                .setPrimaryNavigationFragment(fragment1)
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();
        Fragment fragment2 = new StrictFragment();
        fragment2.setRetainInstance(true);
        fragment2.setTargetFragment(fragment1, 0);
        Fragment fragment3 = new StrictFragment();
        fm.beginTransaction()
                .remove(backStackRetainedFragment)
                .remove(fragment1)
                .add(fragment2, "2")
                .add(fragment3, "3")
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        boolean foundFragment2 = false;
        for (Fragment fragment : fc.getSupportFragmentManager().getFragments()) {
            if (fragment == fragment2) {
                foundFragment2 = true;
                assertNotNull(fragment.getTargetFragment());
                assertEquals("1", fragment.getTargetFragment().getTag());
            } else {
                assertNotEquals("2", fragment.getTag());
            }
        }
        assertTrue(foundFragment2);
        fc.getSupportFragmentManager().popBackStackImmediate();
        Fragment foundBackStackRetainedFragment = fc.getSupportFragmentManager()
                .findFragmentByTag("backStack");
        assertEquals("Retained Fragment on the back stack was not retained",
                backStackRetainedFragment, foundBackStackRetainedFragment);
    }

    /**
     * Check that retained fragments in the backstack correctly restored after two "configChanges"
     */
    @Test
    @UiThreadTest
    public void retainedFragmentInBackstack() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        Fragment fragment1 = new StrictFragment();
        fm.beginTransaction()
                .add(fragment1, "1")
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        Fragment child = new StrictFragment();
        child.setRetainInstance(true);
        fragment1.getChildFragmentManager().beginTransaction()
                .add(child, "child").commit();
        fragment1.getChildFragmentManager().executePendingTransactions();

        Fragment fragment2 = new StrictFragment();
        fm.beginTransaction()
                .remove(fragment1)
                .add(fragment2, "2")
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        savedState = FragmentTestUtil.destroy(mActivityRule, fc);
        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        fm = fc.getSupportFragmentManager();
        fm.popBackStackImmediate();
        Fragment retainedChild = fm.findFragmentByTag("1")
                .getChildFragmentManager().findFragmentByTag("child");
        assertEquals(child, retainedChild);
    }

    /**
     * When there are no retained instance fragments, the FragmentManagerNonConfig's fragments
     * should be null
     */
    @Test
    @UiThreadTest
    public void nullNonConfig() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        Fragment fragment1 = new StrictFragment();
        fm.beginTransaction()
                .add(fragment1, "1")
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();
        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);
        assertNull(savedState.second);
    }

    /**
     * When the FragmentManager state changes, the pending transactions should execute.
     */
    @Test
    @UiThreadTest
    public void runTransactionsOnChange() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        RemoveHelloInOnResume fragment1 = new RemoveHelloInOnResume();
        StrictFragment fragment2 = new StrictFragment();
        fm.beginTransaction()
                .add(fragment1, "1")
                .setReorderingAllowed(false)
                .commit();
        fm.beginTransaction()
                .add(fragment2, "Hello")
                .setReorderingAllowed(false)
                .commit();
        fm.executePendingTransactions();

        assertEquals(2, fm.getFragments().size());
        assertTrue(fm.getFragments().contains(fragment1));
        assertTrue(fm.getFragments().contains(fragment2));

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);
        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        fm = fc.getSupportFragmentManager();

        assertEquals(1, fm.getFragments().size());
        for (Fragment fragment : fm.getFragments()) {
            assertTrue(fragment instanceof RemoveHelloInOnResume);
        }
    }

    @Test
    @UiThreadTest
    public void optionsMenu() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        InvalidateOptionFragment fragment = new InvalidateOptionFragment();
        fm.beginTransaction()
                .add(android.R.id.content, fragment)
                .commit();
        fm.executePendingTransactions();

        Menu menu = mock(Menu.class);
        fc.dispatchPrepareOptionsMenu(menu);
        assertTrue(fragment.onPrepareOptionsMenuCalled);
        fragment.onPrepareOptionsMenuCalled = false;
        FragmentTestUtil.destroy(mActivityRule, fc);
        fc.dispatchPrepareOptionsMenu(menu);
        assertFalse(fragment.onPrepareOptionsMenuCalled);
    }

    /**
     * When a retained instance fragment is saved while in the back stack, it should go
     * through onCreate() when it is popped back.
     */
    @Test
    @UiThreadTest
    public void retainInstanceWithOnCreate() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        OnCreateFragment fragment1 = new OnCreateFragment();

        fm.beginTransaction()
                .add(fragment1, "1")
                .commit();
        fm.beginTransaction()
                .remove(fragment1)
                .addToBackStack(null)
                .commit();

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);
        Pair<Parcelable, FragmentManagerNonConfig> restartState =
                Pair.create(savedState.first, null);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, restartState);

        // Save again, but keep the state
        savedState = FragmentTestUtil.destroy(mActivityRule, fc);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);

        fm = fc.getSupportFragmentManager();

        fm.popBackStackImmediate();
        OnCreateFragment fragment2 = (OnCreateFragment) fm.findFragmentByTag("1");
        assertTrue(fragment2.onCreateCalled);
        fm.popBackStackImmediate();
    }

    /**
     * A retained instance fragment should go through onCreate() once, even through save and
     * restore.
     */
    @Test
    @UiThreadTest
    public void retainInstanceOneOnCreate() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        OnCreateFragment fragment = new OnCreateFragment();

        fm.beginTransaction()
                .add(fragment, "fragment")
                .commit();
        fm.executePendingTransactions();

        fm.beginTransaction()
                .remove(fragment)
                .addToBackStack(null)
                .commit();

        assertTrue(fragment.onCreateCalled);
        fragment.onCreateCalled = false;

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        fm = fc.getSupportFragmentManager();

        fm.popBackStackImmediate();
        assertFalse(fragment.onCreateCalled);
    }

    /**
     * A retained instance fragment added via XML should go through onCreate() once, but should get
     * onInflate calls for each inflation.
     */
    @Test
    @UiThreadTest
    public void retainInstanceLayoutOnInflate() throws Throwable {
        FragmentController fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, null);
        FragmentManager fm = fc.getSupportFragmentManager();

        RetainedInflatedParentFragment parentFragment = new RetainedInflatedParentFragment();

        fm.beginTransaction()
                .add(android.R.id.content, parentFragment)
                .commit();
        fm.executePendingTransactions();

        RetainedInflatedChildFragment childFragment = (RetainedInflatedChildFragment)
                parentFragment.getChildFragmentManager().findFragmentById(R.id.child_fragment);

        fm.beginTransaction()
                .remove(parentFragment)
                .addToBackStack(null)
                .commit();

        Pair<Parcelable, FragmentManagerNonConfig> savedState =
                FragmentTestUtil.destroy(mActivityRule, fc);

        fc = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc, savedState);
        fm = fc.getSupportFragmentManager();

        fm.popBackStackImmediate();

        parentFragment = (RetainedInflatedParentFragment) fm.findFragmentById(android.R.id.content);
        RetainedInflatedChildFragment childFragment2 = (RetainedInflatedChildFragment)
                parentFragment.getChildFragmentManager().findFragmentById(R.id.child_fragment);

        assertEquals("Child Fragment should be retained", childFragment, childFragment2);
        assertEquals("Child Fragment should have onInflate called twice",
                2, childFragment2.mOnInflateCount);
    }

    private void executePendingTransactions(final FragmentManager fm) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fm.executePendingTransactions();
            }
        });
    }

    /**
     * This tests a deliberately odd use of a child fragment, added in onCreateView instead
     * of elsewhere. It simulates creating a UI child fragment added to the view hierarchy
     * created by this fragment.
     */
    public static class ChildFragmentManagerFragment extends StrictFragment {
        private FragmentManager mSavedChildFragmentManager;
        private ChildFragmentManagerChildFragment mChildFragment;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mSavedChildFragmentManager = getChildFragmentManager();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            assertSame("child FragmentManagers not the same instance", mSavedChildFragmentManager,
                    getChildFragmentManager());
            ChildFragmentManagerChildFragment child =
                    (ChildFragmentManagerChildFragment) mSavedChildFragmentManager
                            .findFragmentByTag("tag");
            if (child == null) {
                child = new ChildFragmentManagerChildFragment("foo");
                mSavedChildFragmentManager.beginTransaction()
                        .add(child, "tag")
                        .commitNow();
                assertEquals("argument strings don't match", "foo", child.getString());
            }
            mChildFragment = child;
            return new TextView(container.getContext());
        }

        @Nullable
        public Fragment getChildFragment() {
            return mChildFragment;
        }
    }

    public static class ChildFragmentManagerChildFragment extends StrictFragment {
        private String mString;

        public ChildFragmentManagerChildFragment() {
        }

        public ChildFragmentManagerChildFragment(String arg) {
            final Bundle b = new Bundle();
            b.putString("string", arg);
            setArguments(b);
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mString = requireArguments().getString("string", "NO VALUE");
        }

        public String getString() {
            return mString;
        }
    }

    public static class RemoveHelloInOnResume extends Fragment {
        @Override
        public void onResume() {
            super.onResume();
            Fragment fragment = getFragmentManager().findFragmentByTag("Hello");
            if (fragment != null) {
                getFragmentManager().beginTransaction().remove(fragment).commit();
            }
        }
    }

    public static class InvalidateOptionFragment extends Fragment {
        public boolean onPrepareOptionsMenuCalled;

        public InvalidateOptionFragment() {
            setHasOptionsMenu(true);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            onPrepareOptionsMenuCalled = true;
            assertNotNull(getContext());
            super.onPrepareOptionsMenu(menu);
        }
    }

    public static class OnCreateFragment extends Fragment {
        public boolean onCreateCalled;

        public OnCreateFragment() {
            setRetainInstance(true);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            onCreateCalled = true;
        }
    }

    @ContentView(R.layout.nested_retained_inflated_fragment_parent)
    public static class RetainedInflatedParentFragment extends Fragment {
    }

    @ContentView(R.layout.nested_inflated_fragment_child)
    public static class RetainedInflatedChildFragment extends Fragment {

        int mOnInflateCount = 0;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs,
                @Nullable Bundle savedInstanceState) {
            super.onInflate(context, attrs, savedInstanceState);
            mOnInflateCount++;
        }
    }
}
