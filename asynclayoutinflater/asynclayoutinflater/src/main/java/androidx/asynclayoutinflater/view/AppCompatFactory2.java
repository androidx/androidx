/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.asynclayoutinflater.view;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.R;
import androidx.appcompat.app.AppCompatViewInflater;
import androidx.appcompat.widget.VectorEnabledTintResources;
/**
 * Factory for inflating views in AppCompat activity. This is used when Async inflater is created
 * with AppCompat context.
 */
public class AppCompatFactory2 implements LayoutInflater.Factory2 {
    private static final String TAG = "AppCompatFactory2";
    private AppCompatViewInflater mAppCompatViewInflater;
    /**
     * Creates view using {@link AppCompatViewInflater}.
     */
    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        return createView(parent, name, context, attrs);
    }
    /**
     * Creates view using {@link AppCompatViewInflater}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        return createView(/* parent= */ null, name, context, attrs);
    }
    View createView(View parent, final String name, @NonNull Context context,
            @NonNull AttributeSet attrs) {
        if (mAppCompatViewInflater == null) {
            TypedArray a = context.obtainStyledAttributes(R.styleable.AppCompatTheme);
            String viewInflaterClassName = a.getString(
                    R.styleable.AppCompatTheme_viewInflaterClass);
            if (viewInflaterClassName == null) {
                // Set to null (the default in all AppCompat themes). Create the base inflater
                // (no reflection)
                mAppCompatViewInflater = new AppCompatViewInflater();
            } else {
                try {
                    Class<?> viewInflaterClass = context.getClassLoader().loadClass(
                            viewInflaterClassName);
                    mAppCompatViewInflater =
                            (AppCompatViewInflater) viewInflaterClass
                                    .getDeclaredConstructor()
                                    .newInstance();
                } catch (Throwable t) {
                    Log.i(TAG, "Failed to instantiate custom view inflater " + viewInflaterClassName
                            + ". Falling back to default.", t);
                    mAppCompatViewInflater = new AppCompatViewInflater();
                }
            }
        }
        return mAppCompatViewInflater.createView(parent, name, context, attrs,
                /* inheritContext= */ false,
                /* isPreLollipop= */ false,
                true, /* Read read app:theme as a fallback at all times for legacy reasons */
                VectorEnabledTintResources.shouldBeUsed() /* Only tint wrap the context if
                enabled */);
    }
}
