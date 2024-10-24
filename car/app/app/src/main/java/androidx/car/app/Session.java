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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * The base class for implementing a session for a car app.
 */
public abstract class Session implements LifecycleOwner {
    /**
     * Master {@link LifecycleRegistry} to use internally within the library.
     *
     * <p>This is used to ensure that the public LifecycleRegistry for the session is also
     * registered first before the library's component such as the {@link ScreenManager}. This
     * guarantees that apps listening to the session's lifecycle will get the events in the correct
     * order (e.g. start and destroy) compared to other artifacts within a session (e.g. screens).
     */
    private LifecycleRegistry mRegistry;
    /**
     * The external {@link LifecycleRegistry} that apps can register observers to.
     */
    final LifecycleRegistry mRegistryPublic;
    private CarContext mCarContext;

    private final LifecycleObserver mLifecycleObserver = new LifecycleObserverImpl();

    public Session() {
        mRegistry = new LifecycleRegistry(this);
        mRegistryPublic = new LifecycleRegistry(this);

        // The order here is important, we need to registry the observer that syncs the public
        // LifecycleRegistry first, because that's the one apps will use to observer lifecycle
        // events related to the Session, and we want them to wrap around the events of everything
        // else that happens within the session (e.g. Screen lifecycles).
        mRegistry.addObserver(mLifecycleObserver);
        mCarContext = CarContext.create(mRegistry);
    }

    /**
     * Requests the first {@link Screen} for the application.
     *
     * <p>Once the method returns, {@link Screen#onGetTemplate()} will be called on the
     * {@link Screen} returned, and the app will be displayed on the car screen.
     *
     * <p>To pre-seed a back stack, you can push {@link Screen}s onto the stack, via {@link
     * ScreenManager#push} during this method call.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @param intent the intent that was used to start this app. If the app was started with a
     *               call to {@link CarContext#startCarApp(Intent)}, this intent will be equal to
     *               the intent passed to that method
     */
    public abstract @NonNull Screen onCreateScreen(@NonNull Intent intent);

    /**
     * Notifies that the car app has received a new {@link Intent}.
     *
     * <p>Once the method returns, {@link Screen#onGetTemplate} will be called on the {@link Screen}
     * that is on top of the {@link Screen} stack managed by the {@link ScreenManager}, and the app
     * will be displayed on the car screen.
     *
     * <p>Often used to update the current {@link Screen} or pushing a new one on the stack,
     * based off of the information in the {@code intent}.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @param intent the intent that was used to start this app. If the app was started with a
     *               call to {@link CarContext#startCarApp(Intent)}, this intent will be equal to
     *               the intent passed to that method
     * @see CarContext#startCarApp(Intent)
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
     * Returns the {@link Session}'s {@link Lifecycle}.
     *
     * <p>Here are some of the ways you can use the sessions's {@link Lifecycle}:
     *
     * <ul>
     *   <li>Observe its {@link Lifecycle} by calling {@link Lifecycle#addObserver}. You can use the
     *       {@link androidx.lifecycle.LifecycleObserver} to take specific actions whenever the
     *       {@link Screen} receives different {@link Lifecycle.Event}s.
     *
     *   <li>Use this {@link CarAppService} to observe {@link androidx.lifecycle.LiveData}s that
     *       may drive the backing data for your application.
     * </ul>
     *
     * <p>What each lifecycle related event means for a session:
     *
     * <dl>
     *   <dt>{@link Lifecycle.Event#ON_CREATE}
     *   <dd>The session has just been launched, and this session is being initialized. {@link
     *       #onCreateScreen} will be called at a point after this call.
     *   <dt>{@link #onCreateScreen}
     *   <dd>The host is ready for this session to create the first {@link Screen} so that it can
     *       display its template.
     *   <dt>{@link Lifecycle.Event#ON_START}
     *   <dd>The application is now visible in the car screen.
     *   <dt>{@link Lifecycle.Event#ON_RESUME}
     *   <dd>The user can now interact with this application.
     *   <dt>{@link Lifecycle.Event#ON_PAUSE}
     *   <dd>The user can no longer interact with this application.
     *   <dt>{@link Lifecycle.Event#ON_STOP}
     *   <dd>The application is no longer visible.
     *   <dt>{@link Lifecycle.Event#ON_DESTROY}
     *   <dd>The OS has now destroyed this {@link Session} instance, and it is no longer
     *       valid.
     * </dl>
     *
     * <p>Listeners that are added in {@link Lifecycle.Event#ON_START}, should be removed in {@link
     * Lifecycle.Event#ON_STOP}.
     *
     * <p>Listeners that are added in {@link Lifecycle.Event#ON_CREATE} should be removed in {@link
     * Lifecycle.Event#ON_DESTROY}.
     *
     * <p>Note lifecycle callbacks will be executed on the main thread.
     *
     * @see androidx.lifecycle.LifecycleObserver
     */
    @Override
    public @NonNull Lifecycle getLifecycle() {
        return mRegistryPublic;
    }

    /**
     * Master {@link LifecycleRegistry} to use internally within the library.
     *
     * <p>This should be used to dispatch lifecycle events which ensures app(s) will receive the
     * events with respect to the {@link Session} and {@link Screen} lifecycles in the correct
     * order.
     */
    @VisibleForTesting
    @NonNull Lifecycle getLifecycleInternal() {
        return mRegistry;
    }

    /**
     * Updates the {@link Session}'s lifecycle.
     */
    void handleLifecycleEvent(Lifecycle.Event event) {
        mRegistry.handleLifecycleEvent(event);
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // used by the testing library.
    public void setLifecycleRegistryInternal(@NonNull LifecycleRegistry registry) {
        mRegistry = registry;
        mRegistry.addObserver(mLifecycleObserver);
    }

    /**
     * Returns the {@link CarContext} for this session.
     *
     * <p><b>The {@link CarContext} is not expected to be available until this session's {@link
     * Lifecycle.State} is at least {@link Lifecycle.State#CREATED}</b>. Further, some instance
     * methods within {@link CarContext} should not be called before this state has been reached.
     * See the documentation in {@link CarContext} for details on any such restrictions.
     *
     * @see #getLifecycle
     */
    public final @NonNull CarContext getCarContext() {
        return Objects.requireNonNull(mCarContext);
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // used by the testing library.
    public void setCarContextInternal(@NonNull CarContext carContext) {
        mCarContext = carContext;
    }

    /**
     * Updates the {@link Session} with the given parameters.
     *
     * <p>This should be invoked during onAppCreate to initialize the {@link Session} and its
     * underlying {@link Context} properly.
     */
    void configure(@NonNull Context baseContext,
            @NonNull HandshakeInfo handshakeInfo,
            @NonNull HostInfo hostInfo,
            @NonNull ICarHost carHost,
            @NonNull Configuration configuration) {
        mCarContext.updateHandshakeInfo(handshakeInfo);
        mCarContext.updateHostInfo(hostInfo);
        mCarContext.attachBaseContext(baseContext, configuration);
        mCarContext.setCarHost(carHost);
    }

    /**
     * Updates the {@link CarContext}'s configuration with the new one and notifies the
     * app that it has changed.
     */
    void onCarConfigurationChangedInternal(@NonNull Configuration newConfiguration) {
        mCarContext.onCarConfigurationChanged(newConfiguration);
        onCarConfigurationChanged(mCarContext.getResources().getConfiguration());
    }

    /** A lifecycle observer implementation that forwards events to the screens in the stack. */
    class LifecycleObserverImpl implements DefaultLifecycleObserver {
        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {
            mRegistryPublic.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            mRegistryPublic.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            mRegistryPublic.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            mRegistryPublic.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            mRegistryPublic.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            mRegistryPublic.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
            owner.getLifecycle().removeObserver(this);
        }
    }
}
