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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.app.test.FragmentTestActivity.ChildFragment;
import androidx.fragment.app.test.FragmentTestActivity.ParentFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedFragmentTest {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    private Instrumentation mInstrumentation;
    private ParentFragment mParentFragment;

    @Before
    public void setup() throws Throwable {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        final FragmentManager fragmentManager =
                mActivityRule.getActivity().getSupportFragmentManager();
        mParentFragment = new ParentFragment();
        fragmentManager.beginTransaction().add(mParentFragment, "parent").commit();
        final CountDownLatch latch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragmentManager.executePendingTransactions();
                latch.countDown();
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @UiThreadTest
    @Test(expected = IllegalArgumentException.class)
    public void testThrowsWhenUsingReservedRequestCode() {
        mParentFragment.getChildFragment().startActivityForResult(
                new Intent(Intent.ACTION_CALL), 16777216 /* requestCode */);
    }

    @Test
    public void testNestedFragmentStartActivityForResult() throws Throwable {
        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(
                Activity.RESULT_OK, new Intent());

        Instrumentation.ActivityMonitor activityMonitor =
                mInstrumentation.addMonitor(
                        new IntentFilter(Intent.ACTION_CALL), activityResult, true /* block */);

        // Sanity check that onActivityResult hasn't been called yet.
        assertFalse(mParentFragment.getChildFragment().onActivityResultCalled);

        final CountDownLatch latch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mParentFragment.getChildFragment().startActivityForResult(
                        new Intent(Intent.ACTION_CALL),
                        5 /* requestCode */);
                latch.countDown();
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        assertTrue(mInstrumentation.checkMonitorHit(activityMonitor, 1));

        final ChildFragment childFragment = mParentFragment.getChildFragment();
        assertTrue(childFragment.onActivityResultCalled);
        assertEquals(5, childFragment.onActivityResultRequestCode);
        assertEquals(Activity.RESULT_OK, childFragment.onActivityResultResultCode);
    }
}
