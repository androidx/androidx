/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.car.utils;

import android.car.drivingstate.CarUxRestrictions;

/**
 * Utility handy for testing functionality regarding
 * {@link android.car.drivingstate.CarUxRestrictions}.
 */
public class CarUxRestrictionsTestUtils {

    private CarUxRestrictionsTestUtils() {};

    public static CarUxRestrictions getFullyRestricted() {
        return new CarUxRestrictions.Builder(
                true, CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED, 0).build();
    }

    public static CarUxRestrictions getBaseline() {
        return new CarUxRestrictions.Builder(
                false, CarUxRestrictions.UX_RESTRICTIONS_BASELINE, 0).build();
    }
}
