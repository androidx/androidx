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

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.HostDispatcher;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceListener;
import androidx.car.app.model.Template;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link AppManager} that is used for testing.
 *
 * <p>This class will track the following usages of the {@link AppManager} throughout your test:
 *
 * <ul>
 *   <li>All {@link SurfaceListener}s set via calling {@link AppManager#setSurfaceListener}.
 *   <li>The {@link Template}s returned from {@link Screen#onGetTemplate} due to invalidate calls
 *       via {@link AppManager#invalidate}.
 *   <li>All toasts shown via calling {@link AppManager#showToast}.
 * </ul>
 */
public class TestAppManager extends AppManager {
    private final List<SurfaceListener> mSurfaceListeners = new ArrayList<>();
    private final List<CharSequence> mToastsShown = new ArrayList<>();
    private final List<Pair<Screen, Template>> mTemplatesReturned = new ArrayList<>();

    /** Resets the values tracked by this {@link TestAppManager} and all {@link ScreenController}
     * s. */
    public void reset() {
        mSurfaceListeners.clear();
        mToastsShown.clear();
        mTemplatesReturned.clear();
    }

    /**
     * Retrieves all the {@link SurfaceListener}s set via {@link AppManager#setSurfaceListener}.
     *
     * <p>The listeners are stored in order of calls.
     *
     * <p>The listeners will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<SurfaceListener> getSurfaceListeners() {
        return mSurfaceListeners;
    }

    /**
     * Retrieves all the toasts shown via {@link AppManager#showToast}.
     *
     * <p>The toasts are stored in order of calls.
     *
     * <p>The toasts will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<CharSequence> getToastsShown() {
        return mToastsShown;
    }

    /**
     * Retrieves all the {@link Template}s returned from {@link Screen#onGetTemplate} due to a call
     * to {@link AppManager#invalidate}, and the respective {@link Screen} instance that returned
     * it.
     *
     * <p>The results are stored in order of calls.
     *
     * <p>The results will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<Pair<Screen, Template>> getTemplatesReturned() {
        return mTemplatesReturned;
    }

    @Override
    public void setSurfaceListener(@Nullable SurfaceListener surfaceListener) {
        mSurfaceListeners.add(surfaceListener);
    }

    @Override
    public void showToast(@NonNull CharSequence text, int duration) {
        mToastsShown.add(text);
    }

    void resetTemplatesStoredForScreen(@NonNull Screen screen) {
        List<Pair<Screen, Template>> templatesForOtherScreens = new ArrayList<>();

        for (Pair<Screen, Template> pair : mTemplatesReturned) {
            if (pair.first != screen) {
                templatesForOtherScreens.add(pair);
            }
        }

        mTemplatesReturned.clear();
        mTemplatesReturned.addAll(templatesForOtherScreens);
    }

    void addTemplateReturned(Screen screenThatReturnedTheTemplate, Template template) {
        mTemplatesReturned.add(Pair.create(screenThatReturnedTheTemplate, template));
    }

    TestAppManager(TestCarContext testCarContext, HostDispatcher hostDispatcher) {
        super(testCarContext, hostDispatcher);
    }
}
