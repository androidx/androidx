/*
 * Copyright 2022 The Android Open Source Project
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
import static androidx.car.app.utils.RemoteUtils.dispatchCallFromHost;

import static java.util.Objects.requireNonNull;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.RemoteUtils;
import androidx.car.app.utils.ThreadUtils;
import androidx.car.app.validation.HostValidator;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.InvalidParameterException;

/** Implementation of the binder {@link ICarApp}. */
final class CarAppBinder extends ICarApp.Stub {
    private final SessionInfo mCurrentSessionInfo;

    private @Nullable CarAppService mService;
    private @Nullable Session mCurrentSession;
    private @Nullable HostValidator mHostValidator;
    private @Nullable HandshakeInfo mHandshakeInfo;

    /**
     * Creates a new {@link CarAppBinder} instance for a {@link SessionInfo}. Once the Host
     * requests {@link #onAppCreate(ICarHost, Intent, Configuration, IOnDoneCallback)}, the
     * {@link Session} will be created.
     */
    CarAppBinder(@NonNull CarAppService service, @NonNull SessionInfo sessionInfo) {
        mService = service;
        mCurrentSessionInfo = sessionInfo;
    }

    /**
     * Explicitly mark the binder to be destroyed and remove the reference to the
     * {@link CarAppService}, and any subsequent call from the host after this would be
     * considered invalid and throws an exception.
     *
     * <p>This is needed because the binder object can outlive the service and will not be
     * garbage collected until the car host cleans up its side of the binder reference,
     * causing a leak. See https://github.com/square/leakcanary/issues/1906 for more context
     * related to this issue.
     */
    void destroy() {
        onDestroyLifecycle();
        mCurrentSession = null;
        mHostValidator = null;
        mHandshakeInfo = null;
        mService = null;
    }

    void onDestroyLifecycle() {
        Session session = mCurrentSession;
        if (session != null) {
            session.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }
        mCurrentSession = null;
    }

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

        dispatchCallFromHost(callback, "onAppCreate", () -> {
            CarAppService service = requireNonNull(mService);
            Session session = mCurrentSession;
            if (session == null
                    || session.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                session = service.onCreateSession(requireNonNull(mCurrentSessionInfo));
                mCurrentSession = session;
            }

            session.configure(service,
                    requireNonNull(getHandshakeInfo()),
                    requireNonNull(service.getHostInfo()),
                    carHost, configuration);

            // Whenever the host unbinds, the screens in the stack are destroyed.  If
            // there is another bind, before the OS has destroyed this Service, then
            // the stack will be empty, and we need to treat it as a new instance.
            LifecycleRegistry registry = (LifecycleRegistry) session.getLifecycle();
            Lifecycle.State state = registry.getCurrentState();
            int screenStackSize = session.getCarContext().getCarService(
                    ScreenManager.class).getScreenStackInternal().size();
            if (!state.isAtLeast(Lifecycle.State.CREATED) || screenStackSize < 1) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onAppCreate the app was not yet created or the "
                            + "screen stack was empty state: "
                            + registry.getCurrentState()
                            + ", stack size: " + screenStackSize);
                }
                session.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
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
        dispatchCallFromHost(
                getCurrentLifecycle(), callback,
                "onAppStart", () -> {
                    requireNonNull(mCurrentSession).handleLifecycleEvent(Lifecycle.Event.ON_START);
                    return null;
                });
    }

    @Override
    public void onAppResume(IOnDoneCallback callback) {
        dispatchCallFromHost(
                getCurrentLifecycle(), callback,
                "onAppResume", () -> {
                    requireNonNull(mCurrentSession).handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
                    return null;
                });
    }

    @Override
    public void onAppPause(IOnDoneCallback callback) {
        dispatchCallFromHost(
                getCurrentLifecycle(), callback, "onAppPause",
                () -> {
                    requireNonNull(mCurrentSession).handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
                    return null;
                });
    }

    @Override
    public void onAppStop(IOnDoneCallback callback) {
        dispatchCallFromHost(
                getCurrentLifecycle(), callback, "onAppStop",
                () -> {
                    requireNonNull(mCurrentSession).handleLifecycleEvent(Lifecycle.Event.ON_STOP);
                    return null;
                });
    }

    @Override
    public void onNewIntent(Intent intent, IOnDoneCallback callback) {
        dispatchCallFromHost(
                getCurrentLifecycle(),
                callback,
                "onNewIntent",
                () -> {
                    onNewIntentInternal(requireNonNull(mCurrentSession), intent);
                    return null;
                });
    }

    @Override
    public void onConfigurationChanged(Configuration configuration,
            IOnDoneCallback callback) {
        dispatchCallFromHost(
                getCurrentLifecycle(),
                callback,
                "onConfigurationChanged",
                () -> {
                    onConfigurationChangedInternal(requireNonNull(mCurrentSession),
                            configuration);
                    return null;
                });
    }

    @Override
    public void getManager(@CarContext.CarServiceType @NonNull String type,
            IOnDoneCallback callback) {
        ThreadUtils.runOnMain(() -> {
            Session session = requireNonNull(mCurrentSession);
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
            CarAppService service = requireNonNull(mService);
            RemoteUtils.sendSuccessResponseToHost(
                    callback, "getAppInfo", service.getAppInfo());
        } catch (IllegalArgumentException e) {
            // getAppInfo() could fail with the specified API version is invalid.
            RemoteUtils.sendFailureResponseToHost(callback, "getAppInfo", e);
        }
    }

    @Override
    public void onHandshakeCompleted(Bundleable handshakeInfo,
            IOnDoneCallback callback) {
        CarAppService service = requireNonNull(mService);
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

            AppInfo appInfo = service.getAppInfo();
            int appMinApiLevel = appInfo.getMinCarAppApiLevel();
            int appMaxApiLevel = appInfo.getLatestCarAppApiLevel();
            int hostApiLevel = deserializedHandshakeInfo.getHostCarAppApiLevel();
            if (appMinApiLevel > hostApiLevel) {
                RemoteUtils.sendFailureResponseToHost(callback, "onHandshakeCompleted",
                        new IllegalArgumentException(
                                "Host API level (" + hostApiLevel + ") is "
                                        + "less than the app's min API level ("
                                        + appMinApiLevel + ")"));
                return;
            }
            if (appMaxApiLevel < hostApiLevel) {
                RemoteUtils.sendFailureResponseToHost(callback, "onHandshakeCompleted",
                        new IllegalArgumentException(
                                "Host API level (" + hostApiLevel + ") is "
                                        + "greater than the app's max API level ("
                                        + appMaxApiLevel + ")"));
                return;
            }

            service.setHostInfo(hostInfo);
            mHandshakeInfo = deserializedHandshakeInfo;
            RemoteUtils.sendSuccessResponseToHost(callback, "onHandshakeCompleted",
                    null);
        } catch (BundlerException | IllegalArgumentException e) {
            service.setHostInfo(null);
            RemoteUtils.sendFailureResponseToHost(callback, "onHandshakeCompleted", e);
        }
    }

    private @Nullable Lifecycle getCurrentLifecycle() {
        return mCurrentSession == null ? null : mCurrentSession.getLifecycle();
    }

    private HostValidator getHostValidator() {
        if (mHostValidator == null) {
            mHostValidator = requireNonNull(mService).createHostValidator();
        }
        return mHostValidator;
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

    void onAutoDriveEnabled() {
        Session session = mCurrentSession;
        if (session != null) {
            session.getCarContext().getCarService(
                    NavigationManager.class).onAutoDriveEnabled();
        }
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

    @VisibleForTesting
    @Nullable HandshakeInfo getHandshakeInfo() {
        return mHandshakeInfo;
    }

    @VisibleForTesting
    @Nullable CarAppService getCarAppService() {
        return mService;
    }

    @Nullable Session getCurrentSession() {
        return mCurrentSession;
    }

    @NonNull SessionInfo getCurrentSessionInfo() {
        return mCurrentSessionInfo;
    }
}
