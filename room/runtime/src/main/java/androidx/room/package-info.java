/*
 * Copyright (C) 2017 The Android Open Source Project
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

/**
 * Room is a Database Object Mapping library that makes it easy to access database on Android
 * applications.
 * <p>
 * Rather than hiding the detail of SQLite, Room tries to embrace them by providing convenient APIs
 * to query the database and also verify such queries at compile time. This allows you to access
 * the full power of SQLite while having the type safety provided by Java SQL query builders.
 * <p>
 * There are 3 major components in Room.
 * <ul>
 *     <li>{@link androidx.room.Database Database}: This annotation marks a
 *     class as a database. It should be an abstract class that extends
 *     {@link androidx.room.RoomDatabase RoomDatabase}. At runtime, you can acquire
 *     an instance of it via {@link androidx.room.Room#databaseBuilder(
 *     android.content.Context,java.lang.Class, java.lang.String) Room.databaseBuilder} or
 *     {@link androidx.room.Room#inMemoryDatabaseBuilder(android.content.Context,
 *     java.lang.Class) Room.inMemoryDatabaseBuilder}.
 *     <p>
 *         This class defines the list of entities and data access objects in the database. It is
 *         also the main access point for the underlying connection.
 *     </li>
 *     <li>{@link androidx.room.Entity Entity}: This annotation marks a class as a
 *     database row. For each {@link androidx.room.Entity Entity}, a database table
 *     is created to hold the items. The Entity class must be referenced in the
 *     {@link androidx.room.Database#entities() Database#entities} array. Each field
 *     of the Entity (and its super class) is persisted in the database unless it is denoted
 *     otherwise (see {@link androidx.room.Entity Entity} docs for details).
 *     </li>
 *     <li>{@link androidx.room.Dao Dao}: This annotation marks a class or interface
 *     as a Data Access Object. Data access objects are the main component of Room that are
 *     responsible for defining the methods that access the database. The class that is annotated
 *     with {@link androidx.room.Database Database} must have an abstract method
 *     that has 0 arguments and returns the class that is annotated with Dao. While generating the
 *     code at compile time, Room will generate an implementation of this class.
 *     <pre>
 *     Using Dao classes for database access rather than query builders or direct queries allows you
 *     to keep a separation between different components and easily mock the database access while
 *     testing your application.
 *     </li>
 * </ul>
 * Below is a sample of a simple database.
 * <pre>
 * // File: User.java
 * {@literal @}Entity
 * public class User {
 *   {@literal @}PrimaryKey
 *   private int uid;
 *   private String name;
 *   {@literal @}ColumnInfo(name = "last_name")
 *   private String lastName;
 *   // getters and setters are ignored for brevity but they are required for Room to work.
 * }
 * // File: UserDao.java
 * {@literal @}Dao
 * public interface UserDao {
 *   {@literal @}Query("SELECT * FROM user")
 *   List&lt;User&gt; loadAll();
 *   {@literal @}Query("SELECT * FROM user WHERE uid IN (:userIds)")
 *   List&lt;User&gt; loadAllByUserId(int... userIds);
 *   {@literal @}Query("SELECT * FROM user where name LIKE :first AND last_name LIKE :last LIMIT 1")
 *   User loadOneByNameAndLastName(String first, String last);
 *   {@literal @}Insert
 *   void insertAll(User... users);
 *   {@literal @}Delete
 *   void delete(User user);
 * }
 * // File: AppDatabase.java
 * {@literal @}Database(entities = {User.java})
 * public abstract class AppDatabase extends RoomDatabase {
 *   public abstract UserDao userDao();
 * }
 * </pre>
 * You can create an instance of {@code AppDatabase} as follows:
 * <pre>
 * AppDatabase db = Room
 *     .databaseBuilder(getApplicationContext(), AppDatabase.class, "database-name")
 *     .build();
 * </pre>
 * Since Room verifies your queries at compile time, it also detects information about which tables
 * are accessed by the query or what columns are present in the response.
 * <p>
 * You can observe a particular table for changes using the
 * {@link androidx.room.InvalidationTracker InvalidationTracker} class which you can
 * acquire via {@link androidx.room.RoomDatabase#getInvalidationTracker()
 * RoomDatabase.getInvalidationTracker}.
 * <p>
 * For convenience, Room allows you to return {@link androidx.lifecycle.LiveData
 * LiveData} from {@link androidx.room.Query Query} methods. It will automatically
 * observe the related tables as long as the {@code LiveData} has active observers.
 * <pre>
 * // This live data will automatically dispatch changes as the database changes.
 * {@literal @}Query("SELECT * FROM user ORDER BY name LIMIT 5")
 * LiveData&lt;User&gt; loadFirstFiveUsers();
 * </pre>
 * <p>
 * You can also return arbitrary Java objects from your query results as long as the fields in the
 * object match the list of columns in the query response. This makes it very easy to write
 * applications that drive the UI from persistent storage.
 * <pre>
 * class IdAndFullName {
 *     public int uid;
 *     {@literal @}ColumnInfo(name = "full_name")
 *     public String fullName;
 * }
 * // DAO
 * {@literal @}Query("SELECT uid, name || lastName as full_name FROM user")
 * public IdAndFullName[] loadFullNames();
 * </pre>
 * If there is a mismatch between the query result and the POJO, Room will print a warning during
 * compilation.
 * <p>
 * Please see the documentation of individual classes for details.
 */
package androidx.room;
