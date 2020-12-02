/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.window;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.ContextWrapper;
import android.graphics.Rect;

import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.util.concurrent.MoreExecutors;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

/** Tests for {@link WindowManager} class. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class WindowManagerTest extends WindowTestBase {

    @After
    public void tearDown() {
        WindowBoundsHelper.setForTesting(null);
    }

    @Test
    public void testConstructor_activity() {
        new WindowManager(mock(Activity.class), mock(WindowBackend.class));
    }

    @Test
    public void testConstructor_wrappedActivity() {
        new WindowManager(new ContextWrapper(mock(Activity.class)), mock(WindowBackend.class));
    }

    @Test
    public void testConstructor_nullWindowBackend() {
        new WindowManager(mock(Activity.class), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_applicationContext() {
        new WindowManager(ApplicationProvider.getApplicationContext(), mock(WindowBackend.class));
    }

    @Test
    public void testRegisterLayoutChangeCallback() {
        WindowBackend backend = mock(WindowBackend.class);
        Activity activity = mock(Activity.class);
        WindowManager wm = new WindowManager(activity, backend);

        Executor executor = MoreExecutors.directExecutor();
        Consumer<WindowLayoutInfo> consumer = mock(Consumer.class);
        wm.registerLayoutChangeCallback(executor, consumer);
        verify(backend).registerLayoutChangeCallback(activity, executor, consumer);

        wm.unregisterLayoutChangeCallback(consumer);
        verify(backend).unregisterLayoutChangeCallback(eq(consumer));
    }

    @Test
    public void testRegisterDeviceStateChangeCallback() {
        WindowBackend backend = mock(WindowBackend.class);
        Activity activity = mock(Activity.class);
        WindowManager wm = new WindowManager(activity, backend);

        Executor executor = MoreExecutors.directExecutor();
        Consumer<DeviceState> consumer = mock(Consumer.class);
        wm.registerDeviceStateChangeCallback(executor, consumer);
        verify(backend).registerDeviceStateChangeCallback(executor, consumer);

        wm.unregisterDeviceStateChangeCallback(consumer);
        verify(backend).unregisterDeviceStateChangeCallback(eq(consumer));
    }

    @Test
    public void testGetCurrentWindowMetrics() {
        WindowBackend backend = mock(WindowBackend.class);
        Activity activity = mock(Activity.class);
        WindowManager wm = new WindowManager(activity, backend);

        Rect bounds = new Rect(1, 2, 3, 4);
        TestWindowBoundsHelper mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setCurrentBoundsForActivity(activity, bounds);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);

        WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
        assertNotNull(windowMetrics);
        assertEquals(bounds, windowMetrics.getBounds());
    }

    @Test
    public void testGetMaximumWindowMetrics() {
        WindowBackend backend = mock(WindowBackend.class);
        Activity activity = mock(Activity.class);
        WindowManager wm = new WindowManager(activity, backend);

        Rect bounds = new Rect(0, 2, 4, 5);
        TestWindowBoundsHelper mWindowBoundsHelper = new TestWindowBoundsHelper();
        mWindowBoundsHelper.setMaximumBoundsForActivity(activity, bounds);
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper);

        WindowMetrics windowMetrics = wm.getMaximumWindowMetrics();
        assertNotNull(windowMetrics);
        assertEquals(bounds, windowMetrics.getBounds());
    }
}
