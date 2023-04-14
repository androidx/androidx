/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.app;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Helper for accessing features in {@link PendingIntent}. */
public final class PendingIntentCompat {

    @IntDef(
            flag = true,
            value = {
                PendingIntent.FLAG_ONE_SHOT,
                PendingIntent.FLAG_NO_CREATE,
                PendingIntent.FLAG_CANCEL_CURRENT,
                PendingIntent.FLAG_UPDATE_CURRENT,
                Intent.FILL_IN_ACTION,
                Intent.FILL_IN_DATA,
                Intent.FILL_IN_CATEGORIES,
                Intent.FILL_IN_COMPONENT,
                Intent.FILL_IN_PACKAGE,
                Intent.FILL_IN_SOURCE_BOUNDS,
                Intent.FILL_IN_SELECTOR,
                Intent.FILL_IN_CLIP_DATA
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface Flags {}

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getActivities(Context, int, Intent[], int, Bundle)}.
     */
    public static @NonNull PendingIntent getActivities(
            @NonNull Context context,
            int requestCode,
            @NonNull @SuppressLint("ArrayReturn") Intent[] intents,
            @Flags int flags,
            @NonNull Bundle options,
            boolean isMutable) {
        if (Build.VERSION.SDK_INT >= 16) {
            return Api16Impl.getActivities(
                    context, requestCode, intents, addMutabilityFlags(isMutable, flags), options);
        } else {
            return PendingIntent.getActivities(context, requestCode, intents, flags);
        }
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getActivities(Context, int, Intent[], int, Bundle)}.
     */
    public static @NonNull PendingIntent getActivities(
            @NonNull Context context,
            int requestCode,
            @NonNull @SuppressLint("ArrayReturn") Intent[] intents,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getActivities(
                context, requestCode, intents, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getActivity(Context, int, Intent, int)}.
     */
    public static @NonNull PendingIntent getActivity(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getActivity(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getActivity(Context, int, Intent, int, Bundle)}.
     */
    public static @NonNull PendingIntent getActivity(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            @NonNull Bundle options,
            boolean isMutable) {
        if (Build.VERSION.SDK_INT >= 16) {
            return Api16Impl.getActivity(
                    context, requestCode, intent, addMutabilityFlags(isMutable, flags), options);
        } else {
            return PendingIntent.getActivity(context, requestCode, intent, flags);
        }
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary.
     *
     * @return Returns an existing or new PendingIntent matching the given parameters. May return
     *         {@code null} only if {@link PendingIntent#FLAG_NO_CREATE} has been supplied.
     * @see PendingIntent#getBroadcast(Context, int, Intent, int)
     */
    public static @Nullable PendingIntent getBroadcast(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getBroadcast(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getForegroundService(Context, int, Intent, int)} .
     */
    @RequiresApi(26)
    public static @NonNull PendingIntent getForegroundService(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return Api26Impl.getForegroundService(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getService(Context, int, Intent, int)}.
     */
    public static @NonNull PendingIntent getService(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getService(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    private static int addMutabilityFlags(boolean isMutable, int flags) {
        if (isMutable) {
            if (Build.VERSION.SDK_INT >= 31) {
                flags |= FLAG_MUTABLE;
            }
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                flags |= FLAG_IMMUTABLE;
            }
        }

        return flags;
    }

    private PendingIntentCompat() {}

    @RequiresApi(16)
    private static class Api16Impl {
        private Api16Impl() {}

        @DoNotInline
        public static @NonNull PendingIntent getActivities(
                @NonNull Context context,
                int requestCode,
                @NonNull @SuppressLint("ArrayReturn") Intent[] intents,
                @Flags int flags,
                @NonNull Bundle options) {
            return PendingIntent.getActivities(context, requestCode, intents, flags, options);
        }

        @DoNotInline
        public static @NonNull PendingIntent getActivity(
                @NonNull Context context,
                int requestCode,
                @NonNull Intent intent,
                @Flags int flags,
                @NonNull Bundle options) {
            return PendingIntent.getActivity(context, requestCode, intent, flags, options);
        }
    }

    @RequiresApi(26)
    private static class Api26Impl {
        private Api26Impl() {}

        @DoNotInline
        public static PendingIntent getForegroundService(
                Context context, int requestCode, Intent intent, int flags) {
            return PendingIntent.getForegroundService(context, requestCode, intent, flags);
        }
    }
}
