/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.core.os;

import android.content.Context;
import android.os.Build;
import android.os.UserManager;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper for accessing features in {@link android.os.UserManager} in a backwards compatible
 * fashion.
 */
public class UserManagerCompat {

    private UserManagerCompat() {
    }

    /**
     * Return whether the calling user is running in an "unlocked" state. A user
     * is unlocked only after they've entered their credentials (such as a lock
     * pattern or PIN), and credential-encrypted private app data storage is
     * available.
     */
    public static boolean isUserUnlocked(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.isUserUnlocked(context);
        } else {
            return true;
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isUserUnlocked(Context context) {
            return context.getSystemService(UserManager.class).isUserUnlocked();
        }
    }
}
