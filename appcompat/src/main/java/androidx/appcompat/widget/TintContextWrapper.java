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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A {@link android.content.ContextWrapper} which returns a tint-aware
 * {@link android.content.res.Resources} instance from {@link #getResources()}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TintContextWrapper extends ContextWrapper {

    private static final Object CACHE_LOCK = new Object();
    private static ArrayList<WeakReference<TintContextWrapper>> sCache;

    public static Context wrap(@NonNull final Context context) {
        if (shouldWrap(context)) {
            synchronized (CACHE_LOCK) {
                if (sCache == null) {
                    sCache = new ArrayList<>();
                } else {
                    // This is a convenient place to prune any dead reference entries
                    for (int i = sCache.size() - 1; i >= 0; i--) {
                        final WeakReference<TintContextWrapper> ref = sCache.get(i);
                        if (ref == null || ref.get() == null) {
                            sCache.remove(i);
                        }
                    }
                    // Now check our instance cache
                    for (int i = sCache.size() - 1; i >= 0; i--) {
                        final WeakReference<TintContextWrapper> ref = sCache.get(i);
                        final TintContextWrapper wrapper = ref != null ? ref.get() : null;
                        if (wrapper != null && wrapper.getBaseContext() == context) {
                            return wrapper;
                        }
                    }
                }
                // If we reach here then the cache didn't have a hit, so create a new instance
                // and add it to the cache
                final TintContextWrapper wrapper = new TintContextWrapper(context);
                sCache.add(new WeakReference<>(wrapper));
                return wrapper;
            }
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
        return Build.VERSION.SDK_INT < 21 || VectorEnabledTintResources.shouldBeUsed();
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

    @Override
    public AssetManager getAssets() {
        // Ensure we're returning assets with the correct configuration.
        return mResources.getAssets();
    }
}