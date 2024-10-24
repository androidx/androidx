/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.model.constraints;

import android.content.ContentResolver;

import androidx.annotation.RestrictTo;
import androidx.car.app.model.CarIcon;
import androidx.core.graphics.drawable.IconCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Encapsulates the constraints to apply when rendering a {@link CarIcon} on a template.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CarIconConstraints {
    /** Allow all custom icon types. */
    public static final @NonNull CarIconConstraints UNCONSTRAINED =
            CarIconConstraints.create(
                    new int[]{
                            IconCompat.TYPE_BITMAP,
                            IconCompat.TYPE_RESOURCE,
                            IconCompat.TYPE_URI
                    });

    /** By default, do not allow custom icon types that would load asynchronously in the host. */
    public static final @NonNull CarIconConstraints DEFAULT =
            CarIconConstraints.create(new int[]{IconCompat.TYPE_BITMAP, IconCompat.TYPE_RESOURCE});

    private final int[] mAllowedTypes;

    private static CarIconConstraints create(int[] allowedCustomIconTypes) {
        return new CarIconConstraints(allowedCustomIconTypes);
    }

    /**
     * Returns {@code true} if the {@link CarIcon} meets the constraints' requirement.
     *
     * @throws IllegalStateException    if the custom icon does not have a backing
     *                                  {@link IconCompat} instance
     * @throws IllegalArgumentException if the custom icon type is not allowed
     */
    public void validateOrThrow(@Nullable CarIcon carIcon) {
        if (carIcon == null || carIcon.getType() != CarIcon.TYPE_CUSTOM) {
            return;
        }

        IconCompat iconCompat = carIcon.getIcon();
        if (iconCompat == null) {
            throw new IllegalStateException("Custom icon does not have a backing IconCompat");
        }

        checkSupportedIcon(iconCompat);
    }

    /**
     * Checks whether the given icon is supported.
     *
     * @throws IllegalArgumentException if the given icon type is unsupported
     */
    public @NonNull IconCompat checkSupportedIcon(@NonNull IconCompat iconCompat) {
        int type = iconCompat.getType();
        for (int allowedType : mAllowedTypes) {
            if (type == allowedType) {
                if (type == IconCompat.TYPE_URI
                        && !ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(
                        iconCompat.getUri().getScheme())) {
                    throw new IllegalArgumentException("Unsupported URI scheme for: " + iconCompat);
                }
                return iconCompat;
            }
        }
        throw new IllegalArgumentException("Custom icon type is not allowed: " + type);
    }

    private CarIconConstraints(int[] allowedCustomIconTypes) {
        mAllowedTypes = allowedCustomIconTypes;
    }
}
