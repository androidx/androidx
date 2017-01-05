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

package android.support.v4.media.session;

import static junit.framework.Assert.fail;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class MediaSessionCompatTest {
    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);
    Context mContext;
    Map<String, LockedObject> results = new HashMap<>();

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getContext();
    }

    @Test
    public void testSetNullCallback() throws Throwable {
        initWait("testSetNullCallback");
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    MediaSessionCompat session = new MediaSessionCompat(mContext, "TEST");
                    session.setCallback(null);
                } catch (Exception e) {
                    fail("Fail with an exception: " + e);
                } finally {
                    setResultData("testSetNullCallback", true);
                }
            }
        });
        waitFor("testSetNullCallback");
    }

    private void initWait(String key) throws InterruptedException {
        results.put(key, new LockedObject());
    }

    private Object[] waitFor(String key) throws InterruptedException {
        return results.get(key).waitFor();
    }

    private void setResultData(String key, Object... args) {
        if (results.containsKey(key)) {
            results.get(key).set(args);
        }
    }

    private class LockedObject {
        private Semaphore mLock = new Semaphore(1);
        private volatile Object[] mArgs;

        public LockedObject() {
            mLock.drainPermits();
        }

        public void set(Object... args) {
            mArgs = args;
            mLock.release(1);
        }

        public Object[] waitFor() throws InterruptedException {
            mLock.tryAcquire(1, 2, TimeUnit.SECONDS);
            return mArgs;
        }
    }
}
