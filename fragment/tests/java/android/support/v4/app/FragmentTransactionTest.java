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

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertEquals;

import android.os.SystemClock;
import android.support.fragment.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.FragmentTestActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

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

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
    }

    @Test
    public void testAddTransactionWithValidFragment() throws Throwable {
        final Fragment fragment = new CorrectFragment();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().beginTransaction()
                        .add(R.id.content, fragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue(fragment.isAdded());
    }

    @Test
    public void testAddTransactionWithPrivateFragment() throws Throwable {
        final Fragment fragment = new PrivateFragment();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean exceptionThrown = false;
                try {
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.content, fragment)
                            .addToBackStack(null)
                            .commit();
                    mActivity.getSupportFragmentManager().executePendingTransactions();
                } catch (IllegalStateException e) {
                    exceptionThrown = true;
                } finally {
                    assertTrue("Exception should be thrown", exceptionThrown);
                    assertFalse("Fragment shouldn't be added", fragment.isAdded());
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void testAddTransactionWithPackagePrivateFragment() throws Throwable {
        final Fragment fragment = new PackagePrivateFragment();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean exceptionThrown = false;
                try {
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.content, fragment)
                            .addToBackStack(null)
                            .commit();
                    mActivity.getSupportFragmentManager().executePendingTransactions();
                } catch (IllegalStateException e) {
                    exceptionThrown = true;
                } finally {
                    assertTrue("Exception should be thrown", exceptionThrown);
                    assertFalse("Fragment shouldn't be added", fragment.isAdded());
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void testAddTransactionWithAnonymousFragment() throws Throwable {
        final Fragment fragment = new Fragment() {};
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean exceptionThrown = false;
                try {
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.content, fragment)
                            .addToBackStack(null)
                            .commit();
                    mActivity.getSupportFragmentManager().executePendingTransactions();
                } catch (IllegalStateException e) {
                    exceptionThrown = true;
                } finally {
                    assertTrue("Exception should be thrown", exceptionThrown);
                    assertFalse("Fragment shouldn't be added", fragment.isAdded());
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void testAddTransactionWithNonStaticFragment() throws Throwable {
        final Fragment fragment = new NonStaticFragment();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean exceptionThrown = false;
                try {
                    mActivity.getSupportFragmentManager().beginTransaction()
                            .add(R.id.content, fragment)
                            .addToBackStack(null)
                            .commit();
                    mActivity.getSupportFragmentManager().executePendingTransactions();
                } catch (IllegalStateException e) {
                    exceptionThrown = true;
                } finally {
                    assertTrue("Exception should be thrown", exceptionThrown);
                    assertFalse("Fragment shouldn't be added", fragment.isAdded());
                }
            }
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
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
        for (int i = 0; i < 100; i++) {
            Fragment fragment2 = new CorrectFragment();
            fm.beginTransaction()
                    .add(R.id.content, fragment2)
                    .addToBackStack(null)
                    .commit();
            getFragmentsUntilSize(1);

            fm.popBackStack();
            getFragmentsUntilSize(0);
        }
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
        assertEquals(0, mActivity.getSupportFragmentManager().getFragments().size());
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
}
