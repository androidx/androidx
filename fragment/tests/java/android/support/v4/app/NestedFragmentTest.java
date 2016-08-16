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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.test.FragmentTestActivity;
import android.support.v4.app.test.FragmentTestActivity.ChildFragment;
import android.support.v4.app.test.FragmentTestActivity.ParentFragment;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.LargeTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LargeTest
public class NestedFragmentTest extends ActivityInstrumentationTestCase2<FragmentTestActivity> {

    ParentFragment mParentFragment;

    public NestedFragmentTest() {
        super(FragmentTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        mParentFragment = new ParentFragment();
        fragmentManager.beginTransaction().add(mParentFragment, "parent").commit();
        final CountDownLatch latch = new CountDownLatch(1);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                fragmentManager.executePendingTransactions();
                latch.countDown();
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @UiThreadTest
    public void testThrowsWhenUsingReservedRequestCode() {
        try {
            mParentFragment.getChildFragment().startActivityForResult(
                new Intent(Intent.ACTION_CALL), 16777216 /* requestCode */);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
    }

    public void testNestedFragmentStartActivityForResult() throws Exception {
        Instrumentation.ActivityResult activityResult = new Instrumentation.ActivityResult(
                Activity.RESULT_OK, new Intent());

        Instrumentation.ActivityMonitor activityMonitor =
                getInstrumentation().addMonitor(
                        new IntentFilter(Intent.ACTION_CALL), activityResult, true /* block */);

        // Sanity check that onActivityResult hasn't been called yet.
        assertFalse(mParentFragment.getChildFragment().onActivityResultCalled);

        final CountDownLatch latch = new CountDownLatch(1);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mParentFragment.getChildFragment().startActivityForResult(
                        new Intent(Intent.ACTION_CALL),
                        5 /* requestCode */);
                latch.countDown();
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        assertTrue(getInstrumentation().checkMonitorHit(activityMonitor, 1));

        final ChildFragment childFragment = mParentFragment.getChildFragment();
        assertTrue(childFragment.onActivityResultCalled);
        assertEquals(5, childFragment.onActivityResultRequestCode);
        assertEquals(Activity.RESULT_OK, childFragment.onActivityResultResultCode);
    }
}
