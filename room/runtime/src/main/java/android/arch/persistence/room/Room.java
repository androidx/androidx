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

package android.arch.persistence.room;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Utility class for Room.
 */
@SuppressWarnings("unused")
public class Room {
    static final String LOG_TAG = "ROOM";
    /**
     * The master table where room keeps its metadata information.
     */
    public static final String MASTER_TABLE_NAME = RoomMasterTable.TABLE_NAME;
    private static final String CURSOR_CONV_SUFFIX = "_CursorConverter";

    /**
     * Creates a RoomDatabase.Builder for a persistent database. Once a database is built, you
     * should keep a reference to it and re-use it.
     *
     * @param context The context for the database. This is usually the Application context.
     * @param klass   The abstract class which is annotated with {@link Database} and extends
     *                {@link RoomDatabase}.
     * @param name    The name of the database file.
     * @param <T>     The type of the database class.
     * @return A {@code RoomDatabaseBuilder<T>} which you can use to create the database.
     */
    @SuppressWarnings("WeakerAccess")
    public static <T extends RoomDatabase> RoomDatabase.Builder<T> databaseBuilder(
            @NonNull Context context, @NonNull Class<T> klass, @NonNull String name) {
        //noinspection ConstantConditions
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Cannot build a database with null or empty name."
                    + " If you are trying to create an in memory database, use Room"
                    + ".inMemoryDatabaseBuilder");
        }
        return new RoomDatabase.Builder<>(context, klass, name);
    }

    /**
     * Creates a RoomDatabase.Builder for an in memory database. Information stored in an in memory
     * database disappears when the process is killed.
     * Once a database is built, you should keep a reference to it and re-use it.
     *
     * @param context The context for the database. This is usually the Application context.
     * @param klass   The abstract class which is annotated with {@link Database} and extends
     *                {@link RoomDatabase}.
     * @param <T>     The type of the database class.
     * @return A {@code RoomDatabaseBuilder<T>} which you can use to create the database.
     */
    public static <T extends RoomDatabase> RoomDatabase.Builder<T> inMemoryDatabaseBuilder(
            @NonNull Context context, @NonNull Class<T> klass) {
        return new RoomDatabase.Builder<>(context, klass, null);
    }

    @NonNull
    static <T, C> T getGeneratedImplementation(Class<C> klass, String suffix) {
        final String fullPackage = klass.getPackage().getName();
        String name = klass.getCanonicalName();
        final String postPackageName = fullPackage.isEmpty()
                ? name
                : (name.substring(fullPackage.length() + 1));
        final String implName = postPackageName.replace('.', '_') + suffix;
        //noinspection TryWithIdenticalCatches
        try {

            @SuppressWarnings("unchecked")
            final Class<T> aClass = (Class<T>) Class.forName(
                    fullPackage.isEmpty() ? implName : fullPackage + "." + implName);
            return aClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("cannot find implementation for "
                    + klass.getCanonicalName() + ". " + implName + " does not exist");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access the constructor"
                    + klass.getCanonicalName());
        } catch (InstantiationException e) {
            throw new RuntimeException("Failed to create an instance of "
                    + klass.getCanonicalName());
        }
    }
}
