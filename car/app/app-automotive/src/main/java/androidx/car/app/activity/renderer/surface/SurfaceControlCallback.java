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

import static androidx.car.app.activity.LogTags.TAG;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.jspecify.annotations.NonNull;

/**
 * A host-side interface for reporting to an off-process renderer events affecting the
 * {@link android.view.SurfaceView} it renders content on.
 */
public interface SurfaceControlCallback {
    /** Notifies when the underlying surface changes. */
    @SuppressLint({"CallbackMethodName"})
    void setSurfaceWrapper(@NonNull SurfaceWrapper surfaceWrapper);

    /** Notifies when {@link android.view.SurfaceView} receives a new touch event. */
    void onTouchEvent(@NonNull MotionEvent event);

    /** Notifies when {@link android.view.SurfaceView} receives a new key event. */
    void onKeyEvent(@NonNull KeyEvent event);

    /** Notifies when the window focus changes. */
    void onWindowFocusChanged(boolean hasFocus, boolean isInTouchMode);

    /** Notifies when there is an error. Provide default implementation for easier transition. */
    default void onError(@NonNull String msg, @NonNull Throwable e) {
        Log.e(TAG, msg, e);
    }
}
