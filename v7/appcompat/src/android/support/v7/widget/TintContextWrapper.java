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

package android.support.v7.widget;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.VectorEnabledTintResources;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A {@link android.content.ContextWrapper} which returns a tint-aware
 * {@link android.content.res.Resources} instance from {@link #getResources()}.
 *
 * @hide
 */
public class TintContextWrapper extends ContextWrapper {

    private static final ArrayList<WeakReference<TintContextWrapper>> sCache = new ArrayList<>();

    public static Context wrap(@NonNull final Context context) {
        if (shouldWrap(context)) {
            // First check our instance cache
            for (int i = 0, count = sCache.size(); i < count; i++) {
                final WeakReference<TintContextWrapper> ref = sCache.get(i);
                final TintContextWrapper wrapper = ref != null ? ref.get() : null;
                if (wrapper != null && wrapper.getBaseContext() == context) {
                    return wrapper;
                }
            }
            // If we reach here then the cache didn't have a hit, so create a new instance
            // and add it to the cache
            final TintContextWrapper wrapper = new TintContextWrapper(context);
            sCache.add(new WeakReference<>(wrapper));
            return wrapper;
        }

        return context;
    }

    private static boolean shouldWrap(@NonNull final Context context) {
        if (context instanceof TintContextWrapper
                || context.getResources() instanceof TintResources
                || context.getResources() instanceof VectorEnabledTintResources) {
            // If the Context already has a TintResources[Experimental] impl, no need to wrap again
            // If the Context is already a TintContextWrapper, no need to wrap again
            return false;
        }
        if (AppCompatDelegate.isCompatVectorFromResourcesEnabled()
                && Build.VERSION.SDK_INT > VectorEnabledTintResources.MAX_SDK_WHERE_REQUIRED) {
            // If we're running on API 21+ and have the vector resources enabled, there's
            // no need to wrap
            return false;
        }
        // Else, we should wrap
        return true;
    }

    private final Resources mResources;
    private final Resources.Theme mTheme;

    private TintContextWrapper(@NonNull final Context base) {
        super(base);

        if (VectorEnabledTintResources.shouldBeUsed()) {
            // We need to create a copy of the Theme so that the Theme references our
            // new Resources instance
            mResources = new VectorEnabledTintResources(this, base.getResources());
            mTheme = mResources.newTheme();
            mTheme.setTo(base.getTheme());
        } else {
            mResources = new TintResources(this, base.getResources());
            mTheme = null;
        }
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

    @Override
    public void setTheme(int resid) {
        if (mTheme == null) {
            super.setTheme(resid);
        } else {
            mTheme.applyStyle(resid, true);
        }
    }

    @Override
    public Resources getResources() {
        return mResources;
    }
}