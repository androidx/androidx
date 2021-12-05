/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.core.view;


import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

/**
 * An OnPreDrawListener that will remove itself after one OnPreDraw call. Typical
 * usage is:
 * <pre><code>
 *     OneShotPreDrawListener.add(view, () -> { view.doSomething(); })
 * </code></pre>
 * <p>
 * The onPreDraw always returns true.
 * <p>
 * The listener will also remove itself from the ViewTreeObserver when the view
 * is detached from the view hierarchy. In that case, the Runnable will never be
 * executed.
 */
public final class OneShotPreDrawListener implements ViewTreeObserver.OnPreDrawListener,
        View.OnAttachStateChangeListener {
    private final View mView;
    private ViewTreeObserver mViewTreeObserver;
    private final Runnable mRunnable;

    private OneShotPreDrawListener(View view, Runnable runnable) {
        mView = view;
        mViewTreeObserver = view.getViewTreeObserver();
        mRunnable = runnable;
    }

    /**
     * Creates a OneShotPreDrawListener and adds it to view's ViewTreeObserver.
     * @param view The view whose ViewTreeObserver the OnPreDrawListener should listen.
     * @param runnable The Runnable to execute in the OnPreDraw (once)
     * @return The added OneShotPreDrawListener. It can be removed prior to
     * the onPreDraw by calling {@link #removeListener()}.
     */
    @NonNull
    @SuppressWarnings("ConstantConditions") // Validating nullability contracts.
    public static OneShotPreDrawListener add(@NonNull View view, @NonNull Runnable runnable) {
        if (view == null) throw new NullPointerException("view == null");
        if (runnable == null) throw new NullPointerException("runnable == null");

        OneShotPreDrawListener listener = new OneShotPreDrawListener(view, runnable);
        view.getViewTreeObserver().addOnPreDrawListener(listener);
        view.addOnAttachStateChangeListener(listener);
        return listener;
    }

    @Override
    public boolean onPreDraw() {
        removeListener();
        mRunnable.run();
        return true;
    }

    /**
     * Removes the listener from the ViewTreeObserver. This is useful to call if the
     * callback should be removed prior to {@link #onPreDraw()}.
     */
    public void removeListener() {
        if (mViewTreeObserver.isAlive()) {
            mViewTreeObserver.removeOnPreDrawListener(this);
        } else {
            mView.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        mView.removeOnAttachStateChangeListener(this);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        mViewTreeObserver = v.getViewTreeObserver();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        removeListener();
    }
}
