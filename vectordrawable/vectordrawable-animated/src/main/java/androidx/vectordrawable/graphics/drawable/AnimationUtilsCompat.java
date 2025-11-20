/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.vectordrawable.graphics.drawable;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import androidx.annotation.AnimRes;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

import org.jspecify.annotations.NonNull;

/**
 * Defines common utilities for working with animations.
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class AnimationUtilsCompat {
    /**
     * Loads an {@link Interpolator} object from a resource
     *
     * @param context Application context used to access resources
     * @param id      The resource id of the animation to load
     * @return The animation object reference by the specified id
     */
    @SuppressWarnings("UnnecessaryInitCause") // requires API 24+
    public static @NonNull Interpolator loadInterpolator(@NonNull Context context, @AnimRes int id)
            throws NotFoundException {
        Interpolator interp = AnimationUtils.loadInterpolator(context, id);
        ObjectsCompat.requireNonNull(interp, "Failed to parse interpolator, no start tag "
                + "found");
        return interp;

    }

    private AnimationUtilsCompat() {
    }
}
