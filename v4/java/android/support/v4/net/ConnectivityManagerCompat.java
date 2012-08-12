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

package android.support.v4.net;

import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

/**
 * Helper for accessing features in {@link ConnectivityManager} introduced after
 * API level 16 in a backwards compatible fashion.
 */
public class ConnectivityManagerCompat {

    interface ConnectivityManagerCompatImpl {
        boolean isActiveNetworkMetered(ConnectivityManager cm);
    }

    static class BaseConnectivityManagerCompatImpl implements ConnectivityManagerCompatImpl {
        @Override
        public boolean isActiveNetworkMetered(ConnectivityManager cm) {
            final NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null) {
                // err on side of caution
                return true;
            }

            final int type = info.getType();
            switch (type) {
                case TYPE_MOBILE:
                    return true;
                case TYPE_WIFI:
                    return false;
                default:
                    // err on side of caution
                    return true;
            }
        }
    }

    static class GingerbreadConnectivityManagerCompatImpl implements ConnectivityManagerCompatImpl {
        @Override
        public boolean isActiveNetworkMetered(ConnectivityManager cm) {
            return ConnectivityManagerCompatGingerbread.isActiveNetworkMetered(cm);
        }
    }

    static class HoneycombMR2ConnectivityManagerCompatImpl
            implements ConnectivityManagerCompatImpl {
        @Override
        public boolean isActiveNetworkMetered(ConnectivityManager cm) {
            return ConnectivityManagerCompatHoneycombMR2.isActiveNetworkMetered(cm);
        }
    }

    static class JellyBeanConnectivityManagerCompatImpl implements ConnectivityManagerCompatImpl {
        @Override
        public boolean isActiveNetworkMetered(ConnectivityManager cm) {
            return ConnectivityManagerCompatJellyBean.isActiveNetworkMetered(cm);
        }
    }

    private static final ConnectivityManagerCompatImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new JellyBeanConnectivityManagerCompatImpl();
        } else if (Build.VERSION.SDK_INT >= 13) {
            IMPL = new HoneycombMR2ConnectivityManagerCompatImpl();
        } else if (Build.VERSION.SDK_INT >= 8) {
            IMPL = new GingerbreadConnectivityManagerCompatImpl();
        } else {
            IMPL = new BaseConnectivityManagerCompatImpl();
        }
    }

    /**
     * Returns if the currently active data network is metered. A network is
     * classified as metered when the user is sensitive to heavy data usage on
     * that connection. You should check this before doing large data transfers,
     * and warn the user or delay the operation until another network is
     * available.
     */
    public static boolean isActiveNetworkMetered(ConnectivityManager cm) {
        return IMPL.isActiveNetworkMetered(cm);
    }

    /**
     * Return the {@link NetworkInfo} that caused the given
     * {@link ConnectivityManager#CONNECTIVITY_ACTION} broadcast. This obtains
     * the current state from {@link ConnectivityManager} instead of using the
     * potentially-stale value from
     * {@link ConnectivityManager#EXTRA_NETWORK_INFO}.
     */
    public static NetworkInfo getNetworkInfoFromBroadcast(ConnectivityManager cm, Intent intent) {
        final NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
        return cm.getNetworkInfo(info.getType());
    }
}
