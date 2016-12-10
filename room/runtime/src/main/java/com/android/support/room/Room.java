/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room;

import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Room runtime.
 */
@SuppressWarnings("unused")
public class Room {
    private static Map<Class, CursorConverter> sCache = new HashMap<>();

    /**
     * Returns the CursorConverter for the given type.
     *
     * @param klass The class to convert from Cursor
     * @param <T> The type parameter of the class
     * @return A CursorConverter that can create an instance of the given klass from a Cursor.
     */
    public static <T> CursorConverter<T> getConverter(Class<T> klass) {
        CursorConverter existing = sCache.get(klass);
        if (existing != null) {
            //noinspection unchecked
            return existing;
        }
        CursorConverter<T> generated = getGeneratedCursorConverter(klass);
        sCache.put(klass, generated);
        return generated;
    }

    @Nullable
    private static <T> CursorConverter<T> getGeneratedCursorConverter(Class<T> klass) {
        final String fullPackage = klass.getPackage().getName();
        final String converterName = getConverterName(klass.getSimpleName());
        //noinspection TryWithIdenticalCatches
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends CursorConverter<T>> aClass =
                    (Class<? extends CursorConverter<T>>) Class.forName(
                            fullPackage + "." + converterName);
            return aClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find cursor converter for "
                    + klass.getCanonicalName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access cursor converter constructor"
                    + klass.getCanonicalName());
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to create an instance of the cursor converter"
                    + klass.getCanonicalName());
        }
    }

    private static String getConverterName(String className) {
        return className.replace(".", "_") + "_CursorConverter";
    }
}
