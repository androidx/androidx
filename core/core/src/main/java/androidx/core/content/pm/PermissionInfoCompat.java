/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.content.pm;

import android.annotation.SuppressLint;
import android.content.pm.PermissionInfo;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for accessing features in {@link PermissionInfo}.
 */
public final class PermissionInfoCompat {
    private PermissionInfoCompat() {
    }

    /** @hide */
    @IntDef(value = {
            PermissionInfo.PROTECTION_NORMAL,
            PermissionInfo.PROTECTION_DANGEROUS,
            PermissionInfo.PROTECTION_SIGNATURE,
            PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM,
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Protection {}

    /** @hide */
    @SuppressLint("UniqueConstants") // because _SYSTEM and _PRIVILEGED are aliases.
    @IntDef(flag = true, value = {
            PermissionInfo.PROTECTION_FLAG_PRIVILEGED,
            PermissionInfo.PROTECTION_FLAG_SYSTEM,
            PermissionInfo.PROTECTION_FLAG_DEVELOPMENT,
            PermissionInfo.PROTECTION_FLAG_APPOP,
            PermissionInfo.PROTECTION_FLAG_PRE23,
            PermissionInfo.PROTECTION_FLAG_INSTALLER,
            PermissionInfo.PROTECTION_FLAG_VERIFIER,
            PermissionInfo.PROTECTION_FLAG_PREINSTALLED,
            PermissionInfo.PROTECTION_FLAG_SETUP,
            PermissionInfo.PROTECTION_FLAG_INSTANT,
            PermissionInfo.PROTECTION_FLAG_RUNTIME_ONLY,
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtectionFlags {}

    /**
     * Return the base permission type of a {@link PermissionInfo}.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("WrongConstant") // for "PermissionInfo.PROTECTION_MASK_BASE"
    @Protection
    public static int getProtection(@NonNull PermissionInfo permissionInfo) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getProtection(permissionInfo);
        } else {
            return permissionInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
        }
    }

    /**
     * Return the additional protection flags of a {@link PermissionInfo}.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("WrongConstant") // for "~PermissionInfo.PROTECTION_MASK_BASE"
    @ProtectionFlags
    public static int getProtectionFlags(@NonNull PermissionInfo permissionInfo) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getProtectionFlags(permissionInfo);
        } else {
            return permissionInfo.protectionLevel & ~PermissionInfo.PROTECTION_MASK_BASE;
        }
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static int getProtection(PermissionInfo permissionInfo) {
            return permissionInfo.getProtection();
        }

        @DoNotInline
        static int getProtectionFlags(PermissionInfo permissionInfo) {
            return permissionInfo.getProtectionFlags();
        }
    }
}
