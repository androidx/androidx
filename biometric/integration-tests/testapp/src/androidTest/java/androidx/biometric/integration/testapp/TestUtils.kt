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

package androidx.biometric.integration.testapp

import android.app.Activity
import android.app.Instrumentation
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * The maximum time that [UiDevice.waitForIdle] should wait for the device to become idle.
 */
private const val IDLE_TIMEOUT_MS = 3000L

/**
 * The maximum time that [changeOrientation] should wait for the device to finish rotating.
 */
private const val ROTATE_TIMEOUT_MS = 2000L

/**
 * Brings the given [activity] from the background to the foreground.
 */
internal fun bringToForeground(activity: Activity) {
    val intent = Intent(activity, activity.javaClass)
    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    activity.startActivity(intent)
}

/**
 * Changes the [device] to [landscape] or portrait orientation and waits for rotation to finish.
 */
internal fun changeOrientation(activity: Activity, device: UiDevice, landscape: Boolean) {
    // Create a monitor to wait for the activity to be recreated.
    val monitor = Instrumentation.ActivityMonitor(
        activity.javaClass.name,
        null /* result */,
        false /* block */
    )
    InstrumentationRegistry.getInstrumentation().addMonitor(monitor)

    if (landscape) {
        device.setOrientationLeft()
    } else {
        device.setOrientationNatural()
    }

    // Wait for the rotation to complete.
    InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(
        monitor,
        ROTATE_TIMEOUT_MS
    )
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

/**
 * Checks [context] to determine if the device has an enrolled biometric authentication method.
 */
internal fun hasEnrolledBiometric(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    return biometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS
}

/**
 * Checks [context] to determine if the device is currently locked.
 */
internal fun isDeviceLocked(context: Context): Boolean {
    val keyguard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        context.getSystemService(KeyguardManager::class.java)
    else
        context.getSystemService(Context::KEYGUARD_SERVICE.toString()) as KeyguardManager?

    return when {
        keyguard == null -> false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 -> keyguard.isDeviceLocked
        else -> keyguard.isKeyguardLocked
    }
}

/**
 * Presses the system home button and waits for the [device] to become idle.
 */
internal fun navigateToHomeScreen(device: UiDevice) {
    device.pressHome()
    device.waitForIdle(IDLE_TIMEOUT_MS)
}
