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
package androidx.room

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * Utility functions for Room.
 */
object Room {

    internal const val LOG_TAG = "ROOM"

    /**
     * The master table where room keeps its metadata information.
     */
    const val MASTER_TABLE_NAME = RoomMasterTable.TABLE_NAME

    private const val CURSOR_CONV_SUFFIX = "_CursorConverter"

    @Suppress("UNCHECKED_CAST")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmStatic
    fun <T, C> getGeneratedImplementation(
        klass: Class<C>,
        suffix: String
    ): T {
        val fullPackage = klass.getPackage()!!.name
        val name: String = klass.canonicalName!!
        val postPackageName =
            if (fullPackage.isEmpty()) name else name.substring(fullPackage.length + 1)
        val implName = postPackageName.replace('.', '_') + suffix
        return try {
            val fullClassName = if (fullPackage.isEmpty()) {
                implName
            } else {
                "$fullPackage.$implName"
            }
            val aClass = Class.forName(
                fullClassName, true, klass.classLoader
            ) as Class<T>
            aClass.getDeclaredConstructor().newInstance()
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(
                "Cannot find implementation for ${klass.canonicalName}. $implName does not " +
                    "exist"
            )
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                "Cannot access the constructor ${klass.canonicalName}"
            )
        } catch (e: InstantiationException) {
            throw RuntimeException(
                "Failed to create an instance of ${klass.canonicalName}"
            )
        }
    }

    /**
     * Creates a RoomDatabase.Builder for an in memory database. Information stored in an in memory
     * database disappears when the process is killed.
     * Once a database is built, you should keep a reference to it and re-use it.
     *
     * @param context The context for the database. This is usually the Application context.
     * @param klass   The abstract class which is annotated with [Database] and extends
     * [RoomDatabase].
     * @param T     The type of the database class.
     * @return A `RoomDatabaseBuilder<T>` which you can use to create the database.
     */
    @JvmStatic
    fun <T : RoomDatabase> inMemoryDatabaseBuilder(
        context: Context,
        klass: Class<T>
    ): RoomDatabase.Builder<T> {
        return RoomDatabase.Builder(context, klass, null)
    }

    /**
     * Creates a RoomDatabase.Builder for a persistent database. Once a database is built, you
     * should keep a reference to it and re-use it.
     *
     * @param context The context for the database. This is usually the Application context.
     * @param klass   The abstract class which is annotated with [Database] and extends
     * [RoomDatabase].
     * @param name    The name of the database file.
     * @param T     The type of the database class.
     * @return A `RoomDatabaseBuilder<T>` which you can use to create the database.
     */
    @JvmStatic
    fun <T : RoomDatabase> databaseBuilder(
        context: Context,
        klass: Class<T>,
        name: String?
    ): RoomDatabase.Builder<T> {
        require(!name.isNullOrBlank()) {
            "Cannot build a database with null or empty name." +
                " If you are trying to create an in memory database, use Room" +
                ".inMemoryDatabaseBuilder"
        }
        return RoomDatabase.Builder(context, klass, name)
    }
}
