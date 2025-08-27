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

package androidx.biometric.utils;

import android.app.KeyguardManager;
import android.content.Context;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Utilities related to the {@link KeyguardManager} system service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class KeyguardUtils {
    // Prevent instantiation.
    private KeyguardUtils() {
    }

    /**
     * Gets an instance of the {@link KeyguardManager} system service.
     *
     * @param context The application or activity context.
     * @return An instance of {@link KeyguardManager}.
     */
    public static @Nullable KeyguardManager getKeyguardManager(@NonNull Context context) {
        return context.getSystemService(KeyguardManager.class);
    }

    /**
     * Checks if the user has set up a secure PIN, pattern, or password for the device.
     *
     * @param context The application or activity context.
     * @return Whether a PIN/pattern/password has been set, or {@code false} if unsure.
     */
    public static boolean isDeviceSecuredWithCredential(@NonNull Context context) {
        final KeyguardManager keyguardManager = getKeyguardManager(context);
        if (keyguardManager == null) {
            return false;
        }
        return keyguardManager.isDeviceSecure();
    }
}
