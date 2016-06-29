/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.fragment.test.R;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.app.test.FragmentTestActivity.TestFragment;
import android.support.v4.view.ViewCompat;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;

@MediumTest
public class FragmentTransitionTest extends
        ActivityInstrumentationTestCase2<FragmentTestActivity> {
    private TestFragment mStartFragment;
    private TestFragment mMidFragment;
    private TestFragment mEndFragment;
    private FragmentTestActivity mActivity;

    public FragmentTransitionTest() {
        super(FragmentTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mStartFragment = null;
        mMidFragment = null;
        mEndFragment = null;
        mActivity = getActivity();
    }

    public void testFragmentTransition() throws Throwable {
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            return;
        }
        launchStartFragment();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View sharedElement = mActivity.findViewById(R.id.hello);
                assertEquals("source", ViewCompat.getTransitionName(sharedElement));

                mEndFragment = TestFragment.create(R.layout.fragment_end);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mEndFragment)
                        .addSharedElement(sharedElement, "destination")
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForEnd(mEndFragment, TestFragment.ENTER);
        assertTrue(mEndFragment.wasEndCalled(TestFragment.ENTER));
        assertTrue(mStartFragment.wasEndCalled(TestFragment.EXIT));
        assertTrue(mEndFragment.wasEndCalled(TestFragment.SHARED_ELEMENT_ENTER));
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View textView = mActivity.findViewById(R.id.hello);
                assertEquals("destination", ViewCompat.getTransitionName(textView));
                mActivity.getSupportFragmentManager().popBackStack();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForEnd(mStartFragment, TestFragment.REENTER);
        assertTrue(mStartFragment.wasEndCalled(TestFragment.REENTER));
        assertTrue(mEndFragment.wasEndCalled(TestFragment.RETURN));
    }

    public void testFirstOutLastInTransition() throws Throwable {
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            return;
        }
        launchStartFragment();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMidFragment = TestFragment.create(R.layout.fragment_middle);
                mEndFragment = TestFragment.create(R.layout.fragment_end);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mMidFragment)
                        .replace(R.id.content, mEndFragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForEnd(mEndFragment, TestFragment.ENTER);
        assertTrue(mEndFragment.wasEndCalled(TestFragment.ENTER));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.EXIT));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.REENTER));

        assertTrue(mStartFragment.wasEndCalled(TestFragment.EXIT));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.REENTER));

        assertFalse(mMidFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.RETURN));

        mStartFragment.clearNotifications();
        mEndFragment.clearNotifications();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().popBackStack();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForEnd(mEndFragment, TestFragment.RETURN);
        assertTrue(mEndFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.RETURN));

        assertTrue(mStartFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.RETURN));
    }

    public void testPopTwo() throws Throwable {
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            return;
        }
        launchStartFragment();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMidFragment = TestFragment.create(R.layout.fragment_middle);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mMidFragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForEnd(mMidFragment, TestFragment.ENTER);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEndFragment = TestFragment.create(R.layout.fragment_end);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mEndFragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForEnd(mEndFragment, TestFragment.ENTER);
        assertTrue(mEndFragment.wasEndCalled(TestFragment.ENTER));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.EXIT));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.REENTER));

        assertTrue(mStartFragment.wasEndCalled(TestFragment.EXIT));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.REENTER));

        assertTrue(mMidFragment.wasStartCalled(TestFragment.ENTER));
        assertTrue(mMidFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.RETURN));

        mStartFragment.clearNotifications();
        mMidFragment.clearNotifications();
        mEndFragment.clearNotifications();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentManager fm = mActivity.getSupportFragmentManager();
                int id = fm.getBackStackEntryAt(0).getId();
                fm.popBackStack(id, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fm.executePendingTransactions();
            }
        });
        waitForEnd(mEndFragment, TestFragment.RETURN);
        assertTrue(mEndFragment.wasEndCalled(TestFragment.RETURN));

        assertFalse(mMidFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.RETURN));

        assertTrue(mStartFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.RETURN));
    }

    public void testNullTransition() throws Throwable {
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            return;
        }
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartFragment = TestFragment.create(R.layout.fragment_start);
                mStartFragment.clearTransitions();
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mStartFragment)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForStart(mStartFragment, TestFragment.ENTER);
        // No transitions
        assertFalse(mStartFragment.wasStartCalled(TestFragment.ENTER));

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMidFragment = TestFragment.create(R.layout.fragment_middle);
                mEndFragment = TestFragment.create(R.layout.fragment_end);
                mEndFragment.clearTransitions();
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mMidFragment)
                        .replace(R.id.content, mEndFragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForStart(mEndFragment, TestFragment.ENTER);
        assertFalse(mEndFragment.wasEndCalled(TestFragment.ENTER));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.EXIT));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mEndFragment.wasEndCalled(TestFragment.REENTER));

        assertFalse(mStartFragment.wasEndCalled(TestFragment.EXIT));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mStartFragment.wasEndCalled(TestFragment.REENTER));

        assertFalse(mMidFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.RETURN));

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().popBackStack();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForStart(mEndFragment, TestFragment.RETURN);
        assertFalse(mEndFragment.wasEndCalled(TestFragment.RETURN));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mMidFragment.wasStartCalled(TestFragment.RETURN));

        assertFalse(mStartFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.RETURN));
    }

    public void testRemoveAdded() throws Throwable {
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            return;
        }
        launchStartFragment();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEndFragment = TestFragment.create(R.layout.fragment_end);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mEndFragment)
                        .replace(R.id.content, mStartFragment)
                        .replace(R.id.content, mEndFragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        assertTrue(waitForEnd(mEndFragment, TestFragment.ENTER));
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().popBackStack();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        assertTrue(waitForEnd(mStartFragment, TestFragment.REENTER));
    }

    public void testAddRemoved() throws Throwable {
        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            return;
        }
        launchStartFragment();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEndFragment = TestFragment.create(R.layout.fragment_end);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mEndFragment)
                        .replace(R.id.content, mStartFragment)
                        .addToBackStack(null)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForStart(mStartFragment, TestFragment.ENTER);
        assertFalse(mStartFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.EXIT));
        assertFalse(mEndFragment.wasStartCalled(TestFragment.ENTER));
        assertFalse(mEndFragment.wasStartCalled(TestFragment.EXIT));
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.getSupportFragmentManager().popBackStack();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        waitForStart(mStartFragment, TestFragment.REENTER);
        assertFalse(mStartFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mStartFragment.wasStartCalled(TestFragment.RETURN));
        assertFalse(mEndFragment.wasStartCalled(TestFragment.REENTER));
        assertFalse(mEndFragment.wasStartCalled(TestFragment.RETURN));
    }

    private void launchStartFragment() throws Throwable {
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStartFragment = TestFragment.create(R.layout.fragment_start);
                mActivity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content, mStartFragment)
                        .commit();
                mActivity.getSupportFragmentManager().executePendingTransactions();
            }
        });
        assertTrue(waitForEnd(mStartFragment, TestFragment.ENTER));
        mStartFragment.clearNotifications();
    }

    private boolean waitForStart(TestFragment fragment, int key) throws InterruptedException {
        boolean started = fragment.waitForStart(key);
        getInstrumentation().waitForIdleSync();
        return started;
    }

    private boolean waitForEnd(TestFragment fragment, int key) throws InterruptedException {
        if (!waitForStart(fragment, key)) {
            return false;
        }
        final boolean ended = fragment.waitForEnd(key);
        getInstrumentation().waitForIdleSync();
        return ended;
    }
}
