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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a RoomDatabase.
 * <p>
 * The class should be an abstract class and extend
 * {@link android.arch.persistence.room.RoomDatabase RoomDatabase}.
 * <p>
 * You can receive an implementation of the class via
 * {@link android.arch.persistence.room.Room#databaseBuilder Room.databaseBuilder} or
 * {@link android.arch.persistence.room.Room#inMemoryDatabaseBuilder Room.inMemoryDatabaseBuilder}.
 * <p>
 * <pre>
 * // User and Book are classes annotated with {@literal @}Entity.
 * {@literal @}Database(version = 1, entities = {User.class, Book.class})
 * abstract class AppDatabase extends RoomDatabase() {
 *     // BookDao is a class annotated with {@literal @}Dao.
 *     abstract public BookDao bookDao();
 *     // UserDao is a class annotated with {@literal @}Dao.
 *     abstract public UserDao userDao();
 *     // UserBookDao is a class annotated with {@literal @}Dao.
 *     abstract public UserBookDao userBookDao();
 * }
 * </pre>
 * The example above defines a class that has 2 tables and 3 DAO classes that are used to access it.
 * There is no limit on the number of {@link Entity} or {@link Dao} classes but they must be unique
 * within the Database.
 * <p>
 * Instead of running queries on the database directly, you are highly recommended to create
 * {@link Dao} classes. Using Dao classes will allow you to abstract the database communication in
 * a more logical layer which will be much easier to mock in tests (compared to running direct
 * sql queries). It also automatically does the conversion from {@code Cursor} to your application
 * classes so you don't need to deal with lower level database APIs for most of your data access.
 * <p>
 * Room also verifies all of your queries in {@link Dao} classes while the application is being
 * compiled so that if there is a problem in one of the queries, you will be notified instantly.
 * @see Dao
 * @see Entity
 * @see android.arch.persistence.room.RoomDatabase RoomDatabase
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Database {
    /**
     * The list of entities included in the database. Each entity turns into a table in the
     * database.
     *
     * @return The list of entities in the database.
     */
    Class[] entities();

    /**
     * The database version.
     *
     * @return The database version.
     */
    int version();

    /**
     * You can set annotation processor argument ({@code room.schemaLocation})
     * to tell Room to export the schema into a folder. Even though it is not mandatory, it is a
     * good practice to have version history in your codebase and you should commit that file into
     * your version control system (but don't ship it with your app!).
     * <p>
     * When {@code room.schemaLocation} is set, Room will check this variable and if it is set to
     * {@code true}, its schema will be exported into the given folder.
     * <p>
     * {@code exportSchema} is {@code true} by default but you can disable it for databases when
     * you don't want to keep history of versions (like an in-memory only database).
     *
     * @return Whether the schema should be exported to the given folder when the
     * {@code room.schemaLocation} argument is set. Defaults to {@code true}.
     */
    boolean exportSchema() default true;
}
