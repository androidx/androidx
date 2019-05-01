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

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link KeyedAppStatesReporter} that only allows a single instance to exist at one time,
 * avoiding repeated instantiations.
 */
public class SingletonKeyedAppStatesReporter extends KeyedAppStatesReporter {

    private static final String LOG_TAG = "KeyedAppStatesReporter";

    @SuppressLint("StaticFieldLeak") // Application Context only.
    private static volatile SingletonKeyedAppStatesReporter sSingleton;

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
                new LinkedBlockingQueue<Runnable>() /* Not used */);
    }

    /**
     * Sets executor used to construct the singleton.
     *
     * <p>If required, this method must be called before calling {@link #getInstance(Context)}.
     *
     * <p>If this method is not called, the reporter will run on a newly-created thread.
     * This newly-created thread will be cleaned up and recreated as necessary when idle.
     */
    public static void initialize(@NonNull Context context, @NonNull Executor executor) {
        if (context == null || executor == null) {
            throw new NullPointerException();
        }
        synchronized (KeyedAppStatesReporter.class) {
            if (sSingleton != null) {
                throw new IllegalStateException(
                        "initialize can only be called once and must be called before "
                            + "calling getInstance.");
            }
            initializeSingleton(context, executor);
        }
    }

    /**
     * Returns an instance of the reporter.
     *
     * <p>Creates and initializes an instance if one doesn't already exist.
     */
    @NonNull
    public static KeyedAppStatesReporter getInstance(@NonNull Context context) {
        if (context == null || context.getApplicationContext() == null) {
            throw new NullPointerException();
        }
        if (sSingleton == null) {
            synchronized (KeyedAppStatesReporter.class) {
                if (sSingleton == null) {
                    initializeSingleton(context, createExecutorService());
                }
            }
        }
        return sSingleton;
    }

    private static void initializeSingleton(@NonNull Context context, @NonNull Executor executor) {
        sSingleton = new SingletonKeyedAppStatesReporter(context, executor);
        sSingleton.bind();
    }

    @VisibleForTesting
    static void resetSingleton() {
        synchronized (KeyedAppStatesReporter.class) {
            sSingleton = null;
        }
    }

    private SingletonKeyedAppStatesReporter(Context context, Executor executor) {
        this.mContext = context.getApplicationContext();
        this.mExecutor = executor;
    }

    @Override
    public void setStates(@NonNull Collection<KeyedAppState> states) {
        setStates(states, false);
    }

    private void setStates(final Collection<KeyedAppState> states, final boolean immediate) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (states.isEmpty()) {
                    return;
                }

                unbindOldBindings();
                bind();

                send(buildStatesBundle(states), immediate);
            }
        });
    }

    @Override
    public void setStatesImmediate(@NonNull Collection<KeyedAppState> states) {
        setStates(states, true);
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
        Iterator<Entry<String, BufferedServiceConnection>> iterator =
                mServiceConnections.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, BufferedServiceConnection> entry = iterator.next();
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
    void send(Bundle appStatesBundle, boolean immediate) {
        for (BufferedServiceConnection serviceConnection : mServiceConnections.values()) {
            // Messages cannot be reused so we create a copy for each service connection.
            serviceConnection.send(createStateMessage(appStatesBundle, immediate));
        }
    }

    private static Message createStateMessage(Bundle appStatesBundle, boolean immediate) {
        Message message = Message.obtain();
        message.what = immediate ? WHAT_IMMEDIATE_STATE : WHAT_STATE;
        message.obj = appStatesBundle;
        return message;
    }
}
