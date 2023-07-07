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

package androidx.test.uiautomator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UiDeviceTest {

    private static final String WATCHER_NAME = "test_watcher";

    @Rule
    public TemporaryFolder mTmpDir = new TemporaryFolder();

    private Instrumentation mInstrumentation;
    private UiDevice mDevice;
    private int mDefaultFlags;

    @Before
    public void setUp() {
        mInstrumentation = spy(InstrumentationRegistry.getInstrumentation());
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        doReturn(automation).when(mInstrumentation).getUiAutomation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            doReturn(automation).when(mInstrumentation).getUiAutomation(anyInt());
        }
        mDevice = new UiDevice(mInstrumentation);
        mDefaultFlags = Configurator.getInstance().getUiAutomationFlags();
    }

    @After
    public void tearDown() {
        Configurator.getInstance().setUiAutomationFlags(mDefaultFlags);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testGetDisplayMetrics() throws IOException {
        String densityCmdOutput = mDevice.executeShellCommand("wm density");
        Pattern densityPattern = Pattern.compile("^Physical\\sdensity:\\s(\\d+)\\D+.*");
        Matcher densityMatcher = densityPattern.matcher(densityCmdOutput);
        assertTrue(densityMatcher.find());
        String densityDpi = densityMatcher.group(1);
        assertNotNull(densityDpi);
        float density = Float.parseFloat(densityDpi) / DisplayMetrics.DENSITY_DEFAULT;

        try {
            int width = 800;
            int height = 400;
            mDevice.executeShellCommand(String.format("wm size %dx%d", width, height));

            Point expectedSizeDp = new Point(Math.round(width / density),
                    Math.round(height / density));

            assertEquals(width, mDevice.getDisplayWidth());
            assertEquals(height, mDevice.getDisplayHeight());
            assertEquals(expectedSizeDp, mDevice.getDisplaySizeDp());
        } finally {
            mDevice.executeShellCommand("wm size reset");
        }
    }

    @Test
    public void testGetProductName() {
        assertEquals(Build.PRODUCT, mDevice.getProductName());
    }

    @Test
    public void testRegisterAndRunUiWatcher_conditionMet() {
        // The watcher will return true when its watching condition is met.
        UiWatcher watcher = () -> true;
        mDevice.registerWatcher(WATCHER_NAME, watcher);

        assertFalse(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertFalse(mDevice.hasAnyWatcherTriggered());
        mDevice.runWatchers();
        assertTrue(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertTrue(mDevice.hasAnyWatcherTriggered());
    }

    @Test
    public void testRegisterAndRunUiWatcher_conditionNotMet() {
        UiWatcher watcher = () -> false;
        mDevice.registerWatcher(WATCHER_NAME, watcher);

        assertFalse(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertFalse(mDevice.hasAnyWatcherTriggered());
        mDevice.runWatchers();
        assertFalse(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertFalse(mDevice.hasAnyWatcherTriggered());
    }

    @Test
    public void testResetUiWatcher() {
        UiWatcher watcher = () -> true;
        mDevice.registerWatcher(WATCHER_NAME, watcher);
        mDevice.runWatchers();

        assertTrue(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertTrue(mDevice.hasAnyWatcherTriggered());
        mDevice.resetWatcherTriggers();
        assertFalse(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertFalse(mDevice.hasAnyWatcherTriggered());
    }

    @Test
    public void testRemoveUiWatcher() {
        UiWatcher watcher = () -> true;
        mDevice.registerWatcher(WATCHER_NAME, watcher);
        mDevice.removeWatcher(WATCHER_NAME);
        mDevice.runWatchers();

        assertFalse(mDevice.hasWatcherTriggered(WATCHER_NAME));
        assertFalse(mDevice.hasAnyWatcherTriggered());
    }

    @Test
    public void testFreezeAndUnfreezeRotation() throws Exception {
        ContentResolver resolver = ApplicationProvider.getApplicationContext().getContentResolver();

        mDevice.freezeRotation();
        // The value of `ACCELEROMETER_ROTATION` will be 0 if the accelerometer is NOT used for
        // detecting rotation, and 1 otherwise.
        assertEquals(0,
                Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0));

        mDevice.unfreezeRotation();
        assertEquals(1,
                Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0));
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void testGetUiAutomation_withoutFlags() {
        mDevice.getUiAutomation();
        // Verify that the UiAutomation instance was obtained without flags (prior to N).
        verify(mInstrumentation, atLeastOnce()).getUiAutomation();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void testGetUiAutomation_withDefaultFlags() {
        mDevice.getUiAutomation();
        // Verify that the UiAutomation instance was obtained with default flags (N+).
        verify(mInstrumentation, atLeastOnce()).getUiAutomation(eq(mDefaultFlags));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void testGetUiAutomation_withCustomFlags() {
        int customFlags = 5;
        Configurator.getInstance().setUiAutomationFlags(customFlags);
        mDevice.getUiAutomation();
        // Verify that the UiAutomation instance was obtained with custom flags (N+).
        verify(mInstrumentation, atLeastOnce()).getUiAutomation(eq(customFlags));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    public void testExecuteShellCommand() throws IOException {
        String output = mDevice.executeShellCommand("pm list packages");
        assertTrue(output.contains("package:androidx.test.uiautomator.test"));
    }

    @Test
    public void testTakeScreenshot() throws Exception {
        File outFile = mTmpDir.newFile();
        assertTrue(mDevice.takeScreenshot(outFile));
        // Verify that a valid screenshot was generated with default scale.
        Bitmap screenshot = BitmapFactory.decodeFile(outFile.getPath());
        assertNotNull(screenshot);
        assertEquals(mDevice.getDisplayWidth(), screenshot.getWidth());
        assertEquals(mDevice.getDisplayHeight(), screenshot.getHeight());
    }

    @Test
    public void testTakeScreenshot_scaled() throws Exception {
        File outFile = mTmpDir.newFile();
        assertTrue(mDevice.takeScreenshot(outFile, 0.5f, 100));
        // Verify that a valid screenshot was generated with 1/2 scale.
        Bitmap screenshot = BitmapFactory.decodeFile(outFile.getPath());
        assertNotNull(screenshot);
        assertEquals(mDevice.getDisplayWidth() / 2, screenshot.getWidth());
        assertEquals(mDevice.getDisplayHeight() / 2, screenshot.getHeight());
    }
}
