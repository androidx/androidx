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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static androidx.car.app.utils.LogTags.TAG;
import static androidx.car.app.utils.ThreadUtils.checkMainThread;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.car.app.managers.Manager;
import androidx.car.app.model.TemplateInfo;
import androidx.car.app.model.TemplateWrapper;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Manages the stack of {@link Screen}s and their respective {@link Lifecycle}s.
 */
@MainThread
public class ScreenManager implements Manager {
    private final Deque<Screen> mScreenStack = new ArrayDeque<>();
    private final CarContext mCarContext;
    private final Lifecycle mAppLifecycle;

    /**
     * Returns the {@link Screen} that is at the top of the stack.
     *
     * @throws NullPointerException  if the method is called before a {@link Screen} has been
     *                               pushed to the stack via {@link #push}, or
     *                               {@link #pushForResult}, or returning a {@link Screen} from
     *                               {@link Session#onCreateScreen}
     * @throws IllegalStateException if the current thread is not the main thread
     */
    public @NonNull Screen getTop() {
        checkMainThread();
        return requireNonNull(mScreenStack.peek());
    }

    /**
     * Pushes the {@code screen} to the stack.
     *
     * <p>If the {@code screen} pushed is already in the stack it will be moved to the top of the
     * stack.
     *
     * <p>If the app's lifecycle is already in the {@link State#DESTROYED} state, this operation
     * is a no-op.
     *
     * @throws NullPointerException  if {@code screen} is {@code null}
     * @throws IllegalStateException if the current thread is not the main thread
     */
    public void push(@NonNull Screen screen) {
        checkMainThread();
        if (mAppLifecycle.getCurrentState().equals(State.DESTROYED)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Pushing screens after the DESTROYED state is a no-op");
            }
            return;
        }
        if (screen.getLifecycle().getCurrentState().equals(State.DESTROYED)) {
            throw new IllegalStateException(String.format(Locale.US,
                    "Failed to push screen (%s), because it has already been destroyed. Please "
                            + "note that screens are single-use, so a fresh instance is required "
                            + "every time you call screenManager.push().",
                    screen));
        }
        pushInternal(requireNonNull(screen));

    }

    /**
     * Pushes a {@link Screen}, for which you would like a result from, onto the stack.
     *
     * <p>When the given {@code screen} finishes, the {@code onScreenResultCallback} will receive a
     * callback to {@link OnScreenResultListener#onScreenResult} with the result that the pushed
     * {@code screen} set via {@link Screen#setResult}.
     *
     * <p>If the app's lifecycle is already in the {@link State#DESTROYED} state, this operation
     * is a no-op.
     *
     * @param screen                 the {@link Screen} to push on top of the stack
     * @param onScreenResultListener the listener that will be executed with the result pushed by
     *                               the {@code screen} through {@link Screen#setResult}. This
     *                               callback will be executed on the main thread
     * @throws NullPointerException  if either the {@code screen} or the {@code
     *                               onScreenResultCallback} are {@code null}
     * @throws IllegalStateException if the current thread is not the main thread
     */
    @SuppressLint("ExecutorRegistration")
    public void pushForResult(
            @NonNull Screen screen, @NonNull OnScreenResultListener onScreenResultListener) {
        checkMainThread();
        if (mAppLifecycle.getCurrentState().equals(State.DESTROYED)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Pushing screens after the DESTROYED state is a no-op");
            }
            return;
        }

        requireNonNull(screen).setOnScreenResultListener(requireNonNull(onScreenResultListener));
        pushInternal(screen);
    }

    /**
     * Pops the top {@link Screen} from the stack.
     *
     * <p>If the top {@link Screen} is the only {@link Screen} in the stack, it will not be removed.
     *
     * <p>If the app's lifecycle is already in the {@link State#DESTROYED} state, this operation
     * is a no-op.
     *
     * @throws IllegalStateException if the current thread is not the main thread
     */
    public void pop() {
        checkMainThread();
        if (mAppLifecycle.getCurrentState().equals(State.DESTROYED)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Popping screens after the DESTROYED state is a no-op");
            }
            return;
        }

        if (mScreenStack.size() > 1) {
            popInternal(Collections.singletonList(mScreenStack.pop()));
        }
    }

    /**
     * Removes screens from the top of the stack until a {@link Screen} which has the given {@code
     * marker} is found, or the root has been reached.
     *
     * <p>The root {@link Screen} will not be popped.
     *
     * <p>If the app's lifecycle is already in the {@link State#DESTROYED} state, this operation
     * is a no-op.
     *
     * @throws NullPointerException  if {@code marker} is {@code null}
     * @throws IllegalStateException if the current thread is not the main thread
     * @see Screen#setMarker
     */
    public void popTo(@NonNull String marker) {
        checkMainThread();
        requireNonNull(marker);
        if (mAppLifecycle.getCurrentState().equals(State.DESTROYED)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Popping screens after the DESTROYED state is a no-op");
            }
            return;
        }

        // Pop all screens, except until found root or the provided screen.
        List<Screen> screensToPop = new ArrayList<>();

        while (mScreenStack.size() > 1 && !foundMarker(marker)) {
            screensToPop.add(mScreenStack.pop());
        }

        if (screensToPop.isEmpty()) {
            return;
        }

        popInternal(screensToPop);
    }

    /**
     * Removes all screens from the stack until the root has been reached.
     *
     * <p>If the app's lifecycle is already in the {@link State#DESTROYED} state, this operation
     * is a no-op.
     *
     * @throws IllegalStateException if the current thread is not the main thread
     */
    public void popToRoot() {
        checkMainThread();
        if (mAppLifecycle.getCurrentState().equals(State.DESTROYED)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Popping screens after the DESTROYED state is a no-op");
            }
            return;
        }

        if (mScreenStack.size() <= 1) {
            return;
        }

        // Pop all screens, except until found root or the provided screen.
        List<Screen> screensToPop = new ArrayList<>();
        while (mScreenStack.size() > 1) {
            screensToPop.add(mScreenStack.pop());
        }

        popInternal(screensToPop);
    }

    /**
     * Removes the {@code screen} from the stack.
     *
     * <p>If the {@code screen} is the only {@link Screen} in the stack, it will not be removed.
     *
     * <p>If the app's lifecycle is already in the {@link State#DESTROYED} state, this operation
     * is a no-op.
     *
     * @throws NullPointerException  if {@code screen} is {@code null}
     * @throws IllegalStateException if the current thread is not the main thread
     */
    public void remove(@NonNull Screen screen) {
        checkMainThread();
        requireNonNull(screen);
        if (mAppLifecycle.getCurrentState().equals(State.DESTROYED)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Popping screens after the DESTROYED state is a no-op");
            }
            return;
        }

        if (mScreenStack.size() <= 1) {
            // Don't pop the final Screen.
            return;
        }

        if (screen.equals(getTop())) {
            // Screen passed in is top of stack
            mScreenStack.pop();

            popInternal(Collections.singletonList(screen));
        } else if (mScreenStack.remove(screen)) {
            // Not top of stack, remove and call destroy as it's already stopped.
            screen.dispatchLifecycleEvent(Event.ON_DESTROY);
        }

        // Not in stack;
    }

    /**
     * Returns the current stack size.
     */
    public int getStackSize() {
        return mScreenStack.size();
    }

    /** Creates an instance of {@link ScreenManager}. */
    static ScreenManager create(CarContext carContext, Lifecycle lifecycle) {
        return new ScreenManager(carContext, lifecycle);
    }

    /** Returns the {@link TemplateWrapper} for the {@link Screen} that is on top of the stack. */
    @NonNull TemplateWrapper getTopTemplate() {
        checkMainThread();

        Screen screen = getTop();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Requesting template from Screen " + screen);
        }

        TemplateWrapper templateWrapper = screen.getTemplateWrapper();

        List<TemplateInfo> templateInfoList = new ArrayList<>();
        for (Screen s : mScreenStack) {
            templateInfoList.add(s.getLastTemplateInfo());
        }

        templateWrapper.setTemplateInfosForScreenStack(templateInfoList);
        return templateWrapper;
    }

    void destroyAndClearScreenStack() {
        Deque<Screen> screenStack = new ArrayDeque<>(mScreenStack);
        for (Screen screen : screenStack) {
            stop(screen, true);
        }
        mScreenStack.clear();
    }

    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    protected @NonNull Deque<Screen> getScreenStackInternal() {
        return mScreenStack;
    }

    /** Returns the copy of the current screen stack as a type {@link Collection} */
    public @NonNull Collection<Screen> getScreenStack() {
        return new ArrayList<>(mScreenStack);
    }

    private boolean foundMarker(String marker) {
        return marker.equals(getTop().getMarker());
    }

    private void pushInternal(Screen screen) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Pushing screen " + screen + " to the top of the screen stack");
        }

        if (mScreenStack.contains(screen)) {
            moveToTop(screen);
            return;
        }

        Screen top = mScreenStack.peek();

        pushAndStart(screen, true);

        if (!mScreenStack.contains(screen)) {
            // The screen being pushed was finished during it's set up
            return;
        }

        if (top != null) {
            stop(top, false);
        }

        if (mAppLifecycle.getCurrentState().isAtLeast(State.RESUMED)) {
            screen.dispatchLifecycleEvent(Event.ON_RESUME);
        }
    }

    private void popInternal(List<Screen> poppedScreens) {
        Screen newTop = getTop();

        // A pop operation means that the user is going back to a previous screen. Make sure the
        // next template sent from that screen shares the same ID as the screen's last used
        // template. The host uses this ID information to detect the back operation.
        newTop.setUseLastTemplateId(true);

        // Invalidate so the host can request the template from the screen that is now at the top
        // of the stack.
        mCarContext.getCarService(AppManager.class).invalidate();

        if (mAppLifecycle.getCurrentState().isAtLeast(State.STARTED)) {
            newTop.dispatchLifecycleEvent(Event.ON_START);
        }

        // Stop and destroy all screens popped.
        for (Screen screen : poppedScreens) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Popping screen " + screen + " off the screen stack");
            }
            stop(screen, true);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Screen " + newTop + " is at the top of the screen stack");
        }
        if (mAppLifecycle.getCurrentState().isAtLeast(State.RESUMED)) {
            if (mScreenStack.contains(newTop)) {
                // During the Screen teardown it can send the result to any screen that called
                // startScreenForResult.  If this receiver pops from the stack, it is possible
                // that the newTop is no longer in the stack.
                newTop.dispatchLifecycleEvent(Event.ON_RESUME);
            }
        }
    }

    private void pushAndStart(Screen screen, boolean shouldCreate) {
        mScreenStack.push(screen);
        if (shouldCreate && mAppLifecycle.getCurrentState().isAtLeast(State.CREATED)) {
            screen.dispatchLifecycleEvent(Event.ON_CREATE);
        }

        if (!screen.getLifecycle().getCurrentState().isAtLeast(State.CREATED)) {
            // The screen was finished in it's onCreate
            return;
        }

        if (mAppLifecycle.getCurrentState().isAtLeast(State.STARTED)) {
            mCarContext.getCarService(AppManager.class).invalidate();
            screen.dispatchLifecycleEvent(Event.ON_START);
        }
    }

    private void stop(Screen screen, boolean shouldDestroy) {
        State currentState = screen.getLifecycle().getCurrentState();

        if (currentState.isAtLeast(State.RESUMED)) {
            screen.dispatchLifecycleEvent(Event.ON_PAUSE);
        }

        if (currentState.isAtLeast(State.STARTED)) {
            screen.dispatchLifecycleEvent(Event.ON_STOP);
        }

        if (shouldDestroy) {
            screen.dispatchLifecycleEvent(Event.ON_DESTROY);
        }
    }

    private void moveToTop(Screen screen) {
        Screen top = mScreenStack.peek();
        if (top == null || top == screen) {
            return;
        }

        // Moving screen to top of stack, remove from where it's currently at.
        mScreenStack.remove(screen);

        pushAndStart(screen, false);
        stop(top, false);

        if (mAppLifecycle.getCurrentState().isAtLeast(State.RESUMED)) {
            screen.dispatchLifecycleEvent(Event.ON_RESUME);
        }
    }

    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    protected ScreenManager(@NonNull CarContext carContext, @NonNull Lifecycle lifecycle) {
        mCarContext = carContext;
        mAppLifecycle = lifecycle;
        mAppLifecycle.addObserver(new LifecycleObserverImpl());
    }

    /** A lifecycle observer implementation that forwards events to the screens in the stack. */
    class LifecycleObserverImpl implements DefaultLifecycleObserver {
        @Override
        public void onCreate(@NonNull LifecycleOwner lifecycleOwner) {
        }

        @Override
        public void onStart(@NonNull LifecycleOwner lifecycleOwner) {
            Screen top = getScreenStackInternal().peek();
            if (top == null) {
                Log.e(TAG, "Screen stack was empty during lifecycle onStart");
                return;
            }
            top.dispatchLifecycleEvent(Event.ON_START);
        }

        @Override
        public void onResume(@NonNull LifecycleOwner lifecycleOwner) {
            Screen top = getScreenStackInternal().peek();
            if (top == null) {
                Log.e(TAG, "Screen stack was empty during lifecycle onResume");
                return;
            }
            top.dispatchLifecycleEvent(Event.ON_RESUME);
        }

        @Override
        public void onPause(@NonNull LifecycleOwner lifecycleOwner) {
            Screen top = getScreenStackInternal().peek();
            if (top == null) {
                Log.e(TAG, "Screen stack was empty during lifecycle onPause");
                return;
            }
            top.dispatchLifecycleEvent(Event.ON_PAUSE);
        }

        @Override
        public void onStop(@NonNull LifecycleOwner lifecycleOwner) {
            Screen top = getScreenStackInternal().peek();
            if (top == null) {
                Log.e(TAG, "Screen stack was empty during lifecycle onStop");
                return;
            }
            top.dispatchLifecycleEvent(Event.ON_STOP);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
            destroyAndClearScreenStack();
            lifecycleOwner.getLifecycle().removeObserver(this);
        }
    }
}
