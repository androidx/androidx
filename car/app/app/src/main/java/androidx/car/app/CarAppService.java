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

import static androidx.car.app.utils.ThreadUtils.runOnMain;

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
import androidx.car.app.CarContext.CarServiceType;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.utils.RemoteUtils;
import androidx.car.app.utils.ThreadUtils;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;
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
 * the {@link #SERVICE_INTERFACE} action. For example:
 *
 * <pre>{@code
 * <service
 *   android:name=".YourAppService"
 *   android:exported="true">
 *   <intent-filter>
 *     <action android:name="androidx.car.app.CarAppService" />
 *   </intent-filter>
 * </service>
 * }</pre>
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
// This lint warning is triggered because this has a finish() API. Suppress because we are not
// actually cleaning any held resources in that method.
@SuppressWarnings("NotCloseable")
public abstract class CarAppService extends Service implements LifecycleOwner {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE = "androidx.car.app.CarAppService";

    private static final String TAG = "CarAppService";
    private static final String AUTO_DRIVE = "AUTO_DRIVE";

    @SuppressWarnings({"argument.type.incompatible", "assignment.type.incompatible"})
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    @SuppressWarnings("argument.type.incompatible")
    private final CarContext mCarContext = CarContext.create(mRegistry);

    @Nullable
    HostInfo mHostInfo;

    /**
     * Handles the host binding to this car app.
     *
     * <p>This method is final to ensure this car app's lifecycle is handled properly.
     *
     * <p>Use {@link #onCreateScreen} and {@link #onNewIntent} instead to handle incoming {@link
     * Intent}s.
     */
    @Override
    @CallSuper
    @Nullable
    public IBinder onBind(@NonNull Intent intent) {
        return mBinder;
    }

    /**
     * Handles the host unbinding from this car app.
     *
     * <p>This method is final to ensure this car app's lifecycle is handled properly.
     *
     * <p>Use {@link #onCarAppFinished} instead.
     */
    @Override
    public final boolean onUnbind(@NonNull Intent intent) {
        Log.d(TAG, "onUnbind intent: " + intent);
        runOnMain(() -> {
            // Stop the car app
            mRegistry.handleLifecycleEvent(Event.ON_STOP);

            // Stop any active navigation
            mCarContext.getCarService(NavigationManager.class).stopNavigation();

            // Destroy all screens in the stack
            mCarContext.getCarService(ScreenManager.class).destroyAndClearScreenStack();

            // Remove binders to the host
            mCarContext.resetHosts();

            // Notify the app that the host has unbinded so that it may treat it similar
            // to destroy
            onCarAppFinished();
        });

        // Return true to request an onRebind call.  This means that the process will cache this
        // instance of the Service to return on future bind calls.
        Log.d(TAG, "onUnbind completed");
        return true;
    }

    /**
     * Handles the system destroying this {@link CarAppService}.
     *
     * <p>This method is final to ensure this car app's lifecycle is handled properly.
     *
     * <p>Use a {@link androidx.lifecycle.LifecycleObserver} to observe this car app's {@link
     * Lifecycle}.
     *
     * @see #getLifecycle
     */
    @Override
    public final void onDestroy() {
        Log.d(TAG, "onDestroy");
        runOnMain(
                () -> {
                    mRegistry.handleLifecycleEvent(Event.ON_DESTROY);
                });
        super.onDestroy();
        Log.d(TAG, "onDestroy completed");
    }

    /**
     * Notifies that this car app has finished and should be treated as if it is destroyed.
     *
     * <p>The {@link Screen}s in the stack managed by the {@link ScreenManager} are now all
     * destroyed and removed from the screen stack.
     *
     * <p>{@link #onCreateScreen} will be called if the user reopens the app before the system has
     * destroyed it.
     *
     * <p>For the purposes of the app's lifecycle, you should perform similar destroy functions that
     * you would when this instance's {@link Lifecycle} becomes {@link Lifecycle.State#DESTROYED}.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @see #getLifecycle
     */
    public void onCarAppFinished() {
    }

    /**
     * Requests to finish the car app.
     *
     * <p>Call this when your app is done and should be closed.
     *
     * <p>At some point after this call {@link #onCarAppFinished} will be called, and eventually the
     * system will destroy this {@link CarAppService}.
     */
    public void finish() {
        mCarContext.finishCarApp();
    }

    /**
     * Returns the {@link CarContext} for this car app.
     *
     * <p><b>The {@link CarContext} is not fully initialized until this car app's {@link
     * Lifecycle.State} is at least {@link Lifecycle.State#CREATED}</b>
     *
     * @see #getLifecycle
     */
    @NonNull
    public final CarContext getCarContext() {
        return mCarContext;
    }

    /**
     * Requests the first {@link Screen} for the application.
     *
     * <p>This method is invoked when this car app is first opened by the user.
     *
     * <p>Once the method returns, {@link Screen#onGetTemplate} will be called on the {@link Screen}
     * returned, and the app will be displayed on the car screen.
     *
     * <p>To pre-seed a back stack, you can push {@link Screen}s onto the stack, via {@link
     * ScreenManager#push} during this method call.
     *
     * <p>This method is invoked the first time the app is started, or if this {@link CarAppService}
     * instance has been previously finished and the system has not yet destroyed this car app (See
     * {@link #onCarAppFinished}).
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @param intent the intent that was used to start this app. If the app was started with a
     *               call to {@link CarContext#startCarApp}, this intent will be equal to the
     *               intent passed to that method.
     * @see CarContext#startCarApp
     */
    @NonNull
    public abstract Screen onCreateScreen(@NonNull Intent intent);

    /**
     * Notifies that the car app has received a new {@link Intent}.
     *
     * <p>Once the method returns, {@link Screen#onGetTemplate} will be called on the {@link Screen}
     * that is on top of the {@link Screen} stack managed by the {@link ScreenManager}, and the app
     * will be displayed on the car screen.
     *
     * <p>In contrast to {@link #onCreateScreen}, this method is invoked when the app has already
     * been launched and has not been finished.
     *
     * <p>Often used to update the current {@link Screen} or pushing a new one on the stack,
     * based off of the information in the {@code intent}.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @param intent the intent that was used to start this app. If the app was started with a
     *               call to {@link CarContext#startCarApp}, this intent will be equal to the
     *               intent passed to that method.
     * @see CarContext#startCarApp
     */
    public void onNewIntent(@NonNull Intent intent) {
    }

    /**
     * Notifies that the {@link CarContext}'s {@link Configuration} has changed.
     *
     * <p>At the time that this function is called, the {@link CarContext}'s resources object will
     * have been updated to return resource values matching the new configuration.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @see CarContext
     */
    public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
    }

    /**
     * Returns the {@link CarAppService}'s {@link Lifecycle}.
     *
     * <p>Here are some of the ways you can use the car app's {@link Lifecycle}:
     *
     * <ul>
     *   <li>Observe its {@link Lifecycle} by calling {@link Lifecycle#addObserver}. You can use the
     *       {@link androidx.lifecycle.LifecycleObserver} to take specific actions whenever the
     *       {@link Screen} receives different {@link Event}s.
     *   <li>Use this {@link CarAppService} to observe {@link androidx.lifecycle.LiveData}s that
     *       may drive the backing data for your application.
     * </ul>
     *
     * <p>What each lifecycle related event means for a car app:
     *
     * <dl>
     *   <dt>{@link Event#ON_CREATE}
     *   <dd>The car app has just been launched, and this car app is being initialized. {@link
     *       #onCreateScreen} will be called at a point after this call.
     *   <dt>{@link #onCreateScreen}
     *   <dd>The host is ready for this car app to create the first {@link Screen} so that it can
     *       display its template.
     *   <dt>{@link Event#ON_START}
     *   <dd>The application is now visible in the car screen.
     *   <dt>{@link Event#ON_RESUME}
     *   <dd>The user can now interact with this application.
     *   <dt>{@link Event#ON_PAUSE}
     *   <dd>The user can no longer interact with this application.
     *   <dt>{@link Event#ON_STOP}
     *   <dd>The application is no longer visible.
     *   <dt>{@link #onCarAppFinished}
     *   <dd>Either this car app has requested to be finished (see {@link #finish}), or the host has
     *       finished this car app. Unless this is a navigation app, after a period of time that the
     *       app is no longer displaying in the car, the host may finish this car app.
     *   <dt>{@link Event#ON_DESTROY}
     *   <dd>The OS has now destroyed this {@link CarAppService} instance, and it is no longer
     *       valid.
     * </dl>
     *
     * <p>Listeners that are added in {@link Event#ON_START}, should be removed in {@link
     * Event#ON_STOP}.
     *
     * <p>Listeners that are added in {@link Event#ON_CREATE} should be removed in {@link
     * Event#ON_DESTROY}.
     *
     * @see androidx.lifecycle.LifecycleObserver
     */
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    @Override
    @CallSuper
    public void dump(@NonNull FileDescriptor fd, @NonNull PrintWriter writer,
            @Nullable String[] args) {
        super.dump(fd, writer, args);

        for (String arg : args) {
            if (AUTO_DRIVE.equals(arg)) {
                Log.d(TAG, "Executing onAutoDriveEnabled");
                runOnMain(mCarContext.getCarService(NavigationManager.class)::onAutoDriveEnabled);
            }
        }
    }

    /**
     * Retrieves information about the host attached to this service.
     *
     * @see HostInfo
     */
    @Nullable
    public HostInfo getHostInfo() {
        return mHostInfo;
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
                    Log.d(TAG, "onAppCreate intent: " + intent);
                    RemoteUtils.dispatchHostCall(() -> {
                        // CarContext is not set up until the base Context is attached. First
                        // thing we need to do here is attach the base Context, so that any usage of
                        // it works after this point.
                        CarContext carContext = getCarContext();
                        carContext.attachBaseContext(CarAppService.this, configuration);
                        carContext.setCarHost(carHost);

                        // Whenever the host unbinds, the screens in the stack are destroyed.  If
                        // there is another bind, before the OS has destroyed this Service, then
                        // the stack will be empty, and we need to treat it as a new instance.
                        LifecycleRegistry registry = (LifecycleRegistry) getLifecycle();
                        Lifecycle.State state = registry.getCurrentState();
                        int screenStackSize = carContext.getCarService(
                                ScreenManager.class).getScreenStack().size();
                        if (!state.isAtLeast(State.CREATED) || screenStackSize < 1) {
                            Log.d(TAG, "onAppCreate the app was not yet created or the "
                                    + "screen stack was empty state: "
                                    + registry.getCurrentState()
                                    + ", stack size: " + screenStackSize);
                            registry.handleLifecycleEvent(Event.ON_CREATE);
                            carContext.getCarService(ScreenManager.class).push(
                                    onCreateScreen(intent));
                        } else {
                            Log.d(TAG, "onAppCreate the app was already created");
                            onNewIntentInternal(intent);
                        }
                    }, callback, "onAppCreate");
                    Log.d(TAG, "onAppCreate completed");
                }

                @Override
                public void onAppStart(IOnDoneCallback callback) {
                    RemoteUtils.dispatchHostCall(
                            () -> ((LifecycleRegistry) getLifecycle()).handleLifecycleEvent(
                                    Event.ON_START), callback, "onAppStart");
                }

                @Override
                public void onAppResume(IOnDoneCallback callback) {
                    RemoteUtils.dispatchHostCall(
                            () -> ((LifecycleRegistry) getLifecycle()).handleLifecycleEvent(
                                    Event.ON_RESUME), callback, "onAppResume");
                }

                @Override
                public void onAppPause(IOnDoneCallback callback) {
                    RemoteUtils.dispatchHostCall(
                            () -> ((LifecycleRegistry) getLifecycle()).handleLifecycleEvent(
                                    Event.ON_PAUSE), callback, "onAppPause");
                }

                @Override
                public void onAppStop(IOnDoneCallback callback) {
                    RemoteUtils.dispatchHostCall(
                            () -> ((LifecycleRegistry) getLifecycle()).handleLifecycleEvent(
                                    Event.ON_STOP), callback, "onAppStop");
                }

                @Override
                public void onNewIntent(Intent intent, IOnDoneCallback callback) {
                    RemoteUtils.dispatchHostCall(() -> onNewIntentInternal(intent), callback,
                            "onNewIntent");
                }

                @Override
                public void onConfigurationChanged(Configuration configuration,
                        IOnDoneCallback callback) {
                    RemoteUtils.dispatchHostCall(
                            () -> onConfigurationChangedInternal(configuration),
                            callback,
                            "onConfigurationChanged");
                }

                @Override
                public void getManager(@CarServiceType @NonNull String type,
                        IOnDoneCallback callback) {
                    switch (type) {
                        case CarContext.APP_SERVICE:
                            RemoteUtils.sendSuccessResponse(
                                    callback,
                                    "getManager",
                                    getCarContext().getCarService(
                                            AppManager.class).getIInterface());
                            return;
                        case CarContext.NAVIGATION_SERVICE:
                            RemoteUtils.sendSuccessResponse(
                                    callback,
                                    "getManager",
                                    mCarContext.getCarService(
                                            NavigationManager.class).getIInterface());
                            return;
                        default:
                            Log.e(TAG, type + "%s is not a valid manager");
                            RemoteUtils.sendFailureResponse(callback, "getManager",
                                    new InvalidParameterException(
                                            type + " is not a valid manager type"));
                    }
                }

                @Override
                public void getCarAppVersion(IOnDoneCallback callback) {
                    RemoteUtils.sendSuccessResponse(
                            callback, "getCarAppVersion", CarAppVersion.INSTANCE.toString());
                }

                @Override
                public void onHandshakeCompleted(Bundleable handshakeInfo,
                        IOnDoneCallback callback) {
                    try {
                        HandshakeInfo deserializedHandshakeInfo =
                                (HandshakeInfo) handshakeInfo.get();
                        String packageName = deserializedHandshakeInfo.getHostPackageName();
                        int uid = Binder.getCallingUid();
                        mHostInfo = new HostInfo(packageName, uid);
                    } catch (BundlerException e) {
                        mHostInfo = null;
                    }
                    RemoteUtils.sendSuccessResponse(callback, "onHandshakeCompleted", null);
                }

                // call to onNewIntent(android.content.Intent) not allowed on the given receiver.
                @SuppressWarnings("nullness:method.invocation.invalid")
                @MainThread
                private void onNewIntentInternal(Intent intent) {
                    ThreadUtils.checkMainThread();

                    CarAppService.this.onNewIntent(intent);
                }

                // call to onCarConfigurationChanged(android.content.res.Configuration) not
                // allowed on the given receiver.
                @SuppressWarnings("nullness:method.invocation.invalid")
                @MainThread
                private void onConfigurationChangedInternal(Configuration configuration) {
                    ThreadUtils.checkMainThread();
                    Log.d(TAG, "onCarConfigurationChanged configuration: " + configuration);

                    getCarContext().onCarConfigurationChanged(configuration);
                    onCarConfigurationChanged(getCarContext().getResources().getConfiguration());
                }
            };
}
