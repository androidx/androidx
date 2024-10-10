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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.util.Pair;

import androidx.annotation.RestrictTo;
import androidx.car.app.AppManager;
import androidx.car.app.CarToast;
import androidx.car.app.HostDispatcher;
import androidx.car.app.Screen;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.media.OpenMicrophoneRequest;
import androidx.car.app.media.OpenMicrophoneResponse;
import androidx.car.app.model.Template;
import androidx.car.app.utils.CollectionUtils;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link AppManager} that is used for testing.
 *
 * <p>This class will track the following usages of the {@link AppManager} throughout your test:
 *
 * <ul>
 *   <li>All {@link SurfaceCallback}s set via calling {@link AppManager#setSurfaceCallback}.
 *   <li>The {@link Template}s returned from {@link Screen#onGetTemplate} due to invalidate calls
 *       via {@link AppManager#invalidate}.
 *   <li>All toasts shown via calling {@link AppManager#showToast}.
 * </ul>
 */
public class TestAppManager extends AppManager {
    private final List<CharSequence> mToastsShown = new ArrayList<>();
    private final List<Pair<Screen, Template>> mTemplatesReturned = new ArrayList<>();
    private @Nullable SurfaceCallback mSurfaceCallback;
    private @Nullable OpenMicrophoneRequest mOpenMicrophoneRequest = null;

    /**
     * Resets the values tracked by this {@link TestAppManager} and all {@link ScreenController}s.
     */
    public void reset() {
        mSurfaceCallback = null;
        mToastsShown.clear();
        mTemplatesReturned.clear();
    }

    /**
     * Returns the callback set via {@link AppManager#setSurfaceCallback}, or {@code null} if not
     * set.
     */
    public @Nullable SurfaceCallback getSurfaceCallback() {
        return mSurfaceCallback;
    }

    /**
     * Returns all the toasts shown via {@link AppManager#showToast}.
     *
     * <p>The toasts are stored in the order in which they are sent via
     * {@link AppManager#showToast}, where the first toast in the list is the first toast that
     * was sent.
     *
     * <p>The toasts will be stored until {@link #reset} is called.
     */
    public @NonNull List<CharSequence> getToastsShown() {
        return CollectionUtils.unmodifiableCopy(mToastsShown);
    }

    /**
     * Returns all the {@link Template}s returned from {@link Screen#onGetTemplate} due to a call
     * to {@link AppManager#invalidate}, and the respective {@link Screen} instance that returned
     * it.
     *
     * The results are stored in the order in which they were returned from
     * {@link Screen#onGetTemplate}, where the first template in the list, is the first template
     * returned.
     *
     * <p>The results will be stored until {@link #reset} is called.
     */
    public @NonNull List<Pair<Screen, Template>> getTemplatesReturned() {
        return CollectionUtils.unmodifiableCopy(mTemplatesReturned);
    }

    @SuppressLint("ExecutorRegistration")
    @Override
    public void setSurfaceCallback(@Nullable SurfaceCallback surfaceCallback) {
        mSurfaceCallback = surfaceCallback;
    }

    @Override
    public void showToast(@NonNull CharSequence text, @CarToast.Duration int duration) {
        mToastsShown.add(requireNonNull(text));
    }

    /**
     */
    @Override
    @RestrictTo(LIBRARY_GROUP)
    public @Nullable OpenMicrophoneResponse openMicrophone(@NonNull OpenMicrophoneRequest request) {
        mOpenMicrophoneRequest = request;
        return super.openMicrophone(request);
    }

    /**
     * Returns the last {@link OpenMicrophoneRequest} sent to the host.
     *
     */
    @RestrictTo(LIBRARY_GROUP)
    public @Nullable OpenMicrophoneRequest getOpenMicrophoneRequest() {
        return mOpenMicrophoneRequest;
    }

    void resetTemplatesStoredForScreen(Screen screen) {
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
        super(testCarContext, hostDispatcher, testCarContext.getLifecycleOwner().mRegistry);
    }
}
