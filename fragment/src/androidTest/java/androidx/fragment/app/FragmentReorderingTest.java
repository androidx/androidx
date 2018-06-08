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
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentReorderingTest {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    private ViewGroup mContainer;
    private FragmentManager mFM;
    private Instrumentation mInstrumentation;

    @Before
    public void setup() {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        mContainer = (ViewGroup) mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        mFM = mActivityRule.getActivity().getSupportFragmentManager();
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    // Test that when you add and replace a fragment that only the replace's add
    // actually creates a View.
    @Test
    public void addReplace() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        final StrictViewFragment fragment2 = new StrictViewFragment();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment2)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });
        assertEquals(0, fragment1.onCreateViewCount);
        FragmentTestUtil.assertChildren(mContainer, fragment2);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.popBackStack();
                mFM.popBackStack();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer);
    }

    // Test that it is possible to merge a transaction that starts with pop and adds
    // the same view back again.
    @Test
    public void startWithPop() throws Throwable {
        // Start with a single fragment on the back stack
        final CountCallsFragment fragment1 = new CountCallsFragment();
        mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        // Now pop and add
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.popBackStack();
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer, fragment1);
        assertEquals(1, fragment1.onCreateViewCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer);
        assertEquals(1, fragment1.onCreateViewCount);
    }

    // Popping the back stack in the middle of other operations doesn't fool it.
    @Test
    public void middlePop() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        final CountCallsFragment fragment2 = new CountCallsFragment();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.popBackStack();
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment2)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer, fragment2);
        assertEquals(0, fragment1.onAttachCount);
        assertEquals(1, fragment2.onCreateViewCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer);
        assertEquals(1, fragment2.onDetachCount);
    }

    // ensure that removing a view after adding it is optimized into no
    // View being created. Hide still gets notified.
    @Test
    public void removeRedundantRemove() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        final int[] id = new int[1];
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                id[0] = mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .remove(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer);
        assertEquals(0, fragment1.onCreateViewCount);
        assertEquals(1, fragment1.onHideCount);
        assertEquals(0, fragment1.onShowCount);
        assertEquals(0, fragment1.onDetachCount);
        assertEquals(1, fragment1.onAttachCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, id[0],
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        FragmentTestUtil.assertChildren(mContainer);
        assertEquals(0, fragment1.onCreateViewCount);
        assertEquals(1, fragment1.onHideCount);
        assertEquals(1, fragment1.onShowCount);
        assertEquals(1, fragment1.onDetachCount);
        assertEquals(1, fragment1.onAttachCount);
    }

    // Ensure that removing and adding the same view results in no operation
    @Test
    public void removeRedundantAdd() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        int id = mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(1, fragment1.onCreateViewCount);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .remove(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        FragmentTestUtil.assertChildren(mContainer, fragment1);
        // should be optimized out
        assertEquals(1, fragment1.onCreateViewCount);

        mFM.popBackStack(id, 0);
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer, fragment1);
        // optimize out going back, too
        assertEquals(1, fragment1.onCreateViewCount);
    }

    // detaching, then attaching results in on change. Hide still functions
    @Test
    public void removeRedundantAttach() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        int id = mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(1, fragment1.onAttachCount);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .detach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .attach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        FragmentTestUtil.assertChildren(mContainer, fragment1);
        // can optimize out the detach/attach
        assertEquals(0, fragment1.onDestroyViewCount);
        assertEquals(1, fragment1.onHideCount);
        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onCreateViewCount);
        assertEquals(1, fragment1.onAttachCount);
        assertEquals(0, fragment1.onDetachCount);

        mFM.popBackStack(id, 0);
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        // optimized out again, but not the show
        assertEquals(0, fragment1.onDestroyViewCount);
        assertEquals(1, fragment1.onHideCount);
        assertEquals(1, fragment1.onShowCount);
        assertEquals(1, fragment1.onCreateViewCount);
        assertEquals(1, fragment1.onAttachCount);
        assertEquals(0, fragment1.onDetachCount);
    }

    // attaching, then detaching shouldn't result in a View being created
    @Test
    public void removeRedundantDetach() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        int id = mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .detach(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        // the add detach is not fully optimized out
        assertEquals(1, fragment1.onAttachCount);
        assertEquals(0, fragment1.onDetachCount);
        assertTrue(fragment1.isDetached());
        assertEquals(0, fragment1.onCreateViewCount);
        FragmentTestUtil.assertChildren(mContainer);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .attach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .detach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        FragmentTestUtil.assertChildren(mContainer);
        // can optimize out the attach/detach, and the hide call
        assertEquals(1, fragment1.onAttachCount);
        assertEquals(0, fragment1.onDetachCount);
        assertEquals(1, fragment1.onHideCount);
        assertTrue(fragment1.isHidden());
        assertEquals(0, fragment1.onShowCount);

        mFM.popBackStack(id, 0);
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer);

        // we can optimize out the attach/detach on the way back
        assertEquals(1, fragment1.onAttachCount);
        assertEquals(0, fragment1.onDetachCount);
        assertEquals(1, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);
        assertFalse(fragment1.isHidden());
    }

    // show, then hide should optimize out
    @Test
    public void removeRedundantHide() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        int id = mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .hide(fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .show(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .remove(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        FragmentTestUtil.assertChildren(mContainer, fragment1);
        // optimize out hide/show
        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, id, 0);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        // still optimized out
        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .show(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        // The show/hide can be optimized out and nothing should change.
        FragmentTestUtil.assertChildren(mContainer, fragment1);
        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, id, 0);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .show(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .detach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .attach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        // the detach/attach should not affect the show/hide, so show/hide should cancel each other
        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, id, 0);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        assertEquals(0, fragment1.onShowCount);
        assertEquals(1, fragment1.onHideCount);
    }

    // hiding and showing the same view should optimize out
    @Test
    public void removeRedundantShow() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        int id = mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(0, fragment1.onShowCount);
        assertEquals(0, fragment1.onHideCount);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .hide(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .detach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .attach(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .show(fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.executePendingTransactions();
            }
        });

        FragmentTestUtil.assertChildren(mContainer, fragment1);
        // can optimize out the show/hide
        assertEquals(0, fragment1.onShowCount);
        assertEquals(0, fragment1.onHideCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, id,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        assertEquals(0, fragment1.onShowCount);
        assertEquals(0, fragment1.onHideCount);
    }

    // The View order shouldn't be messed up by reordering -- a view that
    // is optimized to not remove/add should be in its correct position after
    // the transaction completes.
    @Test
    public void viewOrder() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        int id = mFM.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(mContainer, fragment1);

        final CountCallsFragment fragment2 = new CountCallsFragment();

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment2)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();

                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer, fragment2, fragment1);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, id, 0);
        FragmentTestUtil.assertChildren(mContainer, fragment1);
    }

    // Popping an added transaction results in no operation
    @Test
    public void addPopBackStack() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.popBackStack();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer);

        // Was never instantiated because it was popped before anything could happen
        assertEquals(0, fragment1.onCreateViewCount);
    }

    // A non-back-stack transaction doesn't interfere with back stack add/pop
    // optimization.
    @Test
    public void popNonBackStack() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        final CountCallsFragment fragment2 = new CountCallsFragment();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment2)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.popBackStack();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer, fragment2);

        // It should be optimized with the replace, so no View creation
        assertEquals(0, fragment1.onCreateViewCount);
    }

    // When reordering is disabled, the transaction prior to the disabled reordering
    // transaction should all be run prior to running the ordered transaction.
    @Test
    public void noReordering() throws Throwable {
        final CountCallsFragment fragment1 = new CountCallsFragment();
        final CountCallsFragment fragment2 = new CountCallsFragment();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mFM.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .addToBackStack(null)
                        .setReorderingAllowed(true)
                        .commit();
                mFM.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment2)
                        .addToBackStack(null)
                        .setReorderingAllowed(false)
                        .commit();
                mFM.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(mContainer, fragment2);

        // No reordering, so fragment1 should have created its View
        assertEquals(1, fragment1.onCreateViewCount);
    }

    // Test that a fragment view that is created with focus has focus after the transaction
    // completes.
    @UiThreadTest
    @Test
    public void focusedView() throws Throwable {
        mActivityRule.getActivity().setContentView(R.layout.double_container);
        mContainer = (ViewGroup) mActivityRule.getActivity().findViewById(R.id.fragmentContainer1);
        EditText firstEditText = new EditText(mContainer.getContext());
        mContainer.addView(firstEditText);
        firstEditText.requestFocus();

        assertTrue(firstEditText.isFocused());
        final CountCallsFragment fragment1 = new CountCallsFragment();
        final CountCallsFragment fragment2 = new CountCallsFragment();
        fragment2.setLayoutId(R.layout.with_edit_text);
        mFM.beginTransaction()
                .add(R.id.fragmentContainer2, fragment1)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        mFM.beginTransaction()
                .replace(R.id.fragmentContainer2, fragment2)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .commit();
        mFM.executePendingTransactions();
        final View editText = fragment2.getView().findViewById(R.id.editText);
        assertTrue(editText.isFocused());
        assertFalse(firstEditText.isFocused());
    }
}
