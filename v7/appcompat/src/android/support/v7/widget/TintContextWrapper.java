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
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * A {@link android.content.ContextWrapper} which returns a tint-aware
 * {@link android.content.res.Resources} instance from {@link #getResources()}.
 */
class TintContextWrapper extends ContextWrapper {

    private static final ArrayList<WeakReference<TintContextWrapper>> sCache = new ArrayList<>();

    public static Context wrap(@NonNull final Context context) {
        if (!(context instanceof TintContextWrapper)) {
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

    private Resources mResources;

    private TintContextWrapper(Context base) {
        super(base);
    }

    @Override
    public Resources getResources() {
        if (mResources == null) {
            mResources = new TintResources(this, super.getResources());
        }
        return mResources;
    }
}