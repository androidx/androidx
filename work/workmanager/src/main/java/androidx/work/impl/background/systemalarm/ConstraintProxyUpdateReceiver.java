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

package androidx.work.impl.background.systemalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.impl.background.systemalarm.ConstraintProxy.BatteryChargingProxy;
import androidx.work.impl.background.systemalarm.ConstraintProxy.BatteryNotLowProxy;
import androidx.work.impl.background.systemalarm.ConstraintProxy.NetworkStateProxy;
import androidx.work.impl.background.systemalarm.ConstraintProxy.StorageNotLowProxy;
import androidx.work.impl.utils.PackageManagerHelper;


/**
 * The {@link BroadcastReceiver} responsible for updating constraint proxies.
 */
public class ConstraintProxyUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "ConstrntProxyUpdtRecvr";

    static final String ACTION = "androidx.work.impl.background.systemalarm.UpdateProxies";

    static final String KEY_BATTERY_NOT_LOW_PROXY_ENABLED = "KEY_BATTERY_NOT_LOW_PROXY_ENABLED";
    static final String KEY_BATTERY_CHARGING_PROXY_ENABLED = "KEY_BATTERY_CHARGING_PROXY_ENABLED";
    static final String KEY_STORAGE_NOT_LOW_PROXY_ENABLED = "KEY_STORAGE_NOT_LOW_PROXY_ENABLED";
    static final String KEY_NETWORK_STATE_PROXY_ENABLED = "KEY_NETWORK_STATE_PROXY_ENABLED";

    /**
     * @param batteryNotLowProxyEnabled   {@code true} if {@link BatteryNotLowProxy needs to be
     *                                    enabled.}
     * @param batteryChargingProxyEnabled {@code true} if {@link BatteryChargingProxy needs to be
     *                                    enabled.}
     * @param storageNotLowProxyEnabled   {@code true} if {@link StorageNotLowProxy needs to be
     *                                    enabled.}
     * @param networkStateProxyEnabled    {@code true} if {@link NetworkStateProxy needs to be
     *                                    enabled.}
     * @return an {@link Intent} with information about the constraint proxies which need to be
     * enabled.
     */
    public static Intent newConstraintProxyUpdateIntent(
            boolean batteryNotLowProxyEnabled,
            boolean batteryChargingProxyEnabled,
            boolean storageNotLowProxyEnabled,
            boolean networkStateProxyEnabled) {

        Intent intent = new Intent(ACTION);
        intent.putExtra(KEY_BATTERY_NOT_LOW_PROXY_ENABLED, batteryNotLowProxyEnabled)
                .putExtra(KEY_BATTERY_CHARGING_PROXY_ENABLED, batteryChargingProxyEnabled)
                .putExtra(KEY_STORAGE_NOT_LOW_PROXY_ENABLED, storageNotLowProxyEnabled)
                .putExtra(KEY_NETWORK_STATE_PROXY_ENABLED, networkStateProxyEnabled);

        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!ACTION.equals(action)) {
            Log.d(TAG, String.format("Ignoring unknown action %s", action));
        } else {
            boolean batteryNotLowProxyEnabled = intent.getBooleanExtra(
                    KEY_BATTERY_NOT_LOW_PROXY_ENABLED, false);
            boolean batteryChargingProxyEnabled = intent.getBooleanExtra(
                    KEY_BATTERY_CHARGING_PROXY_ENABLED, false);
            boolean storageNotLowProxyEnabled = intent.getBooleanExtra(
                    KEY_STORAGE_NOT_LOW_PROXY_ENABLED, false);
            boolean networkStateProxyEnabled = intent.getBooleanExtra(
                    KEY_NETWORK_STATE_PROXY_ENABLED, false);

            Log.d(TAG, String.format("Updating proxies: BatteryNotLowProxy enabled (%s), "
                            + "BatteryChargingProxy enabled (%s), "
                            + "StorageNotLowProxy (%s), "
                            + "NetworkStateProxy enabled (%s)", batteryNotLowProxyEnabled,
                    batteryChargingProxyEnabled, storageNotLowProxyEnabled,
                    networkStateProxyEnabled));

            PackageManagerHelper.setComponentEnabled(context, BatteryNotLowProxy.class,
                    batteryNotLowProxyEnabled);
            PackageManagerHelper.setComponentEnabled(context, BatteryChargingProxy.class,
                    batteryChargingProxyEnabled);
            PackageManagerHelper.setComponentEnabled(context, StorageNotLowProxy.class,
                    storageNotLowProxyEnabled);
            PackageManagerHelper.setComponentEnabled(context, NetworkStateProxy.class,
                    networkStateProxyEnabled);
        }
    }
}
