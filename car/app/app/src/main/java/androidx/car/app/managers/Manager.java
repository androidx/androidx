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

package androidx.car.app.managers;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;

/**
 * A manager that extends the services provided by {@link androidx.car.app.CarContext}. These
 * manager can be obtained from {@link androidx.car.app.CarContext#getCarService(Class)} or
 * {@link androidx.car.app.CarContext#getCarService(String)}
 */
public interface Manager {
    /**
     * Helper method to create managers by reflection
     *
     * @param clazz interface manager expected to receive
     * @param className class name of the manager
     * @param args arguments to send to the manager
     * @param <U> manager type
     * @return an instance of the given manager, or null if the given class name is not found.
     * @throws IllegalStateException if the class exists, but there was an error trying to
     *         instantiate it.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    static <U extends Manager> @Nullable U create(@NonNull Class<U> clazz,
            @NonNull String className, Object  @NonNull ... args) {
        try { // Check for automotive library first.
            Class<?> c = Class.forName(className);
            Class<?>[] argsTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argsTypes[i] = args[i].getClass();
            }
            Constructor<?> ctor = c.getConstructor(argsTypes);
            return clazz.cast(ctor.newInstance(args));
        } catch (ClassNotFoundException e) {
            // Not found, fall through.
            return null;
        } catch (ReflectiveOperationException e) {
            // Something went wrong with accessing the constructor or calling newInstance().
            throw new IllegalStateException("Mismatch with artifact", e);
        }
    }
}
