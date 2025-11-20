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

package androidx.appcompat.app;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.test.R;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.jspecify.annotations.NonNull;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public abstract class BaseBasicsTestCase<A extends BaseTestActivity> {
    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    protected BaseBasicsTestCase(Class<A> activityClass) {
        mActivityTestRule = new ActivityTestRule<>(activityClass);
    }

    @Test
    public void testActionBarExists() {
        assertNotNull("ActionBar is not null",
                mActivityTestRule.getActivity().getSupportActionBar());
    }

    @Ignore // b/256195085
    @Test
    public void testActionBarOverflowVisibilityListener() {
        if ("ranchu".equals(Build.HARDWARE)) {
            // Skip this test on Android TV due to a bug in Espresso's menu handling.
            return;
        }

        ActionBar actionBar = mActivityTestRule.getActivity().getSupportActionBar();
        final boolean[] madeVisible = new boolean[] {false};
        actionBar.addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                madeVisible[0] = isVisible;
            }
        });
        openActionBarOverflowOrOptionsMenu(getApplicationContext());
        assertTrue("OnMenuVisibilityListener should be called",
                madeVisible[0]);
    }

    @UiThreadTest
    @Test
    public void testActionBarShowHideNoOverflowVisibilityListener() {
        ActionBar actionBar = mActivityTestRule.getActivity().getSupportActionBar();
        final boolean[] receivedCallback = new boolean[] {false};
        actionBar.addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                receivedCallback[0] = true;
            }
        });
        actionBar.openOptionsMenu();
        actionBar.closeOptionsMenu();
        assertFalse("OnMenuVisibilityListener should not be called",
                receivedCallback[0]);
    }

    @Test
    public void testDefaultActionBarTitle() {
        assertEquals(mActivityTestRule.getActivity().getTitle(),
                mActivityTestRule.getActivity().getSupportActionBar().getTitle());
    }

    @UiThreadTest
    @Test
    public void testSetActionBarTitleByActivity() {
        final String newTitle = "hello";
        mActivityTestRule.getActivity().setTitle(newTitle);
        assertEquals("New title is set to ActionBar",
                newTitle, mActivityTestRule.getActivity().getSupportActionBar().getTitle());
    }

    @UiThreadTest
    @Test
    public void testSetActionBarTitleByActionBar() {
        final String newTitle = "hello";
        mActivityTestRule.getActivity().getSupportActionBar().setTitle(newTitle);
        assertEquals("New title is set to ActionBar",
                newTitle, mActivityTestRule.getActivity().getSupportActionBar().getTitle());
        assertEquals("New title is set to root view's accessibilityPaneTitle",
                newTitle,
                ViewCompat.getAccessibilityPaneTitle(
                        mActivityTestRule.getActivity().getWindow().getDecorView()));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    public void testOnApplyWindowInsetsReachesContent() throws Throwable {
        final A activity = mActivityTestRule.getActivity();
        if (!canShowSystemUi(activity)) {
            // Device cannot show system UI so setSystemUiVisibility will do nothing.
            return;
        }

        final View content = activity.findViewById(R.id.test_content);
        assertNotNull(content);

        final WindowInsetsCompat receivedInsets = waitForWindowInsets(content);
        assertNotNull(receivedInsets);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34) // b/427246833
    public void testOnApplyWindowInsetsReachesContent_matchesRootBottom() throws Throwable {
        final A activity = mActivityTestRule.getActivity();
        if (!canShowSystemUi(activity)) {
            // Device cannot show system UI so setSystemUiVisibility will do nothing.
            return;
        }

        final View content = activity.findViewById(R.id.test_content);
        assertNotNull(content);

        final WindowInsetsCompat receivedInsets = waitForWindowInsets(content);
        assertNotNull(receivedInsets);

        final WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(content);
        assertNotNull(rootWindowInsets);
        // Assert that we dispatch the correct bottom system window inset
        assertEquals(rootWindowInsets.getSystemWindowInsets().bottom,
                receivedInsets.getSystemWindowInsets().bottom);
    }

    @Test
    @SdkSuppress(minSdkVersion = 28, maxSdkVersion = 33) // maxSdk 33 b/322355781
    public void testOnApplyWindowInsetsReachesContent_withDisplayCutout() throws Throwable {
        final A activity = mActivityTestRule.getActivity();
        if (!canShowSystemUi(activity)) {
            // Device cannot show system UI so setSystemUiVisibility will do nothing.
            return;
        }

        final View content = activity.findViewById(R.id.test_content);
        assertNotNull(content);

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<WindowInsetsCompat> received = new AtomicReference<>();
        // Set a listener to catch WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(content, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                received.set(insets);
                latch.countDown();
                return insets;
            }
        });

        // Tell the window that we will handle insets, and tell the WindowManager that we want to
        // handle DisplayCutouts
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

                WindowManager.LayoutParams wlp = activity.getWindow().getAttributes();
                wlp.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                activity.getWindow().setAttributes(wlp);
            }
        });

        // Await an inset pass
        latch.await(5, TimeUnit.SECONDS);

        WindowInsetsCompat receivedInsets = received.get();
        WindowInsetsCompat rootWindowInsets = ViewCompat.getRootWindowInsets(content);

        assertNotNull(receivedInsets);
        assertNotNull(rootWindowInsets);
        // Assert that the DisplayCutout was properly propagated
        assertEquals(rootWindowInsets.getDisplayCutout(), receivedInsets.getDisplayCutout());
    }

    @Test
    @UiThreadTest
    public void testSupportActionModeCallbacks() {
        final A activity = mActivityTestRule.getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback callback = mock(ActionMode.Callback.class);
        when(callback.onCreateActionMode(any(ActionMode.class), any(Menu.class))).thenReturn(true);

        // Start an action mode
        final ActionMode actionMode = activity.startSupportActionMode(callback);
        assertNotNull(actionMode);

        // Now verify that onCreateActionMode and onPrepareActionMode are called once
        verify(callback).onCreateActionMode(any(ActionMode.class), any(Menu.class));
        verify(callback).onPrepareActionMode(any(ActionMode.class), any(Menu.class));

        // Now finish and verify that onDestroyActionMode is called once, and there are no more
        // interactions
        actionMode.finish();
        verify(callback).onDestroyActionMode(any(ActionMode.class));
        verifyNoMoreInteractions(callback);
    }

    @Test
    @UiThreadTest
    public void testSupportActionModeCallbacksInvalidate() {
        final A activity = mActivityTestRule.getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback callback = mock(ActionMode.Callback.class);
        when(callback.onCreateActionMode(any(ActionMode.class), any(Menu.class))).thenReturn(true);

        // Start an action mode
        final ActionMode actionMode = activity.startSupportActionMode(callback);
        // Assert that one was created
        assertNotNull(actionMode);
        // Reset the mock so that any callback counts from the create are reset
        reset(callback);

        // Now invalidate the action mode
        actionMode.invalidate();

        // Now verify that onCreateActionMode is not called, and onPrepareActionMode is called once
        verify(callback, never()).onCreateActionMode(any(ActionMode.class), any(Menu.class));
        verify(callback).onPrepareActionMode(any(ActionMode.class), any(Menu.class));
    }

    @Test
    @UiThreadTest
    public void testSupportActionModeCallbacksWithFalseOnCreate() {
        final A activity = mActivityTestRule.getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback callback = mock(ActionMode.Callback.class);
        when(callback.onCreateActionMode(any(ActionMode.class), any(Menu.class))).thenReturn(false);

        // Start an action mode
        final ActionMode actionMode = activity.startSupportActionMode(callback);

        // Now verify that onCreateActionMode is called once
        verify(callback).onCreateActionMode(any(ActionMode.class), any(Menu.class));

        // Now verify that onPrepareActionMode is not called (since onCreateActionMode
        // returns false)
        verify(callback, never()).onPrepareActionMode(any(ActionMode.class), any(Menu.class));

        // Assert that an action mode was not created
        assertNull(actionMode);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("InlinedApi")
    static boolean canShowSystemUi(Activity activity) {
        PackageManager manager = activity.getPackageManager();
        return !manager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                && !manager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                && !manager.hasSystemFeature(PackageManager.FEATURE_WATCH);
    }

    protected void testSupportActionModeAppCompatCallbacks(final boolean fromWindow) {
        final A activity = mActivityTestRule.getActivity();

        // Create a mock action mode callback which returns true from onCreateActionMode
        final ActionMode.Callback amCallback = mock(ActionMode.Callback.class);
        when(amCallback.onCreateActionMode(any(ActionMode.class), any(Menu.class)))
                .thenReturn(true);

        // Create a mock AppCompatCallback, which returns null from
        // onWindowStartingSupportActionMode, and set it on the Activity
        final AppCompatCallback apCallback = mock(AppCompatCallback.class);
        when(apCallback.onWindowStartingSupportActionMode(any(ActionMode.Callback.class)))
                .thenReturn(null);
        activity.setAppCompatCallback(apCallback);

        // Start an action mode with the action mode callback
        final ActionMode actionMode = activity.startSupportActionMode(amCallback);

        if (fromWindow) {
            // Verify that the callback's onWindowStartingSupportActionMode was called
            verify(apCallback).onWindowStartingSupportActionMode(any(ActionMode.Callback.class));
        }

        // Now assert that an action mode was created
        assertNotNull(actionMode);

        // Now verify that onSupportActionModeStarted is called once
        verify(apCallback).onSupportActionModeStarted(any(ActionMode.class));

        // Now finish and verify that onDestroyActionMode is called once
        actionMode.finish();
        verify(apCallback).onSupportActionModeFinished(any(ActionMode.class));
    }

    private WindowInsetsCompat waitForWindowInsets(final @NonNull View view) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<WindowInsetsCompat> received = new AtomicReference<>();
        // Set a listener to catch WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                received.set(insets);
                latch.countDown();
                return insets;
            }
        });

        // Tell the Window that we want to fit any system windows
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowCompat.setDecorFitsSystemWindows(
                        mActivityTestRule.getActivity().getWindow(), false);
            }
        });

        // Await an inset pass
        latch.await(5, TimeUnit.SECONDS);
        return received.get();
    }
}
