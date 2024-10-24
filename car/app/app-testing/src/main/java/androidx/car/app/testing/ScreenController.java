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

import android.util.Pair;

import androidx.car.app.Screen;
import androidx.car.app.model.Template;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ScreenController} provides API that allows testing of a {@link Screen}.
 *
 * <p>This controller will allows:
 *
 * <ul>
 *   <li>Moving a {@link Screen} through its different {@link State}s.
 *   <li>Retrieving all {@link Template}s returned from {@link Screen#onGetTemplate}. The values can
 *       be reset with {@link #reset}.
 * </ul>
 */
public class ScreenController {
    private final TestCarContext mTestCarContext;
    private final Screen mScreen;
    private final TestLifecycleOwner mLifecycleOwner;

    /**
     * Creates a ScreenController to control a {@link Screen} for testing.
     *
     * @throws NullPointerException     if {@code screen} is null
     * @throws IllegalArgumentException if {@code screen} was not created with a
     *                                  {@link TestCarContext}
     */
    public ScreenController(@NonNull Screen screen) {
        mScreen = requireNonNull(screen);
        TestCarContext carContext = (TestCarContext) mScreen.getCarContext();
        if (carContext == null) {
            throw new IllegalArgumentException(
                    "Screen should be created with TestCarContext for testing");
        }
        mTestCarContext = carContext;

        mLifecycleOwner = new TestLifecycleOwner();
        mLifecycleOwner.getRegistry().addObserver(new ScreenLifecycleObserver());
    }

    /** Returns the {@link Screen} being controlled. */
    public @NonNull Screen getScreen() {
        return mScreen;
    }

    /** Resets values tracked by this {@link ScreenController}. */
    public void reset() {
        mTestCarContext.getCarService(TestAppManager.class).resetTemplatesStoredForScreen(
                getScreen());
    }

    /**
     * Returns all the {@link Template}s returned from {@link Screen#onGetTemplate} for the {@link
     * Screen} being controlled.
     *
     * <p>The templates are stored in the order in which they were returned from
     * {@link Screen#onGetTemplate}, where the first template in the list, is the first template
     * returned.
     *
     * <p>The templates will be stored until {@link #reset} is called.
     */
    public @NonNull List<Template> getTemplatesReturned() {
        List<Template> templates = new ArrayList<>();
        for (Pair<Screen, Template> pair :
                mTestCarContext.getCarService(TestAppManager.class).getTemplatesReturned()) {
            if (pair.first == getScreen()) {
                templates.add(pair.second);
            }
        }
        return templates;
    }

    /**
     * Moves the {@link Screen} being controlled to the input {@code state}.
     *
     * <p>Note that moving the {@link Screen} up a state will also push the {@link Screen} onto
     * the {@link androidx.car.app.ScreenManager}'s screen stack if it isn't the current top.
     *
     * <p>{@link Lifecycle.State#DESTROYED} is a terminal state, and you cannot move to any other
     * state after the {@link Screen} reaches that state.
     *
     * @see Screen#getLifecycle
     */
    public @NonNull ScreenController moveToState(Lifecycle.@NonNull State state) {
        mLifecycleOwner.getRegistry().setCurrentState(state);
        return this;
    }

    /**
     * Returns the result that was set via {@link Screen#setResult(Object)}, or {@code null} if
     * one was not set.
     */
    public @Nullable Object getScreenResult() {
        return mScreen.getResultInternal();
    }

    void putScreenOnStackIfNotTop() {
        TestScreenManager testScreenManager = mTestCarContext.getCarService(
                TestScreenManager.class);
        if (!testScreenManager.hasScreens() || !mScreen.equals(testScreenManager.getTop())) {
            testScreenManager.push(mScreen);
        }
    }

    void dispatchLifecycleEvent(Event event) {
        mScreen.dispatchLifecycleEvent(event);
    }

    /**
     * A helper class to forward the lifecycle events from this controller to the screen.
     */
    class ScreenLifecycleObserver implements DefaultLifecycleObserver {
        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {
            putScreenOnStackIfNotTop();
            dispatchLifecycleEvent(Event.ON_CREATE);
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            putScreenOnStackIfNotTop();
            dispatchLifecycleEvent(Event.ON_START);
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            putScreenOnStackIfNotTop();
            dispatchLifecycleEvent(Event.ON_RESUME);
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            dispatchLifecycleEvent(Event.ON_PAUSE);
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            dispatchLifecycleEvent(Event.ON_STOP);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            dispatchLifecycleEvent(Event.ON_DESTROY);
        }
    }
}
