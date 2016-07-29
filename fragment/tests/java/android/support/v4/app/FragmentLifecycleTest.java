/*
 * Copyright (C) 2016 The Android Open Source Project
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


package android.support.v4.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.fragment.test.R;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.EmptyFragmentTestActivity;
import android.support.v4.view.ViewCompat;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import android.widget.TextView;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileDescriptor;
import java.io.PrintWriter;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

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

        // Test removal as well; StrictFragment will throw here too.
        fm.beginTransaction().remove(strictFragment).commit();
        executePendingTransactions(fm);

        assertFalse("fragment is added", strictFragment.isAdded());
        assertFalse("fragment is resumed", strictFragment.isResumed());

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

    @Test
    @UiThreadTest
    public void restoreRetainedInstanceFragments() throws Throwable {
        // Create a new FragmentManager in isolation, nest some assorted fragments
        // and then restore them to a second new FragmentManager.

        final FragmentController fc1 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity()));

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        fc1.attachHost(null);
        fc1.dispatchCreate();

        // Configure fragments.

        // Grandparent fragment will not retain instance
        final StateSaveFragment grandparentFragment = new StateSaveFragment("Grandparent",
                "UnsavedGrandparent");
        assertNotNull("grandparent fragment saved state not initialized",
                grandparentFragment.getSavedState());
        assertNotNull("grandparent fragment unsaved state not initialized",
                grandparentFragment.getUnsavedState());
        fm1.beginTransaction().add(grandparentFragment, "tag:grandparent").commitNow();

        // Parent fragment will retain instance
        final StateSaveFragment parentFragment = new StateSaveFragment("Parent", "UnsavedParent");
        assertNotNull("parent fragment saved state not initialized",
                parentFragment.getSavedState());
        assertNotNull("parent fragment unsaved state not initialized",
                parentFragment.getUnsavedState());
        parentFragment.setRetainInstance(true);
        grandparentFragment.getChildFragmentManager().beginTransaction()
                .add(parentFragment, "tag:parent").commitNow();
        assertSame("parent fragment is not a child of grandparent",
                grandparentFragment, parentFragment.getParentFragment());

        // Child fragment will not retain instance
        final StateSaveFragment childFragment = new StateSaveFragment("Child", "UnsavedChild");
        assertNotNull("child fragment saved state not initialized",
                childFragment.getSavedState());
        assertNotNull("child fragment unsaved state not initialized",
                childFragment.getUnsavedState());
        parentFragment.getChildFragmentManager().beginTransaction()
                .add(childFragment, "tag:child").commitNow();
        assertSame("child fragment is not a child of grandpanret",
                parentFragment, childFragment.getParentFragment());

        // Saved for comparison later
        final FragmentManager parentChildFragmentManager = parentFragment.getChildFragmentManager();

        fc1.dispatchActivityCreated();
        fc1.noteStateNotSaved();
        fc1.execPendingActions();
        fc1.doLoaderStart();
        fc1.dispatchStart();
        fc1.reportLoaderStart();
        fc1.dispatchResume();
        fc1.execPendingActions();

        // Bring the state back down to destroyed, simulating an activity restart
        fc1.dispatchPause();
        final Parcelable savedState = fc1.saveAllState();
        final FragmentManagerNonConfig nonconf = fc1.retainNestedNonConfig();
        fc1.dispatchStop();
        fc1.dispatchReallyStop();
        fc1.dispatchDestroy();

        // Create the new controller and restore state
        final FragmentController fc2 = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity()));

        final FragmentManager fm2 = fc2.getSupportFragmentManager();

        fc2.attachHost(null);
        fc2.restoreAllState(savedState, nonconf);
        fc2.dispatchCreate();

        // Confirm that the restored fragments are available and in the expected states
        final StateSaveFragment restoredGrandparent = (StateSaveFragment) fm2.findFragmentByTag(
                "tag:grandparent");
        assertNotNull("grandparent fragment not restored", restoredGrandparent);

        assertNotSame("grandparent fragment instance was saved",
                grandparentFragment, restoredGrandparent);
        assertEquals("grandparent fragment saved state was not equal",
                grandparentFragment.getSavedState(), restoredGrandparent.getSavedState());
        assertNotEquals("grandparent fragment unsaved state was unexpectedly preserved",
                grandparentFragment.getUnsavedState(), restoredGrandparent.getUnsavedState());

        final StateSaveFragment restoredParent = (StateSaveFragment) restoredGrandparent
                .getChildFragmentManager().findFragmentByTag("tag:parent");
        assertNotNull("parent fragment not restored", restoredParent);

        assertSame("parent fragment instance was not saved", parentFragment, restoredParent);
        assertEquals("parent fragment saved state was not equal",
                parentFragment.getSavedState(), restoredParent.getSavedState());
        assertEquals("parent fragment unsaved state was not equal",
                parentFragment.getUnsavedState(), restoredParent.getUnsavedState());
        assertNotSame("parent fragment has the same child FragmentManager",
                parentChildFragmentManager, restoredParent.getChildFragmentManager());

        final StateSaveFragment restoredChild = (StateSaveFragment) restoredParent
                .getChildFragmentManager().findFragmentByTag("tag:child");
        assertNotNull("child fragment not restored", restoredChild);

        assertNotSame("child fragment instance state was saved", childFragment, restoredChild);
        assertEquals("child fragment saved state was not equal",
                childFragment.getSavedState(), restoredChild.getSavedState());
        assertNotEquals("child fragment saved state was unexpectedly equal",
                childFragment.getUnsavedState(), restoredChild.getUnsavedState());

        fc2.dispatchActivityCreated();
        fc2.noteStateNotSaved();
        fc2.execPendingActions();
        fc2.doLoaderStart();
        fc2.dispatchStart();
        fc2.reportLoaderStart();
        fc2.dispatchResume();
        fc2.execPendingActions();

        // Test that the fragments are in the configuration we expect

        // Bring the state back down to destroyed before we finish the test
        fc2.dispatchPause();
        fc2.saveAllState();
        fc2.dispatchStop();
        fc2.dispatchReallyStop();
        fc2.dispatchDestroy();

        assertTrue("grandparent not destroyed", restoredGrandparent.mCalledOnDestroy);
        assertTrue("parent not destroyed", restoredParent.mCalledOnDestroy);
        assertTrue("child not destroyed", restoredChild.mCalledOnDestroy);
    }

    @Test
    @UiThreadTest
    public void saveAnimationState() throws Throwable {
        FragmentController fc = startupFragmentController(null);
        FragmentManager fm = fc.getSupportFragmentManager();

        fm.beginTransaction()
                .setCustomAnimations(0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .add(android.R.id.content, SimpleFragment.create(R.layout.fragment_a))
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

        // Causes save and restore of fragments and back stack
        fc = restartFragmentController(fc);
        fm = fc.getSupportFragmentManager();

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

        fm.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, 0, 0)
                .replace(android.R.id.content, SimpleFragment.create(R.layout.fragment_b))
                .addToBackStack(null)
                .commit();
        fm.executePendingTransactions();

        assertAnimationsMatch(fm, R.anim.fade_in, R.anim.fade_out, 0, 0);

        // Causes save and restore of fragments and back stack
        fc = restartFragmentController(fc);
        fm = fc.getSupportFragmentManager();

        assertAnimationsMatch(fm, R.anim.fade_in, R.anim.fade_out, 0, 0);

        fm.popBackStackImmediate();

        assertAnimationsMatch(fm, 0, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);

        shutdownFragmentController(fc);
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
        FragmentController fc = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity()));
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

        fc.dispatchStop();
        fc.dispatchReallyStop();
        fc.dispatchDestroy();
    }

    private void assertAnimationsMatch(FragmentManager fm, int enter, int exit, int popEnter,
            int popExit) {
        FragmentManagerImpl fmImpl = (FragmentManagerImpl) fm;
        BackStackRecord record = fmImpl.mBackStack.get(fmImpl.mBackStack.size() - 1);

        Assert.assertEquals(enter, record.mEnterAnim);
        Assert.assertEquals(exit, record.mExitAnim);
        Assert.assertEquals(popEnter, record.mPopEnterAnim);
        Assert.assertEquals(popExit, record.mPopExitAnim);
    }

    private FragmentController restartFragmentController(FragmentController fc) {
        Parcelable savedState = shutdownFragmentController(fc);
        return startupFragmentController(savedState);
    }

    private FragmentController startupFragmentController(Parcelable savedState) {
        final FragmentController fc = FragmentController.createController(
                new HostCallbacks(mActivityRule.getActivity()));
        fc.attachHost(null);
        fc.restoreAllState(savedState, (FragmentManagerNonConfig) null);
        fc.dispatchCreate();
        fc.dispatchActivityCreated();
        fc.noteStateNotSaved();
        fc.execPendingActions();
        fc.doLoaderStart();
        fc.dispatchStart();
        fc.reportLoaderStart();
        fc.dispatchResume();
        fc.execPendingActions();
        return fc;
    }

    private Parcelable shutdownFragmentController(FragmentController fc) {
        fc.dispatchPause();
        final Parcelable savedState = fc.saveAllState();
        fc.dispatchStop();
        fc.dispatchReallyStop();
        fc.dispatchDestroy();
        return savedState;
    }

    private void executePendingTransactions(final FragmentManager fm) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fm.executePendingTransactions();
            }
        });
    }

    public static class StateSaveFragment extends StrictFragment {
        private static final String STATE_KEY = "state";

        private String mSavedState;
        private String mUnsavedState;

        public StateSaveFragment() {
        }

        public StateSaveFragment(String savedState, String unsavedState) {
            mSavedState = savedState;
            mUnsavedState = unsavedState;
        }

        public String getSavedState() {
            return mSavedState;
        }

        public String getUnsavedState() {
            return mUnsavedState;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mSavedState = savedInstanceState.getString(STATE_KEY);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(STATE_KEY, mSavedState);
        }
    }

    public static class ChildFragmentManagerFragment extends StrictFragment {
        private FragmentManager mSavedChildFragmentManager;

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
            ChildFragmentManagerChildFragment child = new ChildFragmentManagerChildFragment("foo");
            mSavedChildFragmentManager.beginTransaction()
                    .add(child, "tag")
                    .commitNow();
            assertEquals("argument strings don't match", "foo", child.getString());
            return new TextView(container.getContext());
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
            mString = getArguments().getString("string", "NO VALUE");
        }

        public String getString() {
            return mString;
        }
    }

    static class HostCallbacks extends FragmentHostCallback<FragmentActivity> {
        private final FragmentActivity mActivity;

        public HostCallbacks(FragmentActivity activity) {
            super(activity);
            mActivity = activity;
        }

        @Override
        public void onDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        }

        @Override
        public boolean onShouldSaveFragmentState(Fragment fragment) {
            return !mActivity.isFinishing();
        }

        @Override
        public LayoutInflater onGetLayoutInflater() {
            return mActivity.getLayoutInflater().cloneInContext(mActivity);
        }

        @Override
        public FragmentActivity onGetHost() {
            return mActivity;
        }

        @Override
        public void onSupportInvalidateOptionsMenu() {
            mActivity.supportInvalidateOptionsMenu();
        }

        @Override
        public void onStartActivityFromFragment(Fragment fragment, Intent intent, int requestCode) {
            mActivity.startActivityFromFragment(fragment, intent, requestCode);
        }

        @Override
        public void onStartActivityFromFragment(
                Fragment fragment, Intent intent, int requestCode, @Nullable Bundle options) {
            mActivity.startActivityFromFragment(fragment, intent, requestCode, options);
        }

        @Override
        public void onRequestPermissionsFromFragment(@NonNull Fragment fragment,
                @NonNull String[] permissions, int requestCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean onShouldShowRequestPermissionRationale(@NonNull String permission) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity, permission);
        }

        @Override
        public boolean onHasWindowAnimations() {
            return mActivity.getWindow() != null;
        }

        @Override
        public int onGetWindowAnimations() {
            final Window w = mActivity.getWindow();
            return (w == null) ? 0 : w.getAttributes().windowAnimations;
        }

        @Override
        public void onAttachFragment(Fragment fragment) {
            mActivity.onAttachFragment(fragment);
        }

        @Nullable
        @Override
        public View onFindViewById(int id) {
            return mActivity.findViewById(id);
        }

        @Override
        public boolean onHasView() {
            final Window w = mActivity.getWindow();
            return (w != null && w.peekDecorView() != null);
        }
    }

    public static class SimpleFragment extends Fragment {
        private int mLayoutId;
        private static final String LAYOUT_ID = "layoutId";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mLayoutId = savedInstanceState.getInt(LAYOUT_ID, mLayoutId);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(LAYOUT_ID, mLayoutId);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(mLayoutId, container, false);
        }

        public static SimpleFragment create(int layoutId) {
            SimpleFragment fragment = new SimpleFragment();
            fragment.mLayoutId = layoutId;
            return fragment;
        }
    }
}
