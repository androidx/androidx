/*
 * Copyright 2017 The Android Open Source Project
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
package android.arch.background.workmanager.background.systemalarm;

import static android.arch.background.workmanager.utils.PackageManagerHelper
        .isComponentExplicitlyEnabled;
import static android.arch.background.workmanager.utils.PackageManagerHelper.setComponentEnabled;

import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import java.util.List;

/**
 * Monitors constrained {@link WorkSpec}s and enables/disables {@link ConstraintProxy} as needed
 * in the manifest.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProxyController implements Observer<List<WorkSpec>> {
    private static final String TAG = "ProxyController";
    private final Class<? extends ConstraintProxy> mProxyClass;
    private final Context mAppContext;

    private ProxyController(
            @NonNull Context context, @NonNull Class<? extends ConstraintProxy> proxyClass) {
        mProxyClass = proxyClass;
        mAppContext = context;
        Log.d(TAG, "ProxyController created for " + proxyClass.getSimpleName());
    }

    @Override
    public void onChanged(@Nullable List<WorkSpec> workSpecs) {
        if (workSpecs == null) {
            return;
        }
        boolean currentState = isComponentExplicitlyEnabled(mAppContext, mProxyClass);
        boolean newState = !workSpecs.isEmpty();
        Log.d(TAG, mProxyClass.getSimpleName()
                + "; currentState = " + currentState + "; newState = " + newState);
        if (currentState != newState) {
            setComponentEnabled(mAppContext, mProxyClass, newState);
        }
    }

    /**
     * Creates and attaches all {@link ProxyController}s to their corresponding LiveData.
     * @param context {@link Context}
     * @param database {@link WorkDatabase}
     * @param owner {@link LifecycleOwner} which controls the {@link ProxyController}
     */
    public static void startProxyControllers(
            Context context, WorkDatabase database, LifecycleOwner owner) {
        ProxyController batteryNotLowController =
                new ProxyController(context, ConstraintProxy.BatteryNotLowProxy.class);
        ProxyController batteryChargingController =
                new ProxyController(context, ConstraintProxy.BatteryChargingProxy.class);

        WorkSpecDao workSpecDao = database.workSpecDao();
        workSpecDao.getIdsForBatteryNotLowController(true)
                .observe(owner, batteryNotLowController);
        workSpecDao.getIdsForBatteryChargingController(true)
                .observe(owner, batteryChargingController);
    }
}
