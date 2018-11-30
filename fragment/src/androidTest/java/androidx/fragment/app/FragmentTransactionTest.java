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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.app.test.NewIntentActivity;
import androidx.fragment.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests usage of the {@link FragmentTransaction} class.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentTransactionTest {

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<>(FragmentTestActivity.class);

    private FragmentTestActivity mActivity;
    private int mOnBackStackChangedTimes;
    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mOnBackStackChangedTimes = 0;
        mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                mOnBackStackChangedTimes++;
            }
        };
        mActivity.getSupportFragmentManager()
                .addOnBackStackChangedListener(mOnBackStackChangedListener);
    }

    @After
    public void tearDown() {
        mActivity.getSupportFragmentManager()
                .removeOnBackStackChangedListener(mOnBackStackChangedListener);
        mOnBackStackChangedListener = null;
    }

    @Test
    @UiThreadTest
    public void testAddTransactionWithValidFragment() {
        final Fragment fragment = new CorrectFragment();
        mActivity.getSupportFragmentManager().beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
        assertEquals(1, mOnBackStackChangedTimes);
        assertTrue(fragment.isAdded());
    }

    @Test
    @UiThreadTest
    public void testAddTransactionWithPrivateFragment() {
        final Fragment fragment = new PrivateFragment();
        boolean exceptionThrown = false;
        try {
            mActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
            mActivity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(1, mOnBackStackChangedTimes);
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        } finally {
            assertTrue("Exception should be thrown", exceptionThrown);
            assertFalse("Fragment shouldn't be added", fragment.isAdded());
        }
    }

    @Test
    @UiThreadTest
    public void testAddTransactionWithPackagePrivateFragment() {
        final Fragment fragment = new PackagePrivateFragment();
        boolean exceptionThrown = false;
        try {
            mActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
            mActivity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(1, mOnBackStackChangedTimes);
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        } finally {
            assertTrue("Exception should be thrown", exceptionThrown);
            assertFalse("Fragment shouldn't be added", fragment.isAdded());
        }
    }

    @Test
    @UiThreadTest
    public void testAddTransactionWithAnonymousFragment() {
        final Fragment fragment = new Fragment() {};
        boolean exceptionThrown = false;
        try {
            mActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
            mActivity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(1, mOnBackStackChangedTimes);
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        } finally {
            assertTrue("Exception should be thrown", exceptionThrown);
            assertFalse("Fragment shouldn't be added", fragment.isAdded());
        }
    }

    @Test
    @UiThreadTest
    public void testGetLayoutInflater() {
        final OnGetLayoutInflaterFragment fragment1 = new OnGetLayoutInflaterFragment();
        assertEquals(0, fragment1.onGetLayoutInflaterCalls);
        mActivity.getSupportFragmentManager().beginTransaction()
                .add(R.id.content, fragment1)
                .addToBackStack(null)
                .commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
        assertEquals(1, fragment1.onGetLayoutInflaterCalls);
        assertEquals(fragment1.layoutInflater, fragment1.getLayoutInflater());
        // getLayoutInflater() didn't force onGetLayoutInflater()
        assertEquals(1, fragment1.onGetLayoutInflaterCalls);

        LayoutInflater layoutInflater = fragment1.layoutInflater;
        // Replacing fragment1 won't detach it, so the value won't be cleared
        final OnGetLayoutInflaterFragment fragment2 = new OnGetLayoutInflaterFragment();
        mActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, fragment2)
                .addToBackStack(null)
                .commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();

        assertSame(layoutInflater, fragment1.getLayoutInflater());
        assertEquals(1, fragment1.onGetLayoutInflaterCalls);

        // Popping it should cause onCreateView again, so a new LayoutInflater...
        mActivity.getSupportFragmentManager().popBackStackImmediate();
        assertNotSame(layoutInflater, fragment1.getLayoutInflater());
        assertEquals(2, fragment1.onGetLayoutInflaterCalls);
        layoutInflater = fragment1.layoutInflater;
        assertSame(layoutInflater, fragment1.getLayoutInflater());

        // Popping it should detach it, clearing the cached value again
        mActivity.getSupportFragmentManager().popBackStackImmediate();

        // once it is detached, the getLayoutInflater() will default to throw
        // an exception, but we've made it return null instead.
        assertEquals(2, fragment1.onGetLayoutInflaterCalls);
        try {
            fragment1.getLayoutInflater();
            fail("getLayoutInflater should throw when the Fragment is detached");
        } catch (IllegalStateException e) {
            // Expected
        }
        assertEquals(3, fragment1.onGetLayoutInflaterCalls);
    }

    @Test
    @UiThreadTest
    public void testAddTransactionWithNonStaticFragment() {
        final Fragment fragment = new NonStaticFragment();
        boolean exceptionThrown = false;
        try {
            mActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.content, fragment)
                    .addToBackStack(null)
                    .commit();
            mActivity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(1, mOnBackStackChangedTimes);
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        } finally {
            assertTrue("Exception should be thrown", exceptionThrown);
            assertFalse("Fragment shouldn't be added", fragment.isAdded());
        }
    }

    @Test
    @UiThreadTest
    public void testPostOnCommit() {
        final boolean[] ran = new boolean[1];
        FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        fm.beginTransaction().runOnCommit(new Runnable() {
            @Override
            public void run() {
                ran[0] = true;
            }
        }).commit();
        fm.executePendingTransactions();

        assertTrue("runOnCommit runnable never ran", ran[0]);

        ran[0] = false;

        boolean threw = false;
        try {
            fm.beginTransaction().runOnCommit(new Runnable() {
                @Override
                public void run() {
                    ran[0] = true;
                }
            }).addToBackStack(null).commit();
        } catch (IllegalStateException ise) {
            threw = true;
        }

        fm.executePendingTransactions();

        assertTrue("runOnCommit was allowed to be called for back stack transaction",
                threw);
        assertFalse("runOnCommit runnable for back stack transaction was run", ran[0]);
    }

    // Ensure that getFragments() works during transactions, even if it is run off thread
    @Test
    public void getFragmentsOffThread() throws Throwable {
        final FragmentManager fm = mActivity.getSupportFragmentManager();

        // Make sure that adding a fragment works
        Fragment fragment = new CorrectFragment();
        fm.beginTransaction()
                .add(R.id.content, fragment)
                .addToBackStack(null)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule);
        Collection<Fragment> fragments = fm.getFragments();
        assertEquals(1, fragments.size());
        assertTrue(fragments.contains(fragment));

        // Removed fragments shouldn't show
        fm.beginTransaction()
                .remove(fragment)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertTrue(fm.getFragments().isEmpty());

        // Now try detached fragments
        FragmentTestUtil.popBackStackImmediate(mActivityRule);
        fm.beginTransaction()
                .detach(fragment)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertTrue(fm.getFragments().isEmpty());

        // Now try hidden fragments
        FragmentTestUtil.popBackStackImmediate(mActivityRule);
        fm.beginTransaction()
                .hide(fragment)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        fragments = fm.getFragments();
        assertEquals(1, fragments.size());
        assertTrue(fragments.contains(fragment));

        // And showing it again shouldn't change anything:
        FragmentTestUtil.popBackStackImmediate(mActivityRule);
        fragments = fm.getFragments();
        assertEquals(1, fragments.size());
        assertTrue(fragments.contains(fragment));

        // Now pop back to the start state
        FragmentTestUtil.popBackStackImmediate(mActivityRule);

        // We can't force concurrency, but we can do it lots of times and hope that
        // we hit it.
        // Reset count here to verify afterwards

        // Wait until we receive a OnBackStackChange callback for the total number of times
        // specified by transactionCount times 2 (1 for adding, 1 for removal)
        final int transactionCount = 100;
        final CountDownLatch backStackLatch = new CountDownLatch(transactionCount * 2);
        final FragmentManager.OnBackStackChangedListener countDownListener =
                new FragmentManager.OnBackStackChangedListener() {

            @Override
            public void onBackStackChanged() {
                backStackLatch.countDown();
            }
        };

        fm.addOnBackStackChangedListener(countDownListener);

        for (int i = 0; i < transactionCount; i++) {
            Fragment fragment2 = new CorrectFragment();
            fm.beginTransaction()
                    .add(R.id.content, fragment2)
                    .addToBackStack(null)
                    .commit();
            getFragmentsUntilSize(1);

            fm.popBackStack();
            getFragmentsUntilSize(0);
        }

        backStackLatch.await();

        fm.removeOnBackStackChangedListener(countDownListener);
    }

    /**
     * When a FragmentManager is detached, it should allow commitAllowingStateLoss()
     * and commitNowAllowingStateLoss() by just dropping the transaction.
     */
    @Test
    public void commitAllowStateLossDetached() throws Throwable {
        Fragment fragment1 = new CorrectFragment();
        mActivity.getSupportFragmentManager()
                .beginTransaction()
                .add(fragment1, "1")
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        final FragmentManager fm = fragment1.getChildFragmentManager();
        mActivity.getSupportFragmentManager()
                .beginTransaction()
                .remove(fragment1)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        Assert.assertEquals(0, mActivity.getSupportFragmentManager().getFragments().size());
        assertEquals(0, fm.getFragments().size());

        // Now the fragment1's fragment manager should allow commitAllowingStateLoss
        // by doing nothing since it has been detached.
        Fragment fragment2 = new CorrectFragment();
        fm.beginTransaction()
                .add(fragment2, "2")
                .commitAllowingStateLoss();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(0, fm.getFragments().size());

        // It should also allow commitNowAllowingStateLoss by doing nothing
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Fragment fragment3 = new CorrectFragment();
                fm.beginTransaction()
                        .add(fragment3, "3")
                        .commitNowAllowingStateLoss();
                assertEquals(0, fm.getFragments().size());
            }
        });
    }

    /**
     * onNewIntent() should note that the state is not saved so that child fragment
     * managers can execute transactions.
     */
    @Test
    public void newIntentUnlocks() throws Throwable {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent1 = new Intent(mActivity, NewIntentActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        NewIntentActivity newIntentActivity =
                (NewIntentActivity) instrumentation.startActivitySync(intent1);
        FragmentTestUtil.waitForExecution(mActivityRule);

        Intent intent2 = new Intent(mActivity, FragmentTestActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity coveringActivity = instrumentation.startActivitySync(intent2);
        FragmentTestUtil.waitForExecution(mActivityRule);

        Intent intent3 = new Intent(mActivity, NewIntentActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.startActivity(intent3);
        assertTrue(newIntentActivity.newIntent.await(1, TimeUnit.SECONDS));
        FragmentTestUtil.waitForExecution(mActivityRule);

        for (Fragment fragment : newIntentActivity.getSupportFragmentManager().getFragments()) {
            // There really should only be one fragment in newIntentActivity.
            assertEquals(1, fragment.getChildFragmentManager().getFragments().size());
        }
    }

    private void getFragmentsUntilSize(int expectedSize) {
        final long endTime = SystemClock.uptimeMillis() + 3000;

        do {
            assertTrue(SystemClock.uptimeMillis() < endTime);
        } while (mActivity.getSupportFragmentManager().getFragments().size() != expectedSize);
    }

    public static class CorrectFragment extends Fragment {}

    private static class PrivateFragment extends Fragment {}

    static class PackagePrivateFragment extends Fragment {}

    private class NonStaticFragment extends Fragment {}

    public static class OnGetLayoutInflaterFragment extends Fragment {
        public int onGetLayoutInflaterCalls = 0;
        public LayoutInflater layoutInflater;

        @NonNull
        @Override
        public LayoutInflater onGetLayoutInflater(Bundle savedInstanceState) {
            onGetLayoutInflaterCalls++;
            layoutInflater = super.onGetLayoutInflater(savedInstanceState);
            return layoutInflater;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_a, container, false);
        }
    }
}
