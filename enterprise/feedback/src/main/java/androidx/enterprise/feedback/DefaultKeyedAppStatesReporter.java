/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.enterprise.feedback;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link KeyedAppStatesReporter} that binds to device owners, profile owners, and the Play store.
 *
 * <p>Each instance maintains bindings, so it's recommended that you maintain a single instance for
 * your whole app, rather than creating instances as needed.
 */
final class DefaultKeyedAppStatesReporter extends KeyedAppStatesReporter {
    private final Context mContext;

    private final Map<String, BufferedServiceConnection> mServiceConnections = new HashMap<>();

    private static final int EXECUTOR_IDLE_ALIVE_TIME_SECS = 20;
    private final Executor mExecutor;

    /**
     * Creates an {@link ExecutorService} which has no persistent background thread, and ensures
     * tasks will run in submit order.
     */
    private static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(
                /* corePoolSize= */ 0,
                /* maximumPoolSize= */ 1,
                EXECUTOR_IDLE_ALIVE_TIME_SECS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Create a reporter using the specified executor.
     *
     * <p>The executor must run all {@link Runnable} instances on the same thread, serially.
     */
    DefaultKeyedAppStatesReporter(@NonNull Context context, @NonNull Executor executor) {
        if (executor == null) {
            throw new NullPointerException("Executor can not be null.");
        }
        this.mContext = context.getApplicationContext();
        this.mExecutor = executor;
    }

    DefaultKeyedAppStatesReporter(@NonNull Context context) {
        this(context, createExecutorService());
    }

    @Override
    @Deprecated
    public void setStates(@NonNull Collection<KeyedAppState> states) {
        setStates(states, /* callback= */ null);
    }

    @Override
    public void setStates(@NonNull Collection<KeyedAppState> states,
            @Nullable KeyedAppStatesCallback callback) {
        setStates(states, callback, /* immediate= */ false);
    }

    private void setStates(final Collection<KeyedAppState> states,
            final KeyedAppStatesCallback callback, final boolean immediate) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (states.isEmpty()) {
                    if (callback != null) {
                        callback.onResult(
                                KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);
                    }
                    return;
                }

                unbindOldBindings();
                bind();

                send(buildStatesBundle(states), callback, immediate);
            }
        });
    }

    @Override
    @Deprecated
    public void setStatesImmediate(@NonNull Collection<KeyedAppState> states) {
        setStatesImmediate(states, /* callback= */ null);
    }

    @Override
    public void setStatesImmediate(@NonNull Collection<KeyedAppState> states,
            @Nullable KeyedAppStatesCallback callback) {
        setStates(states, callback, /* immediate= */ true);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void bind() {
        Collection<String> acceptablePackageNames = getDeviceOwnerAndProfileOwnerPackageNames();
        acceptablePackageNames.add(PHONESKY_PACKAGE_NAME);
        bind(acceptablePackageNames);
    }

    private void bind(Collection<String> acceptablePackageNames) {
        // Remove already-bound packages
        Collection<String> filteredPackageNames = new HashSet<>();
        for (String packageName : acceptablePackageNames) {
            if (!mServiceConnections.containsKey(packageName)) {
                filteredPackageNames.add(packageName);
            }
        }

        if (filteredPackageNames.isEmpty()) {
            return;
        }

        Collection<ServiceInfo> serviceInfos =
                getServiceInfoInPackages(new Intent(ACTION_APP_STATES), filteredPackageNames);

        for (ServiceInfo serviceInfo : serviceInfos) {
            Intent bindIntent = new Intent();
            bindIntent.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));

            BufferedServiceConnection bufferedServiceConnection =
                    new BufferedServiceConnection(
                        mExecutor, mContext, bindIntent, Context.BIND_AUTO_CREATE);
            bufferedServiceConnection.bindService();

            mServiceConnections.put(serviceInfo.packageName, bufferedServiceConnection);
        }
    }

    private Collection<String> getDeviceOwnerAndProfileOwnerPackageNames() {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        Collection<ComponentName> activeAdmins = devicePolicyManager.getActiveAdmins();

        if (activeAdmins == null) {
            return new ArrayList<>();
        }

        Collection<String> deviceOwnerProfileOwnerPackageNames = new ArrayList<>();

        for (ComponentName componentName : activeAdmins) {
            if (devicePolicyManager.isDeviceOwnerApp(componentName.getPackageName())
                    || devicePolicyManager.isProfileOwnerApp(componentName.getPackageName())) {
                deviceOwnerProfileOwnerPackageNames.add(componentName.getPackageName());
            }
        }

        return deviceOwnerProfileOwnerPackageNames;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void unbindOldBindings() {
        Iterator<Map.Entry<String, BufferedServiceConnection>> iterator =
                mServiceConnections.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, BufferedServiceConnection> entry = iterator.next();
            if (packageNameShouldBeUnbound(entry.getKey())) {
                entry.getValue().unbind();
                iterator.remove();
            }
        }
    }

    /** Assumes the given package name is a stored service connection. */
    private boolean packageNameShouldBeUnbound(String packageName) {
        if (Build.VERSION.SDK_INT < 26
                && mServiceConnections.get(packageName).hasBeenDisconnected()) {
            return true;
        }

        if (mServiceConnections.get(packageName).isDead()) {
            return true;
        }

        if (!canPackageReceiveAppStates(mContext, packageName)) {
            return true;
        }

        return false;
    }

    private Collection<ServiceInfo> getServiceInfoInPackages(
            Intent intent, Collection<String> acceptablePackageNames) {
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(intent, /* flags = */0);

        Collection<ServiceInfo> validServiceInfo = new ArrayList<>();
        for (ResolveInfo i : resolveInfos) {
            if (acceptablePackageNames.contains(i.serviceInfo.packageName)) {
                validServiceInfo.add(i.serviceInfo);
            }
        }
        return validServiceInfo;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static Bundle buildStatesBundle(Collection<KeyedAppState> keyedAppStates) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(APP_STATES, buildStateBundles(keyedAppStates));
        return bundle;
    }

    // Returns an ArrayList as required to be used with Bundle#putParcelableArrayList.
    private static ArrayList<Bundle> buildStateBundles(Collection<KeyedAppState> keyedAppStates) {
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (KeyedAppState keyedAppState : keyedAppStates) {
            bundles.add(keyedAppState.toStateBundle());
        }
        return bundles;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void send(
            Bundle appStatesBundle, @Nullable KeyedAppStatesCallback callback, boolean immediate) {
        if (callback != null) {
            // Callback will receive multiple callbacks so we need to merge them into a single one.
            callback = new KeyedAppStatesCallbackMerger(mServiceConnections.size(), callback);
        }
        for (BufferedServiceConnection serviceConnection : mServiceConnections.values()) {
            serviceConnection.send(new SendableMessage(appStatesBundle, callback, immediate));
        }
    }
}
