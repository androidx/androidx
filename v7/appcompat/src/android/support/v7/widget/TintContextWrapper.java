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
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

/**
 * A {@link android.content.ContextWrapper} which returns a tint-aware
 * {@link android.content.res.Resources} instance from {@link #getResources()}.
 */
class TintContextWrapper extends ContextWrapper {

    public static Context wrap(Context context) {
        if (!(context instanceof TintContextWrapper)) {
            context = new TintContextWrapper(context);
        }
        return context;
    }

    private Resources mResources;
    private final Resources.Theme mTheme;

    private TintContextWrapper(@NonNull final Context base) {
        super(base);

        // We need to create a copy of the Theme so that the Theme references our Resources
        // instance
        mTheme = getResources().newTheme();
        mTheme.setTo(base.getTheme());
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme;
    }

    @Override
    public void setTheme(int resid) {
        mTheme.applyStyle(resid, true);
    }

    @Override
    public Resources getResources() {
        if (mResources == null) {
            mResources = new TintResources(this, super.getResources());
        }
        return mResources;
    }
}