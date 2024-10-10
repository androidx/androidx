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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A serializable class containing all the data required to render and interact with a surface from
 * an off-process renderer.
 *
 * This class exists for compatibility with Q devices. In Android R and later,
 * {@link android.view.SurfaceControlViewHost.SurfacePackage} will be used instead.
 */
@CarProtocol
@KeepFields
public final class LegacySurfacePackage {
    private @Nullable ISurfaceControl mISurfaceControl;

    /**
     * Creates a {@link LegacySurfacePackage}.
     *
     * @param callback a {@link SurfaceControlCallback} to be registered to receive off-process
     *                 renderer events affecting the {@link android.view.SurfaceView} that
     *                 content is rendered on.
     */
    @SuppressLint("ExecutorRegistration")
    public LegacySurfacePackage(@NonNull SurfaceControlCallback callback) {
        requireNonNull(callback);

        mISurfaceControl = new ISurfaceControl.Stub() {
            final SurfaceControlCallback mCallback = callback;

            @Override
            public void setSurfaceWrapper(@NonNull Bundleable surfaceWrapper) {
                requireNonNull(surfaceWrapper);
                try {
                    mCallback.setSurfaceWrapper((SurfaceWrapper) surfaceWrapper.get());
                } catch (BundlerException e) {
                    mCallback.onError("Unable to deserialize surface wrapper", e);
                }
            }

            @Override
            public void onWindowFocusChanged(boolean hasFocus, boolean isInTouchMode) {
                mCallback.onWindowFocusChanged(hasFocus, isInTouchMode);
            }

            @Override
            public void onTouchEvent(@NonNull MotionEvent event) {
                requireNonNull(event);
                mCallback.onTouchEvent(event);
            }

            @Override
            public void onKeyEvent(@NonNull KeyEvent event) {
                requireNonNull(event);
                mCallback.onKeyEvent(event);
            }
        };
    }

    /** Empty constructor needed for serializations. */
    private LegacySurfacePackage() {
    }

    @NonNull ISurfaceControl getSurfaceControl() {
        return requireNonNull(mISurfaceControl);
    }
}

