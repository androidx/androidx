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

import static androidx.annotation.RestrictTo.Scope;

import androidx.annotation.RestrictTo;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarColor.CarColorType;

import org.jspecify.annotations.NonNull;

import java.util.HashSet;

/**
 * Encapsulates the constraints to apply when rendering a {@link CarColor} on a template.
 *
 */
@RestrictTo(Scope.LIBRARY)
public final class CarColorConstraints {

    public static final @NonNull CarColorConstraints UNCONSTRAINED =
            CarColorConstraints.create(
                    new int[]{
                            CarColor.TYPE_CUSTOM,
                            CarColor.TYPE_DEFAULT,
                            CarColor.TYPE_PRIMARY,
                            CarColor.TYPE_SECONDARY,
                            CarColor.TYPE_RED,
                            CarColor.TYPE_GREEN,
                            CarColor.TYPE_BLUE,
                            CarColor.TYPE_YELLOW
                    });

    public static final @NonNull CarColorConstraints STANDARD_ONLY =
            CarColorConstraints.create(
                    new int[]{
                            CarColor.TYPE_DEFAULT,
                            CarColor.TYPE_PRIMARY,
                            CarColor.TYPE_SECONDARY,
                            CarColor.TYPE_RED,
                            CarColor.TYPE_GREEN,
                            CarColor.TYPE_BLUE,
                            CarColor.TYPE_YELLOW
                    });

    @CarColorType
    private final HashSet<Integer> mAllowedTypes;

    private static CarColorConstraints create(int[] allowedColorTypes) {
        return new CarColorConstraints(allowedColorTypes);
    }

    /**
     * Returns {@code true} if the {@link CarColor} meets the constraints' requirement.
     *
     * @throws IllegalArgumentException if the color type is not allowed
     */
    public void validateOrThrow(@NonNull CarColor carColor) {
        @CarColorType int type = carColor.getType();
        if (!mAllowedTypes.contains(type)) {
            throw new IllegalArgumentException("Car color type is not allowed: " + carColor);
        }
    }

    private CarColorConstraints(int[] allowedColorTypes) {
        mAllowedTypes = new HashSet<>();
        for (int type : allowedColorTypes) {
            mAllowedTypes.add(type);
        }
    }
}
