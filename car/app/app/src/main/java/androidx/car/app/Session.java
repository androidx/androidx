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

import android.content.Intent;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * The base class for implementing a session for a car app.
 */
public abstract class Session implements LifecycleOwner {
    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);
    private final CarContext mCarContext = CarContext.create(mRegistry);

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
     *               call to {@link CarContext#startCarApp}, this intent will be equal to the
     *               intent passed to that method
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
     * <p>Often used to update the current {@link Screen} or pushing a new one on the stack,
     * based off of the information in the {@code intent}.
     *
     * <p>Called by the system, do not call this method directly.
     *
     * @param intent the intent that was used to start this app. If the app was started with a
     *               call to {@link CarContext#startCarApp}, this intent will be equal to the
     *               intent passed to that method
     *
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
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }

    /**
     * Returns the {@link CarContext} for this session.
     *
     * <p><b>The {@link CarContext} is not fully initialized until this session's {@link
     * Lifecycle.State} is at least {@link Lifecycle.State#CREATED}</b>
     *
     * @see #getLifecycle
     */
    @NonNull
    public final CarContext getCarContext() {
        return mCarContext;
    }
}
