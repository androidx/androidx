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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class UiDeviceTests {

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
        String output = mDevice.executeShellCommand("echo hello world");
        assertEquals("hello world\n", output);
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
