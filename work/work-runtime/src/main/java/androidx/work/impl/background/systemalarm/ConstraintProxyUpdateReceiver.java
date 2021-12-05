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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Logger;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.background.systemalarm.ConstraintProxy.BatteryChargingProxy;
import androidx.work.impl.background.systemalarm.ConstraintProxy.BatteryNotLowProxy;
import androidx.work.impl.background.systemalarm.ConstraintProxy.NetworkStateProxy;
import androidx.work.impl.background.systemalarm.ConstraintProxy.StorageNotLowProxy;
import androidx.work.impl.utils.PackageManagerHelper;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;


/**
 * The {@link BroadcastReceiver} responsible for updating constraint proxies.
 */
public class ConstraintProxyUpdateReceiver extends BroadcastReceiver {
    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("ConstrntProxyUpdtRecvr");
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
            Context context,
            boolean batteryNotLowProxyEnabled,
            boolean batteryChargingProxyEnabled,
            boolean storageNotLowProxyEnabled,
            boolean networkStateProxyEnabled) {

        Intent intent = new Intent(ACTION);
        // Specify the component name as this is a targeted broadcast to
        // ConstraintProxyUpdateReceiver
        ComponentName name = new ComponentName(context, ConstraintProxyUpdateReceiver.class);
        intent.setComponent(name);
        intent.putExtra(KEY_BATTERY_NOT_LOW_PROXY_ENABLED, batteryNotLowProxyEnabled)
                .putExtra(KEY_BATTERY_CHARGING_PROXY_ENABLED, batteryChargingProxyEnabled)
                .putExtra(KEY_STORAGE_NOT_LOW_PROXY_ENABLED, storageNotLowProxyEnabled)
                .putExtra(KEY_NETWORK_STATE_PROXY_ENABLED, networkStateProxyEnabled);

        return intent;
    }

    @Override
    public void onReceive(@NonNull final Context context, @Nullable final Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (!ACTION.equals(action)) {
            Logger.get().debug(TAG, "Ignoring unknown action " + action);
        } else {
            final PendingResult pendingResult = goAsync();
            WorkManagerImpl workManager = WorkManagerImpl.getInstance(context);
            TaskExecutor taskExecutor = workManager.getWorkTaskExecutor();
            taskExecutor.executeOnTaskThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Doing this on a background thread, as using PackageManager to enable
                        // or disable proxies involves writes to the filesystem.
                        // b/134418962
                        boolean batteryNotLowProxyEnabled = intent.getBooleanExtra(
                                KEY_BATTERY_NOT_LOW_PROXY_ENABLED, false);
                        boolean batteryChargingProxyEnabled = intent.getBooleanExtra(
                                KEY_BATTERY_CHARGING_PROXY_ENABLED, false);
                        boolean storageNotLowProxyEnabled = intent.getBooleanExtra(
                                KEY_STORAGE_NOT_LOW_PROXY_ENABLED, false);
                        boolean networkStateProxyEnabled = intent.getBooleanExtra(
                                KEY_NETWORK_STATE_PROXY_ENABLED, false);

                        String message = "Updating proxies: ("
                                + "BatteryNotLowProxy (" + batteryNotLowProxyEnabled + "), "
                                + "BatteryChargingProxy (" + batteryChargingProxyEnabled + "), "
                                + "StorageNotLowProxy (" + storageNotLowProxyEnabled + "), "
                                + "NetworkStateProxy (" + networkStateProxyEnabled + "), ";

                        Logger.get().debug(TAG, message);
                        PackageManagerHelper.setComponentEnabled(context, BatteryNotLowProxy.class,
                                batteryNotLowProxyEnabled);
                        PackageManagerHelper.setComponentEnabled(context,
                                BatteryChargingProxy.class,
                                batteryChargingProxyEnabled);
                        PackageManagerHelper.setComponentEnabled(context, StorageNotLowProxy.class,
                                storageNotLowProxyEnabled);
                        PackageManagerHelper.setComponentEnabled(context, NetworkStateProxy.class,
                                networkStateProxyEnabled);
                    } finally {
                        pendingResult.finish();
                    }
                }
            });
        }
    }

}
