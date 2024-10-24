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

import androidx.car.app.Session;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.jspecify.annotations.NonNull;

/**
 * {@link SessionController} provides API that allows testing of a {@link Session}.
 *
 * <p>This controller allows:
 *
 * <ul>
 *   <li>Injecting a {@link TestCarContext} into the {@link Session} instance, which provides access
 *   to the test managers and other testing functionalities.
 * </ul>
 */
public class SessionController {
    final Session mSession;
    final TestCarContext mTestCarContext;
    final Intent mIntent;
    private final TestLifecycleOwner mLifecycleOwner;

    /**
     * Creates a {@link SessionController} to control the provided {@link Session}.
     *
     * @param session the {@link Session} to control
     * @param context the {@link TestCarContext} that the {@code session} should use.
     * @param intent  the {@link Intent} that the {@code session} should start with during the
     *                {@link androidx.lifecycle.Lifecycle.State#CREATED} state.
     * @throws NullPointerException if {@code session} or {@code context} is {@code null}
     */
    public SessionController(@NonNull Session session, @NonNull TestCarContext context,
            @NonNull Intent intent) {
        mSession = requireNonNull(session);
        mTestCarContext = requireNonNull(context);
        mIntent = requireNonNull(intent);

        mLifecycleOwner = new TestLifecycleOwner();
        mLifecycleOwner.getRegistry().addObserver(new SessionLifecycleObserver());

        mSession.setCarContextInternal(mTestCarContext);
        mSession.setLifecycleRegistryInternal(mTestCarContext.getLifecycleOwner().getRegistry());
    }

    /** Returns the {@link Session} that is being controlled. */
    public @NonNull Session getSession() {
        return mSession;
    }

    /**
     * Moves the {@link Session} being controlled to the input {@code state}.
     *
     * <p>Note that {@link Lifecycle.State#DESTROYED} is a terminal state, and you cannot move to
     * any other state after the {@link Session} reaches that state.</p>
     *
     * @see Session#getLifecycle
     */
    public @NonNull SessionController moveToState(Lifecycle.@NonNull State state) {
        mLifecycleOwner.getRegistry().setCurrentState(state);
        return this;
    }

    /**
     * A helper class to forward the lifecycle events from this controller to the session.
     */
    class SessionLifecycleObserver implements DefaultLifecycleObserver {
        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {
            LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
            TestScreenManager screenManager = mTestCarContext.getCarService(
                    TestScreenManager.class);
            registry.handleLifecycleEvent(Event.ON_CREATE);
            screenManager.push(mSession.onCreateScreen(mIntent));
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
            registry.handleLifecycleEvent(Event.ON_START);
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
            registry.handleLifecycleEvent(Event.ON_RESUME);
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
            registry.handleLifecycleEvent(Event.ON_PAUSE);
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
            registry.handleLifecycleEvent(Event.ON_STOP);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            LifecycleRegistry registry = (LifecycleRegistry) mSession.getLifecycle();
            registry.handleLifecycleEvent(Event.ON_DESTROY);
        }
    }
}
