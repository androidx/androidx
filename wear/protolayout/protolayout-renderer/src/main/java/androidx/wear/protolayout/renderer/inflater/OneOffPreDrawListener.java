/*
 * Copyright 2024 The Android Open Source Project
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
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Supplier;

/**
 * The listener that calls {@link ViewTreeObserver.OnPreDrawListener} only once, after the when View
 * is attached.
 *
 * <p>Similar to the {@link androidx.core.view.OneShotPreDrawListener}, but {@code onPreDraw} method
 * can return false, meaning that the current draw is cancelled.
 */
class OneOffPreDrawListener implements OnPreDrawListener, OnAttachStateChangeListener {

    @NonNull private final View mView;
    @Nullable private ViewTreeObserver mViewTreeObserver;
    @NonNull private final Supplier<Boolean> mSupplier;
    private boolean mIsAttached = false;

    private OneOffPreDrawListener(@NonNull View view, @NonNull Supplier<Boolean> supplier) {
        this.mView = view;
        this.mSupplier = supplier;
    }

    /**
     * Creates and adds an instance of {@link OneOffPreDrawListener} to the given {@link View} by
     *
     * @param view The view whose ViewTreeObserver the OnPreDrawListener should listen.
     *     <p>Note that the returned listener is already attached.
     * @param supplier The Supplier to execute in the OnPreDraw (once). This Supplier should return
     *     false if the current drawing pass that was called for needs to be cancelled.
     */
    @NonNull
    public static OneOffPreDrawListener add(
            @NonNull View view, @NonNull Supplier<Boolean> supplier) {
        OneOffPreDrawListener listener = new OneOffPreDrawListener(view, supplier);
        // OnPreDraw listener will be added only after this View was attached.
        view.addOnAttachStateChangeListener(listener);
        return listener;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull View v) {
        // We need this flag in case detach was called before onPreDraw
        mIsAttached = true;
        mViewTreeObserver = v.getViewTreeObserver();
        mViewTreeObserver.addOnPreDrawListener(this);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull View v) {
        removeOnPreDrawListener();
        mIsAttached = false;
    }

    @Override
    public boolean onPreDraw() {
        // Call onPreDraw only once so removing the listener.
        removeOnPreDrawListener();

        // Only call supplier.get() if View is attached.
        if (!mIsAttached) {
            return true;
        }

        // Since we called the supplier after the View was attached, we can remove that listener
        // too.
        mView.removeOnAttachStateChangeListener(this);
        return mSupplier.get();
    }

    private void removeOnPreDrawListener() {
        if (mViewTreeObserver == null) {
            return;
        }

        if (mViewTreeObserver.isAlive()) {
            mViewTreeObserver.removeOnPreDrawListener(this);
        } else {
            mView.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        mViewTreeObserver = null;
    }
}
