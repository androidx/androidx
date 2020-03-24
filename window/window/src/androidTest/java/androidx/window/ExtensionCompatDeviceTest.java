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

import static androidx.window.Version.VERSION_1_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;
import androidx.window.extensions.ExtensionWindowLayoutInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ExtensionCompat} implementation of {@link ExtensionInterfaceCompat} that are
 * executed with Extension implementation provided on the device (and only if one is available).
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ExtensionCompatDeviceTest extends WindowTestBase implements CompatDeviceTestInterface {
    ExtensionCompat mExtensionCompat;

    @Before
    public void setUp() {
        assumeExtensionV1_0();
        mExtensionCompat =
                new ExtensionCompat((Context) ApplicationProvider.getApplicationContext());
    }

    @Test
    @Override
    public void testGetDeviceState() {
        ExtensionDeviceState extensionDeviceState =
                mExtensionCompat.mWindowExtension.getDeviceState();
        DeviceState deviceState = mExtensionCompat.getDeviceState();
        assertEquals(extensionDeviceState.getPosture(), deviceState.getPosture());
    }

    @Test
    @Override
    public void testGetWindowLayout() {
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        ExtensionWindowLayoutInfo extensionWindowLayoutInfo =
                mExtensionCompat.mWindowExtension.getWindowLayoutInfo(windowToken);
        WindowLayoutInfo windowLayoutInfo = mExtensionCompat.getWindowLayoutInfo(windowToken);

        for (int i = 0; i < windowLayoutInfo.getDisplayFeatures().size(); i++) {
            DisplayFeature feature = windowLayoutInfo.getDisplayFeatures().get(i);
            ExtensionDisplayFeature sidecarDisplayFeature =
                    extensionWindowLayoutInfo.getDisplayFeatures().get(i);

            assertEquals(feature.getType(), sidecarDisplayFeature.getType());
            assertEquals(feature.getBounds(), sidecarDisplayFeature.getBounds());
        }
    }

    private void assumeExtensionV1_0() {
        Version extensionVersion = ExtensionCompat.getExtensionVersion();
        assumeTrue(extensionVersion != null && VERSION_1_0.compareTo(extensionVersion) <= 0);
    }
}
