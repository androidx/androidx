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

import static androidx.window.ExtensionInterfaceCompat.ExtensionCallbackInterface;
import static androidx.window.ExtensionWindowBackend.initAndVerifyExtension;
import static androidx.window.Version.VERSION_0_1;
import static androidx.window.Version.VERSION_1_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.window.extensions.ExtensionDeviceState;
import androidx.window.extensions.ExtensionFoldingFeature;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;

import java.util.HashSet;
import java.util.Set;

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
    public void testDeviceStateCallback() {
        assumeExtensionV10_V01();
        final Set<Integer> validValues = new HashSet<>();
        validValues.add(ExtensionDeviceState.POSTURE_FLIPPED);
        validValues.add(ExtensionDeviceState.POSTURE_HALF_OPENED);
        validValues.add(ExtensionDeviceState.POSTURE_OPENED);
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);
        ExtensionCallbackInterface callbackInterface = mock(ExtensionCallbackInterface.class);
        extension.setExtensionCallback(callbackInterface);
        extension.onDeviceStateListenersChanged(false);

        verify(callbackInterface, atLeastOnce()).onDeviceStateChanged(any());
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

        Rect rect = new Rect(0, 100, 100, 100);
        int type = 1;
        int state = 1;
        ExtensionFoldingFeature displayFeature =
                new ExtensionFoldingFeature(rect, type, state);
        assertEquals(rect, displayFeature.getBounds());
    }

    @Test
    public void testWindowLayoutCallback() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);
        ExtensionCallbackInterface callbackInterface = mock(ExtensionCallbackInterface.class);
        extension.setExtensionCallback(callbackInterface);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        extension.onWindowLayoutChangeListenerAdded(activity);

        assertTrue("Layout must happen after launch", activity.waitForLayout());

        verify(callbackInterface, atLeastOnce()).onWindowLayoutChanged(any(), argThat(
                new WindowLayoutInfoValidator(activity)));
    }

    @Test
    public void testRegisterWindowLayoutChangeListener() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());

        extension.onWindowLayoutChangeListenerAdded(activity);
        extension.onWindowLayoutChangeListenerRemoved(activity);
    }

    @Test
    public void testWindowLayoutUpdatesOnConfigChange() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);
        ExtensionCallbackInterface callbackInterface = mock(ExtensionCallbackInterface.class);
        extension.setExtensionCallback(callbackInterface);

        TestConfigChangeHandlingActivity activity =
                mConfigHandlingActivityTestRule.launchActivity(new Intent());
        extension.onWindowLayoutChangeListenerAdded(activity);

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity.waitForLayout();
        if (activity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT) {
            // Orientation change did not occur on this device config. Skipping the test.
            return;
        }

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        boolean layoutHappened = activity.waitForLayout();
        if (activity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) {
            // Orientation change did not occur on this device config. Skipping the test.
            return;
        }
        assertTrue("Layout must happen after orientation change", layoutHappened);

        if (!isSidecar()) {
            verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(any(), argThat(new DistinctWindowLayoutInfoMatcher()));
        } else {
            verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(any(), any());
        }
    }

    @Test
    public void testWindowLayoutUpdatesOnRecreate() {
        assumeExtensionV10_V01();
        ExtensionInterfaceCompat extension = initAndVerifyExtension(mContext);
        ExtensionCallbackInterface callbackInterface = mock(ExtensionCallbackInterface.class);
        extension.setExtensionCallback(callbackInterface);

        TestActivity activity = mActivityTestRule.launchActivity(new Intent());
        extension.onWindowLayoutChangeListenerAdded(activity);

        activity.resetLayoutCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        activity = mActivityTestRule.getActivity();

        activity.waitForLayout();
        if (activity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_PORTRAIT) {
            // Orientation change did not occur on this device config. Skipping the test.
            return;
        }

        TestActivity.resetResumeCounter();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        TestActivity.waitForOnResume();

        activity = mActivityTestRule.getActivity();

        activity.waitForLayout();
        if (activity.getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) {
            // Orientation change did not occur on this device config. Skipping the test.
            return;
        }

        if (!isSidecar()) {
            verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(any(), argThat(new DistinctWindowLayoutInfoMatcher()));
        } else {
            verify(callbackInterface, atLeastOnce())
                    .onWindowLayoutChanged(any(), any());
        }
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

    private boolean isSidecar() {
        return SidecarCompat.getSidecarVersion() != null;
    }

    /**
     * An argument matcher that ensures the arguments used to call are distinct.  The only exception
     * is to allow the first value to trigger twice in case the initial value is pushed and then
     * replayed.
     */
    private static class DistinctWindowLayoutInfoMatcher implements
            ArgumentMatcher<WindowLayoutInfo> {
        private Set<WindowLayoutInfo> mWindowLayoutInfos = new HashSet<>();

        @Override
        public boolean matches(WindowLayoutInfo windowLayoutInfo) {
            if (mWindowLayoutInfos.size() == 1 && mWindowLayoutInfos.contains(windowLayoutInfo)) {
                // First element is emitted twice so it is allowed
                return true;
            } else if (mWindowLayoutInfos.contains(windowLayoutInfo)) {
                return false;
            } else if (windowLayoutInfo.getDisplayFeatures().isEmpty()) {
                return true;
            } else {
                mWindowLayoutInfos.add(windowLayoutInfo);
                return true;
            }
        }
    }

    /**
     * An argument matcher to ensure each {@link WindowLayoutInfo} is valid.
     */
    private static class WindowLayoutInfoValidator implements ArgumentMatcher<WindowLayoutInfo> {
        private final TestActivity mActivity;

        WindowLayoutInfoValidator(TestActivity activity) {
            mActivity = activity;
        }

        @Override
        public boolean matches(WindowLayoutInfo windowLayoutInfo) {
            if (windowLayoutInfo.getDisplayFeatures().isEmpty()) {
                return true;
            }

            for (DisplayFeature displayFeature : windowLayoutInfo.getDisplayFeatures()) {
                if (!isValid(mActivity, displayFeature)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isValid(TestActivity activity, DisplayFeature displayFeature) {
        if (!(displayFeature instanceof FoldingFeature)) {
            return false;
        }
        FoldingFeature feature = (FoldingFeature) displayFeature;

        Rect featureRect = feature.getBounds();
        WindowMetrics windowMetrics = new WindowManager(activity).getCurrentWindowMetrics();

        if ((featureRect.height() == 0 && featureRect.width() == 0) || featureRect.left < 0
                || featureRect.top < 0) {
            return false;
        }
        if (featureRect.right < 1 || featureRect.right > windowMetrics.getBounds().width()) {
            return false;
        }
        if (featureRect.bottom < 1 || featureRect.bottom > windowMetrics.getBounds().height()) {
            return false;
        }
        return true;
    }
}
