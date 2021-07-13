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

package androidx.car.app.testing;

import static java.util.Objects.requireNonNull;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Session;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleRegistry;

import java.lang.reflect.Field;

/**
 * A controller that allows testing of a {@link Session}.
 *
 * <p>This controller allows:
 *
 * <ul>
 *   <li>Injecting a {@link TestCarContext} into the {@link Session} instance, which provides access
 *   to the test managers and other testing functionalities.
 * </ul>
 */
@SuppressWarnings("NotCloseable")
public class SessionController {
    private final Session mSession;
    private final TestCarContext mTestCarContext;

    /**
     * Creates a {@link SessionController} to control the provided {@link Session}.
     *
     * @param session the {@link Session} to control
     * @param context the {@link TestCarContext} that the {@code session} should use.
     * @throws NullPointerException if {@code session} or {@code context} is {@code null}
     */
    public SessionController(@NonNull Session session, @NonNull TestCarContext context) {
        mSession = requireNonNull(session);
        mTestCarContext = requireNonNull(context);

        // Use reflection to inject the TestCarContext into the Session.
        try {
            Field registry = Session.class.getDeclaredField("mRegistry");
            registry.setAccessible(true);
            registry.set(session, mTestCarContext.getLifecycleOwner().mRegistry);

            Field carContext = Session.class.getDeclaredField("mCarContext");
            carContext.setAccessible(true);
            carContext.set(session, mTestCarContext);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to set internal Session values for testing", e);
        }
    }

    /** Returns the {@link Session} that is being controlled. */
    @NonNull
    public Session getSession() {
        return mSession;
    }

    /**
     * Creates the {@link Session} that is being controlled with the given {@code intent}.
     *
     * <p>If this is the first time this is called on the {@link Session}, this would trigger
     * {@link Session#onCreateScreen(Intent)} and transition the lifecycle to the
     * {@link Lifecycle.State#CREATED} state. Otherwise, this will trigger
     * {@link Session#onNewIntent(Intent)}.
     *
     * @see Session#getLifecycle
     */
    @NonNull
    public SessionController create(@NonNull Intent intent) {
        LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
        Lifecycle.State state = registry.getCurrentState();
        TestScreenManager screenManager = mTestCarContext.getCarService(TestScreenManager.class);

        int screenStackSize = screenManager.getScreensPushed().size();
        if (!state.isAtLeast(Lifecycle.State.CREATED) || screenStackSize < 1) {
            registry.handleLifecycleEvent(Event.ON_CREATE);
            screenManager.push(mSession.onCreateScreen(intent));
        } else {
            mSession.onNewIntent(intent);
        }

        return this;
    }

    /**
     * Starts the {@link Session} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    @NonNull
    public SessionController start() {
        LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
        registry.handleLifecycleEvent(Event.ON_START);

        return this;
    }


    /**
     * Resumes the {@link Session} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    @NonNull
    public SessionController resume() {
        LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
        registry.handleLifecycleEvent(Event.ON_RESUME);

        return this;
    }

    /**
     * Pauses the {@link Session} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    @NonNull
    public SessionController pause() {
        LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
        registry.handleLifecycleEvent(Event.ON_PAUSE);

        return this;
    }

    /**
     * Stops the {@link Session} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    @NonNull
    public SessionController stop() {
        LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
        registry.handleLifecycleEvent(Event.ON_STOP);

        return this;
    }

    /**
     * Destroys the {@link Session} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    @NonNull
    public SessionController destroy() {
        LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
        registry.handleLifecycleEvent(Event.ON_DESTROY);

        return this;
    }
}
