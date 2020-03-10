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

import static androidx.window.ExtensionWindowBackend.initAndVerifyExtension;
import static androidx.window.Version.VERSION_0_1;
import static androidx.window.Version.VERSION_1_0;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionDisplayFeature;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for the extension implementation on the device. */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class ExtensionTest extends WindowTestBase {

    private Context mContext;
    private ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);
    private ActivityTestRule<TestConfigChangeHandlingActivity> mConfigHandlingActivityTestRule =
            new ActivityTestRule<>(TestConfigChangeHandlingActivity.class, false, true);

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testExtensionInterface() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);
        assertTrue(extension.validateExtensionInterface());
    }

    @Test
    public void testGetDeviceState() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);
        DeviceState deviceState = extension.getDeviceState();

        assertThat(deviceState.getPosture()).isIn(Range.range(
                ExtensionDeviceState.POSTURE_UNKNOWN, BoundType.CLOSED,
                ExtensionDeviceState.POSTURE_FLIPPED, BoundType.CLOSED));
    }

    @Test
    public void testRegisterDeviceStateChangeListener() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);

        extension.onDeviceStateListenersChanged(false);
        extension.onDeviceStateListenersChanged(true);
    }

    @Test
    public void testDisplayFeatureDataClass() {
        assumeExtensionV10_V01();

        Rect rect = new Rect(1, 2, 3, 4);
        int type = 1;
        ExtensionDisplayFeature displayFeature = new ExtensionDisplayFeature(rect, type);
        assertEquals(rect, displayFeature.getBounds());
        assertEquals(type, displayFeature.getType());
    }

    @Test
    public void testGetWindowLayoutInfo() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        assertTrue("Layout must happen after launch", activity.waitForLayout());
        WindowLayoutInfo windowLayoutInfo = extension.getWindowLayoutInfo(windowToken);
        if (windowLayoutInfo.getDisplayFeatures().isEmpty()) {
            return;
        }

        for (DisplayFeature displayFeature : windowLayoutInfo.getDisplayFeatures()) {
            int featureType = displayFeature.getType();
            assertThat(featureType).isAtLeast(ExtensionDisplayFeature.TYPE_FOLD);
            assertThat(featureType).isAtMost(ExtensionDisplayFeature.TYPE_HINGE);

            Rect featureRect = displayFeature.getBounds();
            assertFalse(featureRect.width() == 0 && featureRect.height() == 0);
            assertThat(featureRect.left).isAtLeast(0);
            assertThat(featureRect.top).isAtLeast(0);
            assertThat(featureRect.right).isAtLeast(1);
            assertThat(featureRect.right).isAtMost(activity.getWidth());
            assertThat(featureRect.bottom).isAtLeast(1);
            assertThat(featureRect.bottom).isAtMost(activity.getHeight());
        }
    }

    @Test
    public void testRegisterWindowLayoutChangeListener() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        extension.onWindowLayoutChangeListenerAdded(windowToken);
        extension.onWindowLayoutChangeListenerRemoved(windowToken);
    }

    @Test
    public void testWindowLayoutUpdatesOnConfigChange() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);

        TestConfigChangeHandlingActivity activity =
                mConfigHandlingActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity.waitForLayout();
        WindowLayoutInfo portraitWindowLayoutInfo = extension.getWindowLayoutInfo(windowToken);
        if (portraitWindowLayoutInfo.getDisplayFeatures().isEmpty()) {
            // No display feature to compare, finish test early
            return;
        }

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        assertTrue("Layout must happen after orientation change", activity.waitForLayout());
        WindowLayoutInfo landscapeWindowLayoutInfo =
                extension.getWindowLayoutInfo(windowToken);

        assertNotEquals(portraitWindowLayoutInfo, landscapeWindowLayoutInfo);
    }

    @Test
    public void testWindowLayoutUpdatesOnRecreate() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity = mActivityTestRule.getActivity();
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        activity.waitForLayout();
        WindowLayoutInfo portraitWindowLayoutInfo = extension.getWindowLayoutInfo(windowToken);
        if (portraitWindowLayoutInfo.getDisplayFeatures().isEmpty()) {
            // No display feature to compare, finish test early
            return;
        }

        TestActivity.resetResumeCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        TestActivity.waitForOnResume();

        activity = mActivityTestRule.getActivity();
        windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        activity.waitForLayout();
        WindowLayoutInfo landscapeWindowLayoutInfo = extension.getWindowLayoutInfo(windowToken);

        assertNotEquals(portraitWindowLayoutInfo, landscapeWindowLayoutInfo);
    }

    @Test
    public void testVersionSupport() {
        // Only versions 1.0 and 0.1 are supported for now
        Version version = SidecarCompat.getSidecarVersion();
        if (version != null) {
            assertEquals(VERSION_0_1, version);
        }
        version = ExtensionCompat.getExtensionVersion();
        if (version != null) {
            assertEquals(VERSION_1_0, version);
        }
    }

    private void assumeExtensionV10_V01() {
        assumeTrue(VERSION_1_0.equals(ExtensionCompat.getExtensionVersion())
                || VERSION_0_1.equals(SidecarCompat.getSidecarVersion()));
    }
}
