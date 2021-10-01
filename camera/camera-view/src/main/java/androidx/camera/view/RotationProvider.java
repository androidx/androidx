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

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.view.Surface;

import androidx.annotation.CheckResult;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.ImageOutputConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * mRotationProvider.clearListener();
 * </code></pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class RotationProvider {

    final Object mLock = new Object();

    @GuardedBy("mLock")
    @VisibleForTesting
    @NonNull
    final OrientationEventListener mOrientationListener;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @GuardedBy("mLock")
    @NonNull
    final Map<Listener, ListenerWrapper> mListeners = new HashMap<>();

    // Set this value to true to test adding listener in unit tests.
    @VisibleForTesting
    boolean mIgnoreCanDetectForTest = false;

    /**
     * Creates a new RotationProvider.
     */
    public RotationProvider(@NonNull Context context) {
        mOrientationListener = new OrientationEventListener(context) {
            private static final int INVALID_SURFACE_ROTATION = -1;

            private int mRotation = INVALID_SURFACE_ROTATION;

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    // Short-circuit if orientation is unknown. Unknown rotation
                    // can't be handled so it shouldn't be sent.
                    return;
                }

                int newRotation = orientationToSurfaceRotation(orientation);
                if (mRotation != newRotation) {
                    mRotation = newRotation;
                    List<ListenerWrapper> listeners;
                    // Take a snapshot for thread safety.
                    synchronized (mLock) {
                        listeners = new ArrayList<>(mListeners.values());
                    }
                    if (!listeners.isEmpty()) {
                        for (ListenerWrapper listenerWrapper : listeners) {
                            listenerWrapper.onRotationChanged(newRotation);
                        }
                    }
                }
            }
        };
    }

    /**
     * Sets a {@link Listener} that listens for rotation changes.
     *
     * @param executor The executor in which the {@link {@link Listener#onRotationChanged(int)}
     *                 will be run.
     * @return false if the device cannot detection rotation changes. In that case, the listener
     * will not be set.
     */
    @CheckResult
    public boolean addListener(@NonNull Executor executor, @NonNull Listener listener) {
        synchronized (mLock) {
            if (!mOrientationListener.canDetectOrientation() && !mIgnoreCanDetectForTest) {
                return false;
            }
            mListeners.put(listener, new ListenerWrapper(listener, executor));
            mOrientationListener.enable();
        }
        return true;
    }

    /**
     * Removes the given {@link Listener} from this object.
     *
     * <p> The removed listener will no longer receive rotation updates.
     */
    public void removeListener(@NonNull Listener listener) {
        synchronized (mLock) {
            ListenerWrapper listenerWrapper = mListeners.get(listener);
            if (listenerWrapper != null) {
                listenerWrapper.disable();
                mListeners.remove(listener);
            }
            if (mListeners.isEmpty()) {
                mOrientationListener.disable();
            }
        }
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
     * Wrapper of {@link Listener} with the executor and a tombstone flag.
     */
    private static class ListenerWrapper {
        private final Listener mListener;
        private final Executor mExecutor;
        private final AtomicBoolean mEnabled;

        ListenerWrapper(Listener listener, Executor executor) {
            mListener = listener;
            mExecutor = executor;
            mEnabled = new AtomicBoolean(true);
        }

        void onRotationChanged(@ImageOutputConfig.RotationValue int rotation) {
            mExecutor.execute(() -> {
                if (mEnabled.get()) {
                    mListener.onRotationChanged(rotation);
                }
            });
        }

        /**
         * Once disabled, the app will not receive callback even if it has already been posted on
         * the callback thread.
         */
        void disable() {
            mEnabled.set(false);
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
