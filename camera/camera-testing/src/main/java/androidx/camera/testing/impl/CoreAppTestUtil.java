/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl;

import static org.junit.Assume.assumeFalse;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.camera.testing.impl.activity.ForegroundTestActivity;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.AssumptionViolatedException;

/** Utility functions of tests on CoreTestApp. */
public final class CoreAppTestUtil {

    private static final String TAG = "CoreAppTestUtil";

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
    public static <T extends Activity> @Nullable T launchActivity(
            @NonNull Instrumentation instrumentation, @NonNull Class<T> activityClass,
            @Nullable Intent startIntent) {
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

    /**
     * Launch a auto-closed {@link ForegroundTestActivity}.
     *
     * <p>The launched activity will make the callee activity enter PAUSE state and return to
     * RESUME state after the activity is closed.
     *
     * <p>If the <code>countingIdlingResource</code> is specified, the activity will wait for it
     * becoming idle to close the activity. This can be used to control the activity close speed to
     * make it work more like the real user behavior.
     */
    public static void launchAutoClosedForegroundActivity(
            @NonNull Context context,
            @NonNull Instrumentation instrumentation,
            @Nullable CountingIdlingResource countingIdlingResource
    ) {
        ForegroundTestActivity foregroundTestActivity = launchActivity(
                instrumentation,
                ForegroundTestActivity.class,
                new Intent(context, ForegroundTestActivity.class)
        );
        try {
            if (countingIdlingResource != null) {
                IdlingRegistry.getInstance().register(countingIdlingResource);
                Espresso.onIdle();
                IdlingRegistry.getInstance().unregister(countingIdlingResource);
            }
        } finally {
            foregroundTestActivity.finish();
        }
    }
}
