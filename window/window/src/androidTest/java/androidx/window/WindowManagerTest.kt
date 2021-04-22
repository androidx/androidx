/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Rect
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.util.concurrent.MoreExecutors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/** Tests for [WindowManager] class.  */
@LargeTest
@RunWith(AndroidJUnit4::class)
public class WindowManagerTest : WindowTestBase() {

    @After
    public fun tearDown() {
        WindowBoundsHelper.setForTesting(null)
    }

    @Test
    public fun testConstructor_activity() {
        WindowManager(
            mock(Activity::class.java),
            mock(WindowBackend::class.java)
        )
    }

    @Test
    public fun testConstructor_wrappedActivity() {
        WindowManager(
            ContextWrapper(mock(Activity::class.java)),
            mock(WindowBackend::class.java)
        )
    }

    @Test
    public fun testConstructor_nullWindowBackend() {
        WindowManager(mock(Activity::class.java))
    }

    @Test(expected = IllegalArgumentException::class)
    public fun testConstructor_applicationContext() {
        WindowManager(
            ApplicationProvider.getApplicationContext(),
            mock(WindowBackend::class.java)
        )
    }

    @Test
    public fun testRegisterLayoutChangeCallback() {
        val backend = mock(WindowBackend::class.java)
        val activity = mock(
            Activity::class.java
        )
        val wm = WindowManager(activity, backend)
        val executor = MoreExecutors.directExecutor()
        val consumer: Consumer<WindowLayoutInfo> = mock(
            WindowLayoutInfoConsumer::class.java
        )
        wm.registerLayoutChangeCallback(executor, consumer)
        verify(backend).registerLayoutChangeCallback(activity, executor, consumer)
        wm.unregisterLayoutChangeCallback(consumer)
        verify(backend).unregisterLayoutChangeCallback(ArgumentMatchers.eq(consumer))
    }

    @Test
    public fun testGetCurrentWindowMetrics() {
        val backend = mock(WindowBackend::class.java)
        val activity = mock(
            Activity::class.java
        )
        val wm = WindowManager(activity, backend)
        val bounds = Rect(1, 2, 3, 4)
        val mWindowBoundsHelper = TestWindowBoundsHelper()
        mWindowBoundsHelper.setCurrentBoundsForActivity(activity, bounds)
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper)
        val windowMetrics = wm.getCurrentWindowMetrics()
        assertNotNull(windowMetrics)
        assertEquals(bounds, windowMetrics.bounds)
    }

    @Test
    public fun testGetMaximumWindowMetrics() {
        val backend = mock(WindowBackend::class.java)
        val activity = mock(
            Activity::class.java
        )
        val wm = WindowManager(activity, backend)
        val bounds = Rect(0, 2, 4, 5)
        val mWindowBoundsHelper = TestWindowBoundsHelper()
        mWindowBoundsHelper.setMaximumBoundsForActivity(activity, bounds)
        WindowBoundsHelper.setForTesting(mWindowBoundsHelper)
        val windowMetrics = wm.getMaximumWindowMetrics()
        assertNotNull(windowMetrics)
        assertEquals(bounds, windowMetrics.bounds)
    }

    private interface WindowLayoutInfoConsumer : Consumer<WindowLayoutInfo>
}
