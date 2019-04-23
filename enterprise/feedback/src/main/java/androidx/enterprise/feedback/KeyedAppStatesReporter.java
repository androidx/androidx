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
 * A reporter of keyed app states to enable communication between an app and an EMM (enterprise
 * mobility management).
 */
public class KeyedAppStatesReporter {

    private static final String LOG_TAG = "KeyedAppStatesReporter";

    static final String PHONESKY_PACKAGE_NAME = "com.android.vending";

    @SuppressLint("StaticFieldLeak") // Application Context only.
    private static volatile KeyedAppStatesReporter sSingleton;

    /** The value of {@link Message#what} to indicate a state update. */
    static final int WHAT_STATE = 1;

    /**
     * The value of {@link Message#what} to indicate a state update with request for immediate
     * upload.
     */
    static final int WHAT_IMMEDIATE_STATE = 2;

    /** The name for the bundle (stored as a parcelable) containing the keyed app states. */
    static final String APP_STATES = "androidx.enterprise.feedback.APP_STATES";

    /**
     * The name for the keyed app state key for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getKey()
     */
    static final String APP_STATE_KEY = "androidx.enterprise.feedback.APP_STATE_KEY";

    /**
     * The name for the severity of the app state.
     *
     * @see KeyedAppState#getSeverity()
     */
    static final String APP_STATE_SEVERITY = "androidx.enterprise.feedback.APP_STATE_SEVERITY";

    /**
     * The name for the optional app state message for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getMessage()
     */
    static final String APP_STATE_MESSAGE = "androidx.enterprise.feedback.APP_STATE_MESSAGE";

    /**
     * The name for the optional app state data for a given bundle in {@link #APP_STATES}.
     *
     * @see KeyedAppState#getData()
     */
    static final String APP_STATE_DATA = "androidx.enterprise.feedback.APP_STATE_DATA";

    /** The intent action for reporting app states. */
    public static final String ACTION_APP_STATES = "androidx.enterprise.feedback.action.APP_STATES";

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
        sSingleton = new KeyedAppStatesReporter(context, executor);
        sSingleton.bind();
    }

    @VisibleForTesting
    static void resetSingleton() {
        synchronized (KeyedAppStatesReporter.class) {
            sSingleton = null;
        }
    }

    private KeyedAppStatesReporter(Context context, Executor executor) {
        this.mContext = context.getApplicationContext();
        this.mExecutor = executor;
    }

    /**
     * Set app states to be sent to an EMM (enterprise mobility management). The EMM can then
     * display this information to the management organization.
     *
     * <p>Do not send personally-identifiable information with this method.
     *
     * <p>Each provided keyed app state will replace any previously set keyed app states with the
     * same key for this package name.
     *
     * <p>If multiple keyed app states are set with the same key, only one will be received by the
     * EMM. Which will be received is not defined.
     *
     * <p>This information is sent immediately to all device owner and profile owner apps on the
     * device. It is also sent immediately to the app with package name com.android.vending if it
     * exists, which is the Play Store on GMS devices.
     *
     * <p>EMMs can access these states either directly in a custom DPC (device policy manager), via
     * Android Management APIs, or via Play EMM APIs.
     *
     * @see #setStatesImmediate(Collection)
     */
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

    /**
     * Performs the same function as {@link #setStates(Collection)}, except it
     * also requests that the states are immediately uploaded to be accessible
     * via server APIs.
     *
     * <p>The receiver is not obligated to meet this immediate upload request.
     * For example, Play and Android Management APIs have daily quotas.
     */
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

    static boolean canPackageReceiveAppStates(Context context, String packageName) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return packageName.equals(PHONESKY_PACKAGE_NAME)
                || devicePolicyManager.isDeviceOwnerApp(packageName)
                || devicePolicyManager.isProfileOwnerApp(packageName);
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
