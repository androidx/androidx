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

package androidx.car.app.versioning;

import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;

/**
 * API levels supported by this library.
 *
 * <p>Each level denotes a set of elements (classes, fields and methods) known to both clients and
 * hosts.
 *
 * @see CarContext#getCarAppApiLevel()
 */
public final class CarAppApiLevels {
    /**
     * API level 3.
     *
     * <p>Includes a car hardware manager for access to sensors and other vehicle properties.
     */
    @CarAppApiLevel
    public static final int LEVEL_3 = 3;

    /**
     * API level 2.
     *
     * <p>Includes features such as sign-in template, long-message template, and multi-variant
     * text support.
     */
    @CarAppApiLevel
    public static final int LEVEL_2 = 2;

    /**
     * Initial API level.
     *
     * <p>Includes core API services and managers, and templates for parking, charging, and
     * navigation apps.
     */
    @CarAppApiLevel
    public static final int LEVEL_1 = 1;

    /**
     * Unknown API level.
     *
     * <p>Used when the API level hasn't been established yet
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @CarAppApiLevel
    public static final int UNKNOWN = 0;

    /**
     * Returns whether the given integer is a valid {@link CarAppApiLevel}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static boolean isValid(int carApiLevel) {
        return carApiLevel >= getOldest() && carApiLevel <= getLatest();
    }

    /**
     * Returns the highest API level implemented by this library.
     */
    @CarAppApiLevel
    public static int getLatest() {
        return LEVEL_3;
    }

    /**
     * Returns the lowest API level implemented by this library.
     */
    @CarAppApiLevel
    public static int getOldest() {
        return LEVEL_1;
    }

    private CarAppApiLevels() {
    }
}
