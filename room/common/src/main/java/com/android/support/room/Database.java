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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a RoomDatabase.
 * <p>
 * The class should be an abstract class and extend
 * {@link com.android.support.room.RoomDatabase RoomDatabase}.
 * <p>
 * You can receive an implementation of the class via
 * {com.android.support.room.Room#databaseBuilder} or
 * {com.android.support.room.Room#inMemoryDatabaseBuilder}.
 * <p>
 * <pre>
 * // User and Book are classes annotated with {@literal @}Entity.
 * {@literal @}Database(entities = {User.class, Book.class})
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
 * Room also verifies all of your queries in Dao classes while the application is being compiled so
 * that if there is a problem in one of the queries, you will be notified instantly.
 * @see Dao
 * @see Entity
 * @see com.android.support.room.RoomDatabase RoomDatabase
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
}
