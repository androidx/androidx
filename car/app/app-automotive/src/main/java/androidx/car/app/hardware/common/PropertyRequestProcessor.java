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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.car.Car;
import android.car.hardware.CarPropertyConfig;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for interacting with the {@link CarPropertyManager} for getting any vehicle property.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
final class PropertyRequestProcessor {
    private final CarPropertyManager mCarPropertyManager;

    /**
     *  Registers this listener to get results from
     *  {@link #fetchCarPropertyValues(List, OnGetPropertiesListener)}.
     */
    interface OnGetPropertiesListener {
        /**
         * Called when get all properties' value or errors.
         *
         * @param propertyValues    a list of {@link CarPropertyValue}, empty if there are no values
         * @param errors            a list of {@link CarInternalError}, empty if there are no errors
         */
        void onGetProperties(List<CarPropertyValue<?>> propertyValues,
                List<CarInternalError> errors);
    }

    /**
     * Gets {@link CarPropertyValue} and returns results by
     * {@link OnGetPropertiesListener#onGetProperties(List, List)}.
     *
     * @param requests  a list of {@Code Pair<Integer, Integer>}, {@Code Pair.first} is the
     *                  property id, {@Code Pair.second} is the area id
     * @param listener  the listener that will be invoked with the results of the request
     */
    public void fetchCarPropertyValues(
            @NonNull List<Pair<Integer, Integer>> requests,
            @NonNull OnGetPropertiesListener listener) {
        List<CarPropertyValue<?>> values = new ArrayList<>();
        List<CarInternalError> errors = new ArrayList<>();
        for (Pair<Integer, Integer> request : requests) {
            try {
                CarPropertyConfig<?> propertyConfig =
                        mCarPropertyManager.getCarPropertyConfig(request.first);
                if (propertyConfig == null) {
                    errors.add(CarInternalError.create(request.first, request.second,
                            CarValue.STATUS_UNIMPLEMENTED));
                } else {
                    Class<?> clazz = propertyConfig.getPropertyType();
                    CarPropertyValue<?> propertyValue = mCarPropertyManager.getProperty(clazz,
                            request.first, request.second);
                    values.add(propertyValue);
                }
            } catch (IllegalArgumentException e) {
                // TODO(b/191084385): consider using exception inside CarValue
                errors.add(CarInternalError.create(request.first, request.second,
                        CarValue.STATUS_UNIMPLEMENTED));
            } catch (Exception e) {
                errors.add(CarInternalError.create(request.first, request.second,
                        CarValue.STATUS_UNAVAILABLE));
            }
        }
        listener.onGetProperties(values, errors);
    }

    PropertyRequestProcessor(Context context) {
        Car car = Car.createCar(context);
        mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
    }
}
