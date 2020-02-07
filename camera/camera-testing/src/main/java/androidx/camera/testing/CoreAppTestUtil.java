/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.testing;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiDevice;

import org.junit.AssumptionViolatedException;

/** Utility functions of tests on CoreTestApp. */
public final class CoreAppTestUtil {

    private static final int DISMISS_LOCK_SCREEN_CODE = 82;
    private static final int MAX_TIMEOUT_MS = 3000;

    private CoreAppTestUtil() {
    }

    /**
     * Check if this is compatible device for test.
     *
     * <p> Most devices should be compatible except devices with compatible issues.
     *
     */
    public static void assumeCompatibleDevice() {
        // TODO(b/134894604) This will be removed once the issue is fixed.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP
                && Build.MODEL.contains("Nexus 5")) {
            throw new AssumptionViolatedException("Known issue, b/134894604.");
        }
    }

    /**
     * Throws the Exception for the devices which is not compatible to the testing.
     */
    public static void assumeCanTestCameraDisconnect() {
        // TODO(b/141656413) Remove this when the issue is fixed.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M
                && (Build.MODEL.contains("Nexus 5") || Build.MODEL.contains("Pixel C"))) {
            throw new AssumptionViolatedException("Known issue, b/141656413.");
        }
    }

    /**
     * Clean up the device UI and back to the home screen for test.
     * @param instrumentation the instrumentation used to run the test
     */
    public static void clearDeviceUI(@NonNull Instrumentation instrumentation) {
        UiDevice device = UiDevice.getInstance(instrumentation);
        // On some devices, its necessary to wake up the device before attempting unlock, otherwise
        // unlock attempt will not unlock.
        try {
            device.wakeUp();
        } catch (RemoteException remoteException) {
        }
        // In case the lock screen on top, the action to dismiss it.
        device.pressKeyCode(DISMISS_LOCK_SCREEN_CODE);

        device.pressHome();
        device.waitForIdle(MAX_TIMEOUT_MS);

        // Close system dialogs first to avoid interrupt.
        instrumentation.getTargetContext().sendBroadcast(
                new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }
}
