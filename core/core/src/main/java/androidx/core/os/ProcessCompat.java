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

package androidx.core.os;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link Process}.
 */
public final class ProcessCompat {
    private ProcessCompat() {
        // This class is non-instantiable.
    }

    /**
     * Returns whether the given {@code uid} belongs to an application.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 24 and above, this method matches platform behavior.
     * <li>SDK 23 and earlier, this method is a best-effort to match platform behavior, but may
     * default to returning {@code true} if an accurate result is not available.
     * </ul>
     *
     * @param uid a kernel uid
     * @return {@code true} if the uid corresponds to an application sandbox running in a
     * specific user, {@code false} if the uid corresponds to an isolated user ID process or
     * does not otherwise correspond to an application user ID, or a value based on
     * platform-specific fallback behavior
     */
    public static boolean isApplicationUid(int uid) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.isApplicationUid(uid);
        } else {
            return Api19Impl.isApplicationUid(uid);
        }
    }

    /**
     * Returns whether the given {@code uid} belongs to an isolated process.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 28 and above, this method matches platform behavior.
     * <li>SDK 27 and earlier, this method checks if the uid is in the isolated UID range.
     * </ul>
     *
     * @param uid a kernel uid
     * @return {@code true} if the uid corresponds to an isolated process, {@code false} otherwise
     */
    public static boolean isIsolatedUid(int uid) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.isIsolatedUid(uid);
        } else {
            int appId = uid % 100000;
            return appId >= 99000 && appId <= 99999;
        }
    }

    /**
     * Returns whether the given {@code uid} belongs to an SDK sandbox process.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 33 and above, this method matches platform behavior.
     * <li>SDK 32 and earlier, returns {@code false} as SDK sandbox processes did not exist.
     * </ul>
     *
     * @param uid a kernel uid
     * @return {@code true} if the uid corresponds to an SDK sandbox process, {@code false} otherwise
     */
    public static boolean isSdkSandboxUid(int uid) {
        if (Build.VERSION.SDK_INT >= 33) {
            return Api33Impl.isSdkSandboxUid(uid);
        } else {
            return false;
        }
    }

    @RequiresApi(33)
    static class Api33Impl {
        private Api33Impl() {
            // This class is non-instantiable.
        }

        static boolean isSdkSandboxUid(int uid) {
            return Process.isSdkSandboxUid(uid);
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is non-instantiable.
        }

        static boolean isIsolatedUid(int uid) {
            return Process.isIsolatedUid(uid);
        }
    }

    @RequiresApi(24)
    static class Api24Impl {

        private Api24Impl() {
            // This class is non-instantiable.
        }

        static boolean isApplicationUid(int uid) {
            // In N, the method was made public on android.os.Process.
            return Process.isApplicationUid(uid);
        }
    }

    static class Api19Impl {
        private static final Object sResolvedLock = new Object();

        private static Method sMethodUserHandleIsAppMethod;
        private static boolean sResolved;

        private Api19Impl() {
            // This class is non-instantiable.
        }

        @SuppressWarnings({"JavaReflectionMemberAccess", "CatchAndPrintStackTrace"})
        @SuppressLint("DiscouragedPrivateApi")
        static boolean isApplicationUid(int uid) {
            // Prior to API 24, the equivalent isApp(int) hidden method moved to public class
            // android.os.UserHandle.
            try {
                synchronized (sResolvedLock) {
                    if (!sResolved) {
                        sResolved = true;
                        sMethodUserHandleIsAppMethod = UserHandle.class.getDeclaredMethod("isApp",
                                int.class);
                    }
                }
                if (sMethodUserHandleIsAppMethod != null) {
                    Boolean result = (Boolean) sMethodUserHandleIsAppMethod.invoke(null, uid);
                    if (result == null) {
                        // This should never happen, as the method returns a boolean primitive.
                        throw new NullPointerException();
                    }
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }
}
