/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.car.app.shadows.car;

import android.car.Car;
import android.content.Context;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Robolectric shadow class for {@link Car}.
 *
 * <p>{@link Car} is declared final, so this shadow class allows for mocking and
 * controlling return values and behavior of the {@link Car} instance.
 */
@Implements(Car.class)
public class ShadowCar {
    private static Car sCar;

    @Implementation
    public static Car createCar(Context context) {
        return sCar;
    }

    public static void setCar(Car car) {
        sCar = car;
    }

    @Implementation
    public Object getCarManager(String serviceName) {
        return sCar.getCarManager(serviceName);
    }
}
