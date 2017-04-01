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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.support.fragment.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.app.test.NewIntentActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
}
