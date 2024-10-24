/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity.renderer.surface;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.view.SurfaceHolder;

import androidx.annotation.RestrictTo;
import androidx.car.app.activity.ServiceDispatcher;
import androidx.car.app.serialization.Bundleable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A listener of {@link SurfaceHolder}.
 *
 */
@RestrictTo(LIBRARY)
public class SurfaceHolderListener implements SurfaceHolder.Callback {
    private final ServiceDispatcher mServiceDispatcher;
    private @Nullable ISurfaceListener mSurfaceListener;
    private boolean mIsSurfaceAvailable;
    private final SurfaceWrapperProvider mSurfaceWrapperProvider;

    public SurfaceHolderListener(@NonNull ServiceDispatcher serviceDispatcher,
            @NonNull SurfaceWrapperProvider surfaceWrapperProvider) {
        super();
        mSurfaceWrapperProvider = surfaceWrapperProvider;
        mServiceDispatcher = serviceDispatcher;
    }

    /**
     * Registers a listener for surface events.
     */
    public final void setSurfaceListener(@Nullable ISurfaceListener surfaceListener) {
        mSurfaceListener = surfaceListener;
        if (surfaceListener != null && mIsSurfaceAvailable) {
            notifySurfaceCreated();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        requireNonNull(holder);
        mIsSurfaceAvailable = true;
        notifySurfaceCreated();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        requireNonNull(holder);
        notifySurfaceChanged();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        requireNonNull(holder);
        mIsSurfaceAvailable = false;
        notifySurfaceDestroyed();
    }

    private void notifySurfaceCreated() {
        ISurfaceListener surfaceListener = mSurfaceListener;
        if (surfaceListener != null) {
            mServiceDispatcher.dispatch("onSurfaceAvailable",
                    () -> surfaceListener.onSurfaceAvailable(
                            Bundleable.create(mSurfaceWrapperProvider.createSurfaceWrapper())));
        }
    }

    private void notifySurfaceChanged() {
        ISurfaceListener surfaceListener = mSurfaceListener;
        if (surfaceListener != null) {
            mServiceDispatcher.dispatch("onSurfaceChanged",
                    () -> surfaceListener.onSurfaceChanged(
                            Bundleable.create(mSurfaceWrapperProvider.createSurfaceWrapper())));
        }
    }

    private void notifySurfaceDestroyed() {
        ISurfaceListener surfaceListener = mSurfaceListener;
        if (surfaceListener != null) {
            mServiceDispatcher.dispatchNoFail("onSurfaceDestroyed",
                    () -> surfaceListener.onSurfaceDestroyed(
                            Bundleable.create(mSurfaceWrapperProvider.createSurfaceWrapper())));
        }
    }
}
