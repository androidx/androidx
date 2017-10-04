/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.VisibleForTesting;
import android.support.v4.net.ConnectivityManagerCompat;
import android.util.Log;

/**
 * ConstraintsChecker ensures that {@link Constraints} are satisfied for {@link WorkSpec}s.
 */

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class ConstraintsChecker {
    private static final String TAG = "ConstraintsChecker";
    private static final float BATTERY_LOW_THRESHOLD = 15f;
    private Context mContext;

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public ConstraintsChecker(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Determines if all {@link Constraints} specified in a given {@link WorkSpec}
     * are currently satisfied
     * @param workSpec {@link WorkSpec} to check
     * @return {@code true} if all {@link Constraints} are satisfied, {@code false} otherwise
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public boolean areAllConstraintsMet(WorkSpec workSpec) {
        return isBatteryConstraintMet(workSpec)
                && isChargingConstraintMet(workSpec)
                && isDeviceIdleConstraintMet(workSpec)
                && isNetworkTypeConstraintMet(workSpec)
                && isStorageConstraintMet(workSpec);
    }

    boolean isNetworkTypeConstraintMet(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        int networkType = constraints.getRequiredNetworkType();
        if (networkType == Constraints.NETWORK_TYPE_NONE) {
            return true;
        }

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        switch (networkType) {
            case Constraints.NETWORK_TYPE_ANY:
                return isNetworkConnected(cm);
            case Constraints.NETWORK_TYPE_NOT_ROAMING:
                return isNetworkConnected(cm) && !isNetworkRoaming(cm);
            case Constraints.NETWORK_TYPE_METERED:
                return isNetworkConnected(cm) && isNetworkMetered(cm);
            case Constraints.NETWORK_TYPE_UNMETERED:
                return isNetworkConnected(cm) && !isNetworkMetered(cm);
            default:
                Log.e(TAG, "Unknown Network Type : " + networkType);
                return false;
        }
    }

    private boolean isNetworkConnected(ConnectivityManager cm) {
        return cm.getActiveNetworkInfo().isConnected();
    }

    private boolean isNetworkRoaming(ConnectivityManager cm) {
        return cm.getActiveNetworkInfo().isRoaming();
    }

    private boolean isNetworkMetered(ConnectivityManager cm) {
        return ConnectivityManagerCompat.isActiveNetworkMetered(cm);
    }

    boolean isChargingConstraintMet(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        if (!constraints.requiresCharging()) {
            return true;
        }

        Intent batteryInfo = getBatteryInfo();
        int status = batteryInfo.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL);
    }

    boolean isBatteryConstraintMet(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        if (!constraints.requiresBatteryNotLow()) {
            return true;
        }

        Intent batteryInfo = getBatteryInfo();
        int level = batteryInfo.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryInfo.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float) scale;

        boolean batteryLevelAboveThreshold = batteryPct > BATTERY_LOW_THRESHOLD;

        if (Build.VERSION.SDK_INT >= 21) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            boolean batterySaverOff = (pm != null && !pm.isPowerSaveMode());
            return batterySaverOff && batteryLevelAboveThreshold;
        }

        return batteryLevelAboveThreshold;
    }

    private Intent getBatteryInfo() {
        IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return mContext.registerReceiver(null, batteryIntentFilter);
    }

    boolean isDeviceIdleConstraintMet(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        if (!constraints.requiresDeviceIdle()) {
            return true;
        }

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (pm == null) {
            return true;
        } else if (Build.VERSION.SDK_INT >= 20) {
            return !pm.isInteractive();
        } else {
            return !pm.isScreenOn();
        }
    }

    boolean isStorageConstraintMet(WorkSpec workSpec) {
        Constraints constraints = workSpec.getConstraints();
        if (!constraints.requiresStorageNotLow()) {
            return true;
        }
        // TODO(xbhatnag): Investigate possible issues with O.
        IntentFilter storageIntentFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        return mContext.registerReceiver(null, storageIntentFilter) == null;
    }
}
