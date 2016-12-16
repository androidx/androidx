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

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Room runtime.
 */
@SuppressWarnings("unused")
public class Room {
    private static final String CURSOR_CONV_SUFFIX = "_CursorConverter";
    private static Map<Class, CursorConverter> sCursorConverterCache = new HashMap<>();

    /**
     * Creates a RoomDatabase.Builder for a persistent database. Once a database is built, you
     * should keep a reference to it and re-use.
     *
     * @param context The context for the database. This is usually the Application context.
     * @param klass The abstract class which is annotated with {@link Database} and extends
     * {@link RoomDatabase}.
     * @param name The name of the database file.
     * @param <T> The type of the database class.
     * @return A {@code RoomDatabaseBuilder<T>} which you can use to create the database.
     */
    @SuppressWarnings("WeakerAccess")
    public static <T extends RoomDatabase> RoomDatabase.Builder<T> databaseBuilder(
            @NonNull Context context, @NonNull Class<T> klass, @NonNull String name) {
        //noinspection ConstantConditions
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Cannot create a database with null or empty name."
                    + " If you are trying to create an in memory database, use Room"
                    + ".inMemoryDatabaseBuilder");
        }
        return new RoomDatabase.Builder<>(context, klass, name);
    }

    /**
     * Creates a RoomDatabase.Builder for an in memory database. Information stored in an in memory
     * database disappears when the process is killed.
     * Once a database is built, you should keep a reference to it and re-use.
     *
     * @param context The context for the database. This is usually the Application context.
     * @param klass The abstract class which is annotated with {@link Database} and extends
     * {@link RoomDatabase}.
     * @param <T> The type of the database class.
     * @return A {@code RoomDatabaseBuilder<T>} which you can use to create the database.
     */
    public static <T extends RoomDatabase> RoomDatabase.Builder<T> inMemoryDatabaseBuilder(
            @NonNull Context context, @NonNull Class<T> klass) {
        return new RoomDatabase.Builder<>(context, klass, null);
    }

    /**
     * Returns the CursorConverter for the given type.
     *
     * @param klass The class to convert from Cursor
     * @param <T> The type parameter of the class
     * @return A CursorConverter that can create an instance of the given klass from a Cursor.
     */
    public static <T> CursorConverter<T> getConverter(Class<T> klass) {
        CursorConverter existing = sCursorConverterCache.get(klass);
        if (existing != null) {
            //noinspection unchecked
            return existing;
        }
        CursorConverter<T> generated = getGeneratedImplementation(klass, CURSOR_CONV_SUFFIX);
        sCursorConverterCache.put(klass, generated);
        return generated;
    }

    @NonNull
    static <T, C> T getGeneratedImplementation(Class<C> klass, String suffix) {
        //noinspection TryWithIdenticalCatches
        try {
            @SuppressWarnings("unchecked")
            final Class<T> aClass =
                    (Class<T>) Class.forName(klass.getName() + suffix);
            return aClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find implementation for "
                    + klass.getCanonicalName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access the constructor"
                    + klass.getCanonicalName());
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to create an instance of "
                    + klass.getCanonicalName());
        }
    }
}
