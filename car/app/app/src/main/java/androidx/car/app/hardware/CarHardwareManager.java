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
package androidx.car.app.hardware;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.HostDispatcher;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.info.CarInfo;
import androidx.car.app.hardware.info.CarSensors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Manages access to car hardware specific properties and sensors.
 */
@RequiresCarApi(3)
public interface CarHardwareManager {
    /**
     * Returns the {@link CarInfo} that can be used to query the car hardware information
     * such as make, model, etc.
     */
    default @NonNull CarInfo getCarInfo() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the {@link CarSensors} that can be used to access sensor information from the
     * car hardware.
     */
    default @NonNull CarSensors getCarSensors() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an instance of {@link CarHardwareManager} depending on linked library including an
     * optional {@link HostDispatcher}.
     *
     * @throws IllegalStateException if none of the supported classes are found or if a supported
     *                               class was found but the constructor was mismatched
     * @hide
     */
    @RestrictTo(LIBRARY)
    @NonNull
    static CarHardwareManager create(@NonNull Context context,
            @Nullable HostDispatcher hostDispatcher) throws IllegalStateException {

        try { // Check for automotive library first.
            Class<?> c = Class.forName("androidx.car.app.hardware.CarHardwareManagerAutomotive");
            Constructor<?> ctor = c.getConstructor(Context.class);
            Object object = ctor.newInstance(context);
            return (CarHardwareManager) object;
        } catch (ClassNotFoundException e) {
            // No Automotive. Fall through.
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException e) {
            // Something went wrong with accessing the constructor or calling newInstance().
            throw new IllegalStateException("Mismatch with app-automotive artifact", e);
        }

        try { // Check for automotive library first.
            Class<?> c = Class.forName("androidx.car.app.hardware.CarHardwareManagerProjected");
            Constructor<?> ctor = c.getConstructor(HostDispatcher.class);
            Object object = ctor.newInstance(hostDispatcher);
            return (CarHardwareManager) object;
        } catch (ClassNotFoundException e) {
            // No Projected. Fall through.
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException e) {
            // Something went wrong with accessing the constructor or calling newInstance().
            throw new IllegalStateException("Mismatch with app-projected artifact", e);
        }

        throw new IllegalStateException("Vehicle Manager not "
                + "configured. Did you forget to add a dependency on automotive or "
                + "projected artifacts?");
    }
}
