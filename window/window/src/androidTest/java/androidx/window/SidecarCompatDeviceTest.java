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

import static androidx.window.Version.VERSION_0_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link SidecarCompat} implementation of {@link ExtensionInterfaceCompat} that are
 * executed with Sidecar implementation provided on the device (and only if one is available).
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class SidecarCompatDeviceTest extends WindowTestBase implements CompatDeviceTestInterface {
    SidecarCompat mSidecarCompat;

    @Before
    public void setUp() {
        assumeExtensionV01();
        mSidecarCompat = new SidecarCompat((Context) ApplicationProvider.getApplicationContext());
    }

    @Test
    @Override
    public void testGetDeviceState() {
        SidecarDeviceState sidecarDeviceState = mSidecarCompat.mSidecar.getDeviceState();
        DeviceState deviceState = mSidecarCompat.getDeviceState();
        assertEquals(sidecarDeviceState.posture, deviceState.getPosture());
    }

    @Test
    @Override
    public void testGetWindowLayout() {
        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        SidecarWindowLayoutInfo sidecarWindowLayoutInfo =
                mSidecarCompat.mSidecar.getWindowLayoutInfo(windowToken);
        WindowLayoutInfo windowLayoutInfo = mSidecarCompat.getWindowLayoutInfo(windowToken);

        for (int i = 0; i < windowLayoutInfo.getDisplayFeatures().size(); i++) {
            DisplayFeature feature = windowLayoutInfo.getDisplayFeatures().get(i);
            SidecarDisplayFeature sidecarDisplayFeature =
                    sidecarWindowLayoutInfo.displayFeatures.get(i);

            assertEquals(feature.getType(), sidecarDisplayFeature.getType());
            assertEquals(feature.getBounds(), sidecarDisplayFeature.getRect());
        }
    }

    private void assumeExtensionV01() {
        Version sidecarVersion = SidecarCompat.getSidecarVersion();
        assumeTrue(VERSION_0_1.equals(sidecarVersion));
    }
}
