/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnLayoutChangeListener;

import org.jspecify.annotations.NonNull;

/**
 * The listener that calls {@link OnLayoutChangeListener} only once, after the when View is
 * attached.
 *
 * <p>Similar to the {@link androidx.core.view.OneShotPreDrawListener}, but calls {@code
 * onLayoutChange} once. See b/381497935.
 */
class OneOffLayoutChangeListener implements OnLayoutChangeListener, OnAttachStateChangeListener {
    private final @NonNull View mView;
    private final @NonNull Runnable mRunnable;
    private boolean mIsAttached = false;

    private OneOffLayoutChangeListener(@NonNull View view, @NonNull Runnable runnable) {
        this.mView = view;
        this.mRunnable = runnable;
    }

    /**
     * Creates and adds an instance of {@link OneOffLayoutChangeListener} to the given {@link View}.
     *
     * @param view The view whose OnLayoutChangeListener should listen.
     *     <p>Note that the returned listener is already attached to the view and it will be
     *     detached when it's called.
     * @param runnable The Runnable to execute in the onLayoutChange (once). It is this Runnable's
     *     responsibility to invalidate or update any layout in the view.
     */
    public static @NonNull OneOffLayoutChangeListener add(
            @NonNull View view, @NonNull Runnable runnable) {
        OneOffLayoutChangeListener listener = new OneOffLayoutChangeListener(view, runnable);
        // onLayoutChange listener will be added only after this View was attached.
        view.addOnAttachStateChangeListener(listener);
        return listener;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        // We need this flag in case detach was called before onLayoutChange
        mIsAttached = true;
        mView.addOnLayoutChangeListener(this);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        removeOnLayoutChangeListener();
        mIsAttached = false;
    }

    @Override
    public void onLayoutChange(
            View view,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {
        // Only call runnable.run() if View is attached.
        if (!mIsAttached) {
            return;
        }

        // Since we called the runnable after the View was attached, we can remove that listener too
        mView.removeOnAttachStateChangeListener(this);
        removeOnLayoutChangeListener();
        mRunnable.run();
    }

    private void removeOnLayoutChangeListener() {
        mView.removeOnLayoutChangeListener(this);
    }
}
