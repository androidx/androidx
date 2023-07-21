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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.testing.activity.ForegroundTestActivity;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.AssumptionViolatedException;

import java.io.IOException;

/** Utility functions of tests on CoreTestApp. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CoreAppTestUtil {

    /** ADB shell input key code for dismissing keyguard for device with API level <= 22. */
    private static final int DISMISS_LOCK_SCREEN_CODE = 82;
    /** ADB shell command for dismissing keyguard for device with API level >= 23. */
    private static final String ADB_SHELL_DISMISS_KEYGUARD_API23_AND_ABOVE = "wm dismiss-keyguard";
    /** ADB shell command to set the screen always on when usb is connected. */
    private static final String ADB_SHELL_SCREEN_ALWAYS_ON = "svc power stayon true";

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

        assumeFalse("See b/152082918, Wembley Api30 has a libjpeg issue which causes"
                        + " the test failure.",
                Build.MODEL.equalsIgnoreCase("wembley") && Build.VERSION.SDK_INT <= 30);
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
     * Throws the Exception for devices whose front camera is not testable.
     *
     * Vivo 1805 will popup dialog to ask permission for accessing its front camera and then break
     * the test. This function is used for switching camera tests that the tests should be
     * skipped no matter the tests are started from the back or front camera.
     */
    public static void assumeCanTestFrontCamera() throws CameraAccessException {
        if ("vivo 1805".equals(Build.MODEL)) {
            throw new AssumptionViolatedException("Vivo 1805 will popup dialog to ask permission "
                    + "to access the front camera.");
        }
    }

    /**
     * Throws the Exception for devices which the specified camera id is front camera and is not
     * testable.
     *
     * Vivo 1805 will popup dialog to ask permission for accessing its front camera and then break
     * the test.
     */
    public static void assumeNotUntestableFrontCamera(@NonNull String cameraId)
            throws CameraAccessException {
        CameraCharacteristics characteristics =
                CameraUtil.getCameraManager().getCameraCharacteristics(cameraId);
        if ("vivo 1805".equals(Build.MODEL) && characteristics.get(
                CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            throw new AssumptionViolatedException("Vivo 1805 will popup dialog to ask permission "
                    + "to access the front camera.");
        }
    }

    /**
     * Clean up the device UI and back to the home screen for test.
     * @param instrumentation the instrumentation used to run the test
     */
    @SuppressLint("MissingPermission") // Permission needed for action_close_system_dialogs in S
    @SuppressWarnings("deprecation")
    public static void clearDeviceUI(@NonNull Instrumentation instrumentation) {
        UiDevice device = UiDevice.getInstance(instrumentation);
        // On some devices, its necessary to wake up the device before attempting unlock, otherwise
        // unlock attempt will not unlock.
        try {
            device.wakeUp();
        } catch (RemoteException remoteException) {
        }

        // In case the lock screen on top, the action to dismiss it.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            device.pressKeyCode(DISMISS_LOCK_SCREEN_CODE);
        } else {
            try {
                device.executeShellCommand(ADB_SHELL_DISMISS_KEYGUARD_API23_AND_ABOVE);
            } catch (IOException e) {
            }
        }

        try {
            device.executeShellCommand(ADB_SHELL_SCREEN_ALWAYS_ON);
        } catch (IOException e) {
        }

        device.pressHome();
        device.waitForIdle(MAX_TIMEOUT_MS);

        // Close system dialogs first to avoid interrupt.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            instrumentation.getTargetContext().sendBroadcast(
                    new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
    }

    /**
     * Checks whether keyguard is locked.
     *
     * Keyguard is locked if the screen is off or the device is currently locked and requires a
     * PIN, pattern or password to unlock.
     *
     * @throws IllegalStateException if keyguard is locked.
     */
    public static void checkKeyguard(@NonNull Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);

        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            throw new IllegalStateException("<KEYGUARD_STATE_ERROR> Keyguard is locked!");
        }
    }

    /**
     * Try to clear the UI and then check if there is any dialog or lock screen on the top of the
     * window that might cause the activity related test fail.
     *
     * @param instrumentation The instrumentation instance.
     * @throws ForegroundOccupiedError throw the exception when the test app cannot get
     *                                 foreground of the device window in CameraX lab.
     */
    public static void prepareDeviceUI(@NonNull Instrumentation instrumentation)
            throws ForegroundOccupiedError {
        clearDeviceUI(instrumentation);

        ForegroundTestActivity activityRef = null;
        try {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

            Intent startIntent = new Intent(Intent.ACTION_MAIN);
            startIntent.setClassName(context.getPackageName(),
                    ForegroundTestActivity.class.getName());
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            activityRef = ForegroundTestActivity.class.cast(
                    instrumentation.startActivitySync(startIntent));
            instrumentation.waitForIdleSync();

            if (activityRef == null) {
                Logger.d("CoreAppTestUtil", String.format("Activity %s, failed to launch",
                        startIntent.getComponent()) + ", ignore the foreground checking");
                return;
            }
            IdlingRegistry.getInstance().register(activityRef.getViewReadyIdlingResource());

            // The {@link Espresso#onIdle()} throws timeout exception if the
            // ForegroundTestActivity cannot get focus. The default timeout in espresso is 26 sec.
            Espresso.onIdle();
            return;
        } catch (Exception e) {
            Logger.d("CoreAppTestUtil", "Fail to get foreground", e);
        } finally {
            if (activityRef != null) {
                IdlingRegistry.getInstance().unregister(activityRef.getViewReadyIdlingResource());
                final Activity act = activityRef;
                instrumentation.runOnMainSync(() -> act.finish());
                instrumentation.waitForIdleSync();
            }
        }

        // Throw AssumptionViolatedException to skip the test if not in the CameraX lab
        // environment. The loggable tag will be set when running the CameraX daily testing.
        assumeTrue(Log.isLoggable("MH", Log.DEBUG));

        throw new ForegroundOccupiedError("CameraX_fail_to_start_foreground, model:" + Build.MODEL);
    }

    /** The display foreground of the device is occupied that cannot execute UI related test. */
    public static class ForegroundOccupiedError extends Exception {
        public ForegroundOccupiedError(@NonNull String message) {
            super(message);
        }
    }

    /**
     * Launch activity and return the activity instance for testing.
     *
     * @param instrumentation The instrumentation used to run the test
     * @param activityClass   The activity under test. This must be a class in the instrumentation
     *                        targetPackage specified in the AndroidManifest.xml
     * @param startIntent     The Intent that will be used to start the Activity under test. If
     *                        {@code startIntent} is null, a default launch Intent for the
     *                        {@code activityClass} is used.
     * @param <T>             The Activity class under test
     * @return Returns the reference to the activity for test.
     */
    @Nullable
    public static <T extends Activity> T launchActivity(@NonNull Instrumentation instrumentation,
            @NonNull Class<T> activityClass, @Nullable Intent startIntent) {
        Context context = instrumentation.getTargetContext();

        // inject custom intent, if provided
        if (null == startIntent) {
            startIntent = new Intent(Intent.ACTION_MAIN);
        }

        // Set target component if not set Intent
        if (null == startIntent.getComponent()) {
            startIntent.setClassName(context.getPackageName(), activityClass.getName());
        }

        // Set launch flags where if not set Intent
        if (0 /* No flags set */ == startIntent.getFlags()) {
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        T activityRef = activityClass.cast(instrumentation.startActivitySync(startIntent));
        instrumentation.waitForIdleSync();

        return activityRef;
    }
}
