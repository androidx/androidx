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

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.utils.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.notification.CarPendingIntent;
import androidx.car.app.utils.RemoteUtils;
import androidx.car.app.utils.ThreadUtils;
import androidx.car.app.versioning.CarAppApiLevel;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * The CarContext class is a {@link ContextWrapper} subclass accessible to your {@link
 * CarAppService} and {@link Screen} instances, which provides access to car services such as the
 * {@link ScreenManager} for managing the screen stack, the {@link AppManager} for general
 * app-related functionality such as accessing a surface for drawing your navigation app's map, and
 * the {@link NavigationManager} used by turn-by-turn navigation apps to communicate navigation
 * metadata and other navigation-related events with the host. See Access the navigation templates
 * for a comprehensive list of library functionality available to navigation apps.
 *
 * <p>Whenever you use a CarContext to load resources, the following configuration elements come
 * from the car screen's configuration, and not the phone:
 *
 * <ul>
 *   <li>Screen width.
 *   <li>Screen height.
 *   <li>Screen pixel density (DPI).
 *   <li>Night mode (See {@link #isDarkMode}).
 * </ul>
 *
 * <p>Please refer <a
 * href="https://developer.android.com/guide/topics/resources/providing-resources">here</a>, on how
 * to use configuration qualifiers in your resources.
 *
 * @see #getCarService
 */
public class CarContext extends ContextWrapper {
    /**
     * Represents the types of services for client-host communication.
     *
     * @hide
     */
    @StringDef({APP_SERVICE, CAR_SERVICE, NAVIGATION_SERVICE, SCREEN_SERVICE, CONSTRAINT_SERVICE})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface CarServiceType {
    }

    /** Manages all app events such as invalidating the UI, showing a toast, etc. */
    public static final String APP_SERVICE = "app";

    /**
     * Manages all navigation events such as starting navigation when focus is granted, abandoning
     * navigation when focus is lost, etc.
     */
    public static final String NAVIGATION_SERVICE = "navigation";

    /** Manages the screens of the app, including the screen stack. */
    public static final String SCREEN_SERVICE = "screen";

    /** Manages constraints for the app as enforced by the connected host. */
    @RequiresCarApi(2)
    public static final String CONSTRAINT_SERVICE = "constraints";

    /**
     * Internal usage only. Top level binder to host.
     */
    public static final String CAR_SERVICE = "car";

    /**
     * Key for including a IStartCarApp in the notification {@link Intent}, for starting the app
     * if it has not been opened yet.
     */
    public static final String EXTRA_START_CAR_APP_BINDER_KEY = "androidx.car.app.extra"
            + ".START_CAR_APP_BINDER_KEY";

    /**
     * Standard action for navigating to a location.
     *
     * <p>Used as the {@link Intent}'s action for starting a navigation via
     * {@link #startCarApp(Intent)}.
     */
    public static final String ACTION_NAVIGATE = "androidx.car.app.action.NAVIGATE";

    /**
     * Action for requesting permissions on the car app activity.
     */
    static final String REQUEST_PERMISSIONS_ACTION = "androidx.car.app.action.REQUEST_PERMISSIONS";

    /**
     * Key for string array extra for permissions to request.
     */
    static final String EXTRA_PERMISSIONS_KEY = "androidx.car.app.action.EXTRA_PERMISSIONS_KEY";

    /**
     * Key for binder extra for permission result callback.
     */
    static final String EXTRA_ON_REQUEST_PERMISSIONS_RESULT_CALLBACK_KEY =
            "androidx.car.app.action.EXTRA_ON_REQUEST_PERMISSIONS_RESULT_CALLBACK_KEY";

    private final AppManager mAppManager;
    private final NavigationManager mNavigationManager;
    private final ScreenManager mScreenManager;
    private final ConstraintManager mConstraintManager;
    private final OnBackPressedDispatcher mOnBackPressedDispatcher;
    private final HostDispatcher mHostDispatcher;
    private final Lifecycle mLifecycle;

    /** API level, updated once host connection handshake is completed. */
    @CarAppApiLevel
    private int mCarAppApiLevel = CarAppApiLevels.UNKNOWN;

    /** @hide */
    @NonNull
    @RestrictTo(LIBRARY)
    public static CarContext create(@NonNull Lifecycle lifecycle) {
        return new CarContext(lifecycle, new HostDispatcher());
    }

    /**
     * Provides a car service by name.
     *
     * <p>The class of the returned object varies by the requested name.
     *
     * <p>Currently supported car services, and their respective classes, are:
     *
     * <dl>
     *   <dt>{@link #APP_SERVICE}
     *   <dd>An {@link AppManager} for communication between the app and the host.
     *   <dt>{@link #NAVIGATION_SERVICE}
     *   <dd>A {@link NavigationManager} for management of navigation updates.
     *   <dt>{@link #SCREEN_SERVICE}
     *   <dd>A {@link ScreenManager} for management of {@link Screen}s.
     * </dl>
     *
     * @param name The name of the car service requested. This should be one of
     *             {@link #APP_SERVICE},
     *             {@link #NAVIGATION_SERVICE} or {@link #SCREEN_SERVICE}
     * @return The car service instance
     * @throws IllegalArgumentException if {@code name} does not refer to a valid car service
     * @throws NullPointerException     if {@code name} is {@code null}
     */
    // This is kept for the testing library.
    @NonNull
    public Object getCarService(@CarServiceType @NonNull String name) {
        switch (requireNonNull(name)) {
            case APP_SERVICE:
                return mAppManager;
            case NAVIGATION_SERVICE:
                return mNavigationManager;
            case SCREEN_SERVICE:
                return mScreenManager;
            case CONSTRAINT_SERVICE:
                return mConstraintManager;
            default: // fall out
        }

        throw new IllegalArgumentException(
                "The name '" + name + "' does not correspond to a car service");
    }

    /**
     * Returns the a car service, by class.
     *
     * <p>Currently supported classes are: {@link AppManager}, {@link NavigationManager}, {@link
     * ScreenManager}.
     *
     * @param serviceClass the class of the requested service
     * @throws IllegalArgumentException if {@code serviceClass} is not the class of a supported car
     *                                  service
     * @throws NullPointerException     if {@code serviceClass} is {@code null}
     */
    @NonNull
    public <T> T getCarService(@NonNull Class<T> serviceClass) {
        return requireNonNull(serviceClass).cast(getCarService(getCarServiceName(serviceClass)));
    }

    /**
     * Gets the name of the car service that is represented by the specified class.
     *
     * @param serviceClass the class of the requested service
     * @return the car service name to use with {@link #getCarService(String)}
     * @throws IllegalArgumentException if {@code serviceClass} is not the class of a supported car
     *                                  service
     * @throws NullPointerException     if {@code serviceClass} is {@code null}
     * @see #getCarService
     */
    @NonNull
    @CarServiceType
    public String getCarServiceName(@NonNull Class<?> serviceClass) {
        if (requireNonNull(serviceClass).isInstance(mAppManager)) {
            return APP_SERVICE;
        } else if (serviceClass.isInstance(mNavigationManager)) {
            return NAVIGATION_SERVICE;
        } else if (serviceClass.isInstance(mScreenManager)) {
            return SCREEN_SERVICE;
        } else if (serviceClass.isInstance(mConstraintManager)) {
            return CONSTRAINT_SERVICE;
        }

        throw new IllegalArgumentException("The class does not correspond to a car service");
    }

    /**
     * Starts a car app on the car screen.
     *
     * <p>The target application will get the {@link Intent} via {@link Session#onCreateScreen}
     * or {@link Session#onNewIntent}.
     *
     * <p>Supported {@link Intent}s:
     *
     * <dl>
     *   <dt>An {@link Intent} to navigate.
     *   <dd>The action must be {@link #ACTION_NAVIGATE}.
     *   <dd>The data URI scheme must be either a latitude,longitude pair, or a + separated string
     *       query as follows:
     *   <dd>1) "geo:12.345,14.8767" for a latitude, longitude pair.
     *   <dd>2) "geo:0,0?q=123+Main+St,+Seattle,+WA+98101" for an address.
     *   <dd>3) "geo:0,0?q=a+place+name" for a place to search for.
     *   <dt>An {@link Intent} to make a phone call.
     *   <dd>The {@link Intent} must be created as defined <a
     *       href="https://developer.android
     *       .com/guide/components/intents-common#DialPhone">here</a>.
     *   <dt>An {@link Intent} to start this app in the car.
     *   <dd>The component name of the intent must be the one for the {@link CarAppService} that
     *       contains this {@link CarContext}. If the component name is for a different
     *       component, the
     *       method will throw a {@link SecurityException}.
     * </dl>
     *
     * @param intent the {@link Intent} to send to the target application
     * @throws SecurityException         if the app attempts to start a different app explicitly or
     *                                   does not have permissions for the requested action
     * @throws HostException             if the remote call fails
     * @throws NullPointerException      if {@code intent} is {@code null}
     */
    public void startCarApp(@NonNull Intent intent) {
        requireNonNull(intent);

        mHostDispatcher.dispatch(
                CarContext.CAR_SERVICE,
                "startCarApp", (ICarHost host) -> {
                    host.startCarApp(intent);
                    return null;
                }
        );
    }

    /**
     * Starts the car app on the car screen.
     *
     * <p>Use this method if the app has received a broadcast due to a notification action.
     *
     * @param notificationIntent the {@link Intent} that the app received via broadcast due to a
     *                           user taking an action on a notification in the car
     * @param appIntent          the {@link Intent} to use for starting the car app. See {@link
     *                           #startCarApp(Intent)} for the documentation on valid
     *                           {@link Intent}s
     * @throws InvalidParameterException if {@code notificationIntent} is not an {@link Intent}
     *                                   received from a broadcast, due to an action taken by the
     *                                   user in the car
     * @throws NullPointerException      if either {@code notificationIntent} or {@code appIntent
     *                                   } are {@code null}
     * @deprecated use {@link CarPendingIntent#getCarApp(Context, int, Intent, int)} to create
     * the pending intent for the notification action.  This API will NOT work for Automotive OS.
     */
    @Deprecated
    public static void startCarApp(@NonNull Intent notificationIntent, @NonNull Intent appIntent) {
        requireNonNull(notificationIntent);
        requireNonNull(appIntent);

        IBinder binder = null;
        Bundle extras = notificationIntent.getExtras();
        if (extras != null) {
            binder = extras.getBinder(EXTRA_START_CAR_APP_BINDER_KEY);
        }
        if (binder == null) {
            throw new IllegalArgumentException("Notification intent missing expected extra");
        }

        IStartCarApp startCarAppInterface = requireNonNull(IStartCarApp.Stub.asInterface(binder));

        RemoteUtils.dispatchCallToHost(
                "startCarApp from notification", () -> {
                    startCarAppInterface.startCarApp(appIntent);
                    return null;
                }
        );
    }

    /**
     * Requests to finish the car app.
     *
     * <p>Call this when your app is done and should be closed. The {@link Session} corresponding
     * to this {@link CarContext} will become {@code State.DESTROYED}.
     *
     * <p>At some point after this call, the OS will destroy your {@link CarAppService}.
     */
    public void finishCarApp() {
        mHostDispatcher.dispatch(
                CarContext.CAR_SERVICE,
                "finish", (ICarHost host) -> {
                    host.finish();
                    return null;
                }
        );
    }

    /**
     * Returns {@code true} if the car is set to dark mode.
     *
     * <p>Navigation applications must redraw their map with the proper dark colors when the host
     * determines that conditions warrant it, as signaled by the value returned by this method.
     *
     * <p>Whenever the dark mode status changes, you will receive a call to {@link
     * Session#onCarConfigurationChanged}.
     */
    public boolean isDarkMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Returns the {@link OnBackPressedDispatcher} that will be triggered when the user clicks a
     * back button.
     *
     * <p>The default back press behavior is to call {@link ScreenManager#pop}.
     *
     * <p>To override the default behavior, register a
     * {@link androidx.activity.OnBackPressedCallback} via calling
     * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}. Using
     * the {@link LifecycleOwner} ensures that your callback is only registered while its
     * {@link Lifecycle} is at least {@link Lifecycle.State#STARTED}.
     *
     * <p>If there is a {@link androidx.activity.OnBackPressedCallback} that is added and
     * enabled, and you'd like to remove the top {@link Screen} as a part of the callback, you
     * <b>MUST</b> call {@link ScreenManager#pop} in the callback. The default behavior is
     * overridden when you have a callback enabled.
     */
    @NonNull
    public OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return mOnBackPressedDispatcher;
    }

    /**
     * Retrieves the API level negotiated with the host.
     *
     * <p>API levels are used during client and host connection handshake to negotiate a common set
     * of elements that both processes can understand. Different devices might have different host
     * versions. Each of these hosts will support a range of API levels, as a way to provide
     * backwards compatibility.
     *
     * <p>Applications can also provide forward compatibility, by declaring support for a
     * {@link AppInfo#getMinCarAppApiLevel()} lower than {@link AppInfo#getLatestCarAppApiLevel()}.
     * See {@link AppInfo#getMinCarAppApiLevel()} for more details.
     *
     * <p>Clients must ensure no elements annotated with a {@link RequiresCarApi} value higher
     * than returned by this method is used at runtime.
     *
     * <p>Please refer to {@link RequiresCarApi} description for more details on how to
     * implement forward compatibility.
     *
     * @return a value between {@link AppInfo#getMinCarAppApiLevel()} and
     * {@link AppInfo#getLatestCarAppApiLevel()}. In case of incompatibility, the host will
     * disconnect from the service before completing the handshake
     * @throws IllegalStateException if invoked before the connection handshake with the host has
     *                               been completed (for example, before
     *                               {@link Session#onCreateScreen(Intent)})
     */
    @CarAppApiLevel
    public int getCarAppApiLevel() {
        if (mCarAppApiLevel == CarAppApiLevels.UNKNOWN) {
            throw new IllegalStateException("Car App API level hasn't been established yet");
        }
        return mCarAppApiLevel;
    }

    /**
     * Requests the provided {@code permissions} from the user.
     *
     * <p>When the result is available, the {@code callback} provided will be called on the main
     * thread.
     *
     * <p>This method should be called using a parked only listener.
     *
     * <p>If this method is calle while the host deems it is unsafe (for example, when the user
     * is driving), the permission(s) may not be requested from the user, automatically rejecting
     * the permissions requested.
     *
     * <p>If the Session is destroyed before the user accepts or rejects the permissions, the
     * callback will not be executed.
     *
     * @param permissions the runtime permissions to request from the user
     * @param callback    callback that will be notified when the user takes action on the
     *                    permission request
     * @throws NullPointerException if either {@code permissions} or {@code callback} are {@code
     *                              null}
     */
    public void requestPermissions(@NonNull List<String> permissions,
            @NonNull OnRequestPermissionsCallback callback) {
        requestPermissions(permissions, ContextCompat.getMainExecutor(this), callback);
    }

    /**
     * Requests the provided {@code permissions} from the user.
     *
     * <p>When the result is available, the {@code callback} provided will be called using the
     * {@link Executor} provided.
     *
     * <p>This method should be called using a parked only listener.
     *
     * <p>If this method is calle while the host deems it is unsafe (for example, when the user
     * is driving), the permission(s) may not be requested from the user, automatically rejecting
     * the permissions requested.
     *
     * <p>If the Session is destroyed before the user accepts or rejects the permissions, the
     * callback will not be executed.
     *
     * @param permissions the runtime permissions to request from the user
     * @param executor    the executor that will be used for calling the {@code callback} provided
     * @param callback    callback that will be notified when the user takes action on the
     *                    permission request
     * @throws NullPointerException if any of {@code executor}, {@code permissions} or
     *                              {@code callback} are {@code null}
     */
    public void requestPermissions(@NonNull List<String> permissions,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull OnRequestPermissionsCallback callback) {
        requireNonNull(executor);
        requireNonNull(permissions);
        requireNonNull(callback);

        ComponentName appActivityComponent = new ComponentName(this, CarAppInternalActivity.class);

        Lifecycle lifecycle = mLifecycle;
        Bundle extras = new Bundle(2);
        extras.putBinder(EXTRA_ON_REQUEST_PERMISSIONS_RESULT_CALLBACK_KEY,
                new IOnRequestPermissionsCallback.Stub() {
                    @Override
                    public void onRequestPermissionsResult(String[] approvedPermissions,
                            String[] rejectedPermissions) {
                        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
                            List<String> approved = Arrays.asList(approvedPermissions);
                            List<String> rejected = Arrays.asList(rejectedPermissions);
                            executor.execute(() -> callback.onRequestPermissionsResult(approved,
                                    rejected));
                        }
                    }
                }.asBinder());
        extras.putStringArray(EXTRA_PERMISSIONS_KEY, permissions.toArray(new String[0]));
        Intent intent =
                new Intent(REQUEST_PERMISSIONS_ACTION).setComponent(appActivityComponent)
                        .putExtras(extras)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Copies the fields from the provided {@link Configuration} into the {@link Configuration}
     * contained in this object.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @MainThread
    @SuppressWarnings("deprecation")
    void onCarConfigurationChanged(@NonNull Configuration configuration) {
        ThreadUtils.checkMainThread();

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG,
                    "Car configuration changed, configuration: " + configuration
                            + ", displayMetrics: "
                            + getResources().getDisplayMetrics());
        }

        getResources()
                .updateConfiguration(requireNonNull(configuration),
                        getResources().getDisplayMetrics());
    }

    /**
     * Updates context information based on the information provided during connection handshake
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @MainThread
    void updateHandshakeInfo(HandshakeInfo handshakeInfo) {
        mCarAppApiLevel = handshakeInfo.getHostCarAppApiLevel();
    }

    /**
     * Attaches the base {@link Context} for this {@link CarContext} by creating a new display
     * context using {@link #createDisplayContext} with a {@link VirtualDisplay} created using
     * the metrics from the provided {@link Configuration}, and then also calling {@link
     * #createConfigurationContext} with the provided {@link Configuration}.
     *
     * <p>This call creates a display context and then a configuration context to ensure that
     * updates to the phone configuration do not update either the {@link Configuration} or {@link
     * android.util.DisplayMetrics} held by this {@link CarContext}'s resources.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @MainThread
    void attachBaseContext(@NonNull Context context, @NonNull Configuration configuration) {
        ThreadUtils.checkMainThread();

        // If this is the first time attaching the base, actually attach it, otherwise, just
        // update the configuration.
        if (getBaseContext() == null) {
            // Create the virtual display with the proper dimensions.
            VirtualDisplay display =
                    ((DisplayManager) requireNonNull(
                            context.getSystemService(Context.DISPLAY_SERVICE)))
                            .createVirtualDisplay(
                                    "CarAppService",
                                    configuration.screenWidthDp,
                                    configuration.screenHeightDp,
                                    configuration.densityDpi,
                                    null,
                                    VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);

            attachBaseContext(
                    context
                            .createDisplayContext(display.getDisplay())
                            .createConfigurationContext(configuration));
        }

        onCarConfigurationChanged(configuration);
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    @MainThread
    void setCarHost(@NonNull ICarHost carHost) {
        ThreadUtils.checkMainThread();
        mHostDispatcher.setCarHost(requireNonNull(carHost));
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    @SuppressWarnings({
            "argument.type.incompatible",
            "method.invocation.invalid"
    }) // @UnderInitialization not available with androidx
    protected CarContext(@NonNull Lifecycle lifecycle, @NonNull HostDispatcher hostDispatcher) {
        super(null);

        mHostDispatcher = hostDispatcher;
        mAppManager = AppManager.create(this, hostDispatcher, lifecycle);
        mNavigationManager = NavigationManager.create(this, hostDispatcher, lifecycle);
        mScreenManager = ScreenManager.create(this, lifecycle);
        mConstraintManager = ConstraintManager.create(this, hostDispatcher);
        mOnBackPressedDispatcher =
                new OnBackPressedDispatcher(() -> getCarService(ScreenManager.class).pop());
        mLifecycle = lifecycle;

        LifecycleObserver observer = new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner owner) {
                hostDispatcher.resetHosts();
                owner.getLifecycle().removeObserver(this);
            }
        };

        lifecycle.addObserver(observer);
    }
}
