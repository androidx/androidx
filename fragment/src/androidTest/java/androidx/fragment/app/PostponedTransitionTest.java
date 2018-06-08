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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class PostponedTransitionTest {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    private Instrumentation mInstrumentation;
    private PostponedFragment1 mBeginningFragment;

    @Before
    public void setupContainer() throws Throwable {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        mBeginningFragment = new PostponedFragment1();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, mBeginningFragment)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        mBeginningFragment.startPostponedEnterTransition();
        mBeginningFragment.waitForTransition();
        clearTargets(mBeginningFragment);
    }

    // Ensure that replacing with a fragment that has a postponed transition
    // will properly postpone it, both adding and popping.
    @Test
    public void replaceTransition() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final PostponedFragment2 fragment = new PostponedFragment2();
        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        // should be postponed now
        assertPostponedTransition(mBeginningFragment, fragment, null);

        // start the postponed transition
        fragment.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(mBeginningFragment, fragment);

        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        // should be postponed going back, too
        assertPostponedTransition(fragment, mBeginningFragment, null);

        // start the postponed transition
        mBeginningFragment.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment, mBeginningFragment);
    }

    // Ensure that replacing a fragment doesn't cause problems with the back stack nesting level
    @Test
    public void backStackNestingLevel() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        View startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final TransitionFragment fragment1 = new TransitionFragment2();
        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        // make sure transition ran
        assertForwardTransition(mBeginningFragment, fragment1);

        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        // should be postponed going back
        assertPostponedTransition(fragment1, mBeginningFragment, null);

        // start the postponed transition
        mBeginningFragment.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment1, mBeginningFragment);

        startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final TransitionFragment fragment2 = new TransitionFragment2();
        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        // make sure transition ran
        assertForwardTransition(mBeginningFragment, fragment2);

        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        // should be postponed going back
        assertPostponedTransition(fragment2, mBeginningFragment, null);

        // start the postponed transition
        mBeginningFragment.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment2, mBeginningFragment);
    }

    // Ensure that postponed transition is forced after another has been committed.
    // This tests when the transactions are executed together
    @Test
    public void forcedTransition1() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final PostponedFragment2 fragment2 = new PostponedFragment2();
        final PostponedFragment1 fragment3 = new PostponedFragment1();

        final int[] commit = new int[1];
        // Need to run this on the UI thread so that the transaction doesn't start
        // between the two
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                commit[0] = fm.beginTransaction()
                        .addSharedElement(startBlue, "blueSquare")
                        .replace(R.id.fragmentContainer, fragment2)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();

                fm.beginTransaction()
                        .addSharedElement(startBlue, "blueSquare")
                        .replace(R.id.fragmentContainer, fragment3)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
            }
        });
        FragmentTestUtil.waitForExecution(mActivityRule);

        // transition to fragment2 should be started
        assertForwardTransition(mBeginningFragment, fragment2);

        // fragment3 should be postponed, but fragment2 should be executed with no transition.
        assertPostponedTransition(fragment2, fragment3, mBeginningFragment);

        // start the postponed transition
        fragment3.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(fragment2, fragment3);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, commit[0],
                FragmentManager.POP_BACK_STACK_INCLUSIVE);

        assertBackTransition(fragment3, fragment2);

        assertPostponedTransition(fragment2, mBeginningFragment, fragment3);

        // start the postponed transition
        mBeginningFragment.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment2, mBeginningFragment);
    }

    // Ensure that postponed transition is forced after another has been committed.
    // This tests when the transactions are processed separately.
    @Test
    public void forcedTransition2() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final PostponedFragment2 fragment2 = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(mBeginningFragment, fragment2, null);

        final PostponedFragment1 fragment3 = new PostponedFragment1();
        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        // This should cancel the mBeginningFragment -> fragment2 transition
        // and start fragment2 -> fragment3 transition postponed
        FragmentTestUtil.waitForExecution(mActivityRule);

        // fragment3 should be postponed, but fragment2 should be executed with no transition.
        assertPostponedTransition(fragment2, fragment3, mBeginningFragment);

        // start the postponed transition
        fragment3.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(fragment2, fragment3);

        // Pop back to fragment2, but it should be postponed
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment3, fragment2, null);

        // Pop to mBeginningFragment -- should cancel the fragment2 transition and
        // start the mBeginningFragment transaction postponed

        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment2, mBeginningFragment, fragment3);

        // start the postponed transition
        mBeginningFragment.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment2, mBeginningFragment);
    }

    // Do a bunch of things to one fragment in a transaction and see if it can screw things up.
    @Test
    public void crazyTransition() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final PostponedFragment2 fragment2 = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .hide(mBeginningFragment)
                .replace(R.id.fragmentContainer, fragment2)
                .hide(fragment2)
                .detach(fragment2)
                .attach(fragment2)
                .show(fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(mBeginningFragment, fragment2, null);

        // start the postponed transition
        fragment2.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(mBeginningFragment, fragment2);

        // Pop back to fragment2, but it should be postponed
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment2, mBeginningFragment, null);

        // start the postponed transition
        mBeginningFragment.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment2, mBeginningFragment);
    }

    // Execute transactions on different containers and ensure that they don't conflict
    @Test
    public void differentContainers() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .remove(mBeginningFragment)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        FragmentTestUtil.setContentView(mActivityRule, R.layout.double_container);

        TransitionFragment fragment1 = new PostponedFragment1();
        TransitionFragment fragment2 = new PostponedFragment1();

        fm.beginTransaction()
                .add(R.id.fragmentContainer1, fragment1)
                .add(R.id.fragmentContainer2, fragment2)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        fragment1.startPostponedEnterTransition();
        fragment2.startPostponedEnterTransition();
        fragment1.waitForTransition();
        fragment2.waitForTransition();
        clearTargets(fragment1);
        clearTargets(fragment2);

        final View startBlue1 = fragment1.getView().findViewById(R.id.blueSquare);
        final View startBlue2 = fragment2.getView().findViewById(R.id.blueSquare);

        final TransitionFragment fragment3 = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue1, "blueSquare")
                .replace(R.id.fragmentContainer1, fragment3)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment3, null);

        final TransitionFragment fragment4 = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue2, "blueSquare")
                .replace(R.id.fragmentContainer2, fragment4)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment3, null);
        assertPostponedTransition(fragment2, fragment4, null);

        // start the postponed transition
        fragment3.startPostponedEnterTransition();

        // make sure only one ran
        assertForwardTransition(fragment1, fragment3);
        assertPostponedTransition(fragment2, fragment4, null);

        // start the postponed transition
        fragment4.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(fragment2, fragment4);

        // Pop back to fragment2 -- should be postponed
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment4, fragment2, null);

        // Pop back to fragment1 -- also should be postponed
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment4, fragment2, null);
        assertPostponedTransition(fragment3, fragment1, null);

        // start the postponed transition
        fragment2.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment4, fragment2);

        // but not the postponed one
        assertPostponedTransition(fragment3, fragment1, null);

        // start the postponed transition
        fragment1.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment3, fragment1);
    }

    // Execute transactions on different containers and ensure that they don't conflict.
    // The postponement can be started out-of-order
    @Test
    public void outOfOrderContainers() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .remove(mBeginningFragment)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        FragmentTestUtil.setContentView(mActivityRule, R.layout.double_container);

        TransitionFragment fragment1 = new PostponedFragment1();
        TransitionFragment fragment2 = new PostponedFragment1();

        fm.beginTransaction()
                .add(R.id.fragmentContainer1, fragment1)
                .add(R.id.fragmentContainer2, fragment2)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        fragment1.startPostponedEnterTransition();
        fragment2.startPostponedEnterTransition();
        fragment1.waitForTransition();
        fragment2.waitForTransition();
        clearTargets(fragment1);
        clearTargets(fragment2);

        final View startBlue1 = fragment1.getView().findViewById(R.id.blueSquare);
        final View startBlue2 = fragment2.getView().findViewById(R.id.blueSquare);

        final TransitionFragment fragment3 = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue1, "blueSquare")
                .replace(R.id.fragmentContainer1, fragment3)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment3, null);

        final TransitionFragment fragment4 = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue2, "blueSquare")
                .replace(R.id.fragmentContainer2, fragment4)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment3, null);
        assertPostponedTransition(fragment2, fragment4, null);

        // start the postponed transition
        fragment4.startPostponedEnterTransition();

        // make sure only one ran
        assertForwardTransition(fragment2, fragment4);
        assertPostponedTransition(fragment1, fragment3, null);

        // start the postponed transition
        fragment3.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(fragment1, fragment3);

        // Pop back to fragment2 -- should be postponed
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment4, fragment2, null);

        // Pop back to fragment1 -- also should be postponed
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        assertPostponedTransition(fragment4, fragment2, null);
        assertPostponedTransition(fragment3, fragment1, null);

        // start the postponed transition
        fragment1.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment3, fragment1);

        // but not the postponed one
        assertPostponedTransition(fragment4, fragment2, null);

        // start the postponed transition
        fragment2.startPostponedEnterTransition();

        // make sure it ran
        assertBackTransition(fragment4, fragment2);
    }

    // Make sure that commitNow for a transaction on a different fragment container doesn't
    // affect the postponed transaction
    @Test
    public void commitNowNoEffect() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .remove(mBeginningFragment)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        FragmentTestUtil.setContentView(mActivityRule, R.layout.double_container);

        final TransitionFragment fragment1 = new PostponedFragment1();
        final TransitionFragment fragment2 = new PostponedFragment1();

        fm.beginTransaction()
                .add(R.id.fragmentContainer1, fragment1)
                .add(R.id.fragmentContainer2, fragment2)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        fragment1.startPostponedEnterTransition();
        fragment2.startPostponedEnterTransition();
        fragment1.waitForTransition();
        fragment2.waitForTransition();
        clearTargets(fragment1);
        clearTargets(fragment2);

        final View startBlue1 = fragment1.getView().findViewById(R.id.blueSquare);
        final View startBlue2 = fragment2.getView().findViewById(R.id.blueSquare);

        final TransitionFragment fragment3 = new PostponedFragment2();
        final StrictFragment strictFragment1 = new StrictFragment();

        fm.beginTransaction()
                .addSharedElement(startBlue1, "blueSquare")
                .replace(R.id.fragmentContainer1, fragment3)
                .add(strictFragment1, "1")
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment3, null);

        final TransitionFragment fragment4 = new PostponedFragment2();
        final StrictFragment strictFragment2 = new StrictFragment();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fm.beginTransaction()
                        .addSharedElement(startBlue2, "blueSquare")
                        .replace(R.id.fragmentContainer2, fragment4)
                        .remove(strictFragment1)
                        .add(strictFragment2, "2")
                        .setReorderingAllowed(true)
                        .commitNow();
            }
        });

        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment3, null);
        assertPostponedTransition(fragment2, fragment4, null);

        // start the postponed transition
        fragment4.startPostponedEnterTransition();

        // make sure only one ran
        assertForwardTransition(fragment2, fragment4);
        assertPostponedTransition(fragment1, fragment3, null);

        // start the postponed transition
        fragment3.startPostponedEnterTransition();

        // make sure it ran
        assertForwardTransition(fragment1, fragment3);
    }

    // Make sure that commitNow for a transaction affecting a postponed fragment in the same
    // container forces the postponed transition to start.
    @Test
    public void commitNowStartsPostponed() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue1 = mBeginningFragment.getView().findViewById(R.id.blueSquare);

        final TransitionFragment fragment2 = new PostponedFragment2();
        final TransitionFragment fragment1 = new PostponedFragment1();

        fm.beginTransaction()
                .addSharedElement(startBlue1, "blueSquare")
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        final View startBlue2 = fragment2.getView().findViewById(R.id.blueSquare);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fm.beginTransaction()
                        .addSharedElement(startBlue2, "blueSquare")
                        .replace(R.id.fragmentContainer, fragment1)
                        .setReorderingAllowed(true)
                        .commitNow();
            }
        });

        assertPostponedTransition(fragment2, fragment1, mBeginningFragment);

        // start the postponed transition
        fragment1.startPostponedEnterTransition();

        assertForwardTransition(fragment2, fragment1);
    }

    // Make sure that when a transaction that removes a view is postponed that
    // another transaction doesn't accidentally remove the view early.
    @Test
    public void noAccidentalRemoval() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.beginTransaction()
                .remove(mBeginningFragment)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        FragmentTestUtil.setContentView(mActivityRule, R.layout.double_container);

        TransitionFragment fragment1 = new PostponedFragment1();

        fm.beginTransaction()
                .add(R.id.fragmentContainer1, fragment1)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        fragment1.startPostponedEnterTransition();
        fragment1.waitForTransition();
        clearTargets(fragment1);

        TransitionFragment fragment2 = new PostponedFragment2();
        // Create a postponed transaction that removes a view
        fm.beginTransaction()
                .replace(R.id.fragmentContainer1, fragment2)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);
        assertPostponedTransition(fragment1, fragment2, null);

        TransitionFragment fragment3 = new PostponedFragment1();
        // Create a transaction that doesn't interfere with the previously postponed one
        fm.beginTransaction()
                .replace(R.id.fragmentContainer2, fragment3)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(fragment1, fragment2, null);

        fragment3.startPostponedEnterTransition();
        fragment3.waitForTransition();
        clearTargets(fragment3);

        assertPostponedTransition(fragment1, fragment2, null);
    }

    // Ensure that a postponed transaction that is popped runs immediately and that
    // the transaction results in the original state with no transition.
    @Test
    public void popPostponedTransaction() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue = mBeginningFragment.getView().findViewById(R.id.blueSquare);

        final TransitionFragment fragment = new PostponedFragment2();

        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        assertPostponedTransition(mBeginningFragment, fragment, null);

        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        fragment.waitForNoTransition();
        mBeginningFragment.waitForNoTransition();

        assureNoTransition(fragment);
        assureNoTransition(mBeginningFragment);

        assertFalse(fragment.isAdded());
        assertNull(fragment.getView());
        assertNotNull(mBeginningFragment.getView());
        assertEquals(View.VISIBLE, mBeginningFragment.getView().getVisibility());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            assertEquals(1f, mBeginningFragment.getView().getAlpha(), 0f);
        }
        assertTrue(mBeginningFragment.getView().isAttachedToWindow());
    }

    // Make sure that when saving the state during a postponed transaction that it saves
    // the state as if it wasn't postponed.
    @Test
    public void saveWhilePostponed() throws Throwable {
        final FragmentController fc1 = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc1, null);

        final FragmentManager fm1 = fc1.getSupportFragmentManager();

        PostponedFragment1 fragment1 = new PostponedFragment1();
        fm1.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.waitForExecution(mActivityRule);

        Pair<Parcelable, FragmentManagerNonConfig> state =
                FragmentTestUtil.destroy(mActivityRule, fc1);

        final FragmentController fc2 = FragmentTestUtil.createController(mActivityRule);
        FragmentTestUtil.resume(mActivityRule, fc2, state);

        final FragmentManager fm2 = fc2.getSupportFragmentManager();
        Fragment fragment2 = fm2.findFragmentByTag("1");
        assertNotNull(fragment2);
        assertNotNull(fragment2.getView());
        assertEquals(View.VISIBLE, fragment2.getView().getVisibility());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            assertEquals(1f, fragment2.getView().getAlpha(), 0f);
        }
        assertTrue(fragment2.isResumed());
        assertTrue(fragment2.isAdded());
        assertTrue(fragment2.getView().isAttachedToWindow());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                assertTrue(fm2.popBackStackImmediate());

            }
        });

        assertFalse(fragment2.isResumed());
        assertFalse(fragment2.isAdded());
        assertNull(fragment2.getView());
    }

    // Ensure that the postponed fragment transactions don't allow reentrancy in fragment manager
    @Test
    public void postponeDoesNotAllowReentrancy() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final View startBlue = mActivityRule.getActivity().findViewById(R.id.blueSquare);

        final CommitNowFragment fragment = new CommitNowFragment();
        fm.beginTransaction()
                .addSharedElement(startBlue, "blueSquare")
                .replace(R.id.fragmentContainer, fragment)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();

        FragmentTestUtil.waitForExecution(mActivityRule);

        // should be postponed now
        assertPostponedTransition(mBeginningFragment, fragment, null);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // start the postponed transition
                fragment.startPostponedEnterTransition();

                try {
                    // This should trigger an IllegalStateException
                    fm.executePendingTransactions();
                    fail("commitNow() while executing a transaction should cause an "
                            + "IllegalStateException");
                } catch (IllegalStateException e) {
                    // expected
                }
            }
        });
    }

    private void assertPostponedTransition(TransitionFragment fromFragment,
            TransitionFragment toFragment, TransitionFragment removedFragment)
            throws InterruptedException {
        if (removedFragment != null) {
            assertNull(removedFragment.getView());
            assureNoTransition(removedFragment);
        }

        toFragment.waitForNoTransition();
        assertNotNull(fromFragment.getView());
        assertNotNull(toFragment.getView());
        assertTrue(fromFragment.getView().isAttachedToWindow());
        assertTrue(toFragment.getView().isAttachedToWindow());
        assertEquals(View.VISIBLE, fromFragment.getView().getVisibility());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            assertEquals(View.VISIBLE, toFragment.getView().getVisibility());
            assertEquals(0f, toFragment.getView().getAlpha(), 0f);
        } else {
            assertEquals(View.INVISIBLE, toFragment.getView().getVisibility());
        }
        assureNoTransition(fromFragment);
        assureNoTransition(toFragment);
        assertTrue(fromFragment.isResumed());
        assertFalse(toFragment.isResumed());
    }

    private void clearTargets(TransitionFragment fragment) {
        fragment.enterTransition.targets.clear();
        fragment.reenterTransition.targets.clear();
        fragment.exitTransition.targets.clear();
        fragment.returnTransition.targets.clear();
        fragment.sharedElementEnter.targets.clear();
        fragment.sharedElementReturn.targets.clear();
    }

    private void assureNoTransition(TransitionFragment fragment) {
        assertEquals(0, fragment.enterTransition.targets.size());
        assertEquals(0, fragment.reenterTransition.targets.size());
        assertEquals(0, fragment.enterTransition.targets.size());
        assertEquals(0, fragment.returnTransition.targets.size());
        assertEquals(0, fragment.sharedElementEnter.targets.size());
        assertEquals(0, fragment.sharedElementReturn.targets.size());
    }

    private void assertForwardTransition(TransitionFragment start, TransitionFragment end)
            throws InterruptedException {
        start.waitForTransition();
        end.waitForTransition();
        assertEquals(0, start.enterTransition.targets.size());
        assertEquals(1, end.enterTransition.targets.size());

        assertEquals(0, start.reenterTransition.targets.size());
        assertEquals(0, end.reenterTransition.targets.size());

        assertEquals(0, start.returnTransition.targets.size());
        assertEquals(0, end.returnTransition.targets.size());

        assertEquals(1, start.exitTransition.targets.size());
        assertEquals(0, end.exitTransition.targets.size());

        assertEquals(0, start.sharedElementEnter.targets.size());
        assertEquals(2, end.sharedElementEnter.targets.size());

        assertEquals(0, start.sharedElementReturn.targets.size());
        assertEquals(0, end.sharedElementReturn.targets.size());

        final View blue = end.getView().findViewById(R.id.blueSquare);
        assertTrue(end.sharedElementEnter.targets.contains(blue));
        assertEquals("blueSquare", end.sharedElementEnter.targets.get(0).getTransitionName());
        assertEquals("blueSquare", end.sharedElementEnter.targets.get(1).getTransitionName());

        assertNoTargets(start);
        assertNoTargets(end);

        clearTargets(start);
        clearTargets(end);
    }

    private void assertBackTransition(TransitionFragment start, TransitionFragment end)
            throws InterruptedException {
        start.waitForTransition();
        end.waitForTransition();
        assertEquals(1, end.reenterTransition.targets.size());
        assertEquals(0, start.reenterTransition.targets.size());

        assertEquals(0, end.returnTransition.targets.size());
        assertEquals(1, start.returnTransition.targets.size());

        assertEquals(0, start.enterTransition.targets.size());
        assertEquals(0, end.enterTransition.targets.size());

        assertEquals(0, start.exitTransition.targets.size());
        assertEquals(0, end.exitTransition.targets.size());

        assertEquals(0, start.sharedElementEnter.targets.size());
        assertEquals(0, end.sharedElementEnter.targets.size());

        assertEquals(2, start.sharedElementReturn.targets.size());
        assertEquals(0, end.sharedElementReturn.targets.size());

        final View blue = end.getView().findViewById(R.id.blueSquare);
        assertTrue(start.sharedElementReturn.targets.contains(blue));
        assertEquals("blueSquare", start.sharedElementReturn.targets.get(0).getTransitionName());
        assertEquals("blueSquare", start.sharedElementReturn.targets.get(1).getTransitionName());

        assertNoTargets(end);
        assertNoTargets(start);

        clearTargets(start);
        clearTargets(end);
    }

    private static void assertNoTargets(TransitionFragment fragment) {
        assertTrue(fragment.enterTransition.getTargets().isEmpty());
        assertTrue(fragment.reenterTransition.getTargets().isEmpty());
        assertTrue(fragment.exitTransition.getTargets().isEmpty());
        assertTrue(fragment.returnTransition.getTargets().isEmpty());
        assertTrue(fragment.sharedElementEnter.getTargets().isEmpty());
        assertTrue(fragment.sharedElementReturn.getTargets().isEmpty());
    }

    public static class PostponedFragment1 extends TransitionFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            postponeEnterTransition();
            return inflater.inflate(R.layout.scene1, container, false);
        }
    }

    public static class PostponedFragment2 extends TransitionFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            postponeEnterTransition();
            return inflater.inflate(R.layout.scene2, container, false);
        }
    }

    public static class CommitNowFragment extends PostponedFragment1 {
        @Override
        public void onResume() {
            super.onResume();
            // This should throw because this happens during the execution
            getFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, new PostponedFragment1())
                    .commitNow();
        }
    }

    public static class TransitionFragment2 extends TransitionFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            return inflater.inflate(R.layout.scene2, container, false);
        }
    }
}
