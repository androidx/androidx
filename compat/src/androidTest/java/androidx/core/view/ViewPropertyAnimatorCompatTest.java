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

package androidx.core.view;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.support.test.filters.MediumTest;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;

import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
public class ViewPropertyAnimatorCompatTest extends BaseInstrumentationTestCase<VpaActivity> {

    private static final int WAIT_TIMEOUT_MS = 200;

    private View mView;
    private int mNumListenerCalls = 0;

    public ViewPropertyAnimatorCompatTest() {
        super(VpaActivity.class);
    }

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        mView = activity.findViewById(R.id.view);
    }

    @Test
    public void testWithEndAction() throws Throwable {
        final CountDownLatch latch1 = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.animate(mView).alpha(0).setDuration(100).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        latch1.countDown();
                    }
                });
            }
        });
        assertTrue(latch1.await(300, TimeUnit.MILLISECONDS));

        // This test ensures that the endAction listener will be called exactly once
        mNumListenerCalls = 0;
        final CountDownLatch latch2 = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.animate(mView).alpha(0).setDuration(50).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ++mNumListenerCalls;
                        ViewCompat.animate(mView).alpha(1);
                        latch2.countDown();
                    }
                });
            }
        });
        assertTrue(latch2.await(200, TimeUnit.MILLISECONDS));
        waitAndCheckCallCount(1);
    }

    @Test
    public void testWithStartAction() throws Throwable {
        final CountDownLatch latch1 = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.animate(mView).alpha(0).setDuration(100).withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        latch1.countDown();
                    }
                });
            }
        });
        assertTrue(latch1.await(100, TimeUnit.MILLISECONDS));

        // This test ensures that the startAction listener will be called exactly once
        mNumListenerCalls = 0;
        final CountDownLatch latch2 = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.animate(mView).alpha(0).setDuration(50).withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        ++mNumListenerCalls;
                        ViewCompat.animate(mView).alpha(1);
                        latch2.countDown();
                    }
                });
            }
        });
        assertTrue(latch2.await(200, TimeUnit.MILLISECONDS));
        waitAndCheckCallCount(1);
    }

    void waitAndCheckCallCount(final int count) throws InterruptedException {
        int timeLeft = WAIT_TIMEOUT_MS;
        while (mNumListenerCalls != count) {
            Thread.sleep(20);
            timeLeft -= 20;
            assertTrue(timeLeft > 0);
        }
    }
}
