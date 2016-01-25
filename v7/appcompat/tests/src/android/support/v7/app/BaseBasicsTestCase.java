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

package android.support.v7.app;

import org.junit.Test;

import android.os.Build;
import android.support.v7.appcompat.test.R;
import android.support.v7.custom.FitWindowsContentLayout;
import android.support.v7.testutils.BaseTestActivity;
import android.support.v7.testutils.TestUtils;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.view.WindowInsets;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class BaseBasicsTestCase<A extends BaseTestActivity>
        extends BaseInstrumentationTestCase<A> {

    protected BaseBasicsTestCase(Class<A> activityClass) {
        super(activityClass);
    }

    @Test
    @SmallTest
    public void testActionBarExists() {
        assertNotNull("ActionBar is not null", getActivity().getSupportActionBar());
    }

    @Test
    @SmallTest
    public void testDefaultActionBarTitle() {
        assertEquals(getActivity().getTitle(), getActivity().getSupportActionBar().getTitle());
    }

    @Test
    @SmallTest
    public void testSetActionBarTitle() throws Throwable {
        final String newTitle = "hello";
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().setTitle(newTitle);
                assertEquals("New title is set to ActionBar",
                        newTitle, getActivity().getSupportActionBar().getTitle());
            }
        });
    }

    @Test
    @SmallTest
    public void testMenuInvalidationAfterDestroy() throws Throwable {
        final A activity = getActivity();
        // Reset to make sure that we don't have a menu currently
        activity.reset();
        assertNull(activity.getMenu());

        // Now destroy the Activity
        activity.finish();
        TestUtils.waitForActivityDestroyed(activity);

        // Now dispatch a menu invalidation and wait for an idle sync
        activity.supportInvalidateOptionsMenu();
        getInstrumentation().waitForIdleSync();

        // Make sure that we don't have a menu given to use after being destroyed
        assertNull(activity.getMenu());
    }

    @Test
    @SmallTest
    public void testFitSystemWindowsReachesContent() {
        final FitWindowsContentLayout content =
                (FitWindowsContentLayout) getActivity().findViewById(R.id.test_content);
        assertNotNull(content);
        assertTrue(content.getFitsSystemWindowsCalled());
    }

    @Test
    @SmallTest
    public void testOnApplyWindowInsetsReachesContent() {
        if (Build.VERSION.SDK_INT < 21) {
            // OnApplyWindowInsetsListener is only available on API 21+
            return;
        }

        final View content = getActivity().findViewById(R.id.test_content);
        assertNotNull(content);

        final AtomicBoolean applyWindowInsetsCalled = new AtomicBoolean();
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                applyWindowInsetsCalled.set(true);
                return windowInsets;
            }
        });
        assertTrue(applyWindowInsetsCalled.get());
    }
}
