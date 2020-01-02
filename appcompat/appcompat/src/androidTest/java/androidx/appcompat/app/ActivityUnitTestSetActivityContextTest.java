/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.appcompat.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.test.ActivityUnitTestCase;

import androidx.appcompat.view.ContextThemeWrapper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Regression test for {@code ActivityUnitTestCase#setActivityContext} which was deprecated in
 * SDK 24 but may still be in use by code bases that have not migrated to {@code androidx.test}.
 */
@SuppressWarnings("deprecation")
public class ActivityUnitTestSetActivityContextTest extends
        ActivityUnitTestCase<AppCompatActivity> {
    protected Context mContext;
    protected Intent mStartIntent;
    protected AppCompatActivity mActivity;

    public ActivityUnitTestSetActivityContextTest() {
        super(AppCompatActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getInstrumentation().getTargetContext();
        mStartIntent = new Intent(Intent.ACTION_MAIN);
    }

    @Override
    public void tearDown() throws Exception {
        if (mActivity != null) {
            getInstrumentation().callActivityOnPause(mActivity);
        }
        super.tearDown();
    }

    public void testSetActivityContext() throws Throwable {
        final AtomicBoolean serviceStarted = new AtomicBoolean(false);

        // To catch the startService call.
        setActivityContext(new ContextThemeWrapper(getInstrumentation().getTargetContext(),
                androidx.appcompat.test.R.style.Theme_TextColors) {
            @Override
            public ComponentName startService(Intent service) {
                serviceStarted.set(true);
                return null;
            }
        });

        Looper.prepare();
        mActivity = startActivity(mStartIntent, null, null);
        getInstrumentation().callActivityOnResume(mActivity);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mActivity.startService(new Intent(""));
                } catch (IllegalArgumentException e) {
                    assertFalse("Wrong implementation of startService() called.", true);
                }
            }
        });
        assertTrue("Custom implementation of startService() was called", serviceStarted.get());

        int[] attrs = new int[] { android.R.attr.textColorPrimary };
        int textColorPrimary = mActivity.getTheme().obtainStyledAttributes(attrs).getColor(0, 0);
        assertEquals("Custom theme was preserved", 0xFF0000FF, textColorPrimary);
    }
}
