/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static androidx.car.app.utils.LogTags.TAG;
import static androidx.car.app.utils.ThreadUtils.runOnMain;

import static java.util.Objects.requireNonNull;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.validation.HostValidator;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * The base class for implementing a car app that runs in the car
 * <h4>Service Declaration</h4>
 *
 * The app must extend the {@link CarAppService} to be bound by the car host. The service must also
 * respond to {@link Intent} actions coming from the host, by adding an
 * <code>intent-filter</code> to the service in the <code>AndroidManifest.xml</code> that handles
 * the {@link #SERVICE_INTERFACE} action. The app must also declare what category of application
 * it is (e.g. {@link #CATEGORY_NAVIGATION_APP}). For example:
 *
 * <pre>{@code
 * <service
 *   android:name=".YourAppService"
 *   android:exported="true">
 *   <intent-filter>
 *     <action android:name="androidx.car.app.CarAppService" />
 *     <category android:name="androidx.car.app.category.NAVIGATION"/>
 *   </intent-filter>
 * </service>
 * }</pre>
 *
 * <p>For a list of all the supported categories see
 * <a href="https://developer.android.com/training/cars/apps#supported-app-categories">Supported App Categories</a>.
 *
 * <h4>Accessing Location</h4>
 *
 * When the app is running in the car display, the system will not consider it as being in the
 * foreground, and hence it will be considered in the background for the purpose of retrieving
 * location as described <a
 * href="https://developer.android.com/about/versions/10/privacy/changes#app-access-device
 * -location">here</a>.
 *
 * <p>To reliably get location for your car app, we recommended that you use a <a
 * href="https://developer.android.com/guide/components/services?#Types-of-services">foreground
 * service</a>. If you have a service other than your {@link CarAppService} that accesses
 * location, run the service and your `CarAppService` in the same process. Also note that
 * accessing location may become unreliable when the phone is in the battery saver mode.
 */
public abstract class CarAppService extends Service {
    /**
     * The full qualified name of the {@link CarAppService} class.
     *
     * <p>This is the same name that must be used to declare the action of the intent filter for
     * the app's {@link CarAppService} in the app's manifest.
     *
     * @see CarAppService
     */
    public static final String SERVICE_INTERFACE = "androidx.car.app.CarAppService";

    /**
     * Used to declare that this app supports cluster in the manifest.
     */
    @RequiresCarApi(6)
    public static final String CATEGORY_FEATURE_CLUSTER =
            "androidx.car.app.category.FEATURE_CLUSTER";

    /**
     * Used to declare that this app is a navigation app in the manifest.
     */
    public static final String CATEGORY_NAVIGATION_APP = "androidx.car.app.category.NAVIGATION";

    /**
     * Used to declare that this app is a parking app in the manifest.
     *
     * @deprecated use {@link #CATEGORY_POI_APP} instead
     */
    @Deprecated
    public static final String CATEGORY_PARKING_APP = "androidx.car.app.category.PARKING";

    /**
     * Used to declare that this app is a charging app in the manifest.
     *
     * @deprecated use {@link #CATEGORY_POI_APP} instead
     */
    @Deprecated
    public static final String CATEGORY_CHARGING_APP = "androidx.car.app.category.CHARGING";

    /**
     * Used in the app manifest. It declares that this app finds points of interests (POI).
     */
    public static final String CATEGORY_POI_APP = "androidx.car.app.category.POI";

    /**
     * Used in the app manifest. It declares that this app declares physical objects with sensors,
     * that connect and exchange data with other devices and systems.
     */
    @ExperimentalCarApi
    public static final String CATEGORY_IOT_APP = "androidx.car.app.category.IOT";

    /**
     * Used to declare that this app is a settings app in the manifest. This app can be used to
     * provide screens corresponding to the settings page and/or any error resolution screens e.g.
     * sign-in screen.
     */
    @ExperimentalCarApi
    public static final String CATEGORY_SETTINGS_APP = "androidx.car.app.category.SETTINGS";

    /**
     * Used to declare that this app is a messaging app in the manifest.
     *
     * <p> This app can be used to send and receive short-form chat messages (IM/SMS).
     */
    @ExperimentalCarApi
    public static final String CATEGORY_MESSAGING_APP = "androidx.car.app.category.MESSAGING";

    /**
     * Used in the app manifest to declare that this app supports calling.
     */
    @ExperimentalCarApi
    public static final String CATEGORY_CALLING_APP = "androidx.car.app.category.CALLING";

    /**
     * Used to declare that this app is a weather app in the manifest.
     */
    @ExperimentalCarApi
    public static final String CATEGORY_WEATHER_APP = "androidx.car.app.category.WEATHER";

    private static final String AUTO_DRIVE = "AUTO_DRIVE";

    @NonNull
    private final Map<SessionInfo, CarAppBinder> mBinders = new HashMap<>();

    @Nullable
    private AppInfo mAppInfo;

    @Nullable
    private HostInfo mHostInfo;

    @Override
    @CallSuper
    public void onDestroy() {
        synchronized (mBinders) {
            for (CarAppBinder binder : mBinders.values()) {
                binder.destroy();
            }
            mBinders.clear();
        }
    }

    /**
     * Handles the host binding to this car app.
     *
     * <p>This method is final to ensure this car app's lifecycle is handled properly.
     *
     * <p>Use {@link #onCreateSession(SessionInfo)} and {@link Session#onNewIntent} instead to
     * handle incoming {@link Intent}s.
     */
    @Override
    @CallSuper
    @NonNull
    public final IBinder onBind(@NonNull Intent intent) {
        SessionInfo sessionInfo = SessionInfoIntentEncoder.containsSessionInfo(intent)
                ? SessionInfoIntentEncoder.decode(intent)
                : SessionInfo.DEFAULT_SESSION_INFO;

        synchronized (mBinders) {
            if (!mBinders.containsKey(sessionInfo)) {
                mBinders.put(sessionInfo, new CarAppBinder(this, sessionInfo));
            }
            return requireNonNull(mBinders.get(sessionInfo));
        }
    }

    /**
     * Handles the host unbinding from this car app.
     *
     * <p>This method is final to ensure this car app's lifecycle is handled properly.
     */
    @Override
    public final boolean onUnbind(@NonNull Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onUnbind intent: " + intent);
        }

        SessionInfo sessionInfo = SessionInfoIntentEncoder.containsSessionInfo(intent)
                ? SessionInfoIntentEncoder.decode(intent)
                : SessionInfo.DEFAULT_SESSION_INFO;
        runOnMain(() -> {
            synchronized (mBinders) {
                CarAppBinder binder = mBinders.remove(sessionInfo);
                if (binder != null) {
                    // We call onDestroyLifecycle() instead of destroy() here because Service
                    // caches the binder returned by onBind() for a given Intent meaning this
                    // binder might be reused on a future onBind() call.
                    binder.onDestroyLifecycle();
                }
            }
        });

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onUnbind completed");
        }
        // Return true to request an onRebind call.  This means that the process will cache this
        // instance of the Service to return on future bind calls.
        return true;
    }

    /**
     * Returns the {@link HostValidator} this service will use to accept or reject host connections.
     *
     * <p>By default, the provided {@link HostValidator.Builder} would produce a validator that
     * only accepts connections from hosts holding
     * {@link HostValidator#TEMPLATE_RENDERER_PERMISSION} permission.
     *
     * <p>Application developers are expected to also allow connections from known hosts which
     * don't hold the aforementioned permission (for example, Android Auto and Android
     * Automotive OS hosts below API level 31), by allow-listing the signatures of those hosts.
     *
     * <p>Refer to {@code androidx.car.app.R.array.hosts_allowlist_sample} to obtain a
     * list of package names and signatures that should be allow-listed by default.
     *
     * <p>It is also advised to allow connections from unknown hosts in debug builds to facilitate
     * debugging and testing.
     *
     * <p>Below is an example of this method implementation:
     *
     * <pre>
     * &#64;Override
     * &#64;NonNull
     * public HostValidator createHostValidator() {
     *     if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
     *         return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
     *     } else {
     *         return new HostValidator.Builder(context)
     *             .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
     *             .build();
     *     }
     * }
     * </pre>
     */
    @NonNull
    public abstract HostValidator createHostValidator();

    /**
     * Creates a new {@link Session} for the application.
     *
     * <p>This method is invoked the first time the app is started, or if the previous
     * {@link Session} instance has been destroyed and the system has not yet destroyed
     * this service.
     *
     * <p>Once the method returns, {@link Session#onCreateScreen(Intent)} will be called on the
     * {@link Session} returned.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @see CarContext#startCarApp(Intent)
     */
    @NonNull
    public Session onCreateSession() {
        throw new RuntimeException(
                "Please override and implement CarAppService#onCreateSession(SessionInfo).");
    }


    // TODO(b/236140507): Link AndroidManifest.xml documentation or equivalent in this javadoc
    /**
     * Creates a new {@link Session}.
     *
     * <p>This method is invoked once per app-supported physical display with a unique
     * {@link SessionInfo} identifying the type of display. Support for displays is declared within
     * the AndroidManifest.xml. This method can also be invoked if the previous instance has been
     * destroyed (ie. due to memory pressure) and the system has not yet destroyed this service.
     *
     * <p>This method is called by the system and should not be called directly.
     *
     * @see CarContext#startCarApp(Intent)
     */
    @NonNull
    @SuppressWarnings("deprecation")
    @RequiresCarApi(6)
    public Session onCreateSession(@NonNull SessionInfo sessionInfo) {
        return onCreateSession();
    }

    @Override
    @CallSuper
    public final void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter writer,
            @Nullable String[] args) {
        super.dump(fd, writer, args);
        if (args == null) return;
        for (String arg : args) {
            if (AUTO_DRIVE.equals(arg)) {
                runOnMain(() -> {
                    synchronized (mBinders) {
                        for (CarAppBinder binder : mBinders.values()) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Executing onAutoDriveEnabled for "
                                        + binder.getCurrentSessionInfo());
                            }

                            binder.onAutoDriveEnabled();
                        }
                    }
                });
            }
        }
    }

    /**
     * Returns information about the host attached to this service.
     *
     * @see HostInfo
     */
    @Nullable
    public final HostInfo getHostInfo() {
        return mHostInfo;
    }

    void setHostInfo(@Nullable HostInfo hostInfo) {
        mHostInfo = hostInfo;
    }

    /**
     * Returns the current {@link Session} for the main display if one exists, otherwise returns
     * {@code null}.
     *
     * @deprecated use {@link #getSession(SessionInfo)}
     */
    @Nullable
    @Deprecated
    public final Session getCurrentSession() {
        synchronized (mBinders) {
            for (Map.Entry<SessionInfo, CarAppBinder> entry : mBinders.entrySet()) {
                if (entry.getKey().getDisplayType() == SessionInfo.DISPLAY_TYPE_MAIN) {
                    return entry.getValue().getCurrentSession();
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link Session} associated to the given {@link SessionInfo}, or {@code null}
     * if one doesn't exist.
     */
    @Nullable
    public final Session getSession(@NonNull SessionInfo sessionInfo) {
        synchronized (mBinders) {
            CarAppBinder binder = mBinders.get(sessionInfo);
            if (binder == null) {
                return null;
            }
            return binder.getCurrentSession();
        }
    }


    // Strictly to avoid synthetic accessor.
    @NonNull
    AppInfo getAppInfo() {
        if (mAppInfo == null) {
            // Lazy-initialized as the package manager is not available if this is created inlined.
            mAppInfo = AppInfo.create(this);
        }
        return mAppInfo;
    }

    /**
     * Used by tests to verify the different behaviors when the app has different api level than
     * the host.
     */
    @VisibleForTesting
    void setAppInfo(@Nullable AppInfo appInfo) {
        mAppInfo = appInfo;
    }

    @VisibleForTesting
    void setBinder(@NonNull SessionInfo sessionInfo, @Nullable CarAppBinder binder) {
        if (binder == null) {
            mBinders.remove(sessionInfo);
            return;
        }

        mBinders.put(sessionInfo, binder);
    }
}
