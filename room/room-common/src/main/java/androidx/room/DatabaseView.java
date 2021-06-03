/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an SQLite view.
 * <p>
 * The value of the annotation is a SELECT query used when the view is created.
 * <p>
 * The class will behave like normal POJO when it is used in a {@link Dao}. You can SELECT FROM a
 * {@link DatabaseView} similar to an {@link Entity}, but you can not INSERT, DELETE or UPDATE
 * into a {@link DatabaseView}.
 * <p>
 * Similar to an {@link Entity}, you can use {@link ColumnInfo} and {@link Embedded} inside to
 * customize the data class.
 * <p>
 * Example:
 * <pre>
 * {@literal @}DatabaseView(
 *     "SELECT id, name, release_year FROM Song " +
 *     "WHERE release_year >= 1990 AND release_year <= 1999")
 * public class SongFrom90s {
 *   long id;
 *   String name;
 *   {@literal @}ColumnInfo(name = "release_year")
 *   private int releaseYear;
 * }
 * </pre>
 * <p>
 * Views have to be registered to a RoomDatabase via {@link Database#views}.
 *
 * @see Dao
 * @see Database
 * @see ColumnInfo
 * @see Embedded
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface DatabaseView {

    /**
     * The SELECT query.
     *
     * @return The SELECT query.
     */
    String value() default "";

    /**
     * The view name in the SQLite database. If not set, it defaults to the class name.
     *
     * @return The SQLite view name.
     */
    String viewName() default "";
}
