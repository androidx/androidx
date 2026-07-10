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

package androidx.camera.view;

import android.content.Context;
import android.hardware.SensorManager;
import android.view.Surface;

import androidx.annotation.CheckResult;
import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.ImageOutputConfig;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Provider for receiving rotation updates from the {@link SensorManager} when the rotation of
 * the device has changed.
 *
 * <p> This class monitors motion sensor and notifies the listener about physical orientation
 * changes in the format of {@link Surface} rotation. It's useful when the {@link Activity} is in
 * a fixed portrait or landscape orientation, while the app still wants to set the
 * {@link UseCase} target rotation based on the device's physical rotation.
 *
 * <pre><code>
 * // Create a provider.
 * RotationProvider mRotationProvider = new RotationProvider(getContext());
 *
 * // Add listener to receive updates.
 * mRotationProvider.addListener(rotation -> {
 *     mImageCapture.setTargetRotation(rotation);
 * });
 *
 * // Remove when no longer needed.
 * mRotationProvider.removeListener(listener);
 * </code></pre>
 */
public final class RotationProvider {

    private final Object mLock = new Object();

    private final androidx.camera.core.RotationProvider mRotationProviderCore;

    @GuardedBy("mLock")
    private final Map<Listener, androidx.camera.core.RotationProvider.Listener> mListeners =
            new HashMap<>();

    /**
     * Creates a new RotationProvider.
     */
    public RotationProvider(@NonNull Context context) {
        this(context, false);
    }

    @VisibleForTesting
    RotationProvider(@NonNull Context context, boolean ignoreCanDetectForTest) {
        mRotationProviderCore = new androidx.camera.core.RotationProvider(context,
                ignoreCanDetectForTest);
    }

    /**
     * Sets a {@link Listener} that listens for rotation changes.
     *
     * @param executor The executor in which the {@link {@link Listener#onRotationChanged(int)}
     *                 will be run.
     * @param listener The listener to be receive rotation updates.
     * @return false if the device cannot detect rotation changes. In that case, the listener
     * will not be set.
     */
    @CheckResult
    public boolean addListener(@NonNull Executor executor, @NonNull Listener listener) {
        synchronized (mLock) {
            // Remove existing listener to prevent leaks and to allow executor updates.
            androidx.camera.core.RotationProvider.Listener oldCoreListener =
                    mListeners.remove(listener);
            if (oldCoreListener != null) {
                mRotationProviderCore.removeListener(oldCoreListener);
            }

            androidx.camera.core.RotationProvider.Listener coreListener =
                    listener::onRotationChanged;
            if (mRotationProviderCore.addListener(executor, coreListener)) {
                mListeners.put(listener, coreListener);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the given {@link Listener} from this object.
     *
     * <p> The removed listener will no longer receive rotation updates.
     */
    public void removeListener(@NonNull Listener listener) {
        synchronized (mLock) {
            androidx.camera.core.RotationProvider.Listener coreListener =
                    mListeners.remove(listener);
            if (coreListener != null) {
                mRotationProviderCore.removeListener(coreListener);
            }
        }
    }

    /**
     * Updates the orientation for testing purposes.
     */
    @VisibleForTesting
    void updateOrientationForTesting(int orientation) {
        mRotationProviderCore.updateOrientationForTesting(orientation);
    }

    /**
     * Converts orientation degrees to {@link Surface} rotation.
     */
    @VisibleForTesting
    static int orientationToSurfaceRotation(@ImageOutputConfig.RotationValue int orientation) {
        if (orientation >= 315 || orientation < 45) {
            return Surface.ROTATION_0;
        } else if (orientation >= 225) {
            return Surface.ROTATION_90;
        } else if (orientation >= 135) {
            return Surface.ROTATION_180;
        } else {
            return Surface.ROTATION_270;
        }
    }

    /**
     * Callback interface to receive rotation updates.
     */
    public interface Listener {

        /**
         * Called when the physical rotation of the device changes.
         *
         * <p> The rotation is one of the {@link Surface} rotations mapped from orientation
         * degrees.
         *
         * <table summary="Orientation degrees to Surface rotation mapping">
         * <tr><th>Orientation degrees</th><th>Surface rotation</th></tr>
         * <tr><td>[-45°, 45°)</td><td>{@link Surface#ROTATION_0}</td></tr>
         * <tr><td>[45°, 135°)</td><td>{@link Surface#ROTATION_270}</td></tr>
         * <tr><td>[135°, 225°)</td><td>{@link Surface#ROTATION_180}</td></tr>
         * <tr><td>[225°, 315°)</td><td>{@link Surface#ROTATION_90}</td></tr>
         * </table>
         */
        void onRotationChanged(@ImageOutputConfig.RotationValue int rotation);
    }
}
