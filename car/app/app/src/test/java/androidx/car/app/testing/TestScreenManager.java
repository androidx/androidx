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

import androidx.annotation.NonNull;
import androidx.car.app.OnScreenResultCallback;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.lifecycle.Lifecycle.State;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@link ScreenManager} that is used for testing.
 *
 * <p>This class will track the following usages of the {@link ScreenManager} throughout your test:
 *
 * <ul>
 *   <li>All the {@link Screen}s pushed via {@link ScreenManager#push}, or {@link
 *       ScreenManager#pushForResult}.
 *   <li>All the {@link Screen}s removed via, {@link ScreenManager#pop}, {@link
 *       ScreenManager#popTo}, or {@link ScreenManager#remove}.
 * </ul>
 */
public class TestScreenManager extends ScreenManager {
    private final List<Screen> mScreensPushed = new ArrayList<>();
    private final List<Screen> mScreensRemoved = new ArrayList<>();

    /**
     * Resets the values tracked by this {@link TestScreenManager}, and the {@link Screen} stack
     * .
     */
    public void reset() {
        getScreenStack().clear();
        mScreensPushed.clear();
        mScreensRemoved.clear();
    }

    /**
     * Retrieves all the {@link Screen}s pushed via {@link ScreenManager#push}, and {@link
     * ScreenManager#pushForResult}.
     *
     * <p>The screens are stored in order of calls.
     *
     * <p>The screens will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<Screen> getScreensPushed() {
        return mScreensPushed;
    }

    /**
     * Retrieves all the {@link Screen}s removed via {@link ScreenManager#pop}, {@link
     * ScreenManager#popTo}, and {@link ScreenManager#remove}.
     *
     * <p>The screens are stored in order of calls.
     *
     * <p>The screens will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<Screen> getScreensRemoved() {
        return mScreensRemoved;
    }

    /** Returns {@code true} if the {@link Screen} stack has any screens in it. */
    public boolean hasScreens() {
        return !getScreensInStack().isEmpty();
    }

    @Override
    public void push(@NonNull Screen screen) {
        mScreensPushed.add(screen);
        super.push(screen);
    }

    @Override
    public void pushForResult(
            @NonNull Screen screen, @NonNull OnScreenResultCallback onScreenResultCallback) {
        mScreensPushed.add(screen);
        super.pushForResult(screen, onScreenResultCallback);
    }

    @Override
    public void pop() {
        Screen top = getTop();
        super.pop();

        if (!top.equals(getTop())) {
            mScreensRemoved.add(top);
        }
    }

    @Override
    public void popTo(@NonNull String marker) {
        Set<Screen> screensBefore = getScreensInStack();
        super.popTo(marker);
        Set<Screen> screensAfter = getScreensInStack();

        for (Screen screen : screensBefore) {
            if (!screensAfter.contains(screen)) {
                mScreensRemoved.add(screen);
            }
        }
    }

    @Override
    public void remove(@NonNull Screen screen) {
        super.remove(screen);
        if (screen.getLifecycle().getCurrentState() == State.DESTROYED) {
            mScreensRemoved.add(screen);
        }
    }

    private Set<Screen> getScreensInStack() {
        return new HashSet<>(getScreenStack());
    }

    TestScreenManager(TestCarContext testCarContext) {
        super(testCarContext, testCarContext.getLifecycleOwner().mRegistry);
    }
}
