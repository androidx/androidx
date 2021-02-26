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
import static androidx.car.app.activity.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

/**
 * A listener of {@link SurfaceHolder}.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class SurfaceHolderListener implements SurfaceHolder.Callback {
    @Nullable
    private ISurfaceListener mSurfaceListener;
    private boolean mIsSurfaceAvailable;
    private final SurfaceWrapperProvider mSurfaceWrapperProvider;

    public SurfaceHolderListener(@NonNull SurfaceWrapperProvider surfaceWrapperProvider) {
        super();
        mSurfaceWrapperProvider = surfaceWrapperProvider;
    }

    /**
     * Registers a listener for surface events.
     */
    public final void setSurfaceListener(@Nullable ISurfaceListener surfaceListener) {
        mSurfaceListener = surfaceListener;
        onActive();
    }

    private void onActive() {
        if (mIsSurfaceAvailable) {
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
        mIsSurfaceAvailable = false;
    }

    private void notifySurfaceCreated() {
        try {
            if (mSurfaceListener != null) {
                mSurfaceListener.onSurfaceAvailable(
                        Bundleable.create(mSurfaceWrapperProvider.createSurfaceWrapper()));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        } catch (BundlerException e) {
            Log.e(TAG, "Unable to serialize surface wrapper", e);
        }

    }

    private void notifySurfaceChanged() {
        try {
            if (mSurfaceListener != null) {
                mSurfaceListener.onSurfaceChanged(
                        Bundleable.create(mSurfaceWrapperProvider.createSurfaceWrapper()));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        } catch (BundlerException e) {
            Log.e(TAG, "Unable to serialize surface wrapper", e);
        }
    }
}
