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
import static androidx.window.Version.VERSION_1_0;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarDisplayFeature;
import androidx.window.sidecar.SidecarInterface;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SidecarTest {

    private Context mContext;
    private ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);
    private ActivityTestRule<TestConfigChangeHandlingActivity> mConfigHandlingActivityTestRule =
            new ActivityTestRule<>(TestConfigChangeHandlingActivity.class, false, true);

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testSidecarInterface() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);
        assertTrue(SidecarHelper.validateSidecarInterface(sidecar,
                SidecarHelper.getSidecarVersion()));
    }

    @Test
    public void testGetDeviceState() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);
        SidecarDeviceState sidecarDeviceState = sidecar.getDeviceState();
        DeviceState deviceState = SidecarHelper.deviceStateFromSidecar(sidecarDeviceState);

        assertThat(deviceState.getPosture()).isAtLeast(SidecarDeviceState.POSTURE_UNKNOWN);
        assertThat(deviceState.getPosture()).isAtMost(SidecarDeviceState.POSTURE_FLIPPED);
    }

    @Test
    public void testRegisterDeviceStateChangeListener() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);

        sidecar.onDeviceStateListenersChanged(false);
        sidecar.onDeviceStateListenersChanged(true);
    }

    @Test
    public void testDisplayFeatureDataClass() throws Exception {
        assumeSidecarV10_V01();

        Rect rect = new Rect(1, 2, 3, 4);
        int type = 1;
        SidecarDisplayFeature displayFeature = SidecarHelper.versionCompat()
                .newSidecarDisplayFeature(rect, type);
        assertEquals(rect, SidecarHelper.versionCompat().getFeatureBounds(displayFeature));
        assertEquals(type, displayFeature.getType());
    }

    @Test
    public void testGetWindowLayoutInfo() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        assertTrue("Layout must happen after launch", activity.waitForLayout());
        SidecarWindowLayoutInfo sidecarWindowLayoutInfo = sidecar.getWindowLayoutInfo(windowToken);
        WindowLayoutInfo windowLayoutInfo =
                SidecarHelper.windowLayoutInfoFromSidecar(sidecarWindowLayoutInfo);
        if (windowLayoutInfo.getDisplayFeatures().isEmpty()) {
            return;
        }

        for (DisplayFeature displayFeature : windowLayoutInfo.getDisplayFeatures()) {
            int featureType = displayFeature.getType();
            assertThat(featureType).isAtLeast(SidecarDisplayFeature.TYPE_FOLD);
            assertThat(featureType).isAtMost(SidecarDisplayFeature.TYPE_HINGE);

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
    public void testRegisterWindowLayoutChangeListener() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        sidecar.onWindowLayoutChangeListenerAdded(windowToken);
        sidecar.onWindowLayoutChangeListenerRemoved(windowToken);
    }

    @Test
    public void testWindowLayoutUpdatesOnConfigChange() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);

        TestConfigChangeHandlingActivity activity =
                mConfigHandlingActivityTestRule.launchActivity(new Intent());
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity.waitForLayout();
        SidecarWindowLayoutInfo portraitWindowLayoutInfo = sidecar.getWindowLayoutInfo(windowToken);
        WindowLayoutInfo windowLayoutInfo =
                SidecarHelper.windowLayoutInfoFromSidecar(portraitWindowLayoutInfo);
        if (windowLayoutInfo.getDisplayFeatures().isEmpty()) {
            // No display feature to compare, finish test early
            return;
        }

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        assertTrue("Layout must happen after orientation change", activity.waitForLayout());
        SidecarWindowLayoutInfo landscapeWindowLayoutInfo =
                sidecar.getWindowLayoutInfo(windowToken);

        assertNotEquals(portraitWindowLayoutInfo, landscapeWindowLayoutInfo);
    }

    @Test
    public void testWindowLayoutUpdatesOnRecreate() throws Exception {
        assumeSidecarV10_V01();
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity = mActivityTestRule.getActivity();
        IBinder windowToken = getActivityWindowToken(activity);
        assertNotNull(windowToken);

        activity.waitForLayout();
        SidecarWindowLayoutInfo portraitWindowLayoutInfo = sidecar.getWindowLayoutInfo(windowToken);
        WindowLayoutInfo windowLayoutInfo =
                SidecarHelper.windowLayoutInfoFromSidecar(portraitWindowLayoutInfo);
        if (windowLayoutInfo.getDisplayFeatures().isEmpty()) {
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
        SidecarWindowLayoutInfo landscapeWindowLayoutInfo =
                sidecar.getWindowLayoutInfo(windowToken);

        assertNotEquals(portraitWindowLayoutInfo, landscapeWindowLayoutInfo);
    }

    @Test
    public void testVersionSupport() throws Exception {
        Version version = SidecarHelper.getSidecarVersion();
        assumeThat(Version.UNKNOWN, not(equalTo(version)));
        // Only versions 1.0 and 0.1 are supported for now
        assertTrue(VERSION_1_0.equals(version) || VERSION_0_1.equals(version));
    }

    private void assumeSidecarV10_V01() {
        Version version = SidecarHelper.getSidecarVersion();
        assumeTrue(VERSION_1_0.equals(version) || VERSION_0_1.equals(version));
        SidecarInterface sidecar = SidecarHelper.getSidecarImpl(mContext);
        assumeNotNull(sidecar);
    }

    private IBinder getActivityWindowToken(Activity activity) {
        return activity.getWindow().getAttributes().token;
    }
}
