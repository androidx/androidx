/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.core.net;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for accessing features in {@link ConnectivityManager}.
 */
@SuppressWarnings("unused")
public final class ConnectivityManagerCompat {
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {
            RESTRICT_BACKGROUND_STATUS_DISABLED,
            RESTRICT_BACKGROUND_STATUS_WHITELISTED,
            RESTRICT_BACKGROUND_STATUS_ENABLED,
    })
    public @interface RestrictBackgroundStatus {
    }

    /**
     * Device is not restricting metered network activity while application is running on
     * background.
     */
    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;

    /**
     * Device is restricting metered network activity while application is running on background,
     * but application is allowed to bypass it.
     * <p>
     * In this state, application should take action to mitigate metered network access.
     * For example, a music streaming application should switch to a low-bandwidth bitrate.
     */
    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;

    /**
     * Device is restricting metered network activity while application is running on background.
     * <p>
     * In this state, application should not try to use the network while running on background,
     * because it would be denied.
     */
    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;

    /**
     * Returns if the currently active data network is metered. A network is
     * classified as metered when the user is sensitive to heavy data usage on
     * that connection due to monetary costs, data limitations or
     * battery/performance issues. You should check this before doing large
     * data transfers, and warn the user or delay the operation until another
     * network is available.
     * <p>This method requires the caller to hold the permission
     * {@link android.Manifest.permission#ACCESS_NETWORK_STATE}.
     *
     * @return {@code true} if large transfers should be avoided, otherwise
     *        {@code false}.
     * @deprecated Call {@link ConnectivityManager#isActiveNetworkMetered()} directly.
     */
    @Deprecated
    @androidx.annotation.ReplaceWith(expression = "cm.isActiveNetworkMetered()")
    @SuppressWarnings("deprecation")
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static boolean isActiveNetworkMetered(@NonNull ConnectivityManager cm) {
        return cm.isActiveNetworkMetered();
    }

    /**
     * Return the {@link NetworkInfo} that caused the given
     * {@link ConnectivityManager#CONNECTIVITY_ACTION} broadcast. This obtains
     * the current state from {@link ConnectivityManager} instead of using the
     * potentially-stale value from
     * {@link ConnectivityManager#EXTRA_NETWORK_INFO}. May be {@code null}.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("ReferencesDeprecated")
    @Nullable
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public static NetworkInfo getNetworkInfoFromBroadcast(@NonNull ConnectivityManager cm,
            @NonNull Intent intent) {
        final NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        if (info != null) {
            return cm.getNetworkInfo(info.getType());
        } else {
            return null;
        }
    }

    /**
     * Determines if the calling application is subject to metered network restrictions while
     * running on background.
     *
     * @return {@link #RESTRICT_BACKGROUND_STATUS_DISABLED},
     *         {@link #RESTRICT_BACKGROUND_STATUS_ENABLED},
     *         or {@link #RESTRICT_BACKGROUND_STATUS_WHITELISTED}
     */
    @RestrictBackgroundStatus
    public static int getRestrictBackgroundStatus(@NonNull ConnectivityManager cm) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.getRestrictBackgroundStatus(cm);
        } else {
            return RESTRICT_BACKGROUND_STATUS_ENABLED;
        }
    }

    private ConnectivityManagerCompat() {}

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static int getRestrictBackgroundStatus(ConnectivityManager connectivityManager) {
            return connectivityManager.getRestrictBackgroundStatus();
        }
    }
}
