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
import android.content.res.Configuration;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.CarContext.CarServiceType;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.RemoteUtils;
import androidx.car.app.utils.ThreadUtils;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleRegistry;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.InvalidParameterException;

/**
 * The base class for implementing a car app that runs in the car.
 *
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
 * <a href="https://developer.android.com/training/cars/navigation#supported-app-categories">Supported App Categories</a>.
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
 * service</a>. Also note that accessing location may become unreliable when the phone is in the
 * battery saver mode.
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
     * Used to declare that this app is a navigation app in the manifest.
     */
    public static final String CATEGORY_NAVIGATION_APP = "androidx.car.app.category.NAVIGATION";

    /**
     * Used to declare that this app is a parking app in the manifest.
     */
    public static final String CATEGORY_PARKING_APP = "androidx.car.app.category.PARKING";

    /**
     * Used to declare that this app is a charging app in the manifest.
     */
    public static final String CATEGORY_CHARGING_APP = "androidx.car.app.category.CHARGING";

    private static final String AUTO_DRIVE = "AUTO_DRIVE";

    @Nullable
    private AppInfo mAppInfo;

    @Nullable
    private Session mCurrentSession;

    @Nullable
    private HostValidator mHostValidator;

    @Nullable
    private HostInfo mHostInfo;

    @Nullable
    private HandshakeInfo mHandshakeInfo;

    /**
     * Handles the host binding to this car app.
     *
     * <p>This method is final to ensure this car app's lifecycle is handled properly.
     *
     * <p>Use {@link #onCreateSession()} and {@link Session#onNewIntent} instead to handle incoming
     * {@link Intent}s.
     */
    @Override
    @CallSuper
    @NonNull
    public final IBinder onBind(@NonNull Intent intent) {
        return mBinder;
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
        runOnMain(() -> {
            if (mCurrentSession != null) {
                // Destroy the session
                // The session's lifecycle is observed by some of the manager and they will
                // perform cleanup on destroy.  For example, the ScreenManager can destroy all
                // Screens it holds.
                LifecycleRegistry lifecycleRegistry = getLifecycleIfValid();
                if (lifecycleRegistry == null) {
                    Log.e(TAG, "Null Session when unbinding");
                } else {
                    lifecycleRegistry.handleLifecycleEvent(Event.ON_DESTROY);
                }
            }
            mCurrentSession = null;
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
     * <p>Please refer to {@link androidx.car.app.R.array#hosts_allowlist_sample} to obtain a list
     * of package names and signatures that should be allow-listed by default.
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
    public abstract Session onCreateSession();

    @Override
    @CallSuper
    public final void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter writer,
            @Nullable String[] args) {
        super.dump(fd, writer, args);

        for (String arg : args) {
            if (AUTO_DRIVE.equals(arg)) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Executing onAutoDriveEnabled");
                }
                runOnMain(() -> {
                    if (mCurrentSession != null) {
                        mCurrentSession.getCarContext().getCarService(
                                NavigationManager.class).onAutoDriveEnabled();
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
     * Returns the current {@link Session} for this service.
     */
    @Nullable
    public final Session getCurrentSession() {
        return mCurrentSession;
    }

    // Strictly to avoid synthetic accessor.
    void setCurrentSession(@Nullable Session session) {
        mCurrentSession = session;
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

    @NonNull
    HostValidator getHostValidator() {
        if (mHostValidator == null) {
            mHostValidator = createHostValidator();
        }
        return mHostValidator;
    }

    /**
     * Used by tests to verify the different behaviors when the app has different api level than
     * the host.
     */
    @VisibleForTesting
    void setHandshakeInfo(@NonNull HandshakeInfo handshakeInfo) {
        int apiLevel = handshakeInfo.getHostCarAppApiLevel();
        if (!CarAppApiLevels.isValid(apiLevel)) {
            throw new IllegalArgumentException("Invalid Car App API level received: " + apiLevel);
        }

        mHandshakeInfo = handshakeInfo;
    }

    // Strictly to avoid synthetic accessor.
    @Nullable
    HandshakeInfo getHandshakeInfo() {
        return mHandshakeInfo;
    }

    @Nullable
    LifecycleRegistry getLifecycleIfValid() {
        Session session = getCurrentSession();
        return session == null ? null : (LifecycleRegistry) session.getLifecycleInternal();
    }

    @NonNull
    LifecycleRegistry getLifecycle() {
        return requireNonNull(getLifecycleIfValid());
    }

    private final ICarApp.Stub mBinder =
            new ICarApp.Stub() {
                // incompatible argument for parameter context of attachBaseContext.
                // call to onCreateScreen(android.content.Intent) not allowed on the given receiver.
                @SuppressWarnings({
                        "nullness:argument.type.incompatible",
                        "nullness:method.invocation.invalid"
                })
                @Override
                public void onAppCreate(
                        ICarHost carHost,
                        Intent intent,
                        Configuration configuration,
                        IOnDoneCallback callback) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onAppCreate intent: " + intent);
                    }

                    RemoteUtils.dispatchCallFromHost(callback, "onAppCreate", () -> {
                        Session session = getCurrentSession();
                        if (session == null
                                || getLifecycle().getCurrentState() == State.DESTROYED) {
                            session = onCreateSession();
                            setCurrentSession(session);
                        }

                        session.configure(CarAppService.this, requireNonNull(getHandshakeInfo()),
                                carHost, configuration);

                        // Whenever the host unbinds, the screens in the stack are destroyed.  If
                        // there is another bind, before the OS has destroyed this Service, then
                        // the stack will be empty, and we need to treat it as a new instance.
                        LifecycleRegistry registry = getLifecycle();
                        Lifecycle.State state = registry.getCurrentState();
                        int screenStackSize = session.getCarContext().getCarService(
                                ScreenManager.class).getScreenStack().size();
                        if (!state.isAtLeast(State.CREATED) || screenStackSize < 1) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "onAppCreate the app was not yet created or the "
                                        + "screen stack was empty state: "
                                        + registry.getCurrentState()
                                        + ", stack size: " + screenStackSize);
                            }
                            registry.handleLifecycleEvent(Event.ON_CREATE);
                            session.getCarContext().getCarService(ScreenManager.class).push(
                                    session.onCreateScreen(intent));
                        } else {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "onAppCreate the app was already created");
                            }
                            onNewIntentInternal(session, intent);
                        }
                        return null;
                    });

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onAppCreate completed");
                    }
                }

                @Override
                public void onAppStart(IOnDoneCallback callback) {
                    RemoteUtils.dispatchCallFromHost(
                            getLifecycleIfValid(), callback,
                            "onAppStart", () -> {
                                getLifecycle().handleLifecycleEvent(Event.ON_START);
                                return null;
                            });
                }

                @Override
                public void onAppResume(IOnDoneCallback callback) {
                    RemoteUtils.dispatchCallFromHost(
                            getLifecycleIfValid(), callback,
                            "onAppResume", () -> {
                                getLifecycle()
                                        .handleLifecycleEvent(Event.ON_RESUME);
                                return null;
                            });
                }

                @Override
                public void onAppPause(IOnDoneCallback callback) {
                    RemoteUtils.dispatchCallFromHost(
                            getLifecycleIfValid(), callback, "onAppPause",
                            () -> {
                                getLifecycle().handleLifecycleEvent(Event.ON_PAUSE);
                                return null;
                            });
                }

                @Override
                public void onAppStop(IOnDoneCallback callback) {
                    RemoteUtils.dispatchCallFromHost(
                            getLifecycleIfValid(), callback, "onAppStop",
                            () -> {
                                getLifecycle().handleLifecycleEvent(Event.ON_STOP);
                                return null;
                            });
                }

                @Override
                public void onNewIntent(Intent intent, IOnDoneCallback callback) {
                    RemoteUtils.dispatchCallFromHost(
                            getLifecycleIfValid(),
                            callback,
                            "onNewIntent",
                            () -> {
                                onNewIntentInternal(throwIfInvalid(getCurrentSession()), intent);
                                return null;
                            });
                }

                @Override
                public void onConfigurationChanged(Configuration configuration,
                        IOnDoneCallback callback) {
                    RemoteUtils.dispatchCallFromHost(
                            getLifecycleIfValid(),
                            callback,
                            "onConfigurationChanged",
                            () -> {
                                onConfigurationChangedInternal(
                                        throwIfInvalid(getCurrentSession()), configuration);
                                return null;
                            });
                }

                @Override
                public void getManager(@CarServiceType @NonNull String type,
                        IOnDoneCallback callback) {
                    ThreadUtils.runOnMain(() -> {
                        Session session = throwIfInvalid(getCurrentSession());
                        switch (type) {
                            case CarContext.APP_SERVICE:
                                RemoteUtils.sendSuccessResponseToHost(
                                        callback,
                                        "getManager",
                                        session.getCarContext().getCarService(
                                                AppManager.class).getIInterface());
                                return;
                            case CarContext.NAVIGATION_SERVICE:
                                RemoteUtils.sendSuccessResponseToHost(
                                        callback,
                                        "getManager",
                                        session.getCarContext().getCarService(
                                                NavigationManager.class).getIInterface());
                                return;
                            default:
                                Log.e(TAG, type + "%s is not a valid manager");
                                RemoteUtils.sendFailureResponseToHost(callback, "getManager",
                                        new InvalidParameterException(
                                                type + " is not a valid manager type"));
                        }
                    });
                }

                @Override
                public void getAppInfo(IOnDoneCallback callback) {
                    try {
                        RemoteUtils.sendSuccessResponseToHost(
                                callback, "getAppInfo", CarAppService.this.getAppInfo());
                    } catch (IllegalArgumentException e) {
                        // getAppInfo() could fail with the specified API version is invalid.
                        RemoteUtils.sendFailureResponseToHost(callback, "getAppInfo", e);
                    }
                }

                @Override
                public void onHandshakeCompleted(Bundleable handshakeInfo,
                        IOnDoneCallback callback) {
                    try {
                        HandshakeInfo deserializedHandshakeInfo =
                                (HandshakeInfo) handshakeInfo.get();
                        String packageName = deserializedHandshakeInfo.getHostPackageName();
                        int uid = Binder.getCallingUid();
                        HostInfo hostInfo = new HostInfo(packageName, uid);
                        if (!getHostValidator().isValidHost(hostInfo)) {
                            RemoteUtils.sendFailureResponseToHost(callback, "onHandshakeCompleted",
                                    new IllegalArgumentException("Unknown host '"
                                            + packageName + "', uid:" + uid));
                            return;
                        }

                        int appMinApiLevel =
                                CarAppService.this.getAppInfo().getMinCarAppApiLevel();
                        int hostApiLevel = deserializedHandshakeInfo.getHostCarAppApiLevel();
                        if (appMinApiLevel > hostApiLevel) {
                            RemoteUtils.sendFailureResponseToHost(callback, "onHandshakeCompleted",
                                    new IllegalArgumentException(
                                            "Host API level (" + hostApiLevel + ") is "
                                                    + "less than the app's min API level ("
                                                    + appMinApiLevel + ")"));
                            return;
                        }

                        setHostInfo(hostInfo);
                        setHandshakeInfo(deserializedHandshakeInfo);
                        RemoteUtils.sendSuccessResponseToHost(callback, "onHandshakeCompleted",
                                null);
                    } catch (BundlerException | IllegalArgumentException e) {
                        setHostInfo(null);
                        RemoteUtils.sendFailureResponseToHost(callback, "onHandshakeCompleted", e);
                    }
                }

                // call to onNewIntent(android.content.Intent) not allowed on the given receiver.
                @SuppressWarnings("nullness:method.invocation.invalid")
                @MainThread
                private void onNewIntentInternal(Session session, Intent intent) {
                    ThreadUtils.checkMainThread();
                    session.onNewIntent(intent);
                }

                // call to onCarConfigurationChanged(android.content.res.Configuration) not
                // allowed on the given receiver.
                @SuppressWarnings("nullness:method.invocation.invalid")
                @MainThread
                private void onConfigurationChangedInternal(Session session,
                        Configuration configuration) {
                    ThreadUtils.checkMainThread();
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onCarConfigurationChanged configuration: " + configuration);
                    }

                    session.onCarConfigurationChangedInternal(configuration);
                }
            };

    Session throwIfInvalid(@Nullable Session session) {
        if (session == null) {
            throw new IllegalStateException("Null session found when non-null expected");
        }

        return session;
    }
}
