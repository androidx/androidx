/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.internal.widget;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * A {@link android.content.ContextWrapper} which returns a tint-aware
 * {@link android.content.res.Resources} instance from {@link #getResources()}.
 *
 * @hide
 */
public class TintContextWrapper extends ContextWrapper {

    public static Context wrap(Context context) {
        if (!(context instanceof TintContextWrapper)) {
            context = new TintContextWrapper(context);
        }
        return context;
    }

    private Resources mResources;

    private TintContextWrapper(Context base) {
        super(base);
    }

    @Override
    public Resources getResources() {
        if (mResources == null) {
            mResources = new TintResources(super.getResources(), TintManager.get(this));
        }
        return mResources;
    }

    /**
     * This class allows us to intercept calls so that we can tint resources (if applicable).
     */
    static class TintResources extends ResourcesWrapper {

        private final TintManager mTintManager;

        public TintResources(Resources resources, TintManager tintManager) {
            super(resources);
            mTintManager = tintManager;
        }

        /**
         * We intercept this call so that we tint the result (if applicable). This is needed for
         * things like {@link android.graphics.drawable.DrawableContainer}s which can retrieve
         * their children via this method.
         */
        @Override
        public Drawable getDrawable(int id) throws NotFoundException {
            Drawable d = super.getDrawable(id);
            if (d != null) {
                mTintManager.tintDrawableUsingColorFilter(id, d);
            }
            return d;
        }
    }
}